package com.liuzho.browser.fragment;

import android.content.Context;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.liuzho.browser.BrowserConstant;
import com.liuzho.browser.R;
import com.liuzho.browser.storage.BrowserPref;

import java.util.Objects;

public class CleanSettingsPrefFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(BrowserPref.SP_NAME);
        setPreferencesFromResource(R.xml.libbrs_pref_delete, rootKey);
        Context activity = requireContext();

        Preference prefDeleteDatabase = findPreference("sp_deleteDatabase");
        Objects.requireNonNull(prefDeleteDatabase).setOnPreferenceClickListener(preference -> {
            new MaterialAlertDialogBuilder(activity)
                    .setIcon(R.drawable.libbrs_icon_alert)
                    .setTitle(R.string.libbrs_menu_delete)
                    .setMessage(R.string.libbrs_hint_database)
                    .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                        dialog.cancel();
                        activity.deleteDatabase(BrowserConstant.BROWSER_DBNAME);
                        activity.deleteDatabase("faviconView.db");
                        BrowserPref.getInstance().setRestartChanged(true);
                    })
                    .setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> dialog.cancel())
                    .show();
            return true;
        });
    }
}