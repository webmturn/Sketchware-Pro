package pro.sketchware.core;

import android.util.Pair;

import com.besome.sketch.beans.BlockBean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import mod.hey.studios.moreblock.ReturnMoreblockManager;

/**
 * Registry-based replacement for the giant switch in {@link BlockInterpreter#getBlockCode}.
 * Each opcode is mapped to a {@link BlockCodeHandler} that generates the corresponding Java code.
 * <p>
 * To add a new built-in block, call {@link #register(String, BlockCodeHandler)} — no need to
 * modify BlockInterpreter's switch statement.
 */
public class BlockCodeRegistry {

    private static final Map<String, BlockCodeHandler> handlers = new HashMap<>(256);

    static {
        registerCoreBlocks();
        registerOperatorBlocks();
        registerStringBlocks();
        registerCollectionBlocks();
        registerMathBlocks();
        registerControlFlowBlocks();
        registerDrawerBlocks();
        registerViewBlocks();
        registerIntentBlocks();
        registerSharedPrefsBlocks();
        registerCalendarBlocks();
        registerListWidgetBlocks();
        registerWebViewBlocks();
        registerCalendarViewBlocks();
        registerAdViewBlocks();
        registerMapViewBlocks();
        registerTimerBlocks();
        registerFirebaseBlocks();
        registerFirebaseAuthBlocks();
        registerDialogBlocks();
        registerMediaBlocks();
        registerSeekBarBlocks();
        registerAnimationBlocks();
        registerFirebaseStorageBlocks();
        registerFileUtilBlocks();
        registerImageBlocks();
        registerNetworkBlocks();
        registerSpeechBlocks();
        registerBluetoothBlocks();
        registerLocationBlocks();
        registerNotificationBlocks();
        registerSQLiteBlocks();
        registerMiscBlocks();
    }

    public static void register(String opcode, BlockCodeHandler handler) {
        handlers.put(opcode, handler);
    }

    public static BlockCodeHandler get(String opcode) {
        return handlers.get(opcode);
    }

    private static void registerCoreBlocks() {
        register("definedFunc", (bean, params, ctx) -> {
            int space = bean.spec.indexOf(" ");
            String opcode;
            if (bean.parameters.isEmpty()) {
                opcode = bean.type;
                ctx.moreBlock = "_" + (space < 0 ? bean.spec : bean.spec.substring(0, space)) + "()" + ReturnMoreblockManager.getMbEnd(bean.type);
            } else {
                ArrayList<String> paramsTypes = ctx.extractParamsTypes(bean.spec);
                StringBuilder sb = new StringBuilder("_").append(bean.spec, 0, space).append("(");
                boolean hasStringParam = false;
                for (int i = 0; i < params.size(); i++) {
                    if (i > 0) sb.append(", ");
                    String param = ctx.getParamValue(params.get(i), paramsTypes.get(i));
                    if (param.isEmpty()) {
                        ClassInfo info = bean.getParamClassInfo().get(i);
                        if (info.isExactType("boolean")) {
                            sb.append("true");
                        } else if (info.isExactType("double")) {
                            sb.append("0");
                        } else if (info.isExactType("String")) {
                            hasStringParam = true;
                        }
                    } else {
                        sb.append(param);
                    }
                }
                opcode = sb.toString();
                ctx.moreBlock = opcode + ")" + ReturnMoreblockManager.getMbEnd(bean.type);
                if (hasStringParam) ctx.moreBlock = bean.type;
            }
            String op = opcode;
            opcode = ctx.moreBlock;
            ctx.moreBlock = op;
            return opcode;
        });
        register("getArg", (bean, params, ctx) -> "_" + bean.spec);
        register("getVar", (bean, params, ctx) -> bean.spec);
        register("getResStr", (bean, params, ctx) -> "Helper.getResString(R.string." + bean.spec + ")");
        register("setVarBoolean", (bean, params, ctx) -> String.format("%s = %s;", params.get(0), params.get(1)));
        register("setVarInt", (bean, params, ctx) -> String.format("%s = %s;", params.get(0), params.get(1)));
        register("setVarString", (bean, params, ctx) -> String.format("%s = %s;", params.get(0), params.get(1)));
        register("increaseInt", (bean, params, ctx) -> String.format("%s++;", params.get(0)));
        register("decreaseInt", (bean, params, ctx) -> String.format("%s--;", params.get(0)));
        register("addSourceDirectly", (bean, params, ctx) -> {
            String asd = bean.parameters.get(0);
            return (asd != null) ? asd : "";
        });
    }
    private static void registerOperatorBlocks() {
        register("true", (bean, params, ctx) -> "true");
        register("false", (bean, params, ctx) -> "false");
        register("not", (bean, params, ctx) -> String.format("!%s", params.get(0)));
        register("+", (bean, params, ctx) -> String.format("%s + %s", params.get(0), params.get(1)));
        register("-", (bean, params, ctx) -> String.format("%s - %s", params.get(0), params.get(1)));
        register("*", (bean, params, ctx) -> String.format("%s * %s", params.get(0), params.get(1)));
        register("/", (bean, params, ctx) -> String.format("%s / %s", params.get(0), params.get(1)));
        register("%", (bean, params, ctx) -> String.format("%s %% %s", params.get(0), params.get(1)));
        register(">", (bean, params, ctx) -> String.format("%s > %s", params.get(0), params.get(1)));
        register("<", (bean, params, ctx) -> String.format("%s < %s", params.get(0), params.get(1)));
        register("=", (bean, params, ctx) -> String.format("%s == %s", params.get(0), params.get(1)));
        register("&&", (bean, params, ctx) -> String.format("%s && %s", params.get(0), params.get(1)));
        register("||", (bean, params, ctx) -> String.format("%s || %s", params.get(0), params.get(1)));
        register("random", (bean, params, ctx) -> String.format("SketchwareUtil.getRandom((int)(%s), (int)(%s))", params.get(0), params.get(1)));
    }
    private static void registerStringBlocks() {
        register("stringLength", (bean, params, ctx) -> String.format("%s.length()", params.get(0)));
        register("stringJoin", (bean, params, ctx) -> String.format("%s.concat(%s)", params.get(0), params.get(1)));
        register("stringIndex", (bean, params, ctx) -> String.format("%s.indexOf(%s)", params.get(1), params.get(0)));
        register("stringLastIndex", (bean, params, ctx) -> String.format("%s.lastIndexOf(%s)", params.get(1), params.get(0)));
        register("stringSub", (bean, params, ctx) -> String.format("%s.substring((int)(%s), (int)(%s))", params.get(0), params.get(1), params.get(2)));
        register("stringEquals", (bean, params, ctx) -> String.format("%s.equals(%s)", params.get(0), params.get(1)));
        register("stringContains", (bean, params, ctx) -> String.format("%s.contains(%s)", params.get(0), params.get(1)));
        register("stringReplace", (bean, params, ctx) -> String.format("%s.replace(%s, %s)", params.get(0), params.get(1), params.get(2)));
        register("stringReplaceFirst", (bean, params, ctx) -> String.format("%s.replaceFirst(%s, %s)", params.get(0), params.get(1), params.get(2)));
        register("stringReplaceAll", (bean, params, ctx) -> String.format("%s.replaceAll(%s, %s)", params.get(0), params.get(1), params.get(2)));
        register("trim", (bean, params, ctx) -> String.format("%s.trim()", params.get(0)));
        register("toUpperCase", (bean, params, ctx) -> String.format("%s.toUpperCase()", params.get(0)));
        register("toLowerCase", (bean, params, ctx) -> String.format("%s.toLowerCase()", params.get(0)));
        register("toString", (bean, params, ctx) -> String.format("String.valueOf((long)(%s))", params.get(0)));
        register("toStringWithDecimal", (bean, params, ctx) -> String.format("String.valueOf(%s)", params.get(0)));
        register("toStringFormat", (bean, params, ctx) -> String.format("new DecimalFormat(%s).format(%s)", params.get(1), params.get(0)));
        register("toNumber", (bean, params, ctx) -> {
            String doub = params.get(0);
            doub = (!doub.equals("\"\"")) ? doub : "\"0\"";
            return String.format("Double.parseDouble(%s)", doub);
        });
        register("currentTime", (bean, params, ctx) -> "System.currentTimeMillis()");
        register("strToMap", (bean, params, ctx) -> String.format("%s = new Gson().fromJson(%s, new TypeToken<HashMap<String, Object>>(){}.getType());", params.get(1), params.get(0)));
        register("mapToStr", (bean, params, ctx) -> String.format("new Gson().toJson(%s)", params.get(0)));
        register("listMapToStr", (bean, params, ctx) -> String.format("new Gson().toJson(%s)", params.get(0)));
        register("strToListMap", (bean, params, ctx) -> String.format("%s = new Gson().fromJson(%s, new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());", params.get(1), params.get(0)));
    }

