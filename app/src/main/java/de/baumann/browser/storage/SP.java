package de.baumann.browser.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.util.Objects;

import de.baumann.browser.Browser;
import de.baumann.browser.Constant;

public class SP {
    private static SP ins;

    public static SP getInstance() {
        if (ins == null) {
            ins = new SP(Browser.getConfig().context);
        }
        return ins;
    }

    public final SharedPreferences sharedPreferences;

    private SP(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    private static final String KEY_FAVORITE_URL = "favoriteURL";

    public String getFavoriteUrl() {
        String defaultFavoriteUrlFromConfig = Browser.getConfig().getDefaultHomePage();
        String defaultFavoriteUrl = TextUtils.isEmpty(defaultFavoriteUrlFromConfig) ? Constant.DEFAULT_FAVORITE_URL : defaultFavoriteUrlFromConfig;
        return sharedPreferences.getString(KEY_FAVORITE_URL, defaultFavoriteUrl);
    }

    public void setFavoriteUrl(String favoriteUrl) {
        sharedPreferences.edit().putString(KEY_FAVORITE_URL, favoriteUrl).apply();
    }
}
