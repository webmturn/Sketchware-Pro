package pro.sketchware.activities.resourceseditor;

import android.os.Bundle;
import android.os.Parcel;

/** Bounded compatibility input, never a new snapshot of the live editor models. */
final class ResourceEditorLegacyState {
    static final int STATE_BUDGET = 64 * 1024;
    static final int FRAMEWORK_RESERVE = 16 * 1024;
    // Leave room for the omission flags written after the size check as well.
    private static final int INLINE_LIMIT = STATE_BUDGET - FRAMEWORK_RESERVE - 256;
    static final int DRAFT_OMITTED = 1;
    static final int CORRUPT_OMITTED = 2;

    private ResourceEditorLegacyState() { }

    static int writeFallback(Bundle state, Bundle drafts, Bundle corrupt) {
        int omitted = 0;
        if (!putIfFits(state, "resource_editor_drafts", drafts)) omitted |= DRAFT_OMITTED;
        if (!putIfFits(state, "resource_legacy_corrupt", corrupt)) omitted |= CORRUPT_OMITTED;
        return omitted;
    }

    private static boolean putIfFits(Bundle state, String key, Bundle source) {
        if (source == null || source.isEmpty()) return true;
        Bundle candidate = new Bundle(state);
        candidate.putBundle(key, source);
        try {
            if (parcelSize(candidate) > INLINE_LIMIT) return false;
        } catch (RuntimeException unreadable) {
            return false;
        }
        state.putBundle(key, source);
        return true;
    }

    static int parcelSize(Bundle bundle) {
        Parcel parcel = Parcel.obtain();
        try {
            parcel.writeBundle(bundle);
            return parcel.dataSize();
        } finally {
            parcel.recycle();
        }
    }

    static byte[] parcelBytes(Bundle bundle) {
        Parcel parcel = Parcel.obtain();
        try {
            parcel.writeBundle(bundle);
            return parcel.marshall();
        } finally {
            parcel.recycle();
        }
    }
}