    private static void registerCollectionBlocks() {
        register("mapCreateNew", (bean, params, ctx) -> String.format("%s = new HashMap<>();", params.get(0)));
        register("mapPut", (bean, params, ctx) -> String.format("%s.put(%s, %s);", params.get(0), params.get(1), params.get(2)));
        register("mapGet", (bean, params, ctx) -> String.format("%s.get(%s).toString()", params.get(0), params.get(1)));
        register("mapContainKey", (bean, params, ctx) -> String.format("%s.containsKey(%s)", params.get(0), params.get(1)));
        register("mapRemoveKey", (bean, params, ctx) -> String.format("%s.remove(%s);", params.get(0), params.get(1)));
        register("mapSize", (bean, params, ctx) -> String.format("%s.size()", params.get(0)));
        register("lengthList", (bean, params, ctx) -> String.format("%s.size()", params.get(0)));
        register("mapClear", (bean, params, ctx) -> String.format("%s.clear();", params.get(0)));
        register("clearList", (bean, params, ctx) -> String.format("%s.clear();", params.get(0)));
        register("mapIsEmpty", (bean, params, ctx) -> String.format("%s.isEmpty()", params.get(0)));
        register("mapGetAllKeys", (bean, params, ctx) -> String.format("SketchwareUtil.getAllKeysFromMap(%s, %s);", params.get(0), params.get(1)));
        register("addListInt", (bean, params, ctx) -> String.format("%s.add(Double.valueOf(%s));", params.get(1), params.get(0)));
        register("insertListInt", (bean, params, ctx) -> String.format("%s.add((int)(%s), Double.valueOf(%s));", params.get(2), params.get(1), params.get(0)));
        register("getAtListInt", (bean, params, ctx) -> String.format("%s.get((int)(%s)).doubleValue()", params.get(1), params.get(0)));
        register("indexListInt", (bean, params, ctx) -> String.format("%s.indexOf(%s)", params.get(1), params.get(0)));
        register("indexListStr", (bean, params, ctx) -> String.format("%s.indexOf(%s)", params.get(1), params.get(0)));
        register("containListInt", (bean, params, ctx) -> String.format("%s.contains(%s)", params.get(0), params.get(1)));
        register("containListStr", (bean, params, ctx) -> String.format("%s.contains(%s)", params.get(0), params.get(1)));
        register("addListStr", (bean, params, ctx) -> String.format("%s.add(%s);", params.get(1), params.get(0)));
        register("addMapToList", (bean, params, ctx) -> String.format("%s.add(%s);", params.get(1), params.get(0)));
        register("insertListStr", (bean, params, ctx) -> String.format("%s.add((int)(%s), %s);", params.get(2), params.get(1), params.get(0)));
        register("getAtListStr", (bean, params, ctx) -> String.format("%s.get((int)(%s))", params.get(1), params.get(0)));
        register("addListMap", (bean, params, ctx) -> String.format("{\r\nHashMap<String, Object> _item = new HashMap<>();\r\n_item.put(%s, %s);\r\n%s.add(_item);\r\n}", params.get(0), params.get(1), params.get(2)));
        register("insertListMap", (bean, params, ctx) -> String.format("{\r\nHashMap<String, Object> _item = new HashMap<>();\r\n_item.put(%s, %s);\r\n%s.add((int)%s, _item);\r\n}", params.get(0), params.get(1), params.get(3), params.get(2)));
        register("getAtListMap", (bean, params, ctx) -> String.format("%s.get((int)%s).get(%s).toString()", params.get(2), params.get(0), params.get(1)));
        register("setListMap", (bean, params, ctx) -> String.format("%s.get((int)%s).put(%s, %s);", params.get(3), params.get(2), params.get(0), params.get(1)));
        register("containListMap", (bean, params, ctx) -> String.format("%s.get((int)%s).containsKey(%s)", params.get(0), params.get(1), params.get(2)));
        register("insertMapToList", (bean, params, ctx) -> String.format("%s.add((int)%s, %s);", params.get(2), params.get(1), params.get(0)));
        register("getMapInList", (bean, params, ctx) -> String.format("%s = %s.get((int)%s);", params.get(2), params.get(1), params.get(0)));
        register("deleteList", (bean, params, ctx) -> String.format("%s.remove((int)(%s));", params.get(1), params.get(0)));
    }

    private static void registerMathBlocks() {
        register("mathGetDip", (bean, params, ctx) -> String.format("SketchwareUtil.getDip(%s, (int)(%s))", ctx.codeContext.appContext(), params.get(0)));
        register("mathGetDisplayWidth", (bean, params, ctx) -> "SketchwareUtil.getDisplayWidthPixels(" + ctx.codeContext.appContext() + ")");
        register("mathGetDisplayHeight", (bean, params, ctx) -> "SketchwareUtil.getDisplayHeightPixels(" + ctx.codeContext.appContext() + ")");
        register("mathPi", (bean, params, ctx) -> "Math.PI");
        register("mathE", (bean, params, ctx) -> "Math.E");
        register("mathPow", (bean, params, ctx) -> String.format("Math.pow(%s, %s)", params.get(0), params.get(1)));
        register("mathMin", (bean, params, ctx) -> String.format("Math.min(%s, %s)", params.get(0), params.get(1)));
        register("mathMax", (bean, params, ctx) -> String.format("Math.max(%s, %s)", params.get(0), params.get(1)));
        register("mathSqrt", (bean, params, ctx) -> String.format("Math.sqrt(%s)", params.get(0)));
        register("mathAbs", (bean, params, ctx) -> String.format("Math.abs(%s)", params.get(0)));
        register("mathRound", (bean, params, ctx) -> String.format("Math.round(%s)", params.get(0)));
        register("mathCeil", (bean, params, ctx) -> String.format("Math.ceil(%s)", params.get(0)));
        register("mathFloor", (bean, params, ctx) -> String.format("Math.floor(%s)", params.get(0)));
        register("mathSin", (bean, params, ctx) -> String.format("Math.sin(%s)", params.get(0)));
        register("mathCos", (bean, params, ctx) -> String.format("Math.cos(%s)", params.get(0)));
        register("mathTan", (bean, params, ctx) -> String.format("Math.tan(%s)", params.get(0)));
        register("mathAsin", (bean, params, ctx) -> String.format("Math.asin(%s)", params.get(0)));
        register("mathAcos", (bean, params, ctx) -> String.format("Math.acos(%s)", params.get(0)));
        register("mathAtan", (bean, params, ctx) -> String.format("Math.atan(%s)", params.get(0)));
        register("mathExp", (bean, params, ctx) -> String.format("Math.exp(%s)", params.get(0)));
        register("mathLog", (bean, params, ctx) -> String.format("Math.log(%s)", params.get(0)));
        register("mathLog10", (bean, params, ctx) -> String.format("Math.log10(%s)", params.get(0)));
        register("mathToRadian", (bean, params, ctx) -> String.format("Math.toRadians(%s)", params.get(0)));
        register("mathToDegree", (bean, params, ctx) -> String.format("Math.toDegrees(%s)", params.get(0)));
    }
    private static void registerControlFlowBlocks() {
        register("forever", (bean, params, ctx) -> {
            int stack = bean.subStack1;
            return String.format("while(true) {\r\n%s\r\n}", stack >= 0 ? ctx.resolveBlock(String.valueOf(stack), "") : "");
        });
        register("repeat", (bean, params, ctx) -> {
            int stack = bean.subStack1;
            return String.format("for(int _repeat%s = 0; _repeat%s < (int)(%s); _repeat%s++) {\n%s\n}",
                    bean.id, bean.id, params.get(0), bean.id,
                    stack >= 0 ? ctx.resolveBlock(String.valueOf(stack), "") : "");
        });
        register("if", (bean, params, ctx) -> {
            int stack = bean.subStack1;
            return String.format("if (%s) {\r\n%s\r\n}", params.get(0), stack >= 0 ? ctx.resolveBlock(String.valueOf(stack), "") : "");
        });
        register("ifElse", (bean, params, ctx) -> {
            String ifBlock = bean.subStack1 >= 0 ? ctx.resolveBlock(String.valueOf(bean.subStack1), "") : "";
            String elseBlock = bean.subStack2 >= 0 ? ctx.resolveBlock(String.valueOf(bean.subStack2), "") : "";
            return String.format("if (%s) {\r\n%s\r\n} else {\r\n%s\r\n}", params.get(0), ifBlock, elseBlock);
        });
        register("break", (bean, params, ctx) -> "break;");
    }

    private static void registerDrawerBlocks() {
        register("isDrawerOpen", (bean, params, ctx) -> {
            if (ctx.buildConfig.getActivityPermissions(ctx.activityName).hasDrawer) {
                return ctx.isViewBindingEnabled ? "binding.Drawer.isDrawerOpen(GravityCompat.START)" : "_drawer.isDrawerOpen(GravityCompat.START)";
            }
            return "";
        });
        register("openDrawer", (bean, params, ctx) -> {
            if (ctx.buildConfig.getActivityPermissions(ctx.activityName).hasDrawer) {
                return ctx.isViewBindingEnabled ? "binding.Drawer.openDrawer(GravityCompat.START);" : "_drawer.openDrawer(GravityCompat.START);";
            }
            return "";
        });
        register("closeDrawer", (bean, params, ctx) -> {
            if (ctx.buildConfig.getActivityPermissions(ctx.activityName).hasDrawer) {
                return ctx.isViewBindingEnabled ? "binding.Drawer.closeDrawer(GravityCompat.START);" : "_drawer.closeDrawer(GravityCompat.START);";
            }
            return "";
        });
    }

