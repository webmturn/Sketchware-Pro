package pro.sketchware.core.codegen;

import java.io.IOException;
import pro.sketchware.beans.CustomListener;
import pro.sketchware.util.CustomEventsStore;
import pro.sketchware.util.LogUtil;

import java.util.ArrayList;
import java.util.Arrays;


public class ComponentExtraCode {

    public final StringBuilder listenerCodeBuilder;
    public final EventCodeGenerator eventCodeGenerator;

    public ComponentExtraCode(EventCodeGenerator h, StringBuilder st) {
        listenerCodeBuilder = st;
        eventCodeGenerator = h;
    }

    public void appendListenerCode(String listenerCode) {
        // Aldi's original Components
        if (listenerCode.contains("DatePickerFragment")) {
            eventCodeGenerator.eventListenerCode = listenerCode;
            return;
        }
        if (listenerCode.contains("FragmentStatePagerAdapter")) {
            String temp = eventCodeGenerator.eventLogic;
            if (temp.isEmpty()) {
                eventCodeGenerator.eventLogic = listenerCode;
            } else {
                eventCodeGenerator.eventLogic = temp.concat("\r\n\r\n").concat(listenerCode);
            }
            return;
        }
        if (listenerCode.contains("extends AsyncTask<String, Integer, String>")) {
            String temp = eventCodeGenerator.eventLogic;
            if (temp.isEmpty()) {
                eventCodeGenerator.eventLogic = listenerCode;
            } else {
                eventCodeGenerator.eventLogic = temp.concat("\r\n\r\n").concat(listenerCode);
            }
            return;
        }
        if (listenerCode.contains("_onSQLiteError")) {
            String temp = eventCodeGenerator.eventLogic;
            if (temp.isEmpty()) {
                eventCodeGenerator.eventLogic = listenerCode;
            } else {
                eventCodeGenerator.eventLogic = temp.concat("\r\n\r\n").concat(listenerCode);
            }
            return;
        }

        // Hilal's components
        try {
            for (CustomListener listener : CustomEventsStore.INSTANCE.readListeners()) {
                String firstLine = getFirstLine(listener.getCode());
                if (listener.isIndependent() && listenerCode.contains(firstLine)) {
                    String extraCode = listenerCode.replace(firstLine, "");
                    String existingCode = eventCodeGenerator.eventLogic;
                    eventCodeGenerator.eventLogic = existingCode.isEmpty()
                            ? extraCode : existingCode.concat("\r\n\r\n").concat(extraCode);
                    return;
                }
            }
        } catch (IOException e) {
            LogUtil.w("ComponentExtraCode", "Could not read custom listeners", e);
        }

        ///others
        if (listenerCodeBuilder.length() > 0 && !listenerCode.isEmpty()) {
            listenerCodeBuilder.append("\r\n");
            listenerCodeBuilder.append("\r\n");
        }
        listenerCodeBuilder.append(listenerCode);
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
