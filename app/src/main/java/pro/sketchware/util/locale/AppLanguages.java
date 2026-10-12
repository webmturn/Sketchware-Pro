package pro.sketchware.util.locale;

import android.content.Context;
import android.content.res.XmlResourceParser;

import androidx.core.os.LocaleListCompat;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import pro.sketchware.R;

/** Uses the same supported language list as Android's app language settings. */
public final class AppLanguages {
    private AppLanguages() {
    }

    public static List<Locale> read(Context context) {
        try (XmlResourceParser parser = context.getResources().getXml(R.xml.locales_config)) {
            return parse(parser);
        } catch (IOException | XmlPullParserException e) {
            throw new IllegalStateException("Unable to read app language configuration", e);
        }
    }

    static List<Locale> parse(XmlPullParser parser) throws IOException, XmlPullParserException {
        Map<String, Locale> locales = new LinkedHashMap<>();
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() != XmlPullParser.START_TAG || !"locale".equals(parser.getName())) continue;
            String tag = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "name");
            if (tag == null) continue;
            Locale locale = Locale.forLanguageTag(tag);
            if (!locale.getLanguage().isEmpty()) locales.putIfAbsent(locale.toLanguageTag(), locale);
        }
        return new ArrayList<>(locales.values());
    }

    public static int findSelection(List<Locale> supported, Locale current) {
        int exact = supported.indexOf(current);
        if (exact >= 0) return exact;
        // Match a regional locale to a general language/script, never to a different region.
        for (int i = 0; i < supported.size(); i++) {
            Locale candidate = supported.get(i);
            if (candidate.getCountry().isEmpty()
                    && LocaleListCompat.matchesLanguageAndScript(candidate, current)) return i;
        }
        for (int i = 0; i < supported.size(); i++) {
            Locale candidate = supported.get(i);
            if (candidate.getCountry().isEmpty() && candidate.getScript().isEmpty()
                    && candidate.getLanguage().equals(current.getLanguage())) return i;
        }
        return -1;
    }
}
