package mod.hilal.saif.blocks;

import android.util.Pair;

import com.google.gson.Gson;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.PatternSyntaxException;

import mod.hey.studios.util.Helper;
import mod.jbk.util.LogUtil;
import pro.sketchware.utility.FileUtil;

/**
 * Sample usage of CommandBlock:
 * /*-JX4UA2y_f1OckjjvxWI.bQwRei-sLEsBmds7ArsRfi0xSFEP3Php97kjdMCs5ed
 * >void initialize(Bundle _savedInstanceState
 * >2
 * >12
 * >0
 * >replace
 * setSupportActionBar(_toolbar);
 * getSupportActionBar().setDisplayHomeAsUpEnabled(true);
 * BpWI8U4flOpx8Ke66QTlZYBA_NEusQ7BN-D0wvZs7ArsRfi0.EP3Php97kjdMCs
 */
public class CommandBlock {

    private static final String TAG = "CommandBlock";
    private static final String TEMP_COMMANDS_PATH = FileUtil.getExternalStorageDir().concat("/.sketchware/temp/commands");
    private static final String TEMP_LOG_PATH = FileUtil.getExternalStorageDir().concat("/.sketchware/temp/log.txt");
    private static final String JAVA_COMMAND_START_MARKER = "/*-JX4UA2y_f1OckjjvxWI.bQwRei-sLEsBmds7ArsRfi0xSFEP3Php97kjdMCs5ed";
    private static final String JAVA_COMMAND_END_MARKER = "BpWI8U4flOpx8Ke66QTlZYBA_NEusQ7BN-D0wvZs7ArsRfi0.EP3Php97kjdMCs*/";
    private static final String XML_COMMAND_START_MARKER = "/*AXAVajPNTpbJjsz-NGVTp08YDzfI-04kA7ZsuCl4GHqTQQiuWL45sV6Vf4gwK";
    private static final String XML_COMMAND_END_MARKER = "Ui5_PNTJb21WO6OuGwQ3psk3su1LIvyXo_OAol-kVQBC5jtN_DcPLaRCJ0yXp*/";
    private static final String KEY_REFERENCE = "reference";
    private static final String KEY_DISTANCE = "distance";
    private static final String KEY_AFTER = "after";
    private static final String KEY_BEFORE = "before";
    private static final String KEY_COMMAND = "command";
    private static final String KEY_INPUT = "input";
    private static final Gson GSON = new Gson();

    public static String applyCommands(String fileName, String sourceCode) {
        String result = sourceCode;
        ArrayList<HashMap<String, Object>> commandData = readStoredCommands();
        if (commandData.isEmpty()) {
            return sourceCode;
        }
        for (HashMap<String, Object> commandEntry : commandData) {
            if (fileName.equals(getInputName(getStringValue(commandEntry, KEY_INPUT)))) {
                result = applyStoredCommand(result, commandEntry);
            }
        }
        return result;
    }

    private static ArrayList<HashMap<String, Object>> readStoredCommands() {
        ArrayList<HashMap<String, Object>> commandData = new ArrayList<>();
        if (!FileUtil.isExistFile(TEMP_COMMANDS_PATH)) {
            return commandData;
        }
        String fileContent = FileUtil.readFile(TEMP_COMMANDS_PATH);
        if (fileContent.isEmpty() || fileContent.equals("[]")) {
            return commandData;
        }
        try {
            ArrayList<HashMap<String, Object>> parsedCommandData = GSON.fromJson(fileContent, Helper.TYPE_MAP_LIST);
            if (parsedCommandData != null) {
                commandData.addAll(parsedCommandData);
            }
        } catch (RuntimeException e) {
            LogUtil.e(TAG, "Failed to parse command temp file: " + TEMP_COMMANDS_PATH, e);
        }
        return commandData;
    }

