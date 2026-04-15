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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.gtask.remote.GTaskSyncService;

/**
 * 小米便签的设置页面
 * 实现Google账号同步、背景设置、同步时间管理等功能
 */
public class NotesPreferenceActivity extends PreferenceActivity {
    // 共享偏好设置文件名称
    public static final String PREFERENCE_NAME = "notes_preferences";

    // 同步账号名称的存储Key
    public static final String PREFERENCE_SYNC_ACCOUNT_NAME = "pref_key_account_name";

    // 最后同步时间的存储Key
    public static final String PREFERENCE_LAST_SYNC_TIME = "pref_last_sync_time";

    // 背景颜色设置的存储Key
    public static final String PREFERENCE_SET_BG_COLOR_KEY = "pref_key_bg_random_appear";

    // 同步账号偏好设置的Key
    private static final String PREFERENCE_SYNC_ACCOUNT_KEY = "pref_sync_account_key";

    // 权限过滤Key
    private static final String AUTHORITIES_FILTER_KEY = "authorities";

    // 账号分类偏好设置
    private PreferenceCategory mAccountCategory;

    // 同步服务广播接收器
    private GTaskReceiver mReceiver;

    // 原始Google账号数组
    private Account[] mOriAccounts;

    // 标记是否添加了新账号
    private boolean mHasAddedAccount;

    /**
     * Activity创建时初始化
     */
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // 显示ActionBar返回图标
        getActionBar().setDisplayHomeAsUpEnabled(true);

        // 加载偏好设置布局
        addPreferencesFromResource(R.xml.preferences);
        // 绑定账号分类控件
        mAccountCategory = (PreferenceCategory) findPreference(PREFERENCE_SYNC_ACCOUNT_KEY);
        // 初始化广播接收器
        mReceiver = new GTaskReceiver();
        IntentFilter filter = new IntentFilter();
        // 注册同步服务的广播
        filter.addAction(GTaskSyncService.GTASK_SERVICE_BROADCAST_NAME);
        registerReceiver(mReceiver, filter);

