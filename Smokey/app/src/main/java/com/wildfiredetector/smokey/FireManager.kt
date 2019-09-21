package com.wildfiredetector.smokey

import android.content.Context
import org.osmdroid.views.MapView

class FireManager {

    companion object {
        private var fires: ArrayList<Fire> = ArrayList()

        fun addFire(context: Context?, lat: Double?, lon: Double?){
            if(context != null
                && lat != null
                && lon != null)
            {
                fires.add(Fire(context, lat, lon))
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
    }
}