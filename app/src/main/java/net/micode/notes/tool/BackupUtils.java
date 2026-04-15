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
import android.database.Cursor;
import android.os.Environment;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.DataConstants;
import net.micode.notes.data.Notes.NoteColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;


/**
 * 笔记备份工具类
 * 核心功能：将应用内的笔记、文件夹、通话记录导出为文本文件，保存到SD卡
 * 采用单例模式设计
 */
public class BackupUtils {
    // 日志TAG
    private static final String TAG = "BackupUtils";
    // 单例实例
    private static BackupUtils sInstance;

    /**
     * 获取单例对象（线程安全）
     * @param context 上下文
     * @return BackupUtils单例
     */
    public static synchronized BackupUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new BackupUtils(context);
        }
        return sInstance;
    }

    /**
     * 备份/还原状态常量定义
     * 用于标识导出/导入操作的执行结果
     */
    // SD卡未挂载
    public static final int STATE_SD_CARD_UNMOUONTED           = 0;
    // 备份文件不存在
    public static final int STATE_BACKUP_FILE_NOT_EXIST        = 1;
    // 数据格式损坏
    public static final int STATE_DATA_DESTROIED               = 2;
    // 系统运行时异常
    public static final int STATE_SYSTEM_ERROR                 = 3;
    // 操作成功
    public static final int STATE_SUCCESS                      = 4;

    // 文本导出内部类实例
    private TextExport mTextExport;

    /**
     * 私有构造方法（单例模式）
     * @param context 上下文
     */
    private BackupUtils(Context context) {
        mTextExport = new TextExport(context);
    }

    /**
     * 判断外部存储（SD卡）是否可用
     * @return 可用返回true，否则false
     */
    private static boolean externalStorageAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /**
     * 对外暴露的导出文本方法
     * @return 导出状态码
     */
    public int exportToText() {
        return mTextExport.exportToText();
    }

    /**
     * 获取导出的文本文件名
     * @return 文件名
     */
    public String getExportedTextFileName() {
        return mTextExport.mFileName;
    }

    /**
     * 获取导出文件的目录路径
     * @return 目录路径
     */
    public String getExportedTextFileDir() {
        return mTextExport.mFileDirectory;
    }

    /**
     * 文本导出内部实现类
     * 封装所有笔记导出为文本的具体逻辑
     */
    private static class TextExport {
        // 笔记查询投影：查询笔记表需要的字段
        private static final String[] NOTE_PROJECTION = {
                NoteColumns.ID,                // 笔记ID
                NoteColumns.MODIFIED_DATE,     // 修改时间
                NoteColumns.SNIPPET,           // 笔记摘要/文件夹名称
                NoteColumns.TYPE               // 类型（笔记/文件夹）
        };

        // 对应NOTE_PROJECTION数组的索引常量
        private static final int NOTE_COLUMN_ID = 0;
        private static final int NOTE_COLUMN_MODIFIED_DATE = 1;
        private static final int NOTE_COLUMN_SNIPPET = 2;

        // 数据详情查询投影：查询笔记内容表需要的字段
        private static final String[] DATA_PROJECTION = {
                DataColumns.CONTENT,           // 内容
                DataColumns.MIME_TYPE,         // 数据类型
                DataColumns.DATA1,             // 扩展字段1（通话日期）
                DataColumns.DATA2,             // 扩展字段2
                DataColumns.DATA3,             // 扩展字段3
                DataColumns.DATA4,             // 扩展字段4（电话号码）
        };

        // 对应DATA_PROJECTION数组的索引常量
        private static final int DATA_COLUMN_CONTENT = 0;
        private static final int DATA_COLUMN_MIME_TYPE = 1;
        private static final int DATA_COLUMN_CALL_DATE = 2;    // 通话日期
        private static final int DATA_COLUMN_PHONE_NUMBER = 4;  // 电话号码

        // 导出文本格式数组（从资源文件读取）
        private final String [] TEXT_FORMAT;
        // 文本格式索引常量
        private static final int FORMAT_FOLDER_NAME          = 0;  // 文件夹名称格式
        private static final int FORMAT_NOTE_DATE            = 1;  // 笔记日期格式
        private static final int FORMAT_NOTE_CONTENT         = 2;  // 笔记内容格式

        private Context mContext;       // 上下文
        private String mFileName;      // 导出文件名
        private String mFileDirectory; // 导出文件目录

        /**
         * 构造方法：初始化导出格式和上下文
         * @param context 上下文
         */
        public TextExport(Context context) {
            TEXT_FORMAT = context.getResources().getStringArray(R.array.format_for_exported_note);
            mContext = context;
            mFileName = "";
            mFileDirectory = "";
        }

        /**
         * 根据索引获取导出格式字符串
         * @param id 格式索引
         * @return 格式字符串
         */
        private String getFormat(int id) {
            return TEXT_FORMAT[id];
        }

        /**
         * 将指定文件夹下的所有笔记导出到文本流
         * @param folderId 文件夹ID
         * @param ps 打印输出流
         */
        private void exportFolderToText(String folderId, PrintStream ps) {
            // 查询该文件夹下的所有笔记
            Cursor notesCursor = mContext.getContentResolver().query(Notes.CONTENT_NOTE_URI,
                    NOTE_PROJECTION, NoteColumns.PARENT_ID + "=?", new String[] {
                        folderId
                    }, null);

            if (notesCursor != null) {
                // 遍历笔记数据
                if (notesCursor.moveToFirst()) {
                    do {
                        // 写入笔记最后修改时间
                        ps.println(String.format(getFormat(FORMAT_NOTE_DATE), DateFormat.format(
                                mContext.getString(R.string.format_datetime_mdhm),
                                notesCursor.getLong(NOTE_COLUMN_MODIFIED_DATE))));
                        // 获取笔记ID，导出该笔记的详细内容
                        String noteId = notesCursor.getString(NOTE_COLUMN_ID);
                        exportNoteToText(noteId, ps);
                    } while (notesCursor.moveToNext());
                }
                // 关闭游标，释放资源
                notesCursor.close();
            }
        }

        /**
         * 将指定笔记的详细内容导出到文本流
         * @param noteId 笔记ID
         * @param ps 打印输出流
         */
        private void exportNoteToText(String noteId, PrintStream ps) {
            // 查询该笔记的详细数据
            Cursor dataCursor = mContext.getContentResolver().query(Notes.CONTENT_DATA_URI,
                    DATA_PROJECTION, DataColumns.NOTE_ID + "=?", new String[] {
                        noteId
                    }, null);

            if (dataCursor != null) {
                if (dataCursor.moveToFirst()) {
                    do {
                        // 获取数据类型
                        String mimeType = dataCursor.getString(DATA_COLUMN_MIME_TYPE);
                        // 如果是通话记录类型笔记
                        if (DataConstants.CALL_NOTE.equals(mimeType)) {
                            // 获取通话记录相关信息
                            String phoneNumber = dataCursor.getString(DATA_COLUMN_PHONE_NUMBER);
                            long callDate = dataCursor.getLong(DATA_COLUMN_CALL_DATE);
                            String location = dataCursor.getString(DATA_COLUMN_CONTENT);

                            // 写入电话号码
                            if (!TextUtils.isEmpty(phoneNumber)) {
                                ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                        phoneNumber));
                            }
                            // 写入通话日期
                            ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT), DateFormat
                                    .format(mContext.getString(R.string.format_datetime_mdhm),
                                            callDate)));
                            // 写入通话归属地
                            if (!TextUtils.isEmpty(location)) {
                                ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                        location));
                            }
                        } else if (DataConstants.NOTE.equals(mimeType)) {
                            // 如果是普通文本笔记
                            String content = dataCursor.getString(DATA_COLUMN_CONTENT);
                            if (!TextUtils.isEmpty(content)) {
                                ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                        content));
                            }
                        }
                    } while (dataCursor.moveToNext());
                }
                // 关闭游标
                dataCursor.close();
            }
            // 笔记之间写入分隔符，区分不同笔记
            try {
                ps.write(new byte[] {
                        Character.LINE_SEPARATOR, Character.LETTER_NUMBER
                });
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }

        /**
         * 核心导出方法：将所有文件夹、笔记、通话记录导出为文本文件
         * @return 导出状态码
         */
        public int exportToText() {
            // 检查SD卡是否挂载
            if (!externalStorageAvailable()) {
                Log.d(TAG, "Media was not mounted");
                return STATE_SD_CARD_UNMOUONTED;
            }

            // 获取文件输出流
            PrintStream ps = getExportToTextPrintStream();
            if (ps == null) {
                Log.e(TAG, "get print stream error");
                return STATE_SYSTEM_ERROR;
            }
            // 第一步：导出所有文件夹（排除垃圾箱，包含通话记录文件夹）
            Cursor folderCursor = mContext.getContentResolver().query(
                    Notes.CONTENT_NOTE_URI,
                    NOTE_PROJECTION,
                    "(" + NoteColumns.TYPE + "=" + Notes.TYPE_FOLDER + " AND "
                            + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER + ") OR "
                            + NoteColumns.ID + "=" + Notes.ID_CALL_RECORD_FOLDER, null, null);

            if (folderCursor != null) {
                if (folderCursor.moveToFirst()) {
                    do {
                        // 获取文件夹名称
                        String folderName = "";
                        // 特殊处理：通话记录文件夹名称
                        if(folderCursor.getLong(NOTE_COLUMN_ID) == Notes.ID_CALL_RECORD_FOLDER) {
                            folderName = mContext.getString(R.string.call_record_folder_name);
                        } else {
                            // 普通文件夹名称
                            folderName = folderCursor.getString(NOTE_COLUMN_SNIPPET);
                        }
                        // 写入文件夹名称
                        if (!TextUtils.isEmpty(folderName)) {
                            ps.println(String.format(getFormat(FORMAT_FOLDER_NAME), folderName));
                        }
                        // 导出该文件夹下的所有笔记
                        String folderId = folderCursor.getString(NOTE_COLUMN_ID);
                        exportFolderToText(folderId, ps);
                    } while (folderCursor.moveToNext());
                }
                folderCursor.close();
            }

            // 第二步：导出根目录下的普通笔记（不属于任何文件夹）
            Cursor noteCursor = mContext.getContentResolver().query(
                    Notes.CONTENT_NOTE_URI,
                    NOTE_PROJECTION,
                    NoteColumns.TYPE + "=" + +Notes.TYPE_NOTE + " AND " + NoteColumns.PARENT_ID
                            + "=0", null, null);

            if (noteCursor != null) {
                if (noteCursor.moveToFirst()) {
                    do {
                        // 写入笔记修改时间
                        ps.println(String.format(getFormat(FORMAT_NOTE_DATE), DateFormat.format(
                                mContext.getString(R.string.format_datetime_mdhm),
                                noteCursor.getLong(NOTE_COLUMN_MODIFIED_DATE))));
                        // 导出笔记内容
                        String noteId = noteCursor.getString(NOTE_COLUMN_ID);
                        exportNoteToText(noteId, ps);
                    } while (noteCursor.moveToNext());
                }
                noteCursor.close();
            }
            // 关闭输出流
            ps.close();

            // 导出成功
            return STATE_SUCCESS;
        }

        /**
         * 创建并获取导出文件的打印流
         * @return PrintStream 对象，失败返回null
         */
        private PrintStream getExportToTextPrintStream() {
            // 生成SD卡上的导出文件
            File file = generateFileMountedOnSDcard(mContext, R.string.file_path,
                    R.string.file_name_txt_format);
            if (file == null) {
                Log.e(TAG, "create file to exported failed");
                return null;
            }
            // 记录文件名和目录
            mFileName = file.getName();
            mFileDirectory = mContext.getString(R.string.file_path);
            PrintStream ps = null;
            try {
                // 创建文件输出流
                FileOutputStream fos = new FileOutputStream(file);
                ps = new PrintStream(fos);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            } catch (NullPointerException e) {
                e.printStackTrace();
                return null;
            }
            return ps;
        }
    }

    /**
     * 在SD卡上生成导出文件（带日期命名）
     * @param context 上下文
     * @param filePathResId 文件路径资源ID
     * @param fileNameFormatResId 文件名格式资源ID
     * @return 生成的文件对象，失败返回null
     */
    private static File generateFileMountedOnSDcard(Context context, int filePathResId, int fileNameFormatResId) {
        StringBuilder sb = new StringBuilder();
        // 拼接SD卡根目录
        sb.append(Environment.getExternalStorageDirectory());
        // 拼接文件保存路径
        sb.append(context.getString(filePathResId));
        File filedir = new File(sb.toString());
        // 拼接文件名（带当前日期）
        sb.append(context.getString(
                fileNameFormatResId,
                DateFormat.format(context.getString(R.string.format_date_ymd),
                        System.currentTimeMillis())));
        File file = new File(sb.toString());

        try {
            // 目录不存在则创建
            if (!filedir.exists()) {
                filedir.mkdir();
            }
            // 文件不存在则创建
            if (!file.exists()) {
                file.createNewFile();
            }
            return file;
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 创建失败返回null
        return null;
    }
}
