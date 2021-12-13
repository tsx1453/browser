package de.baumann.browser.browser;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;

import java.net.URI;
import java.util.List;

import de.baumann.browser.Browser;
import de.baumann.browser.Constant;
import de.baumann.browser.database.Record;
import de.baumann.browser.database.RecordAction;
import de.baumann.browser.view.NinjaWebView;

public class JSBridge {
    private final NinjaWebView mWebView;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public JSBridge(NinjaWebView mWebView) {
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
            addHomeItems(record.getTitle(), Constant.CUSTOM_FAVICON_HOST + "://" + host, record.getURL());
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
