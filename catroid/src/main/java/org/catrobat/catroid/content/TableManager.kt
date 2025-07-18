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
        /**
         * МОМЕНТАЛЬНО преобразует строку в таблицу.
         * Полностью перезаписывает содержимое таблицы данными из строки.
         */
        suspend fun stringToTable(name: String, data: String, xDelimiter: String, yDelimiter: String) {
            // 1. Получаем прямую ссылку на внутреннюю Map таблицы. Если ее нет, ничего не делаем.
            val table = tables[name] ?: return

            // 2. Распараллеливаем разбор строки. Это самая тяжелая часть.
            val parsedRows = coroutineScope {
                data.split(yDelimiter).map { rowString ->
                    async(Dispatchers.Default) { // Каждую строку разбираем в отдельном потоке
                        rowString.split(xDelimiter).map { it.trim() }
                    }
                }.awaitAll() // Ждем, пока все строки не будут разобраны
            }

            // 3. Быстро очищаем старые данные
            table.clear()

            // 4. Заполняем таблицу новыми данными, работая напрямую с Map
            parsedRows.forEachIndexed { y, columns ->
                columns.forEachIndexed { x, value ->
                    // Конвертируем в число, если возможно, иначе оставляем строкой
                    val finalValue: Any = value.toIntOrNull() ?: value
                    table[Pair(x + 1, y + 1)] = finalValue
                }
            }
        }

        /**
         * МОМЕНТАЛЬНО преобразует таблицу в строку.
         */
        fun tableToString(name: String, xDelimiter: String, yDelimiter: String): String {
            // 1. Получаем таблицу один раз. Если ее нет, возвращаем пустую строку.
            val table = tables[name] ?: return ""
            if (table.isEmpty()) {
                return ""
            }

            // 2. Вычисляем размеры ОДИН РАЗ, перебрав ключи один раз.
            val xSize = table.keys.maxOfOrNull { it.first } ?: 0
            val ySize = table.keys.maxOfOrNull { it.second } ?: 0

            if (xSize == 0 || ySize == 0) return ""

            // 3. Используем StringBuilder для эффективного построения строки.
            val builder = StringBuilder()

            for (y in 1..ySize) {
                for (x in 1..xSize) {
                    // 4. Получаем значение НАПРЯМУЮ из Map. Это сверхбыстро.
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

        /**
         * Возвращает всю таблицу (Map) по имени для быстрого доступа.
         * Это намного эффективнее, чем многократно вызывать getElementValue.
         * @return Map<Pair<Int, Int>, Any?>? или null, если таблица не найдена.
         */
        fun getTable(name: String): Map<Pair<Int, Int>, Any?>? {
            return tables[name]
        }

    }
}
