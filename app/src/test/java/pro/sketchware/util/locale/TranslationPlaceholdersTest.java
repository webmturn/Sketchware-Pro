package pro.sketchware.util.locale;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TranslationPlaceholdersTest {
    @Test
    public void indexedArgumentsCanBeReordered() {
        assertTrue(TranslationPlaceholders.matches("%s has %d blocks", "%2$d blocks for %1$s"));
    }

    @Test
    public void rejectsMissingArgumentsAndInvalidFormats() {
        assertFalse(TranslationPlaceholders.matches("Found %d", "Found %s"));
        assertFalse(TranslationPlaceholders.matches("Found %d", "Found several"));
        assertFalse(TranslationPlaceholders.matches("Found %d", "Found %0d"));
        assertFalse(TranslationPlaceholders.matches("Found %d", "Found %d x %"));
        assertFalse(TranslationPlaceholders.matches("Locale: %s", "Locale %s %#%"));
        assertFalse(TranslationPlaceholders.matches("Locale: %s", "Locale %s %4n"));
        assertFalse(TranslationPlaceholders.matches("%s %<s", "%s %<<s"));
    }

    @Test
    public void supportsLiteralPercentsAndReusedArguments() {
        assertTrue(TranslationPlaceholders.matches("Progress 100%", "100% complete"));
        assertTrue(TranslationPlaceholders.matches("%1$s %<s (%%)", "%s (%%) %<s"));
        assertTrue(TranslationPlaceholders.matches("Found %d", "Found %d%% %n"));
        assertTrue(TranslationPlaceholders.matches("%ts", "%Ts"));
        assertFalse(TranslationPlaceholders.matches("%ts", "%tS"));
        assertFalse(TranslationPlaceholders.matches("%ts", "%s"));
    }

    @Test
    public void translationsCanRepeatExistingArguments() {
        assertTrue(TranslationPlaceholders.matches("Found %d", "Found %d (%<d total)"));
        assertTrue(TranslationPlaceholders.matches("Found %d", "%1$d out of %1$d"));
        assertFalse(TranslationPlaceholders.matches("Found %d", "Found %d (%d total)"));
        assertFalse(TranslationPlaceholders.matches("Found %d", "Found %d (%<s total)"));
        assertFalse(TranslationPlaceholders.matches("%s: %d", "%1$s: %<s"));
        assertFalse(TranslationPlaceholders.matches("Found %d", "%<d found"));
    }
}
