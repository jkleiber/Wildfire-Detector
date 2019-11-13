package com.wildfiredetector.smokey

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ServiceRestartBroadcastReceiver : BroadcastReceiver(){
    override fun onReceive(p0: Context?, p1: Intent?) {
        p0?.startService(Intent(p0, FireReportService::class.java))
    }

}
