package org.catrobat.catroid.content

import com.danvexteam.lunoscript_annotations.LunoClass

@LunoClass
class GlobalManager {
    companion object {
        var stopSounds: Boolean = true
        var saveScenes: Boolean = true
    }
}