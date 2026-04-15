package net.micode.notes.ui;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import net.micode.notes.R;

/**
 * 应用启动欢迎页（闪屏页）
 * 负责展示启动动画、播放背景音乐、延时跳转到主界面
 */
public class SplashActivity extends AppCompatActivity {
    // 动画持续时间：2500毫秒（当前代码未实际使用该动画）
    private static final int ANIMATION_DURATION = 2500;
    // 欢迎页总展示时长：3000毫秒（3秒）
    private static final int SPLASH_DURATION = 3000;

    // 主线程Handler，用于执行延时任务
    private Handler mHandler = new Handler();
    // 媒体播放器，用于播放背景音乐
    private MediaPlayer mMediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置欢迎页布局
        setContentView(R.layout.activity_splash);

        // 播放音频方法调用（当前已注释，未启用）
        // playAudio(R.raw.testmusic);

        /**
         * 版本问题，目前主流已经废弃了~
         */
        /*//自主点击跳转
        Button skipButton = findViewById(R.id.skip_button);
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SplashActivity.this, NotesListActivity.class);
                startActivity(intent);
                finish(); // 销毁欢迎页
            }
        });*/

        // 使用Handler实现延时跳转，延迟SPLASH_DURATION毫秒后执行Runnable
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 页面跳转时，停止并释放媒体播放器资源，避免内存泄漏
                if (mMediaPlayer != null) {
                    mMediaPlayer.stop();
                    mMediaPlayer.release();
                    mMediaPlayer = null;
                }

                // 跳转到便签列表主页面
                Intent intent = new Intent(SplashActivity.this, NotesListActivity.class);
                startActivity(intent);
                // 结束当前欢迎页，防止返回键回到该页面
                finish();
            }
        }, SPLASH_DURATION);
    }

    /**
     * 播放应用背景音乐的工具方法
     * @param audioResId 音频资源ID
     */
    private void playAudio(int audioResId) {
        // 创建MediaPlayer实例，加载指定的音频资源
        final MediaPlayer mediaPlayer = MediaPlayer.create(this, audioResId);
        // 开始播放音频
        mediaPlayer.start();
        // 设置音频播放完成监听器
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                // 音频播放完毕后，停止播放并释放MediaPlayer资源
                mediaPlayer.stop();
                mediaPlayer.release();
            }
        });
    }

}
