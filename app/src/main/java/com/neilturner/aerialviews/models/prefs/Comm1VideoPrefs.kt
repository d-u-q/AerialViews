package com.neilturner.aerialviews.models.prefs

import com.chibatching.kotpref.KotprefModel
import com.chibatching.kotpref.enumpref.enumValuePref
import com.neilturner.aerialviews.models.VideoQuality
import com.neilturner.aerialviews.utils.DeviceHelper

object Comm1VideoPrefs : KotprefModel() {
    override val kotprefName = "${context.packageName}_preferences"

    var enabled by booleanPref(true, "comm1_videos_enabled")
    var quality by enumValuePref(if (DeviceHelper.hasHevcSupport()) VideoQuality.VIDEO_1080_SDR else VideoQuality.VIDEO_1080_H264, "comm1_videos_quality")
}
