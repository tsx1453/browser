package com.liuzho.browser.dialog;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.liuzho.browser.R;
import com.liuzho.browser.browser.ProtectedList;
import com.liuzho.browser.browser.SiteList;
import com.liuzho.browser.browser.StandardList;
import com.liuzho.browser.browser.TrustedList;
import com.liuzho.browser.database.FaviconHelper;
import com.liuzho.browser.storage.BrowserPref;
import com.liuzho.browser.storage.ProfileConfig;
import com.liuzho.browser.unit.HelperUnit;
import com.liuzho.browser.view.NinjaWebView;

import java.util.Objects;

public class FastToggleDialog {

    public static void show(NinjaWebView ninjaWebView, ToggleCallback callback) {
        Context context = ninjaWebView.getContext();
        SiteList listTrusted = new TrustedList(context);
        SiteList listStandard = new StandardList(context);
        SiteList listProtected = new ProtectedList(context);
        String url = ninjaWebView.getUrl();
        assert url != null;

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = View.inflate(context, R.layout.libbrs_dialog_toggle, null);
        builder.setView(dialogView);
        FaviconHelper.setFavicon(context, dialogView, url, R.id.menu_icon, R.drawable.libbrs_icon_image_broken);

        Chip chipStandard = dialogView.findViewById(R.id.chip_profile_standard);
        Chip chipTrusted = dialogView.findViewById(R.id.chip_profile_trusted);
        Chip chipCustom = dialogView.findViewById(R.id.chip_profile_changed);
        Chip chipProtected = dialogView.findViewById(R.id.chip_profile_protected);

        TextView tvDomain = dialogView.findViewById(R.id.dialog_title);
        String domain = HelperUnit.domain(url);
        tvDomain.setText(domain);

        TextView tvWarning = dialogView.findViewById(R.id.dialog_warning);
        String warning = context.getString(R.string.libbrs_profile_warning) + " " + domain;
        tvWarning.setText(warning);

        TextView tvTitle = dialogView.findViewById(R.id.dialog_titleProfile);
        // noinspection WrongConstant
        ninjaWebView.putProfileBoolean("",
                tvTitle, chipTrusted, chipStandard, chipProtected, chipCustom);

        AlertDialog dialog = builder.create();
        dialog.show();
        Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.TOP);

        //ProfileControl

        Chip chipSetTrusted = dialogView.findViewById(R.id.chip_setProfileTrusted);
        chipSetTrusted.setChecked(listTrusted.isWhite(url));
        chipSetTrusted.setOnClickListener(v -> {
            if (listTrusted.isWhite(url)) {
                listTrusted.removeDomain(domain);
            } else {
                listTrusted.addDomain(domain);
                listStandard.removeDomain(domain);
                listProtected.removeDomain(domain);
            }
            ninjaWebView.reload();
            dialog.cancel();
        });

        Chip chipSetProtected = dialogView.findViewById(R.id.chip_setProfileProtected);
        chipSetProtected.setChecked(listProtected.isWhite(url));
        chipSetProtected.setOnClickListener(v -> {
            if (listProtected.isWhite(url)) {
                listProtected.removeDomain(domain);
            } else {
                listProtected.addDomain(domain);
                listTrusted.removeDomain(domain);
                listStandard.removeDomain(domain);
            }
            ninjaWebView.reload();
            dialog.cancel();
        });

        Chip chipSetStandard = dialogView.findViewById(R.id.chip_setProfileStandard);
        chipSetStandard.setChecked(listStandard.isWhite(url));
        chipSetStandard.setOnClickListener(v -> {
            if (listStandard.isWhite(url)) {
                listStandard.removeDomain(domain);
            } else {
                listStandard.addDomain(domain);
                listTrusted.removeDomain(domain);
                listProtected.removeDomain(domain);
            }
            ninjaWebView.reload();
            dialog.cancel();
        });

        BrowserPref browserPref = BrowserPref.getInstance();
        String profile = browserPref.profile();
        chipTrusted.setChecked(BrowserPref.Profile.VAL_PROFILE_TRUSTED.equals(profile));
        chipTrusted.setOnClickListener(v -> {
            browserPref.setProfile(BrowserPref.Profile.VAL_PROFILE_TRUSTED);
            ninjaWebView.reload();
            dialog.cancel();
        });

