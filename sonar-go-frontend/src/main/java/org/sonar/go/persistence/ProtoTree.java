/*
 * SonarSource Go
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.go.persistence;

import com.google.protobuf.CodedInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.go.impl.ArrayTypeTreeImpl;
import org.sonar.go.impl.AssignmentExpressionTreeImpl;
import org.sonar.go.impl.BinaryExpressionTreeImpl;
import org.sonar.go.impl.BlockTreeImpl;
import org.sonar.go.impl.CatchTreeImpl;
import org.sonar.go.impl.ClassDeclarationTreeImpl;
import org.sonar.go.impl.CommentImpl;
import org.sonar.go.impl.CompositeLiteralTreeImpl;
import org.sonar.go.impl.EllipsisTreeImpl;
import org.sonar.go.impl.ExceptionHandlingTreeImpl;
import org.sonar.go.impl.ExpressionStatementTreeImpl;
import org.sonar.go.impl.FloatLiteralTreeImpl;
import org.sonar.go.impl.FunctionDeclarationTreeImpl;
import org.sonar.go.impl.FunctionInvocationTreeImpl;
import org.sonar.go.impl.GoStatementTreeImpl;
import org.sonar.go.impl.IdentifierTreeImpl;
import org.sonar.go.impl.IfTreeImpl;
import org.sonar.go.impl.ImaginaryLiteralTreeImpl;
import org.sonar.go.impl.ImportDeclarationTreeImpl;
import org.sonar.go.impl.ImportSpecificationTreeImpl;
import org.sonar.go.impl.IndexExpressionTreeImpl;
import org.sonar.go.impl.IndexListExpressionTreeImpl;
import org.sonar.go.impl.IntegerLiteralTreeImpl;
import org.sonar.go.impl.JumpTreeImpl;
import org.sonar.go.impl.KeyValueTreeImpl;
import org.sonar.go.impl.LeftRightHandSideTreeImpl;
import org.sonar.go.impl.LiteralTreeImpl;
import org.sonar.go.impl.LoopTreeImpl;
import org.sonar.go.impl.MapTypeTreeImpl;
import org.sonar.go.impl.MatchCaseTreeImpl;
import org.sonar.go.impl.MatchTreeImpl;
import org.sonar.go.impl.MemberSelectTreeImpl;
import org.sonar.go.impl.ModifierTreeImpl;
import org.sonar.go.impl.NativeTreeImpl;
import org.sonar.go.impl.PackageDeclarationTreeImpl;
import org.sonar.go.impl.ParameterTreeImpl;
import org.sonar.go.impl.ParenthesizedExpressionTreeImpl;
import org.sonar.go.impl.PlaceHolderTreeImpl;
import org.sonar.go.impl.ReturnTreeImpl;
import org.sonar.go.impl.SliceTreeImpl;
import org.sonar.go.impl.StarExpressionTreeImpl;
import org.sonar.go.impl.StringLiteralTreeImpl;
import org.sonar.go.impl.ThrowTreeImpl;
import org.sonar.go.impl.TokenImpl;
import org.sonar.go.impl.TopLevelTreeImpl;
import org.sonar.go.impl.TreeMetaDataProvider;
import org.sonar.go.impl.TypeAssertionExpressionTreeImpl;
import org.sonar.go.impl.TypeImpl;
import org.sonar.go.impl.UnaryExpressionTreeImpl;
import org.sonar.go.impl.VariableDeclarationTreeImpl;
import org.sonar.go.impl.cfg.BlockImpl;
import org.sonar.go.impl.cfg.ControlFlowGraphImpl;
import org.sonar.go.persistence.conversion.RangeConverter;
import org.sonar.go.persistence.conversion.StringNativeKind;
import org.sonar.plugins.go.api.AssignmentExpressionTree;
import org.sonar.plugins.go.api.BinaryExpressionTree;
import org.sonar.plugins.go.api.BlockTree;
import org.sonar.plugins.go.api.CatchTree;
import org.sonar.plugins.go.api.Comment;
import org.sonar.plugins.go.api.FunctionInvocationTree;
import org.sonar.plugins.go.api.IdentifierTree;
import org.sonar.plugins.go.api.JumpTree;
import org.sonar.plugins.go.api.LoopTree;
import org.sonar.plugins.go.api.MatchCaseTree;
import org.sonar.plugins.go.api.ModifierTree;
import org.sonar.plugins.go.api.StringLiteralTree;
import org.sonar.plugins.go.api.Token;
import org.sonar.plugins.go.api.Tree;
import org.sonar.plugins.go.api.TreeMetaData;
import org.sonar.plugins.go.api.TreeOrError;
import org.sonar.plugins.go.api.Type;
import org.sonar.plugins.go.api.UnaryExpressionTree;
import org.sonar.plugins.go.api.cfg.Block;
import org.sonar.plugins.go.api.cfg.ControlFlowGraph;
import org.sonarsource.go.proto.SlangProto;
import org.sonarsource.go.proto.SlangProto.Node;

/**
 * Rebuilds the {@code org.sonar.plugins.go.api} trees from the protobuf output of sonar-go-to-slang,
 * whose schema is {@code sonar-go-to-slang/proto/slang/slang.proto} and whose encoder is
 * {@code protoSlang.go}.
 */
