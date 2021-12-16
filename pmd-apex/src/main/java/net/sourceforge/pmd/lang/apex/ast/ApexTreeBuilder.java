/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.apex.ast;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.pmd.lang.apex.multifile.ApexMultifileAnalysis;
import net.sourceforge.pmd.lang.ast.Parser.ParserTask;
import net.sourceforge.pmd.util.document.Chars;
import net.sourceforge.pmd.util.document.TextDocument;
import net.sourceforge.pmd.util.document.TextRegion;

import apex.jorje.data.Location;
import apex.jorje.data.Locations;
import apex.jorje.semantic.ast.AstNode;
import apex.jorje.semantic.ast.compilation.AnonymousClass;
import apex.jorje.semantic.ast.compilation.Compilation;
import apex.jorje.semantic.ast.compilation.ConstructorPreamble;
import apex.jorje.semantic.ast.compilation.InvalidDependentCompilation;
import apex.jorje.semantic.ast.compilation.UserClass;
import apex.jorje.semantic.ast.compilation.UserClassMethods;
import apex.jorje.semantic.ast.compilation.UserEnum;
import apex.jorje.semantic.ast.compilation.UserExceptionMethods;
import apex.jorje.semantic.ast.compilation.UserInterface;
import apex.jorje.semantic.ast.compilation.UserTrigger;
import apex.jorje.semantic.ast.condition.StandardCondition;
import apex.jorje.semantic.ast.expression.ArrayLoadExpression;
import apex.jorje.semantic.ast.expression.ArrayStoreExpression;
import apex.jorje.semantic.ast.expression.AssignmentExpression;
import apex.jorje.semantic.ast.expression.BinaryExpression;
import apex.jorje.semantic.ast.expression.BindExpressions;
import apex.jorje.semantic.ast.expression.BooleanExpression;
import apex.jorje.semantic.ast.expression.CastExpression;
import apex.jorje.semantic.ast.expression.ClassRefExpression;
import apex.jorje.semantic.ast.expression.EmptyReferenceExpression;
import apex.jorje.semantic.ast.expression.Expression;
import apex.jorje.semantic.ast.expression.IllegalStoreExpression;
import apex.jorje.semantic.ast.expression.InstanceOfExpression;
import apex.jorje.semantic.ast.expression.JavaMethodCallExpression;
import apex.jorje.semantic.ast.expression.JavaVariableExpression;
import apex.jorje.semantic.ast.expression.LiteralExpression;
import apex.jorje.semantic.ast.expression.MapEntryNode;
import apex.jorje.semantic.ast.expression.MethodCallExpression;
import apex.jorje.semantic.ast.expression.NestedExpression;
import apex.jorje.semantic.ast.expression.NestedStoreExpression;
import apex.jorje.semantic.ast.expression.NewKeyValueObjectExpression;
import apex.jorje.semantic.ast.expression.NewListInitExpression;
import apex.jorje.semantic.ast.expression.NewListLiteralExpression;
import apex.jorje.semantic.ast.expression.NewMapInitExpression;
import apex.jorje.semantic.ast.expression.NewMapLiteralExpression;
import apex.jorje.semantic.ast.expression.NewObjectExpression;
import apex.jorje.semantic.ast.expression.NewSetInitExpression;
import apex.jorje.semantic.ast.expression.NewSetLiteralExpression;
import apex.jorje.semantic.ast.expression.PackageVersionExpression;
import apex.jorje.semantic.ast.expression.PostfixExpression;
import apex.jorje.semantic.ast.expression.PrefixExpression;
import apex.jorje.semantic.ast.expression.ReferenceExpression;
import apex.jorje.semantic.ast.expression.SoqlExpression;
import apex.jorje.semantic.ast.expression.SoslExpression;
import apex.jorje.semantic.ast.expression.SuperMethodCallExpression;
import apex.jorje.semantic.ast.expression.SuperVariableExpression;
import apex.jorje.semantic.ast.expression.TernaryExpression;
import apex.jorje.semantic.ast.expression.ThisMethodCallExpression;
import apex.jorje.semantic.ast.expression.ThisVariableExpression;
import apex.jorje.semantic.ast.expression.TriggerVariableExpression;
import apex.jorje.semantic.ast.expression.VariableExpression;
import apex.jorje.semantic.ast.member.Field;
import apex.jorje.semantic.ast.member.Method;
import apex.jorje.semantic.ast.member.Parameter;
import apex.jorje.semantic.ast.member.Property;
import apex.jorje.semantic.ast.member.bridge.BridgeMethodCreator;
import apex.jorje.semantic.ast.modifier.Annotation;
import apex.jorje.semantic.ast.modifier.AnnotationParameter;
import apex.jorje.semantic.ast.modifier.Modifier;
import apex.jorje.semantic.ast.modifier.ModifierNode;
import apex.jorje.semantic.ast.modifier.ModifierOrAnnotation;
import apex.jorje.semantic.ast.statement.BlockStatement;
import apex.jorje.semantic.ast.statement.BreakStatement;
import apex.jorje.semantic.ast.statement.CatchBlockStatement;
import apex.jorje.semantic.ast.statement.ConstructorPreambleStatement;
import apex.jorje.semantic.ast.statement.ContinueStatement;
import apex.jorje.semantic.ast.statement.DmlDeleteStatement;
import apex.jorje.semantic.ast.statement.DmlInsertStatement;
import apex.jorje.semantic.ast.statement.DmlMergeStatement;
import apex.jorje.semantic.ast.statement.DmlUndeleteStatement;
import apex.jorje.semantic.ast.statement.DmlUpdateStatement;
import apex.jorje.semantic.ast.statement.DmlUpsertStatement;
import apex.jorje.semantic.ast.statement.DoLoopStatement;
import apex.jorje.semantic.ast.statement.ElseWhenBlock;
import apex.jorje.semantic.ast.statement.ExpressionStatement;
import apex.jorje.semantic.ast.statement.FieldDeclaration;
import apex.jorje.semantic.ast.statement.FieldDeclarationStatements;
import apex.jorje.semantic.ast.statement.ForEachStatement;
import apex.jorje.semantic.ast.statement.ForLoopStatement;
import apex.jorje.semantic.ast.statement.IfBlockStatement;
import apex.jorje.semantic.ast.statement.IfElseBlockStatement;
import apex.jorje.semantic.ast.statement.MethodBlockStatement;
import apex.jorje.semantic.ast.statement.MultiStatement;
import apex.jorje.semantic.ast.statement.ReturnStatement;
import apex.jorje.semantic.ast.statement.RunAsBlockStatement;
import apex.jorje.semantic.ast.statement.Statement;
import apex.jorje.semantic.ast.statement.StatementExecuted;
import apex.jorje.semantic.ast.statement.SwitchStatement;
import apex.jorje.semantic.ast.statement.ThrowStatement;
import apex.jorje.semantic.ast.statement.TryCatchFinallyBlockStatement;
import apex.jorje.semantic.ast.statement.TypeWhenBlock;
import apex.jorje.semantic.ast.statement.ValueWhenBlock;
import apex.jorje.semantic.ast.statement.VariableDeclaration;
import apex.jorje.semantic.ast.statement.VariableDeclarationStatements;
import apex.jorje.semantic.ast.statement.WhenCases.IdentifierCase;
import apex.jorje.semantic.ast.statement.WhenCases.LiteralCase;
import apex.jorje.semantic.ast.statement.WhileLoopStatement;
import apex.jorje.semantic.ast.visitor.AdditionalPassScope;
import apex.jorje.semantic.ast.visitor.AstVisitor;
import apex.jorje.semantic.exception.Errors;

