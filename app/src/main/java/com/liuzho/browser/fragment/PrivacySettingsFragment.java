package com.liuzho.browser.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.GridView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.liuzho.browser.R;
import com.liuzho.browser.browser.AdBlock;
import com.liuzho.browser.storage.BrowserPref;
import com.liuzho.browser.view.GridAdapter;
import com.liuzho.browser.view.GridItem;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PrivacySettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(BrowserPref.SP_NAME);
        setPreferencesFromResource(R.xml.libbrs_pref_privacy, rootKey);
        Context context = requireContext();

        Preference prefAdBlock = findPreference("sp_ad_block");
        Objects.requireNonNull(prefAdBlock);
        new Thread(() -> {
            String hostsDate = AdBlock.getHostsDate(requireContext());
            if (!TextUtils.isEmpty(hostsDate) && isAdded() && !isDetached()) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    FragmentActivity activity = getActivity();
                    if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                        prefAdBlock.setSummary(getString(R.string.libbrs_setting_summary_adblock) + "\n\n" + hostsDate);
                    }
                });
            }
        }).start();

        Preference prefProfile = findPreference("settings_profile");
        assert prefProfile != null;
        prefProfile.setOnPreferenceClickListener(preference -> {

            GridItem itemTrusted = new GridItem(R.drawable.libbrs_icon_profile_trusted, getString(R.string.libbrs_setting_title_profiles_trusted), 11);
            GridItem itemStandard = new GridItem(R.drawable.libbrs_icon_profile_standard, getString(R.string.libbrs_setting_title_profiles_standard), 11);
            GridItem itemProtected = new GridItem(R.drawable.libbrs_icon_profile_protected, getString(R.string.libbrs_setting_title_profiles_protected), 11);

            View dialogView = View.inflate(context, R.layout.libbrs_dialog_menu, null);
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setView(dialogView)
                    .show();

            CardView cardView = dialogView.findViewById(R.id.cardView);
            cardView.setVisibility(View.GONE);

            TextView menuTitle = dialogView.findViewById(R.id.menuTitle);
            menuTitle.setText(getString(R.string.libbrs_setting_title_profiles_edit));

            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
            GridView gvMenu = dialogView.findViewById(R.id.menu_grid);
            List<GridItem> gridList = Arrays.asList(itemTrusted, itemStandard, itemProtected);
            gvMenu.setAdapter(new GridAdapter(context, gridList));
            gvMenu.setOnItemClickListener((parent, view, position, id) -> {
                String profile;
                switch (position) {
                    case 0:
                        profile = BrowserPref.Profile.VAL_PROFILE_TRUSTED;
                        break;
                    case 2:
                        profile = BrowserPref.Profile.VAL_PROFILE_PROTECTED;
                        break;
                    case 1:
                    default:
                        profile = BrowserPref.Profile.VAL_PROFILE_STANDARD;
                        break;
                }
                dialog.cancel();
                ProfileSettingsFragment fragment = new ProfileSettingsFragment();
                Bundle args = new Bundle();
                args.putString(ProfileSettingsFragment.EXTRA_PROFILE, profile);
                fragment.setArguments(args);
                requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace((((View) requireView().getParent()).getId()), fragment)
                        .addToBackStack(null)
                        .commit();
            });
            return false;
        });

    }


    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sp, String key) {
        if (key.equals(BrowserPref.KEY_AD_HOSTS)) {
            AdBlock.downloadHosts(getActivity());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.libbrs_setting_title_profiles_privacy);
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
}
