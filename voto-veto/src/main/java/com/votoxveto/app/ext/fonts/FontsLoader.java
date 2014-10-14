package com.votoxveto.app.ext.fonts;

import android.content.Context;
import android.graphics.Typeface;
import com.google.common.base.Supplier;
import com.google.common.util.concurrent.ListenableFuture;
import com.votoxveto.app.helpers.AsyncHelper;
import com.votoxveto.app.helpers.CollectionsHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class FontsLoader {

    public static final String APPETITE = "Appetite.otf";

    private static final String PREFIX = "fonts/";

    private static class InstanceHolder {
        private static final FontsLoader INSTANCE = new FontsLoader();
    }

    public static FontsLoader getInstance(Context context) {
        return InstanceHolder.INSTANCE.setContext(context);
    }

    private Context mContext;
    private Map<String, Typeface> mFontsMap = new HashMap<>();

    private FontsLoader() {
        /* Prevents outside instantiation */
    }

    private FontsLoader setContext(Context context) {
        mContext = context;
        return this;
    }

    public ListenableFuture<Typeface> getTypeface(final String font) {
        return AsyncHelper.executeTask(new Callable<Typeface>() {
            @Override
            public Typeface call() throws Exception {
                return CollectionsHelper.getOrDefault(mFontsMap, font, new FontsSupplier(PREFIX + font));
            }
        });
    }

    private class FontsSupplier implements Supplier<Typeface> {

        private final String mFont;

        public FontsSupplier(String font) {
            mFont = font;
        }

        @Override
        public Typeface get() {
            return Typeface.createFromAsset(mContext.getAssets(), mFont);
        }
    }
}
