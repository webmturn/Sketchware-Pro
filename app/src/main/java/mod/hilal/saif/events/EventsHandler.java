package mod.hilal.saif.events;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;

import pro.sketchware.core.project.ClassInfo;
import pro.sketchware.core.codegen.EventRegistry;
import mod.hey.studios.util.Helper;
import mod.jbk.util.LogUtil;
import mod.jbk.util.OldResourceIdMapper;
import pro.sketchware.R;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.model.CustomEvent;
import pro.sketchware.model.CustomListener;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

public class EventsHandler {

    private static final String TAG = "EventsHandler";
    private static final Gson GSON = new Gson();

    public static final String CUSTOM_EVENTS_FILE_PATH = SketchwarePaths.getAbsolutePathOf(SketchwarePaths.CUSTOM_EVENTS_FILE);
    public static final String CUSTOM_LISTENER_FILE_PATH = SketchwarePaths.getAbsolutePathOf(SketchwarePaths.CUSTOM_LISTENERS_FILE);

    private static ArrayList<CustomEvent> cachedCustomEvents = readCustomEvents();
    private static ArrayList<CustomListener> cachedCustomListeners = readCustomListeners();

    /**
     * This is a utility class, don't instantiate it.
     */
    private EventsHandler() {
    }

    private static final class EventMatch {

        private final int index;
        private final CustomEvent event;

        private EventMatch(int index, CustomEvent event) {
            this.index = index;
            this.event = event;
        }
    }

    private static final class ListenerMatch {

        private final int index;
        private final CustomListener listener;

        private ListenerMatch(int index, CustomListener listener) {
            this.index = index;
            this.listener = listener;
        }
    }

    private static void reportNullCustomEvent(int index) {
        SketchwareUtil.toastError(String.format(Helper.getResString(R.string.event_error_null), index));
    }

    private static void reportNullCustomListener(int index) {
        SketchwareUtil.toastError(String.format(Helper.getResString(R.string.event_error_null), index));
    }

    private static EventMatch findCustomEventByName(String name) {
        for (int i = 0, cachedCustomEventsSize = cachedCustomEvents.size(); i < cachedCustomEventsSize; i++) {
            CustomEvent customEvent = cachedCustomEvents.get(i);
            if (customEvent == null) {
                reportNullCustomEvent(i);
                continue;
            }
            if (name.equals(customEvent.getName())) {
                return new EventMatch(i, customEvent);
            }
        }
        return null;
    }

    private static ListenerMatch findCustomListenerByName(String name) {
        for (int i = 0, cachedCustomListenersSize = cachedCustomListeners.size(); i < cachedCustomListenersSize; i++) {
            CustomListener customListener = cachedCustomListeners.get(i);
            if (customListener == null) {
                reportNullCustomListener(i);
                continue;
            }
            if (name.equals(customListener.getName())) {
                return new ListenerMatch(i, customListener);
            }
        }
        return null;
    }

    private static void addCustomEventsForType(ClassInfo classInfo, ArrayList<String> list) {
        for (int i = 0, cachedCustomEventsSize = cachedCustomEvents.size(); i < cachedCustomEventsSize; i++) {
            CustomEvent customEvent = cachedCustomEvents.get(i);
            if (customEvent == null) {
                reportNullCustomEvent(i);
                continue;
            }
            if (classInfo.matchesType(customEvent.getVar())) {
                list.add(customEvent.getName());
            }
        }
    }

    private static void addCustomListenersForType(ClassInfo classInfo, ArrayList<String> list) {
        for (int i = 0, cachedCustomEventsSize = cachedCustomEvents.size(); i < cachedCustomEventsSize; i++) {
            CustomEvent customEvent = cachedCustomEvents.get(i);
            if (customEvent == null) {
                reportNullCustomEvent(i);
                continue;
            }
            if (classInfo.matchesType(customEvent.getVar())) {
                String listener = customEvent.getListener();
                if (!list.contains(listener)) {
                    list.add(listener);
                }
            }
        }
    }

    private static void addCustomEventsForListener(String listenerName, ArrayList<String> list) {
        for (int i = 0, cachedCustomEventsSize = cachedCustomEvents.size(); i < cachedCustomEventsSize; i++) {
            CustomEvent customEvent = cachedCustomEvents.get(i);
            if (customEvent == null) {
                reportNullCustomEvent(i);
                continue;
            }
            if (listenerName.equals(customEvent.getListener())) {
                list.add(customEvent.getName());
            }
        }
    }

