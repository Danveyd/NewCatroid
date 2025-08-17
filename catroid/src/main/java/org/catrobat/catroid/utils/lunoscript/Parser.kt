package org.catrobat.catroid.utils.lunoscript

import android.util.Log

// Pratt Parser-inspired structure for expressions for easier precedence handling
class Parser(private val tokens: List<Token>) {
    private var current = 0

    private class ParseError(message: String, token: Token) :
        LunoSyntaxError(message, token.line, token.position)

    fun parse(): ProgramNode {
        val statements = mutableListOf<Statement>()
        val startLine = if (tokens.isNotEmpty() && tokens[0].type != TokenType.EOF) tokens[0].line else 1
        Log.d("LunoParser-Trace", "--- STARTING PARSE ---")
        while (!isAtEnd()) {
            skipComments()
            if (isAtEnd()) break
            try {
                declaration()?.let { statements.add(it) }
            } catch (error: LunoSyntaxError) {
                println("Parser encountered an error: ${error.message}")
                synchronize()
            } catch (error: ParseError) {
                println("Parser encountered an error: ${error.message}")
                synchronize()
            }
        }
        Log.d("LunoParser-Trace", "--- FINISHED PARSE ---")
        return ProgramNode(statements, startLine)
    }

    private fun declaration(): Statement? {
        try {
            skipComments()
            if (isAtEnd()) return null
            if (match(TokenType.FUN)) return funDeclaration("function")
            if (match(TokenType.CLASS)) return classDeclaration()
            if (match(TokenType.VAR)) return varDeclaration()
            if (match(TokenType.IMPORT)) return importStatement()
            return statement()
        } catch (e: ParseError) {
            synchronize()
            return null
        }
    }

    private fun tryStatement(): Statement {
        val tryToken = previous()

        // Напрямую парсим блок TRY
        consume(TokenType.LBRACE, "Expect '{' before 'try' body.")
        val tryBlockBody = blockStatements()
        consume(TokenType.RBRACE, "Expect '}' after 'try' body.")
        val tryBlock = BlockStatement(tryBlockBody, tryToken.line)

        var catchVariable: Token? = null
        var catchBlock: Statement? = null
        if (match(TokenType.CATCH)) {
            consume(TokenType.LPAREN, "Expect '(' after 'catch'.")
            catchVariable = consume(TokenType.IDENTIFIER, "Expect exception variable name.")
            consume(TokenType.RPAREN, "Expect ')' after exception variable.")

            // Напрямую парсим блок CATCH
            consume(TokenType.LBRACE, "Expect '{' before 'catch' body.")
            val catchBlockBody = blockStatements()
            consume(TokenType.RBRACE, "Expect '}' after 'catch' body.")
            catchBlock = BlockStatement(catchBlockBody, catchVariable.line)
        }

        var finallyBlock: Statement? = null
        if (match(TokenType.FINALLY)) {
            // Напрямую парсим блок FINALLY
            consume(TokenType.LBRACE, "Expect '{' before 'finally' body.")
            val finallyBlockBody = blockStatements()
            consume(TokenType.RBRACE, "Expect '}' after 'finally' body.")
            finallyBlock = BlockStatement(finallyBlockBody, previous().line)
        }

        if (catchBlock == null && finallyBlock == null) {
            throw error(tryToken, "A 'try' statement must have at least a 'catch' or a 'finally' block.")
        }

        // tryBlock уже является Statement (BlockStatement), так что передаем его напрямую
        return TryCatchStatement(tryBlock, catchVariable, catchBlock, finallyBlock, tryToken.line)
    }

    private fun importStatement(): Statement {
        val importToken = previous() // 'import'
        val path = mutableListOf<Token>()

        // Парсим путь, состоящий из идентификаторов, разделенных точками
        do {
            path.add(consume(TokenType.IDENTIFIER, "Expect package or class name."))
        } while (match(TokenType.DOT))

        consumeSemicolon("Expect ';' or newline after import statement.")
        return ImportStatement(path, importToken.line)
    }

