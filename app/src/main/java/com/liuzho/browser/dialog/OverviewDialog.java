package com.liuzho.browser.dialog;

import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.liuzho.browser.R;
import com.liuzho.browser.activity.BrowserActivity;
import com.liuzho.browser.database.FaviconHelper;
import com.liuzho.browser.database.Record;
import com.liuzho.browser.database.RecordAction;
import com.liuzho.browser.storage.BrowserPref;
import com.liuzho.browser.unit.HelperUnit;
import com.liuzho.browser.unit.RecordUnit;
import com.liuzho.browser.view.GridAdapter;
import com.liuzho.browser.view.GridItem;
import com.liuzho.browser.view.NinjaWebView;
import com.liuzho.browser.view.RecordAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.liuzho.browser.database.RecordAction.BOOKMARK_ITEM;
import static com.liuzho.browser.database.RecordAction.STARTSITE_ITEM;

public class OverviewDialog {

    private BottomSheetDialog overViewDialog;
    private final BrowserActivity brsActivity;
    private static final int TYPE_BOOKMARKS = 0;
    private static final int TYPE_HISTORY = 1;
    private static final int TYPE_HOME = 2;

    public OverviewDialog(BrowserActivity brsActivity) {
        this.brsActivity = brsActivity;
    }

    public void hide() {
        if (overViewDialog != null && overViewDialog.isShowing()) {
            overViewDialog.cancel();
        }
    }

    public void showBookmarks(NinjaWebView webView) {
        show(webView, TYPE_BOOKMARKS);
    }

    public void showHistory(NinjaWebView webView) {
        show(webView, TYPE_HISTORY);
    }

    public void showHome(NinjaWebView webView) {
        show(webView, TYPE_HOME);
    }

