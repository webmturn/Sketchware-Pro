package pro.sketchware.core.codegen;

import java.util.ArrayList;
import java.util.HashMap;

public final class ExtraBlockDefinitions {

    private ExtraBlockDefinitions() {
    }

    public static void extraBlocks(ArrayList<HashMap<String, Object>> arrayList) {
        arrayList.add(addBlock("caseStrAnd", " ", "", "case %s:", "#e1a92a", "case %s and"));
        arrayList.add(addBlock("caseNumAnd", " ", "", "case ((int)%s):", "#e1a92a", "case %d and"));
        arrayList.add(addBlock("continue", "f", "", "continue;", "#e1a92a", "continue"));
        arrayList.add(addBlock("isEmpty", "b", "", "%s.isEmpty()", "#e1a92a", "%s isEmpty"));
        arrayList.add(addBlock("fileutilcopydir", " ", "", "FileUtil.copyDir(%1$s, %2$s);", "#a1887f", "copy dir path %s to path %s"));
        arrayList.add(addBlock("instanceOfOperator", "b", "", "%1$s instanceof %2$s", "#e1a92a", "%s instanceOf %s"));
        arrayList.add(addBlock("checkViewVisibility", "b", "", "%s.getVisibility() == View.%s", "#4a6cd4", "visibility of %m.view equals %m.visible"));
        arrayList.add(addBlock("intentHasExtra", "b", "", "getIntent().hasExtra(%s)", "#2ca5e2", "Activity hasExtra key %s"));
        arrayList.add(addBlock("intentSetType", " ", "", "%s.setType(%s);", "#2ca5e2", "%m.intent setType %s"));
        arrayList.add(addBlock("intentRemoveExtra", " ", "", "%s.removeExtra(%s);", "#2ca5e2", "%m.intent removeExtra key %s"));
        arrayList.add(addBlock("fileContainsData", "b", "", "%1$s.contains(%2$s)", "#2ca5e2", "%m.file contains %s"));
        arrayList.add(addBlock("viewGetChildAt", "v", "View", "%1$s.getChildAt(%2$s)", "#4a6cd4", "%m.view getChildAt %d"));
        arrayList.add(addBlock("strParseInteger", "d", "", "Integer.parseInt(%s)", "#5cb722", "parse int %s"));
        arrayList.add(addBlock("stringSubSingle", "s", "", "%1$s.substring(%2$s)", "#5cb722", "%s subString %d"));
        arrayList.add(addBlock("webviewGetProgress", "d", "", "%1$s.getProgress()", "#4a6cd4", "%m.webview getProgress"));
        arrayList.add(addBlock("menuItemSetVisible", " ", "", "%1$s.setVisible(%2$s);", "#4a6cd4", "%m.MenuItem setVisible %b"));
        arrayList.add(addBlock("menuItemSetEnabled", " ", "", "%1$s.setEnabled(%2$s);", "#4a6cd4", "%m.MenuItem setEnabled %b"));
        arrayList.add(addBlock("menuFindItem", "v", "MenuItem", "menu.findItem(%s)", "#4a6cd4", "MenuItem findItem %s.inputOnly"));
        arrayList.add(addBlock("listAddAll", " ", "", "%1$s.addAll(%2$s);", "#cc5b22", "%m.listStr addAll from %m.list"));
        arrayList.add(addBlock("fileCanExecute", "b", "", "%s.canExecute()", "#a1887f", "%m.File canExecute"));
        arrayList.add(addBlock("fileCanRead", "b", "", "%s.canRead()", "#a1887f", "%m.File canRead"));
        arrayList.add(addBlock("fileCanWrite", "b", "", "%s.canWrite()", "#a1887f", "%m.File canWrite"));
        arrayList.add(addBlock("fileGetName", "s", "", "%s.getName()", "#a1887f", "%m.File getName"));
        arrayList.add(addBlock("fileGetParent", "s", "", "%s.getParent()", "#a1887f", "%m.File getParent"));
        arrayList.add(addBlock("fileGetPath", "s", "", "%s.getPath()", "#a1887f", "%m.File getPath"));
        arrayList.add(addBlock("fileIsHidden", "b", "", "%s.isHidden()", "#a1887f", "%m.File isHidden"));
        arrayList.add(addBlock("addPermission", " ", "", "", "#493F5A", "Permission Command Block: add %m.Permission"));
        arrayList.add(addBlock("removePermission", " ", "", "", "#493F5A", "Permission Command Block: remove %m.Permission"));
        arrayList.add(addBlock("addCustomVariable", " ", "", "", "#493F5A", "Custom Variable Block: add variable %s"));
        arrayList.add(addBlock("addInitializer", " ", "", "", "#493F5A", "Initializer Block: add initializer %s"));
        arrayList.add(addBlock("otpViewSetFieldCount", " ", "", "%1$s.setFieldCount(%2$s);", "#4a6cd4", "%m.otpview setFieldCount %d"));
        arrayList.add(addBlock("otpViewSetOTPText", " ", "", "%1$s.setOTPText(%2$s);", "#4a6cd4", "%m.otpview setOTPText %s"));
        arrayList.add(addBlock("otpViewGetOTPText", "s", "", "%1$s.getOTPText()", "#4a6cd4", "%m.otpview getOTPText"));
        arrayList.add(addBlock("otpViewSetOTPListener", "c", "", "%1$s.setOTPListener(new OTPListener() {\r\npublic void onOTPEntered(String _otp) {\r\n%2$s\r\n}\r\n});", "#4a6cd4", "%m.otpview onOTPEntered -> _otp"));
        arrayList.add(addBlock("signInButtonSetColorScheme", " ", "", "%1$s.setColorScheme(SignInButton.%2$s);", "#4a6cd4", "%m.signinbutton setColorScheme %m.SignButtonColor"));
        arrayList.add(addBlock("signInButtonSetSize", " ", "", "%1$s.setSize(SignInButton.%2$s);", "#4a6cd4", "%m.signinbutton setSize %m.SignButtonSize"));
        arrayList.add(addBlock("bannerAdViewLoadAd", " ", "", "{\r\nAdRequest adRequest = new AdRequest.Builder().build();\r\n%1$s.loadAd(adRequest);\r\n}", "#4a6cd4", "%m.adview load"));
        arrayList.add(addBlock("rewardedVideoAdLoad", " ", "", "RewardedAd.load(%2$s.this, _reward_ad_unit_id, new AdRequest.Builder().build(), _%1$s_rewarded_ad_load_callback);", "#2ca5e2", "%m.videoad load in %m.activity"));
        arrayList.add(addBlock("rewardedVideoAdShow", " ", "", "%1$s.show(%2$s.this, _%1$s_on_user_earned_reward_listener);", "#2ca5e2", "%m.videoad show in %m.activity"));
    }

    private static HashMap<String, Object> addBlock(String name, String type, String typeName, String code, String color, String spec) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("type", type);
        map.put("typeName", typeName);
        map.put("code", code);
        map.put("color", color);
        map.put("palette", "-1");
        map.put("spec", spec);
        return map;
    }
}
