package com.liuzho.browser.fragment;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.liuzho.browser.R;
import com.liuzho.browser.storage.BrowserPref;

public class UiSettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(BrowserPref.SP_NAME);
        setPreferencesFromResource(R.xml.libbrs_pref_ui, rootKey);
    }

}
