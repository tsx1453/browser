package de.baumann.browser.storage;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.util.Objects;

import de.baumann.browser.Constant;

public class SP {
    private static SP ins;

    public static void init(Context context) {
        if (ins == null) {
            ins = new SP(context);
        }
    }

    public static SP getInstance() {
        return Objects.requireNonNull(ins);
    }

    private final SharedPreferences sharedPreferences;

    private SP(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    private static final String KEY_FAVORITE_URL = "favoriteURL";

    public String getFavoriteUrl() {
        return sharedPreferences.getString(KEY_FAVORITE_URL, Constant.DEFAULT_FAVORITE_URL);
    }

    public void setFavoriteUrl(String favoriteUrl) {
        sharedPreferences.edit().putString(KEY_FAVORITE_URL, favoriteUrl).apply();
    }
}
