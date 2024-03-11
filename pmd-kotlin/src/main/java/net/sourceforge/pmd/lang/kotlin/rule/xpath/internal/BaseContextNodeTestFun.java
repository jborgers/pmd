/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.kotlin.rule.xpath.internal;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.kotlin.ast.KotlinNode;
import net.sourceforge.pmd.lang.kotlin.ast.KotlinParser;
import net.sourceforge.pmd.lang.rule.xpath.internal.AstElementNode;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.SequenceType;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * XPath function {@code pmd-kotlin:hasChildren(count as xs:decimal) as xs:boolean}
 *
 * <p>Example XPath 3.1: {@code //Identifier[pmd-kotlin:hasChildren(3)]}
 *
 * <p>Returns true if the node has children, false otherwise.
 */
public class BaseContextNodeTestFun<T extends KotlinNode> extends BaseKotlinXPathFunction {

    static final SequenceType[] NO_ARGUMENTS = { SequenceType.SINGLE_INTEGER };
    private final Class<T> klass;
    private final BiPredicate<String, T> checker;

    public static final BaseKotlinXPathFunction HAS_CHILDREN = new BaseContextNodeTestFun<>(KotlinNode.class, "hasChildren", TestUtil::hasChildren);
    public static final BaseKotlinXPathFunction HAS_IMPORT = new BaseContextNodeTestFun<>(KotlinNode.class, "hasImport", TestUtil::hasImport);

    protected BaseContextNodeTestFun(Class<T> klass, String localName, BiPredicate<String, T> checker) {
        super(localName);
        this.klass = klass;
        this.checker = checker;
    }

    @Override
    public SequenceType[] getArgumentTypes() {
        return NO_ARGUMENTS;
    }

    @Override
    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return SequenceType.SINGLE_BOOLEAN;
    }

    @Override
    public boolean dependsOnFocus() {
        return true;
    }

    @Override
    public ExtensionFunctionCall makeCallExpression() {
        return new ExtensionFunctionCall() {
            @Override
            public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
                Node contextNode = ((AstElementNode) context.getContextItem()).getUnderlyingNode();
                return BooleanValue.get(klass.isInstance(contextNode) && checker.test(arguments[0].head().getStringValue(), (T) contextNode));
            }
        };
    }

}

class TestUtil {
    private TestUtil() {
        // utility class
    }

    public static boolean hasImport(final @NonNull String reqPackage, final @NonNull Node node) {
        // assuming root is /KotlinFile, if not, add getFirstChild()
        boolean aHeaderHasImport = false;
        Node root = node.getRoot();
        int numChildren = root.getNumChildren();
        for (int i = 0; i < numChildren; i++) {
            if (root.getChild(i) instanceof KotlinParser.KtImportList) {
                aHeaderHasImport = headerHasImport(reqPackage, (KotlinParser.KtImportList)node);
            }
        }
        return aHeaderHasImport;
    }

    private static boolean headerHasImport(final @NonNull String reqPackage, final KotlinParser.KtImportList node) {
        int numChildren = node.getNumChildren();
        for (int i = 0; i < numChildren; i++) {
            if (node.getChild(i) instanceof KotlinParser.KtImportHeader) {
                List<KotlinParser.KtSimpleIdentifier> simpleIdentifiers = ((KotlinParser.KtImportHeader) node.getChild(i)).identifier().simpleIdentifier(); // multiple
                List<String> importParts = new ArrayList<>();
                for (KotlinParser.KtSimpleIdentifier smplIdentifier: simpleIdentifiers) {
                    importParts.add(smplIdentifier.Identifier().getText()); // In designer called T-Identifier
                }
                String importJoined = String.join(".", importParts);
                if (importJoined.contains(reqPackage)) return true;
            }
        }
        return false;
    }

    public static boolean hasChildren(final @NonNull String reqNumChildren, final @NonNull Node node) {
        return node.getNumChildren() == Integer.parseInt(reqNumChildren);
    }

    /**
     * Checks whether the static type of the node is a subtype of the
     * class identified by the given name. This ignores type arguments,
     * if the type of the node is parameterized. Examples:
     *
     * <pre>{@code
     * isA(List.class, <new ArrayList<String>()>)      = true
     * isA(ArrayList.class, <new ArrayList<String>()>) = true
     * isA(int[].class, <new int[0]>)                  = true
     * isA(Object[].class, <new String[0]>)            = true
     * isA(_, null) = false
     * isA(null, _) = NullPointerException
     * }</pre>
     *
     * <p>If either type is unresolved, the types are tested for equality,
     * thus giving more useful results than {@link JTypeMirror#isSubtypeOf(JTypeMirror)}.
     *
     * <p>Note that primitives are NOT considered subtypes of one another
     * by this method, even though {@link JTypeMirror#isSubtypeOf(JTypeMirror)} does.
     *
     * @param clazz a class (non-null)
     * @param node  the type node to check
     *
     * @return true if the type test matches
     *
     * @throws NullPointerException if the class parameter is null
     * TODO
     */
    /*public static boolean isA(final @NonNull Class<?> clazz, final @Nullable TypeNode node) {
        AssertionUtil.requireParamNotNull("class", clazz);
        if (node == null) {
            return false;
        }

        return hasNoSubtypes(clazz) ? isExactlyA(clazz, node)
                : isA(clazz, node.getTypeMirror());
    }*/

}