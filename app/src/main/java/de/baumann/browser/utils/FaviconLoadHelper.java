package de.baumann.browser.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.LruCache;
import android.util.TypedValue;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.palette.graphics.Palette;

import com.liuzho.lib.baseutils.ScreenUtils;

import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.net.URL;

import de.baumann.browser.Browser;
import de.baumann.browser.Constant;
import de.baumann.browser.database.FaviconHelper;

public class FaviconLoadHelper {
    private static LruCache<String, SoftReference<Bitmap>> sCustomFaviconCache = new LruCache<>(10);

    @WorkerThread
    @Nullable
    public static Bitmap loadFavicon(String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        Uri uri;
        try {
            uri = Uri.parse(url);
        } catch (Exception e) {
            return null;
        }
        boolean customFav = Constant.CUSTOM_FAVICON_HOST.equals(uri.getScheme());
        FaviconHelper faviconHelper = new FaviconHelper(Browser.getConfig().context);
        Bitmap favicon = faviconHelper.getFavicon(url);
        if (favicon != null && !customFav) {
            return favicon;
        }
        if (favicon == null) {
            try {
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
                favicon = bitmap;
            } catch (Exception ignore) {
                return null;
            }
        }
        if (favicon != null && customFav) {
            String name = uri.getHost().substring(0, 1);
            String title = uri.getQueryParameter("title");
            if (!TextUtils.isEmpty(title)) {
                name = title.substring(0, 1);
            }
            String cacheKey = uri.getHost() + "_" + name;
            SoftReference<Bitmap> cache = sCustomFaviconCache.get(cacheKey);
            if (cache != null && cache.get() != null && !cache.get().isRecycled()) {
                return cache.get();
            }
            int size = ScreenUtils.dp2px(72f, Browser.getConfig().context.getResources());
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            TextPaint paint = new TextPaint();
            paint.setColor(Palette.from(favicon).generate().getVibrantColor(Browser.getThemeHandler().accentColor(Browser.getConfig().context)));
            canvas.drawOval(0, 0, size, size, paint);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 52f, Browser.getConfig().context.getResources().getDisplayMetrics()));
            paint.setStrokeWidth(8f);
            paint.setColor(Color.WHITE);
            Rect rect = new Rect();
            paint.getTextBounds(name, 0, 1, rect);
            canvas.drawText(name, size / 2f - rect.width() / 2f - rect.left, size / 2f + rect.height() / 2f - rect.bottom, paint);
            sCustomFaviconCache.put(cacheKey, new SoftReference<>(bitmap));
            return bitmap;
        }
        return favicon;
    }
}
