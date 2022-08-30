package com.qihoo360.replugin.utils;

import android.content.Context;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author : zsf
 * @date : 2021/7/20 10:46 AM
 * @desc :
 */
public class ZLog {

    private static final int length = 1024 * 3;

    private static final int I = 0;
    private static final int W = 1;
    private static final int D = 2;
    private static final int E = 3;

    private static final String TAG_PREFIX = "RePlugin";
    private static int CLASS_METHOD_STACK_DEPTH = 5;
    private static final String processName = getCurrentProcessName();
    private static long currentTime = System.currentTimeMillis();
    private static boolean logSwitch = true;

    private static Context appContext = null;

    public static void setIsDebug(boolean isDebug) {
        logSwitch = isDebug;
    }

    public static void init(boolean isDebug, Context context) {
        logSwitch = isDebug;
        appContext = context;
        d("ZLog", "init success; isDebug : " + isDebug);
    }

    public static void i(String tag, String msg) {
        if (!logSwitch) {
            return;
        }
        Log.i(getAssemblyTag(tag), getWholeStackDepthInfo(false, msg));
    }

    public static void d(String tag, String msg) {
        if (!logSwitch) {
            return;
        }
        Log.d(getAssemblyTag(tag), getWholeStackDepthInfo(false, msg));
    }

    public static void e(String tag, String msg) {
        if (!logSwitch) {
            return;
        }
        Log.e(getAssemblyTag(tag), getWholeStackDepthInfo(false, msg));
    }

    public static void ii(String tag, String msg) {
        if (!logSwitch) {
            return;
        }
        Log.i(getAssemblyTag(tag), getWholeStackDepthInfo(true, msg));
    }

    public static void dd(String tag, String msg) {
        if (!logSwitch) {
            return;
        }
        Log.d(getAssemblyTag(tag), getWholeStackDepthInfo(true, msg));
    }

    public static void ee(String tag, String msg) {
        if (!logSwitch) {
            return;
        }
        Log.e(getAssemblyTag(tag), getWholeStackDepthInfo(true, msg));
    }

    /**
     * 耗时
     *
     * @param tag
     * @param msg
     */
    public static void timeConsuming(String tag, String msg) {
        if (!logSwitch) {
            return;
        }
        long now = System.currentTimeMillis();
        Log.e(tag, "msg <=> " + (now - currentTime));
        currentTime = now;
    }

