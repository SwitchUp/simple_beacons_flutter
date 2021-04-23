package com.umair.beacons_plugin

import org.altbeacon.beacon.Beacon
import java.text.SimpleDateFormat
import java.util.*

class BeaconModel(val name: String?, beacon: Beacon) {

    companion object {
        @JvmStatic
        private val dateFormat = SimpleDateFormat("dd MMMM yyyy hh:mm:ss a", Locale.ROOT)

        @JvmStatic
        private fun getBeaconProximity(beacon: Beacon): Proximity = when {
            beacon.distance < 0 -> Proximity.UNKNOWN
            beacon.distance < 0.5 -> Proximity.IMMEDIATE
            beacon.distance < 3.0 -> Proximity.NEAR
            else -> Proximity.FAR
        }
    }

    val uuid: String = beacon.id1.toString()
    val major: String = beacon.id2.toString()
    val minor: String = beacon.id3.toString()
    val distance: String = (Math.round(beacon.distance * 100.0) / 100.0).toString()
    val proximity: String = getBeaconProximity(beacon).value
    val scanTime: String = dateFormat.format(System.currentTimeMillis())
    val macAddress: String = beacon.bluetoothAddress
    val rssi: String = beacon.rssi.toString()
    val txPower: String = beacon.txPower.toString()

    override fun toString(): String = "{\n" +
            "\"name\": \"$name\",\n" +
            "\"uuid\": \"$uuid\",\n" +
            "\"macAddress\": \"$macAddress\",\n" +
            "\"major\": \"$major\",\n" +
            "\"minor\": \"$minor\",\n" +
            "\"distance\": \"$distance\",\n" +
            "\"proximity\": \"$proximity\",\n" +
            "\"scanTime\": \"$scanTime\",\n" +
            "\"rssi\": \"$rssi\",\n" +
            "\"txPower\": \"$txPower\"\n" +
            "}"
}