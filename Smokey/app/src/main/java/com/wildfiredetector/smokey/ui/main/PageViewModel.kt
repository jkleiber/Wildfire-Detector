package com.wildfiredetector.smokey.ui.main

import android.content.ClipData
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PageViewModel : ViewModel() {

    val updateFlag = MutableLiveData<Boolean>()

    fun updateMap(update: Boolean)
    {
        Log.w("FIRE", "Fire detected")
        updateFlag.value = update
    }


}