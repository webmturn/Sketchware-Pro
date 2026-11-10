package pro.sketchware.core.layout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Map;
import org.junit.Test;
import pro.sketchware.beans.LayoutBean;

public class MaterialButtonAttributesTest {
    @Test
    public void unsetColorLeavesTheStyleTintInPlace() {
        assertNull(MaterialButtonAttributes.generatedBackgroundTint(new LayoutBean(), Map.of()));
    }

    @Test
    public void zeroColorEmitsTransparentTint() {
        var layout = new LayoutBean();
        layout.backgroundColor = 0;

        assertEquals(
                "@android:color/transparent",
                MaterialButtonAttributes.generatedBackgroundTint(layout, Map.of()));
    }

    @Test
    public void literalTintIncludesAlphaWhenNeeded() {
        var layout = new LayoutBean();
        layout.backgroundColor = 0xff244e40;
        assertEquals("#244E40", MaterialButtonAttributes.generatedBackgroundTint(layout, Map.of()));

        layout.backgroundColor = 0x80244e40;
        assertEquals(
                "#80244E40", MaterialButtonAttributes.generatedBackgroundTint(layout, Map.of()));
    }

    @Test
    public void resourceTintKeepsExistingReferences() {
        var layout = new LayoutBean();
        layout.backgroundColor = 0;
        layout.backgroundResColor = "brand";
        assertEquals(
                "@color/brand", MaterialButtonAttributes.generatedBackgroundTint(layout, Map.of()));

        layout.backgroundResColor = "@color/brand";
        assertEquals(
                "@color/brand", MaterialButtonAttributes.generatedBackgroundTint(layout, Map.of()));

        layout.backgroundResColor = "?attr/colorPrimary";
        assertEquals(
                "?attr/colorPrimary",
                MaterialButtonAttributes.generatedBackgroundTint(layout, Map.of()));
    }

    @Test
    public void imageBackgroundSuppressesGeneratedTint() {
        var layout = new LayoutBean();
        layout.backgroundColor = 0xff244e40;
        layout.backgroundResource = "paper";
        assertNull(MaterialButtonAttributes.generatedBackgroundTint(layout, Map.of()));

        layout.backgroundResource = "none";
        assertEquals("#244E40", MaterialButtonAttributes.generatedBackgroundTint(layout, Map.of()));
    }

    @Test
    public void explicitTintTakesPrecedenceOverTheLayoutColor() {
        var layout = new LayoutBean();
        layout.backgroundColor = 0xff244e40;

        assertNull(
                MaterialButtonAttributes.generatedBackgroundTint(
                        layout, Map.of("app:backgroundTint", "#FF0000")));
        assertNull(
                MaterialButtonAttributes.generatedBackgroundTint(
                        layout, Map.of("android:backgroundTint", "#FF0000")));
    }

    @Test
    public void toolsReplaceSuppressesOnlyTheNamedTintAttribute() {
        var layout = new LayoutBean();
        layout.backgroundColor = 0xff244e40;

        assertNull(
                MaterialButtonAttributes.generatedBackgroundTint(
                        layout, Map.of("tools:replace", "android:background, app:backgroundTint")));
        assertEquals(
                "#244E40",
                MaterialButtonAttributes.generatedBackgroundTint(
                        layout, Map.of("tools:replace", "android:background")));
    }
}
