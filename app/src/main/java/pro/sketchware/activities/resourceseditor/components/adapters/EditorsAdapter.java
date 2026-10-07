package pro.sketchware.activities.resourceseditor.components.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import pro.sketchware.activities.resourceseditor.ResourceEditorTabs;
import pro.sketchware.activities.resourceseditor.ResourcesEditorActivity;

public class EditorsAdapter extends FragmentStateAdapter {

    private final ResourcesEditorActivity activity;

    public EditorsAdapter(@NonNull ResourcesEditorActivity activity) {
        super(activity);
        this.activity = activity;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return switch (position) {
            case ResourceEditorTabs.STRINGS -> activity.stringsEditor;
            case ResourceEditorTabs.COLORS -> activity.colorsEditor;
            case ResourceEditorTabs.STYLES -> activity.stylesEditor;
            case ResourceEditorTabs.THEMES -> activity.themesEditor;
            case ResourceEditorTabs.ARRAYS -> activity.arraysEditor;
            case ResourceEditorTabs.DIMENS -> activity.dimensEditor;
            default -> throw new IllegalArgumentException("Invalid position");
        };
    }

    @Override
    public int getItemCount() {
        return ResourceEditorTabs.COUNT;
    }
}
