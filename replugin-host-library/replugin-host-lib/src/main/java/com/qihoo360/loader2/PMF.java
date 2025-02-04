/*
 * Copyright (C) 2005-2017 Qihoo 360 Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed To in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.qihoo360.loader2;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.text.TextUtils;

import com.qihoo360.i.Factory;
import com.qihoo360.i.Factory2;
import com.qihoo360.i.IModule;
import com.qihoo360.loader.utils.PatchClassLoaderUtils;
import com.qihoo360.replugin.helper.LogRelease;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import static com.qihoo360.replugin.helper.LogDebug.PLUGIN_TAG;
import static com.qihoo360.replugin.helper.LogRelease.LOGR;

/**
 * 框架和主程序接口代码
 *
 * @author RePlugin Team
 */
public class PMF {

    private static Context sContext;

    static PmBase sPluginMgr;

    /**
     * @param context
     */
    private static final void setApplicationContext(Context context) {
        sContext = context;
    }

    /**
     * @return
     */
    public static final Context getApplicationContext() {
        return sContext;
    }

    /**
     * @param application
     */
    public static final void init(Application application) {
        // 保持对Application的引用
        setApplicationContext(application);

        // 1.这里创建了一个叫 Tasks 的类，在里面中创建了一个主线程的 Handler
        // 2.通过当前进程的名字判断应该将插件分配到哪个进程中,目前通过 sPluginProcessIndex 标识
        // 3.存储当前进程uid到静态变量 PluginManager.sUid
        PluginManager.init(application);

        // PmBase 是 RePlugin 中非常重要的对象，它本身和它内部引用的其他对象掌握了RePlugin中很多重要的功能
        // PmBase 它本身和它内部引用的其他对象掌握了 RePlugin 中很多重要的功能,例如：分配坑位、初始化插件信息、Client 端连接 Server 端、加载插件、更新插件、删除插件、等等
        // 根据当前进程类型，拼接坑位provider和Service所对应名称并存入不同的HashSet中，PmBase类中处理保存了Provider、Service、Activitiy的坑位信息，
        // 这些名字全部都是Replugin在编译的时候在AndroidManifest.xml中声明的坑位名字
        sPluginMgr = new PmBase(application);
        sPluginMgr.init();

        // 将在PmBase构造中创建的 PluginCommImpl 赋值给 Factory.sPluginManager
        Factory.sPluginManager = PMF.getLocal();
        // 将在PmBase构造中创建的 PluginLibraryInternalProxy 赋值给 Factory2.sPLProxy
        Factory2.sPLProxy = PMF.getInternal();

        // Replugin唯一hook点 hook系统ClassLoader
        PatchClassLoaderUtils.patch(application);
    }

    /**
     *
     */
    public static final void callAppCreate() {
        sPluginMgr.callAppCreate();
    }

    /**
     *
     */
    public static final void callAttach() {
        sPluginMgr.callAttach();
    }

    /**
     * @param name
     * @param modc
     * @param module
     */
    public static final void addBuiltinModule(String name, Class<? extends IModule> modc, IModule module) {
        sPluginMgr.addBuiltinModule(name, modc, module);
    }

    /**
     * @return
     */
    public static final PluginCommImpl getLocal() {
        return sPluginMgr.mLocal;
    }

    /**
     * @return
     */
    public static final PluginLibraryInternalProxy getInternal() {
        return sPluginMgr.mInternal;
    }

    /**
     * @param className
     * @param resolve
     * @return
     */
    public static final Class<?> loadClass(String className, boolean resolve) {
        return sPluginMgr.loadClass(className, resolve);
    }

    /**
     * @param activity
     * @param intent
     */
    public static final void forward(Activity activity, Intent intent) {
        //
        activity.finish();

        //
        try {
            PluginIntent ii = new PluginIntent(intent);
            // 原容器
            String original = ii.getOriginal();
            if (TextUtils.isEmpty(original)) {
                if (LOGR) {
                    LogRelease.e(PLUGIN_TAG, "f.a f: orig=nul i=" + intent);
                }
                return;
            }
            // 容器，检查
            String container = ii.getContainer();
            if (TextUtils.isEmpty(container)) {
                if (LOGR) {
                    LogRelease.e(PLUGIN_TAG, "f.a f: c=nul i=" + intent);
                }
                return;
            }
            // 目标插件，检查
            String plugin = ii.getPlugin();
            if (TextUtils.isEmpty(plugin)) {
                if (LOGR) {
                    LogRelease.e(PLUGIN_TAG, "f.a f: n=nul i=" + intent);
                }
                return;
            }
            // 目标activity，检查
            String target = ii.getActivity();
            if (TextUtils.isEmpty(target)) {
                if (LOGR) {
                    LogRelease.e(PLUGIN_TAG, "f.a f: t=nul i=" + intent);
                }
                return;
            }
            // 进程，检查
            int process = ii.getProcess();
            if (!PluginManager.isValidActivityProcess(process)) {
                if (LOGR) {
                    LogRelease.e(PLUGIN_TAG, "f.a f: p=" + process + " i=" + intent);
                }
                return;
            }
            // 计数器，检查
            int counter = ii.getCounter();
            if (counter < 0 || counter >= PluginManager.COUNTER_MAX) {
                if (LOGR) {
                    LogRelease.e(PLUGIN_TAG, "f.a f: ooc c=" + counter);
                }
                return;
            }
            // 计数器，递增
            counter++;
            ii.setCounter(counter);
            //
            sPluginMgr.mClient.mACM.forwardIntent(activity, intent, original, container, plugin, target, process);
        } catch (Throwable e) {
            if (LOGR) {
                LogRelease.e(PLUGIN_TAG, "f.a f: " + e.getMessage(), e);
            }
        }
    }

    public static final void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        sPluginMgr.dump(fd, writer, args);
    }

    // 只为PluginServiceServer调用而准备，不对外公开
    // Added by Jiongxuan Zhang
    public static void stopService(Intent intent) throws RemoteException {
        sPluginMgr.mClient.fetchServiceServer().stopService(intent, null);
    }
}