    public static void writeException(Exception e) {
        PrintStream stream = null;
        try {
            File file = getErrorPath();
            if (file == null) {
                return;
            }
            stream = new PrintStream(file);
            e.printStackTrace(stream);
            stream.flush();
        } catch (Exception w) {
            w.printStackTrace();
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    private static File getErrorPath() {
        if (appContext == null) {
            return null;
        }
        File path = new File(appContext.getExternalCacheDir(), "/error");
        if (!path.exists()) {
            path.mkdirs();
        }
        return new File(path, System.currentTimeMillis() + ".txt");
    }

    public static String getCurrentMethod(int depth) {
        return Thread.currentThread().getStackTrace()[depth].getMethodName();
    }

    public static String getCurrentClass(int depth) {
        return Thread.currentThread().getStackTrace()[depth].getClassName();
    }

    public static String getWholeStackDepthInfo(boolean isWholeStackDepth, String msg) {
        StringBuilder sb = new StringBuilder();
        if (isWholeStackDepth) {
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            int depth = stackTraceElements.length;
            for (int i = depth - 1; i >= CLASS_METHOD_STACK_DEPTH; i--) {
                String methodName = getCurrentMethod(i);
                String className = getCurrentClass(i);
                if (i == depth - 1) {
                    sb.append("\n .\n \n __________________________________________________ 【BEGIN】 __________________________________________________ \n |\n");
                    sb.append(" |    >>> 当前进程 : " + processName + " \n \n");
                }
                if (i == CLASS_METHOD_STACK_DEPTH) {
                    sb.append(" | \n |    >>> " + className + " -> " + methodName + " 】-- msg --> 【 " + msg + " 】\n |");
                    continue;
                }
                sb.append(" |    >>> " + className + " -> " + methodName + " \n");
            }
            sb.append("\n |__________________________________________________ 【END】 __________________________________________________ \n \n \n ");
        } else {
            String methodName = getCurrentMethod(CLASS_METHOD_STACK_DEPTH);
            String className = getCurrentClass(CLASS_METHOD_STACK_DEPTH);
            sb.append("   【进程 : " + processName + "】【 " + className + " -> " + methodName + " 】 <<<-- msg -->>> " + msg);
        }
        return sb.toString();
    }

    private static String getAssemblyTag(String tag) {
        return TAG_PREFIX + "_" + tag;
    }

    /**
     * 返回当前的进程名
     *
     * @return
     */
    public static String getCurrentProcessName() {
        FileInputStream in = null;
        try {
            String fn = "/proc/self/cmdline";
            in = new FileInputStream(fn);
            byte[] buffer = new byte[256];
            int len = 0;
            int b;
            while ((b = in.read()) > 0 && len < buffer.length) {
                buffer[len++] = (byte) b;
            }
            if (len > 0) {
                String s = new String(buffer, 0, len, "UTF-8");
                return s;
            }
        } catch (Throwable e) {
            e(TAG_PREFIX, e.getMessage());
            e.printStackTrace();
        } finally {
            closeQuietly(in);
        }
        return null;
    }

    /**
     * 大部分Close关闭流，以及实现Closeable的功能可使用此方法
     *
     * @param c Closeable对象，包括Stream等
     */
    public static void closeQuietly(Closeable c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (final IOException ioe) {
            // ignore
        }
    }


    public static <K, V> void mapToString(Map<K, V> map) {
        CLASS_METHOD_STACK_DEPTH = 6;
        Iterator<Map.Entry<K, V>> i = map.entrySet().iterator();
        if (!i.hasNext()) {
            d(TAG_PREFIX, "{}");
            CLASS_METHOD_STACK_DEPTH = 5;
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{\n \n ");
        for (; ; ) {
            Map.Entry<K, V> e = i.next();
            K key = e.getKey();
            V value = e.getValue();
            sb.append(key);
            sb.append("  =  ");
            sb.append(value.toString());
            if (!i.hasNext()) {
                d(TAG_PREFIX, sb.append("\n\n}").toString());
                CLASS_METHOD_STACK_DEPTH = 5;
                return;
            }
            sb.append(",\n").append(' ');
        }
    }

    public static <K> void setToString(Set<K> set) {
        CLASS_METHOD_STACK_DEPTH = 6;
        Iterator<K> i = set.iterator();
        if (!i.hasNext()) {
            d(TAG_PREFIX, "{}");
            CLASS_METHOD_STACK_DEPTH = 5;
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{\n \n ");
        for (; ; ) {
            Object e = i.next();
            sb.append(e.toString());
            if (!i.hasNext()) {
                d(TAG_PREFIX, sb.append("\n\n}").toString());
                CLASS_METHOD_STACK_DEPTH = 5;
                return;
            }
            sb.append(",\n").append(' ');
        }
    }

    public static <K> void listToString(List<K> ks) {
        CLASS_METHOD_STACK_DEPTH = 6;
        Iterator<K> i = ks.iterator();
        if (!i.hasNext()) {
            d(TAG_PREFIX, "{}");
            CLASS_METHOD_STACK_DEPTH = 5;
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{\n \n ");
        for (; ; ) {
            Object e = i.next();
            sb.append(e.toString());
            if (!i.hasNext()) {
                d(TAG_PREFIX, sb.append("\n\n}").toString());
                CLASS_METHOD_STACK_DEPTH = 5;
                return;
            }
            sb.append(",\n").append(' ');
        }
    }

    private static void writeLongMsg(int type, String tag, String msg) {
        if (msg.length() <= length) {
            handleLog(type, tag, msg);
            return;
        }
        for (int i = 0; i < msg.length(); i += length) {
            if (i + length < msg.length()) {
                handleLog(type, tag + "<=>" + i, msg.substring(i, i + length));
            } else {
                handleLog(type, tag + "<=>" + i, msg.substring(i));
            }
        }
    }

    private static void handleLog(int type, String tag, String msg) {
        switch (type) {
            case D:
                Log.d(tag, msg);
                break;
            case W:
                Log.w(tag, msg);
                break;
            case E:
                Log.e(tag, msg);
                break;
            default:
                Log.i(tag, msg);
                break;
        }
    }

}
