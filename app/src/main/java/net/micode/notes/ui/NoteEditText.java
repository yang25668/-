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
import android.graphics.Rect;
import android.text.Layout;
import android.text.Selection;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.widget.EditText;

import net.micode.notes.R;

import java.util.HashMap;
import java.util.Map;

/**
 * 自定义笔记编辑框控件
 * 功能：
 * 1. 支持清单模式下的回车新增、删除合并
 * 2. 支持点击链接/电话/邮箱自动识别
 * 3. 处理焦点、键盘事件，与外部页面通信
 * 专门用于清单模式（复选框列表）的编辑项
 */
public class NoteEditText extends EditText {
    private static final String TAG = "NoteEditText";

    private int mIndex;                           // 当前编辑框在列表中的位置索引
    private int mSelectionStartBeforeDelete;     // 删除前记录光标位置

    // 链接类型常量
    private static final String SCHEME_TEL = "tel:" ;       // 电话
    private static final String SCHEME_HTTP = "http:" ;     // 网页
    private static final String SCHEME_EMAIL = "mailto:" ;  // 邮箱

    // 链接类型 → 对应显示文字 的映射
    private static final Map<String, Integer> sSchemaActionResMap = new HashMap<String, Integer>();
    static {
        sSchemaActionResMap.put(SCHEME_TEL, R.string.note_link_tel);
        sSchemaActionResMap.put(SCHEME_HTTP, R.string.note_link_web);
        sSchemaActionResMap.put(SCHEME_EMAIL, R.string.note_link_email);
    }

    /**
     * 编辑框状态变化回调接口
     * 由 NoteEditActivity 实现，用于处理列表增删改
     */
    public interface OnTextViewChangeListener {
        /**
         * 删除当前编辑框（内容为空时按删除键）
         */
        void onEditTextDelete(int index, String text);

        /**
         * 回车新增编辑框
         */
        void onEditTextEnter(int index, String text);

        /**
         * 文字变化（显示/隐藏复选框）
         */
        void onTextChange(int index, boolean hasText);
    }

    private OnTextViewChangeListener mOnTextViewChangeListener;

    public NoteEditText(Context context) {
        super(context, null);
        mIndex = 0; // 默认索引为0
    }

    /**
     * 设置当前编辑框在列表中的索引
     */
    public void setIndex(int index) {
        mIndex = index;
    }

    /**
     * 设置状态变化监听器
     */
    public void setOnTextViewChangeListener(OnTextViewChangeListener listener) {
        mOnTextViewChangeListener = listener;
    }

    public NoteEditText(Context context, AttributeSet attrs) {
        super(context, attrs, android.R.attr.editTextStyle);
    }

    public NoteEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * 触摸事件：点击哪里光标就跳到哪里
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 获取触摸坐标
                int x = (int) event.getX();
                int y = (int) event.getY();

                // 减去内边距，得到文本区域坐标
                x -= getTotalPaddingLeft();
                y -= getTotalPaddingTop();

                // 加上滚动偏移
                x += getScrollX();
                y += getScrollY();

                // 获取点击位置对应的文字偏移量
                Layout layout = getLayout();
                int line = layout.getLineForVertical(y);
                int off = layout.getOffsetForHorizontal(line, x);

                // 设置光标位置
                Selection.setSelection(getText(), off);
                break;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 按键按下：处理回车、删除
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
                // 回车事件交给上层处理，这里不拦截
                if (mOnTextViewChangeListener != null) {
                    return false;
                }
                break;
            case KeyEvent.KEYCODE_DEL:
                // 记录删除前的光标位置
                mSelectionStartBeforeDelete = getSelectionStart();
                break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 按键抬起：处理删除、回车逻辑
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch(keyCode) {
            case KeyEvent.KEYCODE_DEL:
                if (mOnTextViewChangeListener != null) {
                    // 如果光标在最前面，且不是第一个item → 删除当前行
                    if (0 == mSelectionStartBeforeDelete && mIndex != 0) {
                        mOnTextViewChangeListener.onEditTextDelete(mIndex, getText().toString());
                        return true;
                    }
                }
                break;

            case KeyEvent.KEYCODE_ENTER:
                if (mOnTextViewChangeListener != null) {
                    // 回车：把光标后的文字切到新行
                    int selectionStart = getSelectionStart();
                    String text = getText().subSequence(selectionStart, length()).toString();
                    setText(getText().subSequence(0, selectionStart));
                    // 通知上层新增一行
                    mOnTextViewChangeListener.onEditTextEnter(mIndex + 1, text);
                }
                break;

            default:
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * 焦点变化：空内容隐藏复选框
     */
    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if (mOnTextViewChangeListener != null) {
            // 失去焦点 + 内容为空 → 隐藏复选框
            if (!focused && TextUtils.isEmpty(getText())) {
                mOnTextViewChangeListener.onTextChange(mIndex, false);
            } else {
                mOnTextViewChangeListener.onTextChange(mIndex, true);
            }
        }
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    /**
     * 创建长按菜单：识别链接（电话/网页/邮箱）
     */
    @Override
    protected void onCreateContextMenu(ContextMenu menu) {
        if (getText() instanceof Spanned) {
            int selStart = getSelectionStart();
            int selEnd = getSelectionEnd();

            int min = Math.min(selStart, selEnd);
            int max = Math.max(selStart, selEnd);

            // 获取选中区域的链接
            final URLSpan[] urls = ((Spanned) getText()).getSpans(min, max, URLSpan.class);
            if (urls.length == 1) {
                int defaultResId = 0;

                // 判断链接类型
                for(String schema: sSchemaActionResMap.keySet()) {
                    if(urls[0].getURL().indexOf(schema) >= 0) {
                        defaultResId = sSchemaActionResMap.get(schema);
                        break;
                    }
                }

                // 未识别则显示其他
                if (defaultResId == 0) {
                    defaultResId = R.string.note_link_other;
                }

                // 添加菜单，点击直接打开链接
                menu.add(0, 0, 0, defaultResId).setOnMenuItemClickListener(
                        new OnMenuItemClickListener() {
                            public boolean onMenuItemClick(MenuItem item) {
                                urls[0].onClick(NoteEditText.this);
                                return true;
                            }
                        });
            }
        }
        super.onCreateContextMenu(menu);
    }
}
