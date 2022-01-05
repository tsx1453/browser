package com.liuzho.browser.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.StringDef;

import com.liuzho.browser.Browser;
import com.liuzho.browser.BrowserConstant;
import com.liuzho.browser.BuildConfig;
import com.liuzho.browser.R;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class BrowserPref {

    public static final String SP_NAME = BuildConfig.LIBRARY_PACKAGE_NAME + ".browser_preferences";

    public static final String KEY_KEEP_SCREEN_ON = Browser.getConfig().context.getString(R.string.libbrs_spkey_keep_screen_on);
    public static final String KEY_START_TAB = Browser.getConfig().context.getString(R.string.libbrs_spkey_start_tab);
    public static final String KEY_PROFILE_TO_START = Browser.getConfig().context.getString(R.string.libbrs_spkey_profile_to_start);
    public static final String KEY_AUTO_FILL = Browser.getConfig().context.getString(R.string.libbrs_spkey_auto_fill);
    public static final String KEY_RESTORE_TABS = Browser.getConfig().context.getString(R.string.libbrs_spkey_restore_tabs);
    public static final String KEY_RELOAD_TABS = Browser.getConfig().context.getString(R.string.libbrs_spkey_reload_tabs);
    public static final String KEY_PROFILE = "profile";
    public static final String KEY_OPEN_TABS = "open_tabs";
    public static final String KEY_OPEN_TABS_PROFILE = "open_tabs_profile";
    public static final String KEY_PDF_CREATE = "pdf_create";
    public static final String KEY_RESTART_CHANGED = "restart_changed";
    public static final String KEY_CLEAR_WHEN_QUIT = Browser.getConfig().context.getString(R.string.libbrs_spkey_clear_when_quit);
    public static final String KEY_CLEAR_CACHE = Browser.getConfig().context.getString(R.string.libbrs_spkey_clear_cache);
    public static final String KEY_CLEAR_COOKIE = Browser.getConfig().context.getString(R.string.libbrs_spkey_clear_cookie);
    public static final String KEY_CLEAR_HISTORY = Browser.getConfig().context.getString(R.string.libbrs_spkey_clear_history);
    public static final String KEY_CLEAR_INDEXED_DB = Browser.getConfig().context.getString(R.string.libbrs_spkey_clear_indexed_db);
    public static final String KEY_SWIPE_TO_RELOAD = Browser.getConfig().context.getString(R.string.libbrs_spkey_swipe_to_reload);
    public static final String KEY_CONFIRM_CLOSE_TAB = Browser.getConfig().context.getString(R.string.libbrs_spkey_confirm_close_tab);
    public static final String KEY_FONT_ZOOM = Browser.getConfig().context.getString(R.string.libbrs_spkey_font_zoom);
    public static final String KEY_USER_AGENT_SWITCH = Browser.getConfig().context.getString(R.string.libbrs_spkey_user_agent_switch);
    public static final String KEY_CUSTOM_USER_AGENT = Browser.getConfig().context.getString(R.string.libbrs_spkey_custom_user_agent);
    public static final String KEY_JS_PAGE_FINISHED = Browser.getConfig().context.getString(R.string.libbrs_spkey_js_page_finished);
    public static final String KEY_JS_PAGE_FINISHED_SWITCH = Browser.getConfig().context.getString(R.string.libbrs_spkey_js_page_finished_switch);
    public static final String KEY_JS_PAGE_STARTED = Browser.getConfig().context.getString(R.string.libbrs_spkey_js_page_started);
    public static final String KEY_JS_PAGE_STARTED_SWITCH = Browser.getConfig().context.getString(R.string.libbrs_spkey_js_page_started_switch);
    public static final String KEY_JS_PAGE_LOAD_RESOURCE_SWITCH = Browser.getConfig().context.getString(R.string.libbrs_spkey_js_page_load_resource_switch);
    public static final String KEY_JS_PAGE_LOAD_RESOURCE = Browser.getConfig().context.getString(R.string.libbrs_spkey_js_page_load_resource);
    public static final String KEY_AD_HOSTS = Browser.getConfig().context.getString(R.string.libbrs_spkey_ad_hosts);
    public static final String KEY_CUSTOM_SEARCH_ENGINE = Browser.getConfig().context.getString(R.string.libbrs_spkey_custom_search_engine);
    public static final String KEY_CUSTOM_SEARCH_ENGINE_SWITCH = Browser.getConfig().context.getString(R.string.libbrs_spkey_custom_search_engine_switch);
    public static final String KEY_SEARCH_ENGINE = Browser.getConfig().context.getString(R.string.libbrs_spkey_search_engine);
    public static final String KEY_FAVORITE_URL = Browser.getConfig().context.getString(R.string.libbrs_spkey_favorite_url);

    /**
     * 务必确保libbrs_pref_profile_xxx.xml、R.array.profileToStart_values内的值与此一致
     */
    @StringDef({Profile.VAL_PROFILE_STANDARD, Profile.VAL_PROFILE_TRUSTED,
            Profile.VAL_PROFILE_PROTECTED, Profile.VAL_PROFILE_CUSTOM})
    public @interface Profile {
        String VAL_PROFILE_STANDARD = "profile_standard";
        String VAL_PROFILE_TRUSTED = "profile_trusted";
        String VAL_PROFILE_PROTECTED = "profile_protected";
        String VAL_PROFILE_CUSTOM = "profile_custom";
    }

    private static final BrowserPref ins = new BrowserPref(Browser.getConfig().context);

    public static synchronized BrowserPref getInstance() {
        return ins;
    }

    final SharedPreferences preferences;

    private BrowserPref(Context context) {
        preferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

    private static final String KEY_IS_FIRST_LAUNCH = "isFirstLaunch";

    public String getFavoriteUrl() {
        return preferences.getString(KEY_FAVORITE_URL, BrowserConstant.DEFAULT_FAVORITE_URL);
    }

    public void setFavoriteUrl(String favoriteUrl) {
        preferences.edit().putString(KEY_FAVORITE_URL, favoriteUrl).apply();
    }

    public boolean isFirstLaunch() {
        boolean result = preferences.getBoolean(KEY_IS_FIRST_LAUNCH, true);
        preferences.edit().putBoolean(KEY_IS_FIRST_LAUNCH, false).apply();
        return result;
    }

    public int getAndIncreaseCounter() {
        int counter = preferences.getInt("counter", 0);
        counter = counter + 1;
        preferences.edit().putInt("counter", counter).apply();
        return counter;
    }

    public boolean keepScreenOn() {
        return preferences.getBoolean(KEY_KEEP_SCREEN_ON, false);
    }

    public String startTab() {
        return preferences.getString(KEY_START_TAB, "3");
    }

    @Profile
    public String profileToStart() {
        return preferences.getString(KEY_PROFILE_TO_START, Profile.VAL_PROFILE_STANDARD);
    }

    public void setProfileToStart(@Profile String profile){
         preferences.edit().putString(KEY_PROFILE_TO_START, profile).apply();
    }


    public boolean autoFill() {
        return preferences.getBoolean(KEY_AUTO_FILL, true);
    }

    public boolean restoreTabs() {
        return preferences.getBoolean(KEY_RESTORE_TABS, false);
    }

    public boolean reloadTabs() {
        return preferences.getBoolean(KEY_RELOAD_TABS, false);
    }

    public boolean restoreOnRestart() {
        return preferences.getBoolean("restore_on_restart", false);
    }

    public void setRestoreOnRestart(boolean restoreOnRestart) {
        preferences.edit().putBoolean("restore_on_restart", restoreOnRestart).apply();
    }

    @Profile
    public String profile() {
        return preferences.getString(KEY_PROFILE, Profile.VAL_PROFILE_STANDARD);
    }

    public void setProfile(@Profile String profile) {
        preferences.edit().putString(KEY_PROFILE, profile).apply();
    }

    public List<String> openTabs() {
        String tabs = preferences.getString(KEY_OPEN_TABS, "");
        return Arrays.asList(TextUtils.split(tabs, "‚‗‚"));
    }

    public void setOpenTabs(List<String> tabs) {
        String openTabs = TextUtils.join("‚‗‚", tabs);
        preferences.edit().putString(KEY_OPEN_TABS, openTabs).apply();
    }

    public List<String> openTabsProfile() {
        String tabs = preferences.getString(KEY_OPEN_TABS_PROFILE, "");
        return Arrays.asList(TextUtils.split(tabs, "‚‗‚"));
    }


    public void setOpenTabsProfile(List<String> profiles) {
        String tabProfiles = TextUtils.join("‚‗‚", profiles);
        preferences.edit().putString(KEY_OPEN_TABS_PROFILE, tabProfiles).apply();
    }

    public boolean pdfCreate() {
        return preferences.getBoolean(KEY_PDF_CREATE, false);
    }

    public void setPdfCreate(boolean val) {
        preferences.edit().putBoolean(KEY_PDF_CREATE, val).apply();
    }

    public void onBrowserCreate() {
        preferences.edit()
                .putBoolean(KEY_RESTART_CHANGED, false)
                .putBoolean(KEY_PDF_CREATE, false)
                .putString(KEY_PROFILE, profileToStart())
                .apply();
    }

    public boolean clearWhenQuit() {
        return preferences.getBoolean(KEY_CLEAR_WHEN_QUIT, Browser.getConfig().context.getResources().getBoolean(R.bool.libbrs_spval_clear_when_quit));
    }

    public boolean clearCache() {
        return preferences.getBoolean(KEY_CLEAR_CACHE, Browser.getConfig().context.getResources().getBoolean(R.bool.libbrs_spval_clear_cache));
    }

    public boolean clearCookie() {
        return preferences.getBoolean(KEY_CLEAR_COOKIE, Browser.getConfig().context.getResources().getBoolean(R.bool.libbrs_spval_clear_cookie));
    }

    public boolean clearHistory() {
        return preferences.getBoolean(KEY_CLEAR_HISTORY, Browser.getConfig().context.getResources().getBoolean(R.bool.libbrs_spval_clear_history));
    }

    public boolean clearIndexedDb() {
        return preferences.getBoolean(KEY_CLEAR_INDEXED_DB, Browser.getConfig().context.getResources().getBoolean(R.bool.libbrs_spval_clear_indexed_db));
    }

    public boolean restartChanged() {
        return preferences.getBoolean(KEY_RESTART_CHANGED, true);
    }

    public void setRestartChanged(boolean val) {
        preferences.edit().putBoolean(KEY_RESTART_CHANGED, val).apply();
    }

    public boolean swipeToReload() {
        return preferences.getBoolean(KEY_SWIPE_TO_RELOAD, true);
    }

    public boolean confirmCloseTab() {
        return preferences.getBoolean(KEY_CONFIRM_CLOSE_TAB, Browser.getConfig().context.getResources().getBoolean(R.bool.libbrs_spval_confirm_close_tab));
    }

    public int fontZoom() {
        return Integer.parseInt(Objects.requireNonNull(preferences.getString(KEY_FONT_ZOOM, "100")));
    }

    public boolean userAgentSwitch() {
        return preferences.getBoolean(KEY_USER_AGENT_SWITCH, false);
    }

    public void setUserAgentSwitch(boolean val) {
        preferences.edit().putBoolean(KEY_USER_AGENT_SWITCH, val).apply();
    }

    public boolean contains(String key) {
        return preferences.contains(key);
    }

    public String customUserAgent() {
        return preferences.getString(KEY_CUSTOM_USER_AGENT, "");
    }

    public boolean jsPageFinishedSwitch() {
        return preferences.getBoolean(KEY_JS_PAGE_FINISHED_SWITCH, false);
    }

    public String jsPageFinished() {
        return preferences.getString(KEY_JS_PAGE_FINISHED, "");
    }

    public String jsPageStarted() {
        return preferences.getString(KEY_JS_PAGE_STARTED, "");
    }

    public boolean jsPageStartedSwitch() {
        return preferences.getBoolean(KEY_JS_PAGE_STARTED_SWITCH, false);
    }

    public boolean jsPageLoadResourceSwitch() {
        return preferences.getBoolean(KEY_JS_PAGE_LOAD_RESOURCE_SWITCH, false);
    }

    public String jsPageLoadResource() {
        return preferences.getString(KEY_JS_PAGE_LOAD_RESOURCE, "");
    }

    public boolean onceCheck(String key) {
        boolean val = preferences.getBoolean("once_check_" + key, true);
        preferences.edit().putBoolean("once_check_" + key, false).apply();
        return val;
    }

    public String getAdHosts() {
        return preferences.getString(KEY_AD_HOSTS,
                // todo replace it
                "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts");
    }

    public String getSortStartSite() {
        return preferences.getString("sort_start_site", "ordinal");
    }

    public String customSearchEngine() {
        return preferences.getString(KEY_CUSTOM_SEARCH_ENGINE, "");
    }

    public boolean customSearchEngineSwitch() {
        return preferences.getBoolean(KEY_CUSTOM_SEARCH_ENGINE_SWITCH, false);
    }

    public void setCustomSearchEngineSwitch(boolean val) {
        preferences.edit().putBoolean(KEY_CUSTOM_SEARCH_ENGINE_SWITCH, val).apply();
    }

    public int searchEngine() {
        return Integer.parseInt(Objects.requireNonNull(preferences.getString(KEY_SEARCH_ENGINE, "0")));
    }
}
