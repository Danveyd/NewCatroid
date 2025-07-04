package org.catrobat.catroid.content // Или в папке NN, как вам удобнее

import java.util.Collections

object FloatArrayManager {

    // Хранилище для наших массивов
    private val arrays: MutableMap<String, MutableList<Float>> = mutableMapOf()

    /**
     * Создает новый массив или очищает существующий.
     * @param name Имя массива.
     */
    fun createArray(name: String) {
        arrays[name] = mutableListOf()
    }

    /**
     * Удаляет массив.
     * @param name Имя массива.
     */
    fun deleteArray(name: String) {
        arrays.remove(name)
    }

    /**
     * Удаляет все массивы. Вызывать при остановке проекта.
     */
    fun deleteAllArrays() {
        arrays.clear()
    }

    /**
     * Получает массив по имени. Возвращает копию для безопасности.
     * Для внутренних операций лучше использовать getInternalArray.
     */
    fun getArray(name: String): FloatArray? {
        return arrays[name]?.toFloatArray()
    }

    /**
     * Получает прямую ссылку на внутренний MutableList для быстрых операций.
     * Использовать с осторожностью.
     */
    internal fun getInternalArray(name: String): MutableList<Float>? {
        return arrays[name]
    }

    /**
     * Добавляет один элемент в конец массива.
     * @param name Имя массива.
     * @param value Значение для добавления.
     */
    fun addElement(name: String, value: Float) {
        arrays[name]?.add(value)
    }

    /**
     * Вставляет элемент в указанную позицию.
     * @param name Имя массива.
     * @param value Значение для вставки.
     * @param index Индекс (начиная с 0).
     */
    fun insertElement(name: String, value: String, index: Int) {
        val arr = value.split(",")
        for (i in arr) {
            val iF = i.toFloat()
            arrays[name]?.let {
                if (index >= 0 && index <= it.size) {
                    it.add(index, iF)
                }
            }
        }
    }

    /**
     * Удаляет элемент по индексу.
     * @param name Имя массива.
     * @param index Индекс (начиная с 0).
     */
    fun removeElement(name: String, index: Int) {
        arrays[name]?.let {
            if (index >= 0 && index < it.size) {
                it.removeAt(index)
            }
        }
    }

    /**
     * Получает количество элементов в массиве.
     * @param name Имя массива.
     * @return Размер или 0, если массив не существует.
     */
    fun getArraySize(name: String): Int {
        return arrays[name]?.size ?: 0
    }

    fun addTableDataToFloatArray(tableName: String, arrayName: String) {
        val array = getInternalArray(arrayName) ?: return

        val width = TableManager.getTableXSize(tableName)
        val height = TableManager.getTableYSize(tableName)

        if (width == 0 || height == 0) return

        // Проходим по таблице и просто добавляем каждый элемент в конец FloatArray
        for (y in 1..height) {
            for (x in 1..width) {
                val value = TableManager.getElementValue(tableName, x, y)?.toFloatOrNull() ?: 0.0f
                array.add(value)
            }
        }
    }

    // В файле NNUtils.kt

    /**
     * Находит индекс максимального элемента в массиве Float.
     *
     * @param array Массив чисел.
     * @return Индекс максимального элемента (начиная с 0), или -1, если массив пуст.
     */
    fun findMaxIndex(array: FloatArray): Int {
        if (array.isEmpty()) {
            return -1
        }

        var maxIndex = 0
        var maxValue = array[0]

        // Начинаем со второго элемента, так как первый уже взяли за максимум
        for (i in 1 until array.size) {
            if (array[i] > maxValue) {
                maxValue = array[i]
                maxIndex = i
            }
        }

        return maxIndex
    }
}