public final class ProtoTree {

  private static final Logger LOG = LoggerFactory.getLogger(ProtoTree.class);

  /**
   * Real Go files nest deeper than the 100 levels protobuf allows by default, as a node and its kind
   * are two levels of message for one level of AST. The limit is only there to stop a crafted input
   * from overflowing the stack, and the trees that reach this one fail the file rather than the batch.
   * A tree deep enough to exhaust the stack before reaching it fails the same way, which is why the
   * two steps below catch {@link StackOverflowError} as well.
   */
  private static final int RECURSION_LIMIT = 2000;

  private static final int FRAME_LENGTH_BYTES = 4;

  private ProtoTree() {
  }

  /**
   * Reads the files of a batch, each framed as {@code [4 bytes LE length][File]} by {@code
   * toSlangProto}. A file the analyzer cannot rebuild is returned as an error for its own name, so that
   * the other files of the batch are still analyzed.
   *
   * @throws IllegalStateException when the framing itself is broken, which no single file can cause
   */
  public static Map<String, TreeOrError> fromProto(byte[] bytes) {
    Map<String, TreeOrError> trees = new HashMap<>();
    int offset = 0;
    while (offset < bytes.length) {
      int length = frameLength(bytes, offset);
      offset += FRAME_LENGTH_BYTES;
      readFile(bytes, offset, length, trees);
      offset += length;
    }
    return trees;
  }

  private static int frameLength(byte[] bytes, int offset) {
    if (offset + FRAME_LENGTH_BYTES > bytes.length) {
      throw new IllegalStateException("Truncated response from the Go executable: no frame length at offset " + offset);
    }
    int length = ByteBuffer.wrap(bytes, offset, FRAME_LENGTH_BYTES).order(ByteOrder.LITTLE_ENDIAN).getInt();
    // Written as a subtraction, which cannot overflow here, rather than as the addition of a length
    // that a corrupted response could make large enough to wrap around.
    if (length < 0 || length > bytes.length - offset - FRAME_LENGTH_BYTES) {
      throw new IllegalStateException("Invalid frame length " + length + " at offset " + offset + " of the response from the Go executable");
    }
    return length;
  }

  /**
   * Reads one framed {@link SlangProto.File} and puts its tree under its name. The message is merged
   * into a builder rather than parsed in one go because the name is its first field.
   */
  private static void readFile(byte[] bytes, int offset, int length, Map<String, TreeOrError> trees) {
    var file = SlangProto.File.newBuilder();
    var codedInput = CodedInputStream.newInstance(bytes, offset, length);
    codedInput.setRecursionLimit(RECURSION_LIMIT);
    try {
      file.mergeFrom(codedInput);
    } catch (IOException | StackOverflowError e) {
      trees.put(file.getName(), TreeOrError.of("Error reading proto tree: " + e.getMessage()));
      return;
    }
    trees.put(file.getName(), fromProtoTree(file.getTree()));
  }

