package org.catrobat.catroid.utils.lunoscript

class Lexer(private val source: String) {
    private val tokens: MutableList<Token> = mutableListOf()
    private var start = 0
    private var current = 0
    private var line = 1
    private var lineStart = 0

    private val keywords: Map<String, TokenType> = mapOf(
        "var" to TokenType.VAR, "if" to TokenType.IF, "else" to TokenType.ELSE,
        "true" to TokenType.TRUE, "false" to TokenType.FALSE, "null" to TokenType.NULL,
        "fun" to TokenType.FUN, "return" to TokenType.RETURN, "class" to TokenType.CLASS,
        "this" to TokenType.THIS, "super" to TokenType.SUPER, // 'super' пока не используется
        "while" to TokenType.WHILE, "for" to TokenType.FOR, "in" to TokenType.IN,
        "break" to TokenType.BREAK, "continue" to TokenType.CONTINUE,
        "switch" to TokenType.SWITCH, "case" to TokenType.CASE, "default" to TokenType.DEFAULT
    )

    fun scanTokens(): List<Token> {
        while (!isAtEnd()) {
            start = current
            scanToken()
        }
        tokens.add(Token(TokenType.EOF, "", null, line, current - lineStart))
        return tokens
    }

    private fun isAtEnd(): Boolean = current >= source.length

    private fun advance(): Char = source[current++]

    private fun addToken(type: TokenType, literal: Any? = null) {
        val text = source.substring(start, current)
        tokens.add(Token(type, text, literal, line, start - lineStart))
    }

    private fun match(expected: Char): Boolean {
        if (isAtEnd() || source[current] != expected) return false
        current++
        return true
    }

    private fun peek(): Char = if (isAtEnd()) '\u0000' else source[current]
    private fun peekNext(): Char = if (current + 1 >= source.length) '\u0000' else source[current + 1]

    private fun string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') { line++; lineStart = current + 1 }
            // TODO: Handle escape sequences like \n, \t, \"
            advance()
        }
        if (isAtEnd()) throw LunoSyntaxError("Unterminated string.", line, start - lineStart)
        advance() // Closing "
        val value = source.substring(start + 1, current - 1) // TODO: Unescape sequences
        addToken(TokenType.STRING_LITERAL, value)
    }

    private fun number() {
        while (isDigit(peek())) advance()
        if (peek() == '.' && isDigit(peekNext())) {
            advance() // Consume .
            while (isDigit(peek())) advance()
        }
        addToken(TokenType.NUMBER_LITERAL, source.substring(start, current).toDouble())
    }

    private fun identifier() {
        while (isAlphaNumeric(peek())) advance()
        val text = source.substring(start, current)
        addToken(keywords[text] ?: TokenType.IDENTIFIER)
    }

    private fun isDigit(c: Char): Boolean = c in '0'..'9'
    private fun isAlpha(c: Char): Boolean = (c in 'a'..'z') || (c in 'A'..'Z') || c == '_'
    private fun isAlphaNumeric(c: Char): Boolean = isAlpha(c) || isDigit(c)

    private fun scanToken() {
        val c = advance()
        when (c) {
            '(' -> addToken(TokenType.LPAREN)
            ')' -> addToken(TokenType.RPAREN)
            '{' -> addToken(TokenType.LBRACE)
            '}' -> addToken(TokenType.RBRACE)
            '[' -> addToken(TokenType.LBRACKET)
            ']' -> addToken(TokenType.RBRACKET)
            ',' -> addToken(TokenType.COMMA)
            '.' -> addToken(TokenType.DOT)
            ';' -> addToken(TokenType.SEMICOLON)
            ':' -> addToken(TokenType.COLON)

            '-' -> addToken(if (match('=')) TokenType.MINUS_ASSIGN else TokenType.MINUS)
            '+' -> addToken(if (match('=')) TokenType.PLUS_ASSIGN else TokenType.PLUS)
            '*' -> addToken(if (match('=')) TokenType.MULTIPLY_ASSIGN else TokenType.MULTIPLY)
            '%' -> addToken(if (match('=')) TokenType.MODULO_ASSIGN else TokenType.MODULO)
            '!' -> addToken(if (match('=')) TokenType.NEQ else TokenType.BANG)
            '=' -> addToken(if (match('=')) TokenType.EQ else TokenType.ASSIGN)
            '<' -> addToken(if (match('=')) TokenType.LTE else TokenType.LT)
            '>' -> addToken(if (match('=')) TokenType.GTE else TokenType.GT)

            '&' -> if (match('&')) addToken(TokenType.AND) else throw LunoSyntaxError("Unexpected character: '&'", line, current - lineStart -1)
            '|' -> if (match('|')) addToken(TokenType.OR) else throw LunoSyntaxError("Unexpected character: '|'", line, current - lineStart -1)

            '/' -> {
                when {
                    match('/') -> while (peek() != '\n' && !isAtEnd()) advance() // Comment
                    match('=') -> addToken(TokenType.DIVIDE_ASSIGN)
                    else -> addToken(TokenType.DIVIDE)
                }
            }

            // Whitespace
            ' ', '\r', '\t' -> { /* Ignore */ }
            '\n' -> { line++; lineStart = current }

            '"' -> string()
            else -> {
                if (isDigit(c)) number()
                else if (isAlpha(c)) identifier()
                else throw LunoSyntaxError("Unexpected character: '$c'", line, current - lineStart -1)
            }
        }
    }
}