    private static void registerViewBlocks() {
        register("setEnable", (bean, params, ctx) -> String.format("%s.setEnabled(%s);", params.get(0), params.get(1)));
        register("getEnable", (bean, params, ctx) -> String.format("%s.isEnabled()", params.get(0)));
        register("setText", (bean, params, ctx) -> String.format("%s.setText(%s);", params.get(0), params.get(1)));
        register("getText", (bean, params, ctx) -> String.format("%s.getText().toString()", params.get(0)));
        register("setTypeface", (bean, params, ctx) -> {
            String textStyle = params.get(2);
            String styleValue = "";
            Pair<Integer, String>[] styles = SketchwareConstants.getPropertyPairs("property_text_style");
            for (Pair<Integer, String> style : styles) {
                if (style.second.equals(textStyle)) {
                    styleValue = String.valueOf(style.first);
                    break;
                }
            }
            String fontName = params.get(1);
            if ("default_font".equals(fontName)) {
                return String.format("%s.setTypeface(Typeface.DEFAULT, %s);", params.get(0), styleValue);
            } else {
                return String.format("%s.setTypeface(Typeface.createFromAsset(%s,\"fonts/%s.ttf\"), %s);", params.get(0), ctx.codeContext.assets(), fontName, styleValue);
            }
        });
        register("setBgColor", (bean, params, ctx) -> String.format("%s.setBackgroundColor(%s);", params.get(0), params.get(1)));
        register("setBgResource", (bean, params, ctx) -> {
            String res = params.get(1).equals("NONE") ? "0" : "R.drawable." + params.get(1).replaceAll("\\.9", "");
            return String.format("%s.setBackgroundResource(%s);", params.get(0), res);
        });
        register("setTextColor", (bean, params, ctx) -> String.format("%s.setTextColor(%s);", params.get(0), params.get(1)));
        register("setImage", (bean, params, ctx) -> {
            String name = params.get(1).replaceAll("\\.9", "");
            return String.format("%s.setImageResource(R.drawable.%s);", params.get(0), name.toLowerCase());
        });
        register("setColorFilter", (bean, params, ctx) -> String.format("%s.setColorFilter(%s, PorterDuff.Mode.MULTIPLY);", params.get(0), params.get(1)));
        register("requestFocus", (bean, params, ctx) -> String.format("%s.requestFocus();", params.get(0)));
        register("setVisible", (bean, params, ctx) -> String.format("%s.setVisibility(View.%s);", params.get(0), params.get(1)));
        register("setClickable", (bean, params, ctx) -> String.format("%s.setClickable(%s);", params.get(0), params.get(1)));
        register("setRotate", (bean, params, ctx) -> String.format("%s.setRotation((float)(%s));", params.get(0), params.get(1)));
        register("getRotate", (bean, params, ctx) -> String.format("%s.getRotation()", params.get(0)));
        register("setAlpha", (bean, params, ctx) -> String.format("%s.setAlpha((float)(%s));", params.get(0), params.get(1)));
        register("getAlpha", (bean, params, ctx) -> String.format("%s.getAlpha()", params.get(0)));
        register("setTranslationX", (bean, params, ctx) -> String.format("%s.setTranslationX((float)(%s));", params.get(0), params.get(1)));
        register("getTranslationX", (bean, params, ctx) -> String.format("%s.getTranslationX()", params.get(0)));
        register("setTranslationY", (bean, params, ctx) -> String.format("%s.setTranslationY((float)(%s));", params.get(0), params.get(1)));
        register("getTranslationY", (bean, params, ctx) -> String.format("%s.getTranslationY()", params.get(0)));
        register("setScaleX", (bean, params, ctx) -> String.format("%s.setScaleX((float)(%s));", params.get(0), params.get(1)));
        register("getScaleX", (bean, params, ctx) -> String.format("%s.getScaleX()", params.get(0)));
        register("setScaleY", (bean, params, ctx) -> String.format("%s.setScaleY((float)(%s));", params.get(0), params.get(1)));
        register("getScaleY", (bean, params, ctx) -> String.format("%s.getScaleY()", params.get(0)));
        register("getLocationX", (bean, params, ctx) -> String.format("SketchwareUtil.getLocationX(%s)", params.get(0)));
        register("getLocationY", (bean, params, ctx) -> String.format("SketchwareUtil.getLocationY(%s)", params.get(0)));
        register("setChecked", (bean, params, ctx) -> String.format("%s.setChecked(%s);", params.get(0), params.get(1)));
        register("getChecked", (bean, params, ctx) -> String.format("%s.isChecked()", params.get(0)));
        register("setHint", (bean, params, ctx) -> !params.get(0).equals("\"\"") ? String.format("%s.setHint(%s);", params.get(0), params.get(1)) : "");
        register("setHintTextColor", (bean, params, ctx) -> !params.get(1).equals("\"\"") ? String.format("%s.setHintTextColor(%s);", params.get(0), params.get(1)) : "");
        register("viewOnClick", (bean, params, ctx) -> {
            String listener = bean.subStack1 >= 0 ? ctx.resolveBlock(String.valueOf(bean.subStack1), "") : "";
            return String.format("%s.setOnClickListener(new View.OnClickListener() {\n@Override\npublic void onClick(View _view) {\n%s\n}\n});", params.get(0), listener);
        });
        register("progressBarSetIndeterminate", (bean, params, ctx) -> String.format("%s.setIndeterminate(%s);", params.get(0), params.get(1)));
    }

    private static void registerIntentBlocks() {
        register("intentSetAction", (bean, params, ctx) -> String.format("%s.setAction(%s);", params.get(0), (params.get(1).equals("\"\"") ? "" : "Intent." + params.get(1))));
        register("intentSetData", (bean, params, ctx) -> String.format("%s.setData(Uri.parse(%s));", params.get(0), params.get(1)));
        register("intentSetScreen", (bean, params, ctx) -> String.format("%s.setClass(%s, %s.class);", params.get(0), ctx.codeContext.appContext(), params.get(1)));
        register("intentPutExtra", (bean, params, ctx) -> String.format("%s.putExtra(%s, %s);", params.get(0), params.get(1), params.get(2)));
        register("intentSetFlags", (bean, params, ctx) -> String.format("%s.setFlags(%s);", params.get(0), "Intent.FLAG_ACTIVITY_" + params.get(1)));
        register("intentGetString", (bean, params, ctx) -> String.format("getIntent().getStringExtra(%s)", params.get(0)));
        register("startActivity", (bean, params, ctx) -> String.format("startActivity(%s);", params.get(0)));
        register("finishActivity", (bean, params, ctx) -> "finish();");
    }

    private static void registerSharedPrefsBlocks() {
        register("fileSetFileName", (bean, params, ctx) -> String.format("%s = %s(%s, Activity.MODE_PRIVATE);", params.get(0), ctx.codeContext.sharedPreferences(), params.get(1)));
        register("fileGetData", (bean, params, ctx) -> String.format("%s.getString(%s, \"\")", params.get(0), params.get(1)));
        register("fileSetData", (bean, params, ctx) -> String.format("%s.edit().putString(%s, %s).apply();", params.get(0), params.get(1), params.get(2)));
        register("fileRemoveData", (bean, params, ctx) -> String.format("%s.edit().remove(%s).apply();", params.get(0), params.get(1)));
    }

