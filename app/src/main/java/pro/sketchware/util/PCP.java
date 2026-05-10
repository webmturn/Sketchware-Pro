package pro.sketchware.util;

import android.widget.TextView;

import pro.sketchware.widgets.ColorPickerDialog;

public class PCP implements ColorPickerDialog.OnColorPickedListener {

    private final TextView toSetText;

    public PCP(TextView toSetText) {
        this.toSetText = toSetText;
    }

    @Override
    public void onColorPicked(int color) {
        toSetText.setText(String.format("#%08X", color));
    }

    @Override
    public void onResourceColorPicked(String color, int i2) {
        toSetText.setText(String.format("#%08X", i2));
    }
}
