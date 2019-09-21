package com.wildfiredetector.smokey

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import com.google.android.material.tabs.TabLayout
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import com.wildfiredetector.smokey.ui.main.PageViewModel
import com.wildfiredetector.smokey.ui.main.SectionsPagerAdapter
import kotlinx.android.synthetic.main.activity_main_screen.*
import java.lang.Exception


class MainScreenActivity : AppCompatActivity(), LocationListener {

    private val REQUEST_COARSE_LOC = 12
    private val REQUEST_FINE_LOC = 13

    private lateinit var pageViewModel: PageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_screen)
        setSupportActionBar(toolbar)

        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: HackedViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter

        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)

        // Enable GPS detection
        val approved = getPermissions()

        if(approved)
        {
            // Initialize the location listener
            try{
                val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0.0f, this)
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0.0f, this)
            }
            catch (e: SecurityException)
            {
                e.printStackTrace()
            }
        }

        // Init the page view model
        pageViewModel = this.run{
            ViewModelProviders.of(this).get(PageViewModel::class.java)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when(item.itemId) {
            R.id.action_devices -> {
                val settingsIntent = Intent(this, SettingsActivity::class.java)
                startActivity(settingsIntent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onLocationChanged(location: Location?) {
        if(location != null)
        {
            pageViewModel.updateLocation(location)
        }
    }

    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
    }

    override fun onProviderEnabled(p0: String?) {
    }

    override fun onProviderDisabled(p0: String?) {
    }

    private fun getPermissions(): Boolean
    {
        var result: Boolean = false

        // Access coarse location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_COARSE_LOC)
        }
        else
        {
            result = true
        }

        // Access fine location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_FINE_LOC)
        }
        else
        {
            result = result && true
        }

        return result
    }
}