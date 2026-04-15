package pro.sketchware.core;


import com.besome.sketch.beans.BlockBean;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mod.hey.studios.editor.manage.block.ExtraBlockInfo;
import mod.hey.studios.editor.manage.block.v2.BlockLoader;
import mod.hey.studios.moreblock.ReturnMoreblockManager;
import mod.pranav.viewbinding.ViewBindingBuilder;

/**
 * Converts a chain of {@link BlockBean}s into executable Java source code.
 * <p>
 * Each block's {@code opCode} is looked up in {@link BlockCodeRegistry} to find
 * the corresponding code template. Parameters are recursively resolved: nested
 * blocks (prefixed with {@code @}) are expanded in-place, strings are escaped
 * and quoted, numbers are validated, and view references are optionally
 * transformed to ViewBinding accessors.
 * <p>
 * Usage:
 * <pre>
 * BlockInterpreter interpreter = new BlockInterpreter(activityName, buildConfig, blocks, vbEnabled);
 * String javaCode = interpreter.interpretBlocks();
 * </pre>
 *
 * @see BlockCodeRegistry
 * @see BlockBean
 */
public class BlockInterpreter {

    private static final Pattern PARAM_PATTERN = Pattern.compile("%m(?!\\.[\\w]+)");
    private static final Pattern SPEC_PARAM_PATTERN = Pattern.compile("%[bdsm]");
    private static final Pattern EXTRACT_PARAMS_PATTERN = Pattern.compile("%\\w+(?:\\.\\w+)?|%\\w");
    public final boolean isViewBindingEnabled;
    public final CodeContext codeContext;
    public final String currentXmlName;
    private static final Set<String> viewParamsTypes = Set.of(
            "%m.view", "%m.layout", "%m.textview", "%m.button", "%m.edittext", "%m.imageview", "%m.recyclerview",
            "%m.listview", "%m.gridview", "%m.cardview", "%m.viewpager", "%m.webview", "%m.videoview", "%m.progressbar",
            "%m.seekbar", "%m.switch", "%m.checkbox", "%m.spinner", "%m.tablayout", "%m.bottomnavigation", "%m.adview",
            "%m.swiperefreshlayout", "%m.textinputlayout", "%m.ratingbar", "%m.datepicker", "%m.otpview", "%m.lottie",
            "%m.badgeview", "%m.codeview", "%m.patternview", "%m.signinbutton", "%m.youtubeview"
    );
    private static final Set<String> OPERATORS = Set.of("repeat", "+", "-", "*", "/", "%", ">", "=", "<", "&&", "||", "not");
    private static final Set<String> ARITHMETIC = Set.of("+", "-", "*", "/", "%", ">", "=", "<", "&&", "||");
    public String moreBlock = "";
    public String activityName;
    public BuildConfig buildConfig;
    public ArrayList<BlockBean> eventBlocks;
    public Map<String, BlockBean> blockMap;
    private final boolean isActivity;

    public BlockInterpreter(String activityName, BuildConfig buildConfig, ArrayList<BlockBean> eventBlocks, boolean isViewBindingEnabled) {
        this(activityName, buildConfig, eventBlocks, isViewBindingEnabled, null);
    }

    public BlockInterpreter(String activityName, BuildConfig buildConfig, ArrayList<BlockBean> eventBlocks, boolean isViewBindingEnabled, String currentXmlName) {
        this.activityName = activityName;

        isActivity = !(activityName.endsWith("DialogFragmentActivity") || activityName.endsWith("BottomDialogFragmentActivity") || activityName.endsWith("FragmentActivity"));
        this.codeContext = new CodeContext(activityName, !isActivity);

        this.buildConfig = buildConfig;
        this.eventBlocks = eventBlocks;
        this.isViewBindingEnabled = isViewBindingEnabled;
        this.currentXmlName = currentXmlName;
    }

    /**
     * Interprets the entire block chain starting from the first block,
     * building a map of all blocks by ID and recursively generating code.
     *
     * @return the generated Java code string, or empty string if no blocks exist
     */
    public String interpretBlocks() {
        blockMap = new HashMap<>();
        ArrayList<BlockBean> beans = eventBlocks;

        if (beans != null && !beans.isEmpty()) {
            for (BlockBean bean : eventBlocks) {
                blockMap.put(bean.id, bean);
            }

            return generateBlock(eventBlocks.get(0), "");
        } else {
            return "";
        }
    }

