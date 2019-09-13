package com.wildfiredetector.smokey.ui.main

import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.wildfiredetector.smokey.R
import kotlinx.android.synthetic.main.fragment_fire_map.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint


class FireMapFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pageViewModel = ViewModelProviders.of(this).get(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(SectionsPagerAdapter.ARG_SECTION_NUMBER) ?: 1)
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        // Load the OSM Droid context
        val ctx = context
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        return inflater.inflate(R.layout.fragment_fire_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        // Get the user's location
        var lat = 35.2226
        var lon = -97.4395

        // Access the last known location
        var ctx = context

        if(ctx != null)
        {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(ctx)
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location : Location? ->
                    // Got last known location. In some rare situations this can be null.
                    if(location != null)
                    {
                        lat = location.latitude
                        lon = location.longitude
                    }
                }
        }

        // Make a geopoint of the current location
        val curPoint = GeoPoint(lat, lon)

        // Setup the map tiling
        fireMap.setTileSource(TileSourceFactory.MAPNIK)

        // Zoom controls
        fireMap.setMultiTouchControls(true)

        // Start at user's location
        val mapController = fireMap.controller
        mapController.setZoom(12.5)
        mapController.setCenter(curPoint)


        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }

    override fun onResume() {
        super.onResume()

        fireMap.onResume()
    }

    override fun onPause() {
        super.onPause()

        fireMap.onPause()
    }

}