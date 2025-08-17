package org.catrobat.catroid.libraryeditor.data

import com.google.gson.annotations.SerializedName

data class EditableFormula(
    @SerializedName("id") var id: String,
    @SerializedName("function_name") var functionName: String,
    @SerializedName("display_name") var displayName: String,
    @SerializedName("params") val params: MutableList<EditableParam> = mutableListOf()
)