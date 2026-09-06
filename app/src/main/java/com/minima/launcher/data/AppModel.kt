package com.minima.launcher.data

import android.graphics.drawable.Drawable

data class AppModel(
    val packageName: String,
    val className: String,
    val label: String,
    val icon: Drawable?,
)
