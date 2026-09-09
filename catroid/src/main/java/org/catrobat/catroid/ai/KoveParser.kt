package org.catrobat.catroid.ai

enum class TokenType {
    IDENTIFIER, NUMBER, STRING,
    PLUS, MINUS, MULT, DIV, MOD, POW,
    EQUAL_SINGLE, EQUAL_DOUBLE, NOT_EQUAL,
    GREATER, GREATER_EQUAL, LESS, LESS_EQUAL,
    AND, OR, NOT,
    LPAREN, RPAREN, COLON, COMMA, AT,
    IF, ELIF, ELSE, FOREVER, REPEAT, REPEAT_UNTIL,
    TRY, EXCEPT, FINALLY,
    DEF, CLASS, PASS,
    INDENT, DEDENT, NEWLINE, EOF
}

data class Token(val type: TokenType, val value: String, val line: Int, val column: Int)

class KoveLexer(private val input: String) {
    private val tokens = mutableListOf<Token>()
    private val indentStack = mutableListOf(0)
    private var line = 1

    fun tokenize(): List<Token> {
        val rawLines = input.split(Regex("\\r?\\n"))
        for ((index, rawLine) in rawLines.withIndex()) {
            line = index + 1
            if (rawLine.trim().isEmpty()) continue

            var leadingSpaces = 0
            while (leadingSpaces < rawLine.length && rawLine[leadingSpaces] == ' ') {
                leadingSpaces++
            }

            val rest = rawLine.substring(leadingSpaces)
            if (rest.isEmpty() || rest.startsWith("#")) {
                continue
            }

            val currentIndent = indentStack.last()
            if (leadingSpaces > currentIndent) {
                indentStack.add(leadingSpaces)
                tokens.add(Token(TokenType.INDENT, "", line, 1))
            } else if (leadingSpaces < currentIndent) {
                while (indentStack.isNotEmpty() && indentStack.last() > leadingSpaces) {
                    indentStack.removeAt(indentStack.lastIndex)
                    tokens.add(Token(TokenType.DEDENT, "", line, 1))
                }
            }

            tokenizeLine(rest, leadingSpaces + 1)
            tokens.add(Token(TokenType.NEWLINE, "", line, rawLine.length + 1))
        }

        while (indentStack.size > 1) {
            indentStack.removeAt(indentStack.lastIndex)
            tokens.add(Token(TokenType.DEDENT, "", line, 1))
        }
        tokens.add(Token(TokenType.EOF, "", line, 1))
        return tokens
    }

