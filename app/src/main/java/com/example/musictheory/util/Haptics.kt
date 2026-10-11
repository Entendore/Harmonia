package com.example.musictheory.util
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
object HapticsEnabled { @Volatile var on: Boolean = true }
fun View.tap() { if (HapticsEnabled.on) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) }
fun View.confirm() { if (!HapticsEnabled.on) return
    performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM
        else HapticFeedbackConstants.VIRTUAL_KEY) }
