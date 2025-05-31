package org.catrobat.catroid.utils.lunoscript

class Scope(internal val enclosing: Scope? = null) { // internal или без модификатора
    internal val values: MutableMap<String, LunoValue> = mutableMapOf() // internal или без модификатора

    fun define(name: String, value: LunoValue) {
        values[name] = value
    }

    fun assign(nameToken: Token, value: LunoValue): LunoValue {
        val name = nameToken.lexeme
        if (values.containsKey(name)) {
            values[name] = value
            return value
        }
        if (enclosing != null) {
            return enclosing.assign(nameToken, value)
        }
        throw LunoRuntimeError("Undefined variable '${name}'.", nameToken.line)
    }

    fun get(nameToken: Token): LunoValue {
        val name = nameToken.lexeme
        if (values.containsKey(name)) {
            return values[name]!!
        }
        if (enclosing != null) {
            return enclosing.get(nameToken)
        }
        throw LunoRuntimeError("Undefined variable '${name}'.", nameToken.line)
    }

    fun getAt(distance: Int, name: String): LunoValue {
        return ancestor(distance).values[name] ?: LunoValue.Null
    }

    fun assignAt(distance: Int, nameToken: Token, value: LunoValue) {
        ancestor(distance).values[nameToken.lexeme] = value
    }

    private fun ancestor(distance: Int): Scope {
        var scope: Scope = this
        for (i in 0 until distance) {
            scope = scope.enclosing ?: throw LunoRuntimeError("Scope nesting error.")
        }
        return scope
    }

    private val locals: MutableMap<AstNode, Int> = mutableMapOf()
    fun resolve(expr: AstNode, depth: Int) { // AstNode вместо Expression для общности
        locals[expr] = depth
    }
    fun lookUpVariable(name: Token, expr: AstNode): LunoValue { // AstNode вместо Expression
        val distance = locals[expr]
        return if (distance != null) {
            getAt(distance, name.lexeme)
        } else {
            get(name) // Глобальный поиск, если не найдено локально (упрощение)
        }
    }
}