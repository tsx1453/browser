package com.liuzho.browser.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.liuzho.browser.R;
import com.liuzho.browser.activity.BrowserActivity;
import com.liuzho.browser.activity.SettingsActivity;
import com.liuzho.browser.database.FaviconHelper;
import com.liuzho.browser.database.Record;
import com.liuzho.browser.database.RecordAction;
import com.liuzho.browser.storage.BrowserPref;
import com.liuzho.browser.unit.HelperUnit;
import com.liuzho.browser.unit.RecordUnit;
import com.liuzho.browser.view.GridAdapter;
import com.liuzho.browser.view.GridItem;
import com.liuzho.browser.view.NinjaToast;
import com.liuzho.browser.view.NinjaWebView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class OverflowDialog {
    private final BrowserActivity brsActivity;
    private final NinjaWebView ninjaWebView;
    private String url;
    private String title;
    private GridAdapter adapter;
    private final List<TabModel> tabModels = new ArrayList<>();

    static class TabModel {
        AdapterView.OnItemClickListener clickListener;
        int icon;
        List<GridItem> dataList;
    }

    public OverflowDialog(BrowserActivity activity, NinjaWebView ninjaWebView) {
        this.brsActivity = activity;
        this.ninjaWebView = ninjaWebView;
    }

    public void show() {

        url = ninjaWebView.getUrl();
        title = ninjaWebView.getTitle();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(brsActivity);
        View dialogView = View.inflate(brsActivity, R.layout.libbrs_dialog_menu_overflow, null);

        builder.setView(dialogView);
        AlertDialog overflowDialog = builder.create();
        overflowDialog.show();
        Objects.requireNonNull(overflowDialog.getWindow()).setGravity(Gravity.TOP);

        FaviconHelper.setFavicon(brsActivity, dialogView, url, R.id.menu_icon, R.drawable.libbrs_icon_image_broken);

        TextView tvTitle = dialogView.findViewById(R.id.overflow_title);
        tvTitle.setText(TextUtils.isEmpty(title) ? url : title);

        TabLayout tabLayout = dialogView.findViewById(R.id.tabLayout);


        GridView menuGridTab = dialogView.findViewById(R.id.overflow_tab);

        adapter = new GridAdapter(brsActivity, Collections.emptyList());

        // Tab
        setupTabGrid(overflowDialog);
        // Bookmark
        setupTabBookmark(overflowDialog);
        // Share
        setupTabShare(overflowDialog);
        // Save
        setupTabSave(overflowDialog);
        // Other
        setupTabOther(overflowDialog);

        for (TabModel tab : tabModels) {
            tabLayout.addTab(tabLayout.newTab().setIcon(tab.icon).setTag(tab));
        }

        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        menuGridTab.setAdapter(adapter);
        TabLayout.OnTabSelectedListener listener = new TabLayout.OnTabSelectedListener() {

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                TabModel tabModel = (TabModel) tab.getTag();
                Objects.requireNonNull(tabModel);
                adapter.refresh(tabModel.dataList);
                menuGridTab.setOnItemClickListener(tabModel.clickListener);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        };
        tabLayout.addOnTabSelectedListener(listener);
        listener.onTabSelected(tabLayout.getTabAt(0));

    }

    private void setupTabOther(AlertDialog overflowDialog) {
        GridItem itemSearchOnSite = new GridItem(0, brsActivity.getString(R.string.libbrs_menu_other_searchSite), 0);
        GridItem itemDownload = new GridItem(0, brsActivity.getString(R.string.libbrs_menu_download), 0);
        GridItem itemSettings = new GridItem(0, brsActivity.getString(R.string.libbrs_setting_label), 0);
        GridItem itemRestart = new GridItem(0, brsActivity.getString(R.string.libbrs_menu_restart), 0);
        GridItem itemViewMode;
        if (ninjaWebView.isDesktopMode()) {
            itemViewMode = new GridItem(0, brsActivity.getString(R.string.libbrs_menu_mobileView), 0);
        } else {
            itemViewMode = new GridItem(0, brsActivity.getString(R.string.libbrs_menu_desktopView), 0);
        }

        GridItem itemNightMode;
        if (ninjaWebView.isNightMode()) {
            itemNightMode = new GridItem(0, brsActivity.getString((R.string.libbrs_menu_dayView)), 0);
        } else {
            itemNightMode = new GridItem(0, brsActivity.getString((R.string.libbrs_menu_nightView)), 0);
        }

        TabModel tabOther = new TabModel();
        tabOther.dataList = Arrays.asList(
                itemSearchOnSite, itemViewMode, itemNightMode,
                itemDownload, itemSettings, itemRestart);
        tabOther.icon = R.drawable.libbrs_icon_dots;
        tabOther.clickListener = (parent, view, position, id) -> {
            overflowDialog.cancel();
            if (position == 0) {
                brsActivity.searchOnSite();
            } else if (position == 1) {
                ninjaWebView.toggleDesktopMode(true);
            } else if (position == 2) {
                ninjaWebView.toggleNightMode();
                brsActivity.isNightMode = ninjaWebView.isNightMode();
            } else if (position == 3) {
                brsActivity.startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
            } else if (position == 4) {
                Intent settings = new Intent(brsActivity, SettingsActivity.class);
                brsActivity.startActivity(settings);
            } else if (position == 5) {
                brsActivity.saveOpenedTabs();
                HelperUnit.triggerRebirth(brsActivity);
            }
        };
        tabModels.add(tabOther);
    }

    private void setupTabShare(AlertDialog overflowDialog) {
        GridItem itemShareLink = new GridItem(0, brsActivity.getString(R.string.libbrs_menu_share_link), 0);
        GridItem itemCopyToClipboard = new GridItem(0, brsActivity.getString(R.string.libbrs_menu_shareClipboard), 0);
        GridItem itemOpenLinkWith = new GridItem(0, brsActivity.getString(R.string.libbrs_menu_open_with), 0);

        TabModel tabShare = new TabModel();
        tabShare.dataList = Arrays.asList(
                itemShareLink, itemCopyToClipboard, itemOpenLinkWith);
        tabShare.icon = R.drawable.libbrs_icon_menu_share;
        tabShare.clickListener = (parent, view12, position, id) -> {
            overflowDialog.cancel();
            if (position == 0) {
                HelperUnit.shareLink(brsActivity, title, url);
            } else if (position == 1) {
                ClipboardManager clipboard = (ClipboardManager) brsActivity.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("text", url);
                Objects.requireNonNull(clipboard).setPrimaryClip(clip);
                NinjaToast.show(brsActivity, R.string.libbrs_toast_copy_successful);
            } else if (position == 2) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                Intent chooser = Intent.createChooser(intent, brsActivity.getString(R.string.libbrs_menu_open_with));
                brsActivity.startActivity(chooser);
            }
        };
        tabModels.add(tabShare);
    }

    private void setupTabSave(AlertDialog overflowDialog) {
        GridItem itemFavorite = new GridItem(0, brsActivity.getString(R.string.libbrs_menu_fav), 0);
        GridItem itemSaveToHome = new GridItem(0, brsActivity.getString(R.string.libbrs_menu_save_home), 0);
        GridItem itemSaveToBookmark = new GridItem(0, brsActivity.getString(R.string.libbrs_menu_save_bookmark), 0);
        GridItem itemSavePdf = new GridItem(0, brsActivity.getString(R.string.libbrs_menu_save_pdf), 0);
        GridItem itemSaveShortcut = new GridItem(0, brsActivity.getString(R.string.libbrs_menu_sc), 0);
        GridItem itemSaveAs = new GridItem(0, brsActivity.getString(R.string.libbrs_menu_save_as), 0);

        TabModel tabSave = new TabModel();
        tabSave.dataList = Arrays.asList(
                itemFavorite, itemSaveToHome, itemSaveToBookmark,
                itemSavePdf, itemSaveShortcut, itemSaveAs);
        tabSave.icon = R.drawable.libbrs_icon_menu_save;
        tabSave.clickListener = (parent, view, position, id) -> {
            overflowDialog.cancel();
            RecordAction action = new RecordAction(brsActivity);
            if (position == 0) {
                BrowserPref.getInstance().setFavoriteUrl(url);
                NinjaToast.show(brsActivity, R.string.libbrs_app_done);
            } else if (position == 1) {
                brsActivity.saveAtHome(title, url);
            } else if (position == 2) {
                saveBookmark();
                action.close();
            } else if (position == 3) {
                printPDF();
            } else if (position == 4) {
                HelperUnit.createShortcut(brsActivity, ninjaWebView.getTitle(), ninjaWebView.getUrl());
            } else if (position == 5) {
                HelperUnit.saveAs(overflowDialog, ((Activity) brsActivity), url);
            }
        };
        tabModels.add(tabSave);
    }

    private void printPDF() {
        String title = HelperUnit.fileName(ninjaWebView.getUrl());
        PrintManager printManager = (PrintManager) brsActivity.getSystemService(Context.PRINT_SERVICE);
        PrintDocumentAdapter printAdapter = ninjaWebView.createPrintDocumentAdapter(title);
        Objects.requireNonNull(printManager).print(title, printAdapter, new PrintAttributes.Builder().build());
        BrowserPref.getInstance().setPdfCreate(true);
    }

    private void saveBookmark() {
        FaviconHelper faviconHelper = new FaviconHelper(brsActivity);
        faviconHelper.addFavicon(ninjaWebView.getUrl(), ninjaWebView.getFavicon());
        RecordAction action = new RecordAction(brsActivity);
        action.open(true);
        if (action.checkUrl(ninjaWebView.getUrl(), RecordUnit.TABLE_BOOKMARK)) {
            NinjaToast.show(brsActivity, R.string.libbrs_app_error);
        } else {
            long value = 11;  //default red icon
            action.addBookmark(new Record(ninjaWebView.getTitle(), ninjaWebView.getUrl(),
                    0, 0, 2,
                    ninjaWebView.isDesktopMode(), ninjaWebView.isNightMode(), value));
            NinjaToast.show(brsActivity, R.string.libbrs_app_done);
        }
        action.close();
    }

    private void setupTabBookmark(AlertDialog overflowDialog) {
        GridItem itemBookmarks = new GridItem(0, brsActivity.getString(R.string.libbrs_album_title_bookmarks), 0);
        GridItem itemHistory = new GridItem(0, brsActivity.getString(R.string.libbrs_album_title_history), 0);
        GridItem itemHome = new GridItem(0, brsActivity.getString(R.string.libbrs_album_title_home), 0);

        TabModel tabBookmark = new TabModel();
        tabBookmark.dataList = Arrays.asList(itemBookmarks, itemHistory, itemHome);
        tabBookmark.icon = R.drawable.libbrs_icon_web;
        tabBookmark.clickListener = (adapterView, view, position, l) -> {
            switch (position) {
                case 0:
                    brsActivity.showBookmarkOverview();
                    break;
                case 1:
                    brsActivity.showHistoryOverview();
                    break;
                case 2:
                    brsActivity.showHomePageOverview();
                    break;
                default:
                    break;
            }
            overflowDialog.cancel();
        };
        tabModels.add(tabBookmark);
    }

    private void setupTabGrid(Dialog overflowDialog) {
        GridItem itemOpenFav = new GridItem(0, brsActivity.getString(R.string.libbrs_menu_openFav), 0);
        GridItem itemNewTab = new GridItem(0, brsActivity.getString(R.string.libbrs_main_menu_new_tabOpen), 0);
        GridItem itemNewTabWithProfile = new GridItem(0, brsActivity.getString(R.string.libbrs_main_menu_new_tabProfile), 0);
        GridItem itemReload = new GridItem(0, brsActivity.getString(R.string.libbrs_menu_reload), 0);
        GridItem itemCloseTab = new GridItem(0, brsActivity.getString(R.string.libbrs_menu_closeTab), 0);
        GridItem itemQuit = new GridItem(0, brsActivity.getString(R.string.libbrs_menu_quit), 0);

        TabModel tabTab = new TabModel();
        tabTab.dataList = Arrays.asList(
                itemOpenFav, itemNewTab, itemNewTabWithProfile,
                itemReload, itemCloseTab, itemQuit);
        tabTab.icon = R.drawable.libbrs_icon_tab;
        tabTab.clickListener = (parent, view, position, id) -> {
            overflowDialog.cancel();
            if (position == 0) {
                ninjaWebView.loadUrl(BrowserPref.getInstance().getFavoriteUrl());
            } else if (position == 1) {
                brsActivity.addAlbum(brsActivity.getString(R.string.libbrs_app_name), BrowserPref.getInstance().getFavoriteUrl(), true, false, "");
            } else if (position == 2) {
                brsActivity.addAlbum(brsActivity.getString(R.string.libbrs_app_name), BrowserPref.getInstance().getFavoriteUrl(), true, true, "");
            } else if (position == 3) {
                ninjaWebView.reload();
            } else if (position == 4) {
                brsActivity.removeAlbum(brsActivity.getCurrentAlbumController());
            } else if (position == 5) {
                brsActivity.finish();
            }
        };
        tabModels.add(tabTab);
    }
}
