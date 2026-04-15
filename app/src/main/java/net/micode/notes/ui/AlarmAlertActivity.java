/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.micode.notes.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.tool.DataUtils;

import java.io.IOException;

/**
 * 笔记闹钟提醒弹窗 Activity
 * 功能：当笔记设置的提醒时间到达时，弹出对话框，播放闹钟声音，显示提醒内容
 */
public class AlarmAlertActivity extends Activity implements OnClickListener, OnDismissListener {
    // 提醒对应的笔记ID
    private long mNoteId;
    // 笔记摘要内容
    private String mSnippet;
    // 摘要预览最大长度，超过部分会被截取
    private static final int SNIPPET_PREW_MAX_LEN = 60;
    // 媒体播放器，用于播放闹钟铃声
    MediaPlayer mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 去掉Activity标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        final Window win = getWindow();
        // 设置窗口在锁屏界面上显示
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        // 如果屏幕未点亮，设置屏幕常亮、点亮屏幕、允许锁屏等属性
        if (!isScreenOn()) {
            win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);
        }

        // 获取启动当前Activity的Intent
        Intent intent = getIntent();

        try {
            // 从Intent数据中解析笔记ID
            mNoteId = Long.valueOf(intent.getData().getPathSegments().get(1));
            // 根据笔记ID获取笔记摘要
            mSnippet = DataUtils.getSnippetById(this.getContentResolver(), mNoteId);
            // 如果摘要过长，截取前60个字符，并追加省略提示
            mSnippet = mSnippet.length() > SNIPPET_PREW_MAX_LEN ? mSnippet.substring(0,
                    SNIPPET_PREW_MAX_LEN) + getResources().getString(R.string.notelist_string_info)
                    : mSnippet;
        } catch (IllegalArgumentException e) {
            // 数据异常则打印日志并直接返回
            e.printStackTrace();
            return;
        }

        // 初始化媒体播放器
        mPlayer = new MediaPlayer();
        // 检查笔记是否有效且可见
        if (DataUtils.visibleInNoteDatabase(getContentResolver(), mNoteId, Notes.TYPE_NOTE)) {
            // 显示提醒对话框
            showActionDialog();
            // 播放闹钟声音
            playAlarmSound();
        } else {
            // 笔记无效则关闭Activity
            finish();
        }
    }

    /**
     * 判断当前屏幕是否处于点亮状态
     * @return 点亮返回true，否则false
     */
    private boolean isScreenOn() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return pm.isScreenOn();
    }

    /**
     * 播放系统默认的闹钟提示音
     */
    private void playAlarmSound() {
        // 获取系统默认闹钟铃声URI
        Uri url = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);

        // 获取静音模式影响的音频流
        int silentModeStreams = Settings.System.getInt(getContentResolver(),
                Settings.System.MODE_RINGER_STREAMS_AFFECTED, 0);

        // 设置媒体播放器的音频流为闹钟流
        if ((silentModeStreams & (1 << AudioManager.STREAM_ALARM)) != 0) {
            mPlayer.setAudioStreamType(silentModeStreams);
        } else {
            mPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        }
        try {
            // 设置数据源
            mPlayer.setDataSource(this, url);
            // 准备播放
            mPlayer.prepare();
            // 循环播放
            mPlayer.setLooping(true);
            // 开始播放
            mPlayer.start();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示笔记提醒对话框
     */
    private void showActionDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        // 设置标题为应用名称
        dialog.setTitle(R.string.app_name);
        // 设置显示内容为笔记摘要
        dialog.setMessage(mSnippet);
        // 设置确定按钮
        dialog.setPositiveButton(R.string.notealert_ok, this);
        // 如果屏幕已点亮，显示“进入笔记”按钮
        if (isScreenOn()) {
            dialog.setNegativeButton(R.string.notealert_enter, this);
        }
        // 显示对话框并设置消失监听
        dialog.show().setOnDismissListener(this);
    }

    /**
     * 对话框按钮点击事件
     */
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            // 点击“进入”按钮，跳转到笔记编辑页面
            case DialogInterface.BUTTON_NEGATIVE:
                Intent intent = new Intent(this, NoteEditActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                // 传入笔记ID
                intent.putExtra(Intent.EXTRA_UID, mNoteId);
                startActivity(intent);
                break;
            // 确定按钮：无需处理，对话框消失会自动停止声音
            default:
                break;
        }
    }

    /**
     * 对话框消失时触发：停止铃声并关闭页面
     */
    public void onDismiss(DialogInterface dialog) {
        stopAlarmSound();
        finish();
    }

    /**
     * 停止闹钟铃声并释放播放器资源
     */
    private void stopAlarmSound() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
    }
}
