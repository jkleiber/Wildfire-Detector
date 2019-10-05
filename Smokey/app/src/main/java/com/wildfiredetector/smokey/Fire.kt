package com.wildfiredetector.smokey

import android.content.Context
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.OverlayItem
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.*

class Fire(context: Context?, lat: Double, lon: Double) {
    private val ctx = context
    private var latitude = lat
    private var longitude = lon
    private val timestamp: String

    init {
        timestamp = ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now())
    }

    fun getFire(map: MapView): Marker {
        // Make a GeoPoint on the map
        var point = GeoPoint(latitude, longitude)

        // Make an overlay item
        var fireMarker= Marker(map)
        fireMarker.position = point

        // Add a fire icon
        val fireIcon = ctx?.resources?.getDrawable(R.drawable.fire)
        fireMarker.icon = fireIcon

        // Show the description
        fireMarker.title = "Reported at: " + timestamp

        // Show the fire
        return fireMarker
    }
}