package com.example.myfirstxposedmodule;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.callbacks.XCallback;

//import android.media.AudioManager;

public class MyModule implements IXposedHookLoadPackage {
    private static boolean didReachCenter = false;
    private static boolean didVibrate = false;
    private static boolean startFromLeft = false;
    private static boolean triggerBack = true;

//
//    private static final int mKillDelay = 300;
//    private static final int mTorchDelay = 500;
//    private static final int mVolumeLongPressDelay = 500;

//    private static final String CLASS_WINDOW_MANAGER_FUNCS = "com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs";
//    private static final String CLASS_IWINDOW_MANAGER = "android.view.IWindowManager";
//    private static final String CLASS_PHONE_WINDOW_MANAGER = "com.android.server.policy.PhoneWindowManager";

    @SuppressLint("StaticFieldLeak")
    static Context mContext;
//    private static Object mPhoneWindowManager;
//    private static PowerManager mPowerManager;
    //    private static AudioManager mAudioManager;
//    private static PowerManager.WakeLock mWakeLock;

    //    private static boolean firstRun = true;
//    private static boolean mPowerLongPressInterceptedByTorch;
//    private static boolean mTorchStatus;

    @SuppressLint("StaticFieldLeak")
    private static ViewGroup mStatusBarView;

//    private static final Runnable mBackLongPress = () -> {
//        killForegroundApp();
//        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, false, "Xposed - Back Key LongPress");
//    };
//    private static final Runnable mPowerLongPress = () -> {
//        toggleTorch();
//        mPowerLongPressInterceptedByTorch = true;
//        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, false, "Xposed - Power Key LongPress");
//    };
//    private static final Runnable mVolumeUpLongPress = () -> {
//        nextTrack();
//    };

//    private static final XC_MethodHook phoneWindowManagerInitHook = new XC_MethodHook() {
//        @Override
//        protected void afterHookedMethod(MethodHookParam param) {
//            mPhoneWindowManager = param.thisObject;
//            mContext = (Context) XposedHelpers.getObjectField(mPhoneWindowManager, "mContext");
//
//            ((CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE)).registerTorchCallback(new CameraManager.TorchCallback() {
//                @Override
//                public void onTorchModeChanged(String cameraId, boolean enabled) {
//                    super.onTorchModeChanged(cameraId, enabled);
//                    mTorchStatus = enabled;
//                }
//            }, new Handler(getMainLooper()));
//        }
//    };

