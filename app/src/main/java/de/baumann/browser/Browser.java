package de.baumann.browser;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liuzho.lib.baseutils.theme.ThemeHandler;

public class Browser {
    private static Config mConfig;

    public static void init(@NonNull Config config) {
        mConfig = config;
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

        private Config(Context context) {
            this.context = context;
        }

        public String getDefaultHomePage() {
            return defaultHomePage;
        }

        private void setDefaultHomePage(String defaultHomePage) {
            this.defaultHomePage = defaultHomePage;
        }

        public ThemeHandler getThemeHandler() {
            return themeHandler;
        }

        private void setThemeHandler(ThemeHandler themeHandler) {
            this.themeHandler = themeHandler;
        }

        public static class Builder {
            public Context context;
            public String defaultHomePage;
            public ThemeHandler themeHandler;

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

            public Config build() {
                Config config = new Config(context);
                config.setDefaultHomePage(this.defaultHomePage);
                config.setThemeHandler(themeHandler);
                return config;
            }
        }
    }
}
