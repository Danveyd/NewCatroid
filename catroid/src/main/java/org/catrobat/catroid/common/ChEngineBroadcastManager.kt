package org.catrobat.catroid.common

import java.util.concurrent.ConcurrentHashMap

object ChEngineBroadcastManager {
    private val lastParams = ConcurrentHashMap<String, String>()

    fun setParams(signal: String, params: String) {
        lastParams[signal] = params
    }

    fun getParams(signal: String): String {
        return lastParams[signal] ?: ""
    }
}
