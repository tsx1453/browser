package com.liuzho.browser.storage;

import androidx.annotation.StringDef;

public class ProfileConfig {

    private final String profile;

    public ProfileConfig(@BrowserPref.Profile String profile) {
        this.profile = profile;
    }

    @StringDef({FPP, SAVE_HISTORY, AD_BLOCK,
            SAVE_DATA, CAMERA, IMAGES,
            LOCATION, JS, JS_POPUP,
            DOM, COOKIES, MICROPHONE})
    public @interface ConfigKey {
    }

    /**
     * 务必确保libbrs_pref_profile_xxx.xml内的值与此一致
     */
    public static final String FPP = "fingerPrintProtection";
    public static final String SAVE_HISTORY = "saveHistory";
    public static final String AD_BLOCK = "adBlock";
    public static final String SAVE_DATA = "saveData";
    public static final String CAMERA = "camera";
    public static final String MICROPHONE = "microphone";
    public static final String IMAGES = "images";
    public static final String LOCATION = "location";
    public static final String JS = "javascript";
    public static final String JS_POPUP = "javascriptPopup";
    public static final String DOM = "dom";
    public static final String COOKIES = "cookies";

    public boolean config(@ConfigKey String key) {
        return BrowserPref.getInstance()
                .preferences
                .getBoolean(profile + "_" + key, true/* ignore 'defVal' , when first open , will autofill default config */);
    }

    public void toggle(@ConfigKey String key) {
        BrowserPref.getInstance()
                .preferences
                .edit()
                .putBoolean(profile + "_" + key, !config(key))
                .apply();
    }


    public void setup(@BrowserPref.Profile String profile,
                      boolean saveData, boolean images, boolean adBlock,
                      boolean location, boolean fpp, boolean cookies,
                      boolean js, boolean jsPopup, boolean history,
                      boolean camera, boolean microphone, boolean dom) {
        BrowserPref.getInstance()
                .preferences
                .edit()
                .putBoolean(profile + "_" + SAVE_DATA, saveData)
                .putBoolean(profile + "_" + IMAGES, images)
                .putBoolean(profile + "_" + AD_BLOCK, adBlock)
                .putBoolean(profile + "_" + LOCATION, location)
                .putBoolean(profile + "_" + FPP, fpp)
                .putBoolean(profile + "_" + COOKIES, cookies)
                .putBoolean(profile + "_" + JS, js)
                .putBoolean(profile + "_" + JS_POPUP, jsPopup)
                .putBoolean(profile + "_" + SAVE_HISTORY, history)
                .putBoolean(profile + "_" + CAMERA, camera)
                .putBoolean(profile + "_" + MICROPHONE, microphone)
                .putBoolean(profile + "_" + DOM, dom)
                .putString(BrowserPref.KEY_PROFILE, profile) // update profile
                .apply();
    }

}
