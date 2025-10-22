package org.catrobat.catroid.content

object FloatArrayManager {
    private val arrays: MutableMap<String, MutableList<Float>> = mutableMapOf()

    fun createArray(name: String) {
        arrays[name] = mutableListOf()
    }

    fun deleteArray(name: String) {
        arrays.remove(name)
    }

    fun deleteAllArrays() {
        arrays.clear()
    }

    fun getArray(name: String): FloatArray? {
        return arrays[name]?.toFloatArray()
    }

    internal fun getInternalArray(name: String): MutableList<Float>? {
        return arrays[name]
    }

    fun addElement(name: String, value: Float) {
        arrays[name]?.add(value)
    }

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

    fun removeElement(name: String, index: Int) {
        arrays[name]?.let {
            if (index >= 0 && index < it.size) {
                it.removeAt(index)
            }
        }
    }

    fun getArraySize(name: String): Int {
        return arrays[name]?.size ?: 0
    }

    fun addTableDataToFloatArray(tableName: String, arrayName: String) {
        val array = getInternalArray(arrayName) ?: return

        val width = TableManager.getTableXSize(tableName)
        val height = TableManager.getTableYSize(tableName)

        if (width == 0 || height == 0) return

        for (y in 1..height) {
            for (x in 1..width) {
                val value = TableManager.getElementValue(tableName, x, y)?.toFloatOrNull() ?: 0.0f
                array.add(value)
            }
        }
    }


    fun findMaxIndex(array: FloatArray): Int {
        if (array.isEmpty()) {
            return -1
        }

        var maxIndex = 0
        var maxValue = array[0]

        for (i in 1 until array.size) {
            if (array[i] > maxValue) {
                maxValue = array[i]
                maxIndex = i
            }
        }

        return maxIndex
    }
}