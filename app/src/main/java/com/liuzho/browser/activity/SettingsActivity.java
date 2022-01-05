package com.liuzho.browser.activity;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.liuzho.browser.Browser;
import com.liuzho.browser.R;
import com.liuzho.browser.fragment.MainSettingsFragment;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    private final Browser.Callback globalCallback = Browser.getConfig().callback();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        int theme = globalCallback.theme(true);
        if (theme != -1) {
            setTheme(theme);
        }
        super.onCreate(savedInstanceState);
        globalCallback.onActivityCreate(this);

        setContentView(R.layout.libbrs_activity_settings);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, new MainSettingsFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}