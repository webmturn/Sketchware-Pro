package pro.sketchware.core;

import com.besome.sketch.beans.BlockBean;

import java.util.ArrayList;

/**
 * Functional interface for generating Java code from a single block opcode.
 * Implementations are registered in {@link BlockCodeRegistry}.
 */
@FunctionalInterface
public interface BlockCodeHandler {
    /**
     * @param bean    the block being interpreted
     * @param params  already-resolved parameters for this block
     * @param context the interpreter instance (provides resolveBlock, activityName, buildConfig, etc.)
     * @return generated Java code string, or empty string for no-op
     */
    String generate(BlockBean bean, ArrayList<String> params, BlockInterpreter context);
}
