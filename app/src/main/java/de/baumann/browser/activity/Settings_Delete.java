package de.baumann.browser.activity;

import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.liuzho.lib.baseutils.BaseUtils;
import com.liuzho.lib.baseutils.ScreenUtils;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebStorage;
import android.widget.Button;

import java.util.Objects;

import de.baumann.browser.fragment.Fragment_settings_Delete;
import de.baumann.browser.R;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;

public class Settings_Delete extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) getSupportActionBar().hide();
        ScreenUtils.translucent(this);
        if (!BaseUtils.isNight(this)) {
            getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        HelperUnit.initTheme(this);
        setContentView(R.layout.activity_settings_delete);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, new Fragment_settings_Delete())
                .commit();

        Button button = findViewById(R.id.whitelist_add);
        button.setOnClickListener(v -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setMessage(R.string.hint_database);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {

                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
                boolean clearCache = sp.getBoolean("sp_clear_cache", false);
                boolean clearCookie = sp.getBoolean("sp_clear_cookie", false);
                boolean clearHistory = sp.getBoolean("sp_clear_history", false);
                boolean clearIndexedDB = sp.getBoolean("sp_clearIndexedDB", false);

                if (clearCache) {
                    BrowserUnit.clearCache(this);
                }
                if (clearCookie) {
                    BrowserUnit.clearCookie();
                }
                if (clearHistory) {
                    BrowserUnit.clearHistory(this);
                }
                if (clearIndexedDB) {
                    BrowserUnit.clearIndexedDB(this);
                    WebStorage.getInstance().deleteAllData();
                }
            });
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }
}