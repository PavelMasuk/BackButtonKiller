package com.example.myfirstxposedmodule;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.VibrationEffect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.example.myfirstxposedmodule.Classes.EdgeBackGestureHandler;
import static com.example.myfirstxposedmodule.Classes.ModPackageName;
import static com.example.myfirstxposedmodule.Classes.NavigationBarEdgePanel;

public class MyModule implements IXposedHookLoadPackage {
    private static boolean initialized = false;

    private static int screenWidth;
    private static int screenHeight;

    private static boolean didReachCenter = false;
    private static boolean didVibrate = false;
    private static boolean startFromLeft = false;
    private static boolean triggerBack = true;
    private static int backupArrowColor;
    private static Object vibrationHelper;
    private static ValueAnimator arrowColorAnimator;

    @SuppressLint("StaticFieldLeak")
    private static View navBarEdgePanel;
    @SuppressLint("StaticFieldLeak")
    static Context context;

//
//    private static final int mKillDelay = 300;
//    private static final int mTorchDelay = 500;
//    private static final int mVolumeLongPressDelay = 500;

//    private static final String CLASS_WINDOW_MANAGER_FUNCS = "com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs";
//    private static final String CLASS_IWINDOW_MANAGER = "android.view.IWindowManager";
//    private static final String CLASS_PHONE_WINDOW_MANAGER = "com.android.server.policy.PhoneWindowManager";

//    private static Object mPhoneWindowManager;
//    private static PowerManager mPowerManager;
    //    private static AudioManager mAudioManager;
//    private static PowerManager.WakeLock mWakeLock;
//    private static boolean mPowerLongPressInterceptedByTorch;
//    private static boolean mTorchStatus;

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
        final PackageManager packageManager = context.getPackageManager();
        String defaultHomePackage = "com.android.launcher";
        intent.addCategory(Intent.CATEGORY_HOME);
        final ResolveInfo res = packageManager.resolveActivity(intent, 0);
        if (res.activityInfo != null && !res.activityInfo.packageName.equals("android"))
            defaultHomePackage = res.activityInfo.packageName;
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        //noinspection deprecation
        List<ActivityManager.RunningTaskInfo> apps = activityManager.getRunningTasks(1);

        String targetKilled = null;
        if (apps.size() > 0) {
            ComponentName cn = apps.get(0).topActivity;
            if (!cn.getPackageName().equals("com.android.systemui") && !cn.getPackageName().startsWith(defaultHomePackage)) {
                targetKilled = cn.getPackageName();
                try {
                    Object service = XposedHelpers.callMethod(activityManager, "getService");
                    XposedHelpers.callMethod(service, "removeTask", apps.get(0).taskId);
                } catch (Throwable ignore) {
                }
            }
        }
        if (targetKilled != null) {
            try {
                targetKilled = (String) packageManager.getApplicationLabel(packageManager.getApplicationInfo(targetKilled, 0));
            } catch (PackageManager.NameNotFoundException ignored) {
            }
            Toast.makeText(context, targetKilled + " killed", Toast.LENGTH_SHORT).show();
        } else
            Toast.makeText(context, "Nothing to kill", Toast.LENGTH_SHORT).show();
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

        if (lpparam.packageName.equals(ModPackageName) && !initialized) {
            Classes.initClasses(lpparam.classLoader);
            initBackGestureMod();
        }
    }

    private static void initBackGestureMod() {
        XposedHelpers.findAndHookMethod(NavigationBarEdgePanel, "handleMoveEvent", MotionEvent.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                navBarEdgePanel = (View) param.thisObject;
                vibrationHelper = XposedHelpers.getObjectField(param.thisObject, "mVibratorHelper");
                triggerBack = (boolean) XposedHelpers.getObjectField(param.thisObject, "mTriggerBack");
                arrowColorAnimator = (ValueAnimator) XposedHelpers.getObjectField(param.thisObject, "mArrowColorAnimator");
            }
        });

        XposedHelpers.findAndHookMethod(NavigationBarEdgePanel, "updateIsDark", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (didReachCenter)
                    param.setResult(0);
            }
        });

        XposedHelpers.findAndHookMethod(EdgeBackGestureHandler, "onMotionEvent", MotionEvent.class, new XC_MethodHook() {
            @SuppressWarnings("IntegerDivisionInFloatingPointContext")
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                MotionEvent event = (MotionEvent) param.args[0];
                if (context == null) {
                    context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    Point size = new Point();
                    ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRealSize(size);
                    screenWidth = Math.min(size.x, size.y);
                    screenHeight = Math.max(size.x, size.y);
                }
                boolean mAllowGesture = (boolean) XposedHelpers.getObjectField(param.thisObject, "mAllowGesture");
                int orientation = context.getResources().getConfiguration().orientation;
                int currentWidth = orientation == Configuration.ORIENTATION_PORTRAIT ? screenWidth : screenHeight;
                int divider = orientation == Configuration.ORIENTATION_PORTRAIT ? 2 : 3;
                int threshold = currentWidth / divider;

                if (event.getAction() == MotionEvent.ACTION_DOWN)
                    startFromLeft = event.getX() < currentWidth / 2;

                if (event.getAction() == MotionEvent.ACTION_MOVE && mAllowGesture) {
                    boolean condition = startFromLeft ? event.getX() > threshold : event.getX() < currentWidth - threshold;
                    if (condition && !didReachCenter) {
                        didReachCenter = true;
                        changeColor();
                        if (!didVibrate) {
                            vibrate();
                            didVibrate = true;
                        }
                    }
                    if (!condition && didReachCenter) {
                        didReachCenter = false;
                        revertColor();
                    }
                }

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    didVibrate = false;
                    if (didReachCenter) {
                        if (triggerBack)
                            killForegroundApp();
                        else
                            triggerBack = true;
                        didReachCenter = false;
                    }
                }
            }
        });

        initialized = true;
    }

    private static void changeColor() {
        backupArrowColor = (int) XposedHelpers.getIntField(navBarEdgePanel, "mCurrentArrowColor");
        setArrowColor(Color.RED);
    }

    private static void revertColor() {
        setArrowColor(backupArrowColor);
    }

    private static void setArrowColor(int color) {
        arrowColorAnimator.cancel();
        XposedHelpers.setIntField(navBarEdgePanel, "mCurrentArrowColor", color);
        ((Paint) XposedHelpers.getObjectField(navBarEdgePanel, "mPaint")).setColor(color);
        navBarEdgePanel.invalidate();
    }

    private static void vibrate() {
        XposedHelpers.callMethod(vibrationHelper, "vibrate", VibrationEffect.EFFECT_TICK);
    }
}