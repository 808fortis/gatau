package com.xposedyt;

import com.xposedyt.ads.HookAds;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final String YOUTUBE_PACKAGE = "com.google.android.youtube";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals(YOUTUBE_PACKAGE))
            return;

        XposedBridge.log("[xposedYT] loaded into YouTube");

        HookAds.hook(lpparam.classLoader);
    }
}
