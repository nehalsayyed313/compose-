package com.nehal.app.data

import java.io.File

data class FrameItem(
    val id: Int,
    val file: File,
    val isSelected: Boolean = false
)
