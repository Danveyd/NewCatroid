package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import com.gaurav.avnc.vnc.XKeySymUnicode
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class KeyEventAction : TemporalAction() {
    var scope: Scope? = null
    var character: Formula? = null
    var isDown: Formula? = null

    // Простой маппинг символов в X11 keysyms
    private fun stringToKeysym(input: String): Int {
        if (input.isEmpty()) return 0

        // Проверяем специальные клавиши
        return when (input.uppercase()) {
            "<CTRL_L>" -> 0xFFE3 // Left Control
            "<CTRL_R>" -> 0xFFE4 // Right Control
            "<ALT_L>" -> 0xFFE9  // Left Alt
            "<ALT_R>" -> 0xFFEA  // Right Alt
            "<WIN_L>" -> 0xFFEB  // Left Super/Win
            "<WIN_R>" -> 0xFFEC  // Right Super/Win
            "<SHIFT_L>" -> 0xFFE1 // Left Shift
            "<SHIFT_R>" -> 0xFFE2 // Right Shift
            "<DEL>" -> 0xFFFF   // Delete
            "<BACKSPACE>" -> 0xFF08 // BackSpace
            "<ENTER>" -> 0xFF0D   // Enter
            "\n" -> 0xFF0D   // Enter
            "<ESC>" -> 0xFF1B   // Escape
            "<TAB>" -> 0xFF09   // Tab
            "<UP>" -> 0xFF52   // Up arrow
            "<DOWN>" -> 0xFF54 // Down arrow
            "<LEFT>" -> 0xFF51 // Left arrow
            "<RIGHT>" -> 0xFF53// Right arrow
            else -> {
                // Если это не спец. клавиша, используем XKeySymUnicode
                // Берем только первый символ из строки
                XKeySymUnicode.getKeySymForUnicodeChar(input[0].code)
            }
        }
    }

    override fun update(percent: Float) {
        val keyStr = character?.interpretString(scope) ?: ""
        val down = isDown?.interpretBoolean(scope) ?: true

        if (keyStr.isEmpty()) return

        val keysym = stringToKeysym(keyStr)
        if (keysym == 0) return

        val stageActivity = StageActivity.activeStageActivity.get()
        stageActivity?.sendVmKeyEvent(keysym, down)
    }
}