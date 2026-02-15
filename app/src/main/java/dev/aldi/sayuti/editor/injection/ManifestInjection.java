package dev.aldi.sayuti.editor.injection;

import java.util.ArrayList;

import a.a.a.BuildConfig;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.xml.XmlBuilder;

public class ManifestInjection {

    public ArrayList arr;
    public BuildConfig BuildConfig;
    public String path;
    public String replace;
    public String value;

    public ManifestInjection(BuildConfig jqVar, ArrayList arrayList) {
        BuildConfig = jqVar;
        arr = arrayList;
    }

    public void b(XmlBuilder nx, String str, String str2) {
        path = FileUtil.getExternalStorageDir() + "/.sketchware/data/" + BuildConfig.sc_id + "/injection/manifest/" + str;
        if (FileUtil.isExistFile(path)) {
            FileUtil.readFile(path).isEmpty();
        }
    }
}