  private static TreeOrError fromProtoTree(SlangProto.Tree protoTree) {
    if (protoTree.hasError()) {
      return TreeOrError.of(protoTree.getError());
    }

    try {
      return TreeOrError.of(new TreeBuilder(metaDataProvider(protoTree)).tree(protoTree.getRoot()));
    } catch (RuntimeException | StackOverflowError e) {
      return TreeOrError.of("Error converting proto tree: " + e.getMessage());
    }
  }

  private static TreeMetaDataProvider metaDataProvider(SlangProto.Tree protoTree) {
    List<Comment> comments = protoTree.getCommentsList().stream()
      .<Comment>map(comment -> new CommentImpl(comment.getText(), comment.getContentText(),
        RangeConverter.parse(comment.getRange()), RangeConverter.parse(comment.getContentRange())))
      .toList();
    List<Token> tokens = protoTree.getTokensList().stream()
      .<Token>map(token -> new TokenImpl(RangeConverter.parse(token.getTextRange()), token.getText(),
        token.getType().isEmpty() ? Token.Type.OTHER : Token.Type.valueOf(token.getType())))
      .toList();
    return new TreeMetaDataProvider(comments, tokens);
  }

  /**
   * Builds the trees of one file. It is stateful: the nodes of a control flow graph are looked up in
   * the index it fills as it goes, so it cannot be shared between files.
   */
  private static final class TreeBuilder {

    private final TreeMetaDataProvider provider;

    /**
     * The trees built so far by their {@code cfg_id}. The Go side restarts the ids at every top-level
     * function, so an entry only stands until the next function overwrites it, which is enough: a
     * graph is resolved right after the function it belongs to has been built, and refers to no other.
     */
    private final Map<Integer, Tree> treesByCfgId = new HashMap<>();

    private TreeBuilder(TreeMetaDataProvider provider) {
      this.provider = provider;
    }

