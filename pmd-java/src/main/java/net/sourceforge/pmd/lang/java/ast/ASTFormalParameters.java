/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.ast;

import java.util.Iterator;
import java.util.List;

import net.sourceforge.pmd.annotation.InternalApi;


public final class ASTFormalParameters extends AbstractJavaNode implements Iterable<ASTFormalParameter> {

    @InternalApi
    @Deprecated
    public ASTFormalParameters(int id) {
        super(id);
    }

    ASTFormalParameters(JavaParser p, int id) {
        super(p, id);
    }

    public int getParameterCount() {
        final List<ASTFormalParameter> parameters = findChildrenOfType(ASTFormalParameter.class);
        return !parameters.isEmpty() && parameters.get(0).isExplicitReceiverParameter()
               ? parameters.size() - 1 : parameters.size();
    }

    @Override
    public Object jjtAccept(JavaParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }


    @Override
    public <T> void jjtAccept(SideEffectingVisitor<T> visitor, T data) {
        visitor.visit(this, data);
    }


    @Override
    public ASTFormalParameter jjtGetChild(int index) {
        return (ASTFormalParameter) super.jjtGetChild(index);
    }


    @Override
    public Iterator<ASTFormalParameter> iterator() {
        return new NodeChildrenIterator<>(this, ASTFormalParameter.class);
    }
}
