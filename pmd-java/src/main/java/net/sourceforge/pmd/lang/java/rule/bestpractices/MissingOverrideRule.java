/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.rule.bestpractices;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;

import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.lang.java.ast.ASTAnyTypeDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration;
import net.sourceforge.pmd.lang.java.ast.JModifier;
import net.sourceforge.pmd.lang.java.ast.JavaNode;
import net.sourceforge.pmd.lang.java.ast.internal.PrettyPrintingUtil;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRulechainRule;
import net.sourceforge.pmd.lang.java.symbols.JMethodSymbol;
import net.sourceforge.pmd.lang.java.symbols.table.internal.SuperTypesEnumerator;
import net.sourceforge.pmd.lang.java.types.JClassType;
import net.sourceforge.pmd.lang.java.types.JMethodSig;
import net.sourceforge.pmd.lang.java.types.TypeOps;


/**
 * Flags missing @Override annotations.
 *
 * @author Clément Fournier
 * @since 6.2.0
 */
public class MissingOverrideRule extends AbstractJavaRulechainRule {

    public MissingOverrideRule() {
        super(ASTAnyTypeDeclaration.class);
    }

    @Override
    public Object visitJavaNode(JavaNode node, Object data) {
        visitTypeDecl((ASTAnyTypeDeclaration) node, (RuleContext) data);
        return data;
    }

    private void visitTypeDecl(ASTAnyTypeDeclaration node, RuleContext data) {
        // collect methods that may be violations, ie:
        // - may override another method (non private, non static)
        // - not already annotated @Override

        RelevantMethodSet relevantMethods = new RelevantMethodSet(node.getTypeMirror());

        for (ASTMethodDeclaration methodDecl : node.getDeclarations(ASTMethodDeclaration.class)) {
            relevantMethods.addIfRelevant(methodDecl);
        }

        if (relevantMethods.tracked.isEmpty()) {
            return;
        }

        Set<ASTMethodDeclaration> violatingMethods =
            // stream all methods of supertypes
            SuperTypesEnumerator.ALL_STRICT_SUPERTYPES
                .stream(node.getTypeMirror())
                // Filter down to those that may be overridden by one of the possible violations
                // This considers name, arity, and accessibility
                //                                      vvvvvvvvvvvvvvvvvvvvvvvvvvv
                .flatMap(st -> st.streamDeclaredMethods(relevantMethods::isRelevant))
                // For those methods, a simple override-equivalence check is enough,
                // because we already know they're accessible, and declared in a supertype
                .collect(relevantMethods.overriddenRelevantMethodsCollector());

        for (ASTMethodDeclaration violatingMethod : violatingMethods) {
            addViolation(data, violatingMethod, new Object[] { PrettyPrintingUtil.displaySignature(violatingMethod) });
        }
    }

    /**
     * This does a prefilter, so that we only collect methods of supertypes
     * that may be overridden by a sub method. For a method to be potentially
     * a super method, it must have same arity
     */
    private static final class RelevantMethodSet {

        // name to considered arities
        private final Map<String, BitSet> map = new HashMap<>();
        // nodes that may be violations
        private final Set<ASTMethodDeclaration> tracked = new LinkedHashSet<>();

        private final JClassType site;

        private RelevantMethodSet(JClassType site) {
            this.site = site;
        }


        // add a method if it may be a violation
        // this builds the data structure for isRelevant to work
        void addIfRelevant(ASTMethodDeclaration m) {
            if (m.isAnnotationPresent(Override.class)
                || m.getModifiers().hasAny(JModifier.STATIC, JModifier.PRIVATE)) {
                return;
            }
            // then add it
            BitSet aritySet = map.computeIfAbsent(m.getName(), n -> new BitSet(m.getArity() + 1));
            aritySet.set(m.getArity());
            tracked.add(m);
        }

        // we use this to only consider methods that may produce a violation,
        // among the supertype methods
        boolean isRelevant(JMethodSymbol superMethod) {
            if (!TypeOps.isOverridableIn(superMethod, site.getSymbol())) {
                return false;
            }
            BitSet aritySet = map.get(superMethod.getSimpleName());
            return aritySet != null && aritySet.get(superMethod.getArity());
        }

        // then, if the superSig, which comes from a supertype, is overridden
        // by a relevant method (ie a method that is a violation), then that
        // node truly is a violation, and is added to the output set.
        void addToSetIfIsOverridden(Set<ASTMethodDeclaration> relevantOverridingMethods,
                                    JMethodSig superSig) {
            ASTMethodDeclaration subSig = null;
            for (ASTMethodDeclaration it : tracked) {
                // note: we don't use override-equivalence, the definition
                // of an override uses the concept of sub-signature instead,
                // which is slightly different. We could also use TypeOps.overrides
                // but at this point we already know much of what that method checks.
                // https://docs.oracle.com/javase/specs/jls/se15/html/jls-8.html#jls-8.4.8.1
                if (TypeOps.isSubSignature(it.getGenericSignature(), superSig)) {
                    subSig = it;
                    // we assume there is a single relevant method that may match,
                    // otherwise it would be a compile-time error
                    break;
                }
            }
            if (subSig != null) {
                relevantOverridingMethods.add(subSig);
                tracked.remove(subSig); // speedup the check for later
            }
        }

        Collector<JMethodSig, ?, Set<ASTMethodDeclaration>> overriddenRelevantMethodsCollector() {
            return Collector.of(
                HashSet<ASTMethodDeclaration>::new,
                this::addToSetIfIsOverridden,
                (map1, map2) -> {
                    throw new UnsupportedOperationException("Dont use a parallel stream");
                },
                set -> set
            );
        }

    }
}
