package pro.sketchware.util;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.internal.UnsafeAllocator;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

import pro.sketchware.SketchApplication;
import pro.sketchware.activities.tools.CollectErrorActivity;

/** Build synthetic reports without throwing an uncaught exception or touching real crash logs. */
@RunWith(AndroidJUnit4.class)
public class CrashReportLocaleTest {
    @Test public void englishReportUsesRootFormat() throws Exception {
        assertReport(Locale.US);
    }

    @Test public void chineseReportUsesRootFormat() throws Exception {
        assertReport(Locale.SIMPLIFIED_CHINESE);
    }

    @Test public void frenchReportUsesRootFormat() throws Exception {
        assertReport(Locale.FRANCE);
    }

    @Test public void arabicReportUsesLatinDigits() throws Exception {
        assertReport(Locale.forLanguageTag("ar-EG"));
    }

    @Test public void persianReportUsesLatinDigits() throws Exception {
        assertReport(Locale.forLanguageTag("fa-IR"));
    }

    @Test public void storedReportReachesCopyFormattingUnchanged() throws Exception {
        String report = assertReport(Locale.forLanguageTag("ar-EG"));
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File fixture = File.createTempFile("crash-locale-", ".txt", context.getCacheDir());
        try {
            Files.write(fixture.toPath(), report.getBytes(StandardCharsets.UTF_8));
            // No Activity lifecycle, dialog, clipboard write, or crash-log pruning is needed here.
            CollectErrorActivity activity = UnsafeAllocator.INSTANCE.newInstance(CollectErrorActivity.class);
            Method load = CollectErrorActivity.class.getDeclaredMethod("loadCrashReport", Intent.class);
            load.setAccessible(true);
            String loaded = (String) load.invoke(activity,
                    new Intent().putExtra("crash_file", fixture.getAbsolutePath()));
            assertEquals(report, loaded);
            Method copy = CollectErrorActivity.class.getDeclaredMethod("buildClipboardText", String.class, int.class);
            copy.setAccessible(true);
            assertEquals("```\n" + report + "\n```", copy.invoke(activity, loaded, 180000));
        } finally {
            assertTrue(fixture.delete());
        }
    }

    private static String assertReport(Locale locale) throws Exception {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        configuration.setLocale(locale);
        Context localized = base.createConfigurationContext(configuration);
        Configuration original = new Configuration(localized.getResources().getConfiguration());
        Configuration baseOriginal = new Configuration(base.getResources().getConfiguration());
        Locale defaultOriginal = Locale.getDefault();
        Locale formatOriginal = Locale.getDefault(Locale.Category.FORMAT);
        Locale displayOriginal = Locale.getDefault(Locale.Category.DISPLAY);
        PackageInfo info = base.getPackageManager().getPackageInfo(base.getPackageName(), 0);
        long apkSize = new File(info.applicationInfo.sourceDir).length();
        assertTrue(apkSize > 0);
        Thread thread = new Thread("report-线程-😀");
        Throwable failure = new IllegalStateException("synthetic-中文-عربي-😀",
                new IllegalArgumentException("cause-preserved"));
        Field contextField = SketchApplication.class.getDeclaredField("mApplicationContext");
        contextField.setAccessible(true);
        Object originalContext = contextField.get(null);
        String report;
        try {
            // Override only this process, never persisted app or device language settings.
            Locale.setDefault(locale);
            contextField.set(null, localized);
            report = CrashLogManager.buildCrashReport(thread, failure);
        } finally {
            contextField.set(null, originalContext);
            Locale.setDefault(defaultOriginal);
            Locale.setDefault(Locale.Category.FORMAT, formatOriginal);
            Locale.setDefault(Locale.Category.DISPLAY, displayOriginal);
        }
        assertSame(originalContext, SketchApplication.getAppContext());
        assertEquals(original, localized.getResources().getConfiguration());
        assertEquals(baseOriginal, base.getResources().getConfiguration());
        assertEquals(defaultOriginal, Locale.getDefault());
        assertEquals(formatOriginal, Locale.getDefault(Locale.Category.FORMAT));
        assertEquals(displayOriginal, Locale.getDefault(Locale.Category.DISPLAY));
        String sizeLine = "";
        for (String line : report.split("\n")) {
            if (line.startsWith("APK Size: ")) {
                sizeLine = line;
                break;
            }
        }
        Log.i("CrashReportLocaleTest", locale.toLanguageTag() + ": " + sizeLine);
        assertTrue(locale.toLanguageTag() + ": " + sizeLine,
                sizeLine.matches("APK Size: [0-9]+\\.[0-9]{2} MB \\([0-9]+ B\\)"));
        assertTrue(sizeLine, sizeLine.endsWith(" (" + apkSize + " B)"));
        BigDecimal displayedMb = new BigDecimal(sizeLine.substring("APK Size: ".length(), sizeLine.indexOf(" MB")));
        BigDecimal actualMb = BigDecimal.valueOf(apkSize).movePointLeft(6);
        assertTrue(sizeLine, displayedMb.subtract(actualMb).abs().compareTo(new BigDecimal("0.005")) <= 0);
        assertTrue(report.contains("App Version: " + info.versionName + " (" + info.versionCode + ")\n"));
        assertTrue(report.contains("Thread: " + thread.getName() + " (id=" + thread.getId() + ")\n"));
        assertTrue(report.endsWith(Log.getStackTraceString(failure)));
        assertTrue(report.contains("\n--- Stack Trace ---\n"));
        return report;
    }
}