    private static void addImports(ArrayList<String> list, String imports) {
        if (!imports.isEmpty()) {
            list.addAll(new ArrayList<>(Arrays.asList(imports.split("\n"))));
        }
    }

    private static <T> ArrayList<T> readCustomConfig(String filePath, Type listType, String configLabel) {
        if (!FileUtil.isExistFile(filePath)) {
            return new ArrayList<>();
        }

        String content = FileUtil.readFile(filePath);
        if (content.isEmpty() || content.equals("[]")) {
            return new ArrayList<>();
        }

        try {
            ArrayList<T> data = GSON.fromJson(content, listType);
            if (data == null) {
                LogUtil.e(TAG, "Failed to parse " + configLabel + " file! Now using none");
                return new ArrayList<>();
            }
            return data;
        } catch (JsonParseException e) {
            LogUtil.e(TAG, "Failed to parse " + configLabel + " file! Now using none", e);
            return new ArrayList<>();
        }
    }

    /**
     * Used in {@link EventRegistry#getAllActivityEvents()}
     *
     * @return Array of Activity Events.
     * @apiNote Custom Activity Events can be added by writing to the file
     * defined by {@link #CUSTOM_EVENTS_FILE_PATH} and specifying an empty string for "var"
     */
    public static String[] getActivityEvents() {
        ArrayList<String> array = new ArrayList<>();

        array.add("Import");
        array.add("initializeLogic");
        array.add("onActivityResult");
        array.add("onBackPressed");
        array.add("onPostCreate");
        array.add("onStart");
        array.add("onResume");
        array.add("onPause");
        array.add("onStop");
        array.add("onDestroy");
        array.add("onSaveInstanceState");
        array.add("onRestoreInstanceState");
        array.add("onCreateOptionsMenu");
        array.add("onOptionsItemSelected");
        array.add("onCreateContextMenu");
        array.add("onContextItemSelected");
        array.add("onTabLayoutNewTabAdded");

        for (int i = cachedCustomEvents.size() - 1; i >= 0; i--) {
            CustomEvent customEvent = cachedCustomEvents.get(i);
            if (customEvent == null) {
                reportNullCustomEvent(i);
                continue;
            }
            if (customEvent.getVar().isEmpty()) {
                array.add(customEvent.getName());
            }
        }

        return array.toArray(new String[0]);
    }

    /**
     * Used in {@link mod.agus.jcoderz.editor.event.ManageEvent#addExtraEvents(ClassInfo, ArrayList)} to retrieve extra
     * Events for Components, such as custom ones.
     */
    public static void addEvents(ClassInfo classInfo, ArrayList<String> list) {
        if (classInfo.matchesType("Clickable")) {
            list.add(" onLongClick");
        }
        if (classInfo.matchesType("SwipeRefreshLayout")) {
            list.add("onSwipeRefreshLayout");
        }
        if (classInfo.matchesType("AsyncTask")) {
            list.add("onPreExecute");
            list.add("doInBackground");
            list.add("onProgressUpdate");
            list.add("onPostExecute");
        }

        addCustomEventsForType(classInfo, list);
    }

    /**
     * Used in {@link mod.agus.jcoderz.editor.event.ManageEvent#addExtraListeners(ClassInfo, ArrayList)} to get extra
     * listeners for Components and Widgets, such as custom ones.
     */
    public static void addListeners(ClassInfo classInfo, ArrayList<String> list) {
        if (classInfo.matchesType("Clickable")) {
            list.add(" onLongClickListener");
        }
        if (classInfo.matchesType("SwipeRefreshLayout")) {
            list.add("onSwipeRefreshLayoutListener");
        }
        if (classInfo.matchesType("AsyncTask")) {
            list.add("AsyncTaskClass");
        }

        addCustomListenersForType(classInfo, list);
    }

