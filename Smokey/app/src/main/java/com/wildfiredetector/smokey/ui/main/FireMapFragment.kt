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

        // Get the user's location
        var lat = 35.2226
        var lon = -97.4395

        // Access the last known location
        var ctx = context

        if(ctx != null)
        {
            // Get the GPS permissions
            val approved = getPermissions(ctx)
            w("GPS_CTXT", "Context is good")

            // If the GPS permission is approved, get the lat and lon
            if(approved) {
                w("APPROVED", "Permissions good")

                fusedLocationClient = LocationServices.getFusedLocationProviderClient(ctx)
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            w("APPROVED", "location good")
                            lat = location.latitude
                            lon = location.longitude

                            mapController.setCenter(GeoPoint(lat, lon))
                            mapController.setZoom(13.5)
                        }
                    }
            }
            else
            {
                w("REJECTED", "Permissions bad")
            }
        }
        else
        {
            w("GPS_NOT_GOOD", "Context bad")
        }

        // Setup the map tiling
        fireMap.setTileSource(TileSourceFactory.MAPNIK)

        // Zoom controls
        fireMap.setMultiTouchControls(true)

        // Start at a far zoom by default
        mapController.setZoom(5.0)

        // TODO: Remove this debugging code
        // add a fake fire
        FireManager.addFire(context, lat, lon)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }


        pageViewModel.updateFlag.observe(this, Observer<Boolean> {
            // Display the fire
            FireManager.updateFireMap(fireMap)
        })
    }

    override fun onResume() {
        super.onResume()

        fireMap.onResume()
    }

    override fun onPause() {
        super.onPause()

        fireMap.onPause()
    }

    private fun getPermissions(ctx: Context?): Boolean
    {
        var result = false

        if(ctx != null)
        {
            // Access coarse location
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_COARSE_LOC)
            }
            else
            {
                result = true
            }

            // Access fine location
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_FINE_LOC)
            }
            else
            {
                result = result && true
            }
        }

        return result
    }

}