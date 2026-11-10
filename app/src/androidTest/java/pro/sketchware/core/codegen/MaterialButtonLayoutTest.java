package pro.sketchware.core.codegen;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.graphics.Color;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.beans.ViewBeans;
import pro.sketchware.core.project.BuildConfig;

@RunWith(AndroidJUnit4.class)
public class MaterialButtonLayoutTest {
    private static final String BACKGROUND = "preview_background";

    @Test
    public void generatedButtonBackgroundKeepsExplicitTint() {
        var button = new ViewBean("button", ViewBeans.VIEW_TYPE_WIDGET_MATERIALBUTTON);
        button.parent = "root";
        button.layout.backgroundColor = Color.RED;
        assertTrue(buttonXml(button).contains("app:backgroundTint=\"#FF0000\""));

        button.layout.backgroundResource = BACKGROUND;
        String xml = buttonXml(button);
        assertTrue(xml.contains("android:background=\"@drawable/" + BACKGROUND + "\""));
        assertFalse(xml.contains("app:backgroundTint="));

        button.inject = "app:backgroundTint=\"#0000FF\"";
        xml = buttonXml(button);
        assertTrue(xml.contains("android:background=\"@drawable/" + BACKGROUND + "\""));
        assertTrue(xml.contains("app:backgroundTint=\"#0000FF\""));

        button.layout.backgroundResource = null;
        button.inject = "android:background=\"@drawable/" + BACKGROUND + "\"";
        xml = buttonXml(button);
        assertTrue(xml.contains("android:background=\"@drawable/" + BACKGROUND + "\""));
        assertTrue(xml.contains("app:backgroundTint=\"#FF0000\""));
    }

    private static String buttonXml(ViewBean button) {
        var config = new BuildConfig();
        config.sc_id = "MaterialButtonPreviewXml";
        var file = new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_CUSTOM_VIEW, "button");
        var generator = new LayoutGenerator(config, file);
        generator.setViews(new ArrayList<>(List.of(button)));
        return generator.toXmlString();
    }
}
