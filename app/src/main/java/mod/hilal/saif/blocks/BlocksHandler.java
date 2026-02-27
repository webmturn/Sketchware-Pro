package mod.hilal.saif.blocks;

import androidx.annotation.ColorInt;

import com.besome.sketch.editor.LogicEditorActivity;

import java.util.ArrayList;
import java.util.HashMap;

import mod.hey.studios.util.Helper;
import mod.hilal.saif.activities.tools.ConfigActivity;
import pro.sketchware.R;
import pro.sketchware.blocks.ExtraBlocks;
import pro.sketchware.utility.ThemeUtils;

public class BlocksHandler {

    public static void builtInBlocks(ArrayList<HashMap<String, Object>> arrayList) {
        ExtraBlocks.extraBlocks(arrayList);

        HashMap<String, Object> blockDef = new HashMap<>();
        blockDef.put("name", "CommandBlockJava");
        blockDef.put("type", "c");
        blockDef.put("typeName", "");
        blockDef.put(
                "code",
                "/*-JX4UA2y_f1OckjjvxWI.bQwRei-sLEsBmds7ArsRfi0xSFEP3Php97kjdMCs5ed\n"
                        + ">[%1$s]\n"
                        + ">%2$s\n"
                        + ">%3$s\n"
                        + ">%4$s\n"
                        + ">%5$s\n"
                        + "%6$s\n"
                        + "BpWI8U4flOpx8Ke66QTlZYBA_NEusQ7BN-D0wvZs7ArsRfi0.EP3Php97kjdMCs*/");
        blockDef.put("color", "#493F5A");
        blockDef.put("palette", "0");
        blockDef.put(
                "spec",
                "Java Command Block: reference %s distance %d frontend %d backend %d command"
                        + " %m.Command");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "CommandBlockXML");
        blockDef.put("type", "c");
        blockDef.put("typeName", "");
        blockDef.put(
                "code",
                "/*AXAVajPNTpbJjsz-NGVTp08YDzfI-04kA7ZsuCl4GHqTQQiuWL45sV6Vf4gwK\n"
                        + ">[%1$s]\n"
                        + ">%2$s\n"
                        + ">%3$s\n"
                        + ">%4$s\n"
                        + ">%5$s\n"
                        + ">%6$s\n"
                        + "%7$s\n"
                        + "Ui5_PNTJb21WO6OuGwQ3psk3su1LIvyXo_OAol-kVQBC5jtN_DcPLaRCJ0yXp*/");
        blockDef.put("color", "#493F5A");
        blockDef.put("palette", "0");
        blockDef.put(
                "spec",
                "XML Command Block: reference %s distance %d frontend %d backend %d command"
                        + " %m.Command xml name %s.inputOnly");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "viewOnClick");
        blockDef.put("type", "c");
        blockDef.put("typeName", "");
        blockDef.put(
                "code",
                "%s.setOnClickListener(new View.OnClickListener() {\n"
                        + "@Override\n"
                        + "public void onClick(View _view) {\n"
                        + "%s\n"
                        + "}\n"
                        + "});");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "0");
        blockDef.put("spec", "When %m.view clicked");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setRecyclerViewLayoutParams");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put(
                "code",
                "RecyclerView.LayoutParams _lp = new"
                        + " RecyclerView.LayoutParams(ViewGroup.LayoutParams.%s,"
                        + " ViewGroup.LayoutParams.%s);\n"
                        + "_view.setLayoutParams(_lp);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "7");
        blockDef.put(
                "spec", "set RecyclerViewLayoutParams width %m.LayoutParam height %m.LayoutParam");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "refreshingList");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "%s.invalidateViews();");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "0");
        blockDef.put("spec", "%m.listview invalidate views");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "ListViewAddHeader");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "%s.addHeaderView(%s,%s,%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "0");
        blockDef.put("spec", "%m.listview add Header view %m.view data %s selectable? %b");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "ListViewAddFooter");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "%s.addFooterView(%s,%s,%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "0");
        blockDef.put("spec", "%m.listview add Footer view %m.view data %s selectable? %b");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "listViewRemoveHeader");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "%s.removeHeaderView(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "0");
        blockDef.put("spec", "%m.listview remove Header %m.view");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "listViewRemoveFooter");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "%s.removeFooterView(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "0");
        blockDef.put("spec", "%m.listview remove Footer %m.view");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "progressdialogCreate");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "%s = new ProgressDialog(%s.this);");
        blockDef.put("color", "#29A7E4");
        blockDef.put("palette", "7");
        blockDef.put("spec", "%m.progressdialog Create in %m.activity");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "listViewSetSelection");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "%s.setSelection((int)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "7");
        blockDef.put("spec", "%m.listview set selection %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "EditTextdiableSuggestion");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "%s.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "0");
        blockDef.put("spec", "%m.edittext disable suggestions");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "EditTextLines");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "%s.setLines(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "0");
        blockDef.put("spec", "%m.edittext set lines %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "EditTextSingleLine");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "%s.setSingleLine(%2$s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "0");
        blockDef.put("spec", "%m.edittext singleLine? %b");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "EditTextShowError");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "((EditText)%s).setError(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "0");
        blockDef.put("spec", "%m.edittext show error %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "EditTextSelectAll");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "((EditText)%s).selectAll();");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "0");
        blockDef.put("spec", "%m.edittext select all text");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "EditTextSetSelection");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "((EditText)%s).setSelection((int)%s, (int)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "0");
        blockDef.put("spec", "%m.edittext set selection start %d end %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "EditTextSetMaxLines");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "((EditText)%s).setMaxLines((int)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "0");
        blockDef.put("spec", "%m.edittext set max lines %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "EdittextGetselectionStart");
        blockDef.put("type", "d");
        blockDef.put("typeName", "");
        blockDef.put("code", "%s.getSelectionStart()");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "0");
        blockDef.put("spec", "%m.edittext get selection start");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "EdittextGetselectionEnd");
        blockDef.put("type", "d");
        blockDef.put("typeName", "");
        blockDef.put("code", "%s.getSelectionEnd()");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "0");
        blockDef.put("spec", "%m.edittext get selection end");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "performClick");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.performClick();");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "0");
        blockDef.put("spec", "%m.view performClick");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "AsyncTaskExecute");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "new %s().execute(%s);");
        blockDef.put("color", "#29A7E4");
        blockDef.put("palette", "7");
        blockDef.put("spec", "%m.asynctask execute message %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "AsyncTaskPublishProgress");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "publishProgress((int)%s);");
        blockDef.put("color", "#29A7E4");
        blockDef.put("palette", "7");
        blockDef.put("spec", "publish progress %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "progressdialogSetCanceledOutside");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "%s.setCanceledOnTouchOutside(%s);");
        blockDef.put("color", "#29A7E4");
        blockDef.put("palette", "7");
        blockDef.put("spec", "%m.progressdialog setCancelableWhenTouchOutside %b");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "datePickerDialogShow");
        blockDef.put("type", " ");
        blockDef.put(
                "code",
                "DialogFragment datePicker = new DatePickerFragment();\r\n"
                        + "datePicker.show(getSupportFragmentManager(), \"datePicker\");");
        blockDef.put("color", "#2CA5E2");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "DatePickerDialog show");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "timePickerDialogShow");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.show();");
        blockDef.put("color", "#2CA5E2");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.timepickerdialog show");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "imageCrop");
        blockDef.put("type", " ");
        blockDef.put("code", "SketchwareUtil.CropImage(this, %s, (int) %s);");
        blockDef.put("color", "#2CA5E2");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "CropImageView fromFilePath %s RequestCode %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "isConnected");
        blockDef.put("type", " ");
        blockDef.put("code", "SketchwareUtil.isConnected(getApplicationContext())");
        blockDef.put("color", "#2CA5E2");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "isConnected");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "getClipboard");
        blockDef.put("type", "s");
        blockDef.put("typeName", "");
        blockDef.put("code", "SketchwareUtil.getClipboardText(getApplicationContext())");
        blockDef.put("color", "#8A55D7");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "get clipboard text");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "customImport");
        blockDef.put("type", " ");
        blockDef.put("code", "import %s;");
        blockDef.put("color", "#EE7D15");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "import %s.import");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "customImport2");
        blockDef.put("type", " ");
        blockDef.put("code", "import %s;");
        blockDef.put("color", "#EE7D15");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "import %m.import");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "customToast");
        blockDef.put("type", " ");
        blockDef.put(
                "code",
                "SketchwareUtil.CustomToast(getApplicationContext(), %s, %s, %s, %s, %s,"
                        + " SketchwareUtil.%s);");
        blockDef.put("color", "#8A55D7");
        blockDef.put("palette", "-1");
        blockDef.put(
                "spec",
                "CustomToast %s textColor %m.color textSize %d bgColor %m.color cornerRadius %d"
                        + " gravity %m.gravity_t");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "customToastWithIcon");
        blockDef.put("type", " ");
        blockDef.put(
                "code",
                "SketchwareUtil.CustomToastWithIcon(getApplicationContext(), %s, %s, %s, %s, %s,"
                        + " SketchwareUtil.%s, R.drawable.%s);");
        blockDef.put("color", "#8A55D7");
        blockDef.put("palette", "-1");
        blockDef.put(
                "spec",
                "CustomToastWithIcon %s textColor %m.color textSize %d bgColor %m.color"
                        + " cornerRadius %d gravity %m.gravity_t Icon %m.resource");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "LightStatusBar");
        blockDef.put("type", " ");
        blockDef.put(
                "code",
                "getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);\r\n"
                        + "getWindow().setStatusBarColor(0xFFFFFFFF);");
        blockDef.put("color", "#2CA5E2");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "LightStatusBar");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "hideKeyboard");
        blockDef.put("type", " ");
        blockDef.put("code", "SketchwareUtil.hideKeyboard(getApplicationContext());");
        blockDef.put("color", "#2CA5E2");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "Hide keyboard");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "showKeyboard");
        blockDef.put("type", " ");
        blockDef.put("code", "SketchwareUtil.showKeyboard(getApplicationContext());");
        blockDef.put("color", "#2CA5E2");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "Show keyboard");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "progressdialogSetTitle");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setTitle(%s);");
        blockDef.put("color", "#2CA5E2");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.progressdialog setTitle %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "progressdialogSetMessage");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setMessage(%s);");
        blockDef.put("color", "#2CA5E2");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.progressdialog setMessage %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "progressdialogSetMax");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setMax((int)%s);");
        blockDef.put("color", "#2CA5E2");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.progressdialog setMax %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "progressdialogSetProgress");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setProgress((int)%s);");
        blockDef.put("color", "#2CA5E2");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.progressdialog setProgress %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "progressdialogSetCancelable");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setCancelable(%s);");
        blockDef.put("color", "#2CA5E2");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.progressdialog setCancelable %b");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "progressdialogSetCanceled");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setCanceledOnTouchOutside(%s);");
        blockDef.put("color", "#2CA5E2");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.progressdialog setCanceledOnTouchOutside %b");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "progressdialogSetStyle");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setProgressStyle(ProgressDialog.%s);");
        blockDef.put("color", "#2CA5E2");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.progressdialog setProgressStyle %m.styleprogress");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "progressdialogDismiss");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.dismiss();");
        blockDef.put("color", "#2CA5E2");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.progressdialog dismiss");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "progressdialogShow");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.show();");
        blockDef.put("color", "#2CA5E2");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.progressdialog show");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "startService");
        blockDef.put("type", " ");
        blockDef.put("code", "startService(new Intent(getApplicationContext(), %s.class));");
        blockDef.put("color", "#2CA5E2");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "startService %m.activity");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "stopService");
        blockDef.put("type", " ");
        blockDef.put("code", "stopService(new Intent(getApplicationContext(), %s.class));");
        blockDef.put("color", "#2CA5E2");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "stopService %m.activity");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "sendBroadcast");
        blockDef.put("type", " ");
        blockDef.put("code", "sendBroadcast(%s);");
        blockDef.put("color", "#2CA5E2");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "sendBroadcast %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "startActivityWithChooser");
        blockDef.put("type", " ");
        blockDef.put("code", "startActivity(Intent.createChooser(%s, %s));");
        blockDef.put("color", "#2CA5E2");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "StartActivity %m.intent with Chooser %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "finishAffinity");
        blockDef.put("type", "f");
        blockDef.put("code", "finishAffinity();");
        blockDef.put("color", "#2CA5E2");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "Finish Affinity");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "ternaryString");
        blockDef.put("type", "s");
        blockDef.put("code", "%s ? %s : %s");
        blockDef.put("color", "#E1A928");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%b ? %s : %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "ternaryNumber");
        blockDef.put("type", "d");
        blockDef.put("code", "%s ? (int)%s : (int)%s");
        blockDef.put("color", "#E1A928");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%b ? %d : %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "forLoopIncrease");
        blockDef.put("type", "c");
        blockDef.put("code", "for (%s = %s; %s; %s++) {\r\n%s\r\n}");
        blockDef.put("color", "#E1A928");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "for %m.varInt = %d; %b; %m.varInt++");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "notifyDataSetChanged");
        blockDef.put("type", " ");
        blockDef.put("code", "notifyDataSetChanged();");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "RefreshData");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "getLastVisiblePosition");
        blockDef.put("type", "d");
        blockDef.put("code", "%s.getLastVisiblePosition()");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.listview getLastVisiblePosition");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "listscrollparam");
        blockDef.put("type", "d");
        blockDef.put("code", "ListView.%s");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.listscrollparam");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "menuInflater");
        blockDef.put("type", " ");
        blockDef.put("code", "getMenuInflater().inflate(R.menu.%s, menu);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "Menu get menu from file %m.menu");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "menuAddItem");
        blockDef.put("type", " ");
        blockDef.put("code", "menu.add(0, %s, 0, %s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "Menu add id %d title %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "menuAddMenuItem");
        blockDef.put("type", " ");
        blockDef.put(
                "code",
                "MenuItem %1$s = menu.add(Menu.NONE, %2$s, Menu.NONE, %3$s);\r\n"
                        + "%1$s.setIcon(R.drawable.%4$s);\r\n"
                        + "%s.setShowAsAction(MenuItem.%5$s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put(
                "spec",
                "%m.menuitem add id %d title %s icon %m.resource showAsAction %m.menuaction");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "menuAddSubmenu");
        blockDef.put("type", "c");
        blockDef.put("code", "SubMenu %s = menu.addSubMenu(%s);\r\n%s");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "Menu add %m.submenu title %s;");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "submenuAddItem");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.add(0, %s, 0, %s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.submenu add id %d title %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "getAssetFile");
        blockDef.put("type", " ");
        blockDef.put("code", "java.io.InputStream %s = getAssets().open(%s);");
        blockDef.put("color", "#A1887F");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.inputstream getFileFromAsset path %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "renameFile");
        blockDef.put("type", " ");
        blockDef.put(
                "code",
                "{\n"
                        + "java.io.File dYx4Y = new java.io.File(%1$s);\n"
                        + "java.io.File e5Cyk = new java.io.File(%2$s);\n"
                        + "dYx4Y.renameTo(e5Cyk);\n"
                        + "}");
        blockDef.put("color", "#A1887F");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "rename file path %s to %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "copyAssetFile");
        blockDef.put("type", "s");
        blockDef.put("code", "SketchwareUtil.copyFromInputStream(%s)");
        blockDef.put("color", "#A1887F");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.inputstream to String");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "sortListmap");
        blockDef.put("type", " ");
        blockDef.put("code", "SketchwareUtil.sortListMap(%s, %s, %s, %s);");
        blockDef.put("color", "#CC5B21");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "sort %m.listMap key %s isNumber %b isAscending %b");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "deleteMapFromListmap");
        blockDef.put("type", "a");
        blockDef.put("code", "%2$s.remove(%1$s);");
        blockDef.put("color", "#CC5B21");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "delete %m.varMap of %m.listMap");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "html");
        blockDef.put("type", "s");
        blockDef.put("typeName", "");
        blockDef.put("code", "Html.fromHtml(%s)");
        blockDef.put("color", "#5CB721");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "html %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "reverse");
        blockDef.put("type", "s");
        blockDef.put("code", "new StringBuilder(%s).reverse().toString()");
        blockDef.put("color", "#5CB721");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "reverse %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "toHashCode");
        blockDef.put("type", "d");
        blockDef.put("code", "%s.hashCode()");
        blockDef.put("color", "#5CB721");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "toHashCode %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "stringMatches");
        blockDef.put("type", "b");
        blockDef.put("code", "%s.matches(%s)");
        blockDef.put("color", "#5CB721");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%s matches RegExp %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "stringReplaceFirst");
        blockDef.put("type", "s");
        blockDef.put("code", "%s.replaceFirst(%s, %s)");
        blockDef.put("color", "#5CB721");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%s replace first RegExp %s with %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "stringReplaceAll");
        blockDef.put("type", "s");
        blockDef.put("code", "%s.replaceAll(%s, %s)");
        blockDef.put("color", "#5CB721");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%s replace all RegExp %s with %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "stringSplitToList");
        blockDef.put("type", " ");
        blockDef.put("code", "%3$s = new ArrayList<String>(Arrays.asList(%1$s.split(%2$s)));");
        blockDef.put("color", "#5CB721");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "split %s RegExp %s into %m.listStr");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "mapContainValue");
        blockDef.put("type", "b");
        blockDef.put("code", "%s.containsValue(%s)");
        blockDef.put("color", "#EE7D15");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.varMap contain value %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "getHeight");
        blockDef.put("type", "d");
        blockDef.put("typeName", "");
        blockDef.put("code", "%s.getHeight()");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.view getHeight");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "getWidth");
        blockDef.put("type", "d");
        blockDef.put("typeName", "");
        blockDef.put("code", "%s.getWidth()");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.view getWidth");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "removeView");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "%s.removeView(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.view removeView %m.view");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "removeViews");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "%s.removeAllViews();");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.view removeAllViews");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "addView");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "%s.addView(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.view addView %m.view");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "addViews");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "%s.addView(%s, %s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.view addView %m.view index %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setGravity");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setGravity(Gravity.%s | Gravity.%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.view setGravity %m.gravity_v %m.gravity_h");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setImageIdentifier");
        blockDef.put("type", " ");
        blockDef.put(
                "code",
                "%s.setImageResource(getResources().getIdentifier(%s, \"drawable\","
                        + " getPackageName()));");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.imageview set image by name %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setImageCustomRes");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setImageResource(R.drawable.%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.imageview setImage %m.image");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "getRating");
        blockDef.put("type", "d");
        blockDef.put("code", "%s.getRating()");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.ratingbar getRating");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setRating");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setRating((float)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.ratingbar setRating%d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setNumStars");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setNumStars((int)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.ratingbar setNumStars %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setStepSize");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setStepSize((float)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.ratingbar setStepSize %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "timepickerSetIs24Hour");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setIs24HourView(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.timepicker setIs24Hour %b");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "timepickerSetCurrentHour");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setCurrentHour((int)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.timepicker setCurrentHour %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "timepickerSetCurrentMinute");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setCurrentMinute((int)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.timepicker setCurrentMinute%d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "timepickerSetHour");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setHour((int)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.timepicker setHour %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "timepickerSetMinute");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setMinute((int)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.timepicker setMinute%d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "autoComSetData");
        blockDef.put("type", " ");
        blockDef.put(
                "code",
                "%s.setAdapter(new ArrayAdapter<String>(getBaseContext(),"
                        + " android.R.layout.simple_list_item_1, %s));");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.actv setListData %m.listStr");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setThreshold");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setThreshold(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.mactv setThreshold %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setTokenizer");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.mactv CommaTokenizer");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "multiAutoComSetData");
        blockDef.put("type", " ");
        blockDef.put(
                "code",
                "%s.setAdapter(new ArrayAdapter<String>(getBaseContext(),"
                        + " android.R.layout.simple_list_item_1, %s));");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.mactv setListData %m.listStr");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "listSetSelector");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setSelector(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.listview setSelector %m.color");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "gridSetCustomViewData");
        blockDef.put("type", " ");
        blockDef.put("code", "");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.gridview setGridCustomViewData %m.listMap");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "gridSetNumColumns");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setNumColumns((int)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.gridview setNumColumns %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "gridSetColumnWidth");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setColumnWidth((int)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.gridview setColumnWidth %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "gridSetVerticalSpacing");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setVerticalSpacing((int)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.gridview setVerticalSpacing %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "gridSetHorizontalSpacing");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setHorizontalSpacing((int)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.gridview setHorizontalSpacing %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "gridSetStretchMode");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setStretchMode(GridView.%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.gridview setStretchMode %m.gridstretchmode");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "videoviewSetVideoUri");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setVideoURI(Uri.parse(%s));");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.videoview setVideoUri %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "videoviewStart");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.start();");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.videoview start");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "videoviewPause");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.pause();");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.videoview pause");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "videoviewStop");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.stopPlayback();");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.videoview stopPlayback");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "videoviewIsPlaying");
        blockDef.put("type", "b");
        blockDef.put("code", "%s.isPlaying()");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.videoview isPlaying");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "videoviewCanPause");
        blockDef.put("type", "b");
        blockDef.put("code", "%s.canPause()");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.videoview canPause");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "videoviewCanSeekForward");
        blockDef.put("type", "b");
        blockDef.put("code", "%s.canSeekForward()");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.videoview canSeekForward");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "videoviewCanSeekBackward");
        blockDef.put("type", "b");
        blockDef.put("code", "%s.canSeekBackward()");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.videoview canSeekBackward");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "videoviewGetDuration");
        blockDef.put("type", "d");
        blockDef.put("code", "%s.getDuration()");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.videoview getDuration");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "videoviewGetCurrentPosition");
        blockDef.put("type", "d");
        blockDef.put("code", "%s.getCurrentPosition()");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.videoview getCurrentPosition");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "listSetTranscriptMode");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "%s.setTranscriptMode(ListView.%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.listview setTranscriptMode %m.transcriptmode");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "listSetStackFromBottom");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "%s.setStackFromBottom(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.listview setStackFromBottom %b");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setElevation");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setElevation((float)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.view setElevation %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setTextSize");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setTextSize((int)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.textview setTextSize %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setColorFilterView");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.getBackground().setColorFilter(%s, PorterDuff.Mode.%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.view setColorFilter %m.color with %m.porterduff");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setCornerRadiusView");
        blockDef.put("type", " ");
        blockDef.put(
                "code",
                "%s.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a,"
                        + " int b) { this.setCornerRadius(a); this.setColor(b); return this; }"
                        + " }.getIns((int)%s, %s));");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.view setCornerRadius %d color %m.color");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setGradientBackground");
        blockDef.put("type", " ");
        blockDef.put(
                "code",
                "%s.setBackground(new GradientDrawable(GradientDrawable.Orientation.BR_TL, new"
                        + " int[] {%s,%s}));");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.view setGradientBackground %m.color and %m.color");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setStrokeView");
        blockDef.put("type", " ");
        blockDef.put(
                "code",
                "%s.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a,"
                        + " int b, int c) { this.setStroke(a, b); this.setColor(c); return this; }"
                        + " }.getIns((int)%s, %s, %s));");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.view setStroke %d strokeColor %m.color bgColor %m.color");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setRadiusAndStrokeView");
        blockDef.put("type", " ");
        blockDef.put(
                "code",
                "%s.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a,"
                        + " int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c);"
                        + " this.setColor(d); return this; } }.getIns((int)%s, (int)%s, %s, %s));");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put(
                "spec",
                "%m.view setCornerRadius %d stroke %d strokeColor %m.color bgColor %m.color");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "showSnackbar");
        blockDef.put("type", "c");
        blockDef.put(
                "code",
                "com.google.android.material.snackbar.Snackbar.make(%s, %s,"
                        + " com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).setAction(%s,"
                        + " new View.OnClickListener(){\r\n"
                        + "@Override\r\n"
                        + "public void onClick(View _view) {\r\n"
                        + "%s\r\n"
                        + "}\r\n"
                        + "}).show();");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.view showSnackbar text %s actionText %s onClick");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "addTab");
        blockDef.put("type", " ");
        blockDef.put("code", "%1$s.addTab(%1$s.newTab().setText(%2$s));");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.tablayout addTabTitle %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setupWithViewPager");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setupWithViewPager(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.tablayout setupWithViewPager %m.viewpager");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setInlineLabel");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setInlineLabel(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.tablayout setInlineLabel %b");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setTabTextColors");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setTabTextColors(%s, %s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.tablayout setTabTextColors Normal %m.color Selected %m.color");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setTabRippleColor");
        blockDef.put("type", " ");
        blockDef.put(
                "code",
                "%s.setTabRippleColor(new android.content.res.ColorStateList(new int[][]{new"
                        + " int[]{android.R.attr.state_pressed}}, \r\n\r\n"
                        + "new int[] {%s}));");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.tablayout setTabRippleColor %m.color");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setSelectedTabIndicatorColor");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setSelectedTabIndicatorColor(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.tablayout setSelectedTabIndicatorColor %m.color");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setSelectedTabIndicatorHeight");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setSelectedTabIndicatorHeight(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.tablayout setSelectedTabIndicatorHeight %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "returnTitle");
        blockDef.put("type", "f");
        blockDef.put("code", "return %s;");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "return Title %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "returnFragment");
        blockDef.put("type", "f");
        blockDef.put("code", "return new %s();");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "return Fragment %m.activity");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "bottomMenuAddItem");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.getMenu().add(0, %s, 0, %s).setIcon(R.drawable.%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.bottomnavigation add item id %d title %s icon %m.resource");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "codeviewSetCode");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setCode(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.codeview setCode %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "codeviewSetTheme");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setTheme(Theme.%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.codeview setTheme %m.cv_theme");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "codeviewSetLanguage");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setLanguage(Language.%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.codeview setLanguage %m.cv_language");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "codeviewApply");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.apply();");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.codeview apply");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "fabIcon");
        blockDef.put("type", " ");
        blockDef.put("code", "_fab.setImageResource(R.drawable.%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "FAB set icon %m.resource");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "fabSize");
        blockDef.put("type", " ");
        blockDef.put("code", "_fab.setSize(FloatingActionButton.SIZE_%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "FAB setSize %m.fabsize");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "fabVisibility");
        blockDef.put("type", " ");
        blockDef.put("code", "_fab.%s();");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "FAB setVisibility %m.fabvisible");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setBgDrawable");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setBackgroundDrawable(getResources().getDrawable(R.drawable.%s));");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.view setBackgroundDrawable %m.drawable");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setCardBackgroundColor");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setCardBackgroundColor(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.cardview setCardBackgroundColor %m.color");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setCardRadius");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setRadius((float)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.cardview setCornerRadius %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setCardElevation");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setCardElevation((float)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.cardview setCardElevation %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setPreventCornerOverlap");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setPreventCornerOverlap(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.cardview setPreventCornerOverlap %b");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setUseCompatPadding");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setUseCompatPadding(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.cardview setUseCompatPadding %b");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "spnSetCustomViewData");
        blockDef.put("type", " ");
        blockDef.put("code", "");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.spinner setSpinnerCustomViewData %m.listMap");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "lottieSetAnimationFromAsset");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setAnimation(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.lottie setAnimationFromAsset %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "lottieSetAnimationFromJson");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setAnimationFromJson(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.lottie setAnimationFromJson %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "lottieSetAnimationFromUrl");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setAnimationFromUrl(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.lottie setAnimationFromUrl %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "lottieSetRepeatCount");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setRepeatCount((int)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.lottie setRepeatCount %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "lottieSetSpeed");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setSpeed((float)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.lottie setSpeed %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "recyclerSetCustomViewData");
        blockDef.put("type", " ");
        blockDef.put("code", "");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.recyclerview setRecyclerCustomViewData %m.listMap");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "recyclerSetLayoutManager");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setLayoutManager(new LinearLayoutManager(this));");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.recyclerview setLayoutManager");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "recyclerSetLayoutManagerHorizontal");
        blockDef.put("type", " ");
        blockDef.put(
                "code",
                "%s.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,"
                        + " false));");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.recyclerview set Horizontal LayoutManager");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "recyclerSetHasFixedSize");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setHasFixedSize(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.recyclerview setHasFixedSize %b");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "recyclerSmoothScrollToPosition");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.smoothScrollToPosition((int)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.recyclerview smoothScrollToPosition %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "recyclerScrollToPositionWithOffset");
        blockDef.put("type", " ");
        blockDef.put(
                "code",
                " ((LinearLayoutManager) %s.getLayoutManager()).scrollToPositionWithOffset((int)%s,"
                        + " (int)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.recyclerview scrollToPosition %d offset %d ");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "recyclerscrollparam");
        blockDef.put("type", "d");
        blockDef.put("code", "RecyclerView.%s");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.recyclerscrollparam");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "pagerscrollparam");
        blockDef.put("type", "d");
        blockDef.put("code", "ViewPager.%s");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.pagerscrollparam");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "pagerSetCustomViewData");
        blockDef.put("type", " ");
        blockDef.put("code", "");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.viewpager setPagerCustomViewData %m.listMap");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "pagerSetFragmentAdapter");
        blockDef.put("type", " ");
        blockDef.put("code", "%2$s.setTabCount(%3$s);\r\n%1$s.setAdapter(%2$s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.viewpager setFragmentAdapter %m.fragmentAdapter TabCount %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "pagerGetOffscreenPageLimit");
        blockDef.put("type", "d");
        blockDef.put("code", "%s.getOffscreenPageLimit()");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.viewpager getOffscreenPageLimit");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "pagerSetOffscreenPageLimit");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setOffscreenPageLimit((int)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.viewpager setOffscreenPageLimit %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "pagerGetCurrentItem");
        blockDef.put("type", "d");
        blockDef.put("code", "%s.getCurrentItem()");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.viewpager getCurrentItem");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "pagerSetCurrentItem");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setCurrentItem((int)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.viewpager setCurrentItem %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "onSwipeRefreshLayout");
        blockDef.put("type", "c");
        blockDef.put(
                "code",
                "%s.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {\r\n"
                        + "@Override\r\n"
                        + "public void onRefresh() {\r\n"
                        + "%s\r\n"
                        + "}\r\n"
                        + "});");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "When %m.swiperefreshlayout refreshed");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setRefreshing");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setRefreshing(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.swiperefreshlayout setRefreshing %b");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "viewOnLongClick");
        blockDef.put("type", "c");
        blockDef.put(
                "code",
                "%s.setOnLongClickListener(new View.OnLongClickListener() {\r\n"
                        + "@Override\r\n"
                        + "public boolean onLongClick(View _view) {\r\n"
                        + "%s\r\n"
                        + "return true;\r\n"
                        + "}\r\n"
                        + "});");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "When %m.view long clicked");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "viewOnTouch");
        blockDef.put("type", "c");
        blockDef.put(
                "code",
                "%s.setOnTouchListener(new View.OnTouchListener(){\r\n"
                        + "@Override\r\n"
                        + "public boolean onTouch(View _view, MotionEvent _motionEvent){\r\n"
                        + "%s\r\n"
                        + "return true;\r\n"
                        + "}\r\n"
                        + "});");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "When %m.view touched");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "checkboxOnChecked");
        blockDef.put("type", "c");
        blockDef.put(
                "code",
                "%s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {\r\n"
                        + "@Override\r\n"
                        + "public void onCheckedChanged(CompoundButton cb, boolean isChecked) {\r\n"
                        + "%s\r\n"
                        + "}});");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "When %m.checkbox checked");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "checkboxIsChecked");
        blockDef.put("type", "b");
        blockDef.put("code", "isChecked");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "isChecked");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "getBadgeCount");
        blockDef.put("type", "d");
        blockDef.put("code", "%s.getBadgeCount();");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.badgeview getBadgeCount");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setBadgeNumber");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setBadgeCount(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.badgeview setBadgeNumber %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setBadgeString");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setBadgeCount(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.badgeview setBadgeString %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setBadgeBackground");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setBadgeBackground(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.badgeview setBadgeBackground %m.color");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setBadgeTextColor");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setTextColor(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.badgeview setBadgeTextColor %m.color");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setBadgeTextSize");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setTextSize((int)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.badgeview setBadgeTextSize %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setCustomLetter");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setCustomLetter(new String[]%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.sidebar setCustomLetter String[] %s.inputOnly");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setBubbleColor");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setBubbleColor(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "BubbleLayout %m.view setBubbleColor %m.color");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setBubbleStrokeColor");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setStrokeColor(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "BubbleLayout %m.view setStrokeColor %m.color");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setBubbleStrokeWidth");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setStrokeWidth((float)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "BubbleLayout %m.view setStrokeWidth %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setBubbleCornerRadius");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setCornersRadius((float)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "BubbleLayout %m.view setCornerRadius %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setBubbleArrowHeight");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setArrowHeight((float)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "BubbleLayout %m.view setArrowHeight %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setBubbleArrowWidth");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setArrowWidth((float)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "BubbleLayout %m.view setArrowWidth %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setBubbleArrowPosition");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setArrowPosition((float)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "BubbleLayout %m.view setArrowPosition %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "patternToString");
        blockDef.put("type", "s");
        blockDef.put("code", "PatternLockUtils.patternToString(%s, %s)");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.patternview getPattern from %m.listStr to String ");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "patternToMD5");
        blockDef.put("type", "s");
        blockDef.put("code", "PatternLockUtils.patternToMD5(%s, %s)");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.patternview getPattern from %m.listStr to MD5");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "patternToSha1");
        blockDef.put("type", "s");
        blockDef.put("code", "PatternLockUtils.patternToSha1(%s, %s)");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.patternview getPattern from %m.listStr to SHA1");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "patternSetDotCount");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setDotCount((int)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.patternview setDotCount %d ");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "patternSetNormalStateColor");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setNormalStateColor(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.patternview setNormalStateColor %m.color");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "patternSetCorrectStateColor");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setCorrectStateColor(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.patternview setCorrectStateColor %m.color");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "patternSetWrongStateColor");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setWrongStateColor(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.patternview setWrongStateColor %m.color");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "patternSetViewMode");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setViewMode(PatternLockView.PatternViewMode.%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.patternview setViewMode %m.patternviewmode");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "patternLockClear");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.clearPattern();");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.patternview clearPattern");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "tilSetBoxBgColor");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setBoxBackgroundColor(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.textinputlayout setBoxBackgroundColor %m.color");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "tilSetBoxStrokeColor");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setBoxStrokeColor(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.textinputlayout setBoxStrokeColor %m.color");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "tilSetBoxBgMode");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.textinputlayout setBoxBackgroundMode %m.til_box_mode");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "tilSetBoxCornerRadii");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setBoxCornerRadii((float)%s, (float)%s, (float)%s, (float)%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.textinputlayout setBoxCornerRadius TL %d TR %d BL %d BR %d ");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "tilSetError");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setError(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.textinputlayout setError %s ");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "tilSetErrorEnabled");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setErrorEnabled(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.textinputlayout setErrorEnabled %b ");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "tilSetCounterEnabled");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setCounterEnabled(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.textinputlayout setCounterEnabled %b ");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "tilSetCounterMaxLength");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setCounterMaxLength(%s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.textinputlayout setCounterMaxLength %d ");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "tilGetCounterMaxLength");
        blockDef.put("type", "d");
        blockDef.put("code", "%s.getCounterMaxLength()");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.textinputlayout getCounterMaxLength");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "YTPVLifecycle");
        blockDef.put("type", " ");
        blockDef.put("code", "getLifecycle().addObserver(%1$s);");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.youtubeview getLifecycle");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "YTPVSetListener");
        blockDef.put("type", "c");
        blockDef.put(
                "code",
                "%1$s.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {\r\n"
                        + "  @Override\r\n"
                        + "  public void onReady(@NonNull YouTubePlayer youTubePlayer) {\r\n"
                        + "    String videoId = %2$s;\r\n"
                        + "    youTubePlayer.cueVideo(videoId, 0);\r\n"
                        + "    %3$s\r\n"
                        + "  }\r\n"
                        + "});");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.youtubeview addYouTubePlayerListener VideoID %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "launchApp");
        blockDef.put("type", " ");
        blockDef.put("code", "%s = getPackageManager().getLaunchIntentForPackage(%s);");
        blockDef.put("color", "#2CA5E2");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.intent set app package %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "changeStatebarColour");
        blockDef.put("type", " ");
        blockDef.put(
                "code",
                "if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {\r\n"
                        + "final Window window = %s.this.getWindow();\r\n"
                        + "window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);\r\n"
                        + "window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);\r\n"
                        + "window.setStatusBarColor(%s);\r\n"
                        + "}");
        blockDef.put("color", "#2CA5E2");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.activity set statebar color %m.color");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "Dialog SetIcon");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.setIcon(R.drawable.%s);");
        blockDef.put("color", "#2CA5E2");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.dialog setIcon %m.resource_bg");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "ViewPagerNotifyOnDtatChange");
        blockDef.put("type", " ");
        blockDef.put("code", "((PagerAdapter)%s.getAdapter()).notifyDataSetChanged();");
        blockDef.put("color", "#4A6CD4");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.viewpager notifyDataSetChanged");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "returnMap");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "return %s;");
        blockDef.put("color", "#e1a92a");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "return %m.varMap");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "returnListStr");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "return %s;");
        blockDef.put("color", "#e1a92a");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "return %m.listStr");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "returnListMap");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "return %s;");
        blockDef.put("color", "#e1a92a");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "return %m.listMap");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "returnView");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "return %s;");
        blockDef.put("color", "#e1a92a");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "return %m.view");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "repeatKnownNum");
        blockDef.put("type", "c");
        blockDef.put("typeName", "");
        blockDef.put("code", "for (int %2$s = 0; %2$s < (int)(%1$s); %2$s++) {\r\n%3$s\r\n}");
        blockDef.put("color", "#e1a92a");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "repeat %d: %s.inputOnly ++");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "whileLoop");
        blockDef.put("type", "c");
        blockDef.put("typeName", "");
        blockDef.put("code", "while(%s) {\r\n%s\r\n}");
        blockDef.put("color", "#e1a92a");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "while %b");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "tryCatch");
        blockDef.put("type", "e");
        blockDef.put("typeName", "");
        blockDef.put("code", "try {\r\n%s\r\n} catch (Exception e) {\r\n%s\r\n}");
        blockDef.put("color", "#e1a92a");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "try");
        blockDef.put("spec2", "catch");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "getExceptionMessage");
        blockDef.put("type", "s");
        blockDef.put("typeName", "");
        blockDef.put("code", "e.getMessage()");
        blockDef.put("color", "#e1a92a");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "exception message");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "switchStr");
        blockDef.put("type", "c");
        blockDef.put("typeName", "");
        blockDef.put("code", "switch(%s) {\r\n%s\r\n}");
        blockDef.put("color", "#e1a92a");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "switch %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "switchNum");
        blockDef.put("type", "c");
        blockDef.put("typeName", "");
        blockDef.put("code", "switch((int)%s) {\r\n%s\r\n}");
        blockDef.put("color", "#e1a92a");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "switch %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "caseStr");
        blockDef.put("type", "c");
        blockDef.put("typeName", "");
        blockDef.put("code", "case %s: {\r\n%s\r\nbreak;\r\n}");
        blockDef.put("color", "#e1a92a");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "case %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "caseNum");
        blockDef.put("type", "c");
        blockDef.put("typeName", "");
        blockDef.put("code", "case ((int)%s): {\r\n%s\r\nbreak;\r\n}");
        blockDef.put("color", "#e1a92a");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "case %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "defaultSwitch");
        blockDef.put("type", "c");
        blockDef.put("typeName", "");
        blockDef.put("code", "default: {\r\n%s\r\nbreak;\r\n}");
        blockDef.put("color", "#e1a92a");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "default");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "returnString");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "return (%s);");
        blockDef.put("color", "#e1a92a");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "return %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "returnNumber");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "return (%s);");
        blockDef.put("color", "#e1a92a");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "return %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "returnBoolean");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "return (%s);");
        blockDef.put("color", "#e1a92a");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "return %b");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "reverseList");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "Collections.reverse(%s);");
        blockDef.put("color", "#cc5b22");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "reverse %m.list");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "shuffleList");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "Collections.shuffle(%s);");
        blockDef.put("color", "#cc5b22");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "shuffle %m.list");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "sortList");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "Collections.sort(%s);");
        blockDef.put("color", "#cc5b22");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "sort %m.listStr");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "sortListnum");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "Collections.sort(%s);");
        blockDef.put("color", "#cc5b22");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "sort %m.listInt");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "swapInList");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "Collections.swap(%s, (int)(%s), (int)(%s));");
        blockDef.put("color", "#cc5b22");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "swap %m.list position %d with %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "getMapAtPosListmap");
        blockDef.put("type", "a");
        blockDef.put("typeName", "");
        blockDef.put("code", "%2$s.get((int)(%1$s))");
        blockDef.put("color", "#cc5b22");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "get Map at %d of %m.listMap");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setMapAtPosListmap");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "%3$s.set((int)(%2$s), %1$s);");
        blockDef.put("color", "#cc5b22");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "set %m.varMap at %d of %m.listMap");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setAtPosListstr");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "%3$s.set((int)%2$s, %1$s);");
        blockDef.put("color", "#cc5b22");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "set %s at %d of %m.listStr");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "setAtPosListnum");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "%3$s.set((int)%2$s, %1$s);");
        blockDef.put("color", "#cc5b22");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "set %d at %d of %m.listInt");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "GsonListTojsonString");
        blockDef.put("type", "s");
        blockDef.put("typeName", "");
        blockDef.put("code", "new Gson().toJson(%s)");
        blockDef.put("color", "#5cb722");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.list to JSON String");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "GsonStringToListString");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put(
                "code",
                "%2$s = new Gson().fromJson(%1$s, new"
                        + " TypeToken<ArrayList<String>>(){}.getType());");
        blockDef.put("color", "#5cb722");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "JSON %s to %m.listStr");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "GsonStringToListNumber");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put(
                "code",
                "%2$s = new Gson().fromJson(%1$s, new"
                        + " TypeToken<ArrayList<Double>>(){}.getType());");
        blockDef.put("color", "#5cb722");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "JSON %s to %m.listInt");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "hashmapGetNumber");
        blockDef.put("type", "d");
        blockDef.put("code", "(double)%s.get(%s)");
        blockDef.put("palette", "-1");
        blockDef.put("color", "#ee7d15");
        blockDef.put("spec", "%m.varMap get number key %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "hashmapPutNumber");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.put(%s, (int)(%s));");
        blockDef.put("palette", "-1");
        blockDef.put("color", "#ee7d15");
        blockDef.put("spec", "%m.varMap put key %s value int %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "hashmapPutNumber2");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.put(%s, (double)(%s));");
        blockDef.put("palette", "-1");
        blockDef.put("color", "#ee7d15");
        blockDef.put("spec", "%m.varMap put key %s value double %d");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "hashmapGetBoolean");
        blockDef.put("type", "b");
        blockDef.put("code", "(boolean)%s.get(%s)");
        blockDef.put("palette", "-1");
        blockDef.put("color", "#ee7d15");
        blockDef.put("spec", "%m.varMap get boolean key %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "hashmapPutBoolean");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.put(%s, %s);");
        blockDef.put("palette", "-1");
        blockDef.put("color", "#ee7d15");
        blockDef.put("spec", "%m.varMap put key %s value %b");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "hashmapGetMap");
        blockDef.put("type", "a");
        blockDef.put("code", "(HashMap<String,Object>)%s.get(%s)");
        blockDef.put("palette", "-1");
        blockDef.put("color", "#ee7d15");
        blockDef.put("spec", "%m.varMap get Map key %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "hashmapPutMap");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.put(%s, %s);");
        blockDef.put("palette", "-1");
        blockDef.put("color", "#ee7d15");
        blockDef.put("spec", "%m.varMap put key %s value %m.varMap");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "hashmapListstr");
        blockDef.put("type", "l");
        blockDef.put("typeName", "List String");
        blockDef.put("code", "(ArrayList<String>)%s.get(%s)");
        blockDef.put("palette", "-1");
        blockDef.put("color", "#ee7d15");
        blockDef.put("spec", "%m.varMap get List String key %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "hashmapPutListstr");
        blockDef.put("type", " ");
        blockDef.put("code", "%s.put(%s, %s);");
        blockDef.put("palette", "-1");
        blockDef.put("color", "#ee7d15");
        blockDef.put("spec", "%m.varMap put key %s value %m.listStr");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "hashmapGetListmap");
        blockDef.put("type", "l");
        blockDef.put("typeName", "List Map");
        blockDef.put("code", "(ArrayList<HashMap<String,Object>>)%s.get(%s)");
        blockDef.put("palette", "-1");
        blockDef.put("color", "#ee7d15");
        blockDef.put("spec", "%m.varMap get List Map key %s");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "hashmapPutListmap");
        blockDef.put("type", " ");
        blockDef.put("color", "#ee7d15");
        blockDef.put("code", "%s.put(%s, %s);");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.varMap put key %s value %m.listMap");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "addSourceDirectly");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "%s");
        blockDef.put("color", "#5cb722");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "add source directly %s.inputOnly");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "asdBoolean");
        blockDef.put("type", "b");
        blockDef.put("typeName", "");
        blockDef.put("code", "%s");
        blockDef.put("color", "#5cb722");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "boolean %s.inputOnly");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "asdNumber");
        blockDef.put("type", "d");
        blockDef.put("typeName", "");
        blockDef.put("code", "%s");
        blockDef.put("color", "#5cb722");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "number %s.inputOnly");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "asdString");
        blockDef.put("type", "s");
        blockDef.put("typeName", "");
        blockDef.put("code", "%s");
        blockDef.put("color", "#5cb722");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "string %s.inputOnly");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "RepeatKnownNumDescending");
        blockDef.put("type", "c");
        blockDef.put("typeName", "");
        blockDef.put("code", "for (int %2$s = ((int) %1$s - 1); %2$s > -1; %2$s--) {\r\n%3$s\r\n}");
        blockDef.put("color", "#e1a92a");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "repeat %d: %s.inputOnly --");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "interstitialAdLoad");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put(
                "code",
                "{\r\n"
                        + "AdRequest adRequest = new AdRequest.Builder().build();\r\n"
                        + "InterstitialAd.load(%2$s.this, _ad_unit_id, adRequest,"
                        + " _%1$s_interstitial_ad_load_callback);\r\n"
                        + "}");
        blockDef.put("color", "#2aa4e2");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.interstitialad load in %m.activity");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "interstitialAdShow");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put(
                "code",
                "if (%1$s != null) {\r\n"
                        + "%1$s.show(%2$s.this);\r\n"
                        + "} else {\r\n"
                        + "SketchwareUtil.showMessage(getApplicationContext(), \"Error: InterstitialAd"
                        + " %1$s hasn't been loaded yet!\");\r\n"
                        + "}");
        blockDef.put("color", "#2aa4e2");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.interstitialad show ad in %m.activity");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "interstitialAdIsLoaded");
        blockDef.put("type", "b");
        blockDef.put("typeName", "");
        blockDef.put("code", "%1$s != null");
        blockDef.put("color", "#2aa4e2");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "%m.interstitialad is loaded");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "interstitialAdRegisterFullScreenContentCallback");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "");
        blockDef.put("color", "#2aa4e2");
        blockDef.put("palette", "-1");
        blockDef.put(
                "spec",
                "%m.interstitialad register fullscreen content callbacks (This Block isn't needed"
                        + " anymore, please remove it)");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "rewardedAdRegisterFullScreenContentCallback");
        blockDef.put("type", " ");
        blockDef.put("typeName", "");
        blockDef.put("code", "");
        blockDef.put("color", "#2aa4e2");
        blockDef.put("palette", "-1");
        blockDef.put(
                "spec",
                "%m.videoad register fullscreen content callbacks (This Block isn't needed anymore,"
                        + " please remove it)");
        arrayList.add(blockDef);

        blockDef = new HashMap<>();
        blockDef.put("name", "getResString");
        blockDef.put("type", "s");
        blockDef.put("code", "getString(%s)");
        blockDef.put("color", "#7c83db");
        blockDef.put("palette", "-1");
        blockDef.put("spec", "get String from %m.ResString");
        arrayList.add(blockDef);
    }

    private static boolean showAll() {
        return ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_ALWAYS_SHOW_BLOCKS)
                || ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_SHOW_EVERY_SINGLE_BLOCK);
    }

    private static boolean showBuiltIn() {
        return ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_SHOW_BUILT_IN_BLOCKS)
                || ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_SHOW_EVERY_SINGLE_BLOCK);
    }

    public static void primaryBlocksA(
            LogicEditorActivity logicEditorActivity,
            boolean isBoolUsed,
            boolean isIntUsed,
            boolean isStrUsed,
            boolean isMapUsed) {
        logicEditorActivity.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_blocks), getTitleBgColor(logicEditorActivity));
        if (showAll() || isBoolUsed) {
            logicEditorActivity.createPaletteBlock(" ", "setVarBoolean");
        }
        if (showAll() || isIntUsed) {
            logicEditorActivity.createPaletteBlock(" ", "setVarInt");
            logicEditorActivity.createPaletteBlock(" ", "increaseInt");
            logicEditorActivity.createPaletteBlock(" ", "decreaseInt");
        }
        if (showAll() || isStrUsed) {
            logicEditorActivity.createPaletteBlock(" ", "setVarString");
        }
        if (showAll() || isMapUsed) {
            logicEditorActivity.createPaletteBlock(" ", "mapCreateNew");
            logicEditorActivity.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_map_put_values), getTitleBgColor(logicEditorActivity));
            logicEditorActivity.createPaletteBlock(" ", "mapPut");
        }
        if (showBuiltIn() && (showAll() || isMapUsed)) {
            logicEditorActivity.createPaletteBlock(" ", "hashmapPutNumber");
            logicEditorActivity.createPaletteBlock(" ", "hashmapPutNumber2");
            logicEditorActivity.createPaletteBlock(" ", "hashmapPutBoolean");
            logicEditorActivity.createPaletteBlock(" ", "hashmapPutMap");
            logicEditorActivity.createPaletteBlock(" ", "hashmapPutListstr");
            logicEditorActivity.createPaletteBlock(" ", "hashmapPutListmap");
        }
        if (showAll() || isMapUsed) {
            logicEditorActivity.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_map_get_values), getTitleBgColor(logicEditorActivity));
            logicEditorActivity.createPaletteBlock("s", "mapGet");
        }
        if (showBuiltIn() && (showAll() || isMapUsed)) {
            logicEditorActivity.createPaletteBlock("d", "hashmapGetNumber");
            logicEditorActivity.createPaletteBlock("b", "hashmapGetBoolean");
            logicEditorActivity.createPaletteBlock("a", "hashmapGetMap");
            logicEditorActivity.createPaletteBlockWithComponent("", "l", "List String", "hashmapListstr");
            logicEditorActivity.createPaletteBlockWithComponent("", "l", "List Map", "hashmapGetListmap");
        }
        if (showAll() || isMapUsed) {
            logicEditorActivity.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_map_general), getTitleBgColor(logicEditorActivity));
            logicEditorActivity.createPaletteBlock("b", "mapIsEmpty");
            logicEditorActivity.createPaletteBlock("b", "mapContainKey");
            logicEditorActivity.createPaletteBlock("b", "mapContainValue");
            logicEditorActivity.createPaletteBlock("d", "mapSize");
            logicEditorActivity.createPaletteBlock(" ", "mapRemoveKey");
            logicEditorActivity.createPaletteBlock(" ", "mapClear");
            logicEditorActivity.createPaletteBlock(" ", "mapGetAllKeys");
        }
    }

    public static void primaryBlocksB(
            LogicEditorActivity logicEditorActivity,
            boolean isListNumUsed,
            boolean isListStrUsed,
            boolean isListMapUsed) {
        String eventName = logicEditorActivity.eventName;
        boolean inOnBindCustomViewEvent = eventName.equals("onBindCustomView");
        boolean inOnFilesPickedEvent = eventName.equals("onFilesPicked");
        if (showAll() || isListNumUsed) {
            logicEditorActivity.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_list_number), getTitleBgColor(logicEditorActivity));
            logicEditorActivity.createPaletteBlock("b", "containListInt");
            logicEditorActivity.createPaletteBlock("d", "getAtListInt");
            logicEditorActivity.createPaletteBlock("d", "indexListInt");
            logicEditorActivity.createPaletteBlock(" ", "addListInt");
            logicEditorActivity.createPaletteBlock(" ", "insertListInt");
        }
        if (showBuiltIn() && (showAll() || isListNumUsed)) {
            logicEditorActivity.createPaletteBlock(" ", "setAtPosListnum");
        }
        if (showBuiltIn() && (showAll() || isListNumUsed)) {
            logicEditorActivity.createPaletteBlock(" ", "sortListnum");
        }
        if (showAll() || isListStrUsed || inOnFilesPickedEvent) {
            logicEditorActivity.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_list_string), getTitleBgColor(logicEditorActivity));
            logicEditorActivity.createPaletteBlock("b", "containListStr");
            logicEditorActivity.createPaletteBlock("d", "indexListStr");
            logicEditorActivity.createPaletteBlock("s", "getAtListStr");
            logicEditorActivity.createPaletteBlock(" ", "addListStr");
            logicEditorActivity.createPaletteBlock(" ", "insertListStr");
        }
        if (showBuiltIn() && (showAll() || isListStrUsed)) {
            logicEditorActivity.createPaletteBlock(" ", "setAtPosListstr");
        }
        if (showAll() || isListStrUsed) {
            logicEditorActivity.createPaletteBlock(" ", "sortList");
        }
        if (showAll() || isListMapUsed || inOnBindCustomViewEvent) {
            logicEditorActivity.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_list_map), getTitleBgColor(logicEditorActivity));
            logicEditorActivity.createPaletteBlock("b", "containListMap");
            logicEditorActivity.createPaletteBlock("s", "getAtListMap");
            if (showBuiltIn()) {
                logicEditorActivity.createPaletteBlock("a", "getMapAtPosListmap");
            }
            logicEditorActivity.createPaletteBlock(" ", "addListMap");
            logicEditorActivity.createPaletteBlock(" ", "insertListMap");
            logicEditorActivity.createPaletteBlock(" ", "setListMap");
            logicEditorActivity.createPaletteBlock(" ", "setMapAtPosListmap");
        }
        if (showAll() || isListMapUsed) {
            logicEditorActivity.createPaletteBlock(" ", "addMapToList");
            logicEditorActivity.createPaletteBlock(" ", "insertMapToList");
            logicEditorActivity.createPaletteBlock(" ", "getMapInList");
            logicEditorActivity.createPaletteBlock(" ", "deleteMapFromListmap");
            logicEditorActivity.createPaletteBlock(" ", "sortListmap");
        }
        if (showAll()
                || isListMapUsed
                || isListStrUsed
                || isListNumUsed
                || inOnBindCustomViewEvent
                || inOnFilesPickedEvent) {
            logicEditorActivity.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_general), getTitleBgColor(logicEditorActivity));
            logicEditorActivity.createPaletteBlock(" ", "listAddAll");
            logicEditorActivity.createPaletteBlock("d", "lengthList");
            logicEditorActivity.createPaletteBlock(" ", "deleteList");
            logicEditorActivity.createPaletteBlock(" ", "clearList");
            logicEditorActivity.createPaletteBlock(" ", "reverseList");
            if (showBuiltIn()) {
                logicEditorActivity.createPaletteBlock(" ", "shuffleList");
                logicEditorActivity.createPaletteBlock(" ", "swapInList");
            }
        }
    }

    public static void primaryBlocksC(LogicEditorActivity logicEditorActivity) {
        logicEditorActivity.createPaletteBlock("c", "repeat");
        if (showBuiltIn()) {
            logicEditorActivity.createPaletteBlock("c", "repeatKnownNum");
            logicEditorActivity.createPaletteBlock("c", "RepeatKnownNumDescending");
        }
        logicEditorActivity.createPaletteBlock("c", "forever");
        if (showBuiltIn()) {
            logicEditorActivity.createPaletteBlock("c", "whileLoop");
        }
        logicEditorActivity.createPaletteBlock("c", "if");
        logicEditorActivity.createPaletteBlock("e", "ifElse");
        if (showBuiltIn()) {
            logicEditorActivity.createPaletteBlock("b", "instanceOfOperator");
            logicEditorActivity.createPaletteBlock("b", "isEmpty");
            logicEditorActivity.createPaletteBlock("c", "switchStr");
            logicEditorActivity.createPaletteBlock(" ", "caseStrAnd");
            logicEditorActivity.createPaletteBlock("c", "caseStr");
            logicEditorActivity.createPaletteBlock("c", "switchNum");
            logicEditorActivity.createPaletteBlock(" ", "caseNumAnd");
            logicEditorActivity.createPaletteBlock("c", "caseNum");
            logicEditorActivity.createPaletteBlock("c", "defaultSwitch");
            logicEditorActivity.createPaletteBlock("e", "tryCatch");
            logicEditorActivity.createPaletteBlock("s", "getExceptionMessage");
            logicEditorActivity.createPaletteBlock("s", "ternaryString");
            logicEditorActivity.createPaletteBlock("d", "ternaryNumber");
            logicEditorActivity.createPaletteBlock("f", "returnString");
            logicEditorActivity.createPaletteBlock("f", "returnNumber");
            logicEditorActivity.createPaletteBlock("f", "returnBoolean");
            logicEditorActivity.createPaletteBlock("f", "returnMap");
            logicEditorActivity.createPaletteBlock("f", "returnListStr");
            logicEditorActivity.createPaletteBlock("f", "returnListMap");
            logicEditorActivity.createPaletteBlock("f", "returnView");
            logicEditorActivity.createPaletteBlock("f", "break");
            logicEditorActivity.createPaletteBlock("f", "continue");
        }
    }

    public static void primaryBlocksD(LogicEditorActivity logicEditorActivity) {
        logicEditorActivity.createPaletteBlock("b", "true");
        logicEditorActivity.createPaletteBlock("b", "false");
        logicEditorActivity.createPaletteBlock("b", "<");
        logicEditorActivity.createPaletteBlock("b", "=");
        logicEditorActivity.createPaletteBlock("b", ">");
        logicEditorActivity.createPaletteBlock("b", "&&");
        logicEditorActivity.createPaletteBlock("b", "||");
        logicEditorActivity.createPaletteBlock("b", "not");
        logicEditorActivity.createPaletteBlock("d", "+");
        logicEditorActivity.createPaletteBlock("d", "-");
        logicEditorActivity.createPaletteBlock("d", "*");
        logicEditorActivity.createPaletteBlock("d", "/");
        logicEditorActivity.createPaletteBlock("d", "%");
        logicEditorActivity.createPaletteBlock("d", "random");
        logicEditorActivity.createPaletteBlock("d", "stringLength");
        logicEditorActivity.createPaletteBlock("s", "stringJoin");
        logicEditorActivity.createPaletteBlock("d", "stringIndex");
        logicEditorActivity.createPaletteBlock("d", "stringLastIndex");
        logicEditorActivity.createPaletteBlock("s", "stringSub");
        if (showBuiltIn()) {
            logicEditorActivity.createPaletteBlock("s", "stringSubSingle");
        }
        logicEditorActivity.createPaletteBlock("b", "stringEquals");
        logicEditorActivity.createPaletteBlock("b", "stringContains");
        if (showBuiltIn()) {
            logicEditorActivity.createPaletteBlock("b", "stringMatches");
        }
        logicEditorActivity.createPaletteBlock("s", "stringReplace");
        if (showBuiltIn()) {
            logicEditorActivity.createPaletteBlock("s", "stringReplaceFirst");
            logicEditorActivity.createPaletteBlock("s", "stringReplaceAll");
            logicEditorActivity.createPaletteBlock("s", "reverse");
            logicEditorActivity.createPaletteBlock("s", "html");
        }
        logicEditorActivity.createPaletteBlock("s", "trim");
        logicEditorActivity.createPaletteBlock("s", "toUpperCase");
        logicEditorActivity.createPaletteBlock("s", "toLowerCase");
        logicEditorActivity.createPaletteBlock("d", "toNumber");
        logicEditorActivity.createPaletteBlock("d", "strParseInteger");
        logicEditorActivity.createPaletteBlock("d", "toHashCode");
        logicEditorActivity.createPaletteBlock("s", "toString");
        logicEditorActivity.createPaletteBlock("s", "toStringWithDecimal");
        logicEditorActivity.createPaletteBlock("s", "toStringFormat");
        logicEditorActivity.createPaletteBlock(" ", "strToMap");
        logicEditorActivity.createPaletteBlock("s", "mapToStr");
        logicEditorActivity.createPaletteBlock(" ", "strToListMap");
        logicEditorActivity.createPaletteBlock("s", "listMapToStr");
        if (showBuiltIn()) {
            logicEditorActivity.createPaletteBlock(" ", "GsonStringToListString");
            logicEditorActivity.createPaletteBlock(" ", "GsonStringToListNumber");
            logicEditorActivity.createPaletteBlock("s", "GsonListTojsonString");
            logicEditorActivity.createPaletteBlock(" ", "stringSplitToList");
        }
        logicEditorActivity.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_add_source_directly), getTitleBgColor(logicEditorActivity));
        logicEditorActivity.createPaletteBlock(" ", "addSourceDirectly");
        logicEditorActivity.createPaletteBlock("b", "asdBoolean");
        logicEditorActivity.createPaletteBlock("d", "asdNumber");
        logicEditorActivity.createPaletteBlock("s", "asdString");
    }

    private static @ColorInt int getTitleBgColor(LogicEditorActivity logicEditorActivity) {
        return ThemeUtils.getColor(logicEditorActivity, ThemeUtils.isDarkThemeEnabled(logicEditorActivity) ? R.attr.colorSurfaceContainerHigh : R.attr.colorSurfaceInverse);
    }
}
