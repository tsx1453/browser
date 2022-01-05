package com.liuzho.browser.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.liuzho.browser.R;
import com.liuzho.browser.unit.BrowserUnit;

public class CleanSettingsFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.libbrs_settings_delete_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, new CleanSettingsPrefFragment())
                .commit();
        view.findViewById(R.id.clean_now).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(v.getContext())
                        .setMessage(R.string.libbrs_hint_database)
                        .setPositiveButton(android.R.string.ok, (d, w) -> BrowserUnit.clearData(v.getContext()))
                        .setNegativeButton(android.R.string.cancel, (dialog, w) -> dialog.cancel())
                        .show());
    }
}