package pro.sketchware.core;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import javax.crypto.BadPaddingException;
import pro.sketchware.R;
import javax.crypto.IllegalBlockSizeException;
import mod.agus.jcoderz.editor.event.ManageEvent;

public class StringResource {
  public static StringResource instance;
  
  public HashMap<String, String> blockTranslations;
  
  public HashMap<String, String> eventTranslations;
  
  public String translationDir = SketchwarePaths.getLocalizationStringsPath();
  
  public boolean isLoaded;
  
  public final String BLOCK_PREFIX = "block";
  
  public final String ROOT_SPEC_PREFIX = "root_spec";
  
  public StringResource() {
    if (this.blockTranslations == null)
      this.blockTranslations = new HashMap<String, String>(); 
    if (this.eventTranslations == null)
      this.eventTranslations = new HashMap<String, String>(); 
  }
  
  public static StringResource getInstance() {
    if (instance == null) {
      synchronized (StringResource.class) {
        if (instance == null) {
          instance = new StringResource();
        }
      }
    }
    return instance;
  }
  
  public String getTranslatedString(Context context, int resId) {
    return getTranslatedStringFromRes(context.getResources(), resId);
  }
  
  public String getTranslatedStringFormatted(Context context, int resId, Object... formatArgs) {
    return getTranslatedStringFormattedFromRes(context.getResources(), resId, formatArgs);
  }
  
  public String getEventTranslation(Context context, String eventKey) {
    if (this.blockTranslations == null)
      this.blockTranslations = new HashMap<String, String>(); 
    if (this.blockTranslations.isEmpty()) {
      this.isLoaded = false;
      this.blockTranslations = loadTranslationsFromFile(this.translationDir);
    } 
    if (this.eventTranslations == null)
      this.eventTranslations = new HashMap<String, String>(); 
    if (this.eventTranslations.isEmpty())
      loadEventTranslations(context); 
    eventKey = this.eventTranslations.get(eventKey);
    String str = eventKey;
    if (eventKey == null)
      str = ""; 
    return str;
  }
  
