package com.umair.beacons_plugin

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import io.flutter.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.*

/** BeaconsPlugin */
class BeaconsPlugin :
        FlutterPlugin,
        MethodChannel.MethodCallHandler,
        EventChannel.StreamHandler,
        ActivityAware,
        PluginRegistry.ActivityResultListener,
        PluginRegistry.RequestPermissionsResultListener {

    companion object {
        private const val TAG = "BeaconsPlugin"
        private const val RC_ENABLE_BLUETOOTH = 1889
        private const val RC_PERMISSION_LOCATION_FINE = 1890
        private const val RC_PERMISSION_LOCATION_BACKGROUND = 1891

        @JvmStatic
        var runInBackground = false
    }

    private lateinit var beaconManagerWrapper: BeaconManagerWrapper
    private lateinit var channel: MethodChannel
    private lateinit var eventChannel: EventChannel

    private var attachedActivity: Activity? = null
    private var initializeResult: MethodChannel.Result? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        Log.i(TAG, "onAttachedToEngine")

        beaconManagerWrapper = BeaconManagerWrapper(flutterPluginBinding.applicationContext)

        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "beacons_plugin")
        channel.setMethodCallHandler(this)

        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "beacons_plugin_stream")
        eventChannel.setStreamHandler(this)
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        Log.i(TAG, "onDetachedFromEngine")
        channel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
    }

    override fun onAttachedToActivity(activityPluginBinding: ActivityPluginBinding) {
        Log.i(TAG, "onAttachedToActivity")
        attachedActivity = activityPluginBinding.activity
        activityPluginBinding.addActivityResultListener(this)
        activityPluginBinding.addRequestPermissionsResultListener(this)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    override fun onReattachedToActivityForConfigChanges(activityPluginBinding: ActivityPluginBinding) {
        onAttachedToActivity(activityPluginBinding)
    }

    override fun onDetachedFromActivity() {
        Log.i(TAG, "onDetachedFromActivity")
        attachedActivity = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == RC_ENABLE_BLUETOOTH) {
            if (resultCode != Activity.RESULT_OK) initializeResult?.success(false)
            else initialize(::checkAndRequestPermissions, ::checkAndRequestLocation)
            return true
        }
        initializeResult = null
        return false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
        if (requestCode == RC_PERMISSION_LOCATION_FINE || requestCode == RC_PERMISSION_LOCATION_BACKGROUND) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) initializeResult?.success(false)
            else initialize(::checkAndRequestLocation)
            return true
        }
        initializeResult = null
        return false
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        Log.d(TAG, "onMethodCall: method = '${call.method}', arguments = '${call.arguments}'")
        try {
            when (call.method) {
                "initialize" -> {
                    initializeResult = result
                    initialize(::checkAndRequestBluetooth, ::checkAndRequestPermissions, ::checkAndRequestLocation)
                }
                "startMonitoring" -> {
                    beaconManagerWrapper.startScanning()
                    result.success("Started scanning Beacons.")
                }
                "stopMonitoring" -> {
                    beaconManagerWrapper.stopScanning()
                    result.success("Stopped scanning Beacons.")
                }
                "addRegion" -> {
                    val identifier = call.requireArgument<String>("identifier")
                    val uuid = call.requireArgument<String>("uuid")
                    beaconManagerWrapper.addRegion(identifier, uuid)
                    result.success("Region Added: $identifier, UUID: $uuid")
                }
                "clearRegions" -> {
                    beaconManagerWrapper.clearRegions()
                    result.success("Regions Cleared")
                }
                "setNotification" -> {
                    val title = call.requireArgument<String>("title")
                    val text = call.requireArgument<String>("text")
                    beaconManagerWrapper.setNotification(title, text)
                    result.success("Notification Set: $title, $text")
                }
                "runInBackground" -> {
                    runInBackground = call.requireArgument("background")
                    result.success("App will run in background? $runInBackground")
                }
                else -> result.notImplemented()
            }
        } catch (e: Throwable) {
            Log.e(TAG, call.method, e)
            result.error(call.method, e.message ?: e.javaClass.simpleName, null)
        }
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        beaconManagerWrapper.eventSink = events
    }

    override fun onCancel(arguments: Any?) { /* do nothing */ }

    private fun initialize(vararg checks: () -> Boolean) {
        for (check in checks) if (!check()) return
        initializeResult?.success(true)
        initializeResult = null
    }

    private fun isPermissionGranted(permission: String) = attachedActivity?.let {
        ContextCompat.checkSelfPermission(it, permission) == PackageManager.PERMISSION_GRANTED
    } ?: false

    private fun checkAndRequestBluetooth(): Boolean {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter != null) {
            if (adapter.isEnabled) return true
            else attachedActivity?.startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), RC_ENABLE_BLUETOOTH)
        }
        return false
    }

    private fun checkAndRequestLocation(): Boolean {
        val lm = attachedActivity?.getTypedSystemService<LocationManager>(Context.LOCATION_SERVICE)
        if (lm != null) {
            if (LocationManagerCompat.isLocationEnabled(lm)) return true
            else attachedActivity?.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
        return false
    }

    private fun checkAndRequestPermissions(): Boolean {
        if (isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (!isPermissionGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    requestPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION, RC_PERMISSION_LOCATION_BACKGROUND)
                    return false
                }
            }
        } else {
            requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, RC_PERMISSION_LOCATION_FINE)
            return false
        }
        return true
    }

    private fun requestPermission(permission: String, requestCode: Int) {
        val activity = attachedActivity ?: return
        ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
    }
}
