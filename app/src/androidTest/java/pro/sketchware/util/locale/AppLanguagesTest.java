package pro.sketchware.util.locale;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@RunWith(AndroidJUnit4.class)
public class AppLanguagesTest {
    @Test public void readsEveryConfiguredLanguageAndPreservesRegionalAndScriptVariants() throws Exception {
        List<String> tags = Arrays.asList("en", "en-GB", "es", "fr", "de", "it", "pt-BR", "ru",
                "ar", "he", "hi", "bn", "ta", "ja", "ko", "zh-Hans", "zh-Hant", "tr",
                "nl", "pl", "sv", "th", "vi", "id");
        StringBuilder xml = new StringBuilder("<locale-config xmlns:android=\"http://schemas.android.com/apk/res/android\">");
        for (String tag : tags) xml.append("<locale android:name=\"").append(tag).append("\"/>");
        xml.append("<locale android:name=\"EN\"/><locale android:name=\"und\"/><locale/></locale-config>");
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new StringReader(xml.toString()));
        assertEquals(tags, AppLanguages.parse(parser).stream().map(Locale::toLanguageTag).toList());
    }

    @Test public void regionalSelectionPrefersExactMatchAndDoesNotSelectAnotherRegion() {
        List<Locale> regional = locales("en-US", "en-GB");
        assertEquals(1, AppLanguages.findSelection(regional, Locale.UK));
        assertEquals(-1, AppLanguages.findSelection(regional, Locale.forLanguageTag("en-AU")));
        assertEquals(0, AppLanguages.findSelection(locales("en", "en-GB"), Locale.forLanguageTag("en-AU")));
        assertEquals(-1, AppLanguages.findSelection(locales("en", "zh"), Locale.FRENCH));
    }

    @Test public void distinguishesSimplifiedAndTraditionalChinese() {
        List<Locale> scripts = locales("zh-Hans", "zh-Hant");
        assertEquals(0, AppLanguages.findSelection(scripts, Locale.SIMPLIFIED_CHINESE));
        assertEquals(1, AppLanguages.findSelection(scripts, Locale.TRADITIONAL_CHINESE));
        assertEquals(0, AppLanguages.findSelection(locales("zh"), Locale.TRADITIONAL_CHINESE));
    }

    private static List<Locale> locales(String... tags) {
        return Arrays.stream(tags).map(Locale::forLanguageTag).toList();
    }
}