    private Tree tree(Node node) {
      var metaData = RangeConverter.resolveMetaData(provider, node.getTextRange());
      Tree tree = switch (node.getKindCase()) {
        case IDENTIFIER -> identifier(metaData, node.getIdentifier());
        case NATIVE -> nativeTree(metaData, node.getNative());
        case INTEGER_LITERAL -> new IntegerLiteralTreeImpl(metaData, node.getIntegerLiteral().getValue());
        case VARIABLE_DECLARATION -> variableDeclaration(metaData, node.getVariableDeclaration());
        case STRING_LITERAL -> stringLiteral(metaData, node.getStringLiteral());
        case BLOCK -> new BlockTreeImpl(metaData, trees(node.getBlock().getStatementOrExpressionsList()));
        case FUNCTION_INVOCATION -> functionInvocation(metaData, node.getFunctionInvocation());
        case FUNCTION_DECLARATION -> functionDeclaration(metaData, node.getFunctionDeclaration());
        case MEMBER_SELECT -> memberSelect(metaData, node.getMemberSelect());
        case EXPRESSION_STATEMENT -> new ExpressionStatementTreeImpl(metaData, tree(node.getExpressionStatement().getExpression()));
        case BINARY_EXPRESSION -> binaryExpression(metaData, node.getBinaryExpression());
        case PARAMETER -> parameter(metaData, node.getParameter());
        case ASSIGNMENT_EXPRESSION -> assignmentExpression(metaData, node.getAssignmentExpression());
        case ARRAY_TYPE -> arrayType(metaData, node.getArrayType());
        case TOP_LEVEL -> topLevel(metaData, node.getTopLevel());
        case PACKAGE_DECLARATION -> new PackageDeclarationTreeImpl(metaData, trees(node.getPackageDeclaration().getChildrenList()));
        case COMPOSITE_LITERAL -> compositeLiteral(metaData, node.getCompositeLiteral());
        case CLASS_DECLARATION -> classDeclaration(metaData, node.getClassDeclaration());
        case PLACE_HOLDER -> new PlaceHolderTreeImpl(metaData, token(node.getPlaceHolder().getPlaceHolderToken()));
        case INDEX_EXPRESSION -> indexExpression(metaData, node.getIndexExpression());
        case IMPORT_SPECIFICATION -> importSpecification(metaData, node.getImportSpecification());
        case KEY_VALUE -> keyValue(metaData, node.getKeyValue());
        case RETURN_STATEMENT -> returnStatement(metaData, node.getReturnStatement());
        case STAR_EXPRESSION -> new StarExpressionTreeImpl(metaData, tree(node.getStarExpression().getExpression()));
        case LITERAL -> new LiteralTreeImpl(metaData, node.getLiteral().getValue());
        case IMPORT_DECLARATION -> new ImportDeclarationTreeImpl(metaData, trees(node.getImportDeclaration().getChildrenList()));
        case UNARY_EXPRESSION -> unaryExpression(metaData, node.getUnaryExpression());
        case LOOP -> loop(metaData, node.getLoop());
        case MAP_TYPE -> mapType(metaData, node.getMapType());
        case MATCH_CASE -> matchCase(metaData, node.getMatchCase());
        case PARENTHESIZED_EXPRESSION -> parenthesizedExpression(metaData, node.getParenthesizedExpression());
        case LEFT_RIGHT_HAND_SIDE -> new LeftRightHandSideTreeImpl(metaData, trees(node.getLeftRightHandSide().getChildrenList()));
        case FLOAT_LITERAL -> new FloatLiteralTreeImpl(metaData, node.getFloatLiteral().getValue());
        case IF_STATEMENT -> ifStatement(metaData, node.getIfStatement());
        case MATCH -> match(metaData, node.getMatch());
        case JUMP -> jump(metaData, node.getJump());
        case SLICE -> slice(metaData, node.getSlice());
        case INDEX_LIST_EXPRESSION -> indexListExpression(metaData, node.getIndexListExpression());
        case GO_STATEMENT -> goStatement(metaData, node.getGoStatement());
        case TYPE_ASSERTION_EXPRESSION -> typeAssertionExpression(metaData, node.getTypeAssertionExpression());
        case ELLIPSIS -> ellipsis(metaData, node.getEllipsis());
        case IMAGINARY_LITERAL -> new ImaginaryLiteralTreeImpl(metaData, node.getImaginaryLiteral().getValue());
        case CATCH_CLAUSE -> catchClause(metaData, node.getCatchClause());
        case EXCEPTION_HANDLING -> exceptionHandling(metaData, node.getExceptionHandling());
        case MODIFIER -> new ModifierTreeImpl(metaData, ModifierTree.Kind.valueOf(node.getModifier().getKind()));
        case THROW_STATEMENT -> throwStatement(metaData, node.getThrowStatement());
        case KIND_NOT_SET -> throw new IllegalStateException("Node of no kind at " + node.getTextRange());
      };
      if (node.getCfgId() > 0) {
        treesByCfgId.put(node.getCfgId(), tree);
      }
      return tree;
    }

    private List<Tree> trees(List<Node> nodes) {
      return nodes.stream().map(this::tree).toList();
    }

    /**
     * The elements of a list are all of the given class, which the schema cannot express but the Go
     * side guarantees; a node of another class fails the file with a {@link ClassCastException}.
     */
    private <T extends Tree> List<T> trees(List<Node> nodes, Class<T> elementClass) {
      return nodes.stream().map(node -> elementClass.cast(tree(node))).toList();
    }

    @Nullable
    private Tree nullableTree(boolean present, Node node) {
      return present ? tree(node) : null;
    }

    @Nullable
    private <T extends Tree> T nullableTree(boolean present, Node node, Class<T> treeClass) {
      return present ? treeClass.cast(tree(node)) : null;
    }

    private Token token(String textRange) {
      return RangeConverter.resolveToken(provider, textRange);
    }

    @Nullable
    private Token nullableToken(String textRange) {
      return textRange.isEmpty() ? null : token(textRange);
    }

    private static Tree identifier(TreeMetaData metaData, SlangProto.Identifier identifier) {
      return new IdentifierTreeImpl(metaData, identifier.getName(), identifier.getType(), identifier.getPackage(), identifier.getId());
    }

    private Tree nativeTree(TreeMetaData metaData, SlangProto.Native nativeNode) {
      return new NativeTreeImpl(metaData, StringNativeKind.of(nativeNode.getNativeKind()), trees(nativeNode.getChildrenList()));
    }