final class ApexTreeBuilder extends AstVisitor<AdditionalPassScope> {

    private static final Pattern COMMENT_PATTERN =
        // we only need to check for \n as the input is normalized
        Pattern.compile("/\\*\\*([^*]++|\\*(?!/))*+\\*/|//[^\n]++\n");

    private static final Map<Class<? extends AstNode>, Constructor<? extends AbstractApexNode<?>>>
        NODE_TYPE_TO_NODE_ADAPTER_TYPE = new HashMap<>();


    static {
        register(Annotation.class, ASTAnnotation.class);
        register(AnnotationParameter.class, ASTAnnotationParameter.class);
        register(AnonymousClass.class, ASTAnonymousClass.class);
        register(ArrayLoadExpression.class, ASTArrayLoadExpression.class);
        register(ArrayStoreExpression.class, ASTArrayStoreExpression.class);
        register(AssignmentExpression.class, ASTAssignmentExpression.class);
        register(BinaryExpression.class, ASTBinaryExpression.class);
        register(BindExpressions.class, ASTBindExpressions.class);
        register(BlockStatement.class, ASTBlockStatement.class);
        register(BooleanExpression.class, ASTBooleanExpression.class);
        register(BreakStatement.class, ASTBreakStatement.class);
        register(BridgeMethodCreator.class, ASTBridgeMethodCreator.class);
        register(CastExpression.class, ASTCastExpression.class);
        register(CatchBlockStatement.class, ASTCatchBlockStatement.class);
        register(ClassRefExpression.class, ASTClassRefExpression.class);
        register(ConstructorPreamble.class, ASTConstructorPreamble.class);
        register(ConstructorPreambleStatement.class, ASTConstructorPreambleStatement.class);
        register(ContinueStatement.class, ASTContinueStatement.class);
        register(DmlDeleteStatement.class, ASTDmlDeleteStatement.class);
        register(DmlInsertStatement.class, ASTDmlInsertStatement.class);
        register(DmlMergeStatement.class, ASTDmlMergeStatement.class);
        register(DmlUndeleteStatement.class, ASTDmlUndeleteStatement.class);
        register(DmlUpdateStatement.class, ASTDmlUpdateStatement.class);
        register(DmlUpsertStatement.class, ASTDmlUpsertStatement.class);
        register(DoLoopStatement.class, ASTDoLoopStatement.class);
        register(ElseWhenBlock.class, ASTElseWhenBlock.class);
        register(EmptyReferenceExpression.class, ASTEmptyReferenceExpression.class);
        register(Expression.class, ASTExpression.class);
        register(ExpressionStatement.class, ASTExpressionStatement.class);
        register(Field.class, ASTField.class);
        register(FieldDeclaration.class, ASTFieldDeclaration.class);
        register(FieldDeclarationStatements.class, ASTFieldDeclarationStatements.class);
        register(ForEachStatement.class, ASTForEachStatement.class);
        register(ForLoopStatement.class, ASTForLoopStatement.class);
        register(IdentifierCase.class, ASTIdentifierCase.class);
        register(IfBlockStatement.class, ASTIfBlockStatement.class);
        register(IfElseBlockStatement.class, ASTIfElseBlockStatement.class);
        register(IllegalStoreExpression.class, ASTIllegalStoreExpression.class);
        register(InstanceOfExpression.class, ASTInstanceOfExpression.class);
        register(InvalidDependentCompilation.class, ASTInvalidDependentCompilation.class);
        register(JavaMethodCallExpression.class, ASTJavaMethodCallExpression.class);
        register(JavaVariableExpression.class, ASTJavaVariableExpression.class);
        register(LiteralCase.class, ASTLiteralCase.class);
        register(LiteralExpression.class, ASTLiteralExpression.class);
        register(MapEntryNode.class, ASTMapEntryNode.class);
        register(Method.class, ASTMethod.class);
        register(MethodBlockStatement.class, ASTMethodBlockStatement.class);
        register(MethodCallExpression.class, ASTMethodCallExpression.class);
        register(Modifier.class, ASTModifier.class);
        register(ModifierNode.class, ASTModifierNode.class);
        register(ModifierOrAnnotation.class, ASTModifierOrAnnotation.class);
        register(MultiStatement.class, ASTMultiStatement.class);
        register(NestedExpression.class, ASTNestedExpression.class);
        register(NestedStoreExpression.class, ASTNestedStoreExpression.class);
        register(NewKeyValueObjectExpression.class, ASTNewKeyValueObjectExpression.class);
        register(NewListInitExpression.class, ASTNewListInitExpression.class);
        register(NewListLiteralExpression.class, ASTNewListLiteralExpression.class);
        register(NewMapInitExpression.class, ASTNewMapInitExpression.class);
        register(NewMapLiteralExpression.class, ASTNewMapLiteralExpression.class);
        register(NewObjectExpression.class, ASTNewObjectExpression.class);
        register(NewSetInitExpression.class, ASTNewSetInitExpression.class);
        register(NewSetLiteralExpression.class, ASTNewSetLiteralExpression.class);
        register(PackageVersionExpression.class, ASTPackageVersionExpression.class);
        register(Parameter.class, ASTParameter.class);
        register(PostfixExpression.class, ASTPostfixExpression.class);
        register(PrefixExpression.class, ASTPrefixExpression.class);
        register(Property.class, ASTProperty.class);
        register(ReferenceExpression.class, ASTReferenceExpression.class);
        register(ReturnStatement.class, ASTReturnStatement.class);
        register(RunAsBlockStatement.class, ASTRunAsBlockStatement.class);
        register(SoqlExpression.class, ASTSoqlExpression.class);
        register(SoslExpression.class, ASTSoslExpression.class);
        register(StandardCondition.class, ASTStandardCondition.class);
        register(Statement.class, ASTStatement.class);
        register(StatementExecuted.class, ASTStatementExecuted.class);
        register(SuperMethodCallExpression.class, ASTSuperMethodCallExpression.class);
        register(SuperVariableExpression.class, ASTSuperVariableExpression.class);
        register(SwitchStatement.class, ASTSwitchStatement.class);
        register(TernaryExpression.class, ASTTernaryExpression.class);
        register(ThisMethodCallExpression.class, ASTThisMethodCallExpression.class);
        register(ThisVariableExpression.class, ASTThisVariableExpression.class);
        register(ThrowStatement.class, ASTThrowStatement.class);
        register(TriggerVariableExpression.class, ASTTriggerVariableExpression.class);
        register(TryCatchFinallyBlockStatement.class, ASTTryCatchFinallyBlockStatement.class);
        register(TypeWhenBlock.class, ASTTypeWhenBlock.class);
        register(UserClass.class, ASTUserClass.class);
        register(UserClassMethods.class, ASTUserClassMethods.class);
        register(UserExceptionMethods.class, ASTUserExceptionMethods.class);
        register(UserEnum.class, ASTUserEnum.class);
        register(UserInterface.class, ASTUserInterface.class);
        register(UserTrigger.class, ASTUserTrigger.class);
        register(ValueWhenBlock.class, ASTValueWhenBlock.class);
        register(VariableDeclaration.class, ASTVariableDeclaration.class);
        register(VariableDeclarationStatements.class, ASTVariableDeclarationStatements.class);
        register(VariableExpression.class, ASTVariableExpression.class);
        register(WhileLoopStatement.class, ASTWhileLoopStatement.class);
    }

