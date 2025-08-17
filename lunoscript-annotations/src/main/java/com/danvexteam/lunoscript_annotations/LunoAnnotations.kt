// В модуле :lunoscript-annotations
// Путь: lunoscript-annotations/src/main/kotlin/org/catrobat/catroid/utils/lunoscript/annotations/LunoAnnotations.kt

package com.danvexteam.lunoscript_annotations

// Для экспорта целого класса с его методами (в будущем)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE) // Аннотации нужны только во время компиляции
annotation class LunoClass(val name: String = "")

// Для экспорта функции или конструктора
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.SOURCE)
annotation class LunoFunction(val name: String = "")

// Для экспорта поля/свойства
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class LunoProperty(val name: String = "")