package pro.sketchware.core.block;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import pro.sketchware.beans.BlockBean;

public final class BlockTraversal {
    private BlockTraversal() {}

    public static List<BlockBean> reachableEnabledBlocks(List<BlockBean> blocks) {
        if (blocks.isEmpty()) return List.of();

        var byId = new HashMap<String, BlockBean>();
        for (BlockBean block : blocks) {
            byId.put(block.id, block);
        }

        var pending = new ArrayDeque<BlockBean>();
        var visited = new HashSet<BlockBean>();
        var enabled = new ArrayList<BlockBean>();
        pending.push(blocks.get(0));
        while (!pending.isEmpty()) {
            BlockBean block = pending.pop();
            if (!visited.add(block)) continue;

            // A disabled statement still links to the next statement in the same stack.
            if (block.nextBlock >= 0)
                pushIfPresent(pending, byId.get(Integer.toString(block.nextBlock)));
            if (block.disabled) continue;

            enabled.add(block);
            if (block.subStack2 >= 0)
                pushIfPresent(pending, byId.get(Integer.toString(block.subStack2)));
            if (block.subStack1 >= 0)
                pushIfPresent(pending, byId.get(Integer.toString(block.subStack1)));
            for (int i = block.parameters.size() - 1; i >= 0; i--) {
                String parameter = block.parameters.get(i);
                if (parameter != null && parameter.startsWith("@")) {
                    pushIfPresent(pending, byId.get(parameter.substring(1)));
                }
            }
        }
        return enabled;
    }

    private static void pushIfPresent(ArrayDeque<BlockBean> pending, BlockBean block) {
        if (block != null) pending.push(block);
    }
}
