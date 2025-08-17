package org.catrobat.catroid.libraryeditor.data

import com.google.gson.annotations.SerializedName
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R

data class LibraryDraft(
    @SerializedName("id") val id: String,
    @SerializedName("name") var name: String = CatroidApplication.getAppContext().getString(R.string.libs_newlib),
    @SerializedName("author") var author: String = "Автор",
    @SerializedName("code") var code: String = "fun sum(a, b) {\n    return Number(a) + Number(b);\n}\n\nfun toast(sprite, text) {\n    MakeToast(\"Text is: \" + String(text));\n}",
    @SerializedName("formulas") val formulas: MutableList<EditableFormula> = mutableListOf(),
    @SerializedName("bricks") val bricks: MutableList<EditableBrick> = mutableListOf()
)