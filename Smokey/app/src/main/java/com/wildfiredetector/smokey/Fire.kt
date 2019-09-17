package com.wildfiredetector.smokey

import android.content.Context
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.OverlayItem

class Fire(context: Context?, lat: Double, lon: Double) {
    private val ctx = context
    private var latitude = lat
    private var longitude = lon

    fun getFire(map: MapView): Marker {
        // Make a GeoPoint on the map
        var point = GeoPoint(latitude, longitude)

        // Make an overlay item
        var fireMarker= Marker(map)
        fireMarker.position = point

        // Add a fire icon
        val fireIcon = ctx?.resources?.getDrawable(R.drawable.fire)
        fireMarker.icon = fireIcon

        // Show the fire
        return fireMarker
    }
}