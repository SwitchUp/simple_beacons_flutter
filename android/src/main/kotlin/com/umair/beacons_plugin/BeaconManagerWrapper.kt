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
        private const val NOTIFICATION_ID = 456

        @JvmStatic
        private val iBeaconParser = BeaconParser("ibeacon").setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")
    }

    private val appContext = context.applicationContext
    private val manager = BeaconManager.getInstanceForApplication(appContext)
    private val powerSaver = BackgroundPowerSaver(appContext)

    private val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Scanning for Beacons")
            .build()

    private val regions = mutableSetOf<Region>()

    var eventSink: EventChannel.EventSink? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getTypedSystemService<NotificationManager>(Context.NOTIFICATION_SERVICE)
                    ?.createNotificationChannel(NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = CHANNEL_DESCRIPTION
                    })
        }
        manager.beaconParsers.add(iBeaconParser)
        manager.enableForegroundServiceScanning(notification, NOTIFICATION_ID)
        manager.setEnableScheduledScanJobs(false)
        manager.backgroundBetweenScanPeriod = 0
        manager.backgroundScanPeriod = 1100
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
            val uniqueId = regions.find { r -> r.id1.toString() == identifier }?.uniqueId
            eventSink?.success(BeaconModel(uniqueId, it))
        }
    }

    fun addRegion(identifier: String, uuid: String) {
        val region = Region(identifier, Identifier.fromUuid(UUID.fromString(uuid)), null, null)
        Log.d(TAG, "addRegion: region = $region")
        if (!regions.contains(region)) {
            if (manager.isBound(this)) {
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
        if (manager.isBound(this)) {
            regions.forEach { tryStartMonitoringBeaconsInRegion(it) }
        } else {
            manager.bind(this)
        }
    }

    fun stopScanning() {
        Log.d(TAG, "stopScanning")
        removeNotifiers()
        stopRangingAndMonitoring()
        manager.unbind(this)
    }

    private fun addNotifiers() {
        Log.d(TAG, "addNotifiers")
        manager.addMonitorNotifier(this)
        manager.addRangeNotifier(this)
    }

    private fun removeNotifiers() {
        Log.d(TAG, "removeNotifiers")
        manager.removeRangeNotifier(this)
        manager.removeMonitorNotifier(this)
    }

    private fun stopRangingAndMonitoring() {
        Log.d(TAG, "stopRangingAndMonitoring")
        manager.rangedRegions.forEach { tryStopRangingBeaconsInRegion(it) }
        manager.monitoredRegions.forEach { tryStopMonitoringBeaconsInRegion(it) }
    }

    private fun tryStartMonitoringBeaconsInRegion(region: Region) {
        try {
            manager.startMonitoringBeaconsInRegion(region)
        } catch (e: RemoteException) {
            Log.e(TAG, "startMonitoringBeaconsInRegion: error", e)
        }
    }

    private fun tryStopMonitoringBeaconsInRegion(region: Region) {
        try {
            manager.stopMonitoringBeaconsInRegion(region)
        } catch (e: RemoteException) {
            Log.e(TAG, "stopMonitoringBeaconsInRegion: error", e)
        }
    }

    private fun tryStartRangingBeaconsInRegion(region: Region) {
        try {
            manager.startRangingBeaconsInRegion(region)
        } catch (e: RemoteException) {
            Log.e(TAG, "startRangingBeaconsInRegion: error", e)
        }
    }

    private fun tryStopRangingBeaconsInRegion(region: Region) {
        try {
            manager.stopRangingBeaconsInRegion(region)
        } catch (e: RemoteException) {
            Log.e(TAG, "stopRangingBeaconsInRegion: error", e)
        }
    }
}