    private static void registerCalendarBlocks() {
        register("calendarGetNow", (bean, params, ctx) -> String.format("%s = Calendar.getInstance();", params.get(0)));
        register("calendarAdd", (bean, params, ctx) -> String.format("%s.add(Calendar.%s, (int)(%s));", params.get(0), params.get(1), params.get(2)));
        register("calendarSet", (bean, params, ctx) -> String.format("%s.set(Calendar.%s, (int)(%s));", params.get(0), params.get(1), params.get(2)));
        register("calendarFormat", (bean, params, ctx) -> String.format("new SimpleDateFormat(%s).format(%s.getTime())", (!params.get(1).equals("\"\"")) ? params.get(1) : "\"yyyy/MM/dd hh:mm:ss\"", params.get(0)));
        register("calendarDiff", (bean, params, ctx) -> String.format("(long)(%s.getTimeInMillis() - %s.getTimeInMillis())", params.get(0), params.get(1)));
        register("calendarGetTime", (bean, params, ctx) -> String.format("%s.getTimeInMillis()", params.get(0)));
        register("calendarSetTime", (bean, params, ctx) -> String.format("%s.setTimeInMillis((long)(%s));", params.get(0), params.get(1)));
    }
    private static void registerListWidgetBlocks() {
        register("listSetData", (bean, params, ctx) -> String.format("%s.setAdapter(new ArrayAdapter<String>(%s, android.R.layout.simple_list_item_1, %s));", params.get(0), ctx.codeContext.baseContext(), params.get(1)));
        register("listSetCustomViewData", (bean, params, ctx) -> {
            var param = params.get(0);
            if (param.isEmpty()) return "";
            var adapter = param;
            if (ctx.isViewBindingEnabled && adapter.startsWith("binding.")) adapter = adapter.substring("binding.".length());
            return String.format("%s.setAdapter(new %s(%s));", param, ComponentCodeGenerator.getAdapterClassName(adapter, ctx.isViewBindingEnabled), params.get(1));
        });
        register("recyclerSetCustomViewData", (bean, params, ctx) -> {
            var param = params.get(0);
            if (param.isEmpty()) return "";
            var adapter = param;
            if (ctx.isViewBindingEnabled && adapter.startsWith("binding.")) adapter = adapter.substring("binding.".length());
            return String.format("%s.setAdapter(new %s(%s));", param, ComponentCodeGenerator.getAdapterClassName(adapter, ctx.isViewBindingEnabled), params.get(1));
        });
        register("spnSetCustomViewData", (bean, params, ctx) -> {
            var param = params.get(0);
            if (param.isEmpty()) return "";
            var adapter = param;
            if (ctx.isViewBindingEnabled && adapter.startsWith("binding.")) adapter = adapter.substring("binding.".length());
            return String.format("%s.setAdapter(new %s(%s));", param, ComponentCodeGenerator.getAdapterClassName(adapter, ctx.isViewBindingEnabled), params.get(1));
        });
        register("pagerSetCustomViewData", (bean, params, ctx) -> {
            var param = params.get(0);
            if (param.isEmpty()) return "";
            var adapter = param;
            if (ctx.isViewBindingEnabled && adapter.startsWith("binding.")) adapter = adapter.substring("binding.".length());
            return String.format("%s.setAdapter(new %s(%s));", param, ComponentCodeGenerator.getAdapterClassName(adapter, ctx.isViewBindingEnabled), params.get(1));
        });
        register("gridSetCustomViewData", (bean, params, ctx) -> {
            var param = params.get(0);
            if (param.isEmpty()) return "";
            var adapter = param;
            if (ctx.isViewBindingEnabled && adapter.startsWith("binding.")) adapter = adapter.substring("binding.".length());
            return String.format("%s.setAdapter(new %s(%s));", param, ComponentCodeGenerator.getAdapterClassName(adapter, ctx.isViewBindingEnabled), params.get(1));
        });
        register("listRefresh", (bean, params, ctx) -> String.format("((BaseAdapter)%s.getAdapter()).notifyDataSetChanged();", params.get(0)));
        register("listSetItemChecked", (bean, params, ctx) -> String.format("%s.setItemChecked((int)(%s), %s);", params.get(0), params.get(1), params.get(2)));
        register("listGetCheckedPosition", (bean, params, ctx) -> String.format("%s.getCheckedItemPosition()", params.get(0)));
        register("listGetCheckedPositions", (bean, params, ctx) -> String.format("%s = SketchwareUtil.getCheckedItemPositionsToArray(%s);", params.get(1), params.get(0)));
        register("listGetCheckedCount", (bean, params, ctx) -> String.format("%s.getCheckedItemCount()", params.get(0)));
        register("listSmoothScrollTo", (bean, params, ctx) -> String.format("%s.smoothScrollToPosition((int)(%s));", params.get(0), params.get(1)));
        register("spnSetData", (bean, params, ctx) -> String.format("%s.setAdapter(new ArrayAdapter<String>(%s, android.R.layout.simple_spinner_dropdown_item, %s));", params.get(0), ctx.codeContext.baseContext(), params.get(1)));
        register("spnRefresh", (bean, params, ctx) -> String.format("((ArrayAdapter)%s.getAdapter()).notifyDataSetChanged();", params.get(0)));
        register("spnSetSelection", (bean, params, ctx) -> String.format("%s.setSelection((int)(%s));", params.get(0), params.get(1)));
        register("spnGetSelection", (bean, params, ctx) -> String.format("%s.getSelectedItemPosition()", params.get(0)));
    }

    private static void registerWebViewBlocks() {
        register("webViewLoadUrl", (bean, params, ctx) -> String.format("%s.loadUrl(%s);", params.get(0), params.get(1)));
        register("webViewGetUrl", (bean, params, ctx) -> String.format("%s.getUrl()", params.get(0)));
        register("webViewSetCacheMode", (bean, params, ctx) -> String.format("%s.getSettings().setCacheMode(WebSettings.%s);", params.get(0), params.get(1)));
        register("webViewCanGoBack", (bean, params, ctx) -> String.format("%s.canGoBack()", params.get(0)));
        register("webViewCanGoForward", (bean, params, ctx) -> String.format("%s.canGoForward()", params.get(0)));
        register("webViewGoBack", (bean, params, ctx) -> String.format("%s.goBack();", params.get(0)));
        register("webViewGoForward", (bean, params, ctx) -> String.format("%s.goForward();", params.get(0)));
        register("webViewClearCache", (bean, params, ctx) -> String.format("%s.clearCache(true);", params.get(0)));
        register("webViewClearHistory", (bean, params, ctx) -> String.format("%s.clearHistory();", params.get(0)));
        register("webViewStopLoading", (bean, params, ctx) -> String.format("%s.stopLoading();", params.get(0)));
        register("webViewZoomIn", (bean, params, ctx) -> String.format("%s.zoomIn();", params.get(0)));
        register("webViewZoomOut", (bean, params, ctx) -> String.format("%s.zoomOut();", params.get(0)));
    }

    private static void registerCalendarViewBlocks() {
        register("calendarViewGetDate", (bean, params, ctx) -> String.format("%s.getDate()", params.get(0)));
        register("calendarViewSetDate", (bean, params, ctx) -> String.format("%s.setDate((long)(%s), true, true);", params.get(0), params.get(1)));
        register("calendarViewSetMinDate", (bean, params, ctx) -> String.format("%s.setMinDate((long)(%s));", params.get(0), params.get(1)));
        BlockCodeHandler setMaxDate = (bean, params, ctx) -> String.format("%s.setMaxDate((long)(%s));", params.get(0), params.get(1));
        register("calnedarViewSetMaxDate", setMaxDate); // legacy typo, kept for backward compatibility
        register("calendarViewSetMaxDate", setMaxDate);
    }

    private static void registerAdViewBlocks() {
        register("adViewLoadAd", (bean, params, ctx) -> String.format("%s.loadAd(new AdRequest.Builder()%s.build());", params.get(0),
                ctx.buildConfig.testDeviceIds.stream().map(device -> ".addTestDevice(\"" + device + "\")\n").collect(Collectors.joining())));
    }

    private static void registerMapViewBlocks() {
        register("mapViewSetMapType", (bean, params, ctx) -> String.format("_%s_controller.setMapType(GoogleMap.%s);", params.get(0), params.get(1)));
        register("mapViewMoveCamera", (bean, params, ctx) -> String.format("_%s_controller.moveCamera(%s, %s);", params.get(0), params.get(1), params.get(2)));
        register("mapViewZoomTo", (bean, params, ctx) -> String.format("_%s_controller.zoomTo(%s);", params.get(0), params.get(1)));
        register("mapViewZoomIn", (bean, params, ctx) -> String.format("_%s_controller.zoomIn();", params.get(0)));
        register("mapViewZoomOut", (bean, params, ctx) -> String.format("_%s_controller.zoomOut();", params.get(0)));
        register("mapViewAddMarker", (bean, params, ctx) -> String.format("_%s_controller.addMarker(%s, %s, %s);", params.get(0), params.get(1), params.get(2), params.get(3)));
        register("mapViewSetMarkerInfo", (bean, params, ctx) -> String.format("_%s_controller.setMarkerInfo(%s, %s, %s);", params.get(0), params.get(1), params.get(2), params.get(3)));
        register("mapViewSetMarkerPosition", (bean, params, ctx) -> String.format("_%s_controller.setMarkerPosition(%s, %s, %s);", params.get(0), params.get(1), params.get(2), params.get(3)));
        register("mapViewSetMarkerColor", (bean, params, ctx) -> String.format("_%s_controller.setMarkerColor(%s, BitmapDescriptorFactory.%s, %s);", params.get(0), params.get(1), params.get(2), params.get(3)));
        register("mapViewSetMarkerIcon", (bean, params, ctx) -> {
            String name = params.get(2).endsWith(".9") ? params.get(2).replaceAll("\\.9", "") : params.get(2);
            return String.format("_%s_controller.setMarkerIcon(%s, R.drawable.%s);", params.get(0), params.get(1), name.toLowerCase());
        });
        register("mapViewSetMarkerVisible", (bean, params, ctx) -> String.format("_%s_controller.setMarkerVisible(%s, %s);", params.get(0), params.get(1), params.get(2)));
    }