  public String getRootSpecTranslation(Context context, String blockType, String eventName) {
    if (this.blockTranslations == null)
      this.blockTranslations = new HashMap<String, String>(); 
    boolean isEmpty = this.blockTranslations.isEmpty();
    byte b = 0;
    if (isEmpty) {
      this.isLoaded = false;
      this.blockTranslations = loadTranslationsFromFile(this.translationDir);
    } 
    if (this.eventTranslations == null)
      this.eventTranslations = new HashMap<String, String>(); 
    if (this.eventTranslations.isEmpty())
      loadEventTranslations(context); 
    switch (eventName.hashCode()) {
      default:
        b = -1;
        break;
      case 2087273080:
        if (eventName.equals("onFilesPicked")) {
          b = 28;
          break;
        } 
      case 1979400473:
        if (eventName.equals("onItemLongClicked")) {
          b = 4;
          break;
        } 
      case 1803231982:
        if (eventName.equals("onMarkerClicked")) {
          b = 40;
          break;
        } 
      case 1757061906:
        if (eventName.equals("onFilesPickedCancel")) {
          b = 29;
          break;
        } 
      case 1710477203:
        if (eventName.equals("onPageStarted")) {
          b = 6;
          break;
        } 
      case 1586033095:
        if (eventName.equals("onStopTrackingTouch")) {
          b = 10;
          break;
        } 
      case 1395209852:
        if (eventName.equals("onDownloadSuccess")) {
          b = 23;
          break;
        } 
      case 1348605570:
        if (eventName.equals("onPictureTakenCancel")) {
          b = 27;
          break;
        } 
      case 1348442836:
        if (eventName.equals("onDownloadProgress")) {
          b = 21;
          break;
        } 
      case 1170737640:
        if (eventName.equals("onPictureTaken")) {
          b = 26;
          break;
        } 
      case 805710389:
        if (eventName.equals("onItemClicked")) {
          b = 3;
          break;
        } 
      case 694589214:
        if (eventName.equals("onSpeechResult")) {
          b = 32;
          break;
        } 
      case 445802034:
        if (eventName.equals("onCancelled")) {
          b = 19;
          break;
        } 
      case 378110312:
        if (eventName.equals("onTextChanged")) {
          b = 5;
          break;
        } 
      case 372583555:
        if (eventName.equals("onChildAdded")) {
          b = 16;
          break;
        } 
      case 264008033:
        if (eventName.equals("onDataSent")) {
          b = 36;
          break;
        } 
      case 249705131:
        if (eventName.equals("onFailure")) {
          b = 25;
          break;
        } 
      case 162093458:
        if (eventName.equals("onBindCustomView")) {
          b = 14;
          break;
        } 
      case 136827711:
        if (eventName.equals("onAnimationCancel")) {
          b = 13;
          break;
        } 
      case 80616227:
        if (eventName.equals("onUploadSuccess")) {
          b = 22;
          break;
        } 
      case -376002870:
        if (eventName.equals("onErrorResponse")) {
          b = 31;
          break;
        } 
      case -484536541:
        if (eventName.equals("onChildRemoved")) {
          b = 18;
          break;
        } 
      case -505277536:
        if (eventName.equals("onPageFinished")) {
          b = 7;
          break;
        } 
      case -507667891:
        if (eventName.equals("onItemSelected")) {
          b = 2;
          break;
        } 
      case -584901992:
        if (eventName.equals("onCheckedChange")) {
          b = 1;
          break;
        } 
      case -609996822:
        if (eventName.equals("onConnected")) {
          b = 34;
          break;
        } 
      case -672992515:
        if (eventName.equals("onAnimationStart")) {
          b = 11;
          break;
        } 
      case -719893013:
        if (eventName.equals("onConnectionError")) {
          b = 37;
          break;
        } 
      case -732782352:
        if (eventName.equals("onConnectionStopped")) {
          b = 38;
          break;
        } 
      case -749253875:
        if (eventName.equals("onUploadProgress")) {
          b = 20;
          break;
        } 
      case -821066400:
        if (eventName.equals("onLocationChanged")) {
          b = 41;
          break;
        } 
      case -837428873:
        if (eventName.equals("onChildChanged")) {
          b = 17;
          break;
        } 
      case -891988931:
        if (eventName.equals("onDateChange")) {
          b = 15;
          break;
        } 
      case -1153785290:
        if (eventName.equals("onAnimationEnd")) {
          b = 12;
          break;
        } 
      case -1215328199:
        if (eventName.equals("onDeleteSuccess")) {
          b = 24;
          break;
        } 
      case -1351902487:
        if (eventName.equals("onClick"))
          break; 
      case -1358405466:
        if (eventName.equals("onMapReady")) {
          b = 39;
          break;
        } 
      case -1779618840:
        if (eventName.equals("onProgressChanged")) {
          b = 8;
          break;
        } 
      case -1809154262:
        if (eventName.equals("onDataReceived")) {
          b = 35;
          break;
        } 
      case -1865337024:
        if (eventName.equals("onResponse")) {
          b = 30;
          break;
        } 
      case -2067423513:
        if (eventName.equals("onSpeechError")) {
          b = 33;
          break;
        } 
      case -2117913147:
        if (eventName.equals("onStartTrackingTouch")) {
          b = 9;
          break;
        } 
    } 
    switch (b) {
      default:
        String result = this.eventTranslations.get(eventName);
        if (result == null)
          result = ManageEvent.getExtraEventSpec(blockType, eventName); 
        return result;
      case 0:
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
      case 6:
      case 7:
      case 8:
      case 9:
      case 10:
      case 11:
      case 12:
      case 13:
      case 14:
      case 15:
      case 16:
      case 17:
      case 18:
      case 19:
      case 20:
      case 21:
      case 22:
      case 23:
      case 24:
      case 25:
      case 26:
      case 27:
      case 28:
      case 29:
      case 30:
      case 31:
      case 32:
      case 33:
      case 34:
      case 35:
      case 36:
      case 37:
      case 38:
      case 39:
      case 40:
      case 41:
        break;
    } 
    String str = this.eventTranslations.get(eventName);
    eventName = str;
    if (str == null)
      eventName = ""; 
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(getTranslatedString(context, R.string.root_spec_common_when));
    stringBuilder.append(" ");
    stringBuilder.append(blockType);
    stringBuilder.append(" ");
    stringBuilder.append(eventName);
    return stringBuilder.toString();
  }
  
  public String getBlockTranslation(Context context, String blockKeyPrefix, ArrayList<String> specParts) {
    int bodyCount = specParts.size() > 1 ? specParts.size() - 1 : 0;
    if (this.blockTranslations == null)
      this.blockTranslations = new HashMap<String, String>();
    if (this.blockTranslations.isEmpty()) {
      this.isLoaded = false;
      this.blockTranslations = loadTranslationsFromFile(this.translationDir);
    }
    StringBuilder result = new StringBuilder(1024);
    String headKey = blockKeyPrefix + "_head";
    String tailKey = blockKeyPrefix + "_tail";
    boolean useTranslation = false;
    if (this.blockTranslations != null && this.blockTranslations.containsKey(headKey) && this.blockTranslations.containsKey(tailKey)) {
      useTranslation = true;
      for (int i = 0; i < bodyCount; i++) {
        String bk = blockKeyPrefix + "_body_" + (i + 1);
        if (!this.blockTranslations.containsKey(bk)) {
          useTranslation = false;
          break;
        }
      }
    }
    String headStr;
    if (useTranslation) {
      headStr = (String) this.blockTranslations.get(headKey);
    } else {
      try {
        headStr = context.getResources().getString(
            context.getResources().getIdentifier(headKey, "string", context.getPackageName()));
      } catch (Exception e) {
        headStr = "";
      }
    }
    if (headStr == null) headStr = "";
    result.append(headStr);
    int bodyIdx = 0;
    if (specParts.size() > 0) {
      if (result.length() > 0) result.append(" ");
      result.append((String) specParts.get(0));
    }
    while (bodyIdx < bodyCount) {
      bodyIdx++;
      String bodyKey = blockKeyPrefix + "_body_" + bodyIdx;
      String bodyStr;
      if (useTranslation) {
        bodyStr = (String) this.blockTranslations.get(bodyKey);
      } else {
        try {
          bodyStr = context.getResources().getString(
              context.getResources().getIdentifier(bodyKey, "string", context.getPackageName()));
        } catch (Exception e) {
          bodyStr = "";
        }
      }
      if (bodyStr == null) bodyStr = "";
      if (bodyStr.length() > 0) result.append(" ");
      result.append(bodyStr);
      if (result.length() > 0) result.append(" ");
      result.append((String) specParts.get(bodyIdx));
    }
    String tailStr;
    if (useTranslation) {
      tailStr = (String) this.blockTranslations.get(tailKey);
    } else {
      try {
        tailStr = context.getResources().getString(
            context.getResources().getIdentifier(tailKey, "string", context.getPackageName()));
      } catch (Exception e) {
        tailStr = "";
      }
    }
    if (tailStr == null) tailStr = "";
    if (result.length() > 0 && tailStr.length() > 0) result.append(" ");
    result.append(tailStr);
    return result.toString();
  }
  