        // 初始化原始账号为空
        mOriAccounts = null;
        // 加载设置页面头部布局
        View header = LayoutInflater.from(this).inflate(R.layout.settings_header, null);
        getListView().addHeaderView(header, null, true);
    }

    /**
     * Activity恢复到前台时调用
     */
    @Override
    protected void onResume() {
        super.onResume();

        // 如果用户新增了账号，自动设置同步账号
        if (mHasAddedAccount) {
            Account[] accounts = getGoogleAccounts();
            if (mOriAccounts != null && accounts.length > mOriAccounts.length) {
                for (Account accountNew : accounts) {
                    boolean found = false;
                    // 遍历对比新旧账号列表
                    for (Account accountOld : mOriAccounts) {
                        if (TextUtils.equals(accountOld.name, accountNew.name)) {
                            found = true;
                            break;
                        }
                    }
                    // 找到新增的账号，设置为同步账号
                    if (!found) {
                        setSyncAccount(accountNew.name);
                        break;
                    }
                }
            }
        }

        // 刷新UI显示
        refreshUI();
    }

    /**
     * Activity销毁时释放资源
     */
    @Override
    protected void onDestroy() {
        // 注销广播接收器，防止内存泄漏
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        super.onDestroy();
    }

    /**
     * 加载账号偏好设置项
     */
    private void loadAccountPreference() {
        // 清空账号分类下的所有控件
        mAccountCategory.removeAll();

        Preference accountPref = new Preference(this);
        final String defaultAccount = getSyncAccountName(this);
        // 设置账号项标题
        accountPref.setTitle(getString(R.string.preferences_account_title));
        // 设置账号项描述
        accountPref.setSummary(getString(R.string.preferences_account_summary));
        // 设置点击事件
        accountPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                // 同步中不允许修改账号
                if (!GTaskSyncService.isSyncing()) {
                    if (TextUtils.isEmpty(defaultAccount)) {
                        // 首次设置账号，显示选择账号对话框
                        showSelectAccountAlertDialog();
                    } else {
                        // 已设置账号，显示修改确认对话框
                        showChangeAccountConfirmAlertDialog();
                    }
                } else {
                    // 同步中提示无法修改账号
                    Toast.makeText(NotesPreferenceActivity.this,
                            R.string.preferences_toast_cannot_change_account, Toast.LENGTH_SHORT)
                            .show();
                }
                return true;
            }
        });

        // 将账号项添加到分类中
        mAccountCategory.addPreference(accountPref);
    }

    /**
     * 加载同步按钮和同步状态
     */
    private void loadSyncButton() {
        Button syncButton = (Button) findViewById(R.id.preference_sync_button);
        TextView lastSyncTimeView = (TextView) findViewById(R.id.prefenerece_sync_status_textview);

        // 设置同步按钮状态
        if (GTaskSyncService.isSyncing()) {
            // 同步中显示取消按钮
            syncButton.setText(getString(R.string.preferences_button_sync_cancel));
            syncButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // 取消同步
                    GTaskSyncService.cancelSync(NotesPreferenceActivity.this);
                }
            });
        } else {
            // 未同步显示立即同步按钮
            syncButton.setText(getString(R.string.preferences_button_sync_immediately));
            syncButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // 开始同步
                    GTaskSyncService.startSync(NotesPreferenceActivity.this);
                }
            });
        }
        // 未设置账号时禁用同步按钮
        syncButton.setEnabled(!TextUtils.isEmpty(getSyncAccountName(this)));

        // 设置最后同步时间
        if (GTaskSyncService.isSyncing()) {
            // 同步中显示进度
            lastSyncTimeView.setText(GTaskSyncService.getProgressString());
            lastSyncTimeView.setVisibility(View.VISIBLE);
        } else {
            long lastSyncTime = getLastSyncTime(this);
            if (lastSyncTime != 0) {
                // 显示格式化的最后同步时间
                lastSyncTimeView.setText(getString(R.string.preferences_last_sync_time,
                        DateFormat.format(getString(R.string.preferences_last_sync_time_format),
                                lastSyncTime)));
                lastSyncTimeView.setVisibility(View.VISIBLE);
            } else {
                // 无同步记录隐藏文本
                lastSyncTimeView.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 刷新整个设置页面UI
     */
    private void refreshUI() {
        loadAccountPreference();
        loadSyncButton();
    }

    /**
     * 显示选择Google账号的对话框
     */
    private void showSelectAccountAlertDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        // 自定义对话框标题
        View titleView = LayoutInflater.from(this).inflate(R.layout.account_dialog_title, null);
        TextView titleTextView = (TextView) titleView.findViewById(R.id.account_dialog_title);
        titleTextView.setText(getString(R.string.preferences_dialog_select_account_title));
        TextView subtitleTextView = (TextView) titleView.findViewById(R.id.account_dialog_subtitle);
        subtitleTextView.setText(getString(R.string.preferences_dialog_select_account_tips));

        dialogBuilder.setCustomTitle(titleView);
        dialogBuilder.setPositiveButton(null, null);

        // 获取设备上所有Google账号
        Account[] accounts = getGoogleAccounts();
        String defAccount = getSyncAccountName(this);

        mOriAccounts = accounts;
        mHasAddedAccount = false;

        if (accounts.length > 0) {
            CharSequence[] items = new CharSequence[accounts.length];
            final CharSequence[] itemMapping = items;
            int checkedItem = -1;
            int index = 0;
            // 组装账号列表
            for (Account account : accounts) {
                if (TextUtils.equals(account.name, defAccount)) {
                    // 默认选中已设置的账号
                    checkedItem = index;
                }
                items[index++] = account.name;
            }
            // 设置单选列表
            dialogBuilder.setSingleChoiceItems(items, checkedItem,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // 设置选中的账号为同步账号
                            setSyncAccount(itemMapping[which].toString());
                            dialog.dismiss();
                            refreshUI();
                        }
                    });
        }

        // 添加新增账号入口
        View addAccountView = LayoutInflater.from(this).inflate(R.layout.add_account_text, null);
        dialogBuilder.setView(addAccountView);

        final AlertDialog dialog = dialogBuilder.show();
        // 点击新增账号
        addAccountView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mHasAddedAccount = true;
                // 跳转到系统添加账号页面
                Intent intent = new Intent("android.settings.ADD_ACCOUNT_SETTINGS");
                intent.putExtra(AUTHORITIES_FILTER_KEY, new String[] {
                    "gmail-ls"
                });
                startActivityForResult(intent, -1);
                dialog.dismiss();
            }
        });
    }

    /**
     * 显示修改/移除账号的确认对话框
     */
    private void showChangeAccountConfirmAlertDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        // 自定义标题
        View titleView = LayoutInflater.from(this).inflate(R.layout.account_dialog_title, null);
        TextView titleTextView = (TextView) titleView.findViewById(R.id.account_dialog_title);
        titleTextView.setText(getString(R.string.preferences_dialog_change_account_title,
                getSyncAccountName(this)));
        TextView subtitleTextView = (TextView) titleView.findViewById(R.id.account_dialog_subtitle);
        subtitleTextView.setText(getString(R.string.preferences_dialog_change_account_warn_msg));
        dialogBuilder.setCustomTitle(titleView);

        // 对话框选项：修改账号、移除账号、取消
        CharSequence[] menuItemArray = new CharSequence[] {
                getString(R.string.preferences_menu_change_account),
                getString(R.string.preferences_menu_remove_account),
                getString(R.string.preferences_menu_cancel)
        };
        dialogBuilder.setItems(menuItemArray, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    // 选择修改账号
                    showSelectAccountAlertDialog();
                } else if (which == 1) {
                    // 选择移除账号
                    removeSyncAccount();
                    refreshUI();
                }
            }
        });
        dialogBuilder.show();
    }

    /**
     * 获取设备上所有Google账号
     * @return 账号数组
     */
    private Account[] getGoogleAccounts() {
        AccountManager accountManager = AccountManager.get(this);
        return accountManager.getAccountsByType("com.google");
    }

    /**
     * 设置同步使用的Google账号
     * @param account 账号名称
     */
    private void setSyncAccount(String account) {
        if (!getSyncAccountName(this).equals(account)) {
            // 保存账号到共享偏好
            SharedPreferences settings = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            if (account != null) {
                editor.putString(PREFERENCE_SYNC_ACCOUNT_NAME, account);
            } else {
                editor.putString(PREFERENCE_SYNC_ACCOUNT_NAME, "");
            }
            editor.commit();

            // 重置最后同步时间
            setLastSyncTime(this, 0);

            // 子线程清空本地便签的同步相关数据
            new Thread(new Runnable() {
                public void run() {
                    ContentValues values = new ContentValues();
                    values.put(NoteColumns.GTASK_ID, "");
                    values.put(NoteColumns.SYNC_ID, 0);
                    getContentResolver().update(Notes.CONTENT_NOTE_URI, values, null, null);
                }
            }).start();

            // 提示设置成功
            Toast.makeText(NotesPreferenceActivity.this,
                    getString(R.string.preferences_toast_success_set_accout, account),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 移除同步账号，清空相关配置
     */
    private void removeSyncAccount() {
        SharedPreferences settings = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        // 删除账号和同步时间配置
        if (settings.contains(PREFERENCE_SYNC_ACCOUNT_NAME)) {
            editor.remove(PREFERENCE_SYNC_ACCOUNT_NAME);
        }
        if (settings.contains(PREFERENCE_LAST_SYNC_TIME)) {
            editor.remove(PREFERENCE_LAST_SYNC_TIME);
        }
        editor.commit();

        // 子线程清空本地同步数据
        new Thread(new Runnable() {
            public void run() {
                ContentValues values = new ContentValues();
                values.put(NoteColumns.GTASK_ID, "");
                values.put(NoteColumns.SYNC_ID, 0);
                getContentResolver().update(Notes.CONTENT_NOTE_URI, values, null, null);
            }
        }).start();
    }

    /**
     * 静态方法：获取当前设置的同步账号名称
     */
    public static String getSyncAccountName(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        return settings.getString(PREFERENCE_SYNC_ACCOUNT_NAME, "");
    }

    /**
     * 静态方法：设置最后同步时间
     */
    public static void setLastSyncTime(Context context, long time) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(PREFERENCE_LAST_SYNC_TIME, time);
        editor.commit();
    }

    /**
     * 静态方法：获取最后同步时间
     */
    public static long getLastSyncTime(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        return settings.getLong(PREFERENCE_LAST_SYNC_TIME, 0);
    }

    /**
     * 同步服务广播接收器：监听同步状态变化
     */
    private class GTaskReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // 同步状态变化时刷新UI
            refreshUI();
            if (intent.getBooleanExtra(GTaskSyncService.GTASK_SERVICE_BROADCAST_IS_SYNCING, false)) {
                TextView syncStatus = (TextView) findViewById(R.id.prefenerece_sync_status_textview);
                // 显示同步进度信息
                syncStatus.setText(intent
                        .getStringExtra(GTaskSyncService.GTASK_SERVICE_BROADCAST_PROGRESS_MSG));
            }

        }
    }

    /**
     * 顶部导航栏菜单点击事件
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // 返回便签列表页面，清空栈顶
                Intent intent = new Intent(this, NotesListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            default:
                return false;
        }
    }
}