        chipStandard.setChecked(BrowserPref.Profile.VAL_PROFILE_STANDARD.equals(profile));
        chipStandard.setOnClickListener(v -> {
            browserPref.setProfile(BrowserPref.Profile.VAL_PROFILE_STANDARD);
            ninjaWebView.reload();
            dialog.cancel();
        });

        chipProtected.setChecked(BrowserPref.Profile.VAL_PROFILE_PROTECTED.equals(profile));
        chipProtected.setOnClickListener(v -> {
            browserPref.setProfile(BrowserPref.Profile.VAL_PROFILE_PROTECTED);
            ninjaWebView.reload();
            dialog.cancel();
        });

        chipCustom.setChecked(BrowserPref.Profile.VAL_PROFILE_CUSTOM.equals(profile));
        chipCustom.setOnClickListener(v -> {
            browserPref.setProfile(BrowserPref.Profile.VAL_PROFILE_CUSTOM);
            ninjaWebView.reload();
            dialog.cancel();
        });

        // CheckBox
        ProfileConfig config = new ProfileConfig(ninjaWebView.getProfile());
        Chip chipImage = dialogView.findViewById(R.id.chip_image);
        chipImage.setChecked(config.config(ProfileConfig.IMAGES));
        chipImage.setOnClickListener(v -> {
            ninjaWebView.setProfileChanged();
            ninjaWebView.putProfileBoolean(ProfileConfig.IMAGES,
                    tvTitle, chipTrusted, chipStandard, chipProtected, chipCustom);
        });

        Chip chipJavaScript = dialogView.findViewById(R.id.chip_javaScript);
        chipJavaScript.setChecked(config.config(ProfileConfig.JS));
        chipJavaScript.setOnClickListener(v -> {
            ninjaWebView.setProfileChanged();
            ninjaWebView.putProfileBoolean(ProfileConfig.JS,
                    tvTitle, chipTrusted, chipStandard, chipProtected, chipCustom);
        });

        Chip chipJavaScriptPopup = dialogView.findViewById(R.id.chip_javaScriptPopUp);
        chipJavaScriptPopup.setChecked(config.config(ProfileConfig.JS_POPUP));
        chipJavaScriptPopup.setOnClickListener(v -> {
            ninjaWebView.setProfileChanged();
            ninjaWebView.putProfileBoolean(ProfileConfig.JS_POPUP,
                    tvTitle, chipTrusted, chipStandard, chipProtected, chipCustom);
        });

        Chip chipCookie = dialogView.findViewById(R.id.chip_cookie);
        chipCookie.setChecked(config.config(ProfileConfig.COOKIES));
        chipCookie.setOnClickListener(v -> {
            ninjaWebView.setProfileChanged();
            ninjaWebView.putProfileBoolean(ProfileConfig.COOKIES,
                    tvTitle, chipTrusted, chipStandard, chipProtected, chipCustom);
        });

        Chip chipFingerprint = dialogView.findViewById(R.id.chip_Fingerprint);
        chipFingerprint.setChecked(config.config(ProfileConfig.FPP));
        chipFingerprint.setOnClickListener(v -> {
            ninjaWebView.setProfileChanged();
            ninjaWebView.putProfileBoolean(ProfileConfig.FPP,
                    tvTitle, chipTrusted, chipStandard, chipProtected, chipCustom);
        });

        Chip chipAdBlock = dialogView.findViewById(R.id.chip_adBlock);
        chipAdBlock.setChecked(config.config(ProfileConfig.AD_BLOCK));
        chipAdBlock.setOnClickListener(v -> {
            ninjaWebView.setProfileChanged();
            ninjaWebView.putProfileBoolean(ProfileConfig.AD_BLOCK,
                    tvTitle, chipTrusted, chipStandard, chipProtected, chipCustom);
        });

        Chip chipSaveData = dialogView.findViewById(R.id.chip_saveData);
        chipSaveData.setChecked(config.config(ProfileConfig.SAVE_DATA));
        chipSaveData.setOnClickListener(v -> {
            ninjaWebView.setProfileChanged();
            ninjaWebView.putProfileBoolean(ProfileConfig.SAVE_DATA,
                    tvTitle, chipTrusted, chipStandard, chipProtected, chipCustom);
        });

