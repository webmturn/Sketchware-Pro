package pro.sketchware.core.codegen;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import pro.sketchware.beans.BlockBean;

public class DisabledBlockCodeTest {
    @Test
    public void disablingEveryHandlerKeepsTheTryBodyInItsOwnScope() {
        BlockBean attempt = block(1, "tryCatch", "try");
        attempt.subStack1 = 2;
        attempt.subStack2 = 3;
        attempt.nextBlock = 5;
        BlockBean body = source(2, "int value = 1;");
        BlockBean handler = block(3, "catchBlock", "catch %s.inputOnly");
        handler.disabled = true;
        handler.nextBlock = 4;
        BlockBean cleanup = block(4, "finallyBlock", "finally");
        cleanup.disabled = true;
        BlockBean next = source(5, "int value = 2;");

        assertEquals(
                "{\r\nint value = 1;\r\n}\r\nint value = 2;",
                generate(attempt, body, handler, cleanup, next));

        cleanup.disabled = false;
        assertEquals(
                "try {\r\nint value = 1;\r\n} finally {\r\n\r\n}\r\nint value = 2;",
                generate(attempt, body, handler, cleanup, next));

        handler.disabled = false;
        assertEquals(
                "try {\r\nint value = 1;\r\n} catch (Exception e) {\r\n\r\n}\r\n"
                        + "finally {\r\n\r\n}\r\nint value = 2;",
                generate(attempt, body, handler, cleanup, next));
    }

    @Test
    public void disablingAContainerSkipsItsParametersAndSubstacksButKeepsItsSuccessor() {
        BlockBean branch = block(1, "ifElse", "if %b");
        branch.parameters.add("@2");
        branch.subStack1 = 3;
        branch.subStack2 = 4;
        branch.nextBlock = 5;
        branch.disabled = true;
        BlockBean condition = block(2, "true", "true");
        BlockBean yes = source(3, "int yes = 1;");
        BlockBean no = source(4, "int no = 2;");
        BlockBean next = source(5, "int next = 3;");

        assertEquals("int next = 3;", generate(branch, condition, yes, no, next));
    }

    private static BlockBean source(int id, String code) {
        BlockBean block = block(id, "addSourceDirectly", "add source %s");
        block.parameters.add(code);
        return block;
    }

    private static BlockBean block(int id, String opcode, String spec) {
        BlockBean block = new BlockBean();
        block.id = Integer.toString(id);
        block.opCode = opcode;
        block.spec = spec;
        block.type = " ";
        block.typeName = "";
        return block;
    }

    private static String generate(BlockBean... blocks) {
        return new BlockInterpreter("MainActivity", null, new ArrayList<>(List.of(blocks)), false)
                .interpretBlocks();
    }
}
