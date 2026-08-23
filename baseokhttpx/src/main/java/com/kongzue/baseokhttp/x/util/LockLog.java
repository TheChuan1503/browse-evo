package com.kongzue.baseokhttp.x.util;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author: Kongzue
 * @github: https://github.com/kongzue/
 * @homepage: http://kongzue.com/
 * @mail: myzcxhh@live.cn
 * @createTime: 2025/6/18
 */
public class LockLog {

    public static String TAG_RETURN = "<<<";
    public static String TAG_SEND = ">>>";

    private static final BlockingQueue<LogBody> logQueue = new LinkedBlockingQueue<>();
    /**
     * 日志回调监听
     */
    public static LogListener logListener;

    static {
        Thread logThread = new Thread(() -> {
            try {
                while (true) {
                    LogBody log = logQueue.take(); // 阻塞等待日志
                    switch (log.getLevel()) {
                        case INFO:
                            Log.i(log.getTag(), log.getLog());
                            if (logListener != null) {
                                logListener.log(LogBody.LEVEL.INFO, log.getLog());
                            }
                            break;
                        case ERROR:
                            Log.e(log.getTag(), log.getLog());
                            if (logListener != null) {
                                logListener.log(LogBody.LEVEL.ERROR, log.getLog());
                            }
                            break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        logThread.setDaemon(true);
        logThread.start();
    }

    /**
     * 输出 INFO 等级日志
     *
     * @param tag 日志标签
     * @param s   日志内容
     */
    public static void logI(String tag, String s) {
        logQueue.offer(new LogBody(LogBody.LEVEL.INFO, tag, s));
    }

    /**
     * 输出 ERROR 等级日志并携带异常信息
     */
    public static void logE(String tag, Throwable e) {
        logQueue.offer(new LogBody(LogBody.LEVEL.ERROR, tag, getExceptionInfo(e)));
    }

    /**
     * 输出 ERROR 等级日志
     */
    public static void logE(String tag, String e) {
        logQueue.offer(new LogBody(LogBody.LEVEL.ERROR, tag, e));
    }

    /**
     * 批量输出日志
     */
    public static void log(List<LogBody> s) {
        if (s != null) {
            for (LogBody log : s) {
                if (log != null) {
                    logQueue.offer(log);
                }
            }
        }
    }

    public static class LogBody {

        public enum LEVEL {
            INFO,
            ERROR
        }

        private String tag, log;
        private final LEVEL level;

        /**
         * 创建指定等级的日志体
         */
        public LogBody(LEVEL level, String tag, String log) {
            this.tag = tag;
            this.log = log;
            this.level = level;
        }

        /**
         * 创建 INFO 等级日志体
         */
        public LogBody(String tag, String log) {
            this.tag = tag;
            this.log = log;
            this.level = LEVEL.INFO;
        }

        /**
         * 获取日志标签
         */
        public String getTag() {
            return tag == null ? TAG_SEND : tag;
        }

        /**
         * 设置日志标签
         */
        public LogBody setTag(String tag) {
            this.tag = tag;
            return this;
        }

        /**
         * 获取日志等级
         */
        public LEVEL getLevel() {
            return level;
        }

        /**
         * 获取日志内容
         */
        public String getLog() {
            return log == null ? "" : log;
        }

        /**
         * 设置日志内容
         */
        public LogBody setLog(String log) {
            this.log = log;
            return this;
        }
    }

    public static class Builder {

        private final List<LogBody> list;

        /**
         * 构造方法
         */
        private Builder() {
            list = new CopyOnWriteArrayList<>();
        }

        /**
         * 创建 Builder 实例
         */
        public static Builder create() {
            return new Builder();
        }

        /**
         * 添加 INFO 日志
         */
        public Builder i(String tag, String s) {
            list.add(new LogBody(tag, s));
            return this;
        }

        /**
         * 添加 ERROR 日志
         */
        public Builder e(String tag, String s) {
            list.add(new LogBody(LogBody.LEVEL.ERROR, tag, s));
            return this;
        }

        /**
         * 合并日志列表
         */
        public Builder add(List<LogBody> l) {
            if (l != null) list.addAll(l);
            return this;
        }

        /**
         * 输出构建的日志
         */
        public void build() {
            LockLog.log(list);
        }
    }

    /**
     * 获取异常堆栈信息
     */
    public static String getExceptionInfo(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        e.printStackTrace(pw);
        pw.flush();
        sw.flush();
        return sw.toString();
    }

    /**
     * 将 JSON 字符串格式化为日志列表
     */
    public static List<LogBody> formatJson(String msg) {
        String message;
        try {
            if (msg.startsWith("{")) {
                JSONObject jsonObject = new JSONObject(msg);
                message = jsonObject.toString(4);
            } else if (msg.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(msg);
                message = jsonArray.toString(4);
            } else {
                return null;
            }
        } catch (JSONException err) {
            return null;
        }

        String[] lines = message.split("\n");
        List<LogBody> logBodyList = new ArrayList<>();
        for (String line : lines) {
            logBodyList.add(new LogBody(LogBody.LEVEL.INFO, TAG_RETURN, line));
        }
        return logBodyList;
    }

    public interface LogListener {
        void log(LogBody.LEVEL level, String logs);
    }
}