package org.catrobat.catroid.libraryeditor.data

import com.google.gson.annotations.SerializedName

// Универсальный параметр
data class EditableParam(
    // Для блоков: "TEXT_FIELD". Для формул: "NUMBER", "STRING"
    @SerializedName("type") var type: String,
    @SerializedName("default_value") var defaultValue: String? = null, // Только для формул
    @SerializedName("name") var name: String? = null // Только для блоков
)