package com.kwunai.rx.player

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.kwunai.rx.player.ext.getStatusHeight
import com.kwunai.rx.player.ext.hasNotch
import com.kwunai.rx.player.ext.otherwise
import com.kwunai.rx.player.ext.yes
import com.kwunai.rx.player.view.LifeVideoPlayerFork
import com.kwunai.rx.player.view.VideoControllerFork


import kotlinx.android.synthetic.main.activity_main1.*


class VideoActivityFork : AppCompatActivity() {

    private val url = "https://outin-8ad9d45c9a0711e89d5a00163e024c6a.oss-cn-shanghai.aliyuncs.com/ebd13d6e7c73455c8959de39e756bf73/95a605e8e4b843cf804ae075ce35bb51-4e47ec5efbf3b20c134310eaa5dcf1ca-od-S00000001-200000.mp4?Expires=1539009444&OSSAccessKeyId=LTAInFumgYEtNMvC&Signature=4o4f32YaDzLqI55qGvb%2FbYHXL%2Fw%3D"

    private val player: LifeVideoPlayerFork by lazy {
        mPlayer.apply { lifecycle.addObserver(this) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main1)
        fitNotchScreen()
        player.createPlayerConfig(url)
        val controller = VideoControllerFork(this)
        player.setController(controller)
        player.start()
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
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        hasNotch().yes {
            (newConfig.orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE).yes {
                Log.e("lzt", "onConfigurationChanged1")
                systemView.visibility = View.GONE
            }.otherwise {
                Log.e("lzt", "onConfigurationChanged12")
                systemView.visibility = View.VISIBLE
            }
        }
    }
}