    /**
     * @param type TYPE_BOOKMARKS、TYPE_HISTORY、TYPE_HOME
     */
    private void show(NinjaWebView ninjaWebView, int type) {
        hide();
        overViewDialog = new BottomSheetDialog(brsActivity);
        View dialogView = View.inflate(brsActivity, R.layout.libbrs_dialog_overview, null);
        TextView tvTitle = dialogView.findViewById(R.id.overview_title);
        ImageButton btnClose = dialogView.findViewById(R.id.overview_close);

        btnClose.setOnClickListener(view -> brsActivity.hideOverview());

        ListView listView = dialogView.findViewById(R.id.list_overView);
        // allow scrolling in listView without closing the bottomSheetDialog
        // @noinspection ClickableViewAccessibility
        listView.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                // Disallow NestedScrollView to intercept touch events.
                if (listView.canScrollVertically(-1)) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                }
            }
            // Handle ListView touch events.
            v.onTouchEvent(event);
            return true;
        });

        overViewDialog.setContentView(dialogView);
        RecordAction action = new RecordAction(brsActivity);
        action.open(false);
        switch (type) {
            case TYPE_BOOKMARKS:
                tvTitle.setText(R.string.libbrs_album_title_bookmarks);
                attachBookmarksAdapter(listView,
                        action.listBookmark(brsActivity, false, 0),
                        ninjaWebView);
                break;
            case TYPE_HISTORY:
                tvTitle.setText(R.string.libbrs_album_title_history);
                attachHistoryAdapter(listView, action.listHistory(), ninjaWebView);
                break;
            case TYPE_HOME:
                tvTitle.setText(R.string.libbrs_album_title_home);
                attachHomeAdapter(listView, action.listStartSite(), ninjaWebView);
                break;
            default:
                throw new IllegalArgumentException("unknown type " + type);
        }
        action.close();


        BottomSheetBehavior<View> mBehavior = BottomSheetBehavior.from((View) dialogView.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        mBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN || newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    brsActivity.hideOverview();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });
        overViewDialog.show();
    }

    private void attachHomeAdapter(ListView listView, List<Record> list, NinjaWebView ninjaWebView) {
        RecordAdapter adapter = new RecordAdapter(brsActivity, list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (list.get(position).getDesktopMode() != ninjaWebView.isDesktopMode())
                ninjaWebView.toggleDesktopMode(false);
            if (list.get(position).getNightMode() != ninjaWebView.isNightMode()) {
                ninjaWebView.toggleNightMode();
            }
            ninjaWebView.loadUrl(list.get(position).getURL());
            brsActivity.hideOverview();
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showContextMenuList(TYPE_HOME, list.get(position).getTitle(), list.get(position).getURL(), adapter, list, position);
            return true;
        });
    }

    private void attachHistoryAdapter(ListView listView, List<Record> list, NinjaWebView ninjaWebView) {
        RecordAdapter adapter = new RecordAdapter(brsActivity, list) {
            @Override
            @NonNull
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                v.findViewById(R.id.record_item_time).setVisibility(View.VISIBLE);
                return v;
            }
        };
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (list.get(position).getDesktopMode() != ninjaWebView.isDesktopMode()) {
                ninjaWebView.toggleDesktopMode(false);
            }
            if (list.get(position).getNightMode() != ninjaWebView.isNightMode()) {
                ninjaWebView.toggleNightMode();
            }
            ninjaWebView.loadUrl(list.get(position).getURL());
            brsActivity.hideOverview();
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showContextMenuList(TYPE_HISTORY, list.get(position).getTitle(), list.get(position).getURL(), adapter, list, position);
            return true;
        });
    }

    private void attachBookmarksAdapter(ListView listView, List<Record> list, NinjaWebView ninjaWebView) {
        RecordAdapter adapter = new RecordAdapter(brsActivity, list);

        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (list.get(position).getDesktopMode() != ninjaWebView.isDesktopMode()) {
                ninjaWebView.toggleDesktopMode(false);
            }
            if (list.get(position).getNightMode() != ninjaWebView.isNightMode()) {
                ninjaWebView.toggleNightMode();
            }
            ninjaWebView.loadUrl(list.get(position).getURL());
            brsActivity.hideOverview();
        });
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showContextMenuList(TYPE_BOOKMARKS, list.get(position).getTitle(), list.get(position).getURL(), adapter, list, position);
            return true;
        });
    }

    private void showContextMenuList(int type, CharSequence title, String url,
                                     RecordAdapter adapterRecord, List<Record> recordList, int location) {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(brsActivity);
        View dialogView = View.inflate(brsActivity, R.layout.libbrs_dialog_menu, null);

        TextView tvTitle = dialogView.findViewById(R.id.menuTitle);
        tvTitle.setText(title);
        FaviconHelper.setFavicon(brsActivity, dialogView, url, R.id.menu_icon, R.drawable.libbrs_icon_image_broken);

        GridView gvMenu = dialogView.findViewById(R.id.menu_grid);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();
        Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);

        GridItem itemNewTabForeground = new GridItem(0, brsActivity.getString(R.string.libbrs_main_menu_new_tabOpen), 0);
        GridItem itemNewTabBackground = new GridItem(0, brsActivity.getString(R.string.libbrs_main_menu_new_tab), 0);
        GridItem itemNewTabWithProfile = new GridItem(0, brsActivity.getString(R.string.libbrs_main_menu_new_tabProfile), 0);
        GridItem itemShareLink = new GridItem(0, brsActivity.getString(R.string.libbrs_menu_share_link), 0);
        GridItem itemDelete = new GridItem(0, brsActivity.getString(R.string.libbrs_menu_delete), 0);
        GridItem itemEdit = new GridItem(0, brsActivity.getString(R.string.libbrs_menu_edit), 0);

        List<GridItem> gridList = new ArrayList<>();

        List<Runnable> actions = new ArrayList<>();
        gridList.add(itemNewTabForeground);
        actions.add(() -> {
            brsActivity.addAlbum(brsActivity.getString(R.string.libbrs_app_name), url, true, false, "");
            brsActivity.hideOverview();
        });

        gridList.add(itemNewTabBackground);
        actions.add(() -> {
            brsActivity.addAlbum(brsActivity.getString(R.string.libbrs_app_name), url, false, false, "");
        });

        gridList.add(itemNewTabWithProfile);
        actions.add(() -> {
            brsActivity.addAlbum(brsActivity.getString(R.string.libbrs_app_name), url, true, true, "");
            brsActivity.hideOverview();
        });

        gridList.add(itemShareLink);
        actions.add(() -> HelperUnit.shareLink(brsActivity, "", url));

        gridList.add(itemDelete);
        actions.add(() -> {
            AlertDialog confirmDialog = new MaterialAlertDialogBuilder(brsActivity)
                    .setIcon(R.drawable.libbrs_icon_alert)
                    .setTitle(R.string.libbrs_menu_delete)
                    .setMessage(R.string.libbrs_hint_database)
                    .setPositiveButton(android.R.string.ok, (dialog2, whichButton) -> {
                        String recordUrl = recordList.remove(location).getURL();
                        RecordAction action = new RecordAction(brsActivity);
                        action.open(true);
                        if (type == TYPE_HOME) {
                            action.deleteURL(recordUrl, RecordUnit.TABLE_START);
                        } else if (type == TYPE_BOOKMARKS) {
                            action.deleteURL(recordUrl, RecordUnit.TABLE_BOOKMARK);
                        } else if (type == TYPE_HISTORY) {
                            action.deleteURL(recordUrl, RecordUnit.TABLE_HISTORY);
                        }
                        action.close();
                        adapterRecord.notifyDataSetChanged();
                    })
                    .setNegativeButton(android.R.string.cancel, (d, whichButton) -> d.cancel())
                    .show();
            HelperUnit.setupDialog(brsActivity, confirmDialog);
        });

        if (type == TYPE_BOOKMARKS || type == TYPE_HOME) {
            gridList.add(itemEdit);
            Runnable refresh = adapterRecord::notifyDataSetChanged;
            actions.add(() -> showEditRecordDialog(type, title, url, recordList, location, refresh));
        }

        GridAdapter gridAdapter = new GridAdapter(brsActivity, gridList);
        gvMenu.setAdapter(gridAdapter);
        gvMenu.setOnItemClickListener((parent, view, position, id) -> {
            dialog.cancel();
            actions.get(position).run();
        });
    }

    private void showEditRecordDialog(int type, CharSequence title, String url, List<Record> recordList, int location, Runnable refresh) {
        View dialogViewSubMenu = View.inflate(brsActivity, R.layout.libbrs_dialog_edit_title, null);

        TextInputLayout eilTitle = dialogViewSubMenu.findViewById(R.id.edit_title_layout);
        TextInputLayout eilUserName = dialogViewSubMenu.findViewById(R.id.edit_userName_layout);
        TextInputLayout eilPwd = dialogViewSubMenu.findViewById(R.id.edit_PW_layout);
        eilTitle.setVisibility(View.VISIBLE);
        eilUserName.setVisibility(View.GONE);
        eilPwd.setVisibility(View.GONE);

        EditText etTitle = dialogViewSubMenu.findViewById(R.id.edit_title);
        etTitle.setText(title);

        TextInputLayout eilURL = dialogViewSubMenu.findViewById(R.id.edit_URL_layout);
        eilURL.setVisibility(View.VISIBLE);
        EditText etURL = dialogViewSubMenu.findViewById(R.id.edit_URL);
        etURL.setVisibility(View.VISIBLE);
        etURL.setText(url);

        Record record = recordList.get(location);

        Chip chipViewMode = dialogViewSubMenu.findViewById(R.id.edit_bookmark_desktopMode);
        chipViewMode.setChecked(record.getDesktopMode());

        Chip chipNightMode = dialogViewSubMenu.findViewById(R.id.edit_bookmark_nightMode);
        chipNightMode.setChecked(record.getNightMode());

        AlertDialog dialog = new MaterialAlertDialogBuilder(brsActivity)
                .setView(dialogViewSubMenu)
                .setTitle(R.string.libbrs_menu_edit)
                .setIcon(R.drawable.libbrs_icon_alert)
                .setMessage(url)
                .setPositiveButton(android.R.string.ok, (dialog3, whichButton) -> {
                    RecordAction action = new RecordAction(brsActivity);
                    action.open(true);
                    Record newRecord;
                    if (type == TYPE_BOOKMARKS) {
                        action.deleteURL(url, RecordUnit.TABLE_BOOKMARK);
                        newRecord = new Record(etTitle.getText().toString(), etURL.getText().toString(),
                                0, 0, BOOKMARK_ITEM,
                                chipViewMode.isChecked(), chipNightMode.isChecked(), 0);
                        action.addBookmark(newRecord);
                    } else {
                        action.deleteURL(url, RecordUnit.TABLE_START);
                        int counter = BrowserPref.getInstance().getAndIncreaseCounter();
                        newRecord = new Record(etTitle.getText().toString(), etURL.getText().toString(),
                                0, counter, STARTSITE_ITEM,
                                chipViewMode.isChecked(), chipNightMode.isChecked(), 0);
                        action.addStartSite(newRecord);
                    }
                    action.close();
                    recordList.remove(location);
                    recordList.add(location, newRecord);
                    refresh.run();
                })
                .setNegativeButton(android.R.string.cancel, (d, w) -> d.cancel())
                .show();
        Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
    }
}
