package com.wildfiredetector.smokey.ui.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.util.Log.d
import android.util.Log.e
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.snackbar.Snackbar
import com.wildfiredetector.smokey.Fire
import com.wildfiredetector.smokey.FireManager
import com.wildfiredetector.smokey.R
import com.wildfiredetector.smokey.VolleySingleton
import kotlinx.android.synthetic.main.fragment_fire_map.*
import kotlinx.android.synthetic.main.fragment_overview.*
import org.json.JSONObject
import java.lang.ClassCastException
import java.lang.Exception

class OverviewFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel

    // Arbitrary Permission IDs for finding out if permission is approved
    private val REQUEST_COARSE_LOC = 12
    private val REQUEST_FINE_LOC = 13

    // Current device location
    private var currentLocation: Location? = null

    // Database information
    private val reportURL = "http://smokey.x10.bz/php/report_fire.php"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = activity?.run {
            ViewModelProviders.of(this).get(PageViewModel::class.java)
        } ?: throw Exception("Invalid Overview Activity")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Default to returning the overview fragment
        return inflater.inflate(R.layout.fragment_overview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Update location as the user moves around
        pageViewModel.location.observe(this, Observer<Location> { item ->
            // Update location
            currentLocation = item
        })

        // Listen for clicks on the fire report button
        bDetectWildfire.setOnClickListener { view ->
            // Create a JSON packet for sending the data to the database
            val jsonPkt = JSONObject()
            jsonPkt.put("latitude", currentLocation?.latitude)
            jsonPkt.put("longitude", currentLocation?.longitude)

            // Build a new request
            val request = JsonObjectRequest(Request.Method.POST, reportURL, jsonPkt,
                Response.Listener{
                    d("RESPONSE", it.toString())
                    Snackbar.make(view, "Fire reported!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()
                },
                Response.ErrorListener {
                    e("RESPONSE", it?.message)
                    val errorText = "Failed to report fire: %s".format(it.message)
                    Snackbar.make(view, errorText, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()
                }
            )

            // Add the fire to the database by sending a request using Volley
            VolleySingleton.getInstance(activity!!.applicationContext).addToRequestQueue(request)

            // Add the fire to the map
            FireManager.addFire(context, currentLocation?.latitude, currentLocation?.longitude)

            // Update the map
            pageViewModel.updateMap(true)
        }
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