/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.kotlin.rule.xpath.internal;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.kotlin.ast.KotlinNode;
import net.sourceforge.pmd.lang.rule.xpath.internal.AstElementNode;

import static net.sourceforge.pmd.lang.kotlin.ast.KotlinParser.KtImportHeader;
import static net.sourceforge.pmd.lang.kotlin.ast.KotlinParser.KtImportList;
import static net.sourceforge.pmd.lang.kotlin.ast.KotlinParser.KtSimpleIdentifier;

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
            Node child = root.getChild(i);
            if (child instanceof KtImportList) {
                aHeaderHasImport = headerHasImport(reqPackage, (KtImportList)child);
                break;
            }
        }
        return aHeaderHasImport;
    }

    private static boolean headerHasImport(final @NonNull String reqPackage, final @NonNull KtImportList node) {
        int numChildren = node.getNumChildren();
        for (int i = 0; i < numChildren; i++) {
            KotlinNode child = node.getChild(i);
            if (child instanceof KtImportHeader) {
                List<KtSimpleIdentifier> simpleIdentifiers = ((KtImportHeader) child).identifier().simpleIdentifier(); // multiple
                String importJoined = joinIdentifiersText(".", simpleIdentifiers);
                if (importJoined.contains(reqPackage)) return true;
            }
        }
        return false;
    }

    private static String joinIdentifiersText(String delimiter, List<KtSimpleIdentifier> simpleIdentifiers) {
        List<String> parts = new ArrayList<>();
        for (KtSimpleIdentifier smplIdentifier: simpleIdentifiers) {
            parts.add(smplIdentifier.Identifier().getText()); // In designer called T-Identifier
        }
        return String.join(delimiter, parts);
    }

    public static boolean hasChildren(final @NonNull String reqNumChildren, final @NonNull Node node) {
        return node.getNumChildren() == Integer.parseInt(reqNumChildren);
    }
}