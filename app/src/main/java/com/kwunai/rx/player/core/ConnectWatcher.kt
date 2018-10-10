@file:Suppress("DEPRECATION")

package com.kwunai.rx.player.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.kwunai.rx.player.ext.isNetAvailable
import com.kwunai.rx.player.modal.NetworkState

/**
 * 监听网络变化
 */
class ConnectWatcher(context: Context, callback: Callback?) {

    private var mCallback: Callback? = callback
    private var mContext: Context = context

    private var mAvailable: Boolean = false
    private var mTypeName: String? = null

    private lateinit var cm: ConnectivityManager

    fun isAvailable(): Boolean {
        return mAvailable || mContext.isNetAvailable()
    }

    fun getNetworkType(): String? {
        return mTypeName
    }

    fun startup() {
        cm = mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val info: NetworkInfo? = cm.activeNetworkInfo
        mAvailable = info != null && info.isConnected
        mTypeName = if (mAvailable) info!!.typeName else null
        val filter = IntentFilter()
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        mContext.registerReceiver(mReceiver, filter)
    }

    fun shutdown() {
        mContext.unregisterReceiver(mReceiver)
    }

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ConnectivityManager.CONNECTIVITY_ACTION == action) {
                val info = cm.activeNetworkInfo
                val available = info != null && info.isConnected
                val typeName = if (available) info!!.typeName else null
                if (mAvailable != available) {
                    mAvailable = available
                    mTypeName = typeName
                    onAvailable(available)
                } else if (mAvailable) {
                    if (typeName != mTypeName) {
                        mTypeName = typeName
                        notifyEvent(NetworkState.NETWORK_CHANGE)
                    }
                }
            }
        }
    }

    private fun onAvailable(available: Boolean) {
        if (available) {
            notifyEvent(NetworkState.NETWORK_AVAILABLE)
        } else {
            notifyEvent(NetworkState.NETWORK_UNAVAILABLE)
        }
    }

    private fun notifyEvent(event: NetworkState) {
        if (mCallback != null) {
            mCallback!!.onNetworkEvent(event)
        }
    }

    interface Callback {
        fun onNetworkEvent(event: NetworkState)
    }
}