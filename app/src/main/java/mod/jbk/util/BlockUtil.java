package mod.jbk.util;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import a.a.a.FormatUtil;
import a.a.a.BlockView;
import a.a.a.BaseBlockView;
import a.a.a.BlockColorMapper;
import mod.hey.studios.moreblock.ReturnMoreblockManager;

public class BlockUtil {
    public static void loadMoreblockPreview(ViewGroup blockArea, String spec) {
        var moreblock = new BlockView(blockArea.getContext(), 0, ReturnMoreblockManager.getMbName(spec), ReturnMoreblockManager.getMoreblockType(spec), "definedFunc");
        blockArea.addView(moreblock);

        loadPreviewBlockVariables(blockArea, moreblock, spec);
        moreblock.k();
    }

    /**
     * Loads the Variable Blocks of a Block that's for preview only.
     */
    public static void loadPreviewBlockVariables(ViewGroup blockArea, BlockView previewBlock, String spec) {
        int id = 0;
        for (var specPart : FormatUtil.c(spec)) {
            if (specPart.charAt(0) != '%') {
                continue;
            }

            var variable = getVariableBlock(blockArea.getContext(), id + 1, specPart, "getVar");
            if (variable != null) {
                blockArea.addView(variable);
                previewBlock.a((BaseBlockView) previewBlock.V.get(id), variable);
                id++;
            }
        }
    }

    /**
     * @param opCode Block op code like <code>"getArg"</code> (used in Events' heading/start Block)
     *               or <code>"getVar"</code> (type of Blocks in the Palette)
     * @return The Variable Block that's part of for example a MoreBlock or an Event,
     * or <code>null</code> if its spec wasn't recognized.
     */
    @Nullable
    public static BlockView getVariableBlock(Context context, int id, String spec, String opCode) {
        var type = spec.charAt(1);
        return switch (type) {
            case 'b', 'd', 's' ->
                    new BlockView(context, id, spec.substring(3), Character.toString(type), opCode);
            case 'm' -> {
                String specLast = spec.substring(spec.lastIndexOf(".") + 1);
                String specFirst = spec.substring(spec.indexOf(".") + 1, spec.lastIndexOf("."));
                yield new BlockView(context, id, specLast, BlockColorMapper.a(specFirst), BlockColorMapper.b(specFirst), opCode);
            }
            default -> null;
        };
    }
}
