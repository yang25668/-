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

package net.micode.notes.tool;

import android.content.Context;
import android.preference.PreferenceManager;

import net.micode.notes.R;
import net.micode.notes.ui.NotesPreferenceActivity;

/**
 * 资源解析工具类
 * 作用：统一管理笔记背景、字体大小、列表样式、小部件样式等资源ID
 * 方便界面根据配置获取对应的图片/样式资源
 */
public class ResourceParser {

    // ==================== 笔记背景颜色常量定义 ====================
    public static final int YELLOW           = 0; // 黄色
    public static final int BLUE             = 1; // 蓝色
    public static final int WHITE            = 2; // 白色
    public static final int GREEN            = 3; // 绿色
    public static final int RED              = 4; // 红色

    // 默认背景颜色（黄色）
    public static final int BG_DEFAULT_COLOR = YELLOW;

    // ==================== 字体大小常量定义 ====================
    public static final int TEXT_SMALL       = 0; // 小字体
    public static final int TEXT_MEDIUM      = 1; // 中字体
    public static final int TEXT_LARGE       = 2; // 大字体
    public static final int TEXT_SUPER       = 3; // 超大字体

    // 默认字体大小（中等）
    public static final int BG_DEFAULT_FONT_SIZE = TEXT_MEDIUM;

    /**
     * 编辑页面背景资源
     * 提供笔记编辑界面的背景图片
     */
    public static class NoteBgResources {
        // 编辑页面整体背景资源数组
        private final static int [] BG_EDIT_RESOURCES = new int [] {
            R.drawable.edit_yellow,
            R.drawable.edit_blue,
            R.drawable.edit_white,
            R.drawable.edit_green,
            R.drawable.edit_red
        };

        // 编辑页面标题栏背景资源数组
        private final static int [] BG_EDIT_TITLE_RESOURCES = new int [] {
            R.drawable.edit_title_yellow,
            R.drawable.edit_title_blue,
            R.drawable.edit_title_white,
            R.drawable.edit_title_green,
            R.drawable.edit_title_red
        };

        // 根据颜色ID获取编辑页背景图
        public static int getNoteBgResource(int id) {
            return BG_EDIT_RESOURCES[id];
        }

        // 根据颜色ID获取编辑页标题栏背景图
        public static int getNoteTitleBgResource(int id) {
            return BG_EDIT_TITLE_RESOURCES[id];
        }
    }

    /**
     * 获取默认背景颜色ID
     * 如果开启随机背景，则随机返回一种颜色；否则返回默认黄色
     */
    public static int getDefaultBgId(Context context) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                NotesPreferenceActivity.PREFERENCE_SET_BG_COLOR_KEY, false)) {
            return (int) (Math.random() * NoteBgResources.BG_EDIT_RESOURCES.length);
        } else {
            return BG_DEFAULT_COLOR;
        }
    }

    /**
     * 列表项背景资源
     * 用于笔记列表的顶部、中间、底部、单独项、文件夹的背景样式
     */
    public static class NoteItemBgResources {
        // 列表第一项背景
        private final static int [] BG_FIRST_RESOURCES = new int [] {
            R.drawable.list_yellow_up,
            R.drawable.list_blue_up,
            R.drawable.list_white_up,
            R.drawable.list_green_up,
            R.drawable.list_red_up
        };

        // 列表中间项背景
        private final static int [] BG_NORMAL_RESOURCES = new int [] {
            R.drawable.list_yellow_middle,
            R.drawable.list_blue_middle,
            R.drawable.list_white_middle,
            R.drawable.list_green_middle,
            R.drawable.list_red_middle
        };

        // 列表最后一项背景
        private final static int [] BG_LAST_RESOURCES = new int [] {
            R.drawable.list_yellow_down,
            R.drawable.list_blue_down,
            R.drawable.list_white_down,
            R.drawable.list_green_down,
            R.drawable.list_red_down,
        };

        // 列表单独一项的背景
        private final static int [] BG_SINGLE_RESOURCES = new int [] {
            R.drawable.list_yellow_single,
            R.drawable.list_blue_single,
            R.drawable.list_white_single,
            R.drawable.list_green_single,
            R.drawable.list_red_single
        };

        // 获取第一项背景
        public static int getNoteBgFirstRes(int id) {
            return BG_FIRST_RESOURCES[id];
        }

        // 获取最后一项背景
        public static int getNoteBgLastRes(int id) {
            return BG_LAST_RESOURCES[id];
        }

        // 获取单独项背景
        public static int getNoteBgSingleRes(int id) {
            return BG_SINGLE_RESOURCES[id];
        }

        // 获取中间项背景
        public static int getNoteBgNormalRes(int id) {
            return BG_NORMAL_RESOURCES[id];
        }

        // 获取文件夹专用背景
        public static int getFolderBgRes() {
            return R.drawable.list_folder;
        }
    }

    /**
     * 桌面小部件背景资源
     * 提供 2x2 / 4x4 大小小部件的背景图片
     */
    public static class WidgetBgResources {
        // 2x2 小部件背景
        private final static int [] BG_2X_RESOURCES = new int [] {
            R.drawable.widget_2x_yellow,
            R.drawable.widget_2x_blue,
            R.drawable.widget_2x_white,
            R.drawable.widget_2x_green,
            R.drawable.widget_2x_red,
        };

        public static int getWidget2xBgResource(int id) {
            return BG_2X_RESOURCES[id];
        }

        // 4x4 小部件背景
        private final static int [] BG_4X_RESOURCES = new int [] {
            R.drawable.widget_4x_yellow,
            R.drawable.widget_4x_blue,
            R.drawable.widget_4x_white,
            R.drawable.widget_4x_green,
            R.drawable.widget_4x_red
        };

        public static int getWidget4xBgResource(int id) {
            return BG_4X_RESOURCES[id];
        }
    }

    /**
     * 文字样式资源（字体大小、外观）
     */
    public static class TextAppearanceResources {
        // 字体样式数组（小、中、大、超大）
        private final static int [] TEXTAPPEARANCE_RESOURCES = new int [] {
            R.style.TextAppearanceNormal,
            R.style.TextAppearanceMedium,
            R.style.TextAppearanceLarge,
            R.style.TextAppearanceSuper
        };

        // 根据ID获取字体样式，处理越界异常
        public static int getTexAppearanceResource(int id) {
            /**
             * 修复bug：修复共享偏好中存储的资源ID越界问题
             * 如果ID超出范围，返回默认中等字体大小
             */
            if (id >= TEXTAPPEARANCE_RESOURCES.length) {
                return BG_DEFAULT_FONT_SIZE;
            }
            return TEXTAPPEARANCE_RESOURCES[id];
        }

        // 获取字体样式总数量
        public static int getResourcesSize() {
            return TEXTAPPEARANCE_RESOURCES.length;
        }
    }
}
