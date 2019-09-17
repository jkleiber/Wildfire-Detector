package com.wildfiredetector.smokey.ui.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import com.wildfiredetector.smokey.R
import kotlinx.android.synthetic.main.fragment_overview.*
import java.lang.ClassCastException
import java.lang.Exception

class OverviewFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel

    private val REQUEST_COARSE_LOC = 12
    private val REQUEST_FINE_LOC = 13

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
            // Listen for clicks on the fire report button
            bDetectWildfire.setOnClickListener { view ->
                Snackbar.make(view, "Fire Reported!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()

                // Enable GPS detection
                val approved = getPermissions(context)

                // If the GPS permissions are approved, add a fire
                if(approved)
                {

                }

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