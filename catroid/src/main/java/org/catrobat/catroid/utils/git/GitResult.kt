package org.catrobat.catroid.utils.git

import org.catrobat.catroid.content.Project

/**
 * Запечатанный класс для представления результата git-операции.
 */
sealed class GitResult<out T> {
    data class Success<T>(val data: T) : GitResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : GitResult<Nothing>()
    data class MergeConflict(val conflicts: List<Conflict>) : GitResult<Nothing>()
}

/**
 * Описывает результат слияния.
 * @param mergedProject Объединенная версия проекта.
 * @param conflicts Список конфликтов, которые не удалось разрешить автоматически.
 */
data class MergeResult(
    val mergedProject: Project,
    val conflicts: List<Conflict>
)

class MergeConflictException(val conflicts: List<Conflict>) : Exception("Merge conflicts detected")

/**
 * Описывает один конкретный конфликт слияния.
 * @param path Путь конфликтующего элемента (например, Сцена/Фон/).
 * @param fieldName Имя свойства, которое конфликтует.
 * @param localValue Значение этого свойства на устройстве.
 * @param remoteValue Значение этого свойства на сервере.
 */
data class Conflict(
    val path: String,
    val fieldName: String,
    val baseValue: Any?,
    val localValue: Any?,
    val remoteValue: Any?
)

data class MergeResultData(
    val mergedProject: Project,
    val conflicts: List<Conflict>
)