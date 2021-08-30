package com.example.myfirstxposedmodule;

import de.robv.android.xposed.XposedHelpers;

public class Classes {
    static final String ModPackageName = "com.android.systemui";

    static final String String_NavigationBarEdgePanel = "com.android.systemui.statusbar.phone.NavigationBarEdgePanel";
    static final String String_EdgeBackGestureHandler = "com.android.systemui.statusbar.phone.EdgeBackGestureHandler";

    static Class<?> NavigationBarEdgePanel;
    static Class<?> EdgeBackGestureHandler;

    static void initClasses(ClassLoader classLoader){
        NavigationBarEdgePanel = XposedHelpers.findClass(String_NavigationBarEdgePanel, classLoader);
        EdgeBackGestureHandler = XposedHelpers.findClass(String_EdgeBackGestureHandler, classLoader);
    }
}