    private static ActivityManager getActivityManager() {
        return (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
    }

//    private static PowerManager getPowerManager() {
//        if (mPowerManager == null)
//            mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
//        return mPowerManager;
//    }

//    private static AudioManager getAudioManager() {
//        if (mAudioManager == null)
//            mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
//        return mAudioManager;
//    }

//    private static boolean taskNotLocked() {
//        return getActivityManager().getLockTaskModeState() == ActivityManager.LOCK_TASK_MODE_NONE;
//    }

    private static void killForegroundApp() {
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
            Toast.makeText(mContext, targetKilled + " killed", Toast.LENGTH_SHORT).show();
        } else
            Toast.makeText(mContext, "Nothing to kill", Toast.LENGTH_SHORT).show();
    }

//    private static void toggleTorch() {
//        CameraManager mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
//        try {
//            mCameraManager.setTorchMode(mCameraManager.getCameraIdList()[0], !mTorchStatus);
//        } catch (CameraAccessException ignored) {
//        }
//    }
//
//    private static void performHapticFeedback(int effect, boolean always, String reason) {
//        try {
//            XposedHelpers.callMethod(mPhoneWindowManager, "performHapticFeedback", new Class<?>[]{int.class, boolean.class, String.class}, effect, always, reason);
//        } catch (Throwable ignored) {
//        }
//    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
//        if (lpparam.packageName.equals("android") || lpparam.processName.equals("android")) {
//
//            Class<?> mPhoneWindowManagerClass = XposedHelpers.findClass(CLASS_PHONE_WINDOW_MANAGER, lpparam.classLoader);
//            XposedHelpers.findAndHookMethod(mPhoneWindowManagerClass, "init", Context.class, CLASS_IWINDOW_MANAGER, CLASS_WINDOW_MANAGER_FUNCS, phoneWindowManagerInitHook);
//            XposedHelpers.findAndHookMethod(mPhoneWindowManagerClass, "interceptKeyBeforeQueueing",
//                    KeyEvent.class, int.class, new XC_MethodHook(XCallback.PRIORITY_DEFAULT) {
//                        @Override
//                        protected void beforeHookedMethod(MethodHookParam param) {
//                            KeyEvent event = (KeyEvent) param.args[0];
//                            Handler handler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");
//                            if ((event.getFlags() & KeyEvent.FLAG_FROM_SYSTEM) == 0 || !taskNotLocked())
//                                return;
//
//                            switch (event.getKeyCode()) {
//                                case KeyEvent.KEYCODE_BACK:
//                                    if (event.getAction() == KeyEvent.ACTION_UP)
//                                        handler.removeCallbacks(mBackLongPress);
//                                    else if (event.getRepeatCount() == 0) {
//                                        handler.postDelayed(mBackLongPress, mKillDelay);
//                                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, false, "Xposed - Back Key");
//                                    }
//                                    break;
//                                case KeyEvent.KEYCODE_POWER:
//                                    System.out.println("WOOORKING1");
//                                    if (getPowerManager().isInteractive())
//                                        break;
//
//                                    if (event.getAction() == KeyEvent.ACTION_UP) {
//                                        handler.removeCallbacks(mPowerLongPress);
//                                        if (mPowerLongPressInterceptedByTorch) {
//                                            mPowerLongPressInterceptedByTorch = false;
//                                            param.setResult(0);
//                                        } else if (mWakeLock != null) {
//                                            mWakeLock.acquire(1);
//                                            mWakeLock = null;
//                                        }
//                                    } else {
//                                        if (event.getRepeatCount() == 0) {
//                                            mPowerLongPressInterceptedByTorch = false;
//                                            //noinspection deprecation
//                                            mWakeLock = getPowerManager().newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, ":");
//                                            handler.postDelayed(mPowerLongPress, mTorchDelay);
//                                        }
//                                        param.setResult(0);
//                                    }
//                                    break;
////                            case KeyEvent.KEYCODE_VOLUME_UP:
////                                if (getPowerManager().isInteractive() || !getAudioManager().isMusicActive())
////                                    break;
////                                if (event.getAction() == KeyEvent.ACTION_UP)
////                                    handler.removeCallbacks(mVolumeUpLongPress);
////                                else if (event.getRepeatCount() == 0)
////                                    handler.postDelayed(mVolumeUpLongPress, mVolumeLongPressDelay);
////                                break;
////                            case KeyEvent.KEYCODE_VOLUME_DOWN:
////                                if (getPowerManager().isInteractive() || !getAudioManager().isMusicActive())
////                                    break;
////                                break;
//                            }
//                        }
//                    });
//        }
        if (lpparam.packageName.equals("com.android.systemui") || lpparam.processName.equals("com.android.systemui")) {
            final Class<?> statusBarClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.StatusBar", lpparam.classLoader);
            XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.PhoneStatusBarView", lpparam.classLoader,
                    "setBar", statusBarClass, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            mStatusBarView = (ViewGroup) param.thisObject;
                        }
                    });

            XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.NavigationBarEdgePanel", lpparam.classLoader,
                    "handleMoveEvent", MotionEvent.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            triggerBack = (boolean) XposedHelpers.getObjectField(param.thisObject, "mTriggerBack");
                        }
                    });

            Class<?> EdgeBackGestureHandler = XposedHelpers.findClass("com.android.systemui.statusbar.phone.EdgeBackGestureHandler", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(EdgeBackGestureHandler, "onMotionEvent", MotionEvent.class, new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    MotionEvent ev = (MotionEvent) param.args[0];
                    boolean mAllowGesture = (boolean) XposedHelpers.getObjectField(param.thisObject, "mAllowGesture");
                    if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                        startFromLeft = ev.getX() < 540;
                    }
                    if (ev.getAction() == MotionEvent.ACTION_MOVE && mAllowGesture) {
                        if (ev.getX() < 540 && !didReachCenter && !startFromLeft) {
                            didReachCenter = true;
                            if (!didVibrate) {
                                haptic2();
                                didVibrate = true;
                            }
                        }
                        if (ev.getX() > 540 && didReachCenter && !startFromLeft) {
                            didReachCenter = false;
//                            haptic2();
                        }
                        if (ev.getX() > 540 && !didReachCenter && startFromLeft) {
                            didReachCenter = true;
                            if (!didVibrate) {
                                haptic2();
                                didVibrate = true;
                            }
                        }
                        if (ev.getX() < 540 && didReachCenter && startFromLeft) {
                            didReachCenter = false;
//                            haptic2();
                        }
                    }
                    if (ev.getAction() == MotionEvent.ACTION_UP)
                        didVibrate = false;
                    if (ev.getAction() == MotionEvent.ACTION_UP && didReachCenter) {
                        didReachCenter = false;
                        didVibrate = false;
                        mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
//                        param.setResult(0);
                        if (triggerBack)
                            killForegroundApp();
                        else
                            triggerBack = true;
                    }
                }
            });
        }
    }

    private static void haptic2() {
        XposedHelpers.callMethod(mStatusBarView, "performHapticFeedback",
                HapticFeedbackConstants.LONG_PRESS);
    }
}