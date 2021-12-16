package de.baumann.browser.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import de.baumann.browser.Browser;
import de.baumann.browser.database.FaviconHelper;

public class FaviconLoadHelper {
    @WorkerThread
    @Nullable
    public static Bitmap loadFavicon(String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        FaviconHelper faviconHelper = new FaviconHelper(Browser.getConfig().context);
        Bitmap favicon = faviconHelper.getFavicon(url);
        if (favicon != null) {
            return favicon;
        }
        try {
            Uri uri = Uri.parse(url);
            URL imageUrl = new URL("http://" + uri.getHost() + "/favicon.ico");
            HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + ")");
            connection.setDoInput(true);
            connection.connect();
            InputStream inputStream = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap != null) {
                faviconHelper.addFavicon(imageUrl.toString(), bitmap);
            }
            return bitmap;
        } catch (Exception ignore) {
            return null;
        }
    }
}