    private static void registerTimerBlocks() {
        register("timerAfter", (bean, params, ctx) -> {
            String onRun = (bean.subStack1 >= 0) ? ctx.resolveBlock(String.valueOf(bean.subStack1), "") : "";
            return String.format("%s = new TimerTask() {\n@Override\npublic void run() {\n%s(new Runnable() {\n@Override\npublic void run() {\n%s\n}\n});\n}\n};\n_timer.schedule(%s, (int)(%s));", params.get(0), ctx.codeContext.runOnUiThread(), onRun, params.get(0), params.get(1));
        });
        register("timerEvery", (bean, params, ctx) -> {
            String onRun = (bean.subStack1 >= 0) ? ctx.resolveBlock(String.valueOf(bean.subStack1), "") : "";
            return String.format("%s = new TimerTask() {\n@Override\npublic void run() {\n%s(new Runnable() {\n@Override\npublic void run() {\n%s\n}\n});\n}\n};\n_timer.scheduleAtFixedRate(%s, (int)(%s), (int)(%s));", params.get(0), ctx.codeContext.runOnUiThread(), onRun, params.get(0), params.get(1), params.get(2));
        });
        register("timerCancel", (bean, params, ctx) -> String.format("%s.cancel();", params.get(0)));
    }

    private static void registerFirebaseBlocks() {
        register("firebaseAdd", (bean, params, ctx) -> String.format("%s.child(%s).updateChildren(%s);", params.get(0), params.get(1), params.get(2)));
        register("firebasePush", (bean, params, ctx) -> String.format("%s.push().updateChildren(%s);", params.get(0), params.get(1)));
        register("firebaseGetPushKey", (bean, params, ctx) -> String.format("%s.push().getKey()", params.get(0)));
        register("firebaseDelete", (bean, params, ctx) -> String.format("%s.child(%s).removeValue();", params.get(0), params.get(1)));
        register("firebaseGetChildren", (bean, params, ctx) -> String.format(
                "%s.addListenerForSingleValueEvent(new ValueEventListener() {\n" +
                "@Override\npublic void onDataChange(DataSnapshot _dataSnapshot) {\n" +
                "%s = new ArrayList<>();\ntry {\n" +
                "GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};\n" +
                "for (DataSnapshot _data : _dataSnapshot.getChildren()) {\nHashMap<String, Object> _map = _data.getValue(_ind);\n%s.add(_map);\n}\n" +
                "} catch (Exception _e) {\nLog.e(\"Fx\", _e.getMessage(), _e);\n}\n%s\n}\n" +
                "@Override\npublic void onCancelled(DatabaseError _databaseError) {\n}\n});",
                params.get(0), params.get(1), params.get(1),
                (bean.subStack1 >= 0) ? ctx.resolveBlock(String.valueOf(bean.subStack1), "") : ""));
        register("firebaseStartListen", (bean, params, ctx) -> String.format("%s.addChildEventListener(_%s_child_listener);", params.get(0), params.get(0)));
        register("firebaseStopListen", (bean, params, ctx) -> String.format("%s.removeEventListener(_%s_child_listener);", params.get(0), params.get(0)));
    }

    private static void registerFirebaseAuthBlocks() {
        register("firebaseauthCreateUser", (bean, params, ctx) -> {
            if (!params.get(1).equals("\"\"") && !params.get(2).equals("\"\"")) {
                return String.format("%s.createUserWithEmailAndPassword(%s, %s).addOnCompleteListener(%s, %s);", params.get(0), params.get(1), params.get(2), ctx.codeContext.qualifiedThis(), "_" + params.get(0) + "_create_user_listener");
            }
            return "";
        });
        register("firebaseauthSignInUser", (bean, params, ctx) -> {
            if (!params.get(1).equals("\"\"") && !params.get(2).equals("\"\"")) {
                return String.format("%s.signInWithEmailAndPassword(%s, %s).addOnCompleteListener(%s, %s);", params.get(0), params.get(1), params.get(2), ctx.codeContext.qualifiedThis(), "_" + params.get(0) + "_sign_in_listener");
            }
            return "";
        });
        register("firebaseauthSignInAnonymously", (bean, params, ctx) -> String.format("%s.signInAnonymously().addOnCompleteListener(%s, %s);", params.get(0), ctx.codeContext.qualifiedThis(), "_" + params.get(0) + "_sign_in_listener"));
        register("firebaseauthIsLoggedIn", (bean, params, ctx) -> "(FirebaseAuth.getInstance().getCurrentUser() != null)");
        register("firebaseauthGetCurrentUser", (bean, params, ctx) -> "FirebaseAuth.getInstance().getCurrentUser().getEmail()");
        register("firebaseauthGetUid", (bean, params, ctx) -> "FirebaseAuth.getInstance().getCurrentUser().getUid()");
        register("firebaseauthResetPassword", (bean, params, ctx) -> {
            if (!params.get(1).equals("\"\"")) {
                return String.format("%s.sendPasswordResetEmail(%s).addOnCompleteListener(%s);", params.get(0), params.get(1), "_" + params.get(0) + "_reset_password_listener");
            }
            return "";
        });
        register("firebaseauthSignOutUser", (bean, params, ctx) -> "FirebaseAuth.getInstance().signOut();");
        register("gyroscopeStartListen", (bean, params, ctx) -> String.format("%s.registerListener(_%s_sensor_listener, %s.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_NORMAL);", params.get(0), params.get(0), params.get(0)));
        register("gyroscopeStopListen", (bean, params, ctx) -> String.format("%s.unregisterListener(_%s_sensor_listener);", params.get(0), params.get(0)));
    }

    private static void registerDialogBlocks() {
        register("dialogSetTitle", (bean, params, ctx) -> String.format("%s.setTitle(%s);", params.get(0), params.get(1)));
        register("dialogSetMessage", (bean, params, ctx) -> String.format("%s.setMessage(%s);", params.get(0), params.get(1)));
        register("dialogShow", (bean, params, ctx) -> String.format("%s.create().show();", params.get(0)));
        register("dialogOkButton", (bean, params, ctx) -> {
            String onClick = (bean.subStack1 >= 0) ? ctx.resolveBlock(String.valueOf(bean.subStack1), "") : "";
            return String.format("%s.setPositiveButton(%s, new DialogInterface.OnClickListener() {\n@Override\npublic void onClick(DialogInterface _dialog, int _which) {\n%s\n}\n});", params.get(0), params.get(1), onClick);
        });
        register("dialogCancelButton", (bean, params, ctx) -> {
            String onClick = (bean.subStack1 >= 0) ? ctx.resolveBlock(String.valueOf(bean.subStack1), "") : "";
            return String.format("%s.setNegativeButton(%s, new DialogInterface.OnClickListener() {\n@Override\npublic void onClick(DialogInterface _dialog, int _which) {\n%s\n}\n});", params.get(0), params.get(1), onClick);
        });
        register("dialogNeutralButton", (bean, params, ctx) -> {
            String onClick = (bean.subStack1 >= 0) ? ctx.resolveBlock(String.valueOf(bean.subStack1), "") : "";
            return String.format("%s.setNeutralButton(%s, new DialogInterface.OnClickListener() {\n@Override\npublic void onClick(DialogInterface _dialog, int _which) {\n%s\n}\n});", params.get(0), params.get(1), onClick);
        });
    }

    private static void registerMediaBlocks() {
        register("mediaplayerCreate", (bean, params, ctx) -> String.format("%s = MediaPlayer.create(%s, R.raw.%s);", params.get(0), ctx.codeContext.appContext(), params.get(1).toLowerCase()));
        register("mediaplayerStart", (bean, params, ctx) -> String.format("%s.start();", params.get(0)));
        register("mediaplayerPause", (bean, params, ctx) -> String.format("%s.pause();", params.get(0)));
        register("mediaplayerSeek", (bean, params, ctx) -> String.format("%s.seekTo((int)(%s));", params.get(0), params.get(1)));
        register("mediaplayerGetCurrent", (bean, params, ctx) -> String.format("%s.getCurrentPosition()", params.get(0)));
        register("mediaplayerGetDuration", (bean, params, ctx) -> String.format("%s.getDuration()", params.get(0)));
        register("mediaplayerReset", (bean, params, ctx) -> String.format("%s.reset();", params.get(0)));
        register("mediaplayerRelease", (bean, params, ctx) -> String.format("%s.release();", params.get(0)));
        register("mediaplayerIsPlaying", (bean, params, ctx) -> String.format("%s.isPlaying()", params.get(0)));
        register("mediaplayerSetLooping", (bean, params, ctx) -> String.format("%s.setLooping(%s);", params.get(0), params.get(1)));
        register("mediaplayerIsLooping", (bean, params, ctx) -> String.format("%s.isLooping()", params.get(0)));
        register("soundpoolCreate", (bean, params, ctx) -> String.format("%s = new SoundPool((int)(%s), AudioManager.STREAM_MUSIC, 0);", params.get(0), params.get(1)));
        register("soundpoolLoad", (bean, params, ctx) -> String.format("%s.load(%s, R.raw.%s, 1);", params.get(0), ctx.codeContext.appContext(), params.get(1)));
        register("soundpoolStreamPlay", (bean, params, ctx) -> String.format("%s.play((int)(%s), 1.0f, 1.0f, 1, (int)(%s), 1.0f);", params.get(0), params.get(1), params.get(2)));
        register("soundpoolStreamStop", (bean, params, ctx) -> String.format("%s.stop((int)(%s));", params.get(0), params.get(1)));
    }
    private static void registerSeekBarBlocks() {
        register("setThumbResource", (bean, params, ctx) -> { String name = params.get(1).replaceAll("\\.9", ""); return String.format("%s.setThumbResource(R.drawable.%s)", params.get(0), name.toLowerCase()); });
        register("setTrackResource", (bean, params, ctx) -> { String name = params.get(1).replaceAll("\\.9", ""); return String.format("%s.setTrackResource(R.drawable.%s)", params.get(0), name.toLowerCase()); });
        register("seekBarSetProgress", (bean, params, ctx) -> String.format("%s.setProgress((int)%s);", params.get(0), params.get(1)));
        register("seekBarGetProgress", (bean, params, ctx) -> String.format("%s.getProgress()", params.get(0)));
        register("seekBarSetMax", (bean, params, ctx) -> String.format("%s.setMax((int)%s);", params.get(0), params.get(1)));
        register("seekBarGetMax", (bean, params, ctx) -> String.format("%s.getMax()", params.get(0)));
    }

