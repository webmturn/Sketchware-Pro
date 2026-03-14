package mod.agus.jcoderz.editor.manage.block.palette;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.HashMap;

import dev.aldi.sayuti.block.ExtraBlockFile;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.utility.SketchwareUtil;

public class PaletteSelector {

    private final ArrayList<HashMap<String, Object>> list = new ArrayList<>();
    private int start = 9;

    public ArrayList<HashMap<String, Object>> getPaletteSelector() {
        ArrayList<HashMap<String, Object>> palettes = ExtraBlockFile.getPaletteBlockData();
        if (!palettes.isEmpty()) {
            for (int i = 0; i < palettes.size(); i++) {
                HashMap<String, Object> item = palettes.get(i);
                Object name = item.get("name");
                if (!(name instanceof String paletteName)) {
                    continue;
                }

                int color;
                try {
                    color = Color.parseColor(String.valueOf(item.get("color")));
                } catch (IllegalArgumentException e) {
                    SketchwareUtil.toastError(String.format(Helper.getResString(R.string.error_parse_palette_color), i + 1));
                    color = 0xff8a55d7;
                }

                setPaletteData(start, paletteName, color);
                start++;
            }
        }

        return list;
    }

    public void setPaletteData(int index, String name, int color) {
        HashMap<String, Object> palette = new HashMap<>();
        palette.put("index", index);
        palette.put("text", name);
        palette.put("color", color);
        list.add(palette);
    }
}
