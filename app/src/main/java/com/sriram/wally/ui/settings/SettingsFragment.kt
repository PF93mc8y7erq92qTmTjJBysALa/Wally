package com.sriram.wally.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceFragment
import androidx.work.*
import com.sriram.wally.R
import com.sriram.wally.core.RandomWallpaperWorker
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        private const val KEY_RANDOM_WALLPAPER_JOB = "key_random_wallpaper_job"
        private const val TAG_WALLPAPER_WORK = "tag-wallpaper-work"
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (sharedPreferences == null || key == null) return
        if (key == getString(R.string.key_regular_wallpaper)) {
            scheduleOrCancelWallpaperWork(sharedPreferences, key)
        }
    }

    private fun scheduleOrCancelWallpaperWork(pref: SharedPreferences, key: String) {
        val shouldScheduleJob = pref.getBoolean(key, false)
        if (shouldScheduleJob) {
            // schedule a new job
            val jobConstraints = Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val randomWallpaperJob = PeriodicWorkRequestBuilder<RandomWallpaperWorker>(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS) // 15 minutes
                    .addTag(TAG_WALLPAPER_WORK)
                    .setConstraints(jobConstraints)
                    .build()

            WorkManager.getInstance().enqueue(randomWallpaperJob)

            val id = randomWallpaperJob.id.toString()

            pref.edit().putString(KEY_RANDOM_WALLPAPER_JOB, id).apply()
            Timber.i("Started job with ID $id")

        } else {
            // cancel the existing job
            val existingJobKey = pref.getString(KEY_RANDOM_WALLPAPER_JOB, "")
            if (existingJobKey.isNotBlank()) {
                // only stop job if key isn't empty
                WorkManager.getInstance().cancelAllWorkByTag(TAG_WALLPAPER_WORK)
                pref.edit().remove(KEY_RANDOM_WALLPAPER_JOB).apply()
                Timber.i("Stopped existing job with id $existingJobKey")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preference)
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }
}