    private fun tokenizeLine(text: String, startCol: Int) {
        var idx = 0
        val len = text.length
        while (idx < len) {
            val char = text[idx]
            val col = startCol + idx
            when {
                char.isWhitespace() -> idx++
                char == '(' -> { tokens.add(Token(TokenType.LPAREN, "(", line, col)); idx++ }
                char == ')' -> { tokens.add(Token(TokenType.RPAREN, ")", line, col)); idx++ }
                char == ':' -> { tokens.add(Token(TokenType.COLON, ":", line, col)); idx++ }
                char == ',' -> { tokens.add(Token(TokenType.COMMA, ",", line, col)); idx++ }
                char == '@' -> { tokens.add(Token(TokenType.AT, "@", line, col)); idx++ }
                char == '+' -> { tokens.add(Token(TokenType.PLUS, "+", line, col)); idx++ }
                char == '-' -> { tokens.add(Token(TokenType.MINUS, "-", line, col)); idx++ }
                char == '*' -> {
                    if (idx + 1 < len && text[idx + 1] == '*') {
                        tokens.add(Token(TokenType.POW, "**", line, col))
                        idx += 2
                    } else {
                        tokens.add(Token(TokenType.MULT, "*", line, col))
                        idx++
                    }
                }
                char == '/' -> { tokens.add(Token(TokenType.DIV, "/", line, col)); idx++ }
                char == '%' -> { tokens.add(Token(TokenType.MOD, "%", line, col)); idx++ }
                char == '=' -> {
                    if (idx + 1 < len && text[idx + 1] == '=') {
                        tokens.add(Token(TokenType.EQUAL_DOUBLE, "==", line, col))
                        idx += 2
                    } else {
                        tokens.add(Token(TokenType.EQUAL_SINGLE, "=", line, col))
                        idx++
                    }
                }
                char == '!' && idx + 1 < len && text[idx + 1] == '=' -> {
                    tokens.add(Token(TokenType.NOT_EQUAL, "!=", line, col))
                    idx += 2
                }
                char == '>' -> {
                    if (idx + 1 < len && text[idx + 1] == '=') {
                        tokens.add(Token(TokenType.GREATER_EQUAL, ">=", line, col))
                        idx += 2
                    } else {
                        tokens.add(Token(TokenType.GREATER, ">", line, col))
                        idx++
                    }
                }
                char == '<' -> {
                    if (idx + 1 < len && text[idx + 1] == '=') {
                        tokens.add(Token(TokenType.LESS_EQUAL, "<=", line, col))
                        idx += 2
                    } else {
                        tokens.add(Token(TokenType.LESS, "<", line, col))
                        idx++
                    }
                }
                char == '"' || char == '\'' -> {
                    val quote = char
                    idx++
                    val sb = StringBuilder()
                    while (idx < len && text[idx] != quote) {
                        if (text[idx] == '\\' && idx + 1 < len) {
                            sb.append(text[idx + 1])
                            idx += 2
                        } else {
                            sb.append(text[idx])
                            idx++
                        }
                    }
                    if (idx < len) idx++
                    tokens.add(Token(TokenType.STRING, sb.toString(), line, col))
                }
                char.isDigit() -> {
                    val sb = StringBuilder()
                    while (idx < len && (text[idx].isDigit() || text[idx] == '.')) {
                        sb.append(text[idx])
                        idx++
                    }
                    tokens.add(Token(TokenType.NUMBER, sb.toString(), line, col))
                }
                char.isLetter() || char == '_' -> {
                    val sb = StringBuilder()
                    while (idx < len && (text[idx].isLetterOrDigit() || text[idx] == '_')) {
                        sb.append(text[idx])
                        idx++
                    }
                    val word = sb.toString()
                    val type = when (word) {
                        "if" -> TokenType.IF
                        "elif" -> TokenType.ELIF
                        "else" -> TokenType.ELSE
                        "try" -> TokenType.TRY
                        "except", "catch" -> TokenType.EXCEPT
                        "finally" -> TokenType.FINALLY
                        "and" -> TokenType.AND
                        "or" -> TokenType.OR
                        "not" -> TokenType.NOT
                        "forever" -> TokenType.FOREVER
                        "repeat" -> TokenType.REPEAT
                        "repeat_until" -> TokenType.REPEAT_UNTIL
                        "def" -> TokenType.DEF
                        "class" -> TokenType.CLASS
                        "pass" -> TokenType.PASS
                        else -> TokenType.IDENTIFIER
                    }
                    tokens.add(Token(type, word, line, col))
                }
                else -> idx++
            }
        }
    }
}

class KoveParser(private val tokens: List<Token>) {
    private var current = 0

    private fun peek() = tokens[current]
    private fun previous() = tokens[current - 1]
    private fun isAtEnd() = peek().type == TokenType.EOF

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun check(type: TokenType) = if (isAtEnd()) false else peek().type == type

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun peekNext(): Token {
        if (current + 1 >= tokens.size) return Token(TokenType.EOF, "", 0, 0)
        return tokens[current + 1]
    }

    private fun consume(type: TokenType, msg: String): Token {
        if (check(type)) return advance()
        throw RuntimeException("Parser error at line ${peek().line}, column ${peek().column}: $msg")
    }

    fun parseExpression(): ParsedFormulaElement = orExpr()

    private fun orExpr(): ParsedFormulaElement {
        var expr = andExpr()
        while (match(TokenType.OR)) {
            expr = ParsedFormulaElement(FormulaElementType.OPERATOR, "LOGICAL_OR", expr, andExpr())
        }
        return expr
    }

    private fun andExpr(): ParsedFormulaElement {
        var expr = equalityExpr()
        while (match(TokenType.AND)) {
            expr = ParsedFormulaElement(FormulaElementType.OPERATOR, "LOGICAL_AND", expr, equalityExpr())
        }
        return expr
    }

    private fun equalityExpr(): ParsedFormulaElement {
        var expr = comparisonExpr()
        while (match(TokenType.EQUAL_DOUBLE, TokenType.NOT_EQUAL)) {
            val op = if (previous().type == TokenType.EQUAL_DOUBLE) "EQUAL" else "NOT_EQUAL"
            expr = ParsedFormulaElement(FormulaElementType.OPERATOR, op, expr, comparisonExpr())
        }
        return expr
    }

