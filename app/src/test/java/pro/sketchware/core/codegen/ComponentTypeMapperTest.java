package pro.sketchware.core.codegen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import pro.sketchware.core.project.ClassInfo;

/** Protects numeric and textual type identifiers used by legacy Sketchware project files. */
public class ComponentTypeMapperTest {

    @Test
    public void preservesLegacyVariableTypeIds() {
        assertEquals("boolean", ComponentTypeMapper.getVariableTypeName(0));
        assertEquals("double", ComponentTypeMapper.getVariableTypeName(1));
        assertEquals("String", ComponentTypeMapper.getVariableTypeName(2));
        assertEquals("Map", ComponentTypeMapper.getVariableTypeName(3));
        assertEquals("", ComponentTypeMapper.getVariableTypeName(Integer.MAX_VALUE));
    }

    @Test
    public void preservesLegacyListTypeIds() {
        assertEquals("ListInt", ComponentTypeMapper.getListInternalName(1));
        assertEquals("ListString", ComponentTypeMapper.getListInternalName(2));
        assertEquals("ListMap", ComponentTypeMapper.getListInternalName(3));
        assertEquals("", ComponentTypeMapper.getListInternalName(Integer.MAX_VALUE));
    }

    @Test
    public void mapsStoredBlockTypesToInternalClasses() {
        assertEquals("boolean", ComponentTypeMapper.getClassInfo("b", "").getClassName());
        assertEquals("double", ComponentTypeMapper.getClassInfo("d", "").getClassName());
        assertEquals("String", ComponentTypeMapper.getClassInfo("s", "").getClassName());
        assertEquals("ListMap", ComponentTypeMapper.getClassInfo("l", "List Map").getClassName());
    }

    @Test public void canonicalizesViewAndMenuItemSelectorAliases() {
        for (String alias : new String[]{"view", "View"}) {
            assertEquals("View", ComponentTypeMapper.getInternalTypeName(alias));
            assertEquals("View", ComponentTypeMapper.getClassInfo("m", alias).getClassName());
        }
        for (String alias : new String[]{"menuitem", "MenuItem"}) {
            assertEquals("MenuItem", ComponentTypeMapper.getInternalTypeName(alias));
        }
    }

    @Test public void baseViewHierarchyDoesNotMatchAnEmptyType() {
        ClassInfo view = ComponentTypeMapper.getClassInfo("v", "View");
        assertTrue(view.isView());
        assertTrue(view.matchesType("!"));
        assertFalse(view.matchesType(""));
        assertFalse(view.matchesType("MenuItem"));
        assertFalse(view.matchesType("TextView"));
        assertEquals("View", view.classPath[0]);
    }

    @Test public void viewSubclassesStillMatchTheirBaseTypes() {
        ClassInfo button = ComponentTypeMapper.getClassInfo("v", "Button");
        assertTrue(button.matchesType("View"));
        assertTrue(button.matchesType("TextView"));
        assertTrue(button.matchesType("Button"));
        assertFalse(button.matchesType("MenuItem"));
    }
}