    private Tree variableDeclaration(TreeMetaData metaData, SlangProto.VariableDeclaration variableDeclaration) {
      return new VariableDeclarationTreeImpl(
        metaData,
        trees(variableDeclaration.getIdentifiersList(), IdentifierTree.class),
        nullableTree(variableDeclaration.hasType(), variableDeclaration.getType()),
        trees(variableDeclaration.getInitializersList()),
        variableDeclaration.getIsVal());
    }

    private static Tree stringLiteral(TreeMetaData metaData, SlangProto.StringLiteral stringLiteral) {
      return new StringLiteralTreeImpl(metaData, stringLiteral.getValue(), stringLiteral.getContent());
    }

    private Tree functionInvocation(TreeMetaData metaData, SlangProto.FunctionInvocation functionInvocation) {
      List<Type> returnTypes = functionInvocation.getReturnTypeList().stream()
        .<Type>map(TypeImpl::createFromType)
        .toList();
      return new FunctionInvocationTreeImpl(metaData, tree(functionInvocation.getMemberSelect()),
        trees(functionInvocation.getArgumentsList()), returnTypes);
    }

    private Tree functionDeclaration(TreeMetaData metaData, SlangProto.FunctionDeclaration function) {
      var returnType = nullableTree(function.hasReturnType(), function.getReturnType());
      var receiver = nullableTree(function.hasReceiver(), function.getReceiver());
      var name = nullableTree(function.hasName(), function.getName(), IdentifierTree.class);
      var formalParameters = trees(function.getFormalParametersList());
      var typeParameters = nullableTree(function.hasTypeParameters(), function.getTypeParameters());
      var body = nullableTree(function.hasBody(), function.getBody(), BlockTree.class);
      // Every child has to be built before the graph is resolved, as the graph refers to them by the
      // ids they registered while being built.
      var cfg = function.hasCfg() ? controlFlowGraph(function.getCfg()) : null;
      return new FunctionDeclarationTreeImpl(metaData, returnType, receiver, name, formalParameters, typeParameters, body, cfg);
    }

    @CheckForNull
    private ControlFlowGraph controlFlowGraph(SlangProto.Cfg cfg) {
      try {
        var protoBlocks = cfg.getBlocksList();
        if (protoBlocks.isEmpty()) {
          return null;
        }
        List<BlockImpl> blocks = new ArrayList<>(protoBlocks.size());
        for (var protoBlock : protoBlocks) {
          List<Tree> nodes = new ArrayList<>(protoBlock.getNodeIdsCount());
          for (int nodeId : protoBlock.getNodeIdsList()) {
            // A node of the graph that no tree was built for, which the id 0 stands for, is left out.
            var resolved = treesByCfgId.get(nodeId);
            if (resolved != null) {
              nodes.add(resolved);
            }
          }
          blocks.add(new BlockImpl(nodes));
        }
        for (int i = 0; i < protoBlocks.size(); i++) {
          // An index that names no block of this graph means the graph itself is broken: get throws,
          // and the catch below drops it rather than handing the checks one with edges silently gone,
          // which would make them reason about paths that do not exist.
          blocks.get(i).setSuccessors(protoBlocks.get(i).getSuccessorsList().stream()
            .<Block>map(blocks::get)
            .toList());
        }
        return new ControlFlowGraphImpl(List.<Block>copyOf(blocks));
      } catch (RuntimeException e) {
        // Most of the analysis does not need a graph, so a broken one must not fail the whole file.
        LOG.warn("Error while transferring a CFG.", e);
        return null;
      }
    }

    private Tree memberSelect(TreeMetaData metaData, SlangProto.MemberSelect memberSelect) {
      return new MemberSelectTreeImpl(metaData, tree(memberSelect.getExpression()),
        (IdentifierTree) tree(memberSelect.getIdentifier()));
    }

    private Tree binaryExpression(TreeMetaData metaData, SlangProto.BinaryExpression binaryExpression) {
      return new BinaryExpressionTreeImpl(
        metaData,
        BinaryExpressionTree.Operator.valueOf(binaryExpression.getOperator()),
        token(binaryExpression.getOperatorToken()),
        tree(binaryExpression.getLeftOperand()),
        tree(binaryExpression.getRightOperand()));
    }