    /**
     * Generates Java code for a single block and its {@code nextBlock} chain.
     * Wraps arithmetic sub-expressions in parentheses when nested inside operators.
     *
     * @param bean          the block to generate code for
     * @param parentOpcode  the opCode of the parent block (for precedence handling)
     * @return the generated Java code fragment
     */
    public final String generateBlock(BlockBean bean, String parentOpcode) {
        if (bean.disabled) {
            if (bean.nextBlock >= 0) {
                return resolveBlock(String.valueOf(bean.nextBlock), moreBlock);
            }
            return "";
        }

        ArrayList<String> params = getBlockParams(bean);

        String opcode = getBlockCode(bean, params);

        String code = opcode;

        if (needsParentheses(bean.opCode, parentOpcode)) {
            code = "(" + opcode + ")";
        }

        if (bean.nextBlock >= 0) {
            code += (code.isEmpty() ? "" : "\r\n") + resolveBlock(String.valueOf(bean.nextBlock), moreBlock);
        }

        return code;
    }

    private boolean hasEmptySelectorParam(ArrayList<String> params, String spec) {
        var paramMatcher = PARAM_PATTERN.matcher(spec);
        if (!paramMatcher.find()) {
            var matcher = SPEC_PARAM_PATTERN.matcher(spec);
            int count = 0;
            ArrayList<Integer> selectorParamPositions = new ArrayList<>();
            while (matcher.find()) {
                String param = matcher.group();
                if ("%m".equals(param)) {
                    selectorParamPositions.add(count);
                }
                count++;
            }
            if (!selectorParamPositions.isEmpty()) {
                for (int position : selectorParamPositions) {
                    if (position >= params.size()) {
                        continue;
                    }
                    var param = params.get(position);
                    if (param == null || param.isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String escapeString(String input) {
        StringBuilder escapedString = new StringBuilder(4096);
        CharBuffer charBuffer = CharBuffer.wrap(input);

        for (int i = 0; i < charBuffer.length(); ++i) {
            char currentChar = charBuffer.get(i);
            if (currentChar == '"') {
                escapedString.append("\\\"");
            } else if (currentChar == '\\') {
                if (i < charBuffer.length() - 1) {
                    int nextIndex = i + 1;
                    currentChar = charBuffer.get(nextIndex);
                    if (currentChar != 'n' && currentChar != 't') {
                        escapedString.append("\\\\");
                    } else {
                        escapedString.append("\\").append(currentChar);
                        i = nextIndex;
                    }
                } else {
                    escapedString.append("\\\\");
                }
            } else if (currentChar == '\n') {
                escapedString.append("\\n");
            } else {
                escapedString.append(currentChar);
            }
        }

        return escapedString.toString();
    }

    /**
     * Resolves a single block parameter to its Java code representation.
     * <ul>
     *   <li>If the param starts with {@code @}, it references another block (recursive)</li>
     *   <li>Type 0 (boolean): returns the param as-is, defaults to {@code "true"}</li>
     *   <li>Type 1 (number): validates and appends {@code "d"} suffix for doubles</li>
     *   <li>Type 2 (string): escapes special characters and wraps in quotes</li>
     * </ul>
     *
     * @param param  the raw parameter value (may be a block reference like {@code "@5"})
     * @param type   the parameter type: 0=boolean, 1=number, 2=string, 3=other
     * @param opcode the parent block's opCode (passed to recursive resolution)
     * @return the resolved Java code for this parameter
     */
    public final String resolveParam(String param, int type, String opcode) {
        if (!param.isEmpty() && param.charAt(0) == '@') {
            opcode = resolveBlock(param.substring(1), opcode);
            if (type == 2 && opcode.isEmpty()) {
                return "\"\"";
            }
            return opcode;
        } else if (type == 2) {
            return "\"" + escapeString(param) + "\"";
        } else if (type == 1) {
            /**
             * Ideally, pro.sketchware.core.BlockInterpreter#resolveParam(BlockBean, String) should be responsible for parsing the input properly.
             * However, upon decompiling this class, it seems to completely ignore this case.
             * This is the solution for now to prevent errors during code generation.
             */
            try {
                if (param.isEmpty()) {
                    return "0";
                }
                if (param.contains(".")) {
                    Double.parseDouble(param);
                    return param + "d";
                }
                Integer.parseInt(param);
                return param;
            } catch (NumberFormatException e) {
                return param;
            }
        } else if (type == 0) {
            // Same fallback idea as the numeric branch above, but for booleans.
            if (param.isEmpty()) {
                return "true";
            }
        }
        return param;
    }

    /**
     * Resolves a block reference by its ID, generating code for the referenced block.
     *
     * @param blockId       the ID of the block to resolve
     * @param parentOpcode  the parent block's opCode (for precedence handling)
     * @return the generated code, or empty string if the block is not found
     */
    public final String resolveBlock(String blockId, String parentOpcode) {
        BlockBean block = blockMap.get(blockId);
        return block == null ? "" : generateBlock(block, parentOpcode);
    }

    /**
     * Determines if the current block needs parentheses to preserve operator precedence.
     *
     * @param opcode       the current block's opCode
     * @param parentOpcode the parent block's opCode
     * @return {@code true} if the parent is an operator and the child is arithmetic
     */
    public final boolean needsParentheses(String opcode, String parentOpcode) {
        return OPERATORS.contains(parentOpcode) && ARITHMETIC.contains(opcode);
    }

    /**
     * Resolves all parameters of a block to their Java code representations.
     * Each parameter is processed according to its type (extracted from the block spec)
     * and may involve recursive block resolution, ViewBinding transformation, or
     * color attribute resolution.
     *
     * @param bean the block whose parameters to resolve
     * @return list of resolved parameter code strings
     */
    public ArrayList<String> getBlockParams(BlockBean bean) {
        ArrayList<String> params = new ArrayList<>();
        ArrayList<String> paramsTypes = extractParamsTypes(bean.spec);
        for (int i = 0; i < bean.parameters.size(); i++) {
            String param = getParamValue(bean.parameters.get(i), paramsTypes.get(i));
            int type = getBlockType(bean, i);
            params.add(resolveParam(param, type, bean.opCode));
        }
        return params;
    }

    String getParamValue(String param, String type) {
        boolean isWidgetParam = viewParamsTypes.contains(type);
        boolean isColorParam = type.equals("%m.color");

        if (isWidgetParam) {
            String bindingStart = "binding.";
            if (isViewBindingEnabled && !param.isEmpty() && !param.startsWith("@") && !param.startsWith(bindingStart)) {
                return bindingStart + ViewBindingBuilder.generateParameterFromId(param);
            }
        } else if (isColorParam) {
            if (param.startsWith("R.color.")) {
                return "getResources().getColor(" + param + ")";
            }
            String context = isActivity ? activityName + ".this" : "getContext()";
            String attr = null;
            if (param.startsWith("R.attr.")) {
                attr = param;
            } else if (param.startsWith("getMaterialColor(") && param.endsWith(")")) {
                // to keep backward compatibility with old getMaterialColor calls
                attr = param.substring("getMaterialColor(".length(), param.length() - 1);
            }
            if (attr != null) {
                return String.format("SketchwareUtil.getMaterialColor(%s, %s)", context, attr);
            }
        }
        return param;
    }

    ArrayList<String> extractParamsTypes(String input) {
        ArrayList<String> matches = new ArrayList<>();
        Matcher matcher = EXTRACT_PARAMS_PATTERN.matcher(input);

        while (matcher.find()) {
            matches.add(matcher.group().toLowerCase());
        }

        return matches;
    }

    private String getBlockCode(BlockBean bean, ArrayList<String> params) {
        String opcode = "";

        BlockCodeHandler handler = BlockCodeRegistry.get(bean.opCode);
        if (handler != null) {
            opcode = handler.generate(bean, params, this);
        } else {
            opcode = getCodeExtraBlock(bean, params);
        }

        /*
          switch block above should be responsible for handling %m param.
          However, upon decompiling this class, it completely ignore this case.
          This is the solution for now to prevent errors during code generation.
         */
        if (hasEmptySelectorParam(params, bean.spec)) {
            opcode = "";
        }
        return opcode;
    }

    private String getCodeExtraBlock(BlockBean blockBean, ArrayList<String> resolvedParams) {
        ArrayList<String> parameters = new ArrayList<>(resolvedParams);

        if (blockBean.subStack1 >= 0) {
            parameters.add(resolveBlock(String.valueOf(blockBean.subStack1), blockBean.opCode));
        } else {
            parameters.add("");
        }

        if (blockBean.subStack2 >= 0) {
            parameters.add(resolveBlock(String.valueOf(blockBean.subStack2), blockBean.opCode));
        } else {
            parameters.add("");
        }

        ExtraBlockInfo blockInfo = BlockLoader.getBlockInfo(blockBean.opCode);

        if (blockInfo.isMissing) {
            blockInfo = BlockLoader.getBlockFromProject(buildConfig.sc_id, blockBean.opCode);
        }

        String formattedCode;
        if (!parameters.isEmpty()) {
            try {
                formattedCode = String.format(blockInfo.getCode(), parameters.toArray(new Object[0]));
            } catch (Exception e) {
                formattedCode = "/* Failed to resolve Custom Block's code: " + e + " */";
            }
        } else {
            formattedCode = blockInfo.getCode();
        }

        return formattedCode;
    }

    private int getBlockType(BlockBean blockBean, int parameterIndex) {
        int blockType;

        ClassInfo classInfo = blockBean.getParamClassInfo().get(parameterIndex);

        if (classInfo.isExactType("boolean")) {
            blockType = 0;
        } else if (classInfo.isExactType("double")) {
            blockType = 1;
        } else if (classInfo.isExactType("String")) {
            blockType = 2;
        } else {
            blockType = 3;
        }

        return blockType;
    }
}
