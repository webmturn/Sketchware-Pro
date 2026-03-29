package com.besome.sketch.tools;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

public class CollectErrorActivity extends BaseAppCompatActivity {
    private static final int MAX_CLIPBOARD_TEXT_CHARS = 180000;
    private static final int FALLBACK_CLIPBOARD_TEXT_CHARS = 20000;

    @SuppressLint("SetTextI18n")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }

        // Read crash report: prefer file-based (includes device info), fall back to legacy extra
        String crashReport = loadCrashReport(intent);
        if (crashReport == null || crashReport.isEmpty()) {
            crashReport = "(No crash data available)";
        }
        final String report = crashReport;

        var dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.common_error_an_error_occurred)
                .setMessage(R.string.error_crash_report_msg)
                .setPositiveButton(R.string.common_word_copy, null)
                .setNegativeButton(R.string.common_word_cancel, (dialogInterface, which) -> finish())
                .setNeutralButton(R.string.error_crash_show_error, null)
                .setCancelable(false)
                .show();

        TextView messageView = dialog.findViewById(android.R.id.message);

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            messageView.setTextIsSelectable(true);
            messageView.setText(report);
        });
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) {
                SketchwareUtil.toastError(Helper.getResString(R.string.common_error_an_error_occurred));
                return;
            }

            try {
                clipboard.setPrimaryClip(ClipData.newPlainText("crash_report", buildClipboardText(report, MAX_CLIPBOARD_TEXT_CHARS)));
                SketchwareUtil.toast(Helper.getResString(R.string.toast_copied), Toast.LENGTH_LONG);
            } catch (RuntimeException e) {
                Log.e("CollectErrorActivity", "Failed to copy full crash report to clipboard", e);
                try {
                    clipboard.setPrimaryClip(ClipData.newPlainText("crash_report", buildClipboardText(report, FALLBACK_CLIPBOARD_TEXT_CHARS)));
                    SketchwareUtil.toast(Helper.getResString(R.string.toast_copied), Toast.LENGTH_LONG);
                } catch (RuntimeException inner) {
                    Log.e("CollectErrorActivity", "Failed to copy fallback crash report to clipboard", inner);
                    SketchwareUtil.toastError(Helper.getResString(R.string.common_error_an_error_occurred));
                }
            }
        });
    }

    private String buildClipboardText(String report, int maxLength) {
        String formattedReport = formatClipboardText(report);
        if (formattedReport.length() <= maxLength) {
            return formattedReport;
        }

        String truncatedNotice = "\n\n[Crash report truncated for clipboard. Original length: "
                + report.length() + " chars]\n\n";
        int availableReportLength = maxLength - truncatedNotice.length() - 8;
        if (availableReportLength <= 0) {
            return formatClipboardText(truncatedNotice.trim());
        }

        int headLength = availableReportLength / 2;
        int tailLength = availableReportLength - headLength;
        String truncatedReport = report.substring(0, headLength)
                + truncatedNotice
                + report.substring(report.length() - tailLength);
        return formatClipboardText(truncatedReport);
    }

    private String formatClipboardText(String text) {
        return "```\n" + text + "\n```";
    }

    /**
     * Loads the crash report string. Prefers the file-based report (which already
     * contains device info, memory stats, timestamps). Falls back to the legacy
     * "error" string extra for backward compatibility.
     */
    private String loadCrashReport(Intent intent) {
        String crashFilePath = intent.getStringExtra("crash_file");
        if (crashFilePath != null) {
            try {
                return FileUtil.readFile(crashFilePath);
            } catch (Exception e) {
                Log.e("CollectErrorActivity", "Failed to read crash file: " + crashFilePath, e);
            }
        }
        return intent.getStringExtra("error");
    }
}
