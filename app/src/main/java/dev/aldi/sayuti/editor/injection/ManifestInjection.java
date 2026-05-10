package dev.aldi.sayuti.editor.injection;

import java.io.File;
import java.util.ArrayList;

import pro.sketchware.core.project.BuildConfig;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.xml.XmlBuilder;

public class ManifestInjection {

    public ArrayList arr;
    public BuildConfig buildConfig;
    public String path;
    public String replace;
    public String value;

    public ManifestInjection(BuildConfig buildConfig, ArrayList injections) {
        this.buildConfig = buildConfig;
        arr = injections;
    }

    public void injectManifestAttributes(XmlBuilder xmlBuilder, String fileName, String activityName) {
        path = SketchwarePaths.getDataPath(buildConfig.sc_id) + File.separator + "injection"
                + File.separator + "manifest" + File.separator + fileName;
        if (FileUtil.isExistFile(path)) {
            FileUtil.readFile(path).isEmpty();
        }
    }
}