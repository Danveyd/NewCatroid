package org.catrobat.catroid.content
package org.catrobat.catroid.content

import com.badlogic.gdx.utils.FloatArray
import java.util.concurrent.ConcurrentHashMap

object FloatArrayManager {
    private val arrays: ConcurrentHashMap<String, FloatArray> = ConcurrentHashMap()

    fun createArray(name: String) {
        arrays.putIfAbsent(name, FloatArray())
    }

    fun deleteArray(name: String) {
        arrays.remove(name)
    }

    fun deleteAllArrays() {
        arrays.clear()
    }

    fun clearArray(name: String) {
        arrays[name]?.let { gdxArray ->
            synchronized(gdxArray) {
                gdxArray.clear()
            }
        }
    }

    fun getArray(name: String): kotlin.FloatArray? {
        val gdxArray = arrays[name] ?: return null
        synchronized(gdxArray) {
            return gdxArray.toArray()
        }
    }

    internal fun getInternalArray(name: String): FloatArray? {
        return arrays[name]
    }

    fun getElement(name: String, index: Int): Float {
        val gdxArray = arrays[name] ?: return 0.0f
        synchronized(gdxArray) {
            return if (index in 0 until gdxArray.size) gdxArray.get(index) else 0.0f
        }
    }

    fun setElement(name: String, index: Int, value: Float) {
        arrays[name]?.let { gdxArray ->
            synchronized(gdxArray) {
                if (index in 0 until gdxArray.size) {
                    gdxArray.set(index, value)
                }
            }
        }
    }

    fun addElement(name: String, value: Float) {
        arrays[name]?.let { gdxArray ->
            synchronized(gdxArray) {
                gdxArray.add(value)
            }
        }
    }

    fun insertElement(name: String, value: String, index: Int) {
        val arr = value.split(",")
        arrays[name]?.let { gdxArray ->
            synchronized(gdxArray) {
                val safeIndex = index.coerceIn(0, gdxArray.size)
                var currentIndex = safeIndex
                for (i in arr) {
                    val iF = i.trim().toFloatOrNull() ?: continue
                    gdxArray.insert(currentIndex, iF)
                    currentIndex++
                }
            }
        }
    }

    fun removeElement(name: String, index: Int) {
        arrays[name]?.let { gdxArray ->
            synchronized(gdxArray) {
                if (index in 0 until gdxArray.size) {
                    gdxArray.removeIndex(index)
                }
            }
        }
    }

    fun getArraySize(name: String): Int {
        val gdxArray = arrays[name] ?: return 0
        synchronized(gdxArray) {
            return gdxArray.size
        }
    }

    fun addTableDataToFloatArray(tableName: String, arrayName: String) {
        val array = getInternalArray(arrayName) ?: return
        val width = TableManager.getTableXSize(tableName)
        val height = TableManager.getTableYSize(tableName)

        if (width <= 0 || height <= 0) return

        synchronized(array) {
            array.ensureCapacity(width * height)
            for (y in 1..height) {
                for (x in 1..width) {
                    val value = TableManager.getElementValue(tableName, x, y)?.toFloatOrNull() ?: 0.0f
                    array.add(value)
                }
            }
        }
    }

    fun findMaxIndex(array: kotlin.FloatArray): Int {
        if (array.isEmpty()) return -1
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

    fun findMinIndex(array: kotlin.FloatArray): Int {
        if (array.isEmpty()) return -1
        var minIndex = 0
        var minValue = array[0]
        for (i in 1 until array.size) {
            if (array[i] < minValue) {
                minValue = array[i]
                minIndex = i
            }
        }
        return minIndex
    }
}
import com.badlogic.gdx.utils.FloatArray
import java.util.concurrent.ConcurrentHashMap

object FloatArrayManager {
    private val arrays: ConcurrentHashMap<String, FloatArray> = ConcurrentHashMap()

    fun createArray(name: String) {
        arrays.putIfAbsent(name, FloatArray())
    }

    fun deleteArray(name: String) {
        arrays.remove(name)
    }

    fun deleteAllArrays() {
        arrays.clear()
    }

    fun getArray(name: String): kotlin.FloatArray? {
        val gdxArray = arrays[name] ?: return null
        return gdxArray.toArray()
    }

    internal fun getInternalArray(name: String): FloatArray? {
        return arrays[name]
    }

    fun addElement(name: String, value: Float) {
        arrays[name]?.let {
            synchronized(it) {
                it.add(value)
            }
        }
    }

    fun insertElement(name: String, value: String, index: Int) {
        val arr = value.split(",")
        arrays[name]?.let { gdxArray ->
            synchronized(gdxArray) {
                val safeIndex = index.coerceIn(0, gdxArray.size)
                var currentIndex = safeIndex
                for (i in arr) {
                    val iF = i.trim().toFloatOrNull() ?: continue
                    gdxArray.insert(currentIndex, iF)
                    currentIndex++
                }
            }
        }
    }

    fun removeElement(name: String, index: Int) {
        arrays[name]?.let { gdxArray ->
            synchronized(gdxArray) {
                if (index in 0 until gdxArray.size) {
                    gdxArray.removeIndex(index)
                }
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

        synchronized(array) {
            array.ensureCapacity(width * height)
            for (y in 1..height) {
                for (x in 1..width) {
                    val value = TableManager.getElementValue(tableName, x, y)?.toFloatOrNull() ?: 0.0f
                    array.add(value)
                }
            }
        }
    }

    fun findMaxIndex(array: kotlin.FloatArray): Int {
        if (array.isEmpty()) return -1
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
