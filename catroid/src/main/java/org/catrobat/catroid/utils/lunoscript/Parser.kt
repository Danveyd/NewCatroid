package org.catrobat.catroid.utils.lunoscript

// Pratt Parser-inspired structure for expressions for easier precedence handling
class Parser(private val tokens: List<Token>) {
    private var current = 0

    private class ParseError(message: String, token: Token) :
        LunoSyntaxError(message, token.line, token.position)

    fun parse(): ProgramNode {
        val statements = mutableListOf<Statement>()
        val startLine = if (tokens.isNotEmpty() && tokens[0].type != TokenType.EOF) tokens[0].line else 1
        while (!isAtEnd()) {
            try {
                declaration()?.let { statements.add(it) }
            } catch (error: LunoSyntaxError) {
                println("Parser encountered an error: ${error.message}")
                synchronize() // Попытка восстановления после ошибки (пропускаем токены до следующей инструкции)
                // Не добавляем null в statements, просто продолжаем или выбрасываем, если хотим остановить парсинг
            } catch (error: ParseError) { // ParseError уже содержит LunoSyntaxError
                println("Parser encountered an error: ${error.message}")
                synchronize()
            }
        }
        return ProgramNode(statements, startLine)
    }

    // --- Top-level parsing: declarations or statements ---
    private fun declaration(): Statement? {
        return try {
            when {
                match(TokenType.CLASS) -> classDeclaration()
                match(TokenType.FUN) -> funDeclaration("function")
                match(TokenType.VAR) -> varDeclaration()
                else -> statement()
            }
        } catch (e: ParseError) {
            throw e // Перебросить для обработки выше или синхронизации
        }
    }

