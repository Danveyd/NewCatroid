package org.catrobat.catroid.content

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import com.danvexteam.lunoscript_annotations.LunoClass
import org.catrobat.catroid.R

@LunoClass
class NewDialogManager {
    companion object {
        private var context: Activity? = null
        private val dialogMap = mutableMapOf<String, DialogData>()

        fun createEmptyDialog(name: String, title: String, message: String) {
            context = MyActivityManager.stage_activity

            if (dialogMap.containsKey(name)) {
                val existingDialogData = dialogMap[name]
                existingDialogData?.dialogView?.removeAllViews()
                existingDialogData?.radioGroup = null
            }

            val dialogData = DialogData(title, message)
            dialogData.dialogView = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }
            dialogMap[name] = dialogData
        }



        fun setPositiveButton(name: String, buttonText: String) {
            //context = MyActivityManager.stage_activity
            dialogMap[name]?.let { dialogData ->
                dialogData.positiveButtonText = buttonText
            }
        }

        fun setNegativeButton(name: String, buttonText: String) {
            //context = MyActivityManager.stage_activity
            dialogMap[name]?.let { dialogData ->
                dialogData.negativeButtonText = buttonText
            }
        }

        fun setNeutralButton(name: String, buttonText: String) {
            //context = MyActivityManager.stage_activity
            dialogMap[name]?.let { dialogData ->
                dialogData.neutralButtonText = buttonText
            }
        }

        fun addEditText(name: String, defaultValue: String = "") {
            //context = MyActivityManager.stage_activity
            dialogMap[name]?.let { dialogData ->
                val editText = EditText(context).apply {
                    setText(defaultValue)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                dialogData.dialogView?.addView(editText)
            }
        }

        fun addRadio(name: String, option: String) {
            //context = MyActivityManager.stage_activity
            dialogMap[name]?.let { dialogData ->
                var radioGroup = dialogData.radioGroup
                if (radioGroup == null) {
                    radioGroup = RadioGroup(context).apply {
                        layoutParams = RadioGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        orientation = LinearLayout.VERTICAL
                        setPadding(
                            resources.getDimensionPixelSize(R.dimen.dialog_content_area_padding),
                            resources.getDimensionPixelSize(R.dimen.dialog_content_area_padding),
                            resources.getDimensionPixelSize(R.dimen.dialog_content_area_padding_input),
                            resources.getDimensionPixelSize(R.dimen.dialog_content_area_padding_input)
                        )
                        dialogData.dialogView?.addView(this)
                        dialogData.radioGroup = this
                    }
                }
                val radioButton = RadioButton(context).apply {
                    text = option
                    setTextColor(Color.WHITE)
                }
                radioGroup.addView(radioButton)
            }
        }

        fun setCallback(name: String, callback: (result: String) -> Unit) {
            //context = MyActivityManager.stage_activity
            dialogMap[name]?.let { dialogData ->
                dialogData.callback = { result: String ->
                    val enteredValues = mutableListOf<String>()

                    dialogData.dialogView?.let { viewGroup ->
                        for (i in 0 until viewGroup.childCount) {
                            val view = viewGroup.getChildAt(i)
                            if (view is EditText) {
                                enteredValues.add(view.text.toString())
                            }
                        }
                    }

                    val result = result

                    callback(result)
                }
            }
        }


        fun showDialog(name: String) {
            context = MyActivityManager.stage_activity
            dialogMap[name]?.let { dialogData ->
                if(dialogMap[name]?.dialogView?.parent !== null) {
                    (dialogMap[name]?.dialogView?.parent as ViewGroup).removeView(dialogMap[name]?.dialogView)
                }

                dialogData.alertDialogBuilder = AlertDialog.Builder(
                    ContextThemeWrapper(context!!, R.style.Theme_AppCompat_Dialog)
                )
                    .setTitle(dialogData.title)
                    .setMessage(dialogData.message)
                    .setCancelable(false)
                    .setView(dialogData.dialogView)

                dialogData.positiveButtonText?.let {
                    dialogData.alertDialogBuilder?.setPositiveButton(it) { dialog, _ ->
                        dialog.dismiss()
                        val selectedValue = dialogData.getEnteredValue()
                        val result = "1\n$selectedValue"
                        dialogData.callback?.invoke(result)
                    }
                }

                dialogData.negativeButtonText?.let {
                    dialogData.alertDialogBuilder?.setNegativeButton(it) { dialog, _ ->
                        dialog.dismiss()
                        val selectedValue = dialogData.getEnteredValue()
                        val result = "-1\n$selectedValue"
                        dialogData.callback?.invoke(result)
                    }
                }

                dialogData.neutralButtonText?.let {
                    dialogData.alertDialogBuilder?.setNeutralButton(it) { dialog, _ ->
                        dialog.dismiss()
                        val selectedValue = dialogData.getEnteredValue()
                        val result = "0\n$selectedValue"
                        dialogData.callback?.invoke(result)
                    }
                }

                context?.runOnUiThread {
                    dialogData.alertDialogBuilder?.create()?.show()
                }
            }
        }

        private class DialogData(var title: String, var message: String) {
            var alertDialogBuilder: AlertDialog.Builder? = null
            var dialogView: ViewGroup? = null
            var callback: ((result: String) -> Unit)? = null
            var radioGroup: RadioGroup? = null
            var positiveButtonText: String? = null
            var negativeButtonText: String? = null
            var neutralButtonText: String? = null

            fun getEnteredValue(): String {
                val stringBuilder = StringBuilder()

                dialogView?.let {
                    for (i in 0 until it.childCount) {
                        val view = it.getChildAt(i)
                        when (view) {
                            is EditText -> {
                                if (view.text.isNotEmpty()) {
                                    stringBuilder.append(view.text.toString()).append("\n")
                                } else {
                                    stringBuilder.append("\n")
                                }
                            }
                            is RadioGroup -> {
                                val selectedId = view.checkedRadioButtonId
                                if (selectedId != -1) {
                                    val selectedRadioButton = view.findViewById<RadioButton>(selectedId)
                                    stringBuilder.insert(0, selectedRadioButton.text.toString() + "\n")
                                } else {
                                    stringBuilder.insert(0, "\n")
                                }
                            }
                        }
                    }
                }

                return stringBuilder.toString().trim()
            }
        }
    }
}
