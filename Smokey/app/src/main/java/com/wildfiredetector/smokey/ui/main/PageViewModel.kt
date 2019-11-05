package com.wildfiredetector.smokey.ui.main

import android.app.Activity
import android.content.ClipData
import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.material.snackbar.Snackbar
import com.wildfiredetector.smokey.VolleySingleton
import org.json.JSONObject

class PageViewModel : ViewModel() {

    val updateFlag = MutableLiveData<Boolean>()
    val location = MutableLiveData<Location>()
    val bleUpdate = MutableLiveData<Boolean>()

    fun updateMap(update: Boolean)
    {
        Log.w("FIRE", "Fire detected")
        updateFlag.value = update
    }

    fun updateLocation(loc: Location)
    {
        location.value = loc
    }

    fun updateBLEFireReport(update: Boolean)
    {
        Log.d("UPDATE", "updating fire flag")
        bleUpdate.postValue(update)
    }

}