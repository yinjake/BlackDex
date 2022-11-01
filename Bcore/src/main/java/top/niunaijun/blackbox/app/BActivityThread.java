package top.niunaijun.blackbox.app;

import android.app.Application;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.os.Build;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import reflection.android.app.ActivityThread;
import reflection.android.app.ContextImpl;
import reflection.android.app.LoadedApk;
import top.niunaijun.blackbox.core.IBActivityThread;
import top.niunaijun.blackbox.core.VMCore;
import top.niunaijun.blackbox.entity.AppConfig;
import top.niunaijun.blackbox.core.IOCore;
import top.niunaijun.blackbox.entity.dump.DumpResult;
import top.niunaijun.blackbox.utils.FileUtils;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.BlackBoxCore;

/**
 * Created by Milk on 3/31/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
public class BActivityThread extends IBActivityThread.Stub {
    public static final String TAG = "BActivityThread";

    private static BActivityThread sBActivityThread;
    private AppBindData mBoundApplication;
    private Application mInitialApplication;
    private AppConfig mAppConfig;
    private final List<ProviderInfo> mProviders = new ArrayList<>();

    public static BActivityThread currentActivityThread() {
        if (sBActivityThread == null) {
            synchronized (BActivityThread.class) {
                if (sBActivityThread == null) {
                    sBActivityThread = new BActivityThread();
                }
            }
        }
        return sBActivityThread;
    }

    public static synchronized AppConfig getAppConfig() {
        return currentActivityThread().mAppConfig;
    }

    public static List<ProviderInfo> getProviders() {
        return currentActivityThread().mProviders;
    }

    public static String getAppProcessName() {
        if (getAppConfig() != null) {
            return getAppConfig().processName;
        } else if (currentActivityThread().mBoundApplication != null) {
            return currentActivityThread().mBoundApplication.processName;
        } else {
            return null;
        }
    }

    public static String getAppPackageName() {
        if (getAppConfig() != null) {
            return getAppConfig().packageName;
        } else if (currentActivityThread().mInitialApplication != null) {
            return currentActivityThread().mInitialApplication.getPackageName();
        } else {
            return null;
        }
    }

    public static Application getApplication() {
        return currentActivityThread().mInitialApplication;
    }

    public static int getAppPid() {
        return getAppConfig() == null ? -1 : getAppConfig().bpid;
    }

    public static int getAppUid() {
        return getAppConfig() == null ? 10000 : getAppConfig().buid;
    }

    public static int getBaseAppUid() {
        return getAppConfig() == null ? 10000 : getAppConfig().baseBUid;
    }

    public static int getUid() {
        return getAppConfig() == null ? -1 : getAppConfig().uid;
    }

    public static int getUserId() {
        return getAppConfig() == null ? 0 : getAppConfig().userId;
    }

    public void initProcess(AppConfig appConfig) {
        if (this.mAppConfig != null) {
            throw new RuntimeException("reject init process: " + appConfig.processName + ", this process is : " + this.mAppConfig.processName);
        }
        this.mAppConfig = appConfig;
    }

    public boolean isInit() {
        return mBoundApplication != null;
    }

    public void bindApplication(final String packageName, final String processName) {
        if (mAppConfig == null) {
            return;
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            final ConditionVariable conditionVariable = new ConditionVariable();
            new Handler(Looper.getMainLooper()).post(() -> {
                handleBindApplication(packageName, processName);
                conditionVariable.open();
            });
            conditionVariable.block();
        } else {
            handleBindApplication(packageName, processName);
        }
    }

    private synchronized void handleBindApplication(String packageName, String processName) {
        Log.e(TAG, "  111  "+packageName + "  "+processName);
        // 初始化Dump的返回的信息
        DumpResult result = new DumpResult();
        result.packageName = packageName;
        result.dir = new File(BlackBoxCore.get().getDexDumpDir(), packageName).getAbsolutePath();
        try {
            // 以下是获取需要多开应用的信息，然后对当前进程进行重新设置，因为本进程信息是宿主的。
            PackageInfo packageInfo = BlackBoxCore.getBPackageManager().getPackageInfo(packageName, PackageManager.GET_PROVIDERS, BActivityThread.getUserId());
            Log.e(TAG, "  222  "+packageInfo + "  "+packageInfo.providers);
            if (packageInfo == null)
                return;
            ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            if (packageInfo.providers == null) {
                packageInfo.providers = new ProviderInfo[]{};
            }
            mProviders.addAll(Arrays.asList(packageInfo.providers));

            Object boundApplication = ActivityThread.mBoundApplication.get(BlackBoxCore.mainThread());

            Context packageContext = createPackageContext(applicationInfo);
            Object loadedApk = ContextImpl.mPackageInfo.get(packageContext);
            LoadedApk.mSecurityViolation.set(loadedApk, false);
            // fix applicationInfo
            LoadedApk.mApplicationInfo.set(loadedApk, applicationInfo);

            // clear dump file
            // 此处清除dump目录，防止多次脱壳的dex文件乱窜
            // clear dump file
            FileUtils.deleteDir(new File(BlackBoxCore.get().getDexDumpDir(), packageName));

            // init vmCore
            // 初始化native层代码
            VMCore.init(Build.VERSION.SDK_INT);
            assert packageContext != null;
            // 启用IO重定向，支持虚拟应用运行环境
            IOCore.get().enableRedirect(packageContext);

            AppBindData bindData = new AppBindData();
            bindData.appInfo = applicationInfo;
            bindData.processName = processName;
            bindData.info = loadedApk;
            bindData.providers = mProviders;

            ActivityThread.AppBindData.instrumentationName.set(boundApplication,
                    new ComponentName(bindData.appInfo.packageName, Instrumentation.class.getName()));
            ActivityThread.AppBindData.appInfo.set(boundApplication, bindData.appInfo);
            ActivityThread.AppBindData.info.set(boundApplication, bindData.info);
            ActivityThread.AppBindData.processName.set(boundApplication, bindData.processName);
            ActivityThread.AppBindData.providers.set(boundApplication, bindData.providers);

            mBoundApplication = bindData;

            Application application = null;
            BlackBoxCore.get().getAppLifecycleCallback().beforeCreateApplication(packageName, processName, packageContext);
            // 反射LoadedApk获取多开应用的classloader，并且反射LoadedApk#makeApplication函数，makeApplication中会初始化Application，调用其attachBaseContext、onCreate函数，完成Application的初始化。
            try {
                ClassLoader call = LoadedApk.getClassloader.call(loadedApk);
                application = LoadedApk.makeApplication.call(loadedApk, false, null);
                Log.e(TAG, "  777  " + "  "+application);
            } catch (Throwable e) {
                Log.e(TAG, "  777 catch 了   ");
                Slog.e(TAG, "Unable to makeApplication");
                e.printStackTrace();
            }
            mInitialApplication = application;
            ActivityThread.mInitialApplication.set(BlackBoxCore.mainThread(), mInitialApplication);

            // 如果走到此处没有发生异常，说明Application已经完成启动，一般在这种时候从理论上来说，应用已经运行起来了，那么自然加固也已经解密完成，我们接下来进行核心的dex的dump工作。
            if (Objects.equals(packageName, processName)) {
                Log.e(TAG, "  333到这啦  "+packageName + "  "+processName);
                ClassLoader loader;
                // 此处获取需要脱壳的app的classloader
                if (application == null) {
                    loader = LoadedApk.getClassloader.call(loadedApk);
                    Log.e(TAG, "  444  "+loader + "  "+application + " " + loadedApk);
                } else {
                    // 实际上走到这里，理论是启动失败了。不过还可以挣扎一下。
                    loader = application.getClassLoader();
                    Log.e(TAG, "  555  "+loader);
                }
                // 调用核心DumpDex方法，进行dex的dump工作
                handleDumpDex(packageName, result, loader);
            }
        } catch (Throwable e) {
            // 如果发生异常，通知UI并且从BlackBox中卸载该应用
            e.printStackTrace();
            Log.e(TAG, "  失败了  ");
            mAppConfig = null;
            BlackBoxCore.getBDumpManager().noticeMonitor(result.dumpError(e.getMessage()));
            BlackBoxCore.get().uninstallPackage(packageName);
        }
    }

    private void handleDumpDex(String packageName, DumpResult result, ClassLoader classLoader) {
        Log.e(TAG, "  888  "+packageName + "  "+result +"  "+classLoader);
        new Thread(() -> {
            Log.e(TAG, "  8888___111  ");
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
            try {
                Log.e(TAG, "  999___111 ");
                VMCore.cookieDumpDex(classLoader, packageName);
                Log.e(TAG, "  999  ");
            } finally {
                Log.e(TAG, "  101010___111  ");
                mAppConfig = null;
                File dir = new File(result.dir);
                if (!dir.exists() || dir.listFiles().length == 0) {
                    BlackBoxCore.getBDumpManager().noticeMonitor(result.dumpError("not found dex file"));
                    Log.e(TAG, "  101010  ");
                } else {
                    BlackBoxCore.getBDumpManager().noticeMonitor(result.dumpSuccess());
                    Log.e(TAG, "  1111aaaaa  ");
                }
                BlackBoxCore.get().uninstallPackage(packageName);
            }
        }).start();
    }

    private Context createPackageContext(ApplicationInfo info) {
        try {
            return BlackBoxCore.getContext().createPackageContext(info.packageName,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public IBinder getActivityThread() {
        return ActivityThread.getApplicationThread.call(BlackBoxCore.mainThread());
    }

    @Override
    public void bindApplication() {
        if (!isInit()) {
            bindApplication(getAppPackageName(), getAppProcessName());
        }
    }

    public static class AppBindData {
        String processName;
        ApplicationInfo appInfo;
        List<ProviderInfo> providers;
        Object info;
    }
}
