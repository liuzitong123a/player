package com.kwunai.rx.player.neplayer;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;

import com.kwunai.neplayer.NEPlayerManager;
import com.kwunai.player.core.LifeVideoPlayer;
import com.kwunai.player.core.OnCompletedCallback;
import com.kwunai.player.core.PlayerController;
import com.kwunai.player.ext.CommenExtKt;
import com.kwunai.player.ext.SystemBarExtKt;
import com.kwunai.rx.player.R;

public class SampleVideoActivity extends AppCompatActivity implements OnCompletedCallback {

    private View systemView;
    private View nav;
    private LifeVideoPlayer player;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        systemView = findViewById(R.id.systemView);
        nav = findViewById(R.id.nav);
        fitNotchScreen();

        player = findViewById(R.id.life_player);
        getLifecycle().addObserver(player);
        player.init(NEPlayerManager.configPlayer(this));
        PlayerController controller = new PlayerController(this)
                .setVideoTitle("")
                .setOnCompletedCallback(this);
        player.setController(controller);
        player.dispatchReceiverEvent();
        player.createPlayerConfig(NEVideoJavaActivity.URL);
        player.start();
    }

    private void fitNotchScreen() {
        if (SystemBarExtKt.hasNotch(this)) {
            CommenExtKt.visible(systemView, true);
            ViewGroup.LayoutParams layoutParams = systemView.getLayoutParams();
            layoutParams.height += SystemBarExtKt.getStatusHeight(this);
            systemView.setLayoutParams(layoutParams);
        } else {
            CommenExtKt.visible(systemView, false);
        }
        ViewGroup.LayoutParams navLayoutParams = nav.getLayoutParams();
        navLayoutParams.height += SystemBarExtKt.getNavBarHeight(this);
        nav.setLayoutParams(navLayoutParams);
    }

    @Override
    public void onBackPressed() {
        if (player.onBackPressed()) {
            return;
        }
        super.onBackPressed();

    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (SystemBarExtKt.hasNotch(this)) {
            if (newConfig.orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                CommenExtKt.visible(systemView, false);
            } else {
                CommenExtKt.visible(systemView, true);
            }
        }
    }

    @Override
    public void doCompleted() {
        finish();
    }
}
