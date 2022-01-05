package com.liuzho.browser.browser;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;

import java.net.URLEncoder;
import java.util.List;

import com.liuzho.browser.Browser;
import com.liuzho.browser.BrowserConstant;
import com.liuzho.browser.database.Record;
import com.liuzho.browser.database.RecordAction;
import com.liuzho.browser.view.NinjaWebView;

public class DefaultHomePageJsBridge {
    private final NinjaWebView mWebView;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public DefaultHomePageJsBridge(NinjaWebView mWebView) {
        this.mWebView = mWebView;
    }

    @JavascriptInterface
    public void search(String keyword) {
        mHandler.post(() -> mWebView.loadUrl(keyword));
    }

    @JavascriptInterface
    public void initHomeItems() {
        RecordAction action = new RecordAction(Browser.getConfig().context);
        action.open(false);
        List<Record> list = action.listStartSite();
        for (Record record : list) {
            String host = record.getURL();
            try {
                Uri uri = Uri.parse(record.getURL());
                host = uri.getHost();
            } catch (Exception e) {
                e.printStackTrace();
            }
            addHomeItems(record.getTitle(), BrowserConstant.CUSTOM_FAVICON_HOST + "://" + host + "?title=" + URLEncoder.encode(record.getTitle()), record.getURL());
        }
        action.close();
    }

    private void addHomeItems(String title, String icon, String url) {
        final String js = "function addItem() {\n" +
                "            const contentElement = document.getElementById(\"content\");\n" +
                "            const child = document.createElement('div');\n" +
                "            child.className = \"box\";\n" +
                "            child.innerHTML = `\n" +
                "                <a href=\"" + url + "\"></a>\n" +
                "                <p><img class=\"icon\" src=\"" + icon + "\" ></p>\n" +
                "                <p class=\"url\">" + title + "</p>\n" +
                "            `;\n" +
                "            contentElement.appendChild(child)\n" +
                "        }" +
                "addItem();";
        mHandler.post(() -> mWebView.evaluateJavascript(js, null));
    }

}
