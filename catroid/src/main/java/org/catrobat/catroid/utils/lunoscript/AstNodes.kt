package org.catrobat.catroid.utils.lunoscript

sealed interface AstNode {
    val line: Int
}

sealed interface Expression : AstNode
sealed interface Statement : AstNode

data class ProgramNode(val statements: List<Statement>, override val line: Int = 1) : AstNode

// --- Expressions ---
data class LiteralExpr(val value: LunoValue, override val line: Int) : Expression
data class VariableExpr(val name: Token, override val line: Int) : Expression
data class ThisExpr(val keyword: Token, override val line: Int) : Expression // `this`

data class BinaryExpr(val left: Expression, val operator: Token, val right: Expression, override val line: Int) : Expression
data class LogicalExpr(val left: Expression, val operator: Token, val right: Expression, override val line: Int) : Expression // Для && и || (с short-circuit)
data class UnaryExpr(val operator: Token, val right: Expression, override val line: Int) : Expression

data class CallExpr(
    val callee: Expression,
    val arguments: List<Expression>,
    val parenToken: Token, // Токен ')' для номера строки ошибки по количеству аргументов
    override val line: Int
) : Expression

data class GetExpr(val obj: Expression, val name: Token, override val line: Int) : Expression // obj.property или obj[index]
data class SetExpr(val obj: Expression, val name: Token, val value: Expression, override val line: Int) : Expression // obj.property = value или obj[index] = value
data class IndexAccessExpr(val callee: Expression, val bracket: Token, val index: Expression, override val line: Int) : Expression // list[index]
data class ListLiteralExpr(val elements: List<Expression>, val bracket: Token, override val line: Int) : Expression // [1, "a"]
data class MapLiteralExpr(val entries: Map<Token, Expression>, val brace: Token, override val line: Int) : Expression // {"key": val} (ключи могут быть IDENTIFIER или STRING_LITERAL)

// --- Statements ---
data class ExpressionStatement(val expression: Expression, override val line: Int) : Statement
data class VarDeclarationStatement(val name: Token, val initializer: Expression?, override val line: Int) : Statement
// Присваивание теперь всегда Statement. Для compound assignments можно отдельные узлы или обрабатывать в AssignmentStatement
data class AssignmentStatement(val target: Expression, /* было Token name, теперь Expression для SetExpr */ val value: Expression, val operatorToken: Token, override val line: Int) : Statement

data class BlockStatement(val statements: List<Statement>, override val line: Int) : Statement // line - это строка {
data class IfStatement(val condition: Expression, val thenBranch: Statement, val elseBranch: Statement?, val ifToken: Token, override val line: Int) : Statement
data class WhileStatement(val condition: Expression, val body: Statement, val whileToken: Token, override val line: Int) : Statement
data class ForInStatement(val variable: Token, val iterable: Expression, val body: Statement, val forToken: Token, override val line: Int) : Statement

data class FunctionParameter(val name: Token /*, val type: TypeAnnotation? = null */) // Типы пока не добавляем
data class FunDeclarationStatement(val name: Token, val params: List<Token>, val body: BlockStatement, override val line: Int) : Statement
data class ReturnStatement(val keyword: Token, val value: Expression?, override val line: Int) : Statement
data class BreakStatement(val keyword: Token, override val line: Int) : Statement
data class ContinueStatement(val keyword: Token, override val line: Int) : Statement

data class ClassDeclarationStatement(val name: Token, val methods: List<FunDeclarationStatement>, val superclass: VariableExpr?, override val line: Int) : Statement

data class SwitchCase(val valueExpressions: List<Expression>?, val body: Statement, val caseOrDefaultToken: Token, val isDefault: Boolean = false)
data class SwitchStatement(
    val expression: Expression,
    val cases: List<SwitchCase>,
    val switchToken: Token,
    override val line: Int
) : Statement

data class ImportStatement(
    val path: List<Token>, // Путь к классу, например ["com", "badlogic", "gdx", "graphics", "Pixmap"]
    override val line: Int
) : Statement

data class TryCatchStatement(
    val tryBlock: Statement,
    // catchVariable может быть null, если блока catch нет
    val catchVariable: Token?,
    val catchBlock: Statement?,
    // finallyBlock может быть null
    val finallyBlock: Statement?,
    override val line: Int
) : Statement