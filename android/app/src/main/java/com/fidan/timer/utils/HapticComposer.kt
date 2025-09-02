package com.fidan.timer.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import com.fidan.timer.R
import kotlinx.coroutines.*

class HapticComposer(private val context: Context) {
    
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(5)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    
    private val soundMap = mutableMapOf<HapticPattern, Int>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    init {
        loadSounds()
    }
    
    private fun loadSounds() {
        // These would be actual sound resources in production
        // soundMap[HapticPattern.SUCCESS] = soundPool.load(context, R.raw.success, 1)
        // soundMap[HapticPattern.WARNING] = soundPool.load(context, R.raw.warning, 1)
        // soundMap[HapticPattern.TICK] = soundPool.load(context, R.raw.tick, 1)
    }
    
    enum class HapticPattern {
        // Timer patterns
        TIMER_START,
        TIMER_PAUSE,
        TIMER_RESUME,
        TIMER_COMPLETE,
        TIMER_TICK,
        
        // Milestone patterns
        QUARTER_COMPLETE,
        HALF_COMPLETE,
        THREE_QUARTER_COMPLETE,
        
        // Achievement patterns
        ACHIEVEMENT_UNLOCK,
        STREAK_MILESTONE,
        NEW_RECORD,
        
        // UI feedback patterns
        BUTTON_TAP,
        SUCCESS,
        WARNING,
        ERROR,
        
        // Special patterns
        TREE_BLOOM,
        MOTIVATIONAL_PULSE,
        FOCUS_REMINDER
    }
    
    fun playPattern(pattern: HapticPattern, intensity: Float = 1.0f) {
        when (pattern) {
            HapticPattern.TIMER_START -> playTimerStart(intensity)
            HapticPattern.TIMER_PAUSE -> playTimerPause(intensity)
            HapticPattern.TIMER_RESUME -> playTimerResume(intensity)
            HapticPattern.TIMER_COMPLETE -> playTimerComplete(intensity)
            HapticPattern.TIMER_TICK -> playTimerTick(intensity)
            HapticPattern.QUARTER_COMPLETE -> playMilestone(0.25f, intensity)
            HapticPattern.HALF_COMPLETE -> playMilestone(0.5f, intensity)
            HapticPattern.THREE_QUARTER_COMPLETE -> playMilestone(0.75f, intensity)
            HapticPattern.ACHIEVEMENT_UNLOCK -> playAchievementUnlock(intensity)
            HapticPattern.STREAK_MILESTONE -> playStreakMilestone(intensity)
            HapticPattern.NEW_RECORD -> playNewRecord(intensity)
            HapticPattern.BUTTON_TAP -> playButtonTap(intensity)
            HapticPattern.SUCCESS -> playSuccess(intensity)
            HapticPattern.WARNING -> playWarning(intensity)
            HapticPattern.ERROR -> playError(intensity)
            HapticPattern.TREE_BLOOM -> playTreeBloom(intensity)
            HapticPattern.MOTIVATIONAL_PULSE -> playMotivationalPulse(intensity)
            HapticPattern.FOCUS_REMINDER -> playFocusReminder(intensity)
        }
    }
    
