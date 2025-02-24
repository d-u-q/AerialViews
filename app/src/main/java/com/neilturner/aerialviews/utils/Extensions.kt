package com.neilturner.aerialviews.utils

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

// https://stackoverflow.com/a/36795003/247257
fun Any?.toStringOrEmpty() = this?.toString() ?: ""

// https://stackoverflow.com/a/74741495/247257
fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int = 0): PackageInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION")
        getPackageInfo(packageName, flags)
    }
