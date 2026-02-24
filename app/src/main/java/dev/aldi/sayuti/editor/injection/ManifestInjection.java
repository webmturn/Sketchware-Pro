package dev.aldi.sayuti.editor.injection;

import java.util.ArrayList;

import pro.sketchware.core.BuildConfig;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.xml.XmlBuilder;

public class ManifestInjection {

    public ArrayList arr;
    public BuildConfig buildConfig;
    public String path;
    public String replace;
    public String value;

    public ManifestInjection(BuildConfig buildConfig, ArrayList arrayList) {
        this.buildConfig = buildConfig;
        arr = arrayList;
    }

    public void injectManifestAttributes(XmlBuilder nx, String fileName, String activityName) {
        path = FileUtil.getExternalStorageDir() + "/.sketchware/data/" + buildConfig.sc_id + "/injection/manifest/" + fileName;
        if (FileUtil.isExistFile(path)) {
            FileUtil.readFile(path).isEmpty();
        }
    }
}