package de.baumann.browser.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.liuzho.lib.baseutils.ScreenUtils;

import java.util.Objects;

import de.baumann.browser.fragment.Fragment_settings_UI;
import de.baumann.browser.R;
import de.baumann.browser.unit.HelperUnit;

public class Settings_UI extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) getSupportActionBar().hide();
        ScreenUtils.translucent(this);
        getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        HelperUnit.initTheme(this);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, new Fragment_settings_UI())
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }
}