    private static void registerAnimationBlocks() {
        register("objectanimatorSetTarget", (bean, params, ctx) -> String.format("%s.setTarget(%s);", params.get(0), params.get(1)));
        register("objectanimatorSetProperty", (bean, params, ctx) -> String.format("%s.setPropertyName(\"%s\");", params.get(0), params.get(1)));
        register("objectanimatorSetValue", (bean, params, ctx) -> String.format("%s.setFloatValues((float)(%s));", params.get(0), params.get(1)));
        register("objectanimatorSetFromTo", (bean, params, ctx) -> String.format("%s.setFloatValues((float)(%s), (float)(%s));", params.get(0), params.get(1), params.get(2)));
        register("objectanimatorSetDuration", (bean, params, ctx) -> String.format("%s.setDuration((int)(%s));", params.get(0), params.get(1)));
        register("objectanimatorSetRepeatMode", (bean, params, ctx) -> String.format("%s.setRepeatMode(ValueAnimator.%s);", params.get(0), params.get(1)));
        register("objectanimatorSetRepeatCount", (bean, params, ctx) -> String.format("%s.setRepeatCount((int)(%s));", params.get(0), params.get(1)));
        register("objectanimatorSetInterpolator", (bean, params, ctx) -> {
            String interpolator = switch (params.get(1)) {
                case "Accelerate" -> "new AccelerateInterpolator()";
                case "Decelerate" -> "new DecelerateInterpolator()";
                case "AccelerateDeccelerate" -> "new AccelerateDecelerateInterpolator()";
                case "Bounce" -> "new BounceInterpolator()";
                default -> "new LinearInterpolator()";
            };
            return String.format("%s.setInterpolator(%s);", params.get(0), interpolator);
        });
        register("objectanimatorStart", (bean, params, ctx) -> String.format("%s.start();", params.get(0)));
        register("objectanimatorCancel", (bean, params, ctx) -> String.format("%s.cancel();", params.get(0)));
        register("objectanimatorIsRunning", (bean, params, ctx) -> String.format("%s.isRunning()", params.get(0)));
        register("interstitialadCreate", (bean, params, ctx) -> "");
        register("interstitialadLoadAd", (bean, params, ctx) -> "");
        register("interstitialadShow", (bean, params, ctx) -> "");
    }

    private static void registerFirebaseStorageBlocks() {
        register("firebasestorageUploadFile", (bean, params, ctx) -> {
            if (!params.get(1).equals("\"\"") && !params.get(2).equals("\"\"")) {
                return String.format("%s.child(%s).putFile(Uri.fromFile(new File(%s))).addOnFailureListener(_%s_failure_listener).addOnProgressListener(_%s_upload_progress_listener).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {\n@Override\npublic Task<Uri> then(Task<UploadTask.TaskSnapshot> task) throws Exception {\nreturn %s.child(%s).getDownloadUrl();\n}}).addOnCompleteListener(_%s_upload_success_listener);", params.get(0), params.get(2), params.get(1), params.get(0), params.get(0), params.get(0), params.get(2), params.get(0));
            }
            return "";
        });
        register("firebasestorageDownloadFile", (bean, params, ctx) -> {
            if (!params.get(1).equals("\"\"") && !params.get(2).equals("\"\"")) {
                return String.format("_firebase_storage.getReferenceFromUrl(%s).getFile(new File(%s)).addOnSuccessListener(_%s_download_success_listener).addOnFailureListener(_%s_failure_listener).addOnProgressListener(_%s_download_progress_listener);", params.get(1), params.get(2), params.get(0), params.get(0), params.get(0));
            }
            return "";
        });
        register("firebasestorageDelete", (bean, params, ctx) -> {
            if (!params.get(1).equals("\"\"")) {
                return String.format("_firebase_storage.getReferenceFromUrl(%s).delete().addOnSuccessListener(_%s_delete_success_listener).addOnFailureListener(_%s_failure_listener);", params.get(1), params.get(0), params.get(0));
            }
            return "";
        });
    }

    private static void registerFileUtilBlocks() {
        register("fileutilread", (bean, params, ctx) -> !params.get(0).equals("\"\"") ? String.format("FileUtil.readFile(%s)", params.get(0)) : "");
        register("fileutilwrite", (bean, params, ctx) -> !params.get(0).equals("\"\"") ? String.format("FileUtil.writeFile(%s, %s);", params.get(1), params.get(0)) : "");
        register("fileutilcopy", (bean, params, ctx) -> (!params.get(0).equals("\"\"") && !params.get(1).equals("\"\"")) ? String.format("FileUtil.copyFile(%s, %s);", params.get(0), params.get(1)) : "");
        register("fileutilmove", (bean, params, ctx) -> (!params.get(0).equals("\"\"") && !params.get(1).equals("\"\"")) ? String.format("FileUtil.moveFile(%s, %s);", params.get(0), params.get(1)) : "");
        register("fileutildelete", (bean, params, ctx) -> !params.get(0).equals("\"\"") ? String.format("FileUtil.deleteFile(%s);", params.get(0)) : "");
        register("fileutilisexist", (bean, params, ctx) -> !params.get(0).equals("\"\"") ? String.format("FileUtil.isExistFile(%s)", params.get(0)) : "");
        register("fileutilmakedir", (bean, params, ctx) -> !params.get(0).equals("\"\"") ? String.format("FileUtil.makeDir(%s);", params.get(0)) : "");
        register("fileutillistdir", (bean, params, ctx) -> !params.get(0).equals("\"\"") ? String.format("FileUtil.listDir(%s, %s);", params.get(0), params.get(1)) : "");
        register("fileutilisdir", (bean, params, ctx) -> !params.get(0).equals("\"\"") ? String.format("FileUtil.isDirectory(%s)", params.get(0)) : "");
        register("fileutilisfile", (bean, params, ctx) -> !params.get(0).equals("\"\"") ? String.format("FileUtil.isFile(%s)", params.get(0)) : "");
        register("fileutillength", (bean, params, ctx) -> !params.get(0).equals("\"\"") ? String.format("FileUtil.getFileLength(%s)", params.get(0)) : "");
        register("fileutilStartsWith", (bean, params, ctx) -> !params.get(0).equals("\"\"") ? String.format("%s.startsWith(%s)", params.get(0), params.get(1)) : "");
        register("fileutilEndsWith", (bean, params, ctx) -> !params.get(0).equals("\"\"") ? String.format("%s.endsWith(%s)", params.get(0), params.get(1)) : "");
        register("fileutilGetLastSegmentPath", (bean, params, ctx) -> !params.get(0).equals("\"\"") ? String.format("Uri.parse(%s).getLastPathSegment()", params.get(0)) : "");
        register("getExternalStorageDir", (bean, params, ctx) -> "FileUtil.getExternalStorageDir()");
        register("getPackageDataDir", (bean, params, ctx) -> "FileUtil.getPackageDataDir(" + ctx.codeContext.appContext() + ")");
        register("getPublicDir", (bean, params, ctx) -> String.format("FileUtil.getPublicDir(Environment.%s)", params.get(0)));
    }