    private fun skipComments() {
        while (check(TokenType.COMMENT)) {
            advance()
        }
    }

    private fun classDeclaration(): Statement {
        val classToken = previous()
        val name = consume(TokenType.IDENTIFIER, "Expect class name.")
        Log.d("LunoParser-Trace", ">>> Entering classDeclaration for '${name.lexeme}'")
        val superclass = if (match(TokenType.LT)) {
            VariableExpr(consume(TokenType.IDENTIFIER, "Expect superclass name."), previous().line)
        } else null

        consume(TokenType.LBRACE, "Expect '{' before class body.")
        val methods = mutableListOf<FunDeclarationStatement>()

        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            skipComments()
            if (check(TokenType.RBRACE)) break

            try {
                if (match(TokenType.FUN)) {
                    methods.add(funDeclaration("method") as FunDeclarationStatement)
                } else {
                    throw error(peek(), "Only 'fun' declarations are allowed inside a class body.")
                }
            } catch (e: ParseError) {
                println("Parser error inside class '${name.lexeme}': ${e.message}")
                synchronize()
            }
        }

        consume(TokenType.RBRACE, "Expect '}' after class body.")
        Log.d("LunoParser-Trace", "<<< Exiting classDeclaration for '${name.lexeme}'")
        return ClassDeclarationStatement(name, methods, superclass, classToken.line)
    }

    private fun funDeclaration(kind: String): Statement {
        val funToken = previous()
        val name = consume(TokenType.IDENTIFIER, "Expect $kind name.")
        Log.d("LunoParser-Trace", ">>> Entering funDeclaration for '${name.lexeme}' (kind: $kind)")
        consume(TokenType.LPAREN, "Expect '(' after $kind name.")
        val parameters = mutableListOf<Token>()
        if (!check(TokenType.RPAREN)) {
            do {
                if (parameters.size >= 255) error(peek(), "Cannot have more than 255 parameters.")
                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."))
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RPAREN, "Expect ')' after parameters.")
        consume(TokenType.LBRACE, "Expect '{' before $kind body.")

        val bodyStatements = blockStatements() // ВЫЗОВ КЛЮЧЕВОЙ ФУНКЦИИ

        consume(TokenType.RBRACE, "Expect '}' after $kind body.")

        val body = BlockStatement(bodyStatements, funToken.line)
        Log.d("LunoParser-Trace", "<<< Exiting funDeclaration for '${name.lexeme}'")
        return FunDeclarationStatement(name, parameters, body, funToken.line)
    }

    private fun varDeclaration(): Statement {
        val name = consume(TokenType.IDENTIFIER, "Expect variable name.")
        Log.d("LunoParser", "Creating VarDeclarationStatement for '${name.lexeme}'")
        val initializer = if (match(TokenType.ASSIGN)) expression() else null
        consumeSemicolon("Expect ';' or newline after variable declaration.")
        return VarDeclarationStatement(name, initializer, name.line)
    }

    private fun ifStatement(): Statement {
        val ifToken = previous()
        consume(TokenType.LPAREN, "Expect '(' after 'if'.")
        val condition = expression()
        consume(TokenType.RPAREN, "Expect ')' after if condition.")
        val thenBranch = statement()
        val elseBranch = if (match(TokenType.ELSE)) statement() else null
        return IfStatement(condition, thenBranch, elseBranch, ifToken, ifToken.line)
    }

    private fun whileStatement(): Statement {
        val whileToken = previous()
        consume(TokenType.LPAREN, "Expect '(' after 'while'.")
        val condition = expression()
        consume(TokenType.RPAREN, "Expect ')' after while condition.")
        val body = statement()
        return WhileStatement(condition, body, whileToken, whileToken.line)
    }

    private fun forStatement(): Statement {
        val forToken = previous()
        consume(TokenType.LPAREN, "Expect '(' after 'for'.")
        val variable = consume(TokenType.IDENTIFIER, "Expect loop variable name.")
        consume(TokenType.IN, "Expect 'in' after loop variable.")
        val iterable = expression()
        consume(TokenType.RPAREN, "Expect ')' after for-in expression.")
        val body = statement()
        return ForInStatement(variable, iterable, body, forToken, forToken.line)
    }

    private fun switchStatement(): Statement {
        val switchToken = previous()
        consume(TokenType.LPAREN, "Expect '(' after 'switch'.")
        val expr = expression()
        consume(TokenType.RPAREN, "Expect ')' after switch expression.")
        consume(TokenType.LBRACE, "Expect '{' before switch cases.")

        val cases = mutableListOf<SwitchCase>()
        var defaultCase: SwitchCase? = null

        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            val caseOrDefaultToken = peek()
            val isDefault: Boolean
            val values = mutableListOf<Expression>()

            if (match(TokenType.CASE)) {
                isDefault = false
                do {
                    values.add(expression())
                } while (match(TokenType.COMMA) && !check(TokenType.COLON))
            } else if (match(TokenType.DEFAULT)) {
                isDefault = true
                if (defaultCase != null) throw error(previous(), "Multiple default cases in switch statement.")
            } else {
                throw error(peek(), "Expect 'case' or 'default'.")
            }

            consume(TokenType.COLON, "Expect ':' after case/default.")
            val caseStatements = mutableListOf<Statement>()

            val caseBody = statement() // This will parse a single statement or a block { ... }

            val currentCase = SwitchCase(if (isDefault) null else values, caseBody, caseOrDefaultToken, isDefault)
            if (isDefault) defaultCase = currentCase else cases.add(currentCase)
        }
        consume(TokenType.RBRACE, "Expect '}' after switch body.")
        if (defaultCase != null) cases.add(defaultCase) // Add default case at the end if present

        return SwitchStatement(expr, cases, switchToken, switchToken.line)
    }

    private fun returnStatement(): Statement {
        val keyword = previous()
        val value = if (!check(TokenType.SEMICOLON) && !check(TokenType.RBRACE)) expression() else null
        consumeSemicolon("Expect ';' or newline after return value.")
        return ReturnStatement(keyword, value, keyword.line)
    }

    private fun blockStatements(): List<Statement> {
        Log.d("LunoParser-Trace", "    [BS] >> Entering blockStatements. Looking for RBRACE. Current token: ${peek().lexeme}")
        val statements = mutableListOf<Statement>()
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            declaration()?.let { statements.add(it) }
        }
        Log.d("LunoParser-Trace", "    [BS] << Exiting blockStatements. Current token: ${peek().lexeme}")
        return statements
    }

    private fun breakStatement(): Statement {
        val keyword = previous()
        consumeSemicolon("Expect ';' or newline after 'break'.")
        return BreakStatement(keyword, keyword.line)
    }

    private fun continueStatement(): Statement {
        val keyword = previous()
        consumeSemicolon("Expect ';' or newline after 'continue'.")
        return ContinueStatement(keyword, keyword.line)
    }


    private fun expressionStatement(): Statement {
        val expr = call()

        if (peek().type.isAssignmentOperator()) {
            Log.d("LunoParser", "Creating AssignmentStatement for target: $expr")
            if (!(expr is VariableExpr || expr is GetExpr || expr is IndexAccessExpr || expr is ThisExpr)) {
                throw error(peek(), "Invalid assignment target. Left side must be a variable, property, or index access.")
            }
            val operator = advance()
            val value = expression()
            consumeSemicolon("Expect ';' or newline after assignment statement.")
            return AssignmentStatement(expr, value, operator, operator.line)
        } else {
            Log.d("LunoParser", "Creating ExpressionStatement for expr: $expr")
            consumeSemicolon("Expect ';' or newline after expression statement.")
            return ExpressionStatement(expr, expr.line)
        }
    }

    // --- Expression parsing (Pratt style with precedence) ---
    private fun logicalOr(): Expression {
        var expr = logicalAnd()
        while (match(TokenType.OR)) {
            val operator = previous()
            val right = logicalAnd()
            expr = LogicalExpr(expr, operator, right, operator.line)
        }
        return expr
    }

    private fun logicalAnd(): Expression {
        var expr = equality()
        while (match(TokenType.AND)) {
            val operator = previous()
            val right = equality()
            expr = LogicalExpr(expr, operator, right, operator.line)
        }
        return expr
    }

    private fun equality(): Expression {
        var expr = comparison()
        while (match(TokenType.NEQ, TokenType.EQ)) {
            val operator = previous()
            val right = comparison()
            expr = BinaryExpr(expr, operator, right, operator.line)
        }
        return expr
    }

    private fun comparison(): Expression {
        var expr = term()
        while (match(TokenType.GT, TokenType.GTE, TokenType.LT, TokenType.LTE)) {
            val operator = previous()
            val right = term()
            expr = BinaryExpr(expr, operator, right, operator.line)
        }
        return expr
    }

    private fun term(): Expression { // Addition, Subtraction
        var expr = factor()
        while (match(TokenType.MINUS, TokenType.PLUS)) {
            val operator = previous()
            val right = factor()
            expr = BinaryExpr(expr, operator, right, operator.line)
        }
        return expr
    }

    private fun factor(): Expression { // Multiplication, Division, Modulo
        var expr = unary()
        while (match(TokenType.DIVIDE, TokenType.MULTIPLY, TokenType.MODULO)) {
            val operator = previous()
            val right = unary()
            expr = BinaryExpr(expr, operator, right, operator.line)
        }
        return expr
    }

    private fun unary(): Expression {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            val operator = previous()
            val right = unary() // Unary operators are right-associative
            return UnaryExpr(operator, right, operator.line)
        }
        return call()
    }

    private fun call(): Expression {
        var expr = primary()
        while (true) {
            when {
                match(TokenType.LPAREN) -> expr = finishCall(expr)
                match(TokenType.LBRACKET) -> expr = finishIndexAccess(expr)
                match(TokenType.DOT) -> {
                    val name = consume(TokenType.IDENTIFIER, "Expect property name after '.'.")
                    expr = GetExpr(expr, name, name.line)
                }
                else -> break
            }
        }
        return expr
    }

    private fun finishCall(callee: Expression): Expression {
        val arguments = mutableListOf<Expression>()
        val lparenToken = previous() // For line number of call
        if (!check(TokenType.RPAREN)) {
            do {
                if (arguments.size >= 255) error(peek(), "Cannot have more than 255 arguments.")
                arguments.add(expression()) // Parse each argument expression
            } while (match(TokenType.COMMA))
        }
        val paren = consume(TokenType.RPAREN, "Expect ')' after arguments.")
        return CallExpr(callee, arguments, paren, lparenToken.line)
    }

    private fun finishIndexAccess(callee: Expression): Expression {
        val bracketToken = previous() // '[' token
        val index = expression()
        consume(TokenType.RBRACKET, "Expect ']' after index.")
        return IndexAccessExpr(callee, bracketToken, index, bracketToken.line)
    }


    private fun primary(): Expression {
        val token = peek()
        return when {
            match(TokenType.FALSE) -> LiteralExpr(LunoValue.Boolean(false), previous().line)
            match(TokenType.TRUE) -> LiteralExpr(LunoValue.Boolean(true), previous().line)
            match(TokenType.NULL) -> LiteralExpr(LunoValue.Null, previous().line)
            match(TokenType.THIS) -> ThisExpr(previous(), previous().line)
            match(TokenType.NUMBER_LITERAL) -> LiteralExpr(LunoValue.Number(previous().literal as Double), previous().line)
            match(TokenType.FLOAT_LITERAL) -> LiteralExpr(LunoValue.Float(previous().literal as Float), previous().line)
            match(TokenType.STRING_LITERAL) -> LiteralExpr(LunoValue.String(previous().literal as String), previous().line)
            match(TokenType.IDENTIFIER) -> VariableExpr(previous(), previous().line)
            match(TokenType.LPAREN) -> {
                val lParenLine = previous().line
                val expr = expression()
                consume(TokenType.RPAREN, "Expect ')' after expression.")
                // No specific GroupingExpr needed if precedence handles it. Return contained expr.
                expr // Could wrap in GroupingExpr(expr, lParenLine) if needed for source mapping or specific eval
            }
            match(TokenType.LBRACKET) -> listLiteral()
            // LBRACE for map literals needs to be distinguished from block statements
            // This would typically be handled by context (e.g. if an expression is expected)
            // For simplicity, we might skip map literals for now or require a specific keyword.
            // Let's try to parse it if we are in an expression context.
            peek().type == TokenType.LBRACE && isExpressionContext() -> mapLiteral()


            else -> throw error(peek(), "Expect expression, found ${peek().type}.")
        }
    }

    private fun listLiteral(): Expression {
        val bracket = previous() // '['
        val elements = mutableListOf<Expression>()
        if (!check(TokenType.RBRACKET)) {
            do {
                elements.add(expression())
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RBRACKET, "Expect ']' after list elements.")
        return ListLiteralExpr(elements, bracket, bracket.line)
    }

    private fun mapLiteral(): Expression {
        val brace = consume(TokenType.LBRACE, "Expect '{' for map literal.") // Consume LBRACE explicitly
        val entries = mutableMapOf<Token, Expression>()
        if (!check(TokenType.RBRACE)) {
            do {
                // Key can be IDENTIFIER or STRING_LITERAL
                val keyToken = when {
                    check(TokenType.IDENTIFIER) -> advance()
                    check(TokenType.STRING_LITERAL) -> advance()
                    else -> throw error(peek(), "Expect identifier or string as map key.")
                }
                consume(TokenType.COLON, "Expect ':' after map key.")
                val valueExpr = expression()
                entries[keyToken] = valueExpr
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RBRACE, "Expect '}' after map entries.")
        return MapLiteralExpr(entries, brace, brace.line)
    }

    // Helper to determine if we're in a context where an LBRACE could start a map literal
    // This is a simplified check. A more robust way is to have the grammar rules guide this.
    private var expressionDepth = 0
    private fun isExpressionContext(): Boolean {
        // This is a heuristic. If called from primary(), we are likely in expression context.
        // A better way is for the calling rule to know if it expects an expression.
        // For now, let's assume if `primary()` is trying to parse something, it's an expr.
        return true // Simplified: primary always expects an expression.
    }


    // --- Statement specific parsing that involves expressions ---
    // Overrides the `expressionStatement` to handle assignment as a statement.
    // This is part of the key to making `ident = value` a statement.
    private fun statement(): Statement {
        skipComments() // Пропускаем комменты перед любой инструкцией
        if (isAtEnd()) throw error(peek(), "Unexpected end of file.")

        // Диспетчер для всех инструкций, которые НЕ являются объявлениями
        return when {
            match(TokenType.IF) -> ifStatement()
            match(TokenType.WHILE) -> whileStatement()
            match(TokenType.FOR) -> forStatement()
            match(TokenType.SWITCH) -> switchStatement()
            match(TokenType.RETURN) -> returnStatement()
            match(TokenType.BREAK) -> breakStatement()
            match(TokenType.CONTINUE) -> continueStatement()
            match(TokenType.TRY) -> tryStatement()

            // Блок {...} - это самостоятельная инструкция
            match(TokenType.LBRACE) -> {
                val lbraceToken = previous()
                val statements = blockStatements()
                consume(TokenType.RBRACE, "Expect '}' after block.")
                BlockStatement(statements, lbraceToken.line)
            }

            // Если ничего не подошло - это выражение (вызов, присваивание и т.д.)
            else -> expressionStatement()
        }
    }

    private fun expression(): Expression = logicalOr() // Начинаем с логического ИЛИ (или другой операции с нужным приоритетом)

    // Вспомогательная функция для TokenType (если ее еще нет)
    private fun TokenType.isAssignmentOperator(): Boolean {
        return this == TokenType.ASSIGN || this == TokenType.PLUS_ASSIGN || this == TokenType.MINUS_ASSIGN ||
                this == TokenType.MULTIPLY_ASSIGN || this == TokenType.DIVIDE_ASSIGN || this == TokenType.MODULO_ASSIGN
    }


    // --- Utility methods ---
    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun consume(type: TokenType, message: String): Token {
        Log.d("LunoScriptParser", "CONSUME: Attempting to consume ${type}. Current peek is ${peek().type} ('${peek().lexeme}')")
        if (check(type)) {
            val consumedToken = advance()
            Log.d("LunoScriptParser", "CONSUME: Successfully consumed ${consumedToken.type}. Next peek is ${if(!isAtEnd()) peek().type else "EOF"}")
            return consumedToken
        }
        Log.e("LunoScriptParser", "CONSUME_ERROR: Failed to consume ${type}. Peek is ${peek().type}. Throwing error: $message")
        throw error(peek(), message)
    }

    private fun consumeSemicolon(message: String) {
        if (match(TokenType.SEMICOLON)) return
        // Semicolons are optional if followed by RBRACE or EOF, or if it's the last token on a line
        if (check(TokenType.RBRACE) || isAtEnd() || previous().line < peek().line) return
        // Only throw error if semicolon is truly missing where expected and not optional.
        // This simple check might not be perfect for all newline rules.
        if (tokens[current-1].type != TokenType.RBRACE && tokens[current-1].type != TokenType.LBRACE) { // Avoid error after block
            // Don't throw error here, make them truly optional for now.
            // throw error(peek(), message)
        }
    }


    private fun check(type: TokenType): Boolean = if (isAtEnd()) false else peek().type == type
    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun isAtEnd(): Boolean = peek().type == TokenType.EOF
    private fun peek(): Token = tokens[current]
    private fun previous(): Token = tokens[current - 1]

    private fun error(token: Token, message: String): ParseError = ParseError(message, token)

    private fun synchronize() {
        val problemToken = peek()
        Log.d("LunoScriptParser", "SYNC: Attempting to recover. Error occurred at or before: ${problemToken.lexeme} (${problemToken.type}) L${problemToken.line}. Previous: ${if(current > 0) previous().lexeme else "N/A"}")

        if (!isAtEnd()) {
            advance() // Пропускаем токен, вызвавший ошибку
        }

        // Ищем следующую точку для безопасного возобновления
        while (!isAtEnd()) {
            // Если предыдущий токен - точка с запятой, мы, вероятно, в безопасной точке
            if (previous().type == TokenType.SEMICOLON) {
                Log.d("LunoScriptParser", "SYNC: Resuming after semicolon.")
                return
            }

            // Ищем ключевые слова, с которых обычно начинаются новые инструкции
            when (peek().type) {
                TokenType.CLASS,
                TokenType.FUN,
                TokenType.VAR,
                TokenType.FOR,
                TokenType.IF,
                TokenType.WHILE,
                TokenType.SWITCH,
                TokenType.RETURN,
                TokenType.TRY,
                TokenType.RBRACE -> { // RBRACE - важная точка для остановки
                    Log.d("LunoScriptParser", "SYNC: Resuming at keyword ${peek().type}.")
                    return
                }
                else -> {
                    // Ничего не делаем, просто пропускаем токен
                }
            }
            advance()
        }
        Log.d("LunoScriptParser", "SYNC: Recovery reached EOF.")
    }
}