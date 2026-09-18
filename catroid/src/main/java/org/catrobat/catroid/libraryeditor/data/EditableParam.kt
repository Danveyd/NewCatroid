package org.catrobat.catroid.libraryeditor.data

import com.google.gson.annotations.SerializedName

data class EditableParam(
    @SerializedName("type") var type: String,
    @SerializedName("default_value") var defaultValue: String? = null,
    @SerializedName("name") var name: String? = null
)