    private fun classDeclaration(): Statement {
        val classToken = previous()
        val name = consume(TokenType.IDENTIFIER, "Expect class name.")
        val superclass = if (match(TokenType.LT)) { // Простой синтаксис < для наследования (пока не используется)
            VariableExpr(consume(TokenType.IDENTIFIER, "Expect superclass name."), previous().line)
        } else null

        consume(TokenType.LBRACE, "Expect '{' before class body.")
        val methods = mutableListOf<FunDeclarationStatement>()
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            // Внутри класса могут быть только объявления методов (fun)
            // Поля класса будут как var внутри init или через this.field = ...
            if (match(TokenType.FUN)) {
                methods.add(funDeclaration("method") as FunDeclarationStatement)
            } else {
                throw error(peek(), "Only 'fun' (methods) are allowed directly inside a class body. Fields are typically initialized in 'init' or used with 'this'.")
            }
        }
        consume(TokenType.RBRACE, "Expect '}' after class body.")
        return ClassDeclarationStatement(name, methods, superclass, classToken.line)
    }


    private fun funDeclaration(kind: String): Statement { // kind: "function" or "method"
        val funToken = previous() // 'fun' token
        val name = consume(TokenType.IDENTIFIER, "Expect $kind name.")
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
        val body = BlockStatement(blockStatements(), previous().line) // Line of LBRACE
        return FunDeclarationStatement(name, parameters, body, funToken.line)
    }

    private fun varDeclaration(): Statement {
        val name = consume(TokenType.IDENTIFIER, "Expect variable name.")
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
                // Parse one or more expressions for this case, separated by commas, until ':'
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
            // Read statements until next case, default, or RBRACE. No implicit fall-through by default.
            // LunoScript switch will require 'break' for C-style fall-through if desired,
            // or each case block is distinct. For simplicity, let's make each case block distinct.
            // To allow multiple statements without braces, we need a loop.
            // A single statement is simpler for now.
            // For multiple statements, a BlockStatement is better.
            val caseBody = statement() // This will parse a single statement or a block { ... }

            val currentCase = SwitchCase(if (isDefault) null else values, caseBody, caseOrDefaultToken, isDefault)
            if (isDefault) defaultCase = currentCase else cases.add(currentCase)
        }
        consume(TokenType.RBRACE, "Expect '}' after switch body.")
        if (defaultCase != null) cases.add(defaultCase) // Add default case at the end if present

        return SwitchStatement(expr, cases, switchToken, switchToken.line)
    }


    private fun blockStatements(): List<Statement> {
        val statements = mutableListOf<Statement>()
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            declaration()?.let { statements.add(it) }
        }
        return statements
    }

    private fun returnStatement(): Statement {
        val keyword = previous()
        val value = if (!check(TokenType.SEMICOLON) && !check(TokenType.RBRACE)) expression() else null
        consumeSemicolon("Expect ';' or newline after return value.")
        return ReturnStatement(keyword, value, keyword.line)
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
        val expr = expression()
        val line = expr.line
        consumeSemicolon("Expect ';' or newline after expression statement.")
        return ExpressionStatement(expr, line)
    }

    // --- Expression parsing (Pratt style with precedence) ---
    private fun expression(): Expression = assignment()

    // Assignment has the lowest precedence for expressions that can be on the right of '='
    // but `=` itself is not an expression in Kotlin-like LunoScript, it's part of AssignmentStatement.
    // This function handles the right-hand side of an assignment or other expressions.
    // If we wanted `a = b = 5` where assignment is an expression, this would be different.
    // For now, assignment logic is mostly in `parseAssignmentTargetAndValue`.
    private fun assignment(): Expression {
        val expr = logicalOr() // Start with the next higher precedence

        // This is where `IDENTIFIER = value` would be handled IF assignment were an expression.
        // Since we made it a statement, this part is simpler.
        // However, compound assignments like `a += 1` ARE statements but parse like expressions.
        // Let's make `AssignmentStatement` capable of handling this.
        // The `expr` here is the potential target.

        if (match(TokenType.ASSIGN, TokenType.PLUS_ASSIGN, TokenType.MINUS_ASSIGN,
                TokenType.MULTIPLY_ASSIGN, TokenType.DIVIDE_ASSIGN, TokenType.MODULO_ASSIGN)) {
            val operator = previous()
            val value = assignment() // Right-associative

            // The target 'expr' must be assignable (VariableExpr or GetExpr)
            if (expr is VariableExpr || expr is GetExpr || expr is IndexAccessExpr) {
                // This is tricky. We are in an expression parsing function,
                // but assignment is a statement. This indicates that assignment
                // cannot be freely mixed like `print(a = 1)`.
                // It should be parsed by `statement()` directly.
                // This `assignment()` function should only parse expressions
                // that *could be* on the right-hand side of an assignment or used elsewhere.
                // So, if we encounter an '=', it's an error here unless we change assignment to be an expression.

                // For Kotlin-like behavior, direct assignment (`=`) is a statement.
                // `var x = (a = 10);` should be illegal.
                // `a = 10;` is handled by `ExpressionStatement` wrapping an `AssignmentExpr` if assign is expr,
                // OR by `AssignmentStatement` directly. We chose the latter.

                // Let's assume this `assignment()` function is for parsing the RHS of an actual AssignmentStatement
                // or expressions that are NOT assignments themselves.
                // If we hit an assignment operator here, it implies an error in grammar design or call.
                // The `statement()` function should distinguish `IDENTIFIER = ...` as `AssignmentStatement`.

                // For now, we'll throw an error if an assignment operator is found where an expression is expected
                // unless it's a compound assignment that we could desugar or handle via a specific node.
                // Let's re-think: `expressionStatement` calls `expression()`. If `expression()` sees `ident = val`,
                // it should be parsed as an assignment.

                // Correct approach: `expression()` calls `logicalOr()`. `logicalOr()` will NOT parse assignment.
                // `AssignmentStatement` is created by `statement()` if it sees `target OP value`.
                // So, the `ASSIGN` etc. check here is if we allow `a = (b = 5)`.
                // If LunoScript assignment IS an expression (returns the assigned value):
                if (expr is VariableExpr || expr is GetExpr || expr is IndexAccessExpr) {
                    // Create an Assignment EXPRESSION node if we want assignments to be expressions
                    // For now, we don't have an AssignmentExpr distinct from AssignmentStatement.
                    // So this indicates `a = b = 5` would need `AssignmentExpr` to be returned.
                    // Let's stick to assignment being a statement for now.
                    // This means `expression()` should not parse `=`.
                    // This part of code should probably not exist if assignment is not an expression.
                    throw error(operator, "Assignment ('=') is a statement, not an expression. Compound assignments like '+=' are also statements.")
                } else {
                    throw error(operator, "Invalid assignment target.")
                }

            }
            // If we *did* have AssignmentExpr: return AssignmentExpr(expr, operator, value, operator.line)
        }
        return expr // If no assignment operator, return the parsed higher-precedence expression
    }


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
        // Сначала проверяем специфичные для инструкций ключевые слова
        return when {
            match(TokenType.IF) -> ifStatement()
            match(TokenType.WHILE) -> whileStatement()
            match(TokenType.FOR) -> forStatement()
            match(TokenType.SWITCH) -> switchStatement()
            match(TokenType.LBRACE) -> BlockStatement(blockStatements(), previous().line)
            match(TokenType.RETURN) -> returnStatement()
            match(TokenType.BREAK) -> breakStatement()
            match(TokenType.CONTINUE) -> continueStatement()
            // Если это не специальная инструкция, пробуем expressionStatement,
            // которое внутри себя разберется с присваиваниями
            else -> expressionStatement()
        }
    }

    private fun isPotentialAssignment(): Boolean {
        // Heuristic: check if current tokens could form `lvalue ASSIGN_OP ...`
        // This is tricky without full backtracking or more sophisticated lookahead.
        // A simpler way: `expressionStatement` calls `expression()`. If `expression()`
        // itself can parse assignment (as an expression), then `ExpressionStatement` wraps it.
        // If assignment is purely a statement, then `statement()` must distinguish it.

        // Let's try parsing an l-value (target) and then check for an assignment operator.
        var k = 0
        if (!isLValueStart(tokens[current + k])) return false
        k++
        // Skip over .prop or [index] parts of the l-value
        while (true) {
            if (tokens.getOrNull(current + k)?.type == TokenType.DOT &&
                tokens.getOrNull(current + k + 1)?.type == TokenType.IDENTIFIER) {
                k += 2
            } else if (tokens.getOrNull(current + k)?.type == TokenType.LBRACKET) {
                // Need to skip the whole expression inside brackets, too complex for simple lookahead
                // For now, just check for LBRACKET then an assignment op
                k++
                // Simplification: if we see LBRACKET, assume it could be part of an LValue
                // then check for assignment operator right after what could be `expr]`
                // This lookahead is getting complicated.
                // Let's parse the target expression, then check the next token.
                return true // Assume it might be, let `parseAssignmentStatement` confirm.
            } else {
                break
            }
        }
        val operatorToken = tokens.getOrNull(current + k)
        return operatorToken?.type?.isAssignmentOperator() == true
    }

    private fun TokenType.isAssignmentOperator(): Boolean {
        return this == TokenType.ASSIGN || this == TokenType.PLUS_ASSIGN || this == TokenType.MINUS_ASSIGN ||
                this == TokenType.MULTIPLY_ASSIGN || this == TokenType.DIVIDE_ASSIGN || this == TokenType.MODULO_ASSIGN
    }


    private fun isLValueStart(token: Token?): Boolean {
        return token?.type == TokenType.IDENTIFIER || token?.type == TokenType.THIS // Add other valid starts like LPAREN for (obj).prop
    }

    private fun parseAssignmentStatement(): Statement {
        // Parse the target (l-value). This should be `call()` or `primary()` to get `a.b` or `a[i]`.
        val targetExpr = call() // `call()` can parse `ident`, `ident.prop`, `ident[idx]`

        if (!(targetExpr is VariableExpr || targetExpr is GetExpr || targetExpr is IndexAccessExpr)) {
            throw error(peek(), "Invalid assignment target. Expected variable, property, or index access.")
        }

        if (peek().type.isAssignmentOperator()) {
            val operator = advance() // Consume the assignment operator
            val value = expression() // Parse the RHS value
            consumeSemicolon("Expect ';' or newline after assignment.")
            return AssignmentStatement(targetExpr, value, operator, operator.line)
        } else {
            // This wasn't an assignment after all, it was just an expression.
            // This indicates the `isPotentialAssignment` heuristic was too broad or there's a parsing flow issue.
            // Fallback to parsing as a normal expression statement.
            // This requires careful handling to avoid consuming tokens incorrectly.
            // For now, let's assume `isPotentialAssignment` is good enough or `parseAssignmentStatement` is called only when sure.
            // This implies that `statement()` needs to be smarter.
            // A common technique is to parse an expression, and if the next token is an assignment operator
            // and the parsed expression is a valid l-value, then "upgrade" it to an assignment.

            // Let's simplify: `statement()` tries to parse specific statement types.
            // If none match, it calls `expressionStatement()`.
            // `expressionStatement()` will parse an expression.
            // If that expression is `a = b`, and assignment is an EXPRESSION, it works.
            // If assignment is a STATEMENT, then `expressionStatement` should not get `a = b`.
            // The main `statement()` dispatcher needs to identify `a = b` as `AssignmentStatement`.
            // The `isPotentialAssignment()` check and this function are an attempt at that.
            // If `isPotentialAssignment()` is true, we commit to parsing an assignment here.
            // If it fails (e.g. no assignment operator after target), it's a syntax error.
            throw error(peek(), "Expected assignment operator after target.")
        }
    }

    private fun expressionStatementAllowingNoAssignmentInExpression(): Statement {
        // This version of expression parsing should not successfully parse an assignment as the top-level expression
        // if assignment is meant to be a statement.
        // The `expression()` called here should use a grammar where assignment has very low precedence or is not part of the expression rules.
        // Our current `expression()` (which calls `assignment()`, then `logicalOr()`) ISN'T set up for that if `assignment()` parses `=`.
        // The fix was that `assignment()` in the expression hierarchy throws an error on `ASSIGN`.
        val expr = expression()
        val line = expr.line
        consumeSemicolon("Expect ';' or newline after expression statement.")
        return ExpressionStatement(expr, line)
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
        if (check(type)) return advance()
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
        advance() // Consume the erroneous token
        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return // Found a statement boundary
            when (peek().type) { // Heuristic: stop at keywords that likely start new statements/declarations
                TokenType.CLASS, TokenType.FUN, TokenType.VAR, TokenType.FOR, TokenType.IF,
                TokenType.WHILE, TokenType.SWITCH, TokenType.RETURN, TokenType.LBRACE -> return
                else -> advance()
            }
        }
    }
}