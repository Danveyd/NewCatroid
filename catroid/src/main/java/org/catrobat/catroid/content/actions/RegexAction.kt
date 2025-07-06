package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserList
import org.catrobat.catroid.utils.ErrorLog

class RegexAction : TemporalAction() {
    var scope: Scope? = null
    var text_f: Formula? = null
    var regex_f: Formula? = null
    var userlist: UserList? = null

    override fun update(percent: Float) {
        val text = text_f?.interpretString(scope) ?: ""
        val regexPattern = regex_f?.interpretString(scope) ?: ""

        if (userlist != null) {
            // Очищаем список перед добавлением новых результатов
            userlist!!.reset()

            // Пытаемся найти совпадения
            try {
                val regex = Regex(regexPattern)

                // Находим все совпадения
                val matches = regex.findAll(text)

                // Добавляем найденные результаты в userlist
                for (match in matches) {
                    userlist!!.addListItem(match.value)
                }
            } catch (e: Exception) {
                ErrorLog.log(e.message?: "**message not provided :(**")
                Log.e("RegexAction", "Ошибка при обработке регулярного выражения: ${e.message}")
            }
        } else {
            Log.e("RegexAction", "UserList не установлен!")
        }
    }
}
