package parser;

import bg.tu_varna.kst_sit.ci_ep.ast.*;
import bg.tu_varna.kst_sit.ci_ep.ast.assignable.ArrayInitNode;
import bg.tu_varna.kst_sit.ci_ep.ast.assignable.AssignableNode;
import bg.tu_varna.kst_sit.ci_ep.ast.assignable.CharacterLiteralNode;
import bg.tu_varna.kst_sit.ci_ep.ast.assignable.StringLiteralNode;
import bg.tu_varna.kst_sit.ci_ep.ast.assignable.expression.*;
import bg.tu_varna.kst_sit.ci_ep.ast.assignable.expression.operators.binary_operators.additive_operators.AdditionNode;
import bg.tu_varna.kst_sit.ci_ep.ast.assignable.expression.operators.binary_operators.additive_operators.SubtractionNode;
import bg.tu_varna.kst_sit.ci_ep.ast.assignable.expression.operators.binary_operators.logical_operators.AndNode;
import bg.tu_varna.kst_sit.ci_ep.ast.assignable.expression.operators.binary_operators.logical_operators.OrNode;
import bg.tu_varna.kst_sit.ci_ep.ast.assignable.expression.operators.binary_operators.multiplicative_operators.DivisionNode;
import bg.tu_varna.kst_sit.ci_ep.ast.assignable.expression.operators.binary_operators.multiplicative_operators.ModNode;
import bg.tu_varna.kst_sit.ci_ep.ast.assignable.expression.operators.binary_operators.multiplicative_operators.MultiplicationNode;
import bg.tu_varna.kst_sit.ci_ep.ast.assignable.expression.operators.binary_operators.relational_operators.*;
import bg.tu_varna.kst_sit.ci_ep.ast.assignable.expression.operators.unary_operators.MinusNode;
import bg.tu_varna.kst_sit.ci_ep.ast.assignable.expression.operators.unary_operators.NotNode;
import bg.tu_varna.kst_sit.ci_ep.ast.global_definition.FunctionDefinitionNode;
import bg.tu_varna.kst_sit.ci_ep.ast.global_definition.GlobalDefinitionNode;
import bg.tu_varna.kst_sit.ci_ep.ast.global_definition.VariableDefinitionNode;
import bg.tu_varna.kst_sit.ci_ep.ast.statement.*;
import bg.tu_varna.kst_sit.ci_ep.ast.type.PrimitiveTypeNode;
import bg.tu_varna.kst_sit.ci_ep.ast.type.TypeNode;
import bg.tu_varna.kst_sit.ci_ep.ast.type.VoidTypeNode;
import bg.tu_varna.kst_sit.ci_ep.exceptions.SyntaxException;
import bg.tu_varna.kst_sit.ci_ep.lexer.Lexer;
import bg.tu_varna.kst_sit.ci_ep.lexer.token.Token;
import bg.tu_varna.kst_sit.ci_ep.parser.Parser;
import bg.tu_varna.kst_sit.ci_ep.source.SourceImpl;
import bg.tu_varna.kst_sit.ci_ep.utils.CompilerTestHelper;
import lexer.LexerImpl;
import token.TokenType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ParserImpl extends Parser<TokenType, AST> {

    public ParserImpl(Lexer<TokenType> lexer) {
        super(lexer);
    }

    private void accept(TokenType tokenType) {
        if (currentToken.getTokenType() != tokenType) {
            throw new SyntaxException("Token doesn't match! Expected " +
                    tokenType.value + ", Got " + currentToken.getTokenType().value, currentToken);
        }
        currentToken = lexer.nextToken();
    }

    @Override
    public AST entryRule() {
        accept(TokenType.PROGRAM);
        accept(TokenType.LBRACKET);
        programBody();
        accept(TokenType.RBRACKET);
        return currentNode;
    }

    void programBody() {
        List<GlobalDefinitionNode> globalDefinitions = new ArrayList<>();
        while(
                TokenType.isPrimitiveType(currentToken.getTokenType()) ||
                        (currentToken.getTokenType() == TokenType.IDENTIFIER && !currentToken.getText().equals("main"))
                ) {
            if (currentToken.getTokenType() == TokenType.IDENTIFIER) {
                functionDefinition();
            } else {
                variableDefinition();
                accept(TokenType.SEMICOLON);
            }
            globalDefinitions.add((GlobalDefinitionNode) currentNode);
        }
        mainFunction();
        globalDefinitions.add((GlobalDefinitionNode) currentNode);
        currentNode = new ProgramBodyNode(null, globalDefinitions);
    }

    void functionDefinition() {
        Token token = currentToken;
        accept(TokenType.IDENTIFIER);
        accept(TokenType.LPAREN);

        FormalParameterNode formalParameters = null;
        if (TokenType.isPrimitiveType(currentToken.getTokenType())) {
            formalParameters();
            formalParameters = (FormalParameterNode) currentNode;
        }
        accept(TokenType.RPAREN);
        accept(TokenType.ARROW);
        TypeNode typeNode;
        if (currentToken.getTokenType() == TokenType.VOID) {
            typeNode = new VoidTypeNode(currentToken);
            accept(TokenType.VOID);
        } else {
            type();
            typeNode = (TypeNode) currentNode;
        }
        block();
        BlockNode blockNode = (BlockNode)currentNode;
        currentNode = new FunctionDefinitionNode(token, formalParameters, typeNode, blockNode);
    }

    void functionCall() {
        accept(TokenType.AT);  
        Token token = currentToken;
        accept(TokenType.IDENTIFIER);
        accept(TokenType.LPAREN);
        ActualParameterNode actualParameters = null;
        if (TokenType.isLiteralTerminal(currentToken.getTokenType())) {
            actualParameters();
            actualParameters = (ActualParameterNode) currentNode;
        }
        accept(TokenType.RPAREN);
        currentNode = new FunctionCall(token, actualParameters);
    }

    void type() {
        Token token = currentToken;
        boolean isArray = false;
        if (TokenType.isPrimitiveType(currentToken.getTokenType())) {
            accept(currentToken.getTokenType());
            if (currentToken.getTokenType() == TokenType.LSQUARE) {
                isArray = true;
                accept(TokenType.LSQUARE);
                accept(TokenType.RSQUARE);
            }
        } else {
            throw new SyntaxException("Expected return type. Got " + currentToken.getTokenType().value, currentToken);
        }
        currentNode = new PrimitiveTypeNode(token, isArray);
    }

    void formalParameters() {
        List<TypedVariableNode> formalParameters = new ArrayList<>();
        type();
        formalParameters.add(new TypedVariableNode(null, (TypeNode) currentNode, new VariableNode(currentToken, null)));
        accept(TokenType.IDENTIFIER);
        while (currentToken.getTokenType() == TokenType.COMMA) {
            accept(TokenType.COMMA);
            type();
            formalParameters.add(new TypedVariableNode(null, (TypeNode) currentNode, new VariableNode(currentToken, null)));
            accept(TokenType.IDENTIFIER);
    }
    currentNode = new FormalParameterNode(null, formalParameters);
}

    void actualParameters() {
        List<AssignableNode> params = new ArrayList<>();
        assignable();
        params.add((AssignableNode) currentNode);
        while(currentToken.getTokenType() == TokenType.COMMA) {
            accept(TokenType.COMMA);
            assignable();
            params.add((AssignableNode) currentNode);
        }
        currentNode = new ActualParameterNode(null, params);
    }

    void variableDefinition() {
        type();
        TypeNode type = (TypeNode)currentNode;
        assignment();
        currentNode = new VariableDefinitionNode(null, type, (AssignmentNode) currentNode);
    }

    void assignment() {
        variable();
        VariableNode variable = (VariableNode) currentNode;
        Token token = currentToken;
        accept(TokenType.BECOMES);
        if (TokenType.isPrimitiveType(currentToken.getTokenType())) {
            arrayInitialization();
        } else if (TokenType.CHAR_LITERAL == currentToken.getTokenType()) {
            characterLiteral();
        } else if (TokenType.STRING_LITERAL == currentToken.getTokenType()) {
            stringLiteral();
        } else {
            expression();
        }
        AssignableNode assignable = (AssignableNode) currentNode;
        currentNode = new AssignmentNode(token, variable, assignable);
    }

    void arrayInitialization() {
        Token token = currentToken;
        ExpressionNode expression = null;
        if (TokenType.isPrimitiveType(currentToken.getTokenType())) {
            accept(currentToken.getTokenType());
            accept(TokenType.LSQUARE);
            expression();
            expression = (ExpressionNode) currentNode;
            accept(TokenType.RSQUARE);
        } else {
            System.out.println("Expected array initialization. Got " + currentToken.getTokenType());
        }
        currentNode = new ArrayInitNode(token, expression);
    }

    void block() {
        List<Statement> statements = new ArrayList<>();
        accept(TokenType.LBRACKET);
        while (TokenType.isStatementTerminal(currentToken.getTokenType())) {
            statement();
            statements.add((Statement) currentNode);
        }
        accept(TokenType.RBRACKET);
        currentNode = new BlockNode(null, statements);
    }

    void expression() {
        simpleExpression();
        Token<TokenType> token = currentToken;
        ExpressionNode left = (ExpressionNode) currentNode;
        if (TokenType.isRelationalOperator(currentToken.getTokenType())) {
            ExpressionNode right;
            ExpressionNode relationalOperator = null;
            accept((currentToken.getTokenType()));
            simpleExpression();
            right = (ExpressionNode) currentNode;
            switch (token.getTokenType()) {
                case EQUALS:        relationalOperator = new EqualsNode(token, left, right); break;
                case NOTEQUALS:     relationalOperator = new NotEqualNode(token, left, right); break;
                case GREATER:       relationalOperator = new GreaterNode(token, left, right); break;
                case GREATER_EQ:    relationalOperator = new GreaterOrEqualNode(token, left, right); break;
                case LESS:          relationalOperator = new LessNode(token, left, right); break;
                case LESS_EQ:       relationalOperator = new LessOrEqualNode(token, left, right); break;
            }
            currentNode = relationalOperator;
        }
    }

    void simpleExpression() {
        signedTerm();
        ExpressionNode left = (ExpressionNode) currentNode;
        while (TokenType.isOperatorGroupOne(currentToken.getTokenType())) {
            Token<TokenType> token = currentToken;
           accept((currentToken.getTokenType()));
            signedTerm();
            ExpressionNode right = (ExpressionNode) currentNode;
            ExpressionNode additiveOperator = null;
            switch (token.getTokenType()) {
                case PLUS:  additiveOperator = new AdditionNode(token, left, right); break;
                case MINUS: additiveOperator = new SubtractionNode(token, left, right); break;
                case OR:    additiveOperator = new OrNode(token, left, right); break;
            }
            currentNode = left = additiveOperator;
        }
    }

   void signedTerm() {
        Token<TokenType> token = null;
        if (TokenType.isUnaryOperator(currentToken.getTokenType())) {
            token = currentToken;
            accept((currentToken.getTokenType()));
        }
        term();
        ExpressionNode operand = (ExpressionNode) currentNode;
        if (token != null) {
            switch (token.getTokenType()) {
                case NOT: operand = new NotNode(token, operand); break;
                case MINUS: operand = new MinusNode(token, operand); break;
            }
        }
        currentNode = operand;
    }

    void term() {
        factor();
        ExpressionNode left = (ExpressionNode) currentNode;
        while (TokenType.isOperatorGroupTwo(currentToken.getTokenType())) {
            Token<TokenType> token = currentToken;
            accept((currentToken.getTokenType()));
            factor();
            ExpressionNode right = (ExpressionNode) currentNode;
            ExpressionNode multiplicativeOperator = null;
            switch (token.getTokenType()) {
                case MUL: multiplicativeOperator = new MultiplicationNode(token, left, right); break;
                case DIV: multiplicativeOperator = new DivisionNode(token, left, right); break;
                case MOD: multiplicativeOperator = new ModNode(token, left, right); break;
                case AND: multiplicativeOperator = new AndNode(token, left, right); break;
            }
            currentNode = left = multiplicativeOperator;
        }
    }

    void factor() {
        switch(currentToken.getTokenType()) {
            case IDENTIFIER:    variable();
                                break;
            case NUMBER:        currentNode = new IntegerNode(currentToken);
                                accept(TokenType.NUMBER);
                                break;
            case TRUE:
            case FALSE:         currentNode = new BooleanNode(currentToken);
                                accept((currentToken.getTokenType()));
                                break;
            case LENGTH:        arrayLength();
                                break;
            case LPAREN:        accept(TokenType.LPAREN);
                                expression();
                                accept(TokenType.RPAREN);
                                break;
            case AT:            functionCall();
                                break;
            default:   throw new SyntaxException("Expected factor. Got " + currentToken.getTokenType().value, currentToken);
        }
    }

    void variable() {
        Token token = currentToken;
        accept(TokenType.IDENTIFIER);
        ExpressionNode expression = null;
        if (currentToken.getTokenType() == TokenType.LSQUARE) {
            accept(TokenType.LSQUARE);
            simpleExpression();
            expression = (ExpressionNode) currentNode;
            accept(TokenType.RSQUARE);
        }
        currentNode = new VariableNode(token, expression);
    }

    void mainFunction() {
        Token token = currentToken;
        accept(TokenType.IDENTIFIER);
        accept(TokenType.LPAREN);
        accept(TokenType.RPAREN);
        accept(TokenType.ARROW);
        TypeNode typeNode = new VoidTypeNode(currentToken);
        accept(TokenType.VOID);
        block();
        currentNode = new FunctionDefinitionNode(token, null, typeNode, (BlockNode) currentNode);
    }

    void statement() {
        if (TokenType.isCompoundStatementTerminal(currentToken.getTokenType())) {
            compoundStatement();
        } else {
            simpleStatement();
            accept(TokenType.SEMICOLON);
        }
    }

    void simpleStatement() {
        switch(currentToken.getTokenType()) {
            case INT:
            case CHAR:
            case BOOLEAN:       variableDefinition();
                                break;
            case IDENTIFIER:    assignment();
                                break;
            case AT:            functionCall();
                                break;
            case RETURN:        returnStatement();
                                break;
            case PRINT:         printStatement();
                                break;
            case READ:          readStatement();
                                break;
            default: throw new SyntaxException("Expected simpleStatement. Got " + currentToken.getTokenType().value, currentToken);
        }
    }

    void compoundStatement() {
        if (currentToken.getTokenType() == TokenType.IF) {
            ifStatement();
        } else {
            whileStatement();
        }
    }
    
    void ifStatement() {
	    Token token = currentToken;
	    accept(TokenType.IF);
	    accept(TokenType.LPAREN);
	    expression();
	    ExpressionNode expressionNode = (ExpressionNode) currentNode;
	    accept(TokenType.RPAREN);
	    block();
	    BlockNode ifStatement = (BlockNode) currentNode;
	    BlockNode elseStatement = null;
	    if (currentToken.getTokenType() == TokenType.ELSE) {
	        accept(TokenType.ELSE);
	        block();
	        elseStatement = (BlockNode) currentNode;
	    }
	    currentNode = new IfStatementNode(token, expressionNode, ifStatement, elseStatement);
	}

	void whileStatement() {
	    Token token = currentToken;
	    accept(TokenType.WHILE);
	    accept(TokenType.LPAREN);
	    expression();
	    ExpressionNode expressionNode = (ExpressionNode) currentNode;
	    accept(TokenType.RPAREN);
	    block();
	    BlockNode blockNode = (BlockNode) currentNode;
	    currentNode = new WhileStatementNode(token, expressionNode, blockNode);
	}
	
	void returnStatement() {
	    Token token = currentToken;
	    accept(TokenType.RETURN);
	    AssignableNode assignable = null;
	    if (TokenType.isLiteralTerminal(currentToken.getTokenType())) {
	        assignable();
	        assignable = (AssignableNode) currentNode;
	    }
	    currentNode = new ReturnStatementNode(token, assignable);
	}
	
	void printStatement() {
	    Token token = currentToken;
	    accept(TokenType.PRINT);
	    accept(TokenType.LPAREN);
	    actualParameters();
	    ActualParameterNode actualParameters = (ActualParameterNode) currentNode;
	    accept(TokenType.RPAREN);
	    currentNode = new PrintStatementNode(token, actualParameters);
	}
	
	void readStatement() {
	    Token token = currentToken;
	    List<VariableNode> variables = new ArrayList<>();
	    accept(TokenType.READ);
	    accept(TokenType.LPAREN);
	    while (currentToken.getTokenType() == TokenType.IDENTIFIER) {
	        variable();
	        variables.add((VariableNode) currentNode);
	    }
	    accept(TokenType.RPAREN);
	    currentNode = new ReadStatementNode(token, variables);
	}
	
	void assignable() {
	    if (TokenType.isFactorTerminal(currentToken.getTokenType())) {
	        expression();
	    } else if (TokenType.isPrimitiveType(currentToken.getTokenType())) {
	        arrayInitialization();
	    } else if (TokenType.CHAR_LITERAL == currentToken.getTokenType()) {
	        characterLiteral();
	    } else {
	        stringLiteral();
	    }
	}
	
	void characterLiteral() {
	    currentNode = new CharacterLiteralNode(currentToken);
	    accept(TokenType.CHAR_LITERAL);
	}
	
	void stringLiteral() {
	    currentNode = new StringLiteralNode(currentToken);
	    accept(TokenType.STRING_LITERAL);
	}
	
	void arrayLength() {
	    Token token = currentToken;
	    accept(TokenType.LENGTH);
	    accept(TokenType.LPAREN);
	    variable();
	    accept(TokenType.RPAREN);
	    currentNode = new ArrayLengthNode(token, (VariableNode) currentNode);
	}
	
	public static void main(String[] args) throws IOException {
	    Lexer<TokenType> lexer = new LexerImpl(new SourceImpl("resources/Fib.txt"));
	    Parser<TokenType, AST> parser = new ParserImpl(lexer);
	    System.out.println(CompilerTestHelper.getASTasString(parser));
	}
}