    private static String applyStoredCommand(String sourceCode, HashMap<String, Object> commandDefinition) {
        ArrayList<String> lines = new ArrayList<>(Arrays.asList(sourceCode.split("\n")));
        String reference = getStringValue(commandDefinition, KEY_REFERENCE);
        int distance = getNumericValue(commandDefinition, KEY_DISTANCE);
        int after = getNumericValue(commandDefinition, KEY_AFTER);
        int before = getNumericValue(commandDefinition, KEY_BEFORE);
        String command = getStringValue(commandDefinition, KEY_COMMAND);
        String input = getExceptFirstLine(getStringValue(commandDefinition, KEY_INPUT));

        if (command.equals("find-replace")) {
            return sourceCode.replace(reference, input);
        }
        if (command.equals("find-replace-first")) {
            try {
                return sourceCode.replaceFirst(reference, input);
            } catch (PatternSyntaxException e) {
                LogUtil.e(TAG, "Failed to apply replaceFirst command", e);
                return sourceCode;
            }
        }

        if (command.equals("find-replace-all")) {
            try {
                return sourceCode.replaceAll(reference, input);
            } catch (PatternSyntaxException e) {
                LogUtil.e(TAG, "Failed to apply replaceAll command", e);
                return sourceCode;
            }
        }

        int index = getIndex(lines, reference);
        if (index == -1) {
            return sourceCode;
        }

        if (command.equals("insert")) {
            if ((index + distance - before) < 0) {
                lines.add(0, input);
            } else if ((index + distance - before) > (lines.size() - 1)) {
                lines.add(input);
            } else {
                lines.add(index + distance - before, input);
            }
        }
        if (command.equals("add")) {
            if ((index + distance + after + 1) < 0) {
                lines.add(0, input);
            } else if ((index + distance + after + 1) > (lines.size() - 1)) {
                lines.add(input);
            } else {
                lines.add(index + distance + after + 1, input);
            }
        }

        //old method
        /*
        if(command.equals("replace")){
            int xxx = (int)(index + distance - before -1);
            if ( xxx <0 ){ xxx = 0; }

            int ff = (int)(index + distance - before);
            if ( ff <0 ){ ff = 0; }
            int tt = (int)(index + distance + after +1);
            if ( tt > a.size() ){ tt = a.size(); }
            a.subList(ff , tt).clear();

            if (xxx > a.size()-2){
                a.add(input);
            } else {
                a.add((int)(xxx+1) , input);
            }
        }
        */

        if (command.equals("replace")) {
            if (before == 0 && after == 0) {
                int lineToChange = index + distance;
                if (lineToChange < 0) {
                    lineToChange = 0;
                }
                if (lineToChange > (lines.size() - 1)) {
                    lineToChange = lines.size() - 1;
                }
                lines.set(lineToChange, input);
            } else {
                int lineToChange = index + distance;
                if (lineToChange <= 0) { // ignore backend
                    lineToChange = 0;
                    int from = 1;
                    int to = after + 1;
                    if (to > (lines.size() - 1)) {
                        to = lines.size() - 1;
                    }
                    lines.subList(from, to).clear();
                    lines.set(0, input);
                } else if (lineToChange >= (lines.size() - 1)) { //ignore frontend
                    lineToChange = lines.size() - 1;
                    int from = lineToChange - before;
                    int to = lineToChange;
                    if (from < 0) {
                        from = 0;
                    }
                    lines.set(lineToChange, input);
                    lines.subList(from, to).clear();
                } else {  //handle everything
                    if (before < 0) {
                        before = 0;
                    }
                    if (after < 0) {
                        after = 0;
                    }
                    int from = lineToChange + 1;
                    int to = lineToChange + after;
                    if (to > (lines.size() - 1)) {
                        to = lines.size() - 1;
                    }
                    lines.subList(from, to).clear();
                    lines.set(lineToChange, input);
                    from = lineToChange - before;
                    to = lineToChange;
                    if (from < 0) {
                        from = 0;
                    }
                    lines.subList(from, to).clear();
                }
            }
        }

        return joinLines(lines);
    }

    private static String getStringValue(HashMap<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : value.toString();
    }

