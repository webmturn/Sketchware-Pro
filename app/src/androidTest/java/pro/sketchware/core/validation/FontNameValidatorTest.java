package pro.sketchware.core.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.ContextThemeWrapper;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import pro.sketchware.R;
import pro.sketchware.util.Helper;

@RunWith(AndroidJUnit4.class)
public class FontNameValidatorTest {
    @Test(timeout = 10000)
    public void nonLetterFirstCharactersReturnWithAnError() {
        onMainThread(() -> {
            Fixture fixture = new Fixture(null, null);
            for (String name : new String[]{"1font", "_font", " font", ".font"}) {
                fixture.assertInvalid(name, fixture.context.getString(
                        R.string.logic_editor_message_variable_name_must_start_letter));
            }
        });
    }

    @Test(timeout = 10000)
    public void correctingAnInvalidNameClearsTheError() {
        onMainThread(() -> {
            Fixture fixture = new Fixture(null, null);
            fixture.assertValid("font_a");
            fixture.assertInvalid("1font", fixture.context.getString(
                    R.string.logic_editor_message_variable_name_must_start_letter));
            fixture.assertValid("font_b2");
        });
    }

    @Test(timeout = 10000)
    public void lengthLimitsArePreserved() {
        onMainThread(() -> {
            Fixture fixture = new Fixture(null, null);
            fixture.assertInvalid("ab", Helper.getResString(R.string.invalid_value_min_lenth, 3));
            fixture.assertValid("abc");
            fixture.assertValid("a".repeat(70));
            fixture.assertInvalid("a".repeat(71), Helper.getResString(R.string.invalid_value_max_lenth, 70));
            fixture.assertInvalid("   ", Helper.getResString(R.string.invalid_value_min_lenth, 3));
        });
    }

    @Test(timeout = 10000)
    public void namesStillRequireLowercaseAsciiLettersDigitsOrUnderscores() {
        onMainThread(() -> {
            Fixture fixture = new Fixture(null, null);
            fixture.assertValid("font_123");
            for (String name : new String[]{"Font", "字体名", "font-name", "font name", "font "}) {
                fixture.assertInvalid(name, Helper.getResString(R.string.invalid_value_rule_4));
            }
        });
    }

    @Test(timeout = 10000)
    public void reservedKeywordsRemainInvalid() {
        onMainThread(() -> {
            Fixture fixture = new Fixture(null, null);
            for (String name : new String[]{"class", "while"}) {
                fixture.assertInvalid(name, fixture.context.getString(
                        R.string.logic_editor_message_reserved_keywords));
            }
        });
    }

    @Test(timeout = 10000)
    public void unavailableAndDuplicateNamesRemainInvalid() {
        onMainThread(() -> {
            Fixture fixture = new Fixture(new ArrayList<>(Arrays.asList("existing")), null);
            for (String name : new String[]{"default_image", "NONE", "none", "existing"}) {
                fixture.assertInvalid(name, Helper.getResString(R.string.common_message_name_unavailable));
            }
        });
    }

    @Test(timeout = 10000)
    public void renamingAllowsTheOriginalNameButRejectsOtherDuplicates() {
        onMainThread(() -> {
            Fixture fixture = new Fixture(new ArrayList<>(Arrays.asList("original", "existing")), "original");
            fixture.assertValid("original");
            fixture.assertInvalid("existing", Helper.getResString(R.string.common_message_name_unavailable));
            fixture.assertValid("renamed");
        });
    }

    @Test(timeout = 10000)
    public void aMissingFontListStillAllowsValidNames() {
        onMainThread(() -> new Fixture(null, null).assertValid("font_a"));
    }

    @Test(timeout = 10000)
    public void clearingTheInputClearsTheErrorWithoutMakingItValid() {
        onMainThread(() -> {
            Fixture fixture = new Fixture(null, null);
            fixture.assertInvalid("1font", fixture.context.getString(
                    R.string.logic_editor_message_variable_name_must_start_letter));
            fixture.input.setText("");
            assertFalse(fixture.validator.isValid());
            assertNull(fixture.layout.getError());
        });
    }

    private static void onMainThread(Runnable action) {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try {
                action.run();
            } catch (Throwable error) {
                failure.set(error);
            }
        });
        if (failure.get() != null) {
            throw new AssertionError("Font validation failed on the main thread", failure.get());
        }
    }

    private static class Fixture {
        final Context context = new ContextThemeWrapper(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), R.style.Theme_SketchwarePro);
        final TextInputLayout layout = new TextInputLayout(context);
        final TextInputEditText input = new TextInputEditText(context);
        final FontNameValidator validator;

        Fixture(ArrayList<String> fontNames, String editingName) {
            layout.addView(input);
            String[] reservedKeywords = {"class", "while"};
            validator = editingName == null
                    ? new FontNameValidator(context, layout, reservedKeywords, fontNames)
                    : new FontNameValidator(context, layout, reservedKeywords, fontNames, editingName);
        }

        void assertValid(String name) {
            input.setText(name);
            assertTrue(name, validator.isValid());
            assertNull(name, layout.getError());
        }

        void assertInvalid(String name, String error) {
            input.setText(name);
            assertFalse(name, validator.isValid());
            assertEquals(name, error, layout.getError().toString());
        }
    }
}
