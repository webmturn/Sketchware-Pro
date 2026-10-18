package pro.sketchware.activities.resourceseditor;

/**
 * Explicit outcome of one save attempt: per-page XML writes, draft protection,
 * and whether the restore reference may still point at older content.
 */
public final class ResourceEditorSaveResult {
    public enum XmlWrite { SAVED, UNCHANGED, FAILED }
    public enum DraftState {
        /** Latest model committed and the CLEAN manifest was confirmed. */
        CLEAN_CONFIRMED,
        /** Latest model committed, but it is still marked dirty or not all pages were cleaned. */
        CONTENT_PROTECTED,
        /** XML-only save succeeded and the old draft session was closed. */
        DISCARDED,
        /** Latest model or old session could not be made safe. */
        UNSAFE
    }

    public final XmlWrite[] pages;
    public final DraftState draftState;
    public final boolean xmlAttempted;
    public final boolean canSaveXmlOnly;
    public final int savedPages;
    public final int unchangedPages;
    public final int failedPages;
    public final boolean navigationAllowed;
    public final boolean restoreReferenceUnsafe;

    public ResourceEditorSaveResult(XmlWrite[] pages, DraftState draftState,
                                    boolean xmlAttempted, boolean canSaveXmlOnly) {
        this.pages = pages;
        this.draftState = draftState;
        this.xmlAttempted = xmlAttempted;
        this.canSaveXmlOnly = canSaveXmlOnly;
        int saved = 0;
        int unchanged = 0;
        int failed = 0;
        if (pages != null) {
            for (XmlWrite page : pages) {
                if (page == null) continue;
                switch (page) {
                    case SAVED -> saved++;
                    case UNCHANGED -> unchanged++;
                    case FAILED -> failed++;
                }
            }
        }
        savedPages = saved;
        unchangedPages = unchanged;
        failedPages = failed;
        navigationAllowed = xmlAttempted && failed == 0 && draftState != DraftState.UNSAFE;
        restoreReferenceUnsafe = xmlAttempted && draftState == DraftState.UNSAFE;
    }

    public static ResourceEditorSaveResult draftFailure() {
        return new ResourceEditorSaveResult(null, DraftState.UNSAFE, false, true);
    }

    public XmlWrite page(int index) {
        return pages == null ? null : pages[index];
    }

    public boolean allXmlSaved() {
        return xmlAttempted && failedPages == 0;
    }
}
