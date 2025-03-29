package org.catrobat.catroid.content

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class TableManager {
    companion object {
        private val tables: MutableMap<String, MutableMap<Pair<Int, Int>, Any?>> = mutableMapOf()

        val DEFAULT_VALUE = "0"

        // Создать таблицу(name, x, y)
        fun createTable(name: String, x: Int, y: Int) {
            if (!tables.containsKey(name)) {
                val newTable = mutableMapOf<Pair<Int, Int>, Any?>()
                for (i in 1..x) { // Начинаем с 1
                    for (j in 1..y) { // Начинаем с 1
                        newTable[Pair(i, j)] = DEFAULT_VALUE
                    }
                }
                tables[name] = newTable
            }
        }

        // Вставить элемент в таблицу(name, value, x, y)
        fun insertElement(name: String, value: Any?, x: Int, y: Int) {
            tables[name]?.let {
                if (x > 0 && y > 0 && it.containsKey(Pair(x, y))) { // Проверяем, что координаты валидны
                    it[Pair(x, y)] = value // Вставляем элемент по заданным координатам
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

        // Удалить таблицу(name)
        fun deleteTable(name: String) {
            tables.remove(name)
        }

        // Удалить все таблицы
        fun deleteAllTables() {
            tables.clear()
        }

        // Получить размер таблицы по X(name)
        fun getTableXSize(name: String): Int {
            return tables[name]?.keys?.maxOfOrNull { it.first } ?: 0 // Максимальный индекс по X
        }

        // Получить размер таблицы по Y(name)
        fun getTableYSize(name: String): Int {
            return tables[name]?.keys?.maxOfOrNull { it.second } ?: 0 // Максимальный индекс по Y
        }

        // Получить значение элемента в таблице(name, x, y)
        fun getElementValue(name: String, x: Int, y: Int): String? {
            return tables[name]?.get(Pair(x, y)).toString() // Получаем элемент по заданным координатам
        }

        // Преобразовать строку в таблицу (таблица, строка, разделитель x, разделитель y)
        /*fun stringToTable(name: String, data: String, xDelimiter: String, yDelimiter: String) {
            val rows = data.split(yDelimiter)
            for (i in rows.indices) {
                val columns = rows[i].split(xDelimiter)
                for (j in columns.indices) {
                    // Проверяем, что мы не выходим за пределы таблицы
                    if (i < getTableYSize(name) && j < getTableXSize(name)) {
                        insertElement(name, columns[j].trim().toIntOrNull(), j + 1, i + 1)
                    }
                }
            }
        }*/
        suspend fun stringToTable(name: String, data: String, xDelimiter: String, yDelimiter: String) {
            coroutineScope {
                val rows = data.split(yDelimiter).mapIndexed { index, row ->
                    async(Dispatchers.Default) {
                        row.split(xDelimiter).map { it.trim().toIntOrNull() } to index
                    }
                }.awaitAll() // Ждем завершения всех асинхронных операций

                // Вставляем элементы обратно в таблицу, сохраняя порядок
                rows.forEach { (columns, rowIndex) ->
                    columns.forEachIndexed { columnIndex, value ->
                        insertElement(name, value, columnIndex + 1, rowIndex + 1)
                    }
                }
            }
        }

        // Преобразовать таблицу в строку (таблица, разделитель x, разделитель y)
        fun tableToString(name: String, xDelimiter: String, yDelimiter: String): String {
            val builder = StringBuilder()
            val xSize = getTableXSize(name)
            val ySize = getTableYSize(name)

            for (i in 1..ySize) {
                for (j in 1..xSize) {
                    val value = getElementValue(name, j, i)
                    builder.append(value)
                    if (j < xSize) {
                        builder.append(xDelimiter) // Добавляем разделитель по X
                    }
                }
                if (i < ySize) {
                    builder.append(yDelimiter) // Добавляем разделитель по Y
                }
            }

            return builder.toString()
        }

    }
}