  public String getTranslatedStringFromRes(Resources resources, int resId) {
    String str = resources.getResourceEntryName(resId);
    if (this.blockTranslations == null)
      this.blockTranslations = new HashMap<String, String>(); 
    try {
      if (this.blockTranslations.isEmpty()) {
        this.isLoaded = false;
        this.blockTranslations = loadTranslationsFromFile(this.translationDir);
      } 
      return (this.blockTranslations.containsKey(str) && this.blockTranslations.get(str) != null && ((String)this.blockTranslations.get(str)).length() > 0) ? ((String)this.blockTranslations.get(str)).replaceAll("\\\\\\'", "'").replaceAll("\\\\\\\"", "\"").replaceAll("\\\\n", "\\\n") : resources.getString(resId);
    } catch (Exception exception) {
      return resources.getString(resId);
    } 
  }
  
  public String getTranslatedStringFormattedFromRes(Resources resources, int resId, Object... formatArgs) {
    String str = resources.getResourceEntryName(resId);
    if (this.blockTranslations == null)
      this.blockTranslations = new HashMap<String, String>(); 
    boolean isEmpty = this.blockTranslations.isEmpty();
    byte b = 0;
    if (isEmpty) {
      this.isLoaded = false;
      this.blockTranslations = loadTranslationsFromFile(this.translationDir);
    } 
    try {
      if (this.blockTranslations.containsKey(str) && this.blockTranslations.get(str) != null && ((String)this.blockTranslations.get(str)).length() > 0) {
        String object = ((String)this.blockTranslations.get(str)).replaceAll("\\\\\\'", "'").replaceAll("\\\\\\\"", "\"").replaceAll("\\\\n", "\\\n");
        int i = formatArgs.length;
        int j = 0;
        while (b < i) {
          Object object1 = formatArgs[b];
          int k = j;
          String object2 = object;
          if (object.contains("%")) {
            object1 = object1.toString();
            object2 = (String)object1;
            if (object1.equals("\\n"))
              object2 = "\\\\n"; 
            object2 = object.replaceFirst("%s", object2);
            k = j + 1;
          } 
          b++;
          j = k;
          object = (String)object2;
        } 
        if (j == formatArgs.length) {
          boolean hasPlaceholder = object.contains("%");
          if (!hasPlaceholder)
            return (String)object; 
        } 
      } 
    } catch (Exception exception) {
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append("Faild to load (");
      stringBuilder.append(str);
      stringBuilder.append(")");
      Log.e("ERROR", stringBuilder.toString(), exception);
    } 
    return resources.getString(resId, formatArgs);
  }
  
  public final HashMap<String, String> parseXmlTranslations(String filePath) {
    HashMap<String, String> result = new HashMap<String, String>();
    if (!new java.io.File(filePath).exists()) {
      return result;
    }
    java.io.FileInputStream fis = null;
    java.io.InputStreamReader isr = null;
    try {
      org.xmlpull.v1.XmlPullParser parser = org.xmlpull.v1.XmlPullParserFactory.newInstance().newPullParser();
      fis = new java.io.FileInputStream(filePath);
      isr = new java.io.InputStreamReader(fis, "UTF-8");
      parser.setInput(isr);
      int eventType = parser.getEventType();
      while (eventType != 1) {
        if (eventType == 2 && "string".equals(parser.getName())) {
          result.put(parser.getAttributeValue(null, "name"), parser.nextText());
        }
        eventType = parser.next();
      }
      this.isLoaded = true;
    } catch (Exception ex) {
      this.isLoaded = false;
    } finally {
      try { if (fis != null) fis.close(); } catch (Exception ignored) {}
      try { if (isr != null) isr.close(); } catch (Exception ignored) {}
    }
    return result;
  }
  