    /**
     * Used in {@link mod.agus.jcoderz.editor.event.ManageEvent#addEventsForListener(String, ArrayList)} to get extra
     * listeners' Events, such as custom ones.
     */
    public static void addEventsToListener(String name, ArrayList<String> list) {
        switch (name) {
            case " onLongClickListener":
                list.add(" onLongClick");
                break;

            case "onSwipeRefreshLayoutListener":
                list.add("onSwipeRefreshLayout");
                break;

            case "AsyncTaskClass":
                list.add("onPreExecute");
                list.add("doInBackground");
                list.add("onProgressUpdate");
                list.add("onPostExecute");
                break;

            default:
                addCustomEventsForListener(name, list);
                break;
        }
    }

    public static int getIcon(String name) {
        return switch (name) {
            case "Import", "onActivityResult", "initializeLogic", "onBackPressed", "onPostCreate",
                 "onStart", "onResume", "onPause", "onStop", "onDestroy",
                 "onTabLayoutNewTabAdded" -> R.drawable.ic_mtrl_code;
            case " onLongClick" -> R.drawable.ic_mtrl_touch_long;
            case "onSwipeRefreshLayout" -> R.drawable.ic_mtrl_refresh;
            case "onPreExecute" -> R.drawable.ic_mtrl_track_started;
            case "doInBackground" -> R.drawable.ic_mtrl_sprint;
            case "onProgressUpdate" -> R.drawable.ic_mtrl_progress;
            case "onPostExecute" -> R.drawable.ic_mtrl_progress_check;
            default -> {
                EventMatch match = findCustomEventByName(name);
                if (match == null) {
                    yield R.drawable.android_icon;
                }
                try {
                    yield OldResourceIdMapper.getDrawableFromOldResourceId(Integer.parseInt(match.event.getIcon()));
                } catch (NumberFormatException e) {
                    SketchwareUtil.toastError(String.format(Helper.getResString(R.string.event_error_invalid_icon), match.index + 1));
                    yield R.drawable.android_icon;
                }
            }
        };
    }

    public static String getDesc(String name) {
        return switch (name) {
            case "Import" -> Helper.getResString(R.string.event_desc_add_custom_imports);
            case "onActivityResult" -> Helper.getResString(R.string.event_onactivityresult);
            case "initializeLogic" -> Helper.getResString(R.string.event_initializelogic);
            case "onSwipeRefreshLayout" -> Helper.getResString(R.string.event_desc_swipe_refresh);
            case " onLongClick" -> Helper.getResString(R.string.event_onlongclick);
            case "onTabLayoutNewTabAdded" -> Helper.getResString(R.string.event_desc_tab_new_tab);
            case "onPreExecute" ->
                    Helper.getResString(R.string.event_desc_pre_execute);
            case "doInBackground" ->
                    Helper.getResString(R.string.event_desc_do_in_background);
            case "onProgressUpdate" ->
                    Helper.getResString(R.string.event_desc_progress_update);
            case "onPostExecute" ->
                    Helper.getResString(R.string.event_desc_post_execute);
            default -> {
                EventMatch match = findCustomEventByName(name);
                yield match != null ? match.event.getDescription() : "No_Description";
            }
        };
    }

    public static String getEventCode(String targetId, String name, String param) {
        return switch (name) {
            case "Import" ->
                    "//Ul5kmZqmO867OV0QTGOpjwX7MXmgzxzQBSZTf0Y16PnDXkhLsZfvF\r\n" +
                            param + "\r\n" +
                            "//3b5IqsVG57gNqLi7FBO2MeOW6iI7tOustUGwcA7HKXm0o7lovZ";
            case "onActivityResult", "initializeLogic" -> "";
            case "onSwipeRefreshLayout" ->
                    "@Override\r\n" +
                            "public void onRefresh() {\n" +
                            param + "\r\n" +
                            "}";
            case " onLongClick" ->
                    "@Override\r\n" +
                            "public boolean onLongClick(View _view) {\r\n" +
                            param + "\r\n" +
                            "return true;\r\n" +
                            "}";
            case "onTabLayoutNewTabAdded" ->
                    "public CharSequence onTabLayoutNewTabAdded(int _position) {\r\n" +
                            (param.isEmpty() ? "return \"\";\r\n" :
                                    param + "\r\n"
                            ) + "}";
            case "onPreExecute" -> "@Override\r\n" +
                    "protected void onPreExecute() {\r\n" +
                    param + "\r\n" +
                    "}";
            case "doInBackground" -> "@Override\r\n" +
                    "protected String doInBackground(String... params) {\r\n" +
                    "String _param = params[0];\r\n" +
                    param + "\r\n" +
                    "}";
            case "onProgressUpdate" -> "@Override\r\n" +
                    "protected void onProgressUpdate(Integer... values) {\r\n" +
                    "int _value = values[0];\r\n" +
                    param + "\r\n" +
                    "}";
            case "onPostExecute" -> "@Override\r\n" +
                    "protected void onPostExecute(String _result) {\r\n" +
                    param + "\r\n" +
                    "}";
            default -> {
                EventMatch match = findCustomEventByName(name);
                yield match != null ? String.format(match.event.getCode().replace("###", targetId), param) : "//no code";
            }
        };
    }

