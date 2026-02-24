package mod.hilal.saif.components;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;

import pro.sketchware.core.EventCodeGenerator;
import pro.sketchware.utility.FileUtil;

public class ComponentExtraCode {

    public final StringBuilder listenerCodeBuilder;
    public final EventCodeGenerator eventCodeGenerator;

    public ComponentExtraCode(EventCodeGenerator h, StringBuilder st) {
        listenerCodeBuilder = st;
        eventCodeGenerator = h;
    }

    public void appendListenerCode(String str) {
        // Aldi's original Components
        if (str.contains("DatePickerFragment")) {
            eventCodeGenerator.eventListenerCode = str;
            return;
        }
        if (str.contains("FragmentStatePagerAdapter")) {
            String temp = eventCodeGenerator.eventLogic;
            if (temp.isEmpty()) {
                eventCodeGenerator.eventLogic = str;
            } else {
                eventCodeGenerator.eventLogic = temp.concat("\r\n\r\n").concat(str);
            }
            return;
        }
        if (str.contains("extends AsyncTask<String, Integer, String>")) {
            String temp = eventCodeGenerator.eventLogic;
            if (temp.isEmpty()) {
                eventCodeGenerator.eventLogic = str;
            } else {
                eventCodeGenerator.eventLogic = temp.concat("\r\n\r\n").concat(str);
            }
            return;
        }

        // Hilal's components
        String path = FileUtil.getExternalStorageDir().concat("/.sketchware/data/system/listeners.json");
        try {
            if (FileUtil.isExistFile(path) && !FileUtil.readFile(path).isEmpty() && !FileUtil.readFile(path).equals("[]")) {
                JSONArray arr = new JSONArray(FileUtil.readFile(path));
                if (arr.length() > 0) {
                    for (int i = 0; i < arr.length(); i++) {
                        String c = arr.getJSONObject(i).getString("code");
                        String f = getFirstLine(c);
                        if (!arr.getJSONObject(i).isNull("s") && str.contains(f)) {
                            String q = arr.getJSONObject(i).getString("s");
                            if (q.equals("true")) {
                                String temp = eventCodeGenerator.eventLogic;
                                if (temp.isEmpty()) {
                                    eventCodeGenerator.eventLogic = str.replace(f, "");
                                    return;
                                } else {
                                    eventCodeGenerator.eventLogic = temp.concat("\r\n\r\n").concat(str.replace(f, ""));
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (listenerCodeBuilder.length() > 0 && !str.isEmpty()) {
                listenerCodeBuilder.append("\r\n");
                listenerCodeBuilder.append("\r\n");
                listenerCodeBuilder.append(str);
            }
        }


        ///others
        if (listenerCodeBuilder.length() > 0 && !str.isEmpty()) {
            listenerCodeBuilder.append("\r\n");
            listenerCodeBuilder.append("\r\n");
        }
        listenerCodeBuilder.append(str);
    }

    public String getFirstLine(String con) {
        ArrayList<String> list = new ArrayList<>(Arrays.asList(con.split("\n")));
        if (!list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                String a = list.get(i);
                if (!a.isEmpty()) {
                    return a.trim();
                }
            }
        }
        return "qTHwdyRjVoEqNjuXx";
    }
}
