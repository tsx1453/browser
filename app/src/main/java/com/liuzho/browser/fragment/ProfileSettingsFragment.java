package com.liuzho.browser.fragment;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.liuzho.browser.R;
import com.liuzho.browser.storage.BrowserPref;

public class ProfileSettingsFragment extends PreferenceFragmentCompat {

    private int title;

    public static final String EXTRA_PROFILE = "profile";


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(BrowserPref.SP_NAME);

        Bundle args = requireArguments();

        @BrowserPref.Profile
        String profile = args.getString(EXTRA_PROFILE, BrowserPref.Profile.VAL_PROFILE_STANDARD);
        int xmlRes;
        switch (profile) {
            case BrowserPref.Profile.VAL_PROFILE_TRUSTED:
                xmlRes = R.xml.libbrs_pref_profile_trusted;
                title = R.string.libbrs_setting_title_profiles_trusted;
                break;
            case BrowserPref.Profile.VAL_PROFILE_PROTECTED:
                xmlRes = R.xml.libbrs_pref_profile_protected;
                title = R.string.libbrs_setting_title_profiles_protected;
                break;
            case BrowserPref.Profile.VAL_PROFILE_STANDARD:
            default:
                xmlRes = R.xml.libbrs_pref_profile_standard;
                title = R.string.libbrs_setting_title_profiles_standard;
                break;
        }

        setPreferencesFromResource(xmlRes, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(title);
    }
}