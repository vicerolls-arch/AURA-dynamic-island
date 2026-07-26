package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Semantic haptic feedback system modeled after Apple's Taptic Engine design:
 *  - IMPACT_LIGHT  → island collapses / selection tick
 *  - IMPACT_MEDIUM → island expands
 *  - IMPACT_HEAVY  → call alert / warning
 *  - SELECTION     → mode-to-mode switch
 */
enum class HapticType {
    IMPACT_LIGHT,
    IMPACT_MEDIUM,
    IMPACT_HEAVY,
    SELECTION
}

object HapticHelper {

    fun trigger(context: Context, type: HapticType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            val vibrator = manager?.defaultVibrator ?: return
            vibrate(vibrator, type)
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            vibrate(vibrator, type)
        }
    }

    private fun vibrate(vibrator: Vibrator, type: HapticType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = when (type) {
                HapticType.IMPACT_LIGHT  -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                HapticType.IMPACT_MEDIUM -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                HapticType.IMPACT_HEAVY  -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                HapticType.SELECTION     -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            }
            vibrator.vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val (duration, amplitude) = when (type) {
                HapticType.IMPACT_LIGHT  -> 12L to 80
                HapticType.IMPACT_MEDIUM -> 25L to 160
                HapticType.IMPACT_HEAVY  -> 40L to VibrationEffect.DEFAULT_AMPLITUDE
                HapticType.SELECTION     -> 10L to 60
            }
            @Suppress("DEPRECATION")
            vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
        } else {
            val duration = when (type) {
                HapticType.IMPACT_LIGHT  -> 12L
                HapticType.IMPACT_MEDIUM -> 25L
                HapticType.IMPACT_HEAVY  -> 40L
                HapticType.SELECTION     -> 10L
            }
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }
}
