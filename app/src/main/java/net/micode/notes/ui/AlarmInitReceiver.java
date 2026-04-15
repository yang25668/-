/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law law, agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.micode.notes.ui;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;

/**
 * 闹钟初始化广播接收器
 * 作用：设备开机 / 应用重启时，重新注册所有未过期的笔记提醒闹钟
 */
public class AlarmInitReceiver extends BroadcastReceiver {

    // 查询数据库需要的字段：笔记ID、提醒时间
    private static final String [] PROJECTION = new String [] {
        NoteColumns.ID,            // 笔记ID
        NoteColumns.ALERTED_DATE   // 笔记提醒时间
    };

    // 字段对应的索引常量
    private static final int COLUMN_ID                = 0; // 笔记ID列索引
    private static final int COLUMN_ALERTED_DATE      = 1; // 提醒时间列索引

    /**
     * 接收广播时执行（开机启动、应用重新初始化）
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // 获取当前系统时间
        long currentDate = System.currentTimeMillis();

        // 查询数据库：所有【提醒时间大于当前时间】的【普通笔记】
        Cursor c = context.getContentResolver().query(Notes.CONTENT_NOTE_URI,
                PROJECTION,
                NoteColumns.ALERTED_DATE + ">? AND " + NoteColumns.TYPE + "=" + Notes.TYPE_NOTE,
                new String[] { String.valueOf(currentDate) },
                null);

        if (c != null) {
            // 遍历所有需要设置提醒的笔记
            if (c.moveToFirst()) {
                do {
                    // 获取笔记的提醒时间
                    long alertDate = c.getLong(COLUMN_ALERTED_DATE);

                    // 创建意图：触发 AlarmReceiver 广播
                    Intent sender = new Intent(context, AlarmReceiver.class);
                    // 绑定笔记ID，用于后续弹窗显示对应笔记
                    sender.setData(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, c.getLong(COLUMN_ID)));

                    // 创建延迟意图（系统闹钟触发时使用）
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, sender, 0);

                    // 获取系统闹钟服务
                    AlarmManager alermManager = (AlarmManager) context
                            .getSystemService(Context.ALARM_SERVICE);

                    // 设置闹钟：RTC_WAKEUP 表示唤醒CPU，在指定时间触发
                    alermManager.set(AlarmManager.RTC_WAKEUP, alertDate, pendingIntent);

                } while (c.moveToNext()); // 循环下一条笔记
            }
            c.close(); // 关闭游标，释放资源
        }
    }
}
