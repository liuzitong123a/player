package com.kwunai.player.ext

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo


fun Context.isNetAvailable(): Boolean {
    val connMgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo: NetworkInfo? = connMgr.activeNetworkInfo
    return networkInfo != null && networkInfo.isConnected
}