        Chip chipHistory = dialogView.findViewById(R.id.chip_history);
        chipHistory.setChecked(config.config(ProfileConfig.SAVE_HISTORY));
        chipHistory.setOnClickListener(v -> {
            ninjaWebView.setProfileChanged();
            ninjaWebView.putProfileBoolean(ProfileConfig.SAVE_HISTORY,
                    tvTitle, chipTrusted, chipStandard, chipProtected, chipCustom);
        });

        Chip chipLocation = dialogView.findViewById(R.id.chip_location);
        chipLocation.setChecked(config.config(ProfileConfig.LOCATION));
        chipLocation.setOnClickListener(v -> {
            ninjaWebView.setProfileChanged();
            ninjaWebView.putProfileBoolean(ProfileConfig.LOCATION,
                    tvTitle, chipTrusted, chipStandard, chipProtected, chipCustom);
        });

        Chip chipMicrophone = dialogView.findViewById(R.id.chip_microphone);
        chipMicrophone.setChecked(config.config(ProfileConfig.MICROPHONE));
        chipMicrophone.setOnClickListener(v -> {
            ninjaWebView.setProfileChanged();
            ninjaWebView.putProfileBoolean(ProfileConfig.MICROPHONE,
                    tvTitle, chipTrusted, chipStandard, chipProtected, chipCustom);
        });

        Chip chipCamera = dialogView.findViewById(R.id.chip_camera);
        chipCamera.setChecked(config.config(ProfileConfig.CAMERA));
        chipCamera.setOnClickListener(v -> {
            ninjaWebView.setProfileChanged();
            ninjaWebView.putProfileBoolean(ProfileConfig.CAMERA,
                    tvTitle, chipTrusted, chipStandard, chipProtected, chipCustom);
        });

        Chip chipDom = dialogView.findViewById(R.id.chip_dom);
        chipDom.setChecked(config.config(ProfileConfig.DOM));
        chipDom.setOnClickListener(v -> {
            ninjaWebView.setProfileChanged();
            ninjaWebView.putProfileBoolean(ProfileConfig.DOM,
                    tvTitle, chipTrusted, chipStandard, chipProtected, chipCustom);
        });

        if (listTrusted.isWhite(url) || listStandard.isWhite(url) || listProtected.isWhite(url)) {
            tvWarning.setVisibility(View.VISIBLE);
            chipImage.setEnabled(false);
            chipAdBlock.setEnabled(false);
            chipSaveData.setEnabled(false);
            chipLocation.setEnabled(false);
            chipCamera.setEnabled(false);
            chipMicrophone.setEnabled(false);
            chipHistory.setEnabled(false);
            chipFingerprint.setEnabled(false);
            chipCookie.setEnabled(false);
            chipJavaScript.setEnabled(false);
            chipJavaScriptPopup.setEnabled(false);
            chipDom.setEnabled(false);
        }

        String text;
        if (ninjaWebView.isNightMode()) {
            text = context.getString(R.string.libbrs_menu_dayView);
        } else {
            text = context.getString(R.string.libbrs_menu_nightView);
        }
        Button chipNightView = dialogView.findViewById(R.id.chip_toggleNightView);
        chipNightView.setText(text);
        chipNightView.setOnClickListener(v -> {
            ninjaWebView.toggleNightMode();
            callback.onToggleNightMode(ninjaWebView.isNightMode());
            dialog.cancel();
        });

        String textDesktopMode;
        if (ninjaWebView.isDesktopMode()) {
            textDesktopMode = context.getString(R.string.libbrs_menu_mobileView);
        } else {
            textDesktopMode = context.getString(R.string.libbrs_menu_desktopView);
        }
        Button chipViewMode = dialogView.findViewById(R.id.chip_toggleDesktop);
        chipViewMode.setText(textDesktopMode);
        chipViewMode.setOnClickListener(v -> {
            ninjaWebView.toggleDesktopMode(true);
            dialog.cancel();
        });

        dialogView.findViewById(R.id.ib_reload)
                .setOnClickListener(view -> {
                    dialog.cancel();
                    ninjaWebView.reload();
                });
    }

    public interface ToggleCallback{
        void onToggleNightMode(boolean isNight);
    }
}
