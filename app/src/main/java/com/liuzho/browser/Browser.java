package com.liuzho.browser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.liuzho.browser.database.Record;
import com.liuzho.browser.database.RecordAction;
import com.liuzho.browser.storage.BrowserPref;
import com.liuzho.browser.storage.ProfileConfig;
import com.liuzho.lib.baseutils.theme.ThemeHandler;

import java.util.ArrayList;
import java.util.List;

public class Browser {
    @SuppressLint("StaticFieldLeak")
    private static Config mConfig;

    public static void init(@NonNull Config config) {
        mConfig = config;
        insertDefaultHomeEntrance();
        setProfileDefaultValues();
        setDefaultFavoriteUrl();
        setDefaultProfileToStart();
    }

    private static void insertDefaultHomeEntrance() {
        BrowserPref pref = BrowserPref.getInstance();
        if (pref.onceCheck("default_home_entrance")) {
            List<Record> recordList = new ArrayList<>();
            recordList.add(new Record("YouTube", "https://m.youtube.com/", 0, 0, pref.getAndIncreaseCounter(), false, false, 0));
            recordList.add(new Record("Twitter", "https://twitter.com/", 0, 0, pref.getAndIncreaseCounter(), false, false, 0));
            recordList.add(new Record("Facebook", "https://www.facebook.com/", 0, 0, pref.getAndIncreaseCounter(), false, false, 0));
            recordList.add(new Record("Instagram", "https://www.instagram.com/", 0, 0, pref.getAndIncreaseCounter(), false, false, 0));

            RecordAction recordAction = new RecordAction(Browser.getConfig().context);
            recordAction.open(true);
            for (Record record : recordList) {
                recordAction.addStartSite(record);
            }
            recordAction.close();
        }
    }

    private static void setDefaultProfileToStart() {
        BrowserPref pref = BrowserPref.getInstance();
        if (pref.onceCheck("defaultProfileToStart")) {
            pref.setProfileToStart(BrowserPref.Profile.VAL_PROFILE_STANDARD);
        }
    }

    private static void setDefaultFavoriteUrl() {
        BrowserPref browserPref = BrowserPref.getInstance();
        if (browserPref.onceCheck("defaultFavoriteUrl")) {
            String url = mConfig.getDefaultHomePage();
            if (TextUtils.isEmpty(url)) {
                url = BrowserConstant.DEFAULT_FAVORITE_URL;
            }
            browserPref.setFavoriteUrl(url);
        }
    }

    private static void setProfileDefaultValues() {
        if (BrowserPref.getInstance().onceCheck("defaultProfileConfig")) {
            ProfileConfig trustedConfig = new ProfileConfig(BrowserPref.Profile.VAL_PROFILE_TRUSTED);
            trustedConfig.setup(BrowserPref.Profile.VAL_PROFILE_TRUSTED,
                    true, true, true, false,
                    false, true, true, true,
                    true, false, false, true);

            ProfileConfig standardConfig = new ProfileConfig(BrowserPref.Profile.VAL_PROFILE_STANDARD);
            standardConfig.setup(BrowserPref.Profile.VAL_PROFILE_STANDARD,
                    true, true, true, false,
                    true, false, true, false,
                    true, false, false, true);

            ProfileConfig protectedConfig = new ProfileConfig(BrowserPref.Profile.VAL_PROFILE_PROTECTED);
            protectedConfig.setup(BrowserPref.Profile.VAL_PROFILE_PROTECTED,
                    true, true, true, false,
                    true, false, false, false,
                    true, false, false, false);
        }
    }

    @NonNull
    public static Config getConfig() {
        if (mConfig == null) {
            throw new IllegalStateException("call init first");
        }
        return mConfig;
    }

    public static ThemeHandler getThemeHandler() {
        return getConfig().getThemeHandler();
    }

    public static class Config {
        public final Context context;
        private String defaultHomePage;
        private ThemeHandler themeHandler;
        private Callback callback;

        private Config(Context context) {
            this.context = context;
        }

        public String getDefaultHomePage() {
            return defaultHomePage;
        }

        public ThemeHandler getThemeHandler() {
            return themeHandler;
        }

        public Callback callback() {
            return callback;
        }

        public static class Builder {
            Context context;
            String defaultHomePage;
            ThemeHandler themeHandler;
            Callback callback;

            public Builder(@NonNull Context context) {
                this.context = context;
            }

            public Builder defaultHomePage(String homePage) {
                this.defaultHomePage = homePage;
                return this;
            }

            public Builder themeHandler(ThemeHandler themeHandler) {
                this.themeHandler = themeHandler;
                return this;
            }

            public Builder callback(Callback callback) {
                this.callback = callback;
                return this;
            }

            public Config build() {
                Config config = new Config(context);
                config.defaultHomePage = defaultHomePage;
                config.themeHandler = themeHandler;
                if (callback == null) {
                    callback = new Callback() {
                    };
                }
                config.callback = callback;
                return config;
            }
        }
    }

    public interface Callback {
        default void onActivityCreate(AppCompatActivity activity) {
        }

        default int theme(boolean actionBar) {
            return -1;
        }
    }
}
