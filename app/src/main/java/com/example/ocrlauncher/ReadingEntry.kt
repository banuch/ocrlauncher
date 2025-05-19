// ReadingEntry.kt
package com.example.ocrlauncher

data class ReadingEntry(
    val serviceId: String,
    val valueType: String,
    val reading: String,
    val timestamp: String,
    val imagePath: String,
    val isEdited: Boolean
)