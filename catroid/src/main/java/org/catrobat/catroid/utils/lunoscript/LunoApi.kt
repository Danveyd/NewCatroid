// В модуле :catroid
// Путь: catroid/src/main/java/org/catrobat/catroid/utils/lunoscript/LunoApi.kt

package org.catrobat.catroid.utils.lunoscript

import com.danvexteam.lunoscript_annotations.LunoFunction
import org.catrobat.catroid.utils.lunoscript.asSpecificKotlinType
object LunoApi {

    @LunoFunction
    fun addTwoNumbers(a: Double, b: Double): Double {
        return a + b
    }


    @LunoFunction("sayHello")
    fun printGreeting(name: String) {
        println("Hello, $name!")
    }


    fun thisIsAPrivateFunction() {

    }
}