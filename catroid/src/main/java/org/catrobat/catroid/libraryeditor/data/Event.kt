package org.catrobat.catroid.libraryeditor.data

/**
 * Используется как обертка для данных, которые представляют собой одноразовое событие.
 */
open class Event<out T>(private val content: T) {

    var hasBeenHandled = false
        private set // Разрешить чтение, но запретить запись извне

    /**
     * Возвращает контент и помечает его как "обработанный".
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * Возвращает контент, даже если он уже был обработан.
     */
    fun peekContent(): T = content
}