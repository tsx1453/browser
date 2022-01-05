package com.liuzho.browser.view;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.google.android.material.chip.Chip;
import com.liuzho.browser.R;
import com.liuzho.browser.browser.AlbumController;
import com.liuzho.browser.browser.BrowserController;
import com.liuzho.browser.browser.DefaultHomePageJsBridge;
import com.liuzho.browser.browser.NinjaDownloadListener;
import com.liuzho.browser.browser.NinjaWebChromeClient;
import com.liuzho.browser.browser.NinjaWebViewClient;
import com.liuzho.browser.browser.ProtectedList;
import com.liuzho.browser.browser.StandardList;
import com.liuzho.browser.browser.TrustedList;
import com.liuzho.browser.database.FaviconHelper;
import com.liuzho.browser.database.Record;
import com.liuzho.browser.database.RecordAction;
import com.liuzho.browser.storage.BrowserPref;
import com.liuzho.browser.storage.ProfileConfig;
import com.liuzho.browser.unit.BrowserUnit;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class NinjaWebView extends WebView implements AlbumController {

    private OnScrollChangeListener onScrollChangeListener;

    public NinjaWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NinjaWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onScrollChanged(int l, int t, int old_l, int old_t) {
        super.onScrollChanged(l, t, old_l, old_t);
        if (onScrollChangeListener != null) {
            onScrollChangeListener.onScrollChange(t, old_t);
        }
    }

    public void setOnScrollChangeListener(OnScrollChangeListener onScrollChangeListener) {
        this.onScrollChangeListener = onScrollChangeListener;
    }

    public interface OnScrollChangeListener {
        /**
         * Called when the scroll position of a view changes.
         *
         * @param scrollY    Current vertical scroll origin.
         * @param oldScrollY Previous vertical scroll origin.
         */
        void onScrollChange(int scrollY, int oldScrollY);
    }

    private Context context;
    private boolean desktopMode;
    private boolean nightMode;
    public boolean fingerPrintProtection;
    public boolean history;
    public boolean adBlock;
    public boolean saveData;
    public boolean camera;
    private boolean stopped;
    private AlbumItem album;
    private AlbumController predecessor = null;
    private NinjaWebViewClient webViewClient;
    private NinjaWebChromeClient webChromeClient;
    private NinjaDownloadListener downloadListener;

    @BrowserPref.Profile
    private String profile;

    public boolean isBackPressed;

    public void setIsBackPressed(Boolean isBackPressed) {
        this.isBackPressed = isBackPressed;
    }

    private TrustedList listTrusted;
    private StandardList listStandard;
    private ProtectedList listProtected;
    private Bitmap favicon;
    private BrowserPref browserSp = BrowserPref.getInstance();

    private boolean foreground;

    public boolean isForeground() {
        return foreground;
    }

    private BrowserController browserController = null;

    public BrowserController getBrowserController() {
        return browserController;
    }

    public void setBrowserController(BrowserController browserController) {
        this.browserController = browserController;
        this.album.setBrowserController(browserController);
    }

    public NinjaWebView(Context context) {
        super(context);
        String profile = browserSp.profile();
        this.context = context;
        this.foreground = false;
        this.desktopMode = false;
        this.nightMode = false;
        this.isBackPressed = false;
        ProfileConfig config = new ProfileConfig(profile);
        this.fingerPrintProtection = config.config(ProfileConfig.FPP);
        this.history = config.config(ProfileConfig.SAVE_HISTORY);
        this.adBlock = config.config(ProfileConfig.AD_BLOCK);
        this.saveData = config.config(ProfileConfig.SAVE_DATA);
        this.camera = config.config(ProfileConfig.CAMERA);

        this.stopped = false;
        this.listTrusted = new TrustedList(context);
        this.listStandard = new StandardList(context);
        this.listProtected = new ProtectedList(context);
        this.album = new AlbumItem(this.context, this, browserController);
        this.webViewClient = new NinjaWebViewClient(this);
        this.webChromeClient = new NinjaWebChromeClient(this);
        this.downloadListener = new NinjaDownloadListener(context);
        initWebView();
        initAlbum();

        addJavascriptInterface(new DefaultHomePageJsBridge(this), "extra");
    }

    private synchronized void initWebView() {
        setWebViewClient(webViewClient);
        setWebChromeClient(webChromeClient);
        setDownloadListener(downloadListener);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @TargetApi(Build.VERSION_CODES.O)
    public synchronized void initPreferences(String url) {

        profile = browserSp.profile();
        WebSettings webSettings = getSettings();

        String userAgent = getUserAgent(desktopMode);
        webSettings.setUserAgentString(userAgent);
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            webSettings.setSafeBrowsingEnabled(true);
        }
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setTextZoom(browserSp.fontZoom());

        boolean autoFill = browserSp.autoFill();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setImportantForAutofill(autoFill ? View.IMPORTANT_FOR_AUTOFILL_YES : View.IMPORTANT_FOR_AUTOFILL_NO);
        } else {
            webSettings.setSaveFormData(autoFill);
        }


        if (listTrusted.isWhite(url)) {
            profile = BrowserPref.Profile.VAL_PROFILE_TRUSTED;
        } else if (listStandard.isWhite(url)) {
            profile = BrowserPref.Profile.VAL_PROFILE_STANDARD;
        } else if (listProtected.isWhite(url)) {
            profile = BrowserPref.Profile.VAL_PROFILE_PROTECTED;
        }

        ProfileConfig pConfig = new ProfileConfig(profile);

        webSettings.setMediaPlaybackRequiresUserGesture(pConfig.config(ProfileConfig.SAVE_DATA));
        webSettings.setBlockNetworkImage(!pConfig.config(ProfileConfig.IMAGES));
        webSettings.setGeolocationEnabled(pConfig.config(ProfileConfig.LOCATION));
        webSettings.setJavaScriptEnabled(pConfig.config(ProfileConfig.JS));
        webSettings.setJavaScriptCanOpenWindowsAutomatically(pConfig.config(ProfileConfig.JS_POPUP));
        webSettings.setDomStorageEnabled(pConfig.config(ProfileConfig.DOM));
        fingerPrintProtection = pConfig.config(ProfileConfig.FPP);
        history = pConfig.config(ProfileConfig.SAVE_HISTORY);
        adBlock = pConfig.config(ProfileConfig.AD_BLOCK);
        saveData = pConfig.config(ProfileConfig.SAVE_DATA);
        camera = pConfig.config(ProfileConfig.CAMERA);
        initCookieManager(url);
    }

    public synchronized void initCookieManager(String url) {
        profile = browserSp.profile();
        if (listTrusted.isWhite(url)) {
            profile = BrowserPref.Profile.VAL_PROFILE_TRUSTED;
        } else if (listStandard.isWhite(url)) {
            profile = BrowserPref.Profile.VAL_PROFILE_STANDARD;
        } else if (listProtected.isWhite(url)) {
            profile = BrowserPref.Profile.VAL_PROFILE_PROTECTED;
        }
        CookieManager manager = CookieManager.getInstance();
        ProfileConfig config = new ProfileConfig(profile);
        if (config.config(ProfileConfig.COOKIES)) {
            manager.setAcceptCookie(true);
            manager.getCookie(url);
        } else {
            manager.setAcceptCookie(false);
        }
    }

    public void setProfileIcon(ImageButton omniBox_tab) {
        String url = getUrl();
        assert url != null;
        switch (profile) {
            case BrowserPref.Profile.VAL_PROFILE_TRUSTED:
                if (url.startsWith("http:")) {
                    omniBox_tab.setImageResource(R.drawable.libbrs_icon_profile_trusted_red);
                } else {
                    omniBox_tab.setImageResource(R.drawable.libbrs_icon_profile_trusted);
                }
                break;
            case BrowserPref.Profile.VAL_PROFILE_STANDARD:
                if (url.startsWith("http:")) {
                    omniBox_tab.setImageResource(R.drawable.libbrs_icon_profile_standard_red);
                } else {
                    omniBox_tab.setImageResource(R.drawable.libbrs_icon_profile_standard);
                }
                break;
            case BrowserPref.Profile.VAL_PROFILE_PROTECTED:
                if (url.startsWith("http:")) {
                    omniBox_tab.setImageResource(R.drawable.libbrs_icon_profile_protected_red);
                } else {
                    omniBox_tab.setImageResource(R.drawable.libbrs_icon_profile_protected);
                }
                break;
            default:
                if (url.startsWith("http:")) {
                    omniBox_tab.setImageResource(R.drawable.libbrs_icon_profile_custom_red);
                } else {
                    omniBox_tab.setImageResource(R.drawable.libbrs_icon_profile_custom);
                }
                break;
        }

        if (listTrusted.isWhite(url)) {
            if (url.startsWith("http:")) {
                omniBox_tab.setImageResource(R.drawable.libbrs_icon_profile_trusted_red);
            } else {
                omniBox_tab.setImageResource(R.drawable.libbrs_icon_profile_trusted);
            }
        } else if (listStandard.isWhite(url)) {
            if (url.startsWith("http:")) {
                omniBox_tab.setImageResource(R.drawable.libbrs_icon_profile_standard_red);
            } else {
                omniBox_tab.setImageResource(R.drawable.libbrs_icon_profile_standard);
            }
        } else if (listProtected.isWhite(url)) {
            if (url.startsWith("http:")) {
                omniBox_tab.setImageResource(R.drawable.libbrs_icon_profile_protected_red);
            } else {
                omniBox_tab.setImageResource(R.drawable.libbrs_icon_profile_protected);
            }
        }
    }


    public void setProfileChanged() {
        ProfileConfig config = new ProfileConfig(profile);
        new ProfileConfig(BrowserPref.Profile.VAL_PROFILE_CUSTOM)
                .setup(BrowserPref.Profile.VAL_PROFILE_CUSTOM,
                        config.config(ProfileConfig.SAVE_DATA),
                        config.config(ProfileConfig.IMAGES),
                        config.config(ProfileConfig.AD_BLOCK),
                        config.config(ProfileConfig.LOCATION),
                        config.config(ProfileConfig.FPP),
                        config.config(ProfileConfig.COOKIES),
                        config.config(ProfileConfig.JS),
                        config.config(ProfileConfig.JS_POPUP),
                        config.config(ProfileConfig.SAVE_HISTORY),
                        config.config(ProfileConfig.CAMERA),
                        config.config(ProfileConfig.MICROPHONE),
                        config.config(ProfileConfig.DOM));
    }

    public void putProfileBoolean(@ProfileConfig.ConfigKey String config, TextView dialogTitle,
                                  Chip chipTrusted, Chip chipStandard,
                                  Chip chipProtected, Chip chipChanged) {
        ProfileConfig pConfig = new ProfileConfig(BrowserPref.Profile.VAL_PROFILE_CUSTOM);
        switch (config) {
            case ProfileConfig.IMAGES:
                pConfig.toggle(config);
                Toast.makeText(this.context, this.context.getString(R.string.libbrs_setting_title_images), Toast.LENGTH_SHORT).show();
                break;
            case ProfileConfig.JS:
                pConfig.toggle(config);
                Toast.makeText(this.context, this.context.getString(R.string.libbrs_setting_title_javascript), Toast.LENGTH_SHORT).show();
                break;
            case ProfileConfig.JS_POPUP:
                pConfig.toggle(config);
                Toast.makeText(this.context, this.context.getString(R.string.libbrs_setting_title_javascript_popUp), Toast.LENGTH_SHORT).show();
                break;
            case ProfileConfig.COOKIES:
                pConfig.toggle(config);
                Toast.makeText(this.context, this.context.getString(R.string.libbrs_setting_title_cookie), Toast.LENGTH_SHORT).show();
                break;
            case ProfileConfig.FPP:
                pConfig.toggle(config);
                Toast.makeText(this.context, this.context.getString(R.string.libbrs_setting_title_fingerPrint), Toast.LENGTH_SHORT).show();
                break;
            case ProfileConfig.AD_BLOCK:
                pConfig.toggle(config);
                Toast.makeText(this.context, this.context.getString(R.string.libbrs_setting_title_adblock), Toast.LENGTH_SHORT).show();
                break;
            case ProfileConfig.SAVE_DATA:
                pConfig.toggle(config);
                Toast.makeText(this.context, this.context.getString(R.string.libbrs_setting_title_save_data), Toast.LENGTH_SHORT).show();
                break;
            case ProfileConfig.SAVE_HISTORY:
                pConfig.toggle(config);
                Toast.makeText(this.context, this.context.getString(R.string.libbrs_setting_title_history), Toast.LENGTH_SHORT).show();
                break;
            case ProfileConfig.LOCATION:
                pConfig.toggle(config);
                Toast.makeText(this.context, this.context.getString(R.string.libbrs_setting_title_location), Toast.LENGTH_SHORT).show();
                break;
            case ProfileConfig.CAMERA:
                pConfig.toggle(config);
                Toast.makeText(this.context, this.context.getString(R.string.libbrs_setting_title_camera), Toast.LENGTH_SHORT).show();
                break;
            case ProfileConfig.MICROPHONE:
                pConfig.toggle(config);
                Toast.makeText(this.context, this.context.getString(R.string.libbrs_setting_title_microphone), Toast.LENGTH_SHORT).show();
                break;
            case ProfileConfig.DOM:
                pConfig.toggle(config);
                Toast.makeText(this.context, this.context.getString(R.string.libbrs_setting_title_dom), Toast.LENGTH_SHORT).show();
                break;
        }
        initPreferences("");

        String textTitle;
        switch (Objects.requireNonNull(profile)) {
            case BrowserPref.Profile.VAL_PROFILE_TRUSTED:
                chipTrusted.setChecked(true);
                chipStandard.setChecked(false);
                chipProtected.setChecked(false);
                chipChanged.setChecked(false);
                textTitle = this.context.getString(R.string.libbrs_setting_title_profiles_active) + ": " + this.context.getString(R.string.libbrs_setting_title_profiles_trusted);
                break;
            case BrowserPref.Profile.VAL_PROFILE_STANDARD:
                chipTrusted.setChecked(false);
                chipStandard.setChecked(true);
                chipProtected.setChecked(false);
                chipChanged.setChecked(false);
                textTitle = this.context.getString(R.string.libbrs_setting_title_profiles_active) + ": " + this.context.getString(R.string.libbrs_setting_title_profiles_standard);
                break;
            case BrowserPref.Profile.VAL_PROFILE_PROTECTED:
                chipTrusted.setChecked(false);
                chipStandard.setChecked(false);
                chipProtected.setChecked(true);
                chipChanged.setChecked(false);
                textTitle = this.context.getString(R.string.libbrs_setting_title_profiles_active) + ": " + this.context.getString(R.string.libbrs_setting_title_profiles_protected);
                break;
            default:
                chipTrusted.setChecked(false);
                chipStandard.setChecked(false);
                chipProtected.setChecked(false);
                chipChanged.setChecked(true);
                textTitle = this.context.getString(R.string.libbrs_setting_title_profiles_active) + ": " + this.context.getString(R.string.libbrs_setting_title_profiles_changed);
                break;
        }
        dialogTitle.setText(textTitle);
    }

    private synchronized void initAlbum() {
        album.setAlbumTitle(context.getString(R.string.libbrs_app_name));
        album.setBrowserController(browserController);
    }

    public synchronized HashMap<String, String> getRequestHeaders() {
        HashMap<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("DNT", "1");
        //  Server-side detection for GlobalPrivacyControl
        requestHeaders.put("Sec-GPC", "1");
        requestHeaders.put("X-Requested-With", "com.duckduckgo.mobile.android");

        profile = browserSp.profile();
        ProfileConfig config = new ProfileConfig(profile);
        if (config.config(ProfileConfig.SAVE_DATA)) {
            requestHeaders.put("Save-Data", "on");
        }
        return requestHeaders;
    }

    @Override
    public synchronized void stopLoading() {
        stopped = true;
        super.stopLoading();
    }

    public synchronized void reloadWithoutInit() {  //needed for camera usage without deactivating "save_data"
        stopped = false;
        super.reload();
    }

    @Override
    public synchronized void reload() {
        stopped = false;
        this.initPreferences(this.getUrl());
        super.reload();
    }

    @Override
    public synchronized void loadUrl(String url) {
        initPreferences(BrowserUnit.queryWrapper(context, url.trim()));
        InputMethodManager imm = (InputMethodManager) this.context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(this.getWindowToken(), 0);
        favicon = null;
        stopped = false;
        super.loadUrl(BrowserUnit.queryWrapper(context, url.trim()), getRequestHeaders());
    }

    @Override
    public View getAlbumView() {
        return album.getAlbumView();
    }

    public void setAlbumTitle(String title, String url) {
        album.setAlbumTitle(title);
        CardView cardView = getAlbumView().findViewById(R.id.cardView);
        cardView.setVisibility(VISIBLE);
        FaviconHelper.setFavicon(context, getAlbumView(), url, R.id.faviconView, R.drawable.libbrs_icon_image_broken);
    }

    @Override
    public synchronized void activate() {
        requestFocus();
        foreground = true;
        album.activate();
    }

    @Override
    public synchronized void deactivate() {
        clearFocus();
        foreground = false;
        album.deactivate();
    }

    public synchronized void updateTitle(int progress) {
        if (foreground && !stopped) {
            browserController.updateProgress(progress);
        } else if (foreground) {
            browserController.updateProgress(BrowserUnit.LOADING_STOPPED);
        }
        if (isLoadFinish() && !stopped) {
            browserController.updateAutoComplete();
        }
    }

    public synchronized void updateTitle(String title) {
        album.setAlbumTitle(title);
    }

    public synchronized void updateFavicon(String url) {
        CardView cardView = getAlbumView().findViewById(R.id.cardView);
        cardView.setVisibility(VISIBLE);
        FaviconHelper.setFavicon(context, getAlbumView(), url, R.id.faviconView, R.drawable.libbrs_icon_image_broken);
    }

    @Override
    public synchronized void destroy() {
        stopLoading();
        onPause();
        clearHistory();
        setVisibility(GONE);
        removeAllViews();
        super.destroy();
    }

    public boolean isLoadFinish() {
        return getProgress() >= BrowserUnit.PROGRESS_MAX;
    }

    public boolean isDesktopMode() {
        return desktopMode;
    }

    public boolean isNightMode() {
        return nightMode;
    }

    public boolean isFingerPrintProtection() {
        return fingerPrintProtection;
    }

    public boolean isHistory() {
        return history;
    }

    public boolean isAdBlock() {
        return adBlock;
    }

    public boolean isSaveData() {
        return saveData;
    }

    public boolean isCamera() {
        return camera;
    }

    public String getUserAgent(boolean desktopMode) {
        String mobilePrefix = "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + ")";
        String desktopPrefix = "Mozilla/5.0 (X11; Linux " + System.getProperty("os.arch") + ")";

        String newUserAgent = WebSettings.getDefaultUserAgent(context);
        String prefix = newUserAgent.substring(0, newUserAgent.indexOf(")") + 1);

        if (desktopMode) {
            try {
                newUserAgent = newUserAgent.replace(prefix, desktopPrefix);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                newUserAgent = newUserAgent.replace(prefix, mobilePrefix);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Override UserAgent if own UserAgent is defined
        // if new switch_text_preference has never been used initialize the switch
        if (!browserSp.contains(BrowserPref.KEY_USER_AGENT_SWITCH)) {
            browserSp.setUserAgentSwitch(!TextUtils.isEmpty(browserSp.customUserAgent()));
        }

        String customUserAgent = browserSp.customUserAgent();
        if (!TextUtils.isEmpty(customUserAgent) && browserSp.userAgentSwitch()) {
            newUserAgent = customUserAgent;
        }
        return newUserAgent;
    }

    public void toggleDesktopMode(boolean reload) {
        desktopMode = !desktopMode;
        String newUserAgent = getUserAgent(desktopMode);
        getSettings().setUserAgentString(newUserAgent);
        getSettings().setUseWideViewPort(desktopMode);
        getSettings().setSupportZoom(desktopMode);
        getSettings().setLoadWithOverviewMode(desktopMode);
        if (reload) reload();
    }

    public void toggleNightMode() {
        nightMode = !nightMode;
        if (nightMode) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(this.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
            } else {
                Paint paint = new Paint();
                ColorMatrix matrix = new ColorMatrix();
                matrix.set(NEGATIVE_COLOR);
                ColorMatrix gcm = new ColorMatrix();
                gcm.setSaturation(0);
                ColorMatrix concat = new ColorMatrix();
                concat.setConcat(matrix, gcm);
                ColorMatrixColorFilter filter = new ColorMatrixColorFilter(concat);
                paint.setColorFilter(filter);
                // maybe sometime LAYER_TYPE_NONE would better?
                this.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
            }
        } else {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(this.getSettings(), WebSettingsCompat.FORCE_DARK_OFF);
            } else {
                this.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
        }
    }

    private static final float[] NEGATIVE_COLOR = {
            -1.0f, 0, 0, 0, 255, // Red
            0, -1.0f, 0, 0, 255, // Green
            0, 0, -1.0f, 0, 255, // Blue
            0, 0, 0, 1.0f, 0     // Alpha
    };

    public void resetFavicon() {
        this.favicon = null;
    }

    public void setFavicon(Bitmap favicon) {
        this.favicon = favicon;

        //Save faviconView for existing bookmarks or start site entries
        FaviconHelper faviconHelper = new FaviconHelper(context);
        RecordAction action = new RecordAction(context);
        action.open(false);
        List<Record> list;
        list = action.listEntries();
        action.close();
        for (Record listItem : list) {
            if (listItem.getURL().equals(getUrl())) {
                if (faviconHelper.getFavicon(listItem.getURL()) == null)
                    faviconHelper.addFavicon(getUrl(), getFavicon());
            }
        }
    }

    @Nullable
    @Override
    public Bitmap getFavicon() {
        return favicon;
    }

    public void setStopped(boolean stopped) {
        this.stopped = stopped;
    }

    public String getProfile() {
        return profile;
    }

    public AlbumController getPredecessor() {
        return predecessor;
    }

    public void setPredecessor(AlbumController predecessor) {
        this.predecessor = predecessor;
    }
}