  public HashMap<String, String> parseXmlTranslationsFromBytes(byte[] data) {
    HashMap<String, String> result = new HashMap<String, String>();
    java.io.ByteArrayInputStream bis = null;
    java.io.InputStreamReader isr = null;
    try {
      org.xmlpull.v1.XmlPullParser parser = org.xmlpull.v1.XmlPullParserFactory.newInstance().newPullParser();
      bis = new java.io.ByteArrayInputStream(data);
      isr = new java.io.InputStreamReader(bis, "UTF-8");
      parser.setInput(isr);
      int eventType = parser.getEventType();
      while (eventType != 1) {
        if (eventType == 2 && "string".equals(parser.getName())) {
          result.put(parser.getAttributeValue(null, "name"), parser.nextText());
        }
        eventType = parser.next();
      }
      this.isLoaded = true;
    } catch (Exception ex) {
      this.isLoaded = false;
    } finally {
      try { if (bis != null) bis.close(); } catch (Exception ignored) {}
      try { if (isr != null) isr.close(); } catch (Exception ignored) {}
    }
    return result;
  }
  
  public void clearTranslations() {
    HashMap<String, String> hashMap = this.blockTranslations;
    if (hashMap != null) {
      hashMap.clear();
      this.blockTranslations = null;
    } 
    hashMap = this.eventTranslations;
    if (hashMap != null) {
      hashMap.clear();
      this.eventTranslations = null;
    } 
  }
  
