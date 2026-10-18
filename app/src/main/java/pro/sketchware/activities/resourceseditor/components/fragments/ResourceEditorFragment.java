package pro.sketchware.activities.resourceseditor.components.fragments;

import androidx.fragment.app.Fragment;

import java.io.IOException;

import pro.sketchware.activities.resourceseditor.ResourceEditorDraftState;
import pro.sketchware.activities.resourceseditor.ResourceEditorSaveResult;
import pro.sketchware.activities.resourceseditor.ResourceXmlWriter;
import pro.sketchware.activities.resourceseditor.ResourcesEditorActivity;
import pro.sketchware.util.LogUtil;

/** Tracks edits separately from whether the latest draft has already been protected. */
public abstract class ResourceEditorFragment extends Fragment implements ResourceEditorDraftState.Owner {
    public boolean hasUnsavedChanges;
    private long revision;

    public final void markDraftChanged() {
        invalidateDraftSnapshot(null);
        advanceDraftRevision();
    }

    /** All mutations in this edit belong to this model; notes are always captured separately. */
    public final void markDraftChanged(Object changedModel) {
        invalidateDraftSnapshot(changedModel);
        advanceDraftRevision();
    }

    protected void invalidateDraftSnapshot(Object changedModel) { }

    private void advanceDraftRevision() {
        hasUnsavedChanges = true;
        revision++;
        onDraftChanged(revision);
        notifyDraftChanged();
    }

    protected void onDraftChanged(long revision) { }

    protected final void markDraftClean() {
        if (!hasUnsavedChanges) return;
        hasUnsavedChanges = false;
        revision++;
        notifyDraftChanged();
    }

    private void notifyDraftChanged() {
        if (getActivity() instanceof ResourcesEditorActivity activity) activity.onResourceDraftChanged();
    }

    @Override public final long draftRevision() { return revision; }
    @Override public final boolean hasDraftChanges() { return hasUnsavedChanges; }

    public final void restoreDraftRevision(long revision) { this.revision = revision; }

    /** Exact XML bytes for this page; the XML-only path hashes these before writing. */
    public abstract String renderXml();

    /** Destination used by this page's save path. */
    public abstract String resourcePath();

    public final ResourceEditorSaveResult.XmlWrite writePreparedXml(String path, String xml) {
        return writeResourceXml(path, xml);
    }

    protected final ResourceEditorSaveResult.XmlWrite writeResourceXml(String path, String xml) {
        try {
            ResourceXmlWriter.write(path, xml);
            markDraftClean();
            return ResourceEditorSaveResult.XmlWrite.SAVED;
        } catch (IOException | RuntimeException failure) {
            LogUtil.e("ResourceEditor", "Unable to save resource XML: " + path, failure);
            return ResourceEditorSaveResult.XmlWrite.FAILED;
        }
    }
}
