package com.wildfiredetector.smokey.ui.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log.d
import android.util.Log.w
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.wildfiredetector.smokey.FireManager
import com.wildfiredetector.smokey.R
import kotlinx.android.synthetic.main.fragment_fire_map.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus
import org.osmdroid.views.overlay.OverlayItem


class FireMapFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val REQUEST_COARSE_LOC = 5
    private val REQUEST_FINE_LOC = 6

    private val LOCATION_UPDATES_TIL_LOCK = 5   // Number of location updates until the location can be trusted

    private var currentLocation: Location? = null
    private var locationUpdates: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pageViewModel = activity?.run{
            ViewModelProviders.of(this).get(PageViewModel::class.java)
        } ?: throw Exception("Invalid Fire Map Activity")

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
        // Set a map controller
        val mapController = fireMap.controller

        pageViewModel.updateFlag.observe(this, Observer<Boolean> {
            // Display the fire
            FireManager.updateFireMap(fireMap)
        })

        pageViewModel.location.observe(this, Observer<Location> { item ->
            // Display the fire
            currentLocation = item

            if(locationUpdates < LOCATION_UPDATES_TIL_LOCK)
            {
                mapController.setCenter(GeoPoint(currentLocation))
                mapController.setZoom(14.0)

                // Add another location update towards the unlocking from the current view
                locationUpdates++
            }
        })

        // Get the user's location (Default to Norman OK)
        var lat = 35.2226
        var lon = -97.4395

        // Setup the map tiling
        fireMap.setTileSource(TileSourceFactory.MAPNIK)

        // Zoom controls
        fireMap.setMultiTouchControls(true)

        // Start at a far zoom by default
        mapController.setZoom(5.0)
/*
        // TODO: make the FAB into a zoom to current location button
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }*/
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