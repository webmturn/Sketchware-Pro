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
  
  public String getTranslatedString(Context paramContext, int paramInt) {
    return getTranslatedStringFromRes(paramContext.getResources(), paramInt);
  }
  
  public String getTranslatedStringFormatted(Context paramContext, int paramInt, Object... paramVarArgs) {
    return getTranslatedStringFormattedFromRes(paramContext.getResources(), paramInt, paramVarArgs);
  }
  
  public String getEventTranslation(Context paramContext, String paramString) {
    if (this.blockTranslations == null)
      this.blockTranslations = new HashMap<String, String>(); 
    if (this.blockTranslations.isEmpty()) {
      this.isLoaded = false;
      this.blockTranslations = loadTranslationsFromFile(this.translationDir);
    } 
    if (this.eventTranslations == null)
      this.eventTranslations = new HashMap<String, String>(); 
    if (this.eventTranslations.isEmpty())
      loadEventTranslations(paramContext); 
    paramString = this.eventTranslations.get(paramString);
    String str = paramString;
    if (paramString == null)
      str = ""; 
    return str;
  }
  
  public String getRootSpecTranslation(Context paramContext, String paramString1, String paramString2) {
    if (this.blockTranslations == null)
      this.blockTranslations = new HashMap<String, String>(); 
    boolean bool = this.blockTranslations.isEmpty();
    byte b = 0;
    if (bool) {
      this.isLoaded = false;
      this.blockTranslations = loadTranslationsFromFile(this.translationDir);
    } 
    if (this.eventTranslations == null)
      this.eventTranslations = new HashMap<String, String>(); 
    if (this.eventTranslations.isEmpty())
      loadEventTranslations(paramContext); 
    switch (paramString2.hashCode()) {
      default:
        b = -1;
        break;
      case 2087273080:
        if (paramString2.equals("onFilesPicked")) {
          b = 28;
          break;
        } 
      case 1979400473:
        if (paramString2.equals("onItemLongClicked")) {
          b = 4;
          break;
        } 
      case 1803231982:
        if (paramString2.equals("onMarkerClicked")) {
          b = 40;
          break;
        } 
      case 1757061906:
        if (paramString2.equals("onFilesPickedCancel")) {
          b = 29;
          break;
        } 
      case 1710477203:
        if (paramString2.equals("onPageStarted")) {
          b = 6;
          break;
        } 
      case 1586033095:
        if (paramString2.equals("onStopTrackingTouch")) {
          b = 10;
          break;
        } 
      case 1395209852:
        if (paramString2.equals("onDownloadSuccess")) {
          b = 23;
          break;
        } 
      case 1348605570:
        if (paramString2.equals("onPictureTakenCancel")) {
          b = 27;
          break;
        } 
      case 1348442836:
        if (paramString2.equals("onDownloadProgress")) {
          b = 21;
          break;
        } 
      case 1170737640:
        if (paramString2.equals("onPictureTaken")) {
          b = 26;
          break;
        } 
      case 805710389:
        if (paramString2.equals("onItemClicked")) {
          b = 3;
          break;
        } 
      case 694589214:
        if (paramString2.equals("onSpeechResult")) {
          b = 32;
          break;
        } 
      case 445802034:
        if (paramString2.equals("onCancelled")) {
          b = 19;
          break;
        } 
      case 378110312:
        if (paramString2.equals("onTextChanged")) {
          b = 5;
          break;
        } 
      case 372583555:
        if (paramString2.equals("onChildAdded")) {
          b = 16;
          break;
        } 
      case 264008033:
        if (paramString2.equals("onDataSent")) {
          b = 36;
          break;
        } 
      case 249705131:
        if (paramString2.equals("onFailure")) {
          b = 25;
          break;
        } 
      case 162093458:
        if (paramString2.equals("onBindCustomView")) {
          b = 14;
          break;
        } 
      case 136827711:
        if (paramString2.equals("onAnimationCancel")) {
          b = 13;
          break;
        } 
      case 80616227:
        if (paramString2.equals("onUploadSuccess")) {
          b = 22;
          break;
        } 
      case -376002870:
        if (paramString2.equals("onErrorResponse")) {
          b = 31;
          break;
        } 
      case -484536541:
        if (paramString2.equals("onChildRemoved")) {
          b = 18;
          break;
        } 
      case -505277536:
        if (paramString2.equals("onPageFinished")) {
          b = 7;
          break;
        } 
      case -507667891:
        if (paramString2.equals("onItemSelected")) {
          b = 2;
          break;
        } 
      case -584901992:
        if (paramString2.equals("onCheckedChange")) {
          b = 1;
          break;
        } 
      case -609996822:
        if (paramString2.equals("onConnected")) {
          b = 34;
          break;
        } 
      case -672992515:
        if (paramString2.equals("onAnimationStart")) {
          b = 11;
          break;
        } 
      case -719893013:
        if (paramString2.equals("onConnectionError")) {
          b = 37;
          break;
        } 
      case -732782352:
        if (paramString2.equals("onConnectionStopped")) {
          b = 38;
          break;
        } 
      case -749253875:
        if (paramString2.equals("onUploadProgress")) {
          b = 20;
          break;
        } 
      case -821066400:
        if (paramString2.equals("onLocationChanged")) {
          b = 41;
          break;
        } 
      case -837428873:
        if (paramString2.equals("onChildChanged")) {
          b = 17;
          break;
        } 
      case -891988931:
        if (paramString2.equals("onDateChange")) {
          b = 15;
          break;
        } 
      case -1153785290:
        if (paramString2.equals("onAnimationEnd")) {
          b = 12;
          break;
        } 
      case -1215328199:
        if (paramString2.equals("onDeleteSuccess")) {
          b = 24;
          break;
        } 
      case -1351902487:
        if (paramString2.equals("onClick"))
          break; 
      case -1358405466:
        if (paramString2.equals("onMapReady")) {
          b = 39;
          break;
        } 
      case -1779618840:
        if (paramString2.equals("onProgressChanged")) {
          b = 8;
          break;
        } 
      case -1809154262:
        if (paramString2.equals("onDataReceived")) {
          b = 35;
          break;
        } 
      case -1865337024:
        if (paramString2.equals("onResponse")) {
          b = 30;
          break;
        } 
      case -2067423513:
        if (paramString2.equals("onSpeechError")) {
          b = 33;
          break;
        } 
      case -2117913147:
        if (paramString2.equals("onStartTrackingTouch")) {
          b = 9;
          break;
        } 
    } 
    switch (b) {
      default:
        String result = this.eventTranslations.get(paramString2);
        if (result == null)
          result = ManageEvent.i(paramString1, paramString2); 
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
    String str = this.eventTranslations.get(paramString2);
    paramString2 = str;
    if (str == null)
      paramString2 = ""; 
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(getTranslatedString(paramContext, R.string.root_spec_common_when));
    stringBuilder.append(" ");
    stringBuilder.append(paramString1);
    stringBuilder.append(" ");
    stringBuilder.append(paramString2);
    return stringBuilder.toString();
  }
  
  public String getBlockTranslation(Context paramContext, String paramString, ArrayList<String> paramArrayList) {
    int bodyCount = paramArrayList.size() > 1 ? paramArrayList.size() - 1 : 0;
    if (this.blockTranslations == null)
      this.blockTranslations = new HashMap<String, String>();
    if (this.blockTranslations.isEmpty()) {
      this.isLoaded = false;
      this.blockTranslations = loadTranslationsFromFile(this.translationDir);
    }
    StringBuilder result = new StringBuilder(1024);
    String headKey = paramString + "_head";
    String tailKey = paramString + "_tail";
    boolean useTranslation = false;
    if (this.blockTranslations != null && this.blockTranslations.containsKey(headKey) && this.blockTranslations.containsKey(tailKey)) {
      useTranslation = true;
      for (int i = 0; i < bodyCount; i++) {
        String bk = paramString + "_body_" + (i + 1);
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
        headStr = paramContext.getResources().getString(
            paramContext.getResources().getIdentifier(headKey, "string", paramContext.getPackageName()));
      } catch (Exception e) {
        headStr = "";
      }
    }
    if (headStr == null) headStr = "";
    result.append(headStr);
    int bodyIdx = 0;
    if (paramArrayList.size() > 0) {
      if (result.length() > 0) result.append(" ");
      result.append((String) paramArrayList.get(0));
    }
    while (bodyIdx < bodyCount) {
      bodyIdx++;
      String bodyKey = paramString + "_body_" + bodyIdx;
      String bodyStr;
      if (useTranslation) {
        bodyStr = (String) this.blockTranslations.get(bodyKey);
      } else {
        try {
          bodyStr = paramContext.getResources().getString(
              paramContext.getResources().getIdentifier(bodyKey, "string", paramContext.getPackageName()));
        } catch (Exception e) {
          bodyStr = "";
        }
      }
      if (bodyStr == null) bodyStr = "";
      if (bodyStr.length() > 0) result.append(" ");
      result.append(bodyStr);
      if (result.length() > 0) result.append(" ");
      result.append((String) paramArrayList.get(bodyIdx));
    }
    String tailStr;
    if (useTranslation) {
      tailStr = (String) this.blockTranslations.get(tailKey);
    } else {
      try {
        tailStr = paramContext.getResources().getString(
            paramContext.getResources().getIdentifier(tailKey, "string", paramContext.getPackageName()));
      } catch (Exception e) {
        tailStr = "";
      }
    }
    if (tailStr == null) tailStr = "";
    if (result.length() > 0 && tailStr.length() > 0) result.append(" ");
    result.append(tailStr);
    return result.toString();
  }
  
  public String getTranslatedStringFromRes(Resources paramResources, int paramInt) {
    String str = paramResources.getResourceEntryName(paramInt);
    if (this.blockTranslations == null)
      this.blockTranslations = new HashMap<String, String>(); 
    try {
      if (this.blockTranslations.isEmpty()) {
        this.isLoaded = false;
        this.blockTranslations = loadTranslationsFromFile(this.translationDir);
      } 
      return (this.blockTranslations.containsKey(str) && this.blockTranslations.get(str) != null && ((String)this.blockTranslations.get(str)).length() > 0) ? ((String)this.blockTranslations.get(str)).replaceAll("\\\\\\'", "'").replaceAll("\\\\\\\"", "\"").replaceAll("\\\\n", "\\\n") : paramResources.getString(paramInt);
    } catch (Exception exception) {
      return paramResources.getString(paramInt);
    } 
  }
  
  public String getTranslatedStringFormattedFromRes(Resources paramResources, int paramInt, Object... paramVarArgs) {
    String str = paramResources.getResourceEntryName(paramInt);
    if (this.blockTranslations == null)
      this.blockTranslations = new HashMap<String, String>(); 
    boolean bool = this.blockTranslations.isEmpty();
    byte b = 0;
    if (bool) {
      this.isLoaded = false;
      this.blockTranslations = loadTranslationsFromFile(this.translationDir);
    } 
    try {
      if (this.blockTranslations.containsKey(str) && this.blockTranslations.get(str) != null && ((String)this.blockTranslations.get(str)).length() > 0) {
        String object = ((String)this.blockTranslations.get(str)).replaceAll("\\\\\\'", "'").replaceAll("\\\\\\\"", "\"").replaceAll("\\\\n", "\\\n");
        int i = paramVarArgs.length;
        int j = 0;
        while (b < i) {
          Object object1 = paramVarArgs[b];
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
        if (j == paramVarArgs.length) {
          bool = object.contains("%");
          if (!bool)
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
    return paramResources.getString(paramInt, paramVarArgs);
  }
  
  public final HashMap<String, String> parseXmlTranslations(String paramString) {
    HashMap<String, String> result = new HashMap<String, String>();
    if (!new java.io.File(paramString).exists()) {
      return result;
    }
    java.io.FileInputStream fis = null;
    java.io.InputStreamReader isr = null;
    try {
      org.xmlpull.v1.XmlPullParser parser = org.xmlpull.v1.XmlPullParserFactory.newInstance().newPullParser();
      fis = new java.io.FileInputStream(paramString);
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
  
  public HashMap<String, String> parseXmlTranslationsFromBytes(byte[] paramArrayOfbyte) {
    HashMap<String, String> result = new HashMap<String, String>();
    java.io.ByteArrayInputStream bis = null;
    java.io.InputStreamReader isr = null;
    try {
      org.xmlpull.v1.XmlPullParser parser = org.xmlpull.v1.XmlPullParserFactory.newInstance().newPullParser();
      bis = new java.io.ByteArrayInputStream(paramArrayOfbyte);
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
  
  public void loadEventTranslations(Context paramContext) {
    this.eventTranslations = new HashMap<String, String>();
    loadEventSpecTranslation(paramContext, "initializeLogic");
    loadEventSpecTranslation(paramContext, "onBackPressed");
    loadEventSpecTranslation(paramContext, "onPostCreate");
    loadEventSpecTranslation(paramContext, "onStart");
    loadEventSpecTranslation(paramContext, "onStop");
    loadEventSpecTranslation(paramContext, "onDestroy");
    loadEventSpecTranslation(paramContext, "onResume");
    loadEventSpecTranslation(paramContext, "onPause");
    loadEventSpecTranslation(paramContext, "moreBlock");
    loadEventSpecTranslation(paramContext, "onClick");
    loadEventSpecTranslation(paramContext, "onCheckedChange");
    loadEventSpecTranslation(paramContext, "onItemSelected");
    loadEventSpecTranslation(paramContext, "onItemClicked");
    loadEventSpecTranslation(paramContext, "onItemLongClicked");
    loadEventSpecTranslation(paramContext, "onTextChanged");
    loadEventSpecTranslation(paramContext, "onPageStarted");
    loadEventSpecTranslation(paramContext, "onPageFinished");
    loadEventSpecTranslation(paramContext, "onProgressChanged");
    loadEventSpecTranslation(paramContext, "onStartTrackingTouch");
    loadEventSpecTranslation(paramContext, "onStopTrackingTouch");
    loadEventSpecTranslation(paramContext, "onAnimationStart");
    loadEventSpecTranslation(paramContext, "onAnimationEnd");
    loadEventSpecTranslation(paramContext, "onAnimationCancel");
    loadEventSpecTranslation(paramContext, "onBindCustomView");
    loadEventSpecTranslation(paramContext, "onDateChange");
    loadEventSpecTranslation(paramContext, "onChildAdded");
    loadEventSpecTranslation(paramContext, "onChildChanged");
    loadEventSpecTranslation(paramContext, "onChildRemoved");
    loadEventSpecTranslation(paramContext, "onCancelled");
    loadEventSpecTranslation(paramContext, "onSensorChanged");
    loadEventSpecTranslation(paramContext, "onCreateUserComplete");
    loadEventSpecTranslation(paramContext, "onSignInUserComplete");
    loadEventSpecTranslation(paramContext, "onResetPasswordEmailSent");
    loadEventSpecTranslation(paramContext, "onUploadProgress");
    loadEventSpecTranslation(paramContext, "onDownloadProgress");
    loadEventSpecTranslation(paramContext, "onUploadSuccess");
    loadEventSpecTranslation(paramContext, "onDownloadSuccess");
    loadEventSpecTranslation(paramContext, "onDeleteSuccess");
    loadEventSpecTranslation(paramContext, "onFailure");
    loadEventSpecTranslation(paramContext, "onPictureTaken");
    loadEventSpecTranslation(paramContext, "onPictureTakenCancel");
    loadEventSpecTranslation(paramContext, "onFilesPicked");
    loadEventSpecTranslation(paramContext, "onFilesPickedCancel");
    loadEventSpecTranslation(paramContext, "onAdLoaded");
    loadEventSpecTranslation(paramContext, "onAdFailedToLoad");
    loadEventSpecTranslation(paramContext, "onAdOpened");
    loadEventSpecTranslation(paramContext, "onAdClosed");
    loadEventSpecTranslation(paramContext, "onResponse");
    loadEventSpecTranslation(paramContext, "onErrorResponse");
    loadEventSpecTranslation(paramContext, "onSpeechResult");
    loadEventSpecTranslation(paramContext, "onSpeechError");
    loadEventSpecTranslation(paramContext, "onConnected");
    loadEventSpecTranslation(paramContext, "onDataReceived");
    loadEventSpecTranslation(paramContext, "onDataSent");
    loadEventSpecTranslation(paramContext, "onConnectionError");
    loadEventSpecTranslation(paramContext, "onConnectionStopped");
    loadEventSpecTranslation(paramContext, "onMapReady");
    loadEventSpecTranslation(paramContext, "onMarkerClicked");
    loadEventSpecTranslation(paramContext, "onLocationChanged");
    loadBlockTranslation(paramContext, "viewOnClick");
    loadBlockTranslation(paramContext, "setVarBoolean");
    loadBlockTranslation(paramContext, "setVarInt");
    loadBlockTranslation(paramContext, "increaseInt");
    loadBlockTranslation(paramContext, "decreaseInt");
    loadBlockTranslation(paramContext, "setVarString");
    loadBlockTranslation(paramContext, "mapCreateNew");
    loadBlockTranslation(paramContext, "mapPut");
    loadBlockTranslation(paramContext, "mapGet");
    loadBlockTranslation(paramContext, "mapContainKey");
    loadBlockTranslation(paramContext, "mapRemoveKey");
    loadBlockTranslation(paramContext, "mapSize");
    loadBlockTranslation(paramContext, "mapClear");
    loadBlockTranslation(paramContext, "mapIsEmpty");
    loadBlockTranslation(paramContext, "mapGetAllKeys");
    loadBlockTranslation(paramContext, "addListInt");
    loadBlockTranslation(paramContext, "insertListInt");
    loadBlockTranslation(paramContext, "deleteList");
    loadBlockTranslation(paramContext, "getAtListInt");
    loadBlockTranslation(paramContext, "indexListInt");
    loadBlockTranslation(paramContext, "lengthList");
    loadBlockTranslation(paramContext, "containListInt");
    loadBlockTranslation(paramContext, "clearList");
    loadBlockTranslation(paramContext, "addListStr");
    loadBlockTranslation(paramContext, "insertListStr");
    loadBlockTranslation(paramContext, "getAtListStr");
    loadBlockTranslation(paramContext, "indexListStr");
    loadBlockTranslation(paramContext, "containListStr");
    loadBlockTranslation(paramContext, "addListMap");
    loadBlockTranslation(paramContext, "insertListMap");
    loadBlockTranslation(paramContext, "getAtListMap");
    loadBlockTranslation(paramContext, "setListMap");
    loadBlockTranslation(paramContext, "containListMap");
    loadBlockTranslation(paramContext, "addMapToList");
    loadBlockTranslation(paramContext, "insertMapToList");
    loadBlockTranslation(paramContext, "getMapInList");
    loadBlockTranslation(paramContext, "repeat");
    loadBlockTranslation(paramContext, "forever");
    loadBlockTranslation(paramContext, "break");
    loadBlockTranslation(paramContext, "if");
    loadBlockTranslation(paramContext, "ifElse");
    loadBlockTranslation(paramContext, "else");
    loadBlockTranslation(paramContext, "true");
    loadBlockTranslation(paramContext, "false");
    loadBlockTranslation(paramContext, "<");
    loadBlockTranslation(paramContext, "=");
    loadBlockTranslation(paramContext, ">");
    loadBlockTranslation(paramContext, "&&");
    loadBlockTranslation(paramContext, "||");
    loadBlockTranslation(paramContext, "not");
    loadBlockTranslation(paramContext, "+");
    loadBlockTranslation(paramContext, "-");
    loadBlockTranslation(paramContext, "*");
    loadBlockTranslation(paramContext, "/");
    loadBlockTranslation(paramContext, "%");
    loadBlockTranslation(paramContext, "random");
    loadBlockTranslation(paramContext, "stringLength");
    loadBlockTranslation(paramContext, "stringJoin");
    loadBlockTranslation(paramContext, "stringIndex");
    loadBlockTranslation(paramContext, "stringLastIndex");
    loadBlockTranslation(paramContext, "stringSub");
    loadBlockTranslation(paramContext, "stringEquals");
    loadBlockTranslation(paramContext, "stringContains");
    loadBlockTranslation(paramContext, "stringReplace");
    loadBlockTranslation(paramContext, "stringReplaceFirst");
    loadBlockTranslation(paramContext, "stringReplaceAll");
    loadBlockTranslation(paramContext, "toNumber");
    loadBlockTranslation(paramContext, "trim");
    loadBlockTranslation(paramContext, "toUpperCase");
    loadBlockTranslation(paramContext, "toLowerCase");
    loadBlockTranslation(paramContext, "toString");
    loadBlockTranslation(paramContext, "toStringWithDecimal");
    loadBlockTranslation(paramContext, "toStringFormat");
    loadBlockTranslation(paramContext, "addSourceDirectly");
    loadBlockTranslation(paramContext, "mapToStr");
    loadBlockTranslation(paramContext, "strToMap");
    loadBlockTranslation(paramContext, "listMapToStr");
    loadBlockTranslation(paramContext, "strToListMap");
    loadBlockTranslation(paramContext, "mathGetDip");
    loadBlockTranslation(paramContext, "mathGetDisplayWidth");
    loadBlockTranslation(paramContext, "mathGetDisplayHeight");
    loadBlockTranslation(paramContext, "mathPi");
    loadBlockTranslation(paramContext, "mathE");
    loadBlockTranslation(paramContext, "mathPow");
    loadBlockTranslation(paramContext, "mathMin");
    loadBlockTranslation(paramContext, "mathMax");
    loadBlockTranslation(paramContext, "mathSqrt");
    loadBlockTranslation(paramContext, "mathAbs");
    loadBlockTranslation(paramContext, "mathRound");
    loadBlockTranslation(paramContext, "mathCeil");
    loadBlockTranslation(paramContext, "mathFloor");
    loadBlockTranslation(paramContext, "mathSin");
    loadBlockTranslation(paramContext, "mathCos");
    loadBlockTranslation(paramContext, "mathTan");
    loadBlockTranslation(paramContext, "mathAsin");
    loadBlockTranslation(paramContext, "mathAcos");
    loadBlockTranslation(paramContext, "mathAtan");
    loadBlockTranslation(paramContext, "mathExp");
    loadBlockTranslation(paramContext, "mathLog");
    loadBlockTranslation(paramContext, "mathLog10");
    loadBlockTranslation(paramContext, "mathToRadian");
    loadBlockTranslation(paramContext, "mathToDegree");
    loadBlockTranslation(paramContext, "isDrawerOpen");
    loadBlockTranslation(paramContext, "openDrawer");
    loadBlockTranslation(paramContext, "closeDrawer");
    loadBlockTranslation(paramContext, "viewOnClick");
    loadBlockTranslation(paramContext, "setEnable");
    loadBlockTranslation(paramContext, "getEnable");
    loadBlockTranslation(paramContext, "setVisible");
    loadBlockTranslation(paramContext, "setClickable");
    loadBlockTranslation(paramContext, "setText");
    loadBlockTranslation(paramContext, "setTypeface");
    loadBlockTranslation(paramContext, "getText");
    loadBlockTranslation(paramContext, "setBgColor");
    loadBlockTranslation(paramContext, "setBgResource");
    loadBlockTranslation(paramContext, "setTextColor");
    loadBlockTranslation(paramContext, "setHint");
    loadBlockTranslation(paramContext, "setHintTextColor");
    loadBlockTranslation(paramContext, "setImage");
    loadBlockTranslation(paramContext, "setColorFilter");
    loadBlockTranslation(paramContext, "requestFocus");
    loadBlockTranslation(paramContext, "setRotate");
    loadBlockTranslation(paramContext, "getRotate");
    loadBlockTranslation(paramContext, "setAlpha");
    loadBlockTranslation(paramContext, "getAlpha");
    loadBlockTranslation(paramContext, "setTranslationX");
    loadBlockTranslation(paramContext, "getTranslationX");
    loadBlockTranslation(paramContext, "setTranslationY");
    loadBlockTranslation(paramContext, "getTranslationY");
    loadBlockTranslation(paramContext, "setScaleX");
    loadBlockTranslation(paramContext, "getScaleX");
    loadBlockTranslation(paramContext, "setScaleY");
    loadBlockTranslation(paramContext, "getScaleY");
    loadBlockTranslation(paramContext, "getLocationX");
    loadBlockTranslation(paramContext, "getLocationY");
    loadBlockTranslation(paramContext, "setChecked");
    loadBlockTranslation(paramContext, "getChecked");
    loadBlockTranslation(paramContext, "setThumbResource");
    loadBlockTranslation(paramContext, "setTrackResource");
    loadBlockTranslation(paramContext, "listSetData");
    loadBlockTranslation(paramContext, "listSetCustomViewData");
    loadBlockTranslation(paramContext, "listRefresh");
    loadBlockTranslation(paramContext, "listSetItemChecked");
    loadBlockTranslation(paramContext, "listGetCheckedPosition");
    loadBlockTranslation(paramContext, "listGetCheckedPositions");
    loadBlockTranslation(paramContext, "listGetCheckedCount");
    loadBlockTranslation(paramContext, "listSmoothScrollTo");
    loadBlockTranslation(paramContext, "spnSetData");
    loadBlockTranslation(paramContext, "spnRefresh");
    loadBlockTranslation(paramContext, "spnSetSelection");
    loadBlockTranslation(paramContext, "spnGetSelection");
    loadBlockTranslation(paramContext, "webViewLoadUrl");
    loadBlockTranslation(paramContext, "webViewGetUrl");
    loadBlockTranslation(paramContext, "webViewSetCacheMode");
    loadBlockTranslation(paramContext, "webViewCanGoBack");
    loadBlockTranslation(paramContext, "webViewCanGoForward");
    loadBlockTranslation(paramContext, "webViewGoBack");
    loadBlockTranslation(paramContext, "webViewGoForward");
    loadBlockTranslation(paramContext, "webViewClearCache");
    loadBlockTranslation(paramContext, "webViewClearHistory");
    loadBlockTranslation(paramContext, "webViewStopLoading");
    loadBlockTranslation(paramContext, "webViewZoomIn");
    loadBlockTranslation(paramContext, "webViewZoomOut");
    loadBlockTranslation(paramContext, "calendarViewGetDate");
    loadBlockTranslation(paramContext, "calendarViewSetDate");
    loadBlockTranslation(paramContext, "calendarViewSetMinDate");
    loadBlockTranslation(paramContext, "calnedarViewSetMaxDate");
    loadBlockTranslation(paramContext, "adViewLoadAd");
    loadBlockTranslation(paramContext, "mapViewSetMapType");
    loadBlockTranslation(paramContext, "mapViewMoveCamera");
    loadBlockTranslation(paramContext, "mapViewZoomTo");
    loadBlockTranslation(paramContext, "mapViewZoomIn");
    loadBlockTranslation(paramContext, "mapViewZoomOut");
    loadBlockTranslation(paramContext, "mapViewAddMarker");
    loadBlockTranslation(paramContext, "mapViewSetMarkerInfo");
    loadBlockTranslation(paramContext, "mapViewSetMarkerPosition");
    loadBlockTranslation(paramContext, "mapViewSetMarkerColor");
    loadBlockTranslation(paramContext, "mapViewSetMarkerIcon");
    loadBlockTranslation(paramContext, "mapViewSetMarkerVisible");
    loadBlockTranslation(paramContext, "intentSetAction");
    loadBlockTranslation(paramContext, "intentSetData");
    loadBlockTranslation(paramContext, "intentSetScreen");
    loadBlockTranslation(paramContext, "intentPutExtra");
    loadBlockTranslation(paramContext, "intentSetFlags");
    loadBlockTranslation(paramContext, "startActivity");
    loadBlockTranslation(paramContext, "intentGetString");
    loadBlockTranslation(paramContext, "finishActivity");
    loadBlockTranslation(paramContext, "fileGetData");
    loadBlockTranslation(paramContext, "fileSetData");
    loadBlockTranslation(paramContext, "fileRemoveData");
    loadBlockTranslation(paramContext, "calendarGetNow");
    loadBlockTranslation(paramContext, "calendarAdd");
    loadBlockTranslation(paramContext, "calendarSet");
    loadBlockTranslation(paramContext, "calendarFormat");
    loadBlockTranslation(paramContext, "calendarDiff");
    loadBlockTranslation(paramContext, "calendarGetTime");
    loadBlockTranslation(paramContext, "calendarSetTime");
    loadBlockTranslation(paramContext, "vibratorAction");
    loadBlockTranslation(paramContext, "timerAfter");
    loadBlockTranslation(paramContext, "timerEvery");
    loadBlockTranslation(paramContext, "timerCancel");
    loadBlockTranslation(paramContext, "dialogSetTitle");
    loadBlockTranslation(paramContext, "dialogSetMessage");
    loadBlockTranslation(paramContext, "dialogOkButton");
    loadBlockTranslation(paramContext, "dialogCancelButton");
    loadBlockTranslation(paramContext, "dialogNeutralButton");
    loadBlockTranslation(paramContext, "dialogShow");
    loadBlockTranslation(paramContext, "dialogDismiss");
    loadBlockTranslation(paramContext, "mediaplayerCreate");
    loadBlockTranslation(paramContext, "mediaplayerStart");
    loadBlockTranslation(paramContext, "mediaplayerPause");
    loadBlockTranslation(paramContext, "mediaplayerSeek");
    loadBlockTranslation(paramContext, "mediaplayerGetCurrent");
    loadBlockTranslation(paramContext, "mediaplayerGetDuration");
    loadBlockTranslation(paramContext, "mediaplayerIsPlaying");
    loadBlockTranslation(paramContext, "mediaplayerSetLooping");
    loadBlockTranslation(paramContext, "mediaplayerIsLooping");
    loadBlockTranslation(paramContext, "mediaplayerReset");
    loadBlockTranslation(paramContext, "mediaplayerRelease");
    loadBlockTranslation(paramContext, "soundpoolCreate");
    loadBlockTranslation(paramContext, "soundpoolLoad");
    loadBlockTranslation(paramContext, "soundpoolStreamPlay");
    loadBlockTranslation(paramContext, "soundpoolStreamStop");
    loadBlockTranslation(paramContext, "objectanimatorSetTarget");
    loadBlockTranslation(paramContext, "objectanimatorSetProperty");
    loadBlockTranslation(paramContext, "objectanimatorSetValue");
    loadBlockTranslation(paramContext, "objectanimatorSetFromTo");
    loadBlockTranslation(paramContext, "objectanimatorSetDuration");
    loadBlockTranslation(paramContext, "objectanimatorSetRepeatMode");
    loadBlockTranslation(paramContext, "objectanimatorSetRepeatCount");
    loadBlockTranslation(paramContext, "objectanimatorSetInterpolator");
    loadBlockTranslation(paramContext, "objectanimatorStart");
    loadBlockTranslation(paramContext, "objectanimatorCancel");
    loadBlockTranslation(paramContext, "objectanimatorIsRunning");
    loadBlockTranslation(paramContext, "firebaseAdd");
    loadBlockTranslation(paramContext, "firebasePush");
    loadBlockTranslation(paramContext, "firebaseGetPushKey");
    loadBlockTranslation(paramContext, "firebaseDelete");
    loadBlockTranslation(paramContext, "firebaseGetChildren");
    loadBlockTranslation(paramContext, "firebaseauthCreateUser");
    loadBlockTranslation(paramContext, "firebaseauthSignInUser");
    loadBlockTranslation(paramContext, "firebaseauthSignInAnonymously");
    loadBlockTranslation(paramContext, "firebaseauthIsLoggedIn");
    loadBlockTranslation(paramContext, "firebaseauthGetCurrentUser");
    loadBlockTranslation(paramContext, "firebaseauthGetUid");
    loadBlockTranslation(paramContext, "firebaseauthResetPassword");
    loadBlockTranslation(paramContext, "firebaseauthSignOutUser");
    loadBlockTranslation(paramContext, "firebaseStartListen");
    loadBlockTranslation(paramContext, "firebaseStopListen");
    loadBlockTranslation(paramContext, "gyroscopeStartListen");
    loadBlockTranslation(paramContext, "gyroscopeStopListen");
    loadBlockTranslation(paramContext, "interstitialadCreate");
    loadBlockTranslation(paramContext, "interstitialadLoadAd");
    loadBlockTranslation(paramContext, "interstitialadShow");
    loadBlockTranslation(paramContext, "firebasestorageUploadFile");
    loadBlockTranslation(paramContext, "firebasestorageDownloadFile");
    loadBlockTranslation(paramContext, "firebasestorageDelete");
    loadBlockTranslation(paramContext, "camerastarttakepicture");
    loadBlockTranslation(paramContext, "filepickerstartpickfiles");
    loadBlockTranslation(paramContext, "requestnetworkSetParams");
    loadBlockTranslation(paramContext, "requestnetworkSetHeaders");
    loadBlockTranslation(paramContext, "requestnetworkStartRequestNetwork");
    loadBlockTranslation(paramContext, "fileutildelete");
    loadBlockTranslation(paramContext, "fileutilcopy");
    loadBlockTranslation(paramContext, "fileutilwrite");
    loadBlockTranslation(paramContext, "fileutilread");
    loadBlockTranslation(paramContext, "fileutilmove");
    loadBlockTranslation(paramContext, "fileutilisexist");
    loadBlockTranslation(paramContext, "fileutilmakedir");
    loadBlockTranslation(paramContext, "fileutillistdir");
    loadBlockTranslation(paramContext, "fileutilisdir");
    loadBlockTranslation(paramContext, "fileutilisfile");
    loadBlockTranslation(paramContext, "fileutillength");
    loadBlockTranslation(paramContext, "fileutilStartsWith");
    loadBlockTranslation(paramContext, "fileutilEndsWith");
    loadBlockTranslation(paramContext, "fileutilGetLastSegmentPath");
    loadBlockTranslation(paramContext, "doToast");
    loadBlockTranslation(paramContext, "copyToClipboard");
    loadBlockTranslation(paramContext, "setTitle");
    loadBlockTranslation(paramContext, "seekBarGetMax");
    loadBlockTranslation(paramContext, "seekBarGetProgress");
    loadBlockTranslation(paramContext, "seekBarSetMax");
    loadBlockTranslation(paramContext, "seekBarSetProgress");
    loadBlockTranslation(paramContext, "getExternalStorageDir");
    loadBlockTranslation(paramContext, "getPackageDataDir");
    loadBlockTranslation(paramContext, "getPublicDir");
    loadBlockTranslation(paramContext, "resizeBitmapFileRetainRatio");
    loadBlockTranslation(paramContext, "resizeBitmapFileToSquare");
    loadBlockTranslation(paramContext, "resizeBitmapFileToCircle");
    loadBlockTranslation(paramContext, "resizeBitmapFileWithRoundedBorder");
    loadBlockTranslation(paramContext, "cropBitmapFileFromCenter");
    loadBlockTranslation(paramContext, "rotateBitmapFile");
    loadBlockTranslation(paramContext, "scaleBitmapFile");
    loadBlockTranslation(paramContext, "skewBitmapFile");
    loadBlockTranslation(paramContext, "setBitmapFileColorFilter");
    loadBlockTranslation(paramContext, "setBitmapFileBrightness");
    loadBlockTranslation(paramContext, "setBitmapFileContrast");
    loadBlockTranslation(paramContext, "setImageFilePath");
    loadBlockTranslation(paramContext, "setImageUrl");
    loadBlockTranslation(paramContext, "getJpegRotate");
    loadBlockTranslation(paramContext, "progressBarSetIndeterminate");
    loadBlockTranslation(paramContext, "textToSpeechSetPitch");
    loadBlockTranslation(paramContext, "textToSpeechSetSpeechRate");
    loadBlockTranslation(paramContext, "textToSpeechSpeak");
    loadBlockTranslation(paramContext, "textToSpeechIsSpeaking");
    loadBlockTranslation(paramContext, "textToSpeechStop");
    loadBlockTranslation(paramContext, "textToSpeechShutdown");
    loadBlockTranslation(paramContext, "speechToTextStartListening");
    loadBlockTranslation(paramContext, "speechToTextStopListening");
    loadBlockTranslation(paramContext, "speechToTextShutdown");
    loadBlockTranslation(paramContext, "bluetoothConnectReadyConnection");
    loadBlockTranslation(paramContext, "bluetoothConnectReadyConnectionToUuid");
    loadBlockTranslation(paramContext, "bluetoothConnectStartConnection");
    loadBlockTranslation(paramContext, "bluetoothConnectStartConnectionToUuid");
    loadBlockTranslation(paramContext, "bluetoothConnectStopConnection");
    loadBlockTranslation(paramContext, "bluetoothConnectSendData");
    loadBlockTranslation(paramContext, "bluetoothConnectIsBluetoothEnabled");
    loadBlockTranslation(paramContext, "bluetoothConnectIsBluetoothActivated");
    loadBlockTranslation(paramContext, "bluetoothConnectActivateBluetooth");
    loadBlockTranslation(paramContext, "bluetoothConnectGetPairedDevices");
    loadBlockTranslation(paramContext, "bluetoothConnectGetRandomUuid");
    loadBlockTranslation(paramContext, "locationManagerRequestLocationUpdates");
    loadBlockTranslation(paramContext, "locationManagerRemoveUpdates");
  }
  
  public HashMap<String, String> loadTranslationsFromFile(String paramString) {
    HashMap<String, String> hashMap;
    HashMap<Object, Object> hashMap1 = new HashMap<Object, Object>();
    if (!(new File(paramString)).exists())
      return (HashMap)hashMap1; 
    try {
      hashMap = parseXmlTranslations(paramString);
    } catch (Exception exception) {
      hashMap = (HashMap)hashMap1;
    } 
    return hashMap;
  }
  
  public void loadBlockTranslation(Context paramContext, String paramString) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("block_");
    stringBuilder.append(BlockSpecRegistry.getBlockSpec(paramString));
    String str = stringBuilder.toString();
    ArrayList<String> arrayList = BlockSpecRegistry.getBlockParams(paramString);
    this.eventTranslations.put(paramString, getBlockTranslation(paramContext, str, arrayList));
  }
  
  public boolean reloadTranslations(Context paramContext) {
    boolean bool;
    if (this.blockTranslations == null)
      this.blockTranslations = new HashMap<String, String>(); 
    HashMap<String, String> hashMap = this.blockTranslations;
    if (hashMap != null)
      hashMap.clear(); 
    this.blockTranslations = loadTranslationsFromFile(this.translationDir);
    loadEventTranslations(paramContext);
    if (!this.blockTranslations.isEmpty() && this.isLoaded) {
      bool = true;
    } else {
      bool = false;
    } 
    return bool;
  }
  
  public void loadEventSpecTranslation(Context paramContext, String paramString) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("root_spec_");
    stringBuilder.append(BlockSpecRegistry.getEventSpec(paramString));
    String str = stringBuilder.toString();
    ArrayList<String> arrayList = BlockSpecRegistry.getBlockMenuItems(paramString);
    this.eventTranslations.put(paramString, getBlockTranslation(paramContext, str, arrayList));
  }
}
