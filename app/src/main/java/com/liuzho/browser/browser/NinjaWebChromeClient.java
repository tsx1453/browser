package com.liuzho.browser.browser;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.liuzho.browser.R;
import com.liuzho.browser.activity.BrowserActivity;
import com.liuzho.browser.storage.ProfileConfig;
import com.liuzho.browser.unit.HelperUnit;
import com.liuzho.browser.view.NinjaWebView;

import java.util.Objects;

public class NinjaWebChromeClient extends WebChromeClient {

    private final NinjaWebView ninjaWebView;

    public NinjaWebChromeClient(NinjaWebView ninjaWebView) {
        super();
        this.ninjaWebView = ninjaWebView;
    }

    @Override
    public void onProgressChanged(WebView view, int progress) {
        super.onProgressChanged(view, progress);
        ninjaWebView.updateTitle(progress);
        if (Objects.requireNonNull(view.getTitle()).isEmpty()) {
            ninjaWebView.updateTitle(view.getUrl());
        } else {
            ninjaWebView.updateTitle(view.getTitle());
        }
        ninjaWebView.updateFavicon(view.getUrl());
    }

    @Override
    public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, android.os.Message resultMsg) {
        Context context = view.getContext();
        WebView newWebView = new WebView(context);
        view.addView(newWebView);
        WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
        transport.setWebView(newWebView);
        resultMsg.sendToTarget();
        newWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
//                Intent browserIntent = new Intent(Intent.ACTION_VIEW);
//                browserIntent.setData(request.getUrl());
//                context.startActivity(browserIntent);
//                return true;
                Context context = view.getContext();
                while (context instanceof ContextWrapper) {
                    if (context instanceof BrowserActivity) {
                        ((BrowserActivity) context).addAlbum("", request.getUrl().toString(), true, false, "");
                    }
                    context = ((ContextWrapper) context).getBaseContext();
                }
                return true;
            }
        });
        return true;
    }

    @Override
    public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
        ninjaWebView.getBrowserController().onShowCustomView(view, callback);
        super.onShowCustomView(view, callback);
    }

    @Override
    public void onHideCustomView() {
        ninjaWebView.getBrowserController().onHideCustomView();
        super.onHideCustomView();
    }

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
        ninjaWebView.getBrowserController().showFileChooser(filePathCallback);
        return true;
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
        Activity activity = (Activity) ninjaWebView.getContext();
        HelperUnit.grantPermissionsLoc(activity);
        callback.invoke(origin, true, false);
        super.onGeolocationPermissionsShowPrompt(origin, callback);
    }

    @Override
    public void onPermissionRequest(PermissionRequest request) {
        ProfileConfig config = new ProfileConfig(ninjaWebView.getProfile());
        Activity activity = (Activity) ninjaWebView.getContext();
        String[] resources = request.getResources();
        for (String resource : resources) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                if (config.config(ProfileConfig.CAMERA)) {
                    HelperUnit.grantPermissionsCamera(activity);
                    if (ninjaWebView.getSettings().getMediaPlaybackRequiresUserGesture()) {
                        ninjaWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);  //fix conflict with save data option. Temporarily switch off setMediaPlaybackRequiresUserGesture
                        ninjaWebView.reloadWithoutInit();
                    }
                    request.grant(request.getResources());
                }
            } else if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                if (config.config(ProfileConfig.MICROPHONE)) {
                    HelperUnit.grantPermissionsMic(activity);
                    request.grant(request.getResources());
                }
            } else if (PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID.equals(resource)) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(ninjaWebView.getContext());
                builder.setIcon(R.drawable.libbrs_icon_alert);
                builder.setTitle(R.string.libbrs_app_warning);
                builder.setMessage(R.string.libbrs_hint_DRM_Media);
                builder.setPositiveButton(android.R.string.ok, (dialog, whichButton) -> request.grant(request.getResources()));
                builder.setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> request.deny());
                AlertDialog dialog = builder.create();
                dialog.show();
                HelperUnit.setupDialog(ninjaWebView.getContext(), dialog);
            }
        }
    }

    @Override
    public void onReceivedIcon(WebView view, Bitmap icon) {
        ninjaWebView.setFavicon(icon);
        ninjaWebView.updateFavicon(view.getUrl());
        super.onReceivedIcon(view, icon);
    }

    @Override
    public void onReceivedTitle(WebView view, String sTitle) {
        super.onReceivedTitle(view, sTitle);
    }
}
