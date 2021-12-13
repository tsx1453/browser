package de.baumann.browser;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    public static class Config {
        public final Context context;
        private String defaultHomePage;

        private Config(Context context) {
            this.context = context;
        }

        public String getDefaultHomePage() {
            return defaultHomePage;
        }

        private void setDefaultHomePage(String defaultHomePage) {
            this.defaultHomePage = defaultHomePage;
        }

        public static class Builder {
            public Context context;
            public String defaultHomePage;

            public Builder(@NonNull Context context) {
                this.context = context;
            }

            public Builder defaultHomePage(String homePage) {
                this.defaultHomePage = homePage;
                return this;
            }

            public Config build() {
                Config config = new Config(context);
                config.setDefaultHomePage(this.defaultHomePage);
                return config;
            }
        }
    }
}
