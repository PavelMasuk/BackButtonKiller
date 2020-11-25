package com.example.myfirstxposedmodule;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.widget.Toast;

import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.callbacks.XCallback;

public class MyModule implements IXposedHookLoadPackage {
    @SuppressLint("StaticFieldLeak")
    static Context mContext;
    private static Object mPhoneWindowManager;
    private static ActivityManager mActivityManager;
    private static final int mKillDelay = 300;
    private static boolean firstRun = true;

    private static final String CLASS_WINDOW_MANAGER_FUNCS = "com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs";
    private static final String CLASS_IWINDOW_MANAGER = "android.view.IWindowManager";
    private static final String CLASS_PHONE_WINDOW_MANAGER = "com.android.server.policy.PhoneWindowManager";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam.packageName.equals("android") && lpparam.processName.equals("android") && firstRun) {
            firstRun = false;
            Class<?> mPhoneWindowManagerClass = XposedHelpers.findClass(CLASS_PHONE_WINDOW_MANAGER, lpparam.classLoader);
            XposedHelpers.findAndHookMethod(mPhoneWindowManagerClass, "init", Context.class, CLASS_IWINDOW_MANAGER, CLASS_WINDOW_MANAGER_FUNCS, phoneWindowManagerInitHook);
            XposedHelpers.findAndHookMethod(mPhoneWindowManagerClass, "interceptKeyBeforeQueueing",
                    KeyEvent.class, int.class, new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            KeyEvent event = (KeyEvent) param.args[0];
                            Handler handler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");
                            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && (event.getFlags() & KeyEvent.FLAG_FROM_SYSTEM) != 0 && taskNotLocked()) {
                                if (event.getAction() != KeyEvent.ACTION_DOWN)
                                    handler.removeCallbacks(mBackLongPress);
                                else if (event.getRepeatCount() == 0) {
                                    handler.postDelayed(mBackLongPress, mKillDelay);
                                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, false, "Xposed - Back Key");
                                }
                            }
                        }
                    });
        }
    }

    private static final Runnable mBackLongPress = () -> {
        killForegroundApp();
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, false, "Xposed - Back Key Longpress");
    };

    private static final XC_MethodHook phoneWindowManagerInitHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            mPhoneWindowManager = param.thisObject;
            mContext = (Context) XposedHelpers.getObjectField(mPhoneWindowManager, "mContext");
        }
    };

    private static ActivityManager getActivityManager() {
        if (mActivityManager == null)
            mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        return mActivityManager;
    }

    private static boolean taskNotLocked() {
        return getActivityManager().getLockTaskModeState() == ActivityManager.LOCK_TASK_MODE_NONE;
    }

    private static void killForegroundApp() {
        Handler handler = (Handler) XposedHelpers.getObjectField(mPhoneWindowManager, "mHandler");
        if (handler == null) return;

        handler.post(() -> {
            try {
                final Intent intent = new Intent(Intent.ACTION_MAIN);
                final PackageManager pm = mContext.getPackageManager();
                String defaultHomePackage = "com.android.launcher";
                intent.addCategory(Intent.CATEGORY_HOME);
                final ResolveInfo res = pm.resolveActivity(intent, 0);
                if (res.activityInfo != null && !res.activityInfo.packageName.equals("android"))
                    defaultHomePackage = res.activityInfo.packageName;
                ActivityManager am = getActivityManager();
                //noinspection deprecation
                List<ActivityManager.RunningTaskInfo> apps = am.getRunningTasks(1);

                String targetKilled = null;
                if (apps.size() > 0) {
                    ComponentName cn = apps.get(0).topActivity;
                    if (!cn.getPackageName().equals("com.android.systemui") && !cn.getPackageName().startsWith(defaultHomePackage)) {
                        XposedHelpers.callMethod(am, "forceStopPackage", cn.getPackageName());
                        targetKilled = cn.getPackageName();
                        try {
                            Object service = XposedHelpers.callMethod(am, "getService");
                            //noinspection deprecation
                            XposedHelpers.callMethod(service, "removeTask", apps.get(0).id);
                        } catch (Throwable ignore) {
                        }
                    }
                }

                if (targetKilled != null) {
                    try {
                        targetKilled = (String) pm.getApplicationLabel(pm.getApplicationInfo(targetKilled, 0));
                    } catch (PackageManager.NameNotFoundException ignored) {
                    }
                    performHapticFeedback(HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING, true, "Xposed - App killed");
                    Toast.makeText(mContext, targetKilled + " killed", Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(mContext, "Nothing to kill", Toast.LENGTH_SHORT).show();
            } catch (Throwable ignored) {
            }
        });
    }

    private static void performHapticFeedback(int effect, boolean always, String reason) {
        try {
            XposedHelpers.callMethod(mPhoneWindowManager, "performHapticFeedback", new Class<?>[]{int.class, boolean.class, String.class}, effect, always, reason);
        } catch (Throwable ignored) {
        }
    }
}