    private fun playTimerStart(intensity: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(
                longArrayOf(0, 50, 50, 100),
                intArrayOf(0, (128 * intensity).toInt(), 0, (255 * intensity).toInt()),
                -1
            )
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 50, 50, 100), -1)
        }
        playSound(HapticPattern.TIMER_START)
    }
    
    private fun playTimerPause(intensity: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(
                longArrayOf(0, 100, 100, 100),
                intArrayOf(0, (200 * intensity).toInt(), 0, (100 * intensity).toInt()),
                -1
            )
            vibrator.vibrate(effect)
        }
    }
    
    private fun playTimerResume(intensity: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(150, (180 * intensity).toInt())
            vibrator.vibrate(effect)
        }
    }
    
    private fun playTimerComplete(intensity: Float) {
        scope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Celebratory pattern
                val timings = longArrayOf(0, 200, 100, 200, 100, 300, 100, 400)
                val amplitudes = intArrayOf(
                    0,
                    (255 * intensity).toInt(),
                    0,
                    (200 * intensity).toInt(),
                    0,
                    (255 * intensity).toInt(),
                    0,
                    (255 * intensity).toInt()
                )
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            }
        }
        playSound(HapticPattern.TIMER_COMPLETE)
    }
    
    private fun playTimerTick(intensity: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(10, (50 * intensity).toInt())
            vibrator.vibrate(effect)
        }
    }
    
    private fun playMilestone(progress: Float, intensity: Float) {
        val vibrationIntensity = (progress * 255 * intensity).toInt()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val repetitions = (progress * 4).toInt()
            val pattern = mutableListOf<Long>()
            val amplitudes = mutableListOf<Int>()
            
            for (i in 0 until repetitions) {
                pattern.add(0)
                pattern.add(100L)
                pattern.add(50L)
                amplitudes.add(0)
                amplitudes.add(vibrationIntensity)
                amplitudes.add(0)
            }
            
            val effect = VibrationEffect.createWaveform(
                pattern.toLongArray(),
                amplitudes.toIntArray(),
                -1
            )
            vibrator.vibrate(effect)
        }
    }
    
    private fun playAchievementUnlock(intensity: Float) {
        scope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Rising crescendo pattern
                val timings = longArrayOf(0, 50, 30, 70, 30, 90, 30, 110, 30, 200)
                val amplitudes = intArrayOf(
                    0,
                    (50 * intensity).toInt(),
                    0,
                    (100 * intensity).toInt(),
                    0,
                    (150 * intensity).toInt(),
                    0,
                    (200 * intensity).toInt(),
                    0,
                    (255 * intensity).toInt()
                )
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            }
        }
        playSound(HapticPattern.ACHIEVEMENT_UNLOCK)
    }
    
    private fun playStreakMilestone(intensity: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(
                longArrayOf(0, 100, 50, 100, 50, 200),
                intArrayOf(
                    0,
                    (200 * intensity).toInt(),
                    0,
                    (200 * intensity).toInt(),
                    0,
                    (255 * intensity).toInt()
                ),
                -1
            )
            vibrator.vibrate(effect)
        }
    }
    
    private fun playNewRecord(intensity: Float) {
        scope.launch {
            repeat(3) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(200, (255 * intensity).toInt())
                    vibrator.vibrate(effect)
                }
                delay(300)
            }
        }
    }
    
    private fun playButtonTap(intensity: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(5, (100 * intensity).toInt())
            vibrator.vibrate(effect)
        }
    }
    
    private fun playSuccess(intensity: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(
                longArrayOf(0, 30, 30, 60),
                intArrayOf(0, (150 * intensity).toInt(), 0, (255 * intensity).toInt()),
                -1
            )
            vibrator.vibrate(effect)
        }
    }
    
    private fun playWarning(intensity: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(
                longArrayOf(0, 200, 200, 200),
                intArrayOf(0, (180 * intensity).toInt(), 0, (180 * intensity).toInt()),
                -1
            )
            vibrator.vibrate(effect)
        }
    }
    
    private fun playError(intensity: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(
                longArrayOf(0, 100, 50, 100, 50, 100),
                intArrayOf(
                    0,
                    (255 * intensity).toInt(),
                    0,
                    (255 * intensity).toInt(),
                    0,
                    (255 * intensity).toInt()
                ),
                -1
            )
            vibrator.vibrate(effect)
        }
    }
    
    private fun playTreeBloom(intensity: Float) {
        scope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Gentle blooming pattern
                val timings = longArrayOf(0, 300, 100, 400, 100, 500)
                val amplitudes = intArrayOf(
                    0,
                    (100 * intensity).toInt(),
                    0,
                    (150 * intensity).toInt(),
                    0,
                    (80 * intensity).toInt()
                )
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            }
        }
    }
    
    private fun playMotivationalPulse(intensity: Float) {
        scope.launch {
            repeat(2) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(150, (180 * intensity).toInt())
                    vibrator.vibrate(effect)
                }
                delay(200)
            }
        }
    }
    
    private fun playFocusReminder(intensity: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(
                longArrayOf(0, 100, 100, 100, 100, 100),
                intArrayOf(
                    0,
                    (120 * intensity).toInt(),
                    0,
                    (120 * intensity).toInt(),
                    0,
                    (120 * intensity).toInt()
                ),
                -1
            )
            vibrator.vibrate(effect)
        }
    }
    
    private fun playSound(pattern: HapticPattern) {
        soundMap[pattern]?.let { soundId ->
            soundPool.play(soundId, 0.7f, 0.7f, 1, 0, 1.0f)
        }
    }
    
    fun release() {
        scope.cancel()
        soundPool.release()
    }
}