    private static int getNumericValue(HashMap<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static String getExceptFirstLine(String sourceCode) {
        ArrayList<String> lines = new ArrayList<>(Arrays.asList(sourceCode.split("\n")));
        if (!lines.isEmpty()) {
            lines.remove(0);
        } else {
            return sourceCode;
        }
        return joinLines(lines);
    }

    private static String getFirstLine(String sourceCode) {
        ArrayList<String> lines = new ArrayList<>(Arrays.asList(sourceCode.split("\n")));
        if (!lines.isEmpty()) {
            return lines.get(0);
        } else {
            return "";
        }
    }

    public static String getInputName(String input) {
        String firstLine = getFirstLine(input);
        if (firstLine.startsWith(">")) {
            firstLine = firstLine.substring(1).trim();
        }
        return firstLine;
    }

    public static String collectXmlCommandBlocks(String sourceCode) {
        String RC = sourceCode;
        try {
            //commands list
            ArrayList<HashMap<String, Object>> commandDefinitions = new ArrayList<>();
            //get command blocks from java code and add them to the list
            collectCommandBlocks(commandDefinitions, RC, XML_COMMAND_START_MARKER, XML_COMMAND_END_MARKER);
            //remove commands lines from java file
            RC = removeCommandBlocks(RC, XML_COMMAND_START_MARKER, XML_COMMAND_END_MARKER);
            //write temporary file
            appendCommandsToTempFile(commandDefinitions);
            return RC;
        } catch (RuntimeException e) {
            LogUtil.e(TAG, "Failed to collect XML command blocks", e);
            writeLog("Failed to collect XML command blocks", e);
            return removeCommandBlocks(sourceCode, XML_COMMAND_START_MARKER, XML_COMMAND_END_MARKER);
        }
    }

    // Write Temporary File
    private static void appendCommandsToTempFile(ArrayList<HashMap<String, Object>> list) {
        ArrayList<HashMap<String, Object>> data = readStoredCommands();
        data.addAll(list);
        FileUtil.writeFile(TEMP_COMMANDS_PATH, GSON.toJson(data));
    }

    public static void clearTempCommands() {
        if (FileUtil.isExistFile(TEMP_COMMANDS_PATH)) {
            FileUtil.deleteFile(TEMP_COMMANDS_PATH);
        }
    }

    public static String processCommandBlocks(String sourceCode) {
        String RC = sourceCode;
        try {
            //commands list
            ArrayList<HashMap<String, Object>> commandDefinitions = new ArrayList<>();
            //get command blocks from java code and add them to the list
            collectCommandBlocks(commandDefinitions, RC, JAVA_COMMAND_START_MARKER, JAVA_COMMAND_END_MARKER);
            //remove commands lines from java file
            RC = removeCommandBlocks(RC, JAVA_COMMAND_START_MARKER, JAVA_COMMAND_END_MARKER);
            //command blocks for xml
            RC = collectXmlCommandBlocks(RC);
            //apply commands
            RC = applyCollectedCommands(commandDefinitions, RC);
            return RC;
        } catch (RuntimeException e) {
            LogUtil.e(TAG, "Failed to apply command blocks", e);
            writeLog("Failed to apply command blocks", e);
            return removeCommandBlocks(sourceCode, JAVA_COMMAND_START_MARKER, JAVA_COMMAND_END_MARKER);
        }
    }

    private static void writeLog(String message, Throwable throwable) {
        String text = "";
        if (FileUtil.isExistFile(TEMP_LOG_PATH)) {
            text = FileUtil.readFile(TEMP_LOG_PATH);
        }
        StringBuilder logEntry = new StringBuilder();
        logEntry.append("\n=>").append(message);
        if (throwable != null) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            throwable.printStackTrace(printWriter);
            printWriter.flush();
            logEntry.append("\n").append(stringWriter);
        }
        FileUtil.writeFile(TEMP_LOG_PATH, text.concat(logEntry.toString()));
    }

