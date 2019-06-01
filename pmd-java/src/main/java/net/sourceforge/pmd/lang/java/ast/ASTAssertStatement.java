/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.ast;

/**
 * Represents an {@code assert} statement.
 *
 * <pre class="grammar">
 *
 * AssertStatement ::= "assert" {@linkplain ASTExpression Expression} ( ":" {@linkplain ASTExpression Expression} )? ";"
 *
 * </pre>
 */
public final class ASTAssertStatement extends AbstractJavaNode {

    ASTAssertStatement(int id) {
        super(id);
    }

    ASTAssertStatement(JavaParser p, int id) {
        super(p, id);
    }

    @Override
    public Object jjtAccept(JavaParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }


    @Override
    public <T> void jjtAccept(SideEffectingVisitor<T> visitor, T data) {
        visitor.visit(this, data);
    }


    /**
     * Returns the expression tested by this assert statement.
     */
    public ASTExpression getGuardExpressionNode() {
        return (ASTExpression) jjtGetChild(0);
    }


    /**
     * Returns true if this assert statement has a "detail message"
     * expression. In that case, {@link #getDetailMessageNode()} doesn't
     * return null.
     */
    public boolean hasDetailMessage() {
        return jjtGetNumChildren() == 2;
    }


    /**
     * Returns the expression that corresponds to the detail message,
     * i.e. the expression after the colon, if it's present.
     */
    public ASTExpression getDetailMessageNode() {
        return hasDetailMessage() ? (ASTExpression) jjtGetChild(1) : null;
    }

}