    private static void registerImageBlocks() {
        register("resizeBitmapFileRetainRatio", (bean, params, ctx) -> (!params.get(0).equals("\"\"") && !params.get(1).equals("\"\"")) ? String.format("FileUtil.resizeBitmapFileRetainRatio(%s, %s, %s);", params.get(0), params.get(1), params.get(2)) : "");
        register("resizeBitmapFileToSquare", (bean, params, ctx) -> (!params.get(0).equals("\"\"") && !params.get(1).equals("\"\"")) ? String.format("FileUtil.resizeBitmapFileToSquare(%s, %s, %s);", params.get(0), params.get(1), params.get(2)) : "");
        register("resizeBitmapFileToCircle", (bean, params, ctx) -> (!params.get(0).equals("\"\"") && !params.get(1).equals("\"\"")) ? String.format("FileUtil.resizeBitmapFileToCircle(%s, %s);", params.get(0), params.get(1)) : "");
        register("resizeBitmapFileWithRoundedBorder", (bean, params, ctx) -> (!params.get(0).equals("\"\"") && !params.get(1).equals("\"\"")) ? String.format("FileUtil.resizeBitmapFileWithRoundedBorder(%s, %s, %s);", params.get(0), params.get(1), params.get(2)) : "");
        register("cropBitmapFileFromCenter", (bean, params, ctx) -> (!params.get(0).equals("\"\"") && !params.get(1).equals("\"\"")) ? String.format("FileUtil.cropBitmapFileFromCenter(%s, %s, %s, %s);", params.get(0), params.get(1), params.get(3), params.get(2)) : "");
        register("rotateBitmapFile", (bean, params, ctx) -> (!params.get(0).equals("\"\"") && !params.get(1).equals("\"\"")) ? String.format("FileUtil.rotateBitmapFile(%s, %s, %s);", params.get(0), params.get(1), params.get(2)) : "");
        register("scaleBitmapFile", (bean, params, ctx) -> (!params.get(0).equals("\"\"") && !params.get(1).equals("\"\"")) ? String.format("FileUtil.scaleBitmapFile(%s, %s, %s, %s);", params.get(0), params.get(1), params.get(2), params.get(3)) : "");
        register("skewBitmapFile", (bean, params, ctx) -> (!params.get(0).equals("\"\"") && !params.get(1).equals("\"\"")) ? String.format("FileUtil.skewBitmapFile(%s, %s, %s, %s);", params.get(0), params.get(1), params.get(2), params.get(3)) : "");
        register("setBitmapFileColorFilter", (bean, params, ctx) -> (!params.get(0).equals("\"\"") && !params.get(1).equals("\"\"")) ? String.format("FileUtil.setBitmapFileColorFilter(%s, %s, %s);", params.get(0), params.get(1), params.get(2)) : "");
        register("setBitmapFileBrightness", (bean, params, ctx) -> (!params.get(0).equals("\"\"") && !params.get(1).equals("\"\"")) ? String.format("FileUtil.setBitmapFileBrightness(%s, %s, %s);", params.get(0), params.get(1), params.get(2)) : "");
        register("setBitmapFileContrast", (bean, params, ctx) -> (!params.get(0).equals("\"\"") && !params.get(1).equals("\"\"")) ? String.format("FileUtil.setBitmapFileContrast(%s, %s, %s);", params.get(0), params.get(1), params.get(2)) : "");
        register("getJpegRotate", (bean, params, ctx) -> !params.get(0).equals("\"\"") ? String.format("FileUtil.getJpegRotate(%s)", params.get(0)) : "");
        register("filepickerstartpickfiles", (bean, params, ctx) -> String.format("startActivityForResult(%s, REQ_CD_%s);", params.get(0), params.get(0).toUpperCase()));
        register("camerastarttakepicture", (bean, params, ctx) -> String.format("startActivityForResult(%s, REQ_CD_%s);", params.get(0), params.get(0).toUpperCase()));
        register("setImageFilePath", (bean, params, ctx) -> !params.get(1).equals("\"\"") ? String.format("%s.setImageBitmap(FileUtil.decodeSampleBitmapFromPath(%s, 1024, 1024));", params.get(0), params.get(1)) : "");
        register("setImageUrl", (bean, params, ctx) -> !params.get(1).equals("\"\"") ? String.format("Glide.with(%s).load(Uri.parse(%s)).into(%s);", ctx.codeContext.appContext(), params.get(1), params.get(0)) : "");
    }

    private static void registerNetworkBlocks() {
        register("requestnetworkSetParams", (bean, params, ctx) -> String.format("%s.setParams(%s, RequestNetworkController.%s);", params.get(0), params.get(1), params.get(2)));
        register("requestnetworkSetHeaders", (bean, params, ctx) -> String.format("%s.setHeaders(%s);", params.get(0), params.get(1)));
        register("requestnetworkStartRequestNetwork", (bean, params, ctx) -> String.format("%s.startRequestNetwork(RequestNetworkController.%s, %s, %s, _%s_request_listener);", params.get(0), params.get(1), params.get(2), params.get(3), params.get(0)));
        register("requestnetworkUploadFile", (bean, params, ctx) -> params.size() >= 4 ? String.format("%s.uploadFile(%s, %s, %s, %s, _%s_request_listener);", params.get(0), params.get(1), params.get(2), params.get(3), params.get(4), params.get(0)) : "");
    }

    private static void registerSpeechBlocks() {
        register("textToSpeechSetPitch", (bean, params, ctx) -> String.format("%s.setPitch((float)%s);", params.get(0), params.get(1)));
        register("textToSpeechSetSpeechRate", (bean, params, ctx) -> String.format("%s.setSpeechRate((float)%s);", params.get(0), params.get(1)));
        register("textToSpeechSpeak", (bean, params, ctx) -> String.format("%s.speak(%s, TextToSpeech.QUEUE_ADD, null);", params.get(0), params.get(1)));
        register("textToSpeechIsSpeaking", (bean, params, ctx) -> String.format("%s.isSpeaking()", params.get(0)));
        register("textToSpeechStop", (bean, params, ctx) -> String.format("%s.stop();", params.get(0)));
        register("textToSpeechShutdown", (bean, params, ctx) -> String.format("%s.shutdown();", params.get(0)));
        register("speechToTextStartListening", (bean, params, ctx) -> String.format("Intent _intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);\n_intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());\n_intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);\n_intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());\n%s.startListening(_intent);", params.get(0)));
        register("speechToTextStopListening", (bean, params, ctx) -> String.format("%s.stopListening();", params.get(0)));
        register("speechToTextShutdown", (bean, params, ctx) -> String.format("%s.cancel();\n%s.destroy();", params.get(0), params.get(0)));
    }

    private static void registerBluetoothBlocks() {
        register("bluetoothConnectReadyConnection", (bean, params, ctx) -> String.format("%s.readyConnection(_%s_bluetooth_connection_listener, %s);", params.get(0), params.get(0), params.get(1)));
        register("bluetoothConnectReadyConnectionToUuid", (bean, params, ctx) -> String.format("%s.readyConnection(_%s_bluetooth_connection_listener, %s, %s);", params.get(0), params.get(0), params.get(1), params.get(2)));
        register("bluetoothConnectStartConnection", (bean, params, ctx) -> String.format("%s.startConnection(_%s_bluetooth_connection_listener, %s, %s);", params.get(0), params.get(0), params.get(1), params.get(2)));
        register("bluetoothConnectStartConnectionToUuid", (bean, params, ctx) -> String.format("%s.startConnection(_%s_bluetooth_connection_listener, %s, %s, %s);", params.get(0), params.get(0), params.get(1), params.get(2), params.get(3)));
        register("bluetoothConnectStopConnection", (bean, params, ctx) -> String.format("%s.stopConnection(_%s_bluetooth_connection_listener, %s);", params.get(0), params.get(0), params.get(1)));
        register("bluetoothConnectSendData", (bean, params, ctx) -> String.format("%s.sendData(_%s_bluetooth_connection_listener, %s, %s);", params.get(0), params.get(0), params.get(1), params.get(2)));
        register("bluetoothConnectIsBluetoothEnabled", (bean, params, ctx) -> String.format("%s.isBluetoothEnabled()", params.get(0)));
        register("bluetoothConnectIsBluetoothActivated", (bean, params, ctx) -> String.format("%s.isBluetoothActivated()", params.get(0)));
        register("bluetoothConnectActivateBluetooth", (bean, params, ctx) -> String.format("%s.activateBluetooth();", params.get(0)));
        register("bluetoothConnectGetPairedDevices", (bean, params, ctx) -> String.format("%s.getPairedDevices(%s);", params.get(0), params.get(1)));
        register("bluetoothConnectGetRandomUuid", (bean, params, ctx) -> params.get(0) + ".getRandomUUID()");
    }

    private static void registerLocationBlocks() {
        register("locationManagerRequestLocationUpdates", (bean, params, ctx) -> {
            String locationRequest = "%s.requestLocationUpdates(LocationManager.%s, %s, %s, _%s_location_listener);";
            if (ctx.buildConfig.isAppCompatEnabled) {
                return String.format("if (ContextCompat.checkSelfPermission(%s, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {\n" + locationRequest + "\n}", ctx.codeContext.qualifiedThis(), params.get(0), params.get(1), params.get(2), params.get(3), params.get(0));
            } else {
                return String.format("if (Build.VERSION.SDK_INT >= 23) {\nif (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {\n" + locationRequest + "\n}\n}\nelse {\n" + locationRequest + "\n}", params.get(0), params.get(1), params.get(2), params.get(3), params.get(0), params.get(0), params.get(1), params.get(2), params.get(3), params.get(0));
            }
        });
        register("locationManagerRemoveUpdates", (bean, params, ctx) -> params.get(0) + ".removeUpdates(_" + params.get(0) + "_location_listener);");
    }