  public void loadEventTranslations(Context context) {
    this.eventTranslations = new HashMap<String, String>();
    loadEventSpecTranslation(context, "initializeLogic");
    loadEventSpecTranslation(context, "onBackPressed");
    loadEventSpecTranslation(context, "onPostCreate");
    loadEventSpecTranslation(context, "onStart");
    loadEventSpecTranslation(context, "onStop");
    loadEventSpecTranslation(context, "onDestroy");
    loadEventSpecTranslation(context, "onResume");
    loadEventSpecTranslation(context, "onPause");
    loadEventSpecTranslation(context, "moreBlock");
    loadEventSpecTranslation(context, "onClick");
    loadEventSpecTranslation(context, "onCheckedChange");
    loadEventSpecTranslation(context, "onItemSelected");
    loadEventSpecTranslation(context, "onItemClicked");
    loadEventSpecTranslation(context, "onItemLongClicked");
    loadEventSpecTranslation(context, "onTextChanged");
    loadEventSpecTranslation(context, "onPageStarted");
    loadEventSpecTranslation(context, "onPageFinished");
    loadEventSpecTranslation(context, "onProgressChanged");
    loadEventSpecTranslation(context, "onStartTrackingTouch");
    loadEventSpecTranslation(context, "onStopTrackingTouch");
    loadEventSpecTranslation(context, "onAnimationStart");
    loadEventSpecTranslation(context, "onAnimationEnd");
    loadEventSpecTranslation(context, "onAnimationCancel");
    loadEventSpecTranslation(context, "onBindCustomView");
    loadEventSpecTranslation(context, "onDateChange");
    loadEventSpecTranslation(context, "onChildAdded");
    loadEventSpecTranslation(context, "onChildChanged");
    loadEventSpecTranslation(context, "onChildRemoved");
    loadEventSpecTranslation(context, "onCancelled");
    loadEventSpecTranslation(context, "onSensorChanged");
    loadEventSpecTranslation(context, "onCreateUserComplete");
    loadEventSpecTranslation(context, "onSignInUserComplete");
    loadEventSpecTranslation(context, "onResetPasswordEmailSent");
    loadEventSpecTranslation(context, "onUploadProgress");
    loadEventSpecTranslation(context, "onDownloadProgress");
    loadEventSpecTranslation(context, "onUploadSuccess");
    loadEventSpecTranslation(context, "onDownloadSuccess");
    loadEventSpecTranslation(context, "onDeleteSuccess");
    loadEventSpecTranslation(context, "onFailure");
    loadEventSpecTranslation(context, "onPictureTaken");
    loadEventSpecTranslation(context, "onPictureTakenCancel");
    loadEventSpecTranslation(context, "onFilesPicked");
    loadEventSpecTranslation(context, "onFilesPickedCancel");
    loadEventSpecTranslation(context, "onAdLoaded");
    loadEventSpecTranslation(context, "onAdFailedToLoad");
    loadEventSpecTranslation(context, "onAdOpened");
    loadEventSpecTranslation(context, "onAdClosed");
    loadEventSpecTranslation(context, "onResponse");
    loadEventSpecTranslation(context, "onErrorResponse");
    loadEventSpecTranslation(context, "onSpeechResult");
    loadEventSpecTranslation(context, "onSpeechError");
    loadEventSpecTranslation(context, "onConnected");
    loadEventSpecTranslation(context, "onDataReceived");
    loadEventSpecTranslation(context, "onDataSent");
    loadEventSpecTranslation(context, "onConnectionError");
    loadEventSpecTranslation(context, "onConnectionStopped");
    loadEventSpecTranslation(context, "onMapReady");
    loadEventSpecTranslation(context, "onMarkerClicked");
    loadEventSpecTranslation(context, "onLocationChanged");
    loadBlockTranslation(context, "viewOnClick");
    loadBlockTranslation(context, "setVarBoolean");
    loadBlockTranslation(context, "setVarInt");
    loadBlockTranslation(context, "increaseInt");
    loadBlockTranslation(context, "decreaseInt");
    loadBlockTranslation(context, "setVarString");
    loadBlockTranslation(context, "mapCreateNew");
    loadBlockTranslation(context, "mapPut");
    loadBlockTranslation(context, "mapGet");
    loadBlockTranslation(context, "mapContainKey");
    loadBlockTranslation(context, "mapRemoveKey");
    loadBlockTranslation(context, "mapSize");
    loadBlockTranslation(context, "mapClear");
    loadBlockTranslation(context, "mapIsEmpty");
    loadBlockTranslation(context, "mapGetAllKeys");
    loadBlockTranslation(context, "addListInt");
    loadBlockTranslation(context, "insertListInt");
    loadBlockTranslation(context, "deleteList");
    loadBlockTranslation(context, "getAtListInt");
    loadBlockTranslation(context, "indexListInt");
    loadBlockTranslation(context, "lengthList");
    loadBlockTranslation(context, "containListInt");
    loadBlockTranslation(context, "clearList");
    loadBlockTranslation(context, "addListStr");
    loadBlockTranslation(context, "insertListStr");
    loadBlockTranslation(context, "getAtListStr");
    loadBlockTranslation(context, "indexListStr");
    loadBlockTranslation(context, "containListStr");
    loadBlockTranslation(context, "addListMap");
    loadBlockTranslation(context, "insertListMap");
    loadBlockTranslation(context, "getAtListMap");
    loadBlockTranslation(context, "setListMap");
    loadBlockTranslation(context, "containListMap");
    loadBlockTranslation(context, "addMapToList");
    loadBlockTranslation(context, "insertMapToList");
    loadBlockTranslation(context, "getMapInList");
    loadBlockTranslation(context, "repeat");
    loadBlockTranslation(context, "forever");
    loadBlockTranslation(context, "break");
    loadBlockTranslation(context, "if");
    loadBlockTranslation(context, "ifElse");
    loadBlockTranslation(context, "else");
    loadBlockTranslation(context, "true");
    loadBlockTranslation(context, "false");
    loadBlockTranslation(context, "<");
    loadBlockTranslation(context, "=");
    loadBlockTranslation(context, ">");
    loadBlockTranslation(context, "&&");
    loadBlockTranslation(context, "||");
    loadBlockTranslation(context, "not");
    loadBlockTranslation(context, "+");
    loadBlockTranslation(context, "-");
    loadBlockTranslation(context, "*");
    loadBlockTranslation(context, "/");
    loadBlockTranslation(context, "%");
    loadBlockTranslation(context, "random");
    loadBlockTranslation(context, "stringLength");
    loadBlockTranslation(context, "stringJoin");
    loadBlockTranslation(context, "stringIndex");
    loadBlockTranslation(context, "stringLastIndex");
    loadBlockTranslation(context, "stringSub");
    loadBlockTranslation(context, "stringEquals");
    loadBlockTranslation(context, "stringContains");
    loadBlockTranslation(context, "stringReplace");
    loadBlockTranslation(context, "stringReplaceFirst");
    loadBlockTranslation(context, "stringReplaceAll");
    loadBlockTranslation(context, "toNumber");
    loadBlockTranslation(context, "trim");
    loadBlockTranslation(context, "toUpperCase");
    loadBlockTranslation(context, "toLowerCase");
    loadBlockTranslation(context, "toString");
    loadBlockTranslation(context, "toStringWithDecimal");
    loadBlockTranslation(context, "toStringFormat");
    loadBlockTranslation(context, "addSourceDirectly");
    loadBlockTranslation(context, "mapToStr");
    loadBlockTranslation(context, "strToMap");
    loadBlockTranslation(context, "listMapToStr");
    loadBlockTranslation(context, "strToListMap");
    loadBlockTranslation(context, "mathGetDip");
    loadBlockTranslation(context, "mathGetDisplayWidth");
    loadBlockTranslation(context, "mathGetDisplayHeight");
    loadBlockTranslation(context, "mathPi");
    loadBlockTranslation(context, "mathE");
    loadBlockTranslation(context, "mathPow");
    loadBlockTranslation(context, "mathMin");
    loadBlockTranslation(context, "mathMax");
    loadBlockTranslation(context, "mathSqrt");
    loadBlockTranslation(context, "mathAbs");
    loadBlockTranslation(context, "mathRound");
    loadBlockTranslation(context, "mathCeil");
    loadBlockTranslation(context, "mathFloor");
    loadBlockTranslation(context, "mathSin");
    loadBlockTranslation(context, "mathCos");
    loadBlockTranslation(context, "mathTan");
    loadBlockTranslation(context, "mathAsin");
    loadBlockTranslation(context, "mathAcos");
    loadBlockTranslation(context, "mathAtan");
    loadBlockTranslation(context, "mathExp");
    loadBlockTranslation(context, "mathLog");
    loadBlockTranslation(context, "mathLog10");
    loadBlockTranslation(context, "mathToRadian");
    loadBlockTranslation(context, "mathToDegree");
    loadBlockTranslation(context, "isDrawerOpen");
    loadBlockTranslation(context, "openDrawer");
    loadBlockTranslation(context, "closeDrawer");
    loadBlockTranslation(context, "viewOnClick");
    loadBlockTranslation(context, "setEnable");
    loadBlockTranslation(context, "getEnable");
    loadBlockTranslation(context, "setVisible");
    loadBlockTranslation(context, "setClickable");
    loadBlockTranslation(context, "setText");
    loadBlockTranslation(context, "setTypeface");
    loadBlockTranslation(context, "getText");
    loadBlockTranslation(context, "setBgColor");
    loadBlockTranslation(context, "setBgResource");
    loadBlockTranslation(context, "setTextColor");
    loadBlockTranslation(context, "setHint");
    loadBlockTranslation(context, "setHintTextColor");
    loadBlockTranslation(context, "setImage");
    loadBlockTranslation(context, "setColorFilter");
    loadBlockTranslation(context, "requestFocus");
    loadBlockTranslation(context, "setRotate");
    loadBlockTranslation(context, "getRotate");
    loadBlockTranslation(context, "setAlpha");
    loadBlockTranslation(context, "getAlpha");
    loadBlockTranslation(context, "setTranslationX");
    loadBlockTranslation(context, "getTranslationX");
    loadBlockTranslation(context, "setTranslationY");
    loadBlockTranslation(context, "getTranslationY");
    loadBlockTranslation(context, "setScaleX");
    loadBlockTranslation(context, "getScaleX");
    loadBlockTranslation(context, "setScaleY");
    loadBlockTranslation(context, "getScaleY");
    loadBlockTranslation(context, "getLocationX");
    loadBlockTranslation(context, "getLocationY");
    loadBlockTranslation(context, "setChecked");
    loadBlockTranslation(context, "getChecked");
    loadBlockTranslation(context, "setThumbResource");
    loadBlockTranslation(context, "setTrackResource");
    loadBlockTranslation(context, "listSetData");
    loadBlockTranslation(context, "listSetCustomViewData");
    loadBlockTranslation(context, "listRefresh");
    loadBlockTranslation(context, "listSetItemChecked");
    loadBlockTranslation(context, "listGetCheckedPosition");
    loadBlockTranslation(context, "listGetCheckedPositions");
    loadBlockTranslation(context, "listGetCheckedCount");
    loadBlockTranslation(context, "listSmoothScrollTo");
    loadBlockTranslation(context, "spnSetData");
    loadBlockTranslation(context, "spnRefresh");
    loadBlockTranslation(context, "spnSetSelection");
    loadBlockTranslation(context, "spnGetSelection");
    loadBlockTranslation(context, "webViewLoadUrl");
    loadBlockTranslation(context, "webViewGetUrl");
    loadBlockTranslation(context, "webViewSetCacheMode");
    loadBlockTranslation(context, "webViewCanGoBack");
    loadBlockTranslation(context, "webViewCanGoForward");
    loadBlockTranslation(context, "webViewGoBack");
    loadBlockTranslation(context, "webViewGoForward");
    loadBlockTranslation(context, "webViewClearCache");
    loadBlockTranslation(context, "webViewClearHistory");
    loadBlockTranslation(context, "webViewStopLoading");
    loadBlockTranslation(context, "webViewZoomIn");
    loadBlockTranslation(context, "webViewZoomOut");
    loadBlockTranslation(context, "calendarViewGetDate");
    loadBlockTranslation(context, "calendarViewSetDate");
    loadBlockTranslation(context, "calendarViewSetMinDate");
    loadBlockTranslation(context, "calnedarViewSetMaxDate");
    loadBlockTranslation(context, "adViewLoadAd");
    loadBlockTranslation(context, "mapViewSetMapType");
    loadBlockTranslation(context, "mapViewMoveCamera");
    loadBlockTranslation(context, "mapViewZoomTo");
    loadBlockTranslation(context, "mapViewZoomIn");
    loadBlockTranslation(context, "mapViewZoomOut");
    loadBlockTranslation(context, "mapViewAddMarker");
    loadBlockTranslation(context, "mapViewSetMarkerInfo");
    loadBlockTranslation(context, "mapViewSetMarkerPosition");
    loadBlockTranslation(context, "mapViewSetMarkerColor");
    loadBlockTranslation(context, "mapViewSetMarkerIcon");
    loadBlockTranslation(context, "mapViewSetMarkerVisible");
    loadBlockTranslation(context, "intentSetAction");
    loadBlockTranslation(context, "intentSetData");
    loadBlockTranslation(context, "intentSetScreen");
    loadBlockTranslation(context, "intentPutExtra");
    loadBlockTranslation(context, "intentSetFlags");
    loadBlockTranslation(context, "startActivity");
    loadBlockTranslation(context, "intentGetString");
    loadBlockTranslation(context, "finishActivity");
    loadBlockTranslation(context, "fileGetData");
    loadBlockTranslation(context, "fileSetData");
    loadBlockTranslation(context, "fileRemoveData");
    loadBlockTranslation(context, "calendarGetNow");
    loadBlockTranslation(context, "calendarAdd");
    loadBlockTranslation(context, "calendarSet");
    loadBlockTranslation(context, "calendarFormat");
    loadBlockTranslation(context, "calendarDiff");
    loadBlockTranslation(context, "calendarGetTime");
    loadBlockTranslation(context, "calendarSetTime");
    loadBlockTranslation(context, "vibratorAction");
    loadBlockTranslation(context, "timerAfter");
    loadBlockTranslation(context, "timerEvery");
    loadBlockTranslation(context, "timerCancel");
    loadBlockTranslation(context, "dialogSetTitle");
    loadBlockTranslation(context, "dialogSetMessage");
    loadBlockTranslation(context, "dialogOkButton");
    loadBlockTranslation(context, "dialogCancelButton");
    loadBlockTranslation(context, "dialogNeutralButton");
    loadBlockTranslation(context, "dialogShow");
    loadBlockTranslation(context, "dialogDismiss");
    loadBlockTranslation(context, "mediaplayerCreate");
    loadBlockTranslation(context, "mediaplayerStart");
    loadBlockTranslation(context, "mediaplayerPause");
    loadBlockTranslation(context, "mediaplayerSeek");
    loadBlockTranslation(context, "mediaplayerGetCurrent");
    loadBlockTranslation(context, "mediaplayerGetDuration");
    loadBlockTranslation(context, "mediaplayerIsPlaying");
    loadBlockTranslation(context, "mediaplayerSetLooping");
    loadBlockTranslation(context, "mediaplayerIsLooping");
    loadBlockTranslation(context, "mediaplayerReset");
    loadBlockTranslation(context, "mediaplayerRelease");
    loadBlockTranslation(context, "soundpoolCreate");
    loadBlockTranslation(context, "soundpoolLoad");
    loadBlockTranslation(context, "soundpoolStreamPlay");
    loadBlockTranslation(context, "soundpoolStreamStop");
    loadBlockTranslation(context, "objectanimatorSetTarget");
    loadBlockTranslation(context, "objectanimatorSetProperty");
    loadBlockTranslation(context, "objectanimatorSetValue");
    loadBlockTranslation(context, "objectanimatorSetFromTo");
    loadBlockTranslation(context, "objectanimatorSetDuration");
    loadBlockTranslation(context, "objectanimatorSetRepeatMode");
    loadBlockTranslation(context, "objectanimatorSetRepeatCount");
    loadBlockTranslation(context, "objectanimatorSetInterpolator");
    loadBlockTranslation(context, "objectanimatorStart");
    loadBlockTranslation(context, "objectanimatorCancel");
    loadBlockTranslation(context, "objectanimatorIsRunning");
    loadBlockTranslation(context, "firebaseAdd");
    loadBlockTranslation(context, "firebasePush");
    loadBlockTranslation(context, "firebaseGetPushKey");
    loadBlockTranslation(context, "firebaseDelete");
    loadBlockTranslation(context, "firebaseGetChildren");
    loadBlockTranslation(context, "firebaseauthCreateUser");
    loadBlockTranslation(context, "firebaseauthSignInUser");
    loadBlockTranslation(context, "firebaseauthSignInAnonymously");
    loadBlockTranslation(context, "firebaseauthIsLoggedIn");
    loadBlockTranslation(context, "firebaseauthGetCurrentUser");
    loadBlockTranslation(context, "firebaseauthGetUid");
    loadBlockTranslation(context, "firebaseauthResetPassword");
    loadBlockTranslation(context, "firebaseauthSignOutUser");
    loadBlockTranslation(context, "firebaseStartListen");
    loadBlockTranslation(context, "firebaseStopListen");
    loadBlockTranslation(context, "gyroscopeStartListen");
    loadBlockTranslation(context, "gyroscopeStopListen");
    loadBlockTranslation(context, "interstitialadCreate");
    loadBlockTranslation(context, "interstitialadLoadAd");
    loadBlockTranslation(context, "interstitialadShow");
    loadBlockTranslation(context, "firebasestorageUploadFile");
    loadBlockTranslation(context, "firebasestorageDownloadFile");
    loadBlockTranslation(context, "firebasestorageDelete");
    loadBlockTranslation(context, "camerastarttakepicture");
    loadBlockTranslation(context, "filepickerstartpickfiles");
    loadBlockTranslation(context, "requestnetworkSetParams");
    loadBlockTranslation(context, "requestnetworkSetHeaders");
    loadBlockTranslation(context, "requestnetworkStartRequestNetwork");
    loadBlockTranslation(context, "fileutildelete");
    loadBlockTranslation(context, "fileutilcopy");
    loadBlockTranslation(context, "fileutilwrite");
    loadBlockTranslation(context, "fileutilread");
    loadBlockTranslation(context, "fileutilmove");
    loadBlockTranslation(context, "fileutilisexist");
    loadBlockTranslation(context, "fileutilmakedir");
    loadBlockTranslation(context, "fileutillistdir");
    loadBlockTranslation(context, "fileutilisdir");
    loadBlockTranslation(context, "fileutilisfile");
    loadBlockTranslation(context, "fileutillength");
    loadBlockTranslation(context, "fileutilStartsWith");
    loadBlockTranslation(context, "fileutilEndsWith");
    loadBlockTranslation(context, "fileutilGetLastSegmentPath");
    loadBlockTranslation(context, "doToast");
    loadBlockTranslation(context, "copyToClipboard");
    loadBlockTranslation(context, "setTitle");
    loadBlockTranslation(context, "seekBarGetMax");
    loadBlockTranslation(context, "seekBarGetProgress");
    loadBlockTranslation(context, "seekBarSetMax");
    loadBlockTranslation(context, "seekBarSetProgress");
    loadBlockTranslation(context, "getExternalStorageDir");
    loadBlockTranslation(context, "getPackageDataDir");
    loadBlockTranslation(context, "getPublicDir");
    loadBlockTranslation(context, "resizeBitmapFileRetainRatio");
    loadBlockTranslation(context, "resizeBitmapFileToSquare");
    loadBlockTranslation(context, "resizeBitmapFileToCircle");
    loadBlockTranslation(context, "resizeBitmapFileWithRoundedBorder");
    loadBlockTranslation(context, "cropBitmapFileFromCenter");
    loadBlockTranslation(context, "rotateBitmapFile");
    loadBlockTranslation(context, "scaleBitmapFile");
    loadBlockTranslation(context, "skewBitmapFile");
    loadBlockTranslation(context, "setBitmapFileColorFilter");
    loadBlockTranslation(context, "setBitmapFileBrightness");
    loadBlockTranslation(context, "setBitmapFileContrast");
    loadBlockTranslation(context, "setImageFilePath");
    loadBlockTranslation(context, "setImageUrl");
    loadBlockTranslation(context, "getJpegRotate");
    loadBlockTranslation(context, "progressBarSetIndeterminate");
    loadBlockTranslation(context, "textToSpeechSetPitch");
    loadBlockTranslation(context, "textToSpeechSetSpeechRate");
    loadBlockTranslation(context, "textToSpeechSpeak");
    loadBlockTranslation(context, "textToSpeechIsSpeaking");
    loadBlockTranslation(context, "textToSpeechStop");
    loadBlockTranslation(context, "textToSpeechShutdown");
    loadBlockTranslation(context, "speechToTextStartListening");
    loadBlockTranslation(context, "speechToTextStopListening");
    loadBlockTranslation(context, "speechToTextShutdown");
    loadBlockTranslation(context, "bluetoothConnectReadyConnection");
    loadBlockTranslation(context, "bluetoothConnectReadyConnectionToUuid");
    loadBlockTranslation(context, "bluetoothConnectStartConnection");
    loadBlockTranslation(context, "bluetoothConnectStartConnectionToUuid");
    loadBlockTranslation(context, "bluetoothConnectStopConnection");
    loadBlockTranslation(context, "bluetoothConnectSendData");
    loadBlockTranslation(context, "bluetoothConnectIsBluetoothEnabled");
    loadBlockTranslation(context, "bluetoothConnectIsBluetoothActivated");
    loadBlockTranslation(context, "bluetoothConnectActivateBluetooth");
    loadBlockTranslation(context, "bluetoothConnectGetPairedDevices");
    loadBlockTranslation(context, "bluetoothConnectGetRandomUuid");
    loadBlockTranslation(context, "locationManagerRequestLocationUpdates");
    loadBlockTranslation(context, "locationManagerRemoveUpdates");
  }
  
