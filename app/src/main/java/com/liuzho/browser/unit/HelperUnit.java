package com.liuzho.browser.unit;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.liuzho.browser.R;
import com.liuzho.browser.browser.DataURIParser;
import com.liuzho.browser.storage.BrowserPref;
import com.liuzho.browser.view.GridItem;
import com.liuzho.browser.view.NinjaToast;
import com.liuzho.lib.baseutils.PermissionUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static android.content.Context.DOWNLOAD_SERVICE;

public class HelperUnit {

    private static final int REQUEST_CODE_ASK_PERMISSIONS_1 = 1234;
    private static final int REQUEST_CODE_ASK_PERMISSIONS_2 = 12345;
    private static final int REQUEST_CODE_ASK_PERMISSIONS_3 = 123456;

    public static void grantPermissionsLoc(Activity activity) {
        if (!PermissionUtils.hasSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
            builder.setIcon(R.drawable.libbrs_icon_alert);
            builder.setTitle(R.string.libbrs_setting_title_location);
            builder.setMessage(R.string.libbrs_site_req_location_permission_summary);
            builder.setPositiveButton(R.string.libbrs_grant_permission, (dialog, whichButton) -> activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_ASK_PERMISSIONS_1));
            builder.setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            HelperUnit.setupDialog(activity, dialog);
        }
    }

    public static void grantPermissionsCamera(Activity activity) {
        if (!PermissionUtils.hasSelfPermission(activity, Manifest.permission.CAMERA)) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
            builder.setIcon(R.drawable.libbrs_icon_alert);
            builder.setTitle(R.string.libbrs_setting_title_camera);
            builder.setMessage(R.string.libbrs_site_req_camera_permission_summary);
            builder.setPositiveButton(R.string.libbrs_grant_permission, (dialog, whichButton) -> activity.requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_ASK_PERMISSIONS_2));
            builder.setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            HelperUnit.setupDialog(activity, dialog);
        }
    }

    public static void grantPermissionsMic(Activity activity) {
        if (!PermissionUtils.hasSelfPermission(activity, Manifest.permission.RECORD_AUDIO)) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
            builder.setIcon(R.drawable.libbrs_icon_alert);
            builder.setTitle(R.string.libbrs_setting_title_microphone);
            builder.setMessage(R.string.libbrs_site_req_miscrophone_permission_summary);
            builder.setPositiveButton(R.string.libbrs_grant_permission, (dialog, whichButton) -> activity.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_ASK_PERMISSIONS_3));
            builder.setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            HelperUnit.setupDialog(activity, dialog);
        }
    }

    public static void saveAs(AlertDialog dialogToCancel, final Activity activity, final String url) {

        try {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
            View dialogView = View.inflate(activity, R.layout.libbrs_dialog_edit_extension, null);

            final EditText editTitle = dialogView.findViewById(R.id.dialog_edit_1);
            final EditText editExtension = dialogView.findViewById(R.id.dialog_edit_2);

            String filename = URLUtil.guessFileName(url, null, null);
            editTitle.setText(HelperUnit.fileName(url));

            String extension = filename.substring(filename.lastIndexOf("."));
            if (extension.length() <= 8) {
                editExtension.setText(extension);
            }

            builder.setView(dialogView);
            builder.setTitle(R.string.libbrs_menu_save_as);
            builder.setIcon(R.drawable.libbrs_icon_alert);
            builder.setMessage(url);
            builder.setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {

                String title = editTitle.getText().toString().trim();
                String extension1 = editExtension.getText().toString().trim();
                String filename1 = title + extension1;

                if (title.isEmpty() || extension1.isEmpty() || !extension1.startsWith(".")) {
                    NinjaToast.show(activity, activity.getString(R.string.libbrs_toast_input_empty));
                } else {
                    if (BackupUnit.checkPermissionStorage(activity)) {
                        Uri source = Uri.parse(url);
                        DownloadManager.Request request = new DownloadManager.Request(source);
                        request.addRequestHeader("List_protected", CookieManager.getInstance().getCookie(url));
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename1);
                        DownloadManager dm = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
                        assert dm != null;
                        dm.enqueue(request);
                        hideSoftKeyboard(editExtension, activity);
                        dialogToCancel.cancel();
                    } else {
                        BackupUnit.requestPermission(activity);
                    }
                }
            });
            builder.setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> {
                hideSoftKeyboard(editExtension, activity);
                dialogToCancel.cancel();
            });
            AlertDialog dialog = builder.create();
            dialog.show();
            HelperUnit.setupDialog(activity, dialog);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void createShortcut(Context context, String title, String url) {
        try {
            Intent i = new Intent();
            i.setAction(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { // code for adding shortcut on pre oreo device
                Intent installer = new Intent();
                installer.putExtra("android.intent.extra.shortcut.INTENT", i);
                installer.putExtra("android.intent.extra.shortcut.NAME", title);
                installer.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context.getApplicationContext(), R.drawable.libbrs_browser_icon_tab));
                installer.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                context.sendBroadcast(installer);
            } else {
                ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
                assert shortcutManager != null;
                if (shortcutManager.isRequestPinShortcutSupported()) {
                    ShortcutInfo pinShortcutInfo =
                            new ShortcutInfo.Builder(context, url)
                                    .setShortLabel(title)
                                    .setLongLabel(title)
                                    .setIcon(Icon.createWithResource(context, R.drawable.libbrs_browser_icon_tab))
                                    .setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    .build();
                    shortcutManager.requestPinShortcut(pinShortcutInfo, null);
                } else {
                    System.out.println("failed_to_add");
                }
            }
        } catch (Exception e) {
            System.out.println("failed_to_add");
        }
    }

    public static String fileName(String url) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        String domain = Objects.requireNonNull(Uri.parse(url).getHost()).replace("www.", "").trim();
        return domain.replace(".", "_").trim() + "_" + currentTime.trim();
    }

    public static String domain(String url) {
        if (url == null) {
            return "";
        } else {
            try {
                return Objects.requireNonNull(Uri.parse(url).getHost()).replace("www.", "").trim();
            } catch (Exception e) {
                return "";
            }
        }
    }

    public static void addFilterItems(Activity activity, List<GridItem> gridList) {
    }


    public static void setFilterIcons(ImageView ib_icon, long newIcon) {

    }

    public static void saveDataURI(AlertDialog dialogToCancel, Activity activity, DataURIParser dataUriParser) {

        byte[] imagedata = dataUriParser.getImagedata();
        String filename = dataUriParser.getFilename();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        View dialogView = View.inflate(activity, R.layout.libbrs_dialog_edit_extension, null);

        final EditText editTitle = dialogView.findViewById(R.id.dialog_edit_1);
        final EditText editExtension = dialogView.findViewById(R.id.dialog_edit_2);

        editTitle.setText(filename.substring(0, filename.indexOf(".")));

        String extension = filename.substring(filename.lastIndexOf("."));
        if (extension.length() <= 8) {
            editExtension.setText(extension);
        }

        builder.setView(dialogView);
        builder.setTitle(R.string.libbrs_menu_save_as);
        builder.setMessage(dataUriParser.toString());
        builder.setIcon(R.drawable.libbrs_icon_alert);
        builder.setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {

            String title = editTitle.getText().toString().trim();
            String extension1 = editExtension.getText().toString().trim();
            String filename1 = title + extension1;

            if (title.isEmpty() || extension1.isEmpty() || !extension1.startsWith(".")) {
                NinjaToast.show(activity, activity.getString(R.string.libbrs_toast_input_empty));
            } else {
                if (BackupUnit.checkPermissionStorage(activity)) {
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename1);
                    try {
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(imagedata);
                    } catch (Exception e) {
                        System.out.println("Error Downloading File: " + e.toString());
                        e.printStackTrace();
                    }
                    dialogToCancel.cancel();
                } else {
                    BackupUnit.requestPermission(activity);
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> builder.setCancelable(true));

        AlertDialog dialog = builder.create();
        dialog.show();
        HelperUnit.setupDialog(activity, dialog);
    }

    public static void showSoftKeyboard(View view, Activity context) {
        assert view != null;
        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            if (view.requestFocus()) {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 50);
    }

    public static void hideSoftKeyboard(View view, Context context) {
        assert view != null;
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void setupDialog(Context context, Dialog dialog) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorError, typedValue, true);
        int color = typedValue.data;
        ImageView imageView = dialog.findViewById(android.R.id.icon);
        if (imageView != null) imageView.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
    }

    public static void triggerRebirth(Activity activity) {
        BrowserPref browserPref = BrowserPref.getInstance();
        browserPref.setRestartChanged(false);
        browserPref.setRestoreOnRestart(true);

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.libbrs_menu_restart)
                .setIcon(R.drawable.libbrs_icon_alert)
                .setMessage(R.string.libbrs_toast_restart)
                .setPositiveButton(android.R.string.ok, (d, whichButton) -> activity.recreate())
                .setNegativeButton(android.R.string.cancel, (d, whichButton) -> d.cancel())
                .show();
        HelperUnit.setupDialog(activity, dialog);
    }

    public static void shareLink(Context context, String title, String url) {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        sharingIntent.putExtra(Intent.EXTRA_TEXT, url);
        context.startActivity(Intent.createChooser(sharingIntent, context.getString(R.string.libbrs_menu_share_link)));
    }
}