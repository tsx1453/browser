package com.liuzho.browser.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.liuzho.browser.Browser;
import com.liuzho.browser.R;
import com.liuzho.browser.browser.AdBlock;
import com.liuzho.browser.browser.AlbumController;
import com.liuzho.browser.browser.BrowserContainer;
import com.liuzho.browser.browser.BrowserController;
import com.liuzho.browser.browser.DataURIParser;
import com.liuzho.browser.browser.ProtectedList;
import com.liuzho.browser.browser.StandardList;
import com.liuzho.browser.browser.TrustedList;
import com.liuzho.browser.database.FaviconHelper;
import com.liuzho.browser.database.Record;
import com.liuzho.browser.database.RecordAction;
import com.liuzho.browser.dialog.FastToggleDialog;
import com.liuzho.browser.dialog.OverflowDialog;
import com.liuzho.browser.dialog.OverviewDialog;
import com.liuzho.browser.storage.BrowserPref;
import com.liuzho.browser.unit.BrowserUnit;
import com.liuzho.browser.unit.HelperUnit;
import com.liuzho.browser.unit.RecordUnit;
import com.liuzho.browser.view.CompleteAdapter;
import com.liuzho.browser.view.GridAdapter;
import com.liuzho.browser.view.GridItem;
import com.liuzho.browser.view.NinjaToast;
import com.liuzho.browser.view.NinjaWebView;
import com.liuzho.browser.view.RecordAdapter;
import com.liuzho.browser.view.SwipeTouchListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static android.content.ContentValues.TAG;
import static android.webkit.WebView.HitTestResult.IMAGE_TYPE;
import static android.webkit.WebView.HitTestResult.SRC_ANCHOR_TYPE;
import static android.webkit.WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE;
import static com.liuzho.browser.database.RecordAction.BOOKMARK_ITEM;
import static com.liuzho.browser.database.RecordAction.HISTORY_ITEM;
import static com.liuzho.browser.database.RecordAction.STARTSITE_ITEM;

public class BrowserActivity extends AppCompatActivity implements BrowserController {

    // Menus

    private RecordAdapter adapter;
    private ImageButton omniBox_overview;
    private AutoCompleteTextView omniBox_text;

    // Views

    private EditText searchBox;
    private OverviewDialog overviewDialog;
    private AlertDialog dialog_tabPreview;
    private NinjaWebView ninjaWebView;
    private View customView;
    private VideoView videoView;
    private ImageButton omniBox_flavor;
    private KeyListener listener;
    private BadgeDrawable badgeDrawable;
    private LinearProgressIndicator progressBar;

    // Layouts

    private RelativeLayout searchPanel;
    private FrameLayout contentFrame;
    private LinearLayout tab_container;
    private FrameLayout fullscreenHolder;

    // Others

    private int mLastContentHeight = 0;

    private String overViewTab;
    private BroadcastReceiver downloadReceiver;

    private Activity activity;
    private Context context;
    private BrowserPref browserPref;
    private boolean orientationChanged;

    private ValueCallback<Uri[]> filePathCallback = null;
    private AlbumController currentAlbumController = null;

    private static final int INPUT_FILE_REQUEST_CODE = 1;
    private ValueCallback<Uri[]> mFilePathCallback;
    private final Browser.Callback globalCallback = Browser.getConfig().callback();
    private boolean searchOnSite;

    // Classes