    private static <T extends AstNode> void register(Class<T> nodeType, Class<? extends AbstractApexNode<T>> nodeAdapterType) {
        try {
            NODE_TYPE_TO_NODE_ADAPTER_TYPE.put(nodeType, nodeAdapterType.getDeclaredConstructor(nodeType));
        } catch (SecurityException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    // The nodes having children built.
    private final Stack<AbstractApexNode<?>> nodes = new Stack<>();

    // The Apex nodes with children to build.
    private final Stack<AstNode> parents = new Stack<>();

    private final AdditionalPassScope scope = new AdditionalPassScope(Errors.createErrors());

    private final TextDocument sourceCode;
    private final ParserTask task;
    private final CommentInformation commentInfo;

    ApexTreeBuilder(ParserTask task) {
        this.sourceCode = task.getTextDocument();
        this.task = task;
        commentInfo = extractInformationFromComments(sourceCode, task.getCommentMarker());
    }

    static <T extends AstNode> AbstractApexNode<T> createNodeAdapter(T node) {
        try {
            @SuppressWarnings("unchecked")
            // the register function makes sure only ApexNode<T> can be added,
            // where T is "T extends AstNode".
            Constructor<? extends AbstractApexNode<T>> constructor = (Constructor<? extends AbstractApexNode<T>>) NODE_TYPE_TO_NODE_ADAPTER_TYPE
                    .get(node.getClass());
            if (constructor == null) {
                throw new IllegalArgumentException(
                    "There is no Node adapter class registered for the Node class: " + node.getClass());
            }
            return constructor.newInstance(node);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getTargetException());
        }
    }

    ASTApexFile buildTree(Compilation astNode, ApexMultifileAnalysis analysisHandler) {
        assert nodes.isEmpty() : "stack should be empty";
        ASTApexFile root = new ASTApexFile(task, astNode, commentInfo.suppressMap, analysisHandler);
        nodes.push(root);
        parents.push(astNode);

        build(astNode);

        nodes.pop();
        parents.pop();

        addFormalComments();
        closeTree(root);
        return root;
    }

    private <T extends AstNode> void build(T astNode) {
        // Create a Node
        AbstractApexNode<T> node = createNodeAdapter(astNode);

        // Append to parent
        AbstractApexNode<?> parent = nodes.peek();
        parent.addChild(node, parent.getNumChildren());

        // Build the children...
        nodes.push(node);
        parents.push(astNode);
        astNode.traverse(this, scope);
        nodes.pop();
        parents.pop();


        if (nodes.isEmpty()) {
            // add the comments only at the end of the processing as the last step
            addFormalComments();
        }

        // If appropriate, determine whether this node contains comments or not
        if (node instanceof AbstractApexCommentContainerNode) {
            AbstractApexCommentContainerNode<?> commentContainer = (AbstractApexCommentContainerNode<?>) node;
            if (containsComments(commentContainer)) {
                commentContainer.setContainsComment(true);
            }
        }
    }

    private void closeTree(AbstractApexNode<?> node) {
        node.closeNode(sourceCode);
        for (ApexNode<?> child : node.children()) {
            closeTree((AbstractApexNode<?>) child);
        }
    }

    private boolean containsComments(ASTCommentContainer<?> commentContainer) {
        Location loc = commentContainer.getNode().getLoc();
        if (!Locations.isReal(loc)) {
            // Synthetic nodes don't have a location and can't have comments
            return false;
        }

        List<TokenLocation> allComments = commentInfo.allCommentTokens;
        // find the first comment after the start of the container node
        int index = Collections.binarySearch(commentInfo.allCommentTokensByStartIndex, loc.getStartIndex());

        // no exact hit found - this is expected: there is no comment token starting at the very same index as the node
        assert index < 0 : "comment token is at the same position as non-comment token";
        // extract "insertion point"
        index = ~index;

        // now check whether the next comment after the node is still inside the node
        return index >= 0 && index < allComments.size()
            && loc.getStartIndex() < allComments.get(index).index
            && loc.getEndIndex() > allComments.get(index).index;
    }

    private void addFormalComments() {
        for (ApexDocTokenLocation tokenLocation : commentInfo.docTokenLocations) {
            AbstractApexNode<?> parent = tokenLocation.nearestNode;
            if (parent != null) {
                parent.insertChild(new ASTFormalComment(tokenLocation.region, tokenLocation.image), 0);
            }
        }
    }

    private void buildFormalComment(AstNode node) {
        if (node.equals(parents.peek())) {
            assignApexDocTokenToNode(node, nodes.peek());
        }
    }

    /**
     * Only remembers the node, to which the comment could belong.
     * Since the visiting order of the nodes does not match the source order,
     * the nodes appearing later in the source might be visiting first.
     * The correct node will then be visited afterwards, and since the distance
     * to the comment is smaller, it overrides the remembered node.
     *
     * @param jorjeNode the original node
     * @param node the potential parent node, to which the comment could belong
     */
    private void assignApexDocTokenToNode(AstNode jorjeNode, AbstractApexNode<?> node) {
        Location loc = jorjeNode.getLoc();
        if (!Locations.isReal(loc)) {
            // Synthetic nodes such as "<clinit>" don't have a location in the
            // source code, since they are generated by the compiler
            return;
        }
        // find the token, that appears as close as possible before the node
        TextRegion nodeRegion = node.getRegion();
        for (ApexDocTokenLocation comment : commentInfo.docTokenLocations) {
            if (comment.region.compareTo(nodeRegion) > 0) {
                // this and all remaining tokens are after the node
                // so no need to check the remaining tokens.
                break;
            }

            int distance = nodeRegion.getStartOffset() - comment.region.getStartOffset();
            if (comment.nearestNode == null || distance < comment.nearestNodeDistance) {
                comment.nearestNode = node;
                comment.nearestNodeDistance = distance;
            }
        }
    }

    private static CommentInformation extractInformationFromComments(TextDocument source, String suppressMarker) {
        Chars text = source.getText();

        boolean checkForCommentSuppression = suppressMarker != null;
        ArrayList<TokenLocation> allCommentTokens = new ArrayList<>();
        List<ApexDocTokenLocation> tokenLocations = new ArrayList<>();
        Map<Integer, String> suppressMap = new HashMap<>();


        int index = 0;
        Matcher matcher = COMMENT_PATTERN.matcher(text);
        while (matcher.find()) {
            int startIdx = matcher.start();
            int endIdx = matcher.end();
            Chars commentText = text.subSequence(startIdx, endIdx);
            TextRegion commentRegion = TextRegion.fromBothOffsets(startIdx, endIdx);

            final TokenLocation tok;
            if (commentText.startsWith("/**")) {
                ApexDocTokenLocation doctok = new ApexDocTokenLocation(index, commentRegion, commentText);
                tokenLocations.add(doctok);
                tok = doctok;
            } else {
                tok = new TokenLocation(index, commentRegion);
            }
            index++;
            assert tok.index == allCommentTokens.size();
            allCommentTokens.add(tok);

            if (checkForCommentSuppression && commentText.startsWith("//")) {
                Chars trimmed = commentText.subSequence("//".length(), commentText.length()).trimStart();
                if (trimmed.startsWith(suppressMarker)) {
                    Chars userMessage = trimmed.subSequence(suppressMarker.length(), trimmed.length()).trim();
                    suppressMap.put(source.lineNumberAt(startIdx), userMessage.toString());
                }
            }
        }
        return new CommentInformation(suppressMap, allCommentTokens, tokenLocations);
    }

    private static class CommentInformation {

        final Map<Integer, String> suppressMap;
        final List<TokenLocation> allCommentTokens;
        final TokenListByStartIndex allCommentTokensByStartIndex;
        final List<ApexDocTokenLocation> docTokenLocations;

        <T extends List<TokenLocation> & RandomAccess>
        CommentInformation(Map<Integer, String> suppressMap, T allCommentTokens, List<ApexDocTokenLocation> docTokenLocations) {
            this.suppressMap = suppressMap;
            this.allCommentTokens = allCommentTokens;
            this.docTokenLocations = docTokenLocations;
            this.allCommentTokensByStartIndex = new TokenListByStartIndex(allCommentTokens);
        }
    }

    /**
     * List that maps comment tokens to their start index without copy.
     * This is used to implement a "binary search by key" routine which unfortunately isn't in the stdlib.
     *
     * <p>
     * Note that the provided token list must implement {@link RandomAccess}.
     */
    private static final class TokenListByStartIndex extends AbstractList<Integer> implements RandomAccess {

        private final List<TokenLocation> tokens;

        <T extends List<TokenLocation> & RandomAccess> TokenListByStartIndex(T tokens) {
            this.tokens = tokens;
        }

        @Override
        public Integer get(int index) {
            return tokens.get(index).index;
        }

        @Override
        public int size() {
            return tokens.size();
        }
    }

    private static class TokenLocation {

        final TextRegion region;
        final int index;

        TokenLocation(int index, TextRegion region) {
            this.index = index;
            this.region = region;
        }
    }

    private static class ApexDocTokenLocation extends TokenLocation {

        private final Chars image;

        private AbstractApexNode<?> nearestNode;
        private int nearestNodeDistance;

        ApexDocTokenLocation(int index, TextRegion commentRegion, Chars image) {
            super(index, commentRegion);
            this.image = image;
        }
    }

    private boolean visit(AstNode node) {
        if (node.equals(parents.peek())) {
            return true;
        } else {
            build(node);
            return false;
        }
    }

    public Map<Integer, String> getSuppressMap() {
        return commentInfo.suppressMap;
    }

    @Override
    public boolean visit(UserEnum node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(UserInterface node, AdditionalPassScope scope) {
        final boolean ret = visit(node);
        buildFormalComment(node);
        return ret;
    }

    @Override
    public boolean visit(UserTrigger node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(ArrayLoadExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(ArrayStoreExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(AssignmentExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(BinaryExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(BooleanExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(ClassRefExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(InstanceOfExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(JavaMethodCallExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(JavaVariableExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(LiteralExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(ReferenceExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(MethodCallExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(NewListInitExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(NewMapInitExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(NewSetInitExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(NewListLiteralExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(NewObjectExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(NewSetLiteralExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(PackageVersionExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(PostfixExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(PrefixExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(TernaryExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(StandardCondition node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(TriggerVariableExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(VariableExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(BlockStatement node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(BreakStatement node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(ContinueStatement node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(DmlDeleteStatement node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(DmlInsertStatement node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(DmlMergeStatement node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(DmlUndeleteStatement node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(DmlUpdateStatement node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(DmlUpsertStatement node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(DoLoopStatement node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(ExpressionStatement node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(ForEachStatement node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(ForLoopStatement node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(FieldDeclaration node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(FieldDeclarationStatements node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(IfBlockStatement node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(IfElseBlockStatement node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(ReturnStatement node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(RunAsBlockStatement node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(ThrowStatement node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(VariableDeclaration node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(VariableDeclarationStatements node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(WhileLoopStatement node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(BindExpressions node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(SoqlExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(SoslExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(NewMapLiteralExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(MapEntryNode node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(CatchBlockStatement node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(TryCatchFinallyBlockStatement node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(Property node, AdditionalPassScope scope) {
        final boolean ret = visit(node);
        buildFormalComment(node);
        return ret;
    }

    @Override
    public boolean visit(Field node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(Parameter node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(BridgeMethodCreator node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(UserClassMethods node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(UserExceptionMethods node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(Annotation node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(AnnotationParameter node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(ModifierNode node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(SuperMethodCallExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(ThisMethodCallExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(SuperVariableExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(ThisVariableExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(UserClass node, AdditionalPassScope scope) {
        final boolean ret = visit(node);
        buildFormalComment(node);
        return ret;
    }

    @Override
    public boolean visit(Method node, AdditionalPassScope scope) {
        final boolean ret = visit(node);
        buildFormalComment(node);
        return ret;
    }

    @Override
    public boolean visit(AnonymousClass node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(CastExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(NewKeyValueObjectExpression node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(SwitchStatement node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(ElseWhenBlock node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(TypeWhenBlock node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(ValueWhenBlock node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(LiteralCase node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(IdentifierCase node, AdditionalPassScope scope) {
        return visit(node);
    }

    @Override
    public boolean visit(EmptyReferenceExpression node, AdditionalPassScope scope) {
        return visit(node);
    }
}
