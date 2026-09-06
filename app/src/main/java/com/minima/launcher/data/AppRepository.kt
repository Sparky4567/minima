package com.minima.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle

/** Loads launchable apps via LauncherApps (profile-aware, work-profile safe). */
class AppRepository(private val context: Context) {

    fun loadApps(): List<AppModel> {
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return emptyList()
        val profiles: List<UserHandle> = try {
            launcherApps.profiles
        } catch (_: Exception) {
            listOf(Process.myUserHandle())
        }
        return profiles.flatMap { profile ->
            try {
                launcherApps.getActivityList(null, profile).mapNotNull { info ->
                    val app = info.applicationInfo
                    AppModel(
                        packageName = app.packageName,
                        className = info.componentName.className,
                        label = info.label.toString(),
                        icon = try { info.getBadgedIcon(0) } catch (_: Exception) { null },
                    )
                }
            } catch (_: Exception) { emptyList() }
        }.sortedBy { it.label.lowercase() }
    }

    fun launch(app: AppModel) {
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return
        val component = android.content.ComponentName(app.packageName, app.className)
        try {
            val profiles = launcherApps.profiles
            val target = profiles.firstOrNull {
                launcherApps.getActivityList(null, it).any { a -> a.componentName == component }
            } ?: Process.myUserHandle()
            launcherApps.startMainActivity(component, target, null, null)
        } catch (_: Exception) {
            // Last-resort legacy launch.
            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let { context.startActivity(it) }
        }
    }
}