    private class VideoCompletionListener implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            return false;
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            onHideCustomView();
        }
    }

    private final ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {

        @Override
        public void onGlobalLayout() {
            int currentContentHeight = findViewById(Window.ID_ANDROID_CONTENT).getHeight();
            if (mLastContentHeight > currentContentHeight + 100) {
                mLastContentHeight = currentContentHeight;
            } else if (currentContentHeight > mLastContentHeight + 100) {
                mLastContentHeight = currentContentHeight;
                omniBox_text.clearFocus();
            }
        }
    };

    // Overrides

    @Override
    public void onPause() {
        //Save open Tabs in shared preferences
        saveOpenedTabs();
        super.onPause();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        int theme = globalCallback.theme(false);
        if (theme != -1) {
            setTheme(theme);
        }
        super.onCreate(savedInstanceState);
        globalCallback.onActivityCreate(this);
        activity = BrowserActivity.this;
        context = BrowserActivity.this;
        browserPref = BrowserPref.getInstance();
        overviewDialog = new OverviewDialog(this);
        if (browserPref.keepScreenOn()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        OrientationEventListener mOrientationListener = new OrientationEventListener(getApplicationContext()) {
            @Override
            public void onOrientationChanged(int orientation) {
                orientationChanged = true;
            }
        };
        if (mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable();
        }
        browserPref.onBrowserCreate();

        switch (Objects.requireNonNull(browserPref.startTab())) {
            case "3":
                overViewTab = getString(R.string.libbrs_album_title_bookmarks);
                break;
            case "4":
                overViewTab = getString(R.string.libbrs_album_title_history);
                break;
            default:
                overViewTab = getString(R.string.libbrs_album_title_home);
                break;
        }

        setContentView(R.layout.libbrs_activity_main);

        contentFrame = findViewById(R.id.main_content);
        contentFrame.getViewTreeObserver().addOnGlobalLayoutListener(keyboardLayoutListener);

        new AdBlock(context);
        new TrustedList(context);
        new ProtectedList(context);
        new StandardList(context);

        downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                builder.setTitle(R.string.libbrs_menu_download);
                builder.setIcon(R.drawable.libbrs_icon_alert);
                builder.setMessage(R.string.libbrs_toast_downloadComplete);
                builder.setPositiveButton(android.R.string.ok, (dialog, whichButton) -> startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)));
                builder.setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> dialog.cancel());
                Dialog dialog = builder.create();
                dialog.show();
                HelperUnit.setupDialog(context, dialog);
            }
        };

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(downloadReceiver, filter);

        mLastContentHeight = findViewById(Window.ID_ANDROID_CONTENT).getHeight();

        initOmniBox();
        initTabDialog();
        initSearchPanel();
        dispatchIntent(getIntent());

        //restore open Tabs from shared preferences if app got killed
        if (browserPref.restoreTabs()
                || browserPref.reloadTabs()
                || browserPref.restoreOnRestart()) {
            String saveDefaultProfile = browserPref.profile();

            List<String> openTabs = browserPref.openTabs();
            List<String> openTabsProfile = browserPref.openTabsProfile();
            if (openTabs.size() > 0) {
                for (int counter = 0; counter < openTabs.size(); counter++) {
                    addAlbum(getString(R.string.libbrs_app_name), openTabs.get(counter), BrowserContainer.size() < 1, false, openTabsProfile.get(counter));
                }
            }
            browserPref.setProfile(saveDefaultProfile);
            browserPref.setRestoreOnRestart(false);
        }

        if (BrowserContainer.size() < 1) {  //if still no open Tab open default page
            addAlbum(getString(R.string.libbrs_app_name), BrowserPref.getInstance().getFavoriteUrl(), true, false, "");
            getIntent().setAction("");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        Uri[] results = null;
        // Check that the response is a good one
        if (resultCode == Activity.RESULT_OK) {
            if (data != null) {
                // If there is not data, then we may have taken a photo
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
        }
        mFilePathCallback.onReceiveValue(results);
        mFilePathCallback = null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (browserPref.pdfCreate()) {
            browserPref.setPdfCreate(false);
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setTitle(R.string.libbrs_menu_download);
            builder.setIcon(R.drawable.libbrs_icon_alert);
            builder.setMessage(R.string.libbrs_toast_downloadComplete);
            builder.setPositiveButton(android.R.string.ok, (dialog, whichButton) -> startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)));
            builder.setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            HelperUnit.setupDialog(context, dialog);
        }
        dispatchIntent(getIntent());
    }

    @Override
    public void onDestroy() {
        if (browserPref.clearWhenQuit()) {
            BrowserUnit.clearData(this);
        }
        BrowserContainer.clear();

        if (!browserPref.restoreOnRestart() && (!browserPref.reloadTabs() || browserPref.restartChanged())) {
            // clear open tabs in preferences
            browserPref.setOpenTabs(Collections.emptyList());
            browserPref.setOpenTabsProfile(Collections.emptyList());
        }

        unregisterReceiver(downloadReceiver);
        ninjaWebView.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardLayoutListener);
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                showOverflow();
            case KeyEvent.KEYCODE_BACK:
                hideOverview();
                if (fullscreenHolder != null || customView != null || videoView != null) {
                    Log.v(TAG, "BD Browser in fullscreen mode");
                } else if (searchPanel.getVisibility() == View.VISIBLE) {
                    stopSearchOnSite();
                } else if (ninjaWebView.canGoBack()) {
                    WebBackForwardList mWebBackForwardList = ninjaWebView.copyBackForwardList();
                    String historyUrl = mWebBackForwardList.getItemAtIndex(mWebBackForwardList.getCurrentIndex() - 1).getUrl();
                    ninjaWebView.initPreferences(historyUrl);
                    goBackSkipRedirects();
                } else {
                    removeAlbum(currentAlbumController);
                }
                return true;
        }
        return false;
    }

    @Override
    public synchronized void showAlbum(AlbumController controller) {
        View av = (View) controller;
        if (currentAlbumController != null) {
            currentAlbumController.deactivate();
        }
        currentAlbumController = controller;
        currentAlbumController.activate();
        contentFrame.removeAllViews();
        contentFrame.addView(av);
        updateOmniBox();
        if (searchPanel.getVisibility() == View.VISIBLE) {
            stopSearchOnSite();
        }
    }

    @Override
    public void updateAutoComplete() {
        RecordAction action = new RecordAction(this);
        List<Record> list = action.listEntries();
        CompleteAdapter adapter = new CompleteAdapter(this, R.layout.libbrs_item_icon_left, list);
        omniBox_text.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        omniBox_text.setDropDownWidth(context.getResources().getDisplayMetrics().widthPixels);
        omniBox_text.setOnItemClickListener((parent, view, position, id) -> {
            String url = ((TextView) view.findViewById(R.id.record_item_time)).getText().toString();
            for (Record record : list) {
                if (record.getURL().equals(url)) {
                    if ((record.getType() == BOOKMARK_ITEM) || (record.getType() == STARTSITE_ITEM) || (record.getType() == HISTORY_ITEM)) {
                        if (record.getDesktopMode() != ninjaWebView.isDesktopMode()) {
                            ninjaWebView.toggleDesktopMode(false);
                        }
                        if (record.getNightMode() != ninjaWebView.isNightMode()) {
                            ninjaWebView.toggleNightMode();
                        }
                        break;
                    }
                }
            }
            ninjaWebView.loadUrl(url);
        });
    }

    public void showBookmarkOverview() {
        overviewDialog.showBookmarks(ninjaWebView);
    }

    public void showHistoryOverview() {
        overviewDialog.showHistory(ninjaWebView);
    }

    public void showHomePageOverview() {
        overviewDialog.showHome(ninjaWebView);
    }

    public void hideOverview() {
        overviewDialog.hide();
    }

    public void hideTabView() {
        if (dialog_tabPreview != null) {
            dialog_tabPreview.hide();
        }
    }

    public void showTabView() {
        HelperUnit.hideSoftKeyboard(omniBox_text, context);
        dialog_tabPreview.show();
    }

    private void dispatchIntent(Intent intent) {
        String action = intent.getAction();
        String url = intent.getStringExtra(Intent.EXTRA_TEXT);

        if ("".equals(action)) {
            Log.i(TAG, "resumed BD browser");
        } else if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_PROCESS_TEXT)) {
            CharSequence text = getIntent().getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
            assert text != null;
            addAlbum(null, text.toString(), true, false, "");
            getIntent().setAction("");
            hideOverview();
        } else if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_WEB_SEARCH)) {
            addAlbum(null, Objects.requireNonNull(intent.getStringExtra(SearchManager.QUERY)), true, false, "");
            getIntent().setAction("");
            hideOverview();
        } else if (filePathCallback != null) {
            filePathCallback = null;
            getIntent().setAction("");
        } else if (url != null && Intent.ACTION_SEND.equals(action)) {
            addAlbum(getString(R.string.libbrs_app_name), url, true, false, "");
            getIntent().setAction("");
            hideOverview();
        } else if (Intent.ACTION_VIEW.equals(action)) {
            String data = Objects.requireNonNull(getIntent().getData()).toString();
            addAlbum(getString(R.string.libbrs_app_name), data, true, false, "");
            getIntent().setAction("");
            hideOverview();
        }
    }

    private void initTabDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialog_tabPreview_view = View.inflate(context, R.layout.libbrs_dialog_tabs, null);
        tab_container = dialog_tabPreview_view.findViewById(R.id.tab_container);
        builder.setView(dialog_tabPreview_view);
        dialog_tabPreview = builder.create();
        Objects.requireNonNull(dialog_tabPreview.getWindow()).setGravity(Gravity.TOP);
    }

    @SuppressLint({"ClickableViewAccessibility", "UnsafeExperimentalUsageError", "UnsafeOptInUsageError"})
    private void initOmniBox() {
        omniBox_text = findViewById(R.id.omniBox_input);
        listener = omniBox_text.getKeyListener(); // Save the default KeyListener!!!
        omniBox_text.setKeyListener(null); // Disable input
        omniBox_text.setEllipsize(TextUtils.TruncateAt.END);
        omniBox_flavor = findViewById(R.id.omniBox_flavor);
        omniBox_flavor.setOnClickListener(v -> showFastToggleDialog());
        omniBox_overview = findViewById(R.id.omnibox_overview);

        progressBar = findViewById(R.id.main_progress_bar);

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.colorSecondary, typedValue, true);
        int color = typedValue.data;

        badgeDrawable = BadgeDrawable.create(context);
        badgeDrawable.setBadgeGravity(BadgeDrawable.TOP_END);
        badgeDrawable.setNumber(BrowserContainer.size());
        badgeDrawable.setBackgroundColor(color);
        BadgeUtils.attachBadgeDrawable(badgeDrawable, omniBox_overview, findViewById(R.id.layout));

        ImageButton omniboxOverflow = findViewById(R.id.omnibox_overflow);
        omniboxOverflow.setOnClickListener(v -> showOverflow());
        omniBox_text.setOnEditorActionListener((v, actionId, event) -> {
            String query = omniBox_text.getText().toString().trim();
            ninjaWebView.loadUrl(query);
            return false;
        });
        omniBox_text.setOnFocusChangeListener((v, hasFocus) -> {
            if (omniBox_text.hasFocus()) {
                String url = ninjaWebView.getUrl();
                ninjaWebView.stopLoading();
                omniBox_text.setKeyListener(listener);
                if (url == null || url.isEmpty()) {
                    omniBox_text.setText("");
                } else {
                    omniBox_text.setText(url);
                }
                updateAutoComplete();
                omniBox_text.selectAll();
            } else {
                omniBox_text.setKeyListener(null);
                omniBox_text.setEllipsize(TextUtils.TruncateAt.END);
                omniBox_text.setText(ninjaWebView.getTitle());
                updateOmniBox();
            }
        });
        omniBox_overview.setOnClickListener(v -> showTabView());
    }

    private void initSearchPanel() {
        searchPanel = findViewById(R.id.searchBox);
        searchBox = findViewById(R.id.searchBox_input);
        ImageView searchUp = findViewById(R.id.searchBox_up);
        ImageView searchDown = findViewById(R.id.searchBox_down);
        ImageView searchCancel = findViewById(R.id.searchBox_cancel);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (currentAlbumController != null) {
                    ((NinjaWebView) currentAlbumController).findAllAsync(s.toString());
                }
            }
        });
        searchUp.setOnClickListener(v -> ((NinjaWebView) currentAlbumController).findNext(false));
        searchDown.setOnClickListener(v -> ((NinjaWebView) currentAlbumController).findNext(true));
        searchCancel.setOnClickListener(v -> {
            if (searchBox.getText().length() > 0) {
                searchBox.setText("");
            } else {
                stopSearchOnSite();
            }
        });
    }

    private void showFastToggleDialog() {
        FastToggleDialog.show(ninjaWebView, new FastToggleDialog.ToggleCallback() {
            @Override
            public void onToggleNightMode(boolean isNight) {
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setWebView(String title, final String url, final boolean foreground) {
        ninjaWebView = new NinjaWebView(context);

        ninjaWebView.toggleNightMode();
        ninjaWebView.setBrowserController(this);
        ninjaWebView.setAlbumTitle(title, url);

        registerForContextMenu(ninjaWebView);

        SwipeTouchListener swipeTouchListener;
        swipeTouchListener = new SwipeTouchListener(context) {
            public void onSwipeBottom() {
                if (browserPref.swipeToReload()) {
                    ninjaWebView.reload();
                }
            }

            public void onSwipeTop() {
            }
        };

        ninjaWebView.setOnTouchListener(swipeTouchListener);
        ninjaWebView.setOnScrollChangeListener((scrollY, oldScrollY) -> {
            if (scrollY == 0) {
                ninjaWebView.setOnTouchListener(swipeTouchListener);
            } else {
                ninjaWebView.setOnTouchListener(null);
            }
        });

        if (url.isEmpty()) {
            ninjaWebView.loadUrl("about:blank");
        } else {
            ninjaWebView.loadUrl(url);
        }

        if (currentAlbumController != null) {
            ninjaWebView.setPredecessor(currentAlbumController);  //save currentAlbumController and use when TAB is closed via Back button
            int index = BrowserContainer.indexOf(currentAlbumController) + 1;
            BrowserContainer.add(ninjaWebView, index);
        } else {
            BrowserContainer.add(ninjaWebView);
        }

        if (!foreground) {
            ninjaWebView.deactivate();
        } else {
            ninjaWebView.activate();
            showAlbum(ninjaWebView);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                ninjaWebView.reload();
            }
        }
        View albumView = ninjaWebView.getAlbumView();
        tab_container.addView(albumView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        updateOmniBox();
    }

    public synchronized void addAlbum(String title, String url,
                                      boolean foreground,
                                      boolean profileDialog, String profile) {

        //restoreProfile from shared preferences if app got killed
        if (!profile.equals("")) {
            browserPref.setProfile(profile);
        }


        if (profileDialog) {
            GridItem itemTrusted = new GridItem(R.drawable.libbrs_icon_profile_trusted, getString(R.string.libbrs_setting_title_profiles_trusted), 11);
            GridItem itemStandard = new GridItem(R.drawable.libbrs_icon_profile_standard, getString(R.string.libbrs_setting_title_profiles_standard), 11);
            GridItem itemProtected = new GridItem(R.drawable.libbrs_icon_profile_protected, getString(R.string.libbrs_setting_title_profiles_protected), 11);

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            View dialogView = View.inflate(context, R.layout.libbrs_dialog_menu, null);
            builder.setView(dialogView);
            AlertDialog dialog = builder.create();
            FaviconHelper.setFavicon(context, dialogView, url, R.id.menu_icon, R.drawable.libbrs_icon_link);
            TextView dialog_title = dialogView.findViewById(R.id.menuTitle);
            dialog_title.setText(url);
            dialog.show();

            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
            GridView menu_grid = dialogView.findViewById(R.id.menu_grid);
            List<GridItem> gridList = Arrays.asList(itemTrusted, itemStandard, itemProtected);
            GridAdapter gridAdapter = new GridAdapter(context, gridList);
            menu_grid.setAdapter(gridAdapter);
            gridAdapter.notifyDataSetChanged();
            menu_grid.setOnItemClickListener((parent, view, position, id) -> {
                switch (position) {
                    case 0:
                        browserPref.setProfile(BrowserPref.Profile.VAL_PROFILE_TRUSTED);
                        break;
                    case 1:
                        browserPref.setProfile(BrowserPref.Profile.VAL_PROFILE_STANDARD);
                        break;
                    case 2:
                        browserPref.setProfile(BrowserPref.Profile.VAL_PROFILE_PROTECTED);
                        break;
                }
                dialog.cancel();
                setWebView(title, url, foreground);
            });
        } else {
            setWebView(title, url, foreground);
        }
    }

    private void closeTabConfirmation(final Runnable okAction) {
        if (!browserPref.confirmCloseTab()) {
            okAction.run();
        } else {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setTitle(R.string.libbrs_menu_closeTab);
            builder.setIcon(R.drawable.libbrs_icon_alert);
            builder.setMessage(R.string.libbrs_toast_quit_TAB);
            builder.setPositiveButton(android.R.string.ok, (dialog, whichButton) -> okAction.run());
            builder.setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            HelperUnit.setupDialog(context, dialog);
        }
    }

    @Override
    public synchronized void removeAlbum(AlbumController controller) {
        if (BrowserContainer.size() <= 1) {
            finish();
        } else {
            closeTabConfirmation(() -> {
                AlbumController predecessor;
                if (controller == currentAlbumController) {
                    predecessor = ((NinjaWebView) controller).getPredecessor();
                } else {
                    predecessor = currentAlbumController;  //if not the current TAB is being closed return to current TAB
                }
                tab_container.removeView(controller.getAlbumView());
                int index = BrowserContainer.indexOf(controller);
                BrowserContainer.remove(controller);
                if ((predecessor != null) && (BrowserContainer.indexOf(predecessor) != -1)) { //if predecessor is stored and has not been closed in the meantime
                    showAlbum(predecessor);
                } else {
                    if (index >= BrowserContainer.size()) {
                        index = BrowserContainer.size() - 1;
                    }
                    showAlbum(BrowserContainer.get(index));
                }
            });
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void updateOmniBox() {

        badgeDrawable.setNumber(BrowserContainer.size());
        BadgeUtils.attachBadgeDrawable(badgeDrawable, omniBox_overview, findViewById(R.id.layout));
        omniBox_text.clearFocus();
        ninjaWebView = (NinjaWebView) currentAlbumController;
        String url = ninjaWebView.getUrl();

        if (url != null) {
            omniBox_flavor.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            ninjaWebView.setProfileIcon(omniBox_flavor);
            ninjaWebView.initCookieManager(url);

            if (Objects.requireNonNull(ninjaWebView.getTitle()).isEmpty()) {
                omniBox_text.setText(url);
            } else {
                omniBox_text.setText(ninjaWebView.getTitle());
            }
            if (url.isEmpty()) {
                omniBox_text.setText("");
            }
            omniBox_flavor.setOnClickListener(v -> showFastToggleDialog());
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!orientationChanged) {
            saveOpenedTabs();
            HelperUnit.triggerRebirth(this);
        } else {
            orientationChanged = false;
        }
    }


    @Override
    public synchronized void updateProgress(int progress) {
        progressBar.setProgressCompat(progress, true);
        if (progress != BrowserUnit.LOADING_STOPPED) updateOmniBox();
        if (progress < BrowserUnit.PROGRESS_MAX) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void showFileChooser(ValueCallback<Uri[]> filePathCallback) {
        if (mFilePathCallback != null) {
            mFilePathCallback.onReceiveValue(null);
        }
        mFilePathCallback = filePathCallback;
        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("*/*");
        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        //noinspection deprecation
        startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);
    }

    @Override
    public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
        if (view == null) {
            return;
        }
        if (customView != null && callback != null) {
            callback.onCustomViewHidden();
            return;
        }

        customView = view;
        fullscreenHolder = new FrameLayout(context);
        fullscreenHolder.addView(
                customView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                ));

        FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
        decorView.addView(
                fullscreenHolder,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                ));

        customView.setKeepScreenOn(true);
        ((View) currentAlbumController).setVisibility(View.GONE);
        setCustomFullscreen(true);

        if (view instanceof FrameLayout) {
            if (((FrameLayout) view).getFocusedChild() instanceof VideoView) {
                videoView = (VideoView) ((FrameLayout) view).getFocusedChild();
                videoView.setOnErrorListener(new VideoCompletionListener());
                videoView.setOnCompletionListener(new VideoCompletionListener());
            }
        }
    }

    @Override
    public void onHideCustomView() {
        FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
        decorView.removeView(fullscreenHolder);

        customView.setKeepScreenOn(false);
        ((View) currentAlbumController).setVisibility(View.VISIBLE);
        setCustomFullscreen(false);

        fullscreenHolder = null;
        customView = null;
        if (videoView != null) {
            videoView.setOnErrorListener(null);
            videoView.setOnCompletionListener(null);
            videoView = null;
        }
        contentFrame.requestFocus();
    }

    private void showContextMenuLink(final String title, final String url, int type) {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = View.inflate(context, R.layout.libbrs_dialog_menu, null);

        TextView menuTitle = dialogView.findViewById(R.id.menuTitle);
        menuTitle.setText(url);
        ImageView menu_icon = dialogView.findViewById(R.id.menu_icon);

        if (type == SRC_ANCHOR_TYPE) {
            FaviconHelper faviconHelper = new FaviconHelper(context);
            Bitmap bitmap = faviconHelper.getFavicon(url);
            if (bitmap != null) {
                menu_icon.setImageBitmap(bitmap);
            } else {
                menu_icon.setImageResource(R.drawable.libbrs_icon_link);
            }
        } else if (type == IMAGE_TYPE) {
            menu_icon.setImageResource(R.drawable.libbrs_icon_image_favicon);
        } else {
            menu_icon.setImageResource(R.drawable.libbrs_icon_link);
        }

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();
        Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);

        GridItem item_01 = new GridItem(0, getString(R.string.libbrs_main_menu_new_tabOpen), 0);
        GridItem item_02 = new GridItem(0, getString(R.string.libbrs_main_menu_new_tab), 0);
        GridItem item_03 = new GridItem(0, getString(R.string.libbrs_main_menu_new_tabProfile), 0);
        GridItem item_04 = new GridItem(0, getString(R.string.libbrs_menu_share_link), 0);
        GridItem item_05 = new GridItem(0, getString(R.string.libbrs_menu_open_with), 0);
        GridItem item_06 = new GridItem(0, getString(R.string.libbrs_menu_save_as), 0);
        GridItem item_07 = new GridItem(0, getString(R.string.libbrs_menu_save_home), 0);

        final List<GridItem> gridList = new LinkedList<>();

        gridList.add(gridList.size(), item_01);
        gridList.add(gridList.size(), item_02);
        gridList.add(gridList.size(), item_03);
        gridList.add(gridList.size(), item_04);
        gridList.add(gridList.size(), item_05);
        gridList.add(gridList.size(), item_06);
        gridList.add(gridList.size(), item_07);

        GridView menu_grid = dialogView.findViewById(R.id.menu_grid);
        GridAdapter gridAdapter = new GridAdapter(context, gridList);
        menu_grid.setAdapter(gridAdapter);
        gridAdapter.notifyDataSetChanged();
        menu_grid.setOnItemClickListener((parent, view, position, id) -> {
            dialog.cancel();
            switch (position) {
                case 0:
                    addAlbum(getString(R.string.libbrs_app_name), url, true, false, "");
                    break;
                case 1:
                    addAlbum(getString(R.string.libbrs_app_name), url, false, false, "");
                    break;
                case 2:
                    addAlbum(getString(R.string.libbrs_app_name), url, true, true, "");
                    break;
                case 3:
                    HelperUnit.shareLink(this, "", url);
                    break;
                case 4:
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    Intent chooser = Intent.createChooser(intent, getString(R.string.libbrs_menu_open_with));
                    startActivity(chooser);
                    break;
                case 5:
                    if (url.startsWith("data:")) {
                        DataURIParser dataURIParser = new DataURIParser(url);
                        HelperUnit.saveDataURI(dialog, activity, dataURIParser);
                    } else {
                        HelperUnit.saveAs(dialog, activity, url);
                    }
                    break;
                case 6:
                    saveAtHome(title, url);
                    break;
            }
        });
    }

    public void searchOnSite() {
        searchOnSite = true;
        searchPanel.setVisibility(View.VISIBLE);
        HelperUnit.showSoftKeyboard(searchBox, activity);
    }

    private void stopSearchOnSite() {
        searchOnSite = false;
        searchBox.setText("");
        searchPanel.setVisibility(View.GONE);
        HelperUnit.hideSoftKeyboard(searchBox, context);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        WebView.HitTestResult result = ninjaWebView.getHitTestResult();
        if (result.getExtra() != null) {
            if (result.getType() == SRC_ANCHOR_TYPE) {
                showContextMenuLink(HelperUnit.domain(result.getExtra()), result.getExtra(), SRC_ANCHOR_TYPE);
            } else if (result.getType() == SRC_IMAGE_ANCHOR_TYPE) {
                // Create a background thread that has a Looper
                HandlerThread handlerThread = new HandlerThread("HandlerThread");
                handlerThread.start();
                // Create a handler to execute tasks in the background thread.
                Handler backgroundHandler = new Handler(handlerThread.getLooper());
                Message msg = backgroundHandler.obtainMessage();
                ninjaWebView.requestFocusNodeHref(msg);
                String url = (String) msg.getData().get("url");
                showContextMenuLink(HelperUnit.domain(url), url, SRC_ANCHOR_TYPE);
            } else if (result.getType() == IMAGE_TYPE) {
                showContextMenuLink(HelperUnit.domain(result.getExtra()), result.getExtra(), IMAGE_TYPE);
            } else {
                showContextMenuLink(HelperUnit.domain(result.getExtra()), result.getExtra(), 0);
            }
        }
    }

    private void showOverflow() {
        HelperUnit.hideSoftKeyboard(omniBox_text, context);
        new OverflowDialog(this, ninjaWebView).show();
    }

    public void saveOpenedTabs() {
        ArrayList<String> openTabs = new ArrayList<>();
        for (int i = 0; i < BrowserContainer.size(); i++) {
            if (currentAlbumController == BrowserContainer.get(i)) {
                openTabs.add(0, ((NinjaWebView) (BrowserContainer.get(i))).getUrl());
            } else {
                openTabs.add(((NinjaWebView) (BrowserContainer.get(i))).getUrl());
            }
        }
        browserPref.setOpenTabs(openTabs);

        //Save profile of open Tabs in shared preferences
        ArrayList<String> openTabsProfile = new ArrayList<>();
        for (int i = 0; i < BrowserContainer.size(); i++) {
            if (currentAlbumController == BrowserContainer.get(i)) {
                openTabsProfile.add(0, ((NinjaWebView) (BrowserContainer.get(i))).getProfile());
            } else {
                openTabsProfile.add(((NinjaWebView) (BrowserContainer.get(i))).getProfile());
            }
        }
        browserPref.setOpenTabsProfile(openTabsProfile);
    }

    public void saveAtHome(String title, String url) {

        FaviconHelper faviconHelper = new FaviconHelper(context);
        faviconHelper.addFavicon(ninjaWebView.getUrl(), ninjaWebView.getFavicon());

        RecordAction action = new RecordAction(context);
        action.open(true);
        if (action.checkUrl(url, RecordUnit.TABLE_START)) {
            NinjaToast.show(this, R.string.libbrs_app_error);
        } else {
            int counter = browserPref.getAndIncreaseCounter();
            if (action.addStartSite(new Record(title, url, 0, counter, 1, ninjaWebView.isDesktopMode(), ninjaWebView.isNightMode(), 0))) {
                NinjaToast.show(this, R.string.libbrs_app_done);
            } else {
                NinjaToast.show(this, R.string.libbrs_app_error);
            }
        }
        action.close();
    }

    private void setCustomFullscreen(boolean fullscreen) {
        if (fullscreen) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                final WindowInsetsController insetsController = getWindow().getInsetsController();
                if (insetsController != null) {
                    insetsController.hide(WindowInsets.Type.statusBars());
                    insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                getWindow().setFlags(
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN
                );
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                final WindowInsetsController insetsController = getWindow().getInsetsController();
                if (insetsController != null) {
                    insetsController.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                getWindow().setFlags(
                        WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN
                );
            }
        }
    }

    public AlbumController getCurrentAlbumController() {
        return currentAlbumController;
    }

    private AlbumController nextAlbumController(boolean next) {
        if (BrowserContainer.size() <= 1) {
            return currentAlbumController;
        }
        List<AlbumController> list = BrowserContainer.list();
        int index = list.indexOf(currentAlbumController);
        if (next) {
            index++;
            if (index >= list.size()) {
                index = 0;
            }
        } else {
            index--;
            if (index < 0) {
                index = list.size() - 1;
            }
        }
        return list.get(index);
    }

    public void goBackSkipRedirects() {
        if (ninjaWebView.canGoBack()) {
            ninjaWebView.setIsBackPressed(true);
            ninjaWebView.goBack();
        }
    }
}