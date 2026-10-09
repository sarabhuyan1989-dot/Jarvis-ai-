package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviceActionController(private val context: Context) {

    fun executeAction(action: DeviceAction): ActionResult {
        Log.d("DeviceActionController", "Executing action: ${action.key}")
        return try {
            when (action) {
                DeviceAction.OPEN_YOUTUBE -> launchYouTube()
                DeviceAction.OPEN_SETTINGS -> launchSettings()
                DeviceAction.OPEN_CAMERA -> launchCamera()
                DeviceAction.OPEN_DIALER -> launchDialer()
                DeviceAction.OPEN_BROWSER -> launchBrowser()
                DeviceAction.OPEN_MAPS -> launchMaps()
                DeviceAction.OPEN_CALCULATOR -> launchCalculator()
                DeviceAction.SET_TIMER -> launchTimer()
                DeviceAction.TELL_TIME -> getSystemTime()
                DeviceAction.STOP_SPEAKING -> ActionResult(success = true, message = "SPEECH OUTPUT TERMINATED")
                DeviceAction.GO_HOME -> launchHomeScreen()
                DeviceAction.NONE -> ActionResult(success = true, message = "NO ACTION REQUIRED")
            }
        } catch (e: Exception) {
            Log.e("DeviceActionController", "Failed to execute ${action.key}: ${e.message}", e)
            ActionResult(success = false, message = "EXECUTION FAILED: ${e.message}")
        }
    }

    private fun launchYouTube(): ActionResult {
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(appIntent)
            ActionResult(success = true, message = "LAUNCHED YOUTUBE APP")
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
            ActionResult(success = true, message = "LAUNCHED YOUTUBE (WEB FALLBACK)")
        }
    }

    private fun launchSettings(): ActionResult {
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return ActionResult(success = true, message = "OPENED ANDROID SETTINGS")
    }

    private fun launchCamera(): ActionResult {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            ActionResult(success = true, message = "LAUNCHED CAMERA")
        } catch (e: Exception) {
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(captureIntent)
            ActionResult(success = true, message = "LAUNCHED CAMERA CAPTURE")
        }
    }

    private fun launchDialer(): ActionResult {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return ActionResult(success = true, message = "OPENED PHONE DIALER")
    }

    private fun launchBrowser(): ActionResult {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return ActionResult(success = true, message = "OPENED WEB BROWSER")
    }

    private fun launchMaps(): ActionResult {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=Odisha")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            ActionResult(success = true, message = "OPENED MAPS")
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
            ActionResult(success = true, message = "OPENED MAPS (WEB)")
        }
    }

    private fun launchCalculator(): ActionResult {
        val calcIntents = listOf(
            Intent().setClassName("com.google.android.calculator", "com.android.calculator2.Calculator"),
            Intent().setClassName("com.android.calculator2", "com.android.calculator2.Calculator"),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR)
        )
        for (intent in calcIntents) {
            try {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return ActionResult(success = true, message = "LAUNCHED CALCULATOR")
            } catch (_: Exception) {}
        }
        return ActionResult(success = false, message = "NO CALCULATOR APP FOUND")
    }

    private fun launchTimer(): ActionResult {
        return try {
            val timerIntent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, 300) // Default 5 minutes
                putExtra(AlarmClock.EXTRA_MESSAGE, "JARVIS Timer")
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(timerIntent)
            ActionResult(success = true, message = "CONFIGURED SYSTEM TIMER (5 MIN)")
        } catch (e: Exception) {
            try {
                val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_MESSAGE, "JARVIS Alarm")
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(alarmIntent)
                ActionResult(success = true, message = "OPENED CLOCK/ALARM INTERFACE")
            } catch (ex: Exception) {
                ActionResult(success = false, message = "CANNOT ACCESS CLOCK APP: ${ex.message}")
            }
        }
    }

    private fun getSystemTime(): ActionResult {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val currentTime = timeFormat.format(Date())
        return ActionResult(success = true, message = "CURRENT TIME: $currentTime")
    }

    private fun launchHomeScreen(): ActionResult {
        return try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(homeIntent)
            ActionResult(success = true, message = "RETURNED TO HOME SCREEN")
        } catch (e: Exception) {
            ActionResult(success = false, message = "FAILED TO MINIMIZE: ${e.message}")
        }
    }
}

data class ActionResult(
    val success: Boolean,
    val message: String
)
