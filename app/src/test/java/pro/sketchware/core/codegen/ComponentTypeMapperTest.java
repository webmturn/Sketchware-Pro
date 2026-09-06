package pro.sketchware.core.codegen;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

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
}
