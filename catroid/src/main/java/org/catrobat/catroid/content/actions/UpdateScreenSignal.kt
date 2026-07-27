package org.catrobat.catroid.content.actions

object UpdateScreenSignal {
    @Volatile
    private var yieldRequested: Boolean = false

    @JvmStatic
    fun requestYield() {
        yieldRequested = true
    }

    @JvmStatic
    fun checkAndResetYield(): Boolean {
        if (yieldRequested) {
            yieldRequested = false
            return true
        }
        return false
    }

    @JvmStatic
    fun reset() {
        yieldRequested = false
    }
}
