package pro.sketchware.utility;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import java.util.HashMap;

import pro.sketchware.core.SharedPrefsHelper;
import pro.sketchware.core.SketchToast;
import pro.sketchware.core.ProjectListManager;
import pro.sketchware.core.EncryptedFileUtil;
import pro.sketchware.core.SketchwarePaths;
import pro.sketchware.core.MapValueHelper;
import mod.hey.studios.util.Helper;
import mod.jbk.build.BuiltInLibraries;
import pro.sketchware.R;
import pro.sketchware.activities.main.activities.MainActivity;

public class DataResetter {
    public static void a(Context context, boolean resetOnlySettings) {
        new SharedPrefsHelper(context, "P2").a();
        new SharedPrefsHelper(context, "P3").a();
        new SharedPrefsHelper(context, "P1").a();
        new SharedPrefsHelper(context, "P12").a();
        new SharedPrefsHelper(context, "P99").a();
        new SharedPrefsHelper(context, "P25").a();
        new SharedPrefsHelper(context, "P26").a();
        new SharedPrefsHelper(context, "P16").a("P16I0");

        EncryptedFileUtil oBVar = new EncryptedFileUtil();
        oBVar.b(SketchwarePaths.getLibsPath());
        oBVar.b(BuiltInLibraries.EXTRACTED_COMPILE_ASSETS_PATH.getAbsolutePath());

        if (!resetOnlySettings) {
            oBVar.b(SketchwarePaths.getSketchwareRootPath());
            oBVar.b(SketchwarePaths.getSketchwarePath());

            for (HashMap<String, Object> project : ProjectListManager.listProjects()) {
                String sc_id = MapValueHelper.c(project, "sc_id");
                new SharedPrefsHelper(context, "D01_" + sc_id).a();
                new SharedPrefsHelper(context, "D02_" + sc_id).a();
                new SharedPrefsHelper(context, "D03_" + sc_id).a();
                new SharedPrefsHelper(context, "D04_" + sc_id).a();
                new SharedPrefsHelper(context, "D05_" + sc_id).a();
            }
            new SharedPrefsHelper(context, "P15").a();
        }

        SketchToast.toast(context, Helper.getResString(R.string.program_information_reset_system_complete_initialize), SketchToast.TOAST_NORMAL).show();
        ProjectListManager.initializeDb(context, true);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Activity activity = (Activity) context;
        activity.startActivity(intent);
        activity.finishAffinity();
    }
}
