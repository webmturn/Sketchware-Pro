package pro.sketchware.fragments.settings.events;

import java.io.File;

import pro.sketchware.core.SketchwarePaths;

public class EventsManagerConstants {
    public static final File EVENT_EXPORT_LOCATION = new File(SketchwarePaths.getAbsolutePathOf(SketchwarePaths.EVENT_EXPORT_PATH));
    public static final File EVENTS_FILE = new File(SketchwarePaths.getAbsolutePathOf(SketchwarePaths.CUSTOM_EVENTS_FILE));
    public static final File LISTENERS_FILE = new File(SketchwarePaths.getAbsolutePathOf(SketchwarePaths.CUSTOM_LISTENERS_FILE));
}