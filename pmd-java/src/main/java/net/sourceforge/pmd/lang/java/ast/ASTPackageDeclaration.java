/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.ast;

import net.sourceforge.pmd.annotation.DeprecatedUntil700;
import net.sourceforge.pmd.lang.rule.xpath.DeprecatedAttribute;

/**
 * Package declaration at the top of a {@linkplain ASTCompilationUnit source file}.
 * Since 7.0, there is no {@linkplain ASTName Name} node anymore. Use
 * {@link #getPackageNameImage()} instead.
 *
 *
 * <pre class="grammar">
 *
 * PackageDeclaration ::= {@link ASTModifierList AnnotationList} "package" Name ";"
 *
 * </pre>
 *
 */
public final class ASTPackageDeclaration extends AbstractJavaNode implements Annotatable, ASTTopLevelDeclaration, JavadocCommentOwner {

    ASTPackageDeclaration(int id) {
        super(id);
    }


    @Override
    protected <P, R> R acceptVisitor(JavaVisitor<? super P, ? extends R> visitor, P data) {
        return visitor.visit(this, data);
    }

    /**
     * Returns the name of the package.
     *
     * @since 4.2
     * @deprecated Use {@link #getName()}
     */
    @Deprecated
    @DeprecatedUntil700
    @DeprecatedAttribute(replaceWith = "@Name")
    public String getPackageNameImage() {
        return super.getImage();
    }


    /**
     * Returns the name of the package.
     *
     * @since 7.0.0
     */
    public String getName() {
        return getPackageNameImage();
    }

    @Override
    public String getImage() {
        // the image was null before 7.0, best keep it that way
        return null;
    }
}