    private static void collectCommandBlocks(ArrayList<HashMap<String, Object>> commandDefinitions, String sourceCode, String startMarker, String endMarker) {
        ArrayList<String> lines = new ArrayList<>(Arrays.asList(sourceCode.split("\n")));
        boolean isInsideCommandBlock = false;
        int startIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (isInsideCommandBlock) {
                if (lines.get(i).contains(endMarker)) {
                    Pair<Integer, Integer> blockRange = new Pair<>(startIndex, i);
                    addCommandDefinition(lines, commandDefinitions, blockRange);
                    isInsideCommandBlock = false;
                    startIndex = -1;
                }
            } else if (lines.get(i).contains(startMarker)) {
                startIndex = i;
                isInsideCommandBlock = true;
            }
        }
    }

    private static String joinLines(ArrayList<String> lines) {
        return String.join("\n", lines);
    }

    private static String applyCollectedCommands(ArrayList<HashMap<String, Object>> commandDefinitions, String sourceCode) {
        ArrayList<String> lines = new ArrayList<>(Arrays.asList(sourceCode.split("\n")));
        for (HashMap<String, Object> commandDefinition : commandDefinitions) {
            String reference = getStringValue(commandDefinition, KEY_REFERENCE);
            int distance = getNumericValue(commandDefinition, KEY_DISTANCE);
            int after = getNumericValue(commandDefinition, KEY_AFTER);
            int before = getNumericValue(commandDefinition, KEY_BEFORE);
            String command = getStringValue(commandDefinition, KEY_COMMAND);
            String input = getStringValue(commandDefinition, KEY_INPUT);

            if (command.equals("find-replace")) {
                String temp = joinLines(lines);
                temp = temp.replace(reference, input);
                lines = new ArrayList<>(Arrays.asList(temp.split("\n")));
                continue;
            }
            if (command.equals("find-replace-first")) {
                try {
                    String temp = joinLines(lines);
                    temp = temp.replaceFirst(reference, input);
                    lines = new ArrayList<>(Arrays.asList(temp.split("\n")));
                    continue;
                } catch (PatternSyntaxException e) {
                    LogUtil.e(TAG, "Failed to apply replaceFirst command", e);
                    continue;
                }
            }

            if (command.equals("find-replace-all")) {
                try {
                    String temp = joinLines(lines);
                    temp = temp.replaceAll(reference, input);
                    lines = new ArrayList<>(Arrays.asList(temp.split("\n")));
                    continue;
                } catch (PatternSyntaxException e) {
                    LogUtil.e(TAG, "Failed to apply replaceAll command", e);
                    continue;
                }
            }

            int index = getIndex(lines, reference);
            if (index == -1) {
                continue;
            }


            if (command.equals("insert")) {
                if ((index + distance - before) < 0) {
                    lines.add(0, input);
                } else if ((index + distance - before) > (lines.size() - 1)) {
                    lines.add(input);
                } else {
                    lines.add(index + distance - before, input);
                }
                continue;
            }
            if (command.equals("add")) {
                if ((index + distance + after + 1) < 0) {
                    lines.add(0, input);
                } else if ((index + distance + after + 1) > (lines.size() - 1)) {
                    lines.add(input);
                } else {
                    lines.add(index + distance + after + 1, input);
                }
                continue;
            }

            ///old method
        /*
        if(command.equals("replace")){
            boolean isZ = false;
            int xxx = (int)(index + distance - before -1);
            if ( xxx <=0 ){ isZ = true; xxx = 0; }

            int ff = (int)(index + distance - before);
            if ( ff <0 ){ ff = 0; }
            int tt = (int)(index + distance + after +1);
            if ( tt > a.size() ){ tt = a.size(); }
            a.subList(ff , tt).clear();
            //// fix bug
            if (xxx > a.size()-2){
                a.add(input);
            } else if(isZ) {
                a.add(0, input);
            } else {
                a.add((int)(xxx+1) , input);
            } continue ;}*/

            if (command.equals("replace")) {
                if (before == 0 && after == 0) {
                    int lineToChange = index + distance;
                    if (lineToChange < 0) {
                        lineToChange = 0;
                    }
                    if (lineToChange > (lines.size() - 1)) {
                        lineToChange = lines.size() - 1;
                    }
                    lines.set(lineToChange, input);
                } else {
                    int lineToChange = index + distance;
                    if (lineToChange <= 0) { // ignore backend
                        lineToChange = 0;
                        int from = 1;
                        int to = after + 1;
                        if (to > (lines.size() - 1)) {
                            to = lines.size() - 1;
                        }
                        lines.subList(from, to).clear();
                        lines.set(0, input);
                    } else if (lineToChange >= (lines.size() - 1)) { //ignore frontend
                        lineToChange = lines.size() - 1;
                        int from = lineToChange - before;
                        int to = lineToChange;
                        if (from < 0) {
                            from = 0;
                        }
                        lines.set(lineToChange, input);
                        lines.subList(from, to).clear();
                    } else {  //handle everything
                        if (before < 0) {
                            before = 0;
                        }
                        if (after < 0) {
                            after = 0;
                        }
                        int from = lineToChange + 1;
                        int to = lineToChange + after;
                        if (to > (lines.size() - 1)) {
                            to = lines.size() - 1;
                        }
                        lines.subList(from, to).clear();
                        lines.set(lineToChange, input);
                        from = lineToChange - before;
                        to = lineToChange;
                        if (from < 0) {
                            from = 0;
                        }
                        lines.subList(from, to).clear();
                    }
                }
            }
        }

        return joinLines(lines);
    }

    private static int parseIntOrZero(String s) {
        if (s == null || s.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int getIndex(ArrayList<String> lines, String reference) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains(reference)) {
                return i;
            }
        }
        return -1;
    }

    private static String removeCommandBlocks(String sourceCode, String startMarker, String endMarker) {
        ArrayList<String> lines = new ArrayList<>(Arrays.asList(sourceCode.split("\n")));
        ArrayList<String> resultLines = new ArrayList<>();
        boolean shouldKeepLine = true;
        for (int i = 0; i < lines.size(); i++) {
            if (shouldKeepLine) {
                if (!lines.get(i).contains(startMarker)) {
                    resultLines.add(lines.get(i));
                }
            }

            if (lines.get(i).contains(startMarker)) {
                shouldKeepLine = false;
            }
            if (lines.get(i).contains(endMarker)) {
                shouldKeepLine = true;
            }
        }

        return joinLines(resultLines);
    }

    private static void addCommandDefinition(ArrayList<String> lines, ArrayList<HashMap<String, Object>> commandDefinitions, Pair<Integer, Integer> blockRange) {
        String reference;
        int distance;
        int after;
        int before;
        String command;
        String input = "";

        String line = lines.get(blockRange.first + 1);
        String jsonReference = line.substring(line.indexOf(">") + 1);
        ArrayList<String> parsedReference = GSON.fromJson(jsonReference, Helper.TYPE_STRING);
        reference = parsedReference.get(0);

        line = lines.get(blockRange.first + 2);
        distance = parseIntOrZero(line.substring(line.indexOf(">") + 1));

        line = lines.get(blockRange.first + 3);
        after = parseIntOrZero(line.substring(line.indexOf(">") + 1));

        line = lines.get(blockRange.first + 4);
        before = parseIntOrZero(line.substring(line.indexOf(">") + 1));

        line = lines.get(blockRange.first + 5);
        command = line.substring(line.indexOf(">") + 1);

        for (int i = 0; i < (blockRange.second - blockRange.first - 6); i++) {
            if (i == 0) {
                input = lines.get(blockRange.first + i + 6);
            } else {
                input = input.concat("\n").concat(lines.get(blockRange.first + i + 6));
            }
        }

        HashMap<String, Object> commandDefinition = new HashMap<>();
        commandDefinition.put(KEY_REFERENCE, reference);
        commandDefinition.put(KEY_DISTANCE, distance);
        commandDefinition.put(KEY_AFTER, after);
        commandDefinition.put(KEY_BEFORE, before);
        commandDefinition.put(KEY_COMMAND, command);
        commandDefinition.put(KEY_INPUT, input);
        commandDefinitions.add(commandDefinition);
    }
}
