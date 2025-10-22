package org.catrobat.catroid.content

import com.danvexteam.lunoscript_annotations.LunoClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@LunoClass
class TableManager {
    companion object {
        private val tables: MutableMap<String, MutableMap<Pair<Int, Int>, Any?>> = mutableMapOf()

        val DEFAULT_VALUE = "0"

        fun createTable(name: String, x: Int, y: Int) {
            if (!tables.containsKey(name)) {
                val newTable = mutableMapOf<Pair<Int, Int>, Any?>()
                for (i in 1..x) {
                    for (j in 1..y) {
                        newTable[Pair(i, j)] = DEFAULT_VALUE
                    }
                }
                tables[name] = newTable
            }
        }

        fun insertElement(name: String, value: Any?, x: Int, y: Int) {
            tables[name]?.let {
                if (x > 0 && y > 0 && it.containsKey(Pair(x, y))) {
                    it[Pair(x, y)] = value
                }
            }
        }

        fun insertElement(name: String, value: Int, x: Int, y: Int) {
            tables[name]?.let {
                if (x > 0 && y > 0 && it.containsKey(Pair(x, y))) {
                    it[Pair(x, y)] = value
                }
            }
        }

        fun deleteTable(name: String) {
            tables.remove(name)
        }

        fun deleteAllTables() {
            tables.clear()
        }

        fun getTableXSize(name: String): Int {
            return tables[name]?.keys?.maxOfOrNull { it.first } ?: 0
        }

        fun getTableYSize(name: String): Int {
            return tables[name]?.keys?.maxOfOrNull { it.second } ?: 0
        }

        fun getElementValue(name: String, x: Int, y: Int): String? {
            return tables[name]?.get(Pair(x, y)).toString()
        }

        suspend fun stringToTable(name: String, data: String, xDelimiter: String, yDelimiter: String) {
            val table = tables[name] ?: return

            val parsedRows = coroutineScope {
                data.split(yDelimiter).map { rowString ->
                    async(Dispatchers.Default) {
                        rowString.split(xDelimiter).map { it.trim() }
                    }
                }.awaitAll()
            }

            table.clear()

            parsedRows.forEachIndexed { y, columns ->
                columns.forEachIndexed { x, value ->
                    val finalValue: Any = value.toIntOrNull() ?: value
                    table[Pair(x + 1, y + 1)] = finalValue
                }
            }
        }

        fun tableToString(name: String, xDelimiter: String, yDelimiter: String): String {
            val table = tables[name] ?: return ""
            if (table.isEmpty()) {
                return ""
            }

            val xSize = table.keys.maxOfOrNull { it.first } ?: 0
            val ySize = table.keys.maxOfOrNull { it.second } ?: 0

            if (xSize == 0 || ySize == 0) return ""

            val builder = StringBuilder()

            for (y in 1..ySize) {
                for (x in 1..xSize) {
                    val value = table[Pair(x, y)] ?: DEFAULT_VALUE
                    builder.append(value)
                    if (x < xSize) {
                        builder.append(xDelimiter)
                    }
                }
                if (y < ySize) {
                    builder.append(yDelimiter)
                }
            }

            return builder.toString()
        }

        fun getTable(name: String): Map<Pair<Int, Int>, Any?>? {
            return tables[name]
        }

    }
}