    private fun comparisonExpr(): ParsedFormulaElement {
        var expr = termExpr()
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            val op = when (previous().type) {
                TokenType.GREATER -> "GREATER_THAN"
                TokenType.GREATER_EQUAL -> "GREATER_OR_EQUAL"
                TokenType.LESS -> "SMALLER_THAN"
                TokenType.LESS_EQUAL -> "SMALLER_OR_EQUAL"
                else -> ""
            }
            expr = ParsedFormulaElement(FormulaElementType.OPERATOR, op, expr, termExpr())
        }
        return expr
    }

    private fun termExpr(): ParsedFormulaElement {
        var expr = factorExpr()
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            val op = if (previous().type == TokenType.PLUS) "PLUS" else "MINUS"
            expr = ParsedFormulaElement(FormulaElementType.OPERATOR, op, expr, factorExpr())
        }
        return expr
    }

    private fun factorExpr(): ParsedFormulaElement {
        var expr = unaryExpr()
        while (match(TokenType.MULT, TokenType.DIV, TokenType.MOD)) {
            val op = when (previous().type) {
                TokenType.MULT -> "MULT"
                TokenType.DIV -> "DIVIDE"
                TokenType.MOD -> "MOD"
                else -> ""
            }
            expr = ParsedFormulaElement(FormulaElementType.OPERATOR, op, expr, unaryExpr())
        }
        return expr
    }

    private fun unaryExpr(): ParsedFormulaElement {
        if (match(TokenType.NOT)) {
            return ParsedFormulaElement(FormulaElementType.OPERATOR, "LOGICAL_NOT", null, unaryExpr())
        }
        if (match(TokenType.MINUS)) {
            return ParsedFormulaElement(FormulaElementType.OPERATOR, "MINUS", ParsedFormulaElement(FormulaElementType.NUMBER, "0"), primaryExpr())
        }
        return primaryExpr()
    }

    private fun primaryExpr(): ParsedFormulaElement {
        if (match(TokenType.NUMBER)) return ParsedFormulaElement(FormulaElementType.NUMBER, previous().value)
        if (match(TokenType.STRING)) return ParsedFormulaElement(FormulaElementType.STRING, previous().value)
        if (match(TokenType.LPAREN)) {
            val expr = parseExpression()
            consume(TokenType.RPAREN, "Expected ')'")
            return expr
        }
        if (match(TokenType.IDENTIFIER)) {
            val name = previous().value
            if (match(TokenType.LPAREN)) {
                val args = mutableListOf<ParsedFormulaElement>()
                if (!check(TokenType.RPAREN)) {
                    do {
                        args.add(parseExpression())
                    } while (match(TokenType.COMMA))
                }
                consume(TokenType.RPAREN, "Expected ')'")
                return mapSpecialFunction(name, args)
            }
            return ParsedFormulaElement(FormulaElementType.USER_VARIABLE, name)
        }
        throw RuntimeException("Unexpected expression token: ${peek().value}")
    }

    private fun mapSpecialFunction(name: String, args: List<ParsedFormulaElement>): ParsedFormulaElement {
        return when (name) {
            "var" -> ParsedFormulaElement(FormulaElementType.USER_VARIABLE, args.firstOrNull()?.value ?: "")
            "list" -> ParsedFormulaElement(FormulaElementType.USER_LIST, args.firstOrNull()?.value ?: "")
            "sensor" -> ParsedFormulaElement(FormulaElementType.SENSOR, args.firstOrNull()?.value ?: "")
            "touches" -> ParsedFormulaElement(FormulaElementType.COLLISION_FORMULA, args.firstOrNull()?.value ?: "")
            "input" -> ParsedFormulaElement(FormulaElementType.USER_DEFINED_BRICK_INPUT, args.firstOrNull()?.value ?: "")
            else -> {
                val left = args.getOrNull(0)
                val right = args.getOrNull(1)
                val additional = if (args.size > 2) args.drop(2) else emptyList()
                ParsedFormulaElement(FormulaElementType.FUNCTION, name.uppercase(), left, right, additional)
            }
        }
    }

    fun parseBricks(): List<ParsedBrick> {
        val bricks = mutableListOf<ParsedBrick>()
        while (!isAtEnd() && !check(TokenType.DEDENT)) {
            try {
                val b = parseBrick()
                if (b != null) {
                    bricks.add(b)
                }
            } catch (e: Exception) {
                android.util.Log.e("KOVE_PARSER", "Recovering from syntax error: ${e.message}")
                recoverToNextLine()
            }
            while (match(TokenType.NEWLINE)) {}
        }
        return bricks
    }

    private fun recoverToNextLine() {
        while (!isAtEnd() && !check(TokenType.NEWLINE) && !check(TokenType.DEDENT)) {
            advance()
        }
        if (check(TokenType.NEWLINE)) {
            advance()
        }
    }

    private fun parseBrick(): ParsedBrick? {
        while (match(TokenType.NEWLINE)) {}
        if (isAtEnd() || check(TokenType.DEDENT)) return null

        if (match(TokenType.PASS)) {
            match(TokenType.NEWLINE)
            return null
        }

        if (match(TokenType.IF)) {
            val cond = parseExpression()
            consume(TokenType.COLON, "Expected ':' after 'if'")
            match(TokenType.NEWLINE)

            var thenBranch = emptyList<ParsedBrick>()
            if (match(TokenType.INDENT)) {
                thenBranch = parseBricks()
                match(TokenType.DEDENT)
            }

            val elseIfBranches = mutableListOf<ParsedElseIf>()
            while (match(TokenType.NEWLINE)) {}

            while (match(TokenType.ELIF)) {
                val elifCond = parseExpression()
                consume(TokenType.COLON, "Expected ':' after 'elif'")
                match(TokenType.NEWLINE)
                var elifBody = emptyList<ParsedBrick>()
                if (match(TokenType.INDENT)) {
                    elifBody = parseBricks()
                    match(TokenType.DEDENT)
                }
                elseIfBranches.add(ParsedElseIf(elifCond, elifBody))
                while (match(TokenType.NEWLINE)) {}
            }

            var elseBranch: List<ParsedBrick>? = null
            if (match(TokenType.ELSE)) {
                consume(TokenType.COLON, "Expected ':' after 'else'")
                match(TokenType.NEWLINE)
                if (match(TokenType.INDENT)) {
                    elseBranch = parseBricks()
                    match(TokenType.DEDENT)
                }
            }

            return ParsedBrick.If(cond, thenBranch, elseIfBranches, elseBranch)
        }

        if (match(TokenType.TRY)) {
            consume(TokenType.COLON, "Expected ':' after 'try'")
            match(TokenType.NEWLINE)
            var tryBody = emptyList<ParsedBrick>()
            if (match(TokenType.INDENT)) {
                tryBody = parseBricks()
                match(TokenType.DEDENT)
            }

            while (match(TokenType.NEWLINE)) {}
            var catchVar: String? = null
            var catchBody = emptyList<ParsedBrick>()

            if (match(TokenType.EXCEPT)) {
                if (match(TokenType.LPAREN)) {
                    if (match(TokenType.IDENTIFIER)) {
                        catchVar = previous().value
                    }
                    consume(TokenType.RPAREN, "Expected ')' after except argument")
                }
                consume(TokenType.COLON, "Expected ':' after 'except'")
                match(TokenType.NEWLINE)
                if (match(TokenType.INDENT)) {
                    catchBody = parseBricks()
                    match(TokenType.DEDENT)
                }
            }

            while (match(TokenType.NEWLINE)) {}
            var finallyBody: List<ParsedBrick>? = null
            if (match(TokenType.FINALLY)) {
                consume(TokenType.COLON, "Expected ':' after 'finally'")
                match(TokenType.NEWLINE)
                if (match(TokenType.INDENT)) {
                    finallyBody = parseBricks()
                    match(TokenType.DEDENT)
                }
            }

            return ParsedBrick.TryCatch(tryBody, catchVar, catchBody, finallyBody)
        }

        if (match(TokenType.IDENTIFIER, TokenType.REPEAT, TokenType.FOREVER, TokenType.REPEAT_UNTIL)) {
            val name = previous().value
            consume(TokenType.LPAREN, "Expected '(' after block name '$name'")

            val arguments = mutableMapOf<String, ParsedFormulaElement>()
            var positionalIndex = 1

            if (!check(TokenType.RPAREN)) {
                do {
                    if (check(TokenType.IDENTIFIER) && peekNext().type == TokenType.EQUAL_SINGLE) {
                        val argName = advance().value
                        consume(TokenType.EQUAL_SINGLE, "Expected '='")
                        arguments[argName] = parseExpression()
                    } else {
                        val key = if (positionalIndex == 1) "val" else "val$positionalIndex"
                        arguments[key] = parseExpression()
                        positionalIndex++
                    }
                } while (match(TokenType.COMMA))
            }
            consume(TokenType.RPAREN, "Expected ')'")

            if (match(TokenType.COLON)) {
                match(TokenType.NEWLINE)
                var body = emptyList<ParsedBrick>()
                if (match(TokenType.INDENT)) {
                    body = parseBricks()
                    match(TokenType.DEDENT)
                }
                return ParsedBrick.Composite(name, arguments, body)
            }

            match(TokenType.NEWLINE)
            return ParsedBrick.Simple(name, arguments)
        }

        advance()
        return null
    }
}