    public static String getBlocks(String name) {
        return switch (name) {
            case "Import", "initializeLogic", "onSwipeRefreshLayout", " onLongClick",
                 "onPreExecute" -> "";
            case "onActivityResult" -> "%d.requestCode %d.resultCode %m.intent";
            case "onTabLayoutNewTabAdded", "onProgressUpdate" -> "%d";
            case "doInBackground", "onPostExecute" -> "%s";
            default -> {
                EventMatch match = findCustomEventByName(name);
                yield match != null ? match.event.getParameters() : "";
            }
        };
    }

    public static String getSpec(String name, String event) {
        return switch (event) {
            case "Import" -> "create new import";
            case "onActivityResult" ->
                    "OnActivityResult %d.requestCode %d.resultCode %m.intent.data";
            case "initializeLogic" -> "initializeLogic";
            case "onSwipeRefreshLayout" -> "when " + name + " refresh";
            case " onLongClick" -> "when " + name + " long clicked";
            case "onTabLayoutNewTabAdded" -> name + " return tab title %d.position";
            case "onPreExecute" -> name + " onPreExecute ";
            case "doInBackground" -> name + " doInBackground %s.param";
            case "onProgressUpdate" -> name + " onProgressUpdate progress %d.value";
            case "onPostExecute" -> name + " onPostExecute result %s.result";
            default -> {
                EventMatch match = findCustomEventByName(event);
                yield match != null ? match.event.getHeaderSpec().replace("###", name) : "no spec";
            }
        };
    }

    public static String getListenerCode(String name, String var, String param) {
        return switch (name) {
            case " onLongClickListener" ->
                    var + ".setOnLongClickListener(new View.OnLongClickListener() {\r\n" +
                            param + "\r\n" +
                            "});";
            case "onSwipeRefreshLayoutListener" ->
                    var + ".setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {\r\n" +
                            param + "\r\n" +
                            "});";
            case "AsyncTaskClass" ->
                    "private class " + var + " extends AsyncTask<String, Integer, String> {\r\n" +
                            param + "\r\n" +
                            "}";
            default -> {
                ListenerMatch match = findCustomListenerByName(name);
                yield match != null ? String.format(match.listener.getCode().replace("###", var), param) : "//no listener code";
            }
        };
    }

    public static void getImports(ArrayList<String> list, String name) {
        for (int i = 0, cachedCustomListenersSize = cachedCustomListeners.size(); i < cachedCustomListenersSize; i++) {
            CustomListener customListener = cachedCustomListeners.get(i);
            if (customListener == null) {
                reportNullCustomListener(i);
                continue;
            }
            if (name.equals(customListener.getName())) {
                addImports(list, customListener.getImports());
            }
        }
    }

    public static void refreshCachedCustomEvents() {
        cachedCustomEvents = readCustomEvents();
    }

    public static void refreshCachedCustomListeners() {
        cachedCustomListeners = readCustomListeners();
    }

    private static ArrayList<CustomEvent> readCustomEvents() {
        return readCustomConfig(
                CUSTOM_EVENTS_FILE_PATH,
                new TypeToken<ArrayList<CustomEvent>>(){}.getType(),
                "Custom Events"
        );
    }

    private static ArrayList<CustomListener> readCustomListeners() {
        return readCustomConfig(
                CUSTOM_LISTENER_FILE_PATH,
                new TypeToken<ArrayList<CustomListener>>(){}.getType(),
                "Custom Listeners"
        );
    }
}
