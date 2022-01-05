package com.liuzho.browser.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
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
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
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
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;
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
import com.liuzho.browser.dialog.FastToggleDialog;

public class BrowserActivity extends AppCompatActivity implements BrowserController {

    // Menus

    private RecordAdapter adapter;
    private ImageButton omniBox_overview;
    private AutoCompleteTextView omniBox_text;

    // Views

    private EditText searchBox;
    private BottomSheetDialog bottomSheetDialog_OverView;
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
    private long newIcon;
    private boolean filter;
    private boolean isNightMode;
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
                    goBack_skipRedirects();
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
                        if (record.getDesktopMode() != ninjaWebView.isDesktopMode())
                            ninjaWebView.toggleDesktopMode(false);
                        if (record.getNightMode() == ninjaWebView.isNightMode() && !isNightMode) {
                            ninjaWebView.toggleNightMode();
                            isNightMode = ninjaWebView.isNightMode();
                        }
                        break;
                    }
                }
            }
            ninjaWebView.loadUrl(url);
        });
    }

//    private void showOverview() {
//        initOverview();
//        bottomSheetDialog_OverView.show();
//    }

    private void showBookmarkOverview() {
        initOverview(0);
        bottomSheetDialog_OverView.show();
    }

    private void showHistoryOverview() {
        initOverview(1);
        bottomSheetDialog_OverView.show();
    }

    private void showHomePageOverview() {
        initOverview(2);
        bottomSheetDialog_OverView.show();
    }

    public void hideOverview() {
        if (bottomSheetDialog_OverView != null) {
            bottomSheetDialog_OverView.cancel();
        }
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

    private void printPDF() {
        String title = HelperUnit.fileName(ninjaWebView.getUrl());
        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        PrintDocumentAdapter printAdapter = ninjaWebView.createPrintDocumentAdapter(title);
        Objects.requireNonNull(printManager).print(title, printAdapter, new PrintAttributes.Builder().build());
        browserPref.setPdfCreate(true);
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

    @SuppressLint("ClickableViewAccessibility")
    private void initOverview(int type) {
        bottomSheetDialog_OverView = new BottomSheetDialog(context);
        View dialogView = View.inflate(context, R.layout.libbrs_dialog_overview, null);
        TextView title = dialogView.findViewById(R.id.overview_title);
        ImageButton close = dialogView.findViewById(R.id.overview_close);

        close.setOnClickListener(view -> hideOverview());

        ListView listView = dialogView.findViewById(R.id.list_overView);
        // allow scrolling in listView without closing the bottomSheetDialog
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

        bottomSheetDialog_OverView.setContentView(dialogView);
        RecordAction action = new RecordAction(context);
        action.open(false);
        final List<Record> list;
        switch (type) {
            case 0:
                overViewTab = getString(R.string.libbrs_album_title_bookmarks);
                list = action.listBookmark(activity, filter, 0);
                action.close();

                adapter = new RecordAdapter(context, list) {
                    @NonNull
                    @Override
                    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                        View v = super.getView(position, convertView, parent);
                        ImageView record_item_icon = v.findViewById(R.id.record_item_icon);
                        record_item_icon.setVisibility(View.VISIBLE);
                        return v;
                    }
                };

                listView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                filter = false;
                listView.setOnItemClickListener((parent, view, position, id) -> {
                    if (list.get(position).getDesktopMode() != ninjaWebView.isDesktopMode())
                        ninjaWebView.toggleDesktopMode(false);
                    if (list.get(position).getNightMode() == ninjaWebView.isNightMode() && !isNightMode) {
                        ninjaWebView.toggleNightMode();
                        isNightMode = ninjaWebView.isNightMode();
                    }
                    ninjaWebView.loadUrl(list.get(position).getURL());
                    hideOverview();
                });
                listView.setOnItemLongClickListener((parent, view, position, id) -> {
                    showContextMenuList(list.get(position).getTitle(), list.get(position).getURL(), adapter, list, position);
                    return true;
                });
                break;
            case 1:
                overViewTab = getString(R.string.libbrs_album_title_history);


                list = action.listHistory();
                action.close();

                //noinspection NullableProblems
                adapter = new RecordAdapter(context, list) {
                    @Override
                    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                        View v = super.getView(position, convertView, parent);
                        TextView record_item_time = v.findViewById(R.id.record_item_time);
                        record_item_time.setVisibility(View.VISIBLE);
                        return v;
                    }
                };

                listView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                listView.setOnItemClickListener((parent, view, position, id) -> {
                    if (list.get(position).getDesktopMode() != ninjaWebView.isDesktopMode())
                        ninjaWebView.toggleDesktopMode(false);
                    if (list.get(position).getNightMode() == ninjaWebView.isNightMode() && !isNightMode) {
                        ninjaWebView.toggleNightMode();
                        isNightMode = ninjaWebView.isNightMode();
                    }
                    ninjaWebView.loadUrl(list.get(position).getURL());
                    hideOverview();
                });

                listView.setOnItemLongClickListener((parent, view, position, id) -> {
                    showContextMenuList(list.get(position).getTitle(), list.get(position).getURL(), adapter, list, position);
                    return true;
                });
                break;
            case 2:
                omniBox_overview.setImageResource(R.drawable.libbrs_icon_web);
                overViewTab = getString(R.string.libbrs_album_title_home);

                list = action.listStartSite();
                action.close();

                adapter = new RecordAdapter(context, list);
                listView.setAdapter(adapter);
                adapter.notifyDataSetChanged();

                listView.setOnItemClickListener((parent, view, position, id) -> {
                    if (list.get(position).getDesktopMode() != ninjaWebView.isDesktopMode())
                        ninjaWebView.toggleDesktopMode(false);
                    if (list.get(position).getNightMode() == ninjaWebView.isNightMode() && !isNightMode) {
                        ninjaWebView.toggleNightMode();
                        isNightMode = ninjaWebView.isNightMode();
                    }
                    ninjaWebView.loadUrl(list.get(position).getURL());
                    hideOverview();
                });

                listView.setOnItemLongClickListener((parent, view, position, id) -> {
                    showContextMenuList(list.get(position).getTitle(), list.get(position).getURL(), adapter, list, position);
                    return true;
                });
                break;
        }
        title.setText(overViewTab);
        BottomSheetBehavior<View> mBehavior = BottomSheetBehavior.from((View) dialogView.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        mBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN || newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    hideOverview();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });
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
                isNightMode = isNight;
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setWebView(String title, final String url, final boolean foreground) {
        ninjaWebView = new NinjaWebView(context);

        if (isNightMode) {
            ninjaWebView.toggleNightMode();
            isNightMode = ninjaWebView.isNightMode();
        }
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

    public synchronized void addAlbum(String title, final String url, final boolean foreground, final boolean profileDialog, String profile) {

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
    public synchronized void removeAlbum(final AlbumController controller) {
        if (BrowserContainer.size() <= 1) {
            doubleTapsQuit();
        } else {
            closeTabConfirmation(() -> {
                AlbumController predecessor;
                if (controller == currentAlbumController) {
                    predecessor = ((NinjaWebView) controller).getPredecessor();
                } else
                    predecessor = currentAlbumController;  //if not the current TAB is being closed return to current TAB
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
    public void onConfigurationChanged(Configuration newConfig) {
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
                    shareLink("", url);
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
                    } else HelperUnit.saveAs(dialog, activity, url);
                    break;
                case 6:
                    save_atHome(title, url);
                    break;
            }
        });
    }

    private void shareLink(String title, String url) {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        sharingIntent.putExtra(Intent.EXTRA_TEXT, url);
        context.startActivity(Intent.createChooser(sharingIntent, (context.getString(R.string.libbrs_menu_share_link))));
    }

    private void searchOnSite() {
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

    private void saveBookmark() {
        FaviconHelper faviconHelper = new FaviconHelper(context);
        faviconHelper.addFavicon(ninjaWebView.getUrl(), ninjaWebView.getFavicon());
        RecordAction action = new RecordAction(context);
        action.open(true);
        if (action.checkUrl(ninjaWebView.getUrl(), RecordUnit.TABLE_BOOKMARK)) {
            NinjaToast.show(this, R.string.libbrs_app_error);
        } else {
            long value = 11;  //default red icon
            action.addBookmark(new Record(ninjaWebView.getTitle(), ninjaWebView.getUrl(), 0, 0, 2, ninjaWebView.isDesktopMode(), ninjaWebView.isNightMode(), value));
            NinjaToast.show(this, R.string.libbrs_app_done);
        }
        action.close();
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

    private void doubleTapsQuit() {
        finish();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showOverflow() {

        HelperUnit.hideSoftKeyboard(omniBox_text, context);

        String url = ninjaWebView.getUrl();
        String title = ninjaWebView.getTitle();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = View.inflate(context, R.layout.libbrs_dialog_menu_overflow, null);

        builder.setView(dialogView);
        AlertDialog dialog_overflow = builder.create();
        dialog_overflow.show();
        Objects.requireNonNull(dialog_overflow.getWindow()).setGravity(Gravity.TOP);
        FaviconHelper.setFavicon(context, dialogView, url, R.id.menu_icon, R.drawable.libbrs_icon_image_broken);

        TextView overflow_title = dialogView.findViewById(R.id.overflow_title);
        assert title != null;
        if (title.isEmpty()) {
            overflow_title.setText(url);
        } else {
            overflow_title.setText(title);
        }


        final GridView menu_grid_tab = dialogView.findViewById(R.id.overflow_tab);
        final GridView menu_grid_share = dialogView.findViewById(R.id.overflow_share);
        final GridView menu_grid_save = dialogView.findViewById(R.id.overflow_save);
        final GridView menu_grid_other = dialogView.findViewById(R.id.overflow_other);
        final GridView menuGridRecord = dialogView.findViewById(R.id.overflow_record);

        menu_grid_tab.setVisibility(View.VISIBLE);
        menuGridRecord.setVisibility(View.GONE);
        menu_grid_share.setVisibility(View.GONE);
        menu_grid_save.setVisibility(View.GONE);
        menu_grid_other.setVisibility(View.GONE);

        // Tab

        GridItem item_01 = new GridItem(0, getString(R.string.libbrs_menu_openFav), 0);
        GridItem item_02 = new GridItem(0, getString(R.string.libbrs_main_menu_new_tabOpen), 0);
        GridItem item_03 = new GridItem(0, getString(R.string.libbrs_main_menu_new_tabProfile), 0);
        GridItem item_04 = new GridItem(0, getString(R.string.libbrs_menu_reload), 0);
        GridItem item_05 = new GridItem(0, getString(R.string.libbrs_menu_closeTab), 0);
        GridItem item_06 = new GridItem(0, getString(R.string.libbrs_menu_quit), 0);

        final List<GridItem> gridList_tab = new LinkedList<>();

        gridList_tab.add(gridList_tab.size(), item_01);
        gridList_tab.add(gridList_tab.size(), item_02);
        gridList_tab.add(gridList_tab.size(), item_03);
        gridList_tab.add(gridList_tab.size(), item_04);
        gridList_tab.add(gridList_tab.size(), item_05);
        gridList_tab.add(gridList_tab.size(), item_06);

        GridAdapter gridAdapter_tab = new GridAdapter(context, gridList_tab);
        menu_grid_tab.setAdapter(gridAdapter_tab);
        gridAdapter_tab.notifyDataSetChanged();

        menu_grid_tab.setOnItemClickListener((parent, view14, position, id) -> {
            dialog_overflow.cancel();
            if (position == 0) {
                ninjaWebView.loadUrl(BrowserPref.getInstance().getFavoriteUrl());
            } else if (position == 1) {
                addAlbum(getString(R.string.libbrs_app_name), BrowserPref.getInstance().getFavoriteUrl(), true, false, "");
            } else if (position == 2) {
                addAlbum(getString(R.string.libbrs_app_name), BrowserPref.getInstance().getFavoriteUrl(), true, true, "");
            } else if (position == 3) {
                ninjaWebView.reload();
            } else if (position == 4) {
                removeAlbum(currentAlbumController);
            } else if (position == 5) {
                doubleTapsQuit();
            }
        });

        // Bookmark
        GridItem item_bm_1 = new GridItem(0, getString(R.string.libbrs_album_title_bookmarks), 0);
        GridItem item_bm_2 = new GridItem(0, getString(R.string.libbrs_album_title_history), 0);
        GridItem item_bm_3 = new GridItem(0, getString(R.string.libbrs_album_title_home), 0);
        final List<GridItem> gridListBookMark = new LinkedList<>();
        gridListBookMark.add(item_bm_1);
        gridListBookMark.add(item_bm_2);
        gridListBookMark.add(item_bm_3);
        GridAdapter gridAdapterBookMark = new GridAdapter(context, gridListBookMark);
        menuGridRecord.setAdapter(gridAdapterBookMark);
        gridAdapterBookMark.notifyDataSetChanged();
        menuGridRecord.setOnItemClickListener((adapterView, view, position, l) -> {
            switch (position) {
                case 0:
                    showBookmarkOverview();
                    break;
                case 1:
                    showHistoryOverview();
                    break;
                case 2:
                    showHomePageOverview();
                    break;
                default:
                    break;
            }
            dialog_overflow.cancel();
        });

        // Save
        GridItem item_21 = new GridItem(0, getString(R.string.libbrs_menu_fav), 0);
        GridItem item_22 = new GridItem(0, getString(R.string.libbrs_menu_save_home), 0);
        GridItem item_23 = new GridItem(0, getString(R.string.libbrs_menu_save_bookmark), 0);
        GridItem item_24 = new GridItem(0, getString(R.string.libbrs_menu_save_pdf), 0);
        GridItem item_25 = new GridItem(0, getString(R.string.libbrs_menu_sc), 0);
        GridItem item_26 = new GridItem(0, getString(R.string.libbrs_menu_save_as), 0);

        final List<GridItem> gridList_save = new LinkedList<>();
        gridList_save.add(gridList_save.size(), item_21);
        gridList_save.add(gridList_save.size(), item_22);
        gridList_save.add(gridList_save.size(), item_23);
        gridList_save.add(gridList_save.size(), item_24);
        gridList_save.add(gridList_save.size(), item_25);
        gridList_save.add(gridList_save.size(), item_26);

        GridAdapter gridAdapter_save = new GridAdapter(context, gridList_save);
        menu_grid_save.setAdapter(gridAdapter_save);
        gridAdapter_save.notifyDataSetChanged();

        menu_grid_save.setOnItemClickListener((parent, view13, position, id) -> {
            dialog_overflow.cancel();
            RecordAction action = new RecordAction(context);
            if (position == 0) {
                BrowserPref.getInstance().setFavoriteUrl(url);
                NinjaToast.show(this, R.string.libbrs_app_done);
            } else if (position == 1) {
                save_atHome(title, url);
            } else if (position == 2) {
                saveBookmark();
                action.close();
            } else if (position == 3) {
                printPDF();
            } else if (position == 4) {
                HelperUnit.createShortcut(context, ninjaWebView.getTitle(), ninjaWebView.getUrl());
            } else if (position == 5) {
                HelperUnit.saveAs(dialog_overflow, activity, url);
            }
        });

        // Share
        GridItem item_11 = new GridItem(0, getString(R.string.libbrs_menu_share_link), 0);
        GridItem item_12 = new GridItem(0, getString(R.string.libbrs_menu_shareClipboard), 0);
        GridItem item_13 = new GridItem(0, getString(R.string.libbrs_menu_open_with), 0);

        final List<GridItem> gridList_share = new LinkedList<>();
        gridList_share.add(gridList_share.size(), item_11);
        gridList_share.add(gridList_share.size(), item_12);
        gridList_share.add(gridList_share.size(), item_13);

        GridAdapter gridAdapter_share = new GridAdapter(context, gridList_share);
        menu_grid_share.setAdapter(gridAdapter_share);
        gridAdapter_share.notifyDataSetChanged();

        menu_grid_share.setOnItemClickListener((parent, view12, position, id) -> {
            dialog_overflow.cancel();
            if (position == 0) {
                shareLink(title, url);
            } else if (position == 1) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("text", url);
                Objects.requireNonNull(clipboard).setPrimaryClip(clip);
                NinjaToast.show(this, R.string.libbrs_toast_copy_successful);
            } else if (position == 2) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                Intent chooser = Intent.createChooser(intent, getString(R.string.libbrs_menu_open_with));
                startActivity(chooser);
            }
        });

        // Other
        GridItem item_31 = new GridItem(0, getString(R.string.libbrs_menu_other_searchSite), 0);
        GridItem item_32 = new GridItem(0, getString(R.string.libbrs_menu_download), 0);
        GridItem item_33 = new GridItem(0, getString(R.string.libbrs_setting_label), 0);
        GridItem item_36 = new GridItem(0, getString(R.string.libbrs_menu_restart), 0);
        GridItem item_34;
        if (ninjaWebView.isDesktopMode()) {
            item_34 = new GridItem(0, getString((R.string.libbrs_menu_mobileView)), 0);
        } else {
            item_34 = new GridItem(0, getString((R.string.libbrs_menu_desktopView)), 0);
        }

        GridItem item_35;
        if (ninjaWebView.isNightMode())
            item_35 = new GridItem(0, getString((R.string.libbrs_menu_dayView)), 0);
        else item_35 = new GridItem(0, getString((R.string.libbrs_menu_nightView)), 0);

        final List<GridItem> gridList_other = new LinkedList<>();
        gridList_other.add(gridList_other.size(), item_31);
        gridList_other.add(gridList_other.size(), item_34);
        gridList_other.add(gridList_other.size(), item_35);
        gridList_other.add(gridList_other.size(), item_32);
        gridList_other.add(gridList_other.size(), item_33);
        gridList_other.add(gridList_other.size(), item_36);

        GridAdapter gridAdapter_other = new GridAdapter(context, gridList_other);
        menu_grid_other.setAdapter(gridAdapter_other);
        gridAdapter_other.notifyDataSetChanged();

        menu_grid_other.setOnItemClickListener((parent, view1, position, id) -> {
            dialog_overflow.cancel();
            if (position == 0) {
                searchOnSite();
            } else if (position == 1) {
                ninjaWebView.toggleDesktopMode(true);
            } else if (position == 2) {
                ninjaWebView.toggleNightMode();
                isNightMode = ninjaWebView.isNightMode();
            } else if (position == 3) {
                startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
            } else if (position == 4) {
                Intent settings = new Intent(BrowserActivity.this, SettingsActivity.class);
                startActivity(settings);
            } else if (position == 5) {
                saveOpenedTabs();
                HelperUnit.triggerRebirth(this);
            }
        });

        TabLayout tabLayout = dialogView.findViewById(R.id.tabLayout);

        TabLayout.Tab tab_tab = tabLayout.newTab().setIcon(R.drawable.libbrs_icon_tab);
        TabLayout.Tab tab_share = tabLayout.newTab().setIcon(R.drawable.libbrs_icon_menu_share);
        TabLayout.Tab tab_save = tabLayout.newTab().setIcon(R.drawable.libbrs_icon_menu_save);
        TabLayout.Tab tab_other = tabLayout.newTab().setIcon(R.drawable.libbrs_icon_dots);
        TabLayout.Tab tabRecord = tabLayout.newTab().setIcon(R.drawable.libbrs_icon_web);

        tabLayout.addTab(tab_tab);
        tabLayout.addTab(tabRecord);
        tabLayout.addTab(tab_share);
        tabLayout.addTab(tab_save);
        tabLayout.addTab(tab_other);

        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            private final View[] tabs = new View[]{menu_grid_tab, menuGridRecord, menu_grid_share, menu_grid_save, menu_grid_other};

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                for (int i = 0; i < tabs.length; i++) {
                    if (tab.getPosition() == i) {
                        tabs[i].setVisibility(View.VISIBLE);
                    } else {
                        tabs[i].setVisibility(View.GONE);
                    }
                }
//                if (tab.getPosition() == 0) {
//                    menu_grid_tab.setVisibility(View.VISIBLE);
//                    menu_grid_share.setVisibility(View.GONE);
//                    menu_grid_save.setVisibility(View.GONE);
//                    menu_grid_other.setVisibility(View.GONE);
//                } else if (tab.getPosition() == 1) {
//                    menu_grid_tab.setVisibility(View.GONE);
//                    menu_grid_share.setVisibility(View.VISIBLE);
//                    menu_grid_save.setVisibility(View.GONE);
//                    menu_grid_other.setVisibility(View.GONE);
//                } else if (tab.getPosition() == 2) {
//                    menu_grid_tab.setVisibility(View.GONE);
//                    menu_grid_share.setVisibility(View.GONE);
//                    menu_grid_save.setVisibility(View.VISIBLE);
//                    menu_grid_other.setVisibility(View.GONE);
//                } else if (tab.getPosition() == 3) {
//                    menu_grid_tab.setVisibility(View.GONE);
//                    menu_grid_share.setVisibility(View.GONE);
//                    menu_grid_save.setVisibility(View.GONE);
//                    menu_grid_other.setVisibility(View.VISIBLE);
//                } else if (tab.getPosition() == 4) {
//
//                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void saveOpenedTabs() {
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

    private void showContextMenuList(final String title, final String url,
                                     final RecordAdapter adapterRecord, final List<Record> recordList, final int location) {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = View.inflate(context, R.layout.libbrs_dialog_menu, null);

        TextView menuTitle = dialogView.findViewById(R.id.menuTitle);
        menuTitle.setText(title);
        FaviconHelper.setFavicon(context, dialogView, url, R.id.menu_icon, R.drawable.libbrs_icon_image_broken);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();
        Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);

        GridItem item_01 = new GridItem(0, getString(R.string.libbrs_main_menu_new_tabOpen), 0);
        GridItem item_02 = new GridItem(0, getString(R.string.libbrs_main_menu_new_tab), 0);
        GridItem item_03 = new GridItem(0, getString(R.string.libbrs_main_menu_new_tabProfile), 0);
        GridItem item_04 = new GridItem(0, getString(R.string.libbrs_menu_share_link), 0);
        GridItem item_05 = new GridItem(0, getString(R.string.libbrs_menu_delete), 0);
        GridItem item_06 = new GridItem(0, getString(R.string.libbrs_menu_edit), 0);

        final List<GridItem> gridList = new LinkedList<>();

        if (overViewTab.equals(getString(R.string.libbrs_album_title_bookmarks)) || overViewTab.equals(getString(R.string.libbrs_album_title_home))) {
            gridList.add(gridList.size(), item_01);
            gridList.add(gridList.size(), item_02);
            gridList.add(gridList.size(), item_03);
            gridList.add(gridList.size(), item_04);
            gridList.add(gridList.size(), item_05);
            gridList.add(gridList.size(), item_06);
        } else {
            gridList.add(gridList.size(), item_01);
            gridList.add(gridList.size(), item_02);
            gridList.add(gridList.size(), item_03);
            gridList.add(gridList.size(), item_04);
            gridList.add(gridList.size(), item_05);
        }

        GridView menu_grid = dialogView.findViewById(R.id.menu_grid);
        GridAdapter gridAdapter = new GridAdapter(context, gridList);
        menu_grid.setAdapter(gridAdapter);
        gridAdapter.notifyDataSetChanged();
        menu_grid.setOnItemClickListener((parent, view, position, id) -> {
            dialog.cancel();
            MaterialAlertDialogBuilder builderSubMenu;
            AlertDialog dialogSubMenu;
            switch (position) {
                case 0:
                    addAlbum(getString(R.string.libbrs_app_name), url, true, false, "");
                    hideOverview();
                    break;
                case 1:
                    addAlbum(getString(R.string.libbrs_app_name), url, false, false, "");
                    break;
                case 2:
                    addAlbum(getString(R.string.libbrs_app_name), url, true, true, "");
                    hideOverview();
                    break;
                case 3:
                    shareLink("", url);
                    break;
                case 4:
                    builderSubMenu = new MaterialAlertDialogBuilder(context);
                    builderSubMenu.setIcon(R.drawable.libbrs_icon_alert);
                    builderSubMenu.setTitle(R.string.libbrs_menu_delete);
                    builderSubMenu.setMessage(R.string.libbrs_hint_database);
                    builderSubMenu.setPositiveButton(android.R.string.ok, (dialog2, whichButton) -> {
                        Record record = recordList.get(location);
                        RecordAction action = new RecordAction(context);
                        action.open(true);
                        if (overViewTab.equals(getString(R.string.libbrs_album_title_home))) {
                            action.deleteURL(record.getURL(), RecordUnit.TABLE_START);
                        } else if (overViewTab.equals(getString(R.string.libbrs_album_title_bookmarks))) {
                            action.deleteURL(record.getURL(), RecordUnit.TABLE_BOOKMARK);
                        } else if (overViewTab.equals(getString(R.string.libbrs_album_title_history))) {
                            action.deleteURL(record.getURL(), RecordUnit.TABLE_HISTORY);
                        }
                        action.close();
                        recordList.remove(location);
                        adapterRecord.notifyDataSetChanged();
                    });
                    builderSubMenu.setNegativeButton(android.R.string.cancel, (dialog2, whichButton) -> builderSubMenu.setCancelable(true));
                    dialogSubMenu = builderSubMenu.create();
                    dialogSubMenu.show();
                    HelperUnit.setupDialog(context, dialogSubMenu);
                    break;
                case 5:
                    builderSubMenu = new MaterialAlertDialogBuilder(context);
                    View dialogViewSubMenu = View.inflate(context, R.layout.libbrs_dialog_edit_title, null);

                    TextInputLayout edit_title_layout = dialogViewSubMenu.findViewById(R.id.edit_title_layout);
                    TextInputLayout edit_userName_layout = dialogViewSubMenu.findViewById(R.id.edit_userName_layout);
                    TextInputLayout edit_PW_layout = dialogViewSubMenu.findViewById(R.id.edit_PW_layout);
                    edit_title_layout.setVisibility(View.VISIBLE);
                    edit_userName_layout.setVisibility(View.GONE);
                    edit_PW_layout.setVisibility(View.GONE);

                    EditText edit_title = dialogViewSubMenu.findViewById(R.id.edit_title);
                    edit_title.setText(title);

                    TextInputLayout edit_URL_layout = dialogViewSubMenu.findViewById(R.id.edit_URL_layout);
                    edit_URL_layout.setVisibility(View.VISIBLE);
                    EditText edit_URL = dialogViewSubMenu.findViewById(R.id.edit_URL);
                    edit_URL.setVisibility(View.VISIBLE);
                    edit_URL.setText(url);

                    Chip chip_desktopMode = dialogViewSubMenu.findViewById(R.id.edit_bookmark_desktopMode);
                    chip_desktopMode.setChecked(recordList.get(location).getDesktopMode());
                    Chip chip_nightMode = dialogViewSubMenu.findViewById(R.id.edit_bookmark_nightMode);
                    chip_nightMode.setChecked(!recordList.get(location).getNightMode());

                    ImageView ib_icon = dialogViewSubMenu.findViewById(R.id.edit_icon);
                    if (!overViewTab.equals(getString(R.string.libbrs_album_title_bookmarks))) {
                        ib_icon.setVisibility(View.GONE);
                    }
                    ib_icon.setOnClickListener(v -> {
                        MaterialAlertDialogBuilder builderFilter = new MaterialAlertDialogBuilder(context);
                        View dialogViewFilter = View.inflate(context, R.layout.libbrs_dialog_menu, null);
                        builderFilter.setView(dialogViewFilter);
                        AlertDialog dialogFilter = builderFilter.create();
                        dialogFilter.show();
                        TextView menuTitleFilter = dialogViewFilter.findViewById(R.id.menuTitle);
                        menuTitleFilter.setText(R.string.libbrs_menu_filter);
                        CardView cardView = dialogViewFilter.findViewById(R.id.cardView);
                        cardView.setVisibility(View.GONE);
                        Objects.requireNonNull(dialogFilter.getWindow()).setGravity(Gravity.BOTTOM);
                        GridView menu_grid2 = dialogViewFilter.findViewById(R.id.menu_grid);
                        final List<GridItem> gridList2 = new LinkedList<>();
                        HelperUnit.addFilterItems(activity, gridList2);
                        GridAdapter gridAdapter2 = new GridAdapter(context, gridList2);
                        menu_grid2.setAdapter(gridAdapter2);
                        gridAdapter2.notifyDataSetChanged();
                        menu_grid2.setOnItemClickListener((parent2, view2, position2, id2) -> {
                            newIcon = gridList2.get(position2).getData();
                            HelperUnit.setFilterIcons(ib_icon, newIcon);
                            dialogFilter.cancel();
                        });
                    });
                    newIcon = recordList.get(location).getIconColor();
                    HelperUnit.setFilterIcons(ib_icon, newIcon);

                    builderSubMenu.setView(dialogViewSubMenu);
                    builderSubMenu.setTitle(getString(R.string.libbrs_menu_edit));
                    builderSubMenu.setIcon(R.drawable.libbrs_icon_alert);
                    builderSubMenu.setMessage(url);
                    builderSubMenu.setPositiveButton(android.R.string.ok, (dialog3, whichButton) -> {
                        if (overViewTab.equals(getString(R.string.libbrs_album_title_bookmarks))) {
                            RecordAction action = new RecordAction(context);
                            action.open(true);
                            action.deleteURL(url, RecordUnit.TABLE_BOOKMARK);
                            action.addBookmark(new Record(edit_title.getText().toString(), edit_URL.getText().toString(), 0, 0, BOOKMARK_ITEM, chip_desktopMode.isChecked(), chip_nightMode.isChecked(), newIcon));
                            action.close();
                        } else {
                            RecordAction action = new RecordAction(context);
                            action.open(true);
                            action.deleteURL(url, RecordUnit.TABLE_START);
                            int counter = browserPref.getAndIncreaseCounter();
                            action.addStartSite(new Record(edit_title.getText().toString(), edit_URL.getText().toString(), 0, counter, STARTSITE_ITEM, chip_desktopMode.isChecked(), chip_nightMode.isChecked(), 0));
                            action.close();
                        }
                    });
                    builderSubMenu.setNegativeButton(android.R.string.cancel, (dialog3, whichButton) -> builderSubMenu.setCancelable(true));
                    dialogSubMenu = builderSubMenu.create();
                    dialogSubMenu.show();
                    HelperUnit.setupDialog(context, dialogSubMenu);
                    break;
            }
        });
    }

    private void save_atHome(final String title, final String url) {

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

    public void goBack_skipRedirects() {
        if (ninjaWebView.canGoBack()) {
            ninjaWebView.setIsBackPressed(true);
            ninjaWebView.goBack();
        }
    }
}