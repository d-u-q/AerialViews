@file:Suppress("unused")

package com.neilturner.aerialviews.ui.settings

import android.Manifest
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import com.google.modernstorage.permissions.StoragePermissions
import com.google.modernstorage.permissions.StoragePermissions.Action
import com.google.modernstorage.permissions.StoragePermissions.FileType
import com.neilturner.aerialviews.R
import com.neilturner.aerialviews.models.prefs.LocalVideoPrefs
import com.neilturner.aerialviews.models.videos.AerialVideo
import com.neilturner.aerialviews.utils.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnyVideosFragment :
    PreferenceFragmentCompat(),
    PreferenceManager.OnPreferenceTreeClickListener,
    SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var storagePermissions: StoragePermissions
    private lateinit var requestPermission: ActivityResultLauncher<String>

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_any_videos, rootKey)
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)

        storagePermissions = StoragePermissions(requireContext())
        requestPermission = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (!isGranted) {
                resetPreference()
            }
        }

        limitTextInput()
    }

    override fun onDestroy() {
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroy()
    }

    override fun onResume() {
        val canReadVideos = storagePermissions.hasAccess(
            action = Action.READ,
            types = listOf(FileType.Video),
            createdBy = StoragePermissions.CreatedBy.AllApps
        )

        if (!canReadVideos &&
            requiresPermission()
        ) {
            resetPreference()
        }
        super.onResume()
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key.isNullOrEmpty())
            return super.onPreferenceTreeClick(preference)

        if (preference.key.contains("local_videos_test_filter")) {
            lifecycleScope.launch {
                testLocalVideosFilter()
            }
            return true
        }

        return super.onPreferenceTreeClick(preference)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == "local_videos_enabled" &&
            requiresPermission()
        ) {

            val canReadVideos = storagePermissions.hasAccess(
                action = Action.READ,
                types = listOf(FileType.Video),
                createdBy = StoragePermissions.CreatedBy.AllApps
            )

            if (!canReadVideos) {
                requestPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun limitTextInput() {
        preferenceScreen.findPreference<EditTextPreference>("local_videos_filter_folder_name")?.setOnBindEditTextListener { it.setSingleLine() }
    }

    private suspend fun testLocalVideosFilter() {
        if (LocalVideoPrefs.filter_folder_name.isEmpty() &&
            LocalVideoPrefs.filter_enabled
        ) {
            showDialog("Error", "No folder has been specified.")
            return
        }

        val videos = mutableListOf<AerialVideo>()
        val localVideos = FileHelper.findAllMedia(requireContext())
        var excluded = 0
        var filtered = 0

        for (video in localVideos) {
            val uri = Uri.parse(video)
            val filename = uri.lastPathSegment!!

            if (!FileHelper.isVideoFilename(filename)) {
                // Log.i(TAG, "Probably not a video: $filename")
                excluded++
                continue
            }

            if (LocalVideoPrefs.filter_enabled && FileHelper.shouldFilter(uri, LocalVideoPrefs.filter_folder_name)) {
                // Log.i(TAG, "Filtering out video: $filename")
                filtered++
                continue
            }

            videos.add(AerialVideo(uri, ""))
        }

        var message = "Videos found by Media Scanner: ${localVideos.size}\n"
        message += "Videos with supported file extensions: ${localVideos.size - excluded}\n"
        message += if (LocalVideoPrefs.filter_enabled) {
            "Videos removed by filter: $filtered\n"
        } else {
            "Videos removed by filter: (disabled)\n"
        }

        message += "Videos selected for playback: ${localVideos.size - (filtered + excluded)}"
        showDialog("Results", message)
    }

    private fun requiresPermission(): Boolean {
        return LocalVideoPrefs.enabled
    }

    private fun resetPreference() {
        val pref = findPreference<SwitchPreference>("local_videos_enabled")
        pref?.isChecked = false
    }

    private suspend fun showDialog(title: String = "", message: String) = withContext(Dispatchers.Main) {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(title)
            setMessage(message)
            setPositiveButton("OK", null)
            create().show()
        }
    }

    companion object {
        private const val TAG = "AnyVideosFragment"
    }
}
