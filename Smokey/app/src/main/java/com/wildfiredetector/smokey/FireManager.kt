package com.wildfiredetector.smokey

import android.content.Context
import org.osmdroid.views.MapView
import java.sql.Timestamp

class FireManager {

    companion object {
        private var fires: ArrayList<Fire> = ArrayList()

        fun addFire(context: Context?, lat: Double, lon: Double, timestamp: String){
            if(context != null)
            {
                fires.add(Fire(context, lat, lon, timestamp))
            }
        }

        fun updateFireMap(map: MapView)
        {
            for (fire in fires)
            {
                val fireMarker = fire.getFire(map)

                if(!map.overlays.contains(fireMarker))
                {
                    map.overlays.add(fireMarker)
                }
            }
        }

        fun clearFires()
        {
            fires.clear()
        }
    }
}