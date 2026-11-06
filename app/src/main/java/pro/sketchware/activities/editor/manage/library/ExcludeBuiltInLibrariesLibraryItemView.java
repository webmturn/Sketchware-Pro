package pro.sketchware.activities.editor.manage.library;

import android.content.Context;
import androidx.annotation.Nullable;
import java.util.List;
import pro.sketchware.R;
import pro.sketchware.beans.ProjectLibraryBean;
import pro.sketchware.core.library.BuiltInLibraries;
import pro.sketchware.core.library.ExcludeBuiltInLibrariesConfig;
import pro.sketchware.util.Helper;

public class ExcludeBuiltInLibrariesLibraryItemView extends LibraryItemView {
    private final String sc_id;

    public ExcludeBuiltInLibrariesLibraryItemView(Context context, String sc_id) {
        super(context);
        this.sc_id = sc_id;
    }

    @Override
    public void setData(@Nullable ProjectLibraryBean projectLibraryBean) {
        boolean excludingEnabled = ExcludeBuiltInLibrariesConfig.isExcludingEnabled(sc_id);
        List<BuiltInLibraries.BuiltInLibrary> excludedLibraries =
                ExcludeBuiltInLibrariesConfig.getExcludedLibraries(sc_id);
        icon.setImageResource(ExcludeBuiltInLibrariesActivity.getItemIcon());
        title.setText(ExcludeBuiltInLibrariesActivity.getItemTitle());
        description.setText(
                !excludingEnabled
                        ? ExcludeBuiltInLibrariesActivity.getDefaultItemDescription()
                        : String.format(
                                ExcludeBuiltInLibrariesActivity
                                        .getSelectedLibrariesItemDescription(),
                                excludedLibraries.size(),
                                BuiltInLibraries.KNOWN_BUILT_IN_LIBRARIES.length));
        enabled.setText(
                excludingEnabled
                        ? Helper.getResString(R.string.library_status_on)
                        : Helper.getResString(R.string.library_status_off));
        enabled.setSelected(excludingEnabled);
    }
}
