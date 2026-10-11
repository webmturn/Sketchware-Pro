package pro.sketchware.activities.editor.makeblock;

import pro.sketchware.util.locale.LocalizedDisplayNames;
import pro.sketchware.R;
import pro.sketchware.util.Helper;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import pro.sketchware.util.LogUtil;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.util.FileUtil;

/**
 * Helper used in {@link pro.sketchware.activities.editor.LogicEditorActivity} to get menus for custom
 * Menus defined in {@link mod.hilal.saif.activities.tools.BlockSelectorActivity}.
 */
public class BlockMenu {

    public static Pair<String, String[]> getMenu(String name) {
        Pair<String, String[]> result = null;
        Pair<String, String[]> fallback = new Pair<>(Helper.getResString(R.string.block_select_variable, name), new String[0]);
        File file = new File(SketchwarePaths.getBlockSelectorMenuPath());
        if (!file.exists()) {
            result = fallback;
        } else {
            try {
                JSONArray jSONArray = new JSONArray(FileUtil.readFile(file.getAbsolutePath()));
                for (int i = 0; i < jSONArray.length(); i++) {
                    JSONObject jSONObject = jSONArray.getJSONObject(i);
                    if (jSONObject.getString("name").equals(name)) {
                        String title = jSONObject.getString("title");
                        if ("typeview".equals(name) && "Select typeview:".equals(title)) {
                            title = LocalizedDisplayNames.get(title);
                        }
                        result = new Pair<>(title, jsonarrayToArr(jSONObject.getJSONArray("data")));
                        break;
                    }
                }
            } catch (Exception ignored) {
                LogUtil.w("BlockMenu", "Failed to parse block menu JSON for: " + name, ignored);
            }
            if (result == null) {
                result = fallback;
            }
        }

        return result;
    }

    private static String[] jsonarrayToArr(JSONArray jSONArray) throws JSONException {
        String[] items = new String[jSONArray.length()];
        for (int i = 0; i < jSONArray.length(); i++) {
            items[i] = jSONArray.getString(i);
        }
        return items;
    }
}
