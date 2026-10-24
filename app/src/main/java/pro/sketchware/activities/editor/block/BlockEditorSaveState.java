package pro.sketchware.activities.editor.block;

import androidx.lifecycle.ViewModel;

/** Small completion receipt retained across Activity recreation; never holds the editor. */
public final class BlockEditorSaveState extends ViewModel {
    boolean saved;
    boolean conflicted;
}
