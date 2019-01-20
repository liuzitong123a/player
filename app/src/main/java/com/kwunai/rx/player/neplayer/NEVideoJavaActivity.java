package com.kwunai.rx.player.neplayer;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.kwunai.neplayer.NEPlayerManager;
import com.kwunai.player.core.LifeVideoPlayer;
import com.kwunai.player.core.OnCompletedCallback;
import com.kwunai.player.core.PlayerController;
import com.kwunai.player.ext.CommenExtKt;
import com.kwunai.player.ext.SystemBarExtKt;
import com.kwunai.rx.player.R;

public class NEVideoJavaActivity extends AppCompatActivity implements OnCompletedCallback {

    private static final String URL = "https://outin-8ad9d45c9a0711e89d5a00163e024c6a.oss-cn-shanghai.aliyuncs.com/783f1c1a84294b88b0b55a66fd52210c/806d19e958a04f0eb1764c89184033f4-3e8e0c93e26fc464bc5af868ddbf8335-od-S00000001-200000.mp4?Expires=1548053609&OSSAccessKeyId=LTAInFumgYEtNMvC&Signature=l7kNWqQYDDDyifuRhjpjq5VGR8o%3D";

    private static final String COVER_URL = "https://qingliu.oss-cn-beijing.aliyuncs.com/course/2018-08/20180817201136_1_110.jpg";

    private View systemView;
    private View nav;
    private LifeVideoPlayer player;
    private ImageView mIvCover;
    private ImageView mIvStartPlayer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        systemView = findViewById(R.id.systemView);
        nav = findViewById(R.id.nav);

        mIvCover = findViewById(R.id.mIvCover);
        mIvStartPlayer = findViewById(R.id.mIvStartPlayer);
        ImageView mIvBack = findViewById(R.id.mIvBack);
        fitNotchScreen();
        Glide.with(this).load(COVER_URL).into(mIvCover);

        // init video
        player = findViewById(R.id.mPlayer);
        getLifecycle().addObserver(player);
        player.init(NEPlayerManager.configPlayer(this));
        PlayerController controller = new PlayerController(this)
                .setVideoTitle("")
                .setOnCompletedCallback(this);
        player.setController(controller);
        player.dispatchReceiverEvent();

        mIvStartPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CommenExtKt.visible(mIvCover, false);
                CommenExtKt.visible(mIvStartPlayer, false);
                player.createPlayerConfig(URL);
                player.start();
            }
        });
        mIvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
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
    public void doCompleted() {
        CommenExtKt.visible(mIvCover, true);
        CommenExtKt.visible(mIvStartPlayer, true);
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
}
