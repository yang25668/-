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
import android.text.format.DateUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.tool.DataUtils;
import net.micode.notes.tool.ResourceParser.NoteItemBgResources;

/**
 * 便签列表的每一项Item的自定义控件类
 * 继承自LinearLayout，用于展示单个便条/文件夹的UI
 */
public class NotesListItem extends LinearLayout {
    // 提醒闹钟图标
    private ImageView mAlert;
    // 标题/内容摘要文本
    private TextView mTitle;
    // 最后修改时间文本
    private TextView mTime;
    // 通话记录名称文本
    private TextView mCallName;
    // 列表项对应的数据实体
    private NoteItemData mItemData;
    // 多选模式下的选择框
    private CheckBox mCheckBox;

    /**
     * 构造方法：初始化列表项布局和控件
     * @param context 上下文
     */
    public NotesListItem(Context context) {
        super(context);
        // 加载列表项布局文件
        inflate(context, R.layout.note_item, this);
        // 初始化各个控件
        mAlert = (ImageView) findViewById(R.id.iv_alert_icon);
        mTitle = (TextView) findViewById(R.id.tv_title);
        mTime = (TextView) findViewById(R.id.tv_time);
        mCallName = (TextView) findViewById(R.id.tv_name);
        mCheckBox = (CheckBox) findViewById(android.R.id.checkbox);
    }

    /**
     * 绑定数据到列表项UI，刷新显示内容
     * @param context 上下文
     * @param data 列表项数据
     * @param choiceMode 是否处于选择模式
     * @param checked 选择框是否选中
     */
    public void bind(Context context, NoteItemData data, boolean choiceMode, boolean checked) {
        // 如果是选择模式，且数据类型为普通便签，则显示选择框
        if (choiceMode && data.getType() == Notes.TYPE_NOTE) {
            mCheckBox.setVisibility(View.VISIBLE);
            mCheckBox.setChecked(checked);
        } else {
            // 非选择模式隐藏选择框
            mCheckBox.setVisibility(View.GONE);
        }

        // 保存当前数据实体
        mItemData = data;
        // 判断是否为【通话记录文件夹】
        if (data.getId() == Notes.ID_CALL_RECORD_FOLDER) {
            // 隐藏通话名称
            mCallName.setVisibility(View.GONE);
            // 显示图标
            mAlert.setVisibility(View.VISIBLE);
            // 设置标题样式
            mTitle.setTextAppearance(context, R.style.TextAppearancePrimaryItem);
            // 设置标题：文件夹名称 + 包含的便签数量
            mTitle.setText(context.getString(R.string.call_record_folder_name)
                    + context.getString(R.string.format_folder_files_count, data.getNotesCount()));
            // 设置图标为通话记录图标
            mAlert.setImageResource(R.drawable.call_record);
        } 
        // 判断是否为【通话记录文件夹内的便签】
        else if (data.getParentId() == Notes.ID_CALL_RECORD_FOLDER) {
            // 显示通话名称
            mCallName.setVisibility(View.VISIBLE);
            mCallName.setText(data.getCallName());
            // 设置标题样式
            mTitle.setTextAppearance(context,R.style.TextAppearanceSecondaryItem);
            // 设置内容摘要
            mTitle.setText(DataUtils.getFormattedSnippet(data.getSnippet()));
            // 判断是否有提醒，有则显示闹钟图标
            if (data.hasAlert()) {
                mAlert.setImageResource(R.drawable.clock);
                mAlert.setVisibility(View.VISIBLE);
            } else {
                mAlert.setVisibility(View.GONE);
            }
        } 
        // 普通便签/普通文件夹
        else {
            mCallName.setVisibility(View.GONE);
            mTitle.setTextAppearance(context, R.style.TextAppearancePrimaryItem);

            // 如果是文件夹类型
            if (data.getType() == Notes.TYPE_FOLDER) {
                // 标题：文件夹名称 + 包含数量
                mTitle.setText(data.getSnippet()
                        + context.getString(R.string.format_folder_files_count,
                                data.getNotesCount()));
                // 隐藏提醒图标
                mAlert.setVisibility(View.GONE);
            } 
            // 如果是普通便签
            else {
                // 设置内容摘要
                mTitle.setText(DataUtils.getFormattedSnippet(data.getSnippet()));
                // 判断是否有提醒
                if (data.hasAlert()) {
                    mAlert.setImageResource(R.drawable.clock);
                    mAlert.setVisibility(View.VISIBLE);
                } else {
                    mAlert.setVisibility(View.GONE);
                }
            }
        }
        // 设置最后修改时间（相对时间）
        mTime.setText(DateUtils.getRelativeTimeSpanString(data.getModifiedDate()));

        // 设置列表项背景
        setBackground(data);
    }

    /**
     * 根据数据类型和位置设置不同的背景样式
     * @param data 列表项数据
     */
    private void setBackground(NoteItemData data) {
        // 获取背景颜色ID
        int id = data.getBgColorId();
        // 如果是便签类型
        if (data.getType() == Notes.TYPE_NOTE) {
            if (data.isSingle() || data.isOneFollowingFolder()) {
                // 单条便签/文件夹后第一条便签
                setBackgroundResource(NoteItemBgResources.getNoteBgSingleRes(id));
            } else if (data.isLast()) {
                // 最后一条便签
                setBackgroundResource(NoteItemBgResources.getNoteBgLastRes(id));
            } else if (data.isFirst() || data.isMultiFollowingFolder()) {
                // 第一条便签/多个便签跟随文件夹
                setBackgroundResource(NoteItemBgResources.getNoteBgFirstRes(id));
            } else {
                // 普通中间便签
                setBackgroundResource(NoteItemBgResources.getNoteBgNormalRes(id));
            }
        } else {
            // 文件夹类型使用固定背景
            setBackgroundResource(NoteItemBgResources.getFolderBgRes());
        }
    }

    /**
     * 获取当前列表项绑定的数据实体
     * @return NoteItemData 数据对象
     */
    public NoteItemData getItemData() {
        return mItemData;
    }
}
