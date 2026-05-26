package com.example.e68.app.data.report.typography;

import android.content.Context;
import android.graphics.Typeface;
import com.itextpdf.io.font.PdfEncodings;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FontManager {

    private final PdfFont regular;
    private final PdfFont bold;

    public FontManager(Context context) throws Exception {
        // Используем шрифты из assets
        regular = loadFontFromAssets(context, "times.ttf");
        bold = loadFontFromAssets(context, "timesbd.ttf");
    }

    // Метод для загрузки шрифта из assets
    private PdfFont loadFontFromAssets(Context context, String fontFileName) throws Exception {
        File tempFontFile = copyFontToCache(context, fontFileName);
        return PdfFontFactory.createFont(tempFontFile.getAbsolutePath(), PdfEncodings.IDENTITY_H);
    }

    private File copyFontToCache(Context context, String fontFileName) throws Exception {
        File cacheFile = new File(context.getCacheDir(), fontFileName);

        if (!cacheFile.exists()) {
            try (InputStream is = context.getAssets().open("fonts/" + fontFileName);
                 FileOutputStream fos = new FileOutputStream(cacheFile)) {

                byte[] buffer = new byte[8192];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
            }
        }

        return cacheFile;
    }

    public PdfFont regular() {
        return regular;
    }

    public PdfFont bold() {
        return bold;
    }
}