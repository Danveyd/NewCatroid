package org.catrobat.catroid.utils.lunoscript

enum class TokenType {
    // Keywords - обновлен список
    VAR, IF, ELSE, FUN, CLASS, RETURN, TRUE, FALSE, NULL, WHILE, FOR, IN, SWITCH, CASE, DEFAULT, BREAK, CONTINUE, THIS, SUPER,

    // Identifiers and Literals
    IDENTIFIER,
    NUMBER_LITERAL,
    STRING_LITERAL,

    // Operators - обновлен список
    ASSIGN,         // =
    PLUS,           // +
    MINUS,          // -
    MULTIPLY,       // *
    DIVIDE,         // /
    MODULO,         // %
    EQ,             // ==
    NEQ,            // !=
    LT,             // <
    GT,             // >
    LTE,            // <=
    GTE,            // >=
    PLUS_ASSIGN,    // +=
    MINUS_ASSIGN,   // -=
    MULTIPLY_ASSIGN,// *=
    DIVIDE_ASSIGN,  // /=
    MODULO_ASSIGN,  // %=
    BANG,           // ! (logical NOT)
    AND,            // &&
    OR,             // ||

    // Punctuation
    LPAREN,         // (
    RPAREN,         // )
    LBRACE,         // {
    RBRACE,         // }
    LBRACKET,       // [ (для списков)
    RBRACKET,       // ]
    COMMA,          // ,
    DOT,            // .
    SEMICOLON,      // ;
    COLON,          // : (для switch case)

    // End of File
    EOF
}

data class Token(
    val type: TokenType,
    val lexeme: String,
    val literal: Any?,
    val line: Int,
    val position: Int
)