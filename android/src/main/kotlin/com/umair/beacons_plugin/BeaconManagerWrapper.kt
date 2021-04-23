package com.umair.beacons_plugin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.RemoteException
import androidx.core.app.NotificationCompat
import io.flutter.Log
import io.flutter.plugin.common.EventChannel
import org.altbeacon.beacon.*
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.powersave.BackgroundPowerSaver
import java.util.*

class BeaconManagerWrapper(context: Context): BeaconConsumer, MonitorNotifier, RangeNotifier {

    companion object {
        private const val TAG = "BeaconManagerWrapper"

        private const val CHANNEL_ID = "Beacon Manager Channel"
        private const val CHANNEL_NAME = "Beacon Manager Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifies about updates on beacon scanning and detection"
        private const val NOTIFICATION_ID = 123

        @JvmStatic
        private val iBeaconParser = BeaconParser("ibeacon").setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")
    }

    private val appContext = context.applicationContext
    private val beaconManager = BeaconManager.getInstanceForApplication(appContext)
    private val notificationManager = appContext.getTypedSystemService<NotificationManager>(Context.NOTIFICATION_SERVICE)
    private val powerSaver = BackgroundPowerSaver(appContext)
    private val preferences = BeaconPreferences.getInstance(appContext)

    private val regions = mutableSetOf<Region>()

    var eventSink: EventChannel.EventSink? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager?.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
                            .apply { description = CHANNEL_DESCRIPTION }
            )
        }
        beaconManager.beaconParsers.add(iBeaconParser)
        beaconManager.enableForegroundServiceScanning(buildNotification(), NOTIFICATION_ID)
        beaconManager.setEnableScheduledScanJobs(false)
        beaconManager.backgroundBetweenScanPeriod = 0
        beaconManager.backgroundScanPeriod = 1100
    }

    override fun getApplicationContext(): Context = appContext

    override fun bindService(intent: Intent?, connection: ServiceConnection, mode: Int): Boolean =
            appContext.bindService(intent, connection, mode)

    override fun unbindService(connection: ServiceConnection) = appContext.unbindService(connection)

    override fun onBeaconServiceConnect() {
        addNotifiers()
        regions.add(Region("emptyRegion", null, null, null))
        startScanning()
    }

    override fun didEnterRegion(region: Region) {
        Log.d(TAG, "didEnterRegion: region = $region")
        tryStartRangingBeaconsInRegion(region)
    }

    override fun didExitRegion(region: Region) {
        Log.d(TAG, "didExitRegion: region = $region")
        tryStopRangingBeaconsInRegion(region)
    }

    override fun didDetermineStateForRegion(state: Int, region: Region) { /* do nothing */ }

    override fun didRangeBeaconsInRegion(beacons: MutableCollection<Beacon>, region: Region) {
        beacons.forEach {
            val identifier = it.id1.toString()
            val uniqueId = regions.find { r -> r.id1?.toString() == identifier }?.uniqueId
            eventSink?.success(BeaconModel(uniqueId, it).toString())
        }
    }

    fun setNotification(title: String, text: String) {
        Log.d(TAG, "setNotification: title = $title, text = $text")
        preferences.edit {
            putString(BeaconPreferences.KEY_NOTIFICATION_TITLE, title)
            putString(BeaconPreferences.KEY_NOTIFICATION_TEXT, text)
        }
        if (beaconManager.isBound(this)) {
            notificationManager?.notify(NOTIFICATION_ID, buildNotification())
        }
    }

    fun addRegion(identifier: String, uuid: String) {
        val region = Region(identifier, Identifier.fromUuid(UUID.fromString(uuid)), null, null)
        Log.d(TAG, "addRegion: region = $region")
        if (!regions.contains(region)) {
            if (beaconManager.isBound(this)) {
                tryStartMonitoringBeaconsInRegion(region)
            } else {
                Log.w(TAG, "addRegion: service not bound yet")
            }
            regions.add(region)
        } else {
            Log.w(TAG, "addRegion: region already present")
        }
    }

    fun clearRegions() {
        Log.d(TAG, "clearRegions")
        removeNotifiers()
        stopRangingAndMonitoring()
        regions.clear()
        addNotifiers()
    }

    fun startScanning() {
        Log.d(TAG, "startScanning")
        if (beaconManager.isBound(this)) {
            regions.forEach { tryStartMonitoringBeaconsInRegion(it) }
        } else {
            beaconManager.bind(this)
        }
    }

    fun stopScanning() {
        Log.d(TAG, "stopScanning")
        removeNotifiers()
        stopRangingAndMonitoring()
        beaconManager.unbind(this)
    }

    private fun buildNotification() = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(preferences.getString(BeaconPreferences.KEY_NOTIFICATION_TITLE, BeaconPreferences.DEFAULT_NOTIFICATION_TITLE))
            .setContentText(preferences.getString(BeaconPreferences.KEY_NOTIFICATION_TEXT, BeaconPreferences.DEFAULT_NOTIFICATION_TEXT))
            .build()

    private fun addNotifiers() {
        Log.d(TAG, "addNotifiers")
        beaconManager.addMonitorNotifier(this)
        beaconManager.addRangeNotifier(this)
    }

    private fun removeNotifiers() {
        Log.d(TAG, "removeNotifiers")
        beaconManager.removeRangeNotifier(this)
        beaconManager.removeMonitorNotifier(this)
    }

    private fun stopRangingAndMonitoring() {
        Log.d(TAG, "stopRangingAndMonitoring")
        beaconManager.rangedRegions.forEach { tryStopRangingBeaconsInRegion(it) }
        beaconManager.monitoredRegions.forEach { tryStopMonitoringBeaconsInRegion(it) }
    }

    private fun tryStartMonitoringBeaconsInRegion(region: Region) {
        try {
            beaconManager.startMonitoringBeaconsInRegion(region)
        } catch (e: RemoteException) {
            Log.e(TAG, "startMonitoringBeaconsInRegion: error", e)
        }
    }

    private fun tryStopMonitoringBeaconsInRegion(region: Region) {
        try {
            beaconManager.stopMonitoringBeaconsInRegion(region)
        } catch (e: RemoteException) {
            Log.e(TAG, "stopMonitoringBeaconsInRegion: error", e)
        }
    }

    private fun tryStartRangingBeaconsInRegion(region: Region) {
        try {
            beaconManager.startRangingBeaconsInRegion(region)
        } catch (e: RemoteException) {
            Log.e(TAG, "startRangingBeaconsInRegion: error", e)
        }
    }

    private fun tryStopRangingBeaconsInRegion(region: Region) {
        try {
            beaconManager.stopRangingBeaconsInRegion(region)
        } catch (e: RemoteException) {
            Log.e(TAG, "stopRangingBeaconsInRegion: error", e)
        }
    }
}