  public HashMap<String, String> loadTranslationsFromFile(String filePath) {
    HashMap<String, String> hashMap;
    HashMap<Object, Object> emptyMap = new HashMap<Object, Object>();
    if (!(new File(filePath)).exists())
      return (HashMap)emptyMap; 
    try {
      hashMap = parseXmlTranslations(filePath);
    } catch (Exception exception) {
      hashMap = (HashMap)emptyMap;
    } 
    return hashMap;
  }
  
  public void loadBlockTranslation(Context context, String blockName) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("block_");
    stringBuilder.append(BlockSpecRegistry.getBlockSpec(blockName));
    String str = stringBuilder.toString();
    ArrayList<String> arrayList = BlockSpecRegistry.getBlockParams(blockName);
    this.eventTranslations.put(blockName, getBlockTranslation(context, str, arrayList));
  }
  
  public boolean reloadTranslations(Context context) {
    boolean loadedSuccessfully;
    if (this.blockTranslations == null)
      this.blockTranslations = new HashMap<String, String>(); 
    HashMap<String, String> hashMap = this.blockTranslations;
    if (hashMap != null)
      hashMap.clear(); 
    this.blockTranslations = loadTranslationsFromFile(this.translationDir);
    loadEventTranslations(context);
    if (!this.blockTranslations.isEmpty() && this.isLoaded) {
      loadedSuccessfully = true;
    } else {
      loadedSuccessfully = false;
    } 
    return loadedSuccessfully;
  }
  
  public void loadEventSpecTranslation(Context context, String eventName) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("root_spec_");
    stringBuilder.append(BlockSpecRegistry.getEventSpec(eventName));
    String str = stringBuilder.toString();
    ArrayList<String> arrayList = BlockSpecRegistry.getBlockMenuItems(eventName);
    this.eventTranslations.put(eventName, getBlockTranslation(context, str, arrayList));
  }
}