    private Tree parameter(TreeMetaData metaData, SlangProto.Parameter parameter) {
      return new ParameterTreeImpl(metaData, (IdentifierTree) tree(parameter.getIdentifier()),
        nullableTree(parameter.hasType(), parameter.getType()));
    }

    private Tree assignmentExpression(TreeMetaData metaData, SlangProto.AssignmentExpression assignment) {
      return new AssignmentExpressionTreeImpl(
        metaData,
        AssignmentExpressionTree.Operator.valueOf(assignment.getOperator()),
        tree(assignment.getLeftHandSide()),
        tree(assignment.getStatementOrExpression()));
    }

    private Tree arrayType(TreeMetaData metaData, SlangProto.ArrayType arrayType) {
      return new ArrayTypeTreeImpl(metaData, nullableTree(arrayType.hasLength(), arrayType.getLength()), tree(arrayType.getElement()));
    }

    private Tree topLevel(TreeMetaData metaData, SlangProto.TopLevel topLevel) {
      var declarations = trees(topLevel.getDeclarationsList());
      var firstCpdToken = nullableToken(topLevel.getFirstCpdToken());
      return new TopLevelTreeImpl(metaData, declarations, metaData.commentsInside(), firstCpdToken);
    }

    private Tree compositeLiteral(TreeMetaData metaData, SlangProto.CompositeLiteral compositeLiteral) {
      return new CompositeLiteralTreeImpl(metaData, nullableTree(compositeLiteral.hasType(), compositeLiteral.getType()),
        trees(compositeLiteral.getElementsList()));
    }

    private Tree classDeclaration(TreeMetaData metaData, SlangProto.ClassDeclaration classDeclaration) {
      var classTree = tree(classDeclaration.getClassTree());
      var reference = classDeclaration.getIdentifier();
      var identifier = RangeConverter.resolveNullableTree(classTree, reference.isEmpty() ? null : reference, IdentifierTree.class);
      return new ClassDeclarationTreeImpl(metaData, identifier, classTree);
    }

    private Tree indexExpression(TreeMetaData metaData, SlangProto.IndexExpression indexExpression) {
      return new IndexExpressionTreeImpl(metaData, tree(indexExpression.getExpression()), tree(indexExpression.getIndex()));
    }

    private Tree importSpecification(TreeMetaData metaData, SlangProto.ImportSpecification importSpecification) {
      return new ImportSpecificationTreeImpl(
        metaData,
        nullableTree(importSpecification.hasName(), importSpecification.getName(), IdentifierTree.class),
        (StringLiteralTree) tree(importSpecification.getPath()));
    }

    private Tree keyValue(TreeMetaData metaData, SlangProto.KeyValue keyValue) {
      return new KeyValueTreeImpl(metaData, tree(keyValue.getKey()), tree(keyValue.getValue()));
    }

    private Tree returnStatement(TreeMetaData metaData, SlangProto.Return returnStatement) {
      return new ReturnTreeImpl(metaData, token(returnStatement.getKeyword()), trees(returnStatement.getExpressionsList()));
    }

    private Tree unaryExpression(TreeMetaData metaData, SlangProto.UnaryExpression unaryExpression) {
      return new UnaryExpressionTreeImpl(metaData, UnaryExpressionTree.Operator.valueOf(unaryExpression.getOperator()),
        tree(unaryExpression.getOperand()));
    }

    private Tree loop(TreeMetaData metaData, SlangProto.Loop loop) {
      return new LoopTreeImpl(
        metaData,
        nullableTree(loop.hasCondition(), loop.getCondition()),
        tree(loop.getBody()),
        LoopTree.LoopKind.valueOf(loop.getKind()),
        token(loop.getKeyword()));
    }

    private Tree mapType(TreeMetaData metaData, SlangProto.MapType mapType) {
      return new MapTypeTreeImpl(metaData, tree(mapType.getKey()), tree(mapType.getValue()));
    }

