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

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import net.micode.notes.data.Contact;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.tool.DataUtils;

/**
 * 笔记列表项数据模型
 * 作用：将数据库查询出的Cursor数据封装成一个Java对象
 * 供列表适配器NotesListAdapter使用，方便获取每条笔记/文件夹的所有信息
 */
public class NoteItemData {
    // 数据库查询字段：每次查询列表都使用这些列
    static final String [] PROJECTION = new String [] {
        NoteColumns.ID,                  // 笔记ID
        NoteColumns.ALERTED_DATE,        // 提醒时间
        NoteColumns.BG_COLOR_ID,         // 背景颜色ID
        NoteColumns.CREATED_DATE,        // 创建时间
        NoteColumns.HAS_ATTACHMENT,      // 是否有附件
        NoteColumns.MODIFIED_DATE,       // 修改时间
        NoteColumns.NOTES_COUNT,         // 子笔记数量（文件夹用）
        NoteColumns.PARENT_ID,           // 父文件夹ID
        NoteColumns.SNIPPET,             // 笔记摘要/文件夹名称
        NoteColumns.TYPE,                // 类型：笔记/文件夹/系统文件夹
        NoteColumns.WIDGET_ID,           // 小部件ID
        NoteColumns.WIDGET_TYPE,         // 小部件类型
    };

    // 字段索引定义
    private static final int ID_COLUMN                    = 0;
    private static final int ALERTED_DATE_COLUMN          = 1;
    private static final int BG_COLOR_ID_COLUMN           = 2;
    private static final int CREATED_DATE_COLUMN          = 3;
    private static final int HAS_ATTACHMENT_COLUMN        = 4;
    private static final int MODIFIED_DATE_COLUMN         = 5;
    private static final int NOTES_COUNT_COLUMN           = 6;
    private static final int PARENT_ID_COLUMN             = 7;
    private static final int SNIPPET_COLUMN               = 8;
    private static final int TYPE_COLUMN                  = 9;
    private static final int WIDGET_ID_COLUMN             = 10;
    private static final int WIDGET_TYPE_COLUMN           = 11;

    // 成员变量，存储一条笔记/文件夹的所有信息
    private long mId;
    private long mAlertDate;
    private int mBgColorId;
    private long mCreatedDate;
    private boolean mHasAttachment;
    private long mModifiedDate;
    private int mNotesCount;
    private long mParentId;
    private String mSnippet;
    private int mType;
    private int mWidgetId;
    private int mWidgetType;

    // 通话记录专用：联系人姓名 & 电话号码
    private String mName;
    private String mPhoneNumber;

    // 列表位置状态（用于绘制列表分割线、布局样式）
    private boolean mIsLastItem;          // 是否是最后一项
    private boolean mIsFirstItem;         // 是否是第一项
    private boolean mIsOnlyOneItem;       // 列表是否只有一项
    private boolean mIsOneNoteFollowingFolder;  // 文件夹后只有一个笔记
    private boolean mIsMultiNotesFollowingFolder; // 文件夹后有多个笔记

    /**
     * 构造方法：从Cursor中读取数据并封装
     */
    public NoteItemData(Context context, Cursor cursor) {
        // 读取基础字段
        mId = cursor.getLong(ID_COLUMN);
        mAlertDate = cursor.getLong(ALERTED_DATE_COLUMN);
        mBgColorId = cursor.getInt(BG_COLOR_ID_COLUMN);
        mCreatedDate = cursor.getLong(CREATED_DATE_COLUMN);
        mHasAttachment = (cursor.getInt(HAS_ATTACHMENT_COLUMN) > 0);
        mModifiedDate = cursor.getLong(MODIFIED_DATE_COLUMN);
        mNotesCount = cursor.getInt(NOTES_COUNT_COLUMN);
        mParentId = cursor.getLong(PARENT_ID_COLUMN);
        mSnippet = cursor.getString(SNIPPET_COLUMN);

        // 移除清单模式的勾选符号，只显示纯文本
        mSnippet = mSnippet.replace(NoteEditActivity.TAG_CHECKED, "")
                .replace(NoteEditActivity.TAG_UNCHECKED, "");

        mType = cursor.getInt(TYPE_COLUMN);
        mWidgetId = cursor.getInt(WIDGET_ID_COLUMN);
        mWidgetType = cursor.getInt(WIDGET_TYPE_COLUMN);

        mPhoneNumber = "";
        mName = "";

        // 如果是通话记录文件夹里的笔记，自动读取联系人信息
        if (mParentId == Notes.ID_CALL_RECORD_FOLDER) {
            mPhoneNumber = DataUtils.getCallNumberByNoteId(context.getContentResolver(), mId);
            if (!TextUtils.isEmpty(mPhoneNumber)) {
                // 获取联系人姓名
                mName = Contact.getContact(context, mPhoneNumber);
                if (mName == null) {
                    mName = mPhoneNumber; // 没有姓名则显示号码
                }
            }
        }

        // 检查当前item在列表中的位置
        checkPostion(cursor);
    }

