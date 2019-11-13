package com.wildfiredetector.smokey.ui.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.util.Log.d
import android.util.Log.w
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.wildfiredetector.smokey.FireManager
import com.wildfiredetector.smokey.R
import com.wildfiredetector.smokey.VolleySingleton
import kotlinx.android.synthetic.main.fragment_fire_map.*
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus
import org.osmdroid.views.overlay.OverlayItem


class FireMapFragment : Fragment() {
    companion object

    private lateinit var pageViewModel: PageViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val REQUEST_COARSE_LOC = 5
    private val REQUEST_FINE_LOC = 6

    private val LOCATION_UPDATES_TIL_LOCK = 2   // Number of location updates until the location can be trusted

    private var currentLocation: Location? = null
    private var locationUpdates: Int = 0

    private val allReportsURL = "http://smokey.x10.bz/php/get_all_reports.php"

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

        // Get all the fire reports in the database
        // TODO: only get active fires and active fire reports
        val getAllFiresRequest = JsonArrayRequest(Request.Method.POST, allReportsURL, null,
            Response.Listener{ fires: JSONArray ->
                d("RESPONSE", fires.toString(2))

                // Process fires from database
                receiveFires(fires)

                // Update the map
                FireManager.updateFireMap(fireMap)
            },
            Response.ErrorListener {
                Log.e("RESPONSE", it?.message)
                val errorText = "Failed to report fire: %s".format(it.message)
                Snackbar.make(view, errorText, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
        )

        // Send the request to initialize the map
        VolleySingleton.getInstance(context!!).addToRequestQueue(getAllFiresRequest)

        pageViewModel.updateFlag.observe(this, Observer<Boolean> {
            // Pull the fire from the database
            val updateFiresRequest = JsonArrayRequest(Request.Method.POST, allReportsURL, null,
                Response.Listener{ fires: JSONArray ->
                    d("RESPONSE", fires.toString(2))

                    // Update the current fires from request and refresh the map
                    FireManager.clearFires()
                    receiveFires(fires)
                    FireManager.updateFireMap(fireMap)
                },
                Response.ErrorListener {
                    Log.e("RESPONSE", it?.message)
                    val errorText = "Failed to report fire: %s".format(it.message)
                    Snackbar.make(view, errorText, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()
                }
            )

            // Send the update request
            VolleySingleton.getInstance(context!!).addToRequestQueue(updateFiresRequest)
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

            // Update last known location
            val sharedPref = context?.getSharedPreferences("LAST_LOCATION", Context.MODE_PRIVATE)
            val prefEditor = sharedPref?.edit()
            prefEditor?.putFloat("lat", item.latitude.toFloat())
            prefEditor?.putFloat("lon", item.longitude.toFloat())
            prefEditor?.commit()
        })

        var lat = 0.0
        var lon = 0.0


        // Get the user's last known location
        if (context != null)
        {
            val sharedPref = context!!.getSharedPreferences("LAST_LOCATION", Context.MODE_PRIVATE)
            lat = sharedPref.getFloat("lat", 0.0f).toDouble()
            lon = sharedPref.getFloat("lon", 0.0f).toDouble()
        }

        mapController.setCenter(GeoPoint(lat, lon))

        // Setup the map tiling
        fireMap.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)

        // Zoom controls
        fireMap.setMultiTouchControls(true)

        // Start at a far zoom by default
        mapController.setZoom(8.0)
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

    private fun receiveFires(fires: JSONArray)
    {
        // Go through each fire in the report
        for(i in 1..fires.length())
        {
            // Get the individual fire
            val fireString = fires.getString(i - 1)
            val fire = JSONObject(fireString)

            // Get the fire info
            val fireTime = fire.getString("timestamp")
            val fireLat = fire.getDouble("lat")
            val fireLon = fire.getDouble("lon")

            // Add the fire to the map
            FireManager.addFire(context, fireLat, fireLon, fireTime)
        }
    }

}