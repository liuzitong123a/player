package com.kwunai.rx.player.neplayer

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle

import android.support.v7.app.AppCompatActivity
import android.view.View
import com.bumptech.glide.Glide
import com.kwunai.neplayer.NEPlayerManager
import com.kwunai.player.ext.*
import com.kwunai.player.core.LifeVideoPlayer
import com.kwunai.player.core.OnCompletedCallback
import com.kwunai.player.core.PlayerController
import com.kwunai.rx.player.R
import kotlinx.android.synthetic.main.activity_main.*

class NEVideoActivity : AppCompatActivity(), OnCompletedCallback {

    private val url = ""
    private val coverUrl = ""

    private val player: LifeVideoPlayer by lazy {
        mPlayer.apply { lifecycle.addObserver(this) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fitNotchScreen()
        Glide.with(this).load(coverUrl).into(mIvCover)
        player.init(NEPlayerManager.configPlayer(this))
        val controller = PlayerController(this)
                .setVideoTitle("")
                .setOnCompletedCallback(this)
        player.setController(controller)
        mIvStartPlayer.setOnClickListener {
            mIvCover.visible(false)
            mIvStartPlayer.visible(false)
            player.createPlayerConfig(url)
            player.start()
        }
        mIvBack.setOnClickListener { finish() }
    }

    private fun fitNotchScreen() {
        if (hasNotch()) {
            systemView.visibility = View.VISIBLE
            val layoutParams = systemView.layoutParams
            layoutParams.height += getStatusHeight()
            systemView.layoutParams = layoutParams
        } else {
            systemView.visibility = View.GONE
        }
        val navLayoutParams = nav.layoutParams
        navLayoutParams.height += getNavBarHeight()
        nav.layoutParams = navLayoutParams
    }

    override fun onBackPressed() {
        player.onBackPressed().yes { return }
        super.onBackPressed()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        hasNotch().yes {
            (newConfig.orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE).yes {
                systemView.visibility = View.GONE
            }.otherwise {
                systemView.visibility = View.VISIBLE
            }
        }
    }

    override fun doCompleted() {
        mIvCover.visible(true)
        mIvStartPlayer.visible(true)
    }
}