    private static void registerNotificationBlocks() {
        register("notifCreateChannel", (bean, params, ctx) -> params.size() >= 4 ? String.format("if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {\nNotificationChannel _channel_%s = new NotificationChannel(%s, %s, NotificationManager.%s);\n_nm_%s.createNotificationChannel(_channel_%s);\n}\n%s = new NotificationCompat.Builder(%s, %s);\n%s.setSmallIcon(R.mipmap.ic_launcher);", params.get(0), params.get(1), params.get(2), params.get(3), params.get(0), params.get(0), params.get(0), ctx.codeContext.appContext(), params.get(1), params.get(0)) : "");
        register("notifSetChannel", (bean, params, ctx) -> params.size() >= 2 ? String.format("%s = new NotificationCompat.Builder(%s, %s);\n%s.setSmallIcon(R.mipmap.ic_launcher);", params.get(0), ctx.codeContext.appContext(), params.get(1), params.get(0)) : "");
        register("notifSetTitle", (bean, params, ctx) -> params.size() >= 2 ? String.format("%s.setContentTitle(%s);", params.get(0), params.get(1)) : "");
        register("notifSetContent", (bean, params, ctx) -> params.size() >= 2 ? String.format("%s.setContentText(%s);", params.get(0), params.get(1)) : "");
        register("notifSetSmallIcon", (bean, params, ctx) -> params.size() >= 2 ? String.format("%s.setSmallIcon(R.drawable.%s);", params.get(0), params.get(1).replaceAll("\\.9", "").toLowerCase()) : "");
        register("notifSetAutoCancel", (bean, params, ctx) -> params.size() >= 2 ? String.format("%s.setAutoCancel(%s);", params.get(0), params.get(1)) : "");
        register("notifSetPriority", (bean, params, ctx) -> params.size() >= 2 ? String.format("%s.setPriority(NotificationCompat.%s);", params.get(0), params.get(1)) : "");
        register("notifSetClickIntent", (bean, params, ctx) -> params.size() >= 2 ? String.format("%s.setContentIntent(PendingIntent.getActivity(%s, 0, %s, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));", params.get(0), ctx.codeContext.appContext(), params.get(1)) : "");
        register("notifShow", (bean, params, ctx) -> params.size() >= 2 ? String.format("if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(%s, \"android.permission.POST_NOTIFICATIONS\") != PackageManager.PERMISSION_GRANTED) {\nActivityCompat.requestPermissions(%s, new String[]{\"android.permission.POST_NOTIFICATIONS\"}, 9901);\n} else {\n_nm_%s.notify((int)(%s), %s.build());\n}", ctx.codeContext.qualifiedThis(), ctx.codeContext.qualifiedThis(), params.get(0), params.get(1), params.get(0)) : "");
        register("notifCancel", (bean, params, ctx) -> params.size() >= 2 ? String.format("_nm_%s.cancel((int)(%s));", params.get(0), params.get(1)) : "");
    }

    private static void registerSQLiteBlocks() {
        register("sqliteOpen", (bean, params, ctx) -> params.size() >= 2 ? String.format("try {\n%s = openOrCreateDatabase(%s, MODE_PRIVATE, null);\n} catch (Exception _sqliteException) {\n_%s_onSQLiteError(_sqliteException.getMessage());\n}", params.get(0), params.get(1), params.get(0)) : "");
        register("sqliteClose", (bean, params, ctx) -> params.size() >= 1 ? String.format("if (%s != null) { %s.close(); }", params.get(0), params.get(0)) : "");
        register("sqliteExecSQL", (bean, params, ctx) -> params.size() >= 2 ? String.format("try {\n%s.execSQL(%s);\n} catch (Exception _sqliteException) {\n_%s_onSQLiteError(_sqliteException.getMessage());\n}", params.get(0), params.get(1), params.get(0)) : "");
        register("sqliteRawQuery", (bean, params, ctx) -> params.size() >= 2 ? String.format("try {\n_%s_cursor = %s.rawQuery(%s, null);\n} catch (Exception _sqliteException) {\n_%s_onSQLiteError(_sqliteException.getMessage());\n}", params.get(0), params.get(0), params.get(1), params.get(0)) : "");
        register("sqliteMoveToFirst", (bean, params, ctx) -> params.size() >= 1 ? String.format("(_%s_cursor != null && _%s_cursor.moveToFirst())", params.get(0), params.get(0)) : "");
        register("sqliteMoveToNext", (bean, params, ctx) -> params.size() >= 1 ? String.format("(_%s_cursor != null && !_%s_cursor.isAfterLast() ? (_%s_cursor.moveToNext() || true) : false)", params.get(0), params.get(0), params.get(0)) : "");
        register("sqliteIsAfterLast", (bean, params, ctx) -> params.size() >= 1 ? String.format("(_%s_cursor == null || _%s_cursor.isAfterLast())", params.get(0), params.get(0)) : "");
        register("sqliteGetString", (bean, params, ctx) -> params.size() >= 2 ? String.format("(_%s_cursor != null ? _%s_cursor.getString(_%s_cursor.getColumnIndexOrThrow(%s)) : \"\")", params.get(0), params.get(0), params.get(0), params.get(1)) : "");
        register("sqliteGetNumber", (bean, params, ctx) -> params.size() >= 2 ? String.format("(_%s_cursor != null ? _%s_cursor.getDouble(_%s_cursor.getColumnIndexOrThrow(%s)) : 0)", params.get(0), params.get(0), params.get(0), params.get(1)) : "");
        register("sqliteGetCount", (bean, params, ctx) -> params.size() >= 1 ? String.format("(double)(_%s_cursor != null ? _%s_cursor.getCount() : 0)", params.get(0), params.get(0)) : "");
        register("sqliteBeginTransaction", (bean, params, ctx) -> params.size() >= 1 ? String.format("if (%s != null && %s.isOpen()) { %s.beginTransaction(); }", params.get(0), params.get(0), params.get(0)) : "");
        register("sqliteSetTransactionSuccessful", (bean, params, ctx) -> params.size() >= 1 ? String.format("if (%s != null && %s.isOpen() && %s.inTransaction()) { %s.setTransactionSuccessful(); }", params.get(0), params.get(0), params.get(0), params.get(0)) : "");
        register("sqliteEndTransaction", (bean, params, ctx) -> params.size() >= 1 ? String.format("if (%s != null && %s.isOpen() && %s.inTransaction()) { %s.endTransaction(); }", params.get(0), params.get(0), params.get(0), params.get(0)) : "");
        register("sqliteForEachRow", (bean, params, ctx) -> {
            if (params.size() >= 1) {
                String forEachBody = (bean.subStack1 >= 0) ? ctx.resolveBlock(String.valueOf(bean.subStack1), "") : "";
                return String.format("if (_%s_cursor != null && _%s_cursor.moveToFirst()) {\nwhile (!_%s_cursor.isAfterLast()) {\n%s\n_%s_cursor.moveToNext();\n}\n}", params.get(0), params.get(0), params.get(0), forEachBody, params.get(0));
            }
            return "";
        });
        register("sqliteEnableWAL", (bean, params, ctx) -> params.size() >= 1 ? String.format("if (%s != null) { %s.enableWriteAheadLogging(); }", params.get(0), params.get(0)) : "");
        register("sqliteCloseCursor", (bean, params, ctx) -> params.size() >= 1 ? String.format("if (_%s_cursor != null) { _%s_cursor.close(); }", params.get(0), params.get(0)) : "");
        register("sqliteIsOpen", (bean, params, ctx) -> params.size() >= 1 ? String.format("(%s != null && %s.isOpen())", params.get(0), params.get(0)) : "");
        register("sqliteCursorIsNull", (bean, params, ctx) -> params.size() >= 1 ? String.format("(_%s_cursor == null || _%s_cursor.isClosed())", params.get(0), params.get(0)) : "");
    }

    private static void registerMiscBlocks() {
        register("doToast", (bean, params, ctx) -> String.format("SketchwareUtil.showMessage(%s, %s);", ctx.codeContext.appContext(), params.get(0)));
        register("copyToClipboard", (bean, params, ctx) -> String.format("(%s(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText(\"clipboard\", %s));", ctx.codeContext.systemService("ClipboardManager"), params.get(0)));
        register("setTitle", (bean, params, ctx) -> String.format("setTitle(%s);", params.get(0)));
        register("vibratorAction", (bean, params, ctx) -> String.format("%s.vibrate((long)(%s));", params.get(0), params.get(1)));
    }
}
