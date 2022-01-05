package com.liuzho.browser.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.liuzho.browser.R;
import com.liuzho.browser.storage.BrowserPref;

public class MainSettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(BrowserPref.SP_NAME);
        setPreferencesFromResource(R.xml.libbrs_pref_setting, rootKey);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sp, String key) {
        if (key.equals(BrowserPref.KEY_CUSTOM_USER_AGENT) ||
                key.equals(BrowserPref.KEY_USER_AGENT_SWITCH) ||
                key.equals(BrowserPref.KEY_CUSTOM_SEARCH_ENGINE) ||
                key.equals(BrowserPref.KEY_CUSTOM_SEARCH_ENGINE_SWITCH) ||
                key.equals(BrowserPref.KEY_SEARCH_ENGINE)) {
            BrowserPref.getInstance().setRestartChanged(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        requireActivity().setTitle(R.string.libbrs_setting_label);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
}