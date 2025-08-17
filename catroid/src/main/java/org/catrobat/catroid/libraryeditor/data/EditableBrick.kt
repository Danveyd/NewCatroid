package org.catrobat.catroid.libraryeditor.data

import com.google.gson.annotations.SerializedName

data class EditableBrick(
    @SerializedName("id") var id: String,
    @SerializedName("function_name") var functionName: String,
    @SerializedName("header_text") var headerText: String,
    @SerializedName("params") val params: MutableList<EditableParam> = mutableListOf()
)