    /**
     * 检查当前条目在列表中的位置
     * 用于判断列表布局样式（分割线、背景等）
     */
    private void checkPostion(Cursor cursor) {
        mIsLastItem = cursor.isLast();
        mIsFirstItem = cursor.isFirst();
        mIsOnlyOneItem = (cursor.getCount() == 1);
        mIsMultiNotesFollowingFolder = false;
        mIsOneNoteFollowingFolder = false;

        // 如果是笔记，且不是第一个，判断前面是不是文件夹
        if (mType == Notes.TYPE_NOTE && !mIsFirstItem) {
            int currentPosition = cursor.getPosition();
            if (cursor.moveToPrevious()) {
                int previousType = cursor.getInt(TYPE_COLUMN);
                // 前一项是文件夹/系统文件夹
                if (previousType == Notes.TYPE_FOLDER || previousType == Notes.TYPE_SYSTEM) {
                    if (cursor.getCount() > (currentPosition + 1)) {
                        // 文件夹后面有多个笔记
                        mIsMultiNotesFollowingFolder = true;
                    } else {
                        // 文件夹后面只有一个笔记
                        mIsOneNoteFollowingFolder = true;
                    }
                }
                // 移回原位置
                if (!cursor.moveToNext()) {
                    throw new IllegalStateException("cursor move to previous but can't move back");
                }
            }
        }
    }

    // ==================== 以下是各种getter方法 ====================
    public boolean isOneFollowingFolder() {
        return mIsOneNoteFollowingFolder;
    }

    public boolean isMultiFollowingFolder() {
        return mIsMultiNotesFollowingFolder;
    }

    public boolean isLast() {
        return mIsLastItem;
    }

    public String getCallName() {
        return mName;
    }

    public boolean isFirst() {
        return mIsFirstItem;
    }

    public boolean isSingle() {
        return mIsOnlyOneItem;
    }

    public long getId() {
        return mId;
    }

    public long getAlertDate() {
        return mAlertDate;
    }

    public long getCreatedDate() {
        return mCreatedDate;
    }

    public boolean hasAttachment() {
        return mHasAttachment;
    }

    public long getModifiedDate() {
        return mModifiedDate;
    }

    public int getBgColorId() {
        return mBgColorId;
    }

    public long getParentId() {
        return mParentId;
    }

    public int getNotesCount() {
        return mNotesCount;
    }

    public long getFolderId () {
        return mParentId;
    }

    public int getType() {
        return mType;
    }

    public int getWidgetType() {
        return mWidgetType;
    }

    public int getWidgetId() {
        return mWidgetId;
    }

    public String getSnippet() {
        return mSnippet;
    }

    /**
     * 是否设置了提醒
     */
    public boolean hasAlert() {
        return (mAlertDate > 0);
    }

    /**
     * 是否是通话记录笔记
     */
    public boolean isCallRecord() {
        return (mParentId == Notes.ID_CALL_RECORD_FOLDER && !TextUtils.isEmpty(mPhoneNumber));
    }

    /**
     * 静态方法：直接从Cursor获取笔记类型
     */
    public static int getNoteType(Cursor cursor) {
        return cursor.getInt(TYPE_COLUMN);
    }
}
