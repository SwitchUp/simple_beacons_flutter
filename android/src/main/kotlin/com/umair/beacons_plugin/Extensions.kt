package com.umair.beacons_plugin

import android.content.Context
import android.content.SharedPreferences
import io.flutter.plugin.common.MethodCall

@Suppress("unchecked_cast")
fun <T> Context.getTypedSystemService(name: String) = getSystemService(name) as? T

fun <T: Any> MethodCall.requireArgument(key: String) = requireNotNull(argument<T>(key)) { "argument '$key' is null" }

fun SharedPreferences.edit(action: SharedPreferences.Editor.() -> Unit) {
    val editor = edit()
    action(editor)
    editor.apply()
}