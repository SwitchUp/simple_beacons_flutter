package com.umair.beacons_plugin

import android.content.Context
import android.content.SharedPreferences

object BeaconPreferences: SingletonHolder<SharedPreferences, Context>({
    it.getSharedPreferences("com.umair.beacons_plugin_preferences", Context.MODE_PRIVATE)
}) {
    // Keys
    const val KEY_NOTIFICATION_TITLE = "notificationTitle"
    const val KEY_NOTIFICATION_TEXT = "notificationText"

    // Defaults
    const val DEFAULT_NOTIFICATION_TITLE = "Beacons Service"
    const val DEFAULT_NOTIFICATION_TEXT = "Scanning for Beacons around you"
}