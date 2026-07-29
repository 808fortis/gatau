/*
 * Adapted from Morphe (https://github.com/MorpheApp/morphe-patches)
 * Original: ReVanced patches (GPLv3)
 */

package com.xposedyt.ads;

import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public final class HookAds {

    private static final List<String> AD_PATH_PATTERNS = Arrays.asList(
            "_ad_with",
            "_buttoned_layout",
            "ads_video_with_context",
            "banner_text_icon",
            "brand_video_shelf",
            "brand_video_singleton",
            "carousel_ad",
            "carousel_footered_layout",
            "carousel_headered_layout",
            "full_width_square_image_layout",
            "hero_promo_image",
            "image_button_group_layout",
            "primetime_promo",
            "product_details",
            "shopping_timely_shelf",
            "square_image_layout",
            "text_image_button_layout",
            "video_display_button_group_layout",
            "watch_metadata_app_promo",
            "product_carousel",
            "shopping_carousel",
            "paid_content_overlay",
            "cta_shelf_card",
            "products_in_video",
            "product_item",
            "shopping_overlay",
            "shopping_description_item",
            "shopping_description_shelf",
            "browsy_bar",
            "compact_movie",
            "offer_module_root"
    );

    private static final List<String> PLAYER_POPUP_AD_PANELS = Arrays.asList(
            "PAproduct",
            "jumpahead"
    );

    private UnifiedAdsHook() {}

    public static void hook(ClassLoader classLoader) {
        hookLithoComponents(classLoader);
        hookAdViewCreation(classLoader);
        hookGetPremiumView(classLoader);
        hookBuildProperties(classLoader);
        hookInnerTubeConnection(classLoader);
        hookAdPlayerOverlay(classLoader);
    }

    private static void hookLithoComponents(ClassLoader cl) {
        try {
            Class<?> lithoViewClass = null;
            try {
                lithoViewClass = cl.loadClass("com.facebook.litho.LithoView");
            } catch (ClassNotFoundException e) {
                try {
                    lithoViewClass = cl.loadClass("com.facebook.litho.ComponentTree");
                } catch (ClassNotFoundException ignored) {}
            }

            if (lithoViewClass != null) {
                XposedBridge.log("[xposedYT] Found Litho, hooking component mount");

                for (Method m : lithoViewClass.getDeclaredMethods()) {
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length > 0 && View.class.isAssignableFrom(params[0])) {
                        XposedBridge.log("[xposedYT] Hooking Litho method: " + m.getName());
                        XposedHelpers.findAndHookMethod(lithoViewClass, m.getName(),
                                getParamArray(params),
                                new XC_MethodHook() {
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) {
                                        hideAdView(param);
                                    }
                                });
                    }
                }
            } else {
                XposedBridge.log("[xposedYT] Litho not found, using fallback ViewGroup hook");
                hookViewGroupAddView(cl);
            }
        } catch (Throwable t) {
            XposedBridge.log("[xposedYT] Litho hook error: " + t.getMessage());
            hookViewGroupAddView(cl);
        }
    }

    private static void hookViewGroupAddView(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod(ViewGroup.class, "addView", View.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            View view = (View) param.args[0];
                            if (isAdView(view)) {
                                view.setVisibility(View.GONE);
                            }
                        }
                    });
            XposedBridge.log("[xposedYT] ViewGroup.addView hooked (fallback)");
        } catch (Throwable t) {
            XposedBridge.log("[xposedYT] ViewGroup hook failed: " + t.getMessage());
        }
    }

    private static void hookAdViewCreation(ClassLoader cl) {
        try {
            Class<?>[] allClasses = findClassesWithPrefix(cl, "com.google.android.apps.youtube");
            for (Class<?> cls : allClasses) {
                if (cls == null) continue;
                String name = cls.getName();
                if (name.contains("ad") || name.contains("Ad") || name.contains("ads")) {
                    for (Method m : cls.getDeclaredMethods()) {
                        if (m.getReturnType() == void.class &&
                                m.getParameterCount() == 1 &&
                                View.class.isAssignableFrom(m.getParameterTypes()[0])) {
                            XposedHelpers.findAndHookMethod(cls, m.getName(), View.class,
                                    new XC_MethodHook() {
                                        @Override
                                        protected void beforeHookedMethod(MethodHookParam param) {
                                            View view = (View) param.args[0];
                                            view.setVisibility(View.GONE);
                                        }
                                    });
                        }
                    }
                }
            }
        } catch (Throwable t) {
            XposedBridge.log("[xposedYT] Ad view hook: " + t.getMessage());
        }
    }

    private static void hookGetPremiumView(ClassLoader cl) {
        try {
            Class<?> premiumView = cl.loadClass(
                    "com.google.android.apps.youtube.app.red.presenter.CompactYpcOfferModuleView");
            XposedBridge.log("[xposedYT] Found Premium view, hooking onMeasure");

            XposedHelpers.findAndHookMethod(premiumView, "onMeasure", int.class, int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            param.setResult(null);
                        }
                    });
        } catch (ClassNotFoundException e) {
            XposedBridge.log("[xposedYT] Premium view class not found (version mismatch)");
        }
    }

    private static boolean isAdView(View view) {
        if (view == null) return false;
        try {
            String contentDesc = String.valueOf(view.getContentDescription());
            for (String pattern : AD_PATH_PATTERNS) {
                if (contentDesc.contains(pattern)) return true;
            }
            if (view.getId() != View.NO_ID && view.getResources() != null) {
                String resName = view.getResources().getResourceEntryName(view.getId());
                if (resName != null && (resName.contains("ad") || resName.contains("attribution"))) {
                    return true;
                }
            }
            Object tag = view.getTag();
            if (tag != null) {
                String tagStr = tag.toString();
                for (String pattern : AD_PATH_PATTERNS) {
                    if (tagStr.contains(pattern)) return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static void hideAdView(XC_MethodHook.MethodHookParam param) {
        if (param.args == null) return;
        for (Object arg : param.args) {
            if (arg instanceof View) {
                View view = (View) arg;
                if (isAdView(view)) {
                    view.setVisibility(View.GONE);
                    if (view.getParent() instanceof ViewGroup) {
                        ((ViewGroup) view.getParent()).removeView(view);
                    }
                }
            }
        }
    }

    private static void hookBuildProperties(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod(Build.class, "getString", String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String key = (String) param.args[0];
                            if ("ro.product.model".equals(key) || "ro.product.device".equals(key)) {
                                param.setResult("Android Automotive");
                            }
                        }
                    });
            XposedBridge.log("[xposedYT] Build properties hook installed");
        } catch (Throwable t) {
            XposedBridge.log("[xposedYT] Build hook failed: " + t.getMessage());
        }
    }

    private static void hookInnerTubeConnection(ClassLoader cl) {
        try {
            Class<?>[] candidates = {
                    cl.loadClass("com.google.android.apps.youtube.innertube.net.InnerTubeConnection"),
                    cl.loadClass("com.google.android.libraries.youtube.innertube.net.InnerTubeConnection"),
            };

            for (Class<?> innerTubeClass : candidates) {
                for (Method method : innerTubeClass.getDeclaredMethods()) {
                    if (method.getName().contains("header") || method.getName().contains("Header")) {
                        Class<?>[] paramTypes = method.getParameterTypes();
                        if (paramTypes.length >= 2 &&
                                (paramTypes[0] == String.class || paramTypes[0] == int.class)) {
                            XposedBridge.log("[xposedYT] Hooking InnerTube method: " + method.getName());
                            XposedHelpers.findAndHookMethod(innerTubeClass, method.getName(),
                                    getParamArray(paramTypes),
                                    new XC_MethodHook() {
                                        @Override
                                        protected void beforeHookedMethod(MethodHookParam param) {
                                            for (int i = 0; i < param.args.length; i++) {
                                                if (param.args[i] instanceof String) {
                                                    String val = (String) param.args[i];
                                                    if (val.contains("Android")) {
                                                        param.args[i] = "Android Automotive";
                                                    }
                                                }
                                            }
                                        }
                                    });
                        }
                    }
                }
            }
            XposedBridge.log("[xposedYT] InnerTube connection hook installed");
        } catch (Throwable t) {
            XposedBridge.log("[xposedYT] InnerTube hook: " + t.getMessage());
        }
    }

    private static void hookAdPlayerOverlay(ClassLoader cl) {
        try {
            String[] adClassCandidates = {
                    "com.google.android.apps.youtube.app.player.overlay.AdOverlayController",
                    "com.google.android.apps.youtube.app.player.overlay.PlayerOverlayAdLayout",
                    "com.google.android.apps.youtube.app.player.overlay.timely.TimelyShelfController",
            };

            for (String className : adClassCandidates) {
                try {
                    Class<?> clazz = cl.loadClass(className);
                    XposedBridge.log("[xposedYT] Found ad overlay class: " + className);

                    for (Method m : clazz.getDeclaredMethods()) {
                        if (m.getParameterCount() == 1 &&
                                View.class.isAssignableFrom(m.getParameterTypes()[0])) {
                            XposedHelpers.findAndHookMethod(clazz, m.getName(), View.class,
                                    new XC_MethodHook() {
                                        @Override
                                        protected void beforeHookedMethod(MethodHookParam param) {
                                            View view = (View) param.args[0];
                                            view.setVisibility(View.GONE);
                                        }
                                    });
                        }
                    }
                } catch (ClassNotFoundException ignored) {}
            }
        } catch (Throwable t) {
            XposedBridge.log("[xposedYT] Player overlay hook: " + t.getMessage());
        }
    }

    private static Class<?>[] findClassesWithPrefix(ClassLoader cl, String prefix) {
        return new Class<?>[0];
    }

    private static Class<?>[] getParamArray(Class<?>... params) {
        return params;
    }
}