    private Tree matchCase(TreeMetaData metaData, SlangProto.MatchCase matchCase) {
      return new MatchCaseTreeImpl(metaData, nullableTree(matchCase.hasExpression(), matchCase.getExpression()),
        nullableTree(matchCase.hasBody(), matchCase.getBody()));
    }

    private Tree parenthesizedExpression(TreeMetaData metaData, SlangProto.ParenthesizedExpression expression) {
      return new ParenthesizedExpressionTreeImpl(metaData, tree(expression.getExpression()),
        token(expression.getLeftParenthesis()), token(expression.getRightParenthesis()));
    }

    private Tree ifStatement(TreeMetaData metaData, SlangProto.If ifStatement) {
      return new IfTreeImpl(
        metaData,
        tree(ifStatement.getCondition()),
        tree(ifStatement.getThenBranch()),
        nullableTree(ifStatement.hasElseBranch(), ifStatement.getElseBranch()),
        token(ifStatement.getIfKeyword()),
        nullableToken(ifStatement.getElseKeyword()));
    }

    private Tree match(TreeMetaData metaData, SlangProto.Match match) {
      return new MatchTreeImpl(
        metaData,
        nullableTree(match.hasExpression(), match.getExpression()),
        trees(match.getCasesList(), MatchCaseTree.class),
        token(match.getKeyword()));
    }

    private Tree jump(TreeMetaData metaData, SlangProto.Jump jump) {
      return new JumpTreeImpl(
        metaData,
        token(jump.getKeyword()),
        JumpTree.JumpKind.valueOf(jump.getKind()),
        nullableTree(jump.hasLabel(), jump.getLabel(), IdentifierTree.class));
    }

    private Tree slice(TreeMetaData metaData, SlangProto.Slice slice) {
      return new SliceTreeImpl(
        metaData,
        tree(slice.getExpression()),
        nullableTree(slice.hasLow(), slice.getLow()),
        nullableTree(slice.hasHigh(), slice.getHigh()),
        nullableTree(slice.hasMax(), slice.getMax()),
        slice.getSlice3());
    }

    private Tree indexListExpression(TreeMetaData metaData, SlangProto.IndexListExpression indexListExpression) {
      return new IndexListExpressionTreeImpl(metaData, tree(indexListExpression.getExpression()),
        trees(indexListExpression.getIndicesList()));
    }

    private Tree goStatement(TreeMetaData metaData, SlangProto.GoStatement goStatement) {
      return new GoStatementTreeImpl(metaData, token(goStatement.getGoToken()),
        (FunctionInvocationTree) tree(goStatement.getFunctionInvocation()));
    }

    private Tree typeAssertionExpression(TreeMetaData metaData, SlangProto.TypeAssertionExpression expression) {
      return new TypeAssertionExpressionTreeImpl(metaData, tree(expression.getExpression()),
        nullableTree(expression.hasType(), expression.getType()));
    }

    private Tree ellipsis(TreeMetaData metaData, SlangProto.Ellipsis ellipsis) {
      return new EllipsisTreeImpl(metaData, token(ellipsis.getEllipsis()), nullableTree(ellipsis.hasElement(), ellipsis.getElement()));
    }

    private Tree catchClause(TreeMetaData metaData, SlangProto.Catch catchClause) {
      return new CatchTreeImpl(metaData, nullableTree(catchClause.hasCatchParameter(), catchClause.getCatchParameter()),
        tree(catchClause.getCatchBlock()), token(catchClause.getKeyword()));
    }

    private Tree exceptionHandling(TreeMetaData metaData, SlangProto.ExceptionHandling exceptionHandling) {
      return new ExceptionHandlingTreeImpl(
        metaData,
        tree(exceptionHandling.getTryBlock()),
        token(exceptionHandling.getTryKeyword()),
        trees(exceptionHandling.getCatchBlocksList(), CatchTree.class),
        nullableTree(exceptionHandling.hasFinallyBlock(), exceptionHandling.getFinallyBlock()));
    }

    private Tree throwStatement(TreeMetaData metaData, SlangProto.Throw throwStatement) {
      return new ThrowTreeImpl(metaData, token(throwStatement.getKeyword()),
        nullableTree(throwStatement.hasBody(), throwStatement.getBody()));
    }
  }
}
