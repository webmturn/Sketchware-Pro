package pro.sketchware.dialogs;

import pro.sketchware.core.block.view.FieldBlockView;

/**
 * Callback for writing a user-edited value back into a block field,
 * implemented by the hosting logic editor.
 */
public interface FieldValueSetter {

    void setFieldValue(FieldBlockView fieldBlock, Object value);
}
