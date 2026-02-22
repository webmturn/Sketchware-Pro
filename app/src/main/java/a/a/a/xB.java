package a.a.a;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import mod.agus.jcoderz.editor.event.ManageEvent;

public class xB {
  public static xB a;
  
  public HashMap<String, String> b;
  
  public HashMap<String, String> c;
  
  public String d = wq.l();
  
  public boolean e;
  
  public final String f = "block";
  
  public final String g = "root_spec";
  
  public xB() {
    if (this.b == null)
      this.b = new HashMap<String, String>(); 
    if (this.c == null)
      this.c = new HashMap<String, String>(); 
  }
  
  public static xB b() {
    if (a == null) {
      synchronized (xB.class) {
        if (a == null) {
          a = new xB();
        }
      }
    }
    return a;
  }
  
  public String a(Context paramContext, int paramInt) {
    return a(paramContext.getResources(), paramInt);
  }
  
  public String a(Context paramContext, int paramInt, Object... paramVarArgs) {
    return a(paramContext.getResources(), paramInt, paramVarArgs);
  }
  
  public String a(Context paramContext, String paramString) {
    if (this.b == null)
      this.b = new HashMap<String, String>(); 
    if (this.b.isEmpty()) {
      this.e = false;
      this.b = b(this.d);
    } 
    if (this.c == null)
      this.c = new HashMap<String, String>(); 
    if (this.c.isEmpty())
      a(paramContext); 
    paramString = this.c.get(paramString);
    String str = paramString;
    if (paramString == null)
      str = ""; 
    return str;
  }
  
  public String a(Context paramContext, String paramString1, String paramString2) {
    if (this.b == null)
      this.b = new HashMap<String, String>(); 
    boolean bool = this.b.isEmpty();
    byte b = 0;
    if (bool) {
      this.e = false;
      this.b = b(this.d);
    } 
    if (this.c == null)
      this.c = new HashMap<String, String>(); 
    if (this.c.isEmpty())
      a(paramContext); 
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
        String result = this.c.get(paramString2);
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
    String str = this.c.get(paramString2);
    paramString2 = str;
    if (str == null)
      paramString2 = ""; 
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(a((Context)null, 2131626026));
    stringBuilder.append(" ");
    stringBuilder.append(paramString1);
    stringBuilder.append(" ");
    stringBuilder.append(paramString2);
    return stringBuilder.toString();
  }
  
  public String a(Context paramContext, String paramString, ArrayList<String> paramArrayList) {
    int bodyCount = paramArrayList.size() > 1 ? paramArrayList.size() - 1 : 0;
    if (this.b == null)
      this.b = new HashMap<String, String>();
    if (this.b.isEmpty()) {
      this.e = false;
      this.b = b(this.d);
    }
    StringBuilder result = new StringBuilder(1024);
    String headKey = paramString + "_head";
    String tailKey = paramString + "_tail";
    boolean useTranslation = false;
    if (this.b != null && this.b.containsKey(headKey) && this.b.containsKey(tailKey)) {
      useTranslation = true;
      for (int i = 0; i < bodyCount; i++) {
        String bk = paramString + "_body_" + (i + 1);
        if (!this.b.containsKey(bk)) {
          useTranslation = false;
          break;
        }
      }
    }
    String headStr;
    if (useTranslation) {
      headStr = (String) this.b.get(headKey);
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
        bodyStr = (String) this.b.get(bodyKey);
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
      tailStr = (String) this.b.get(tailKey);
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
  
  public String a(Resources paramResources, int paramInt) {
    String str = paramResources.getResourceEntryName(paramInt);
    if (this.b == null)
      this.b = new HashMap<String, String>(); 
    try {
      if (this.b.isEmpty()) {
        this.e = false;
        this.b = b(this.d);
      } 
      return (this.b.containsKey(str) && this.b.get(str) != null && ((String)this.b.get(str)).length() > 0) ? ((String)this.b.get(str)).replaceAll("\\\\\\'", "'").replaceAll("\\\\\\\"", "\"").replaceAll("\\\\n", "\\\n") : paramResources.getString(paramInt);
    } catch (Exception exception) {
      return paramResources.getString(paramInt);
    } 
  }
  
  public String a(Resources paramResources, int paramInt, Object... paramVarArgs) {
    String str = paramResources.getResourceEntryName(paramInt);
    if (this.b == null)
      this.b = new HashMap<String, String>(); 
    boolean bool = this.b.isEmpty();
    byte b = 0;
    if (bool) {
      this.e = false;
      this.b = b(this.d);
    } 
    try {
      if (this.b.containsKey(str) && this.b.get(str) != null && ((String)this.b.get(str)).length() > 0) {
        String object = ((String)this.b.get(str)).replaceAll("\\\\\\'", "'").replaceAll("\\\\\\\"", "\"").replaceAll("\\\\n", "\\\n");
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
  
  public final HashMap<String, String> a(String paramString) {
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
      this.e = true;
    } catch (Exception ex) {
      this.e = false;
    } finally {
      try { if (fis != null) fis.close(); } catch (Exception ignored) {}
      try { if (isr != null) isr.close(); } catch (Exception ignored) {}
    }
    return result;
  }
  
  public HashMap<String, String> a(byte[] paramArrayOfbyte) {
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
      this.e = true;
    } catch (Exception ex) {
      this.e = false;
    } finally {
      try { if (bis != null) bis.close(); } catch (Exception ignored) {}
      try { if (isr != null) isr.close(); } catch (Exception ignored) {}
    }
    return result;
  }
  
  public void a() {
    HashMap<String, String> hashMap = this.b;
    if (hashMap != null) {
      hashMap.clear();
      this.b = null;
    } 
    hashMap = this.c;
    if (hashMap != null) {
      hashMap.clear();
      this.c = null;
    } 
  }
  
  public void a(Context paramContext) {
    this.c = new HashMap<String, String>();
    c(paramContext, "initializeLogic");
    c(paramContext, "onBackPressed");
    c(paramContext, "onPostCreate");
    c(paramContext, "onStart");
    c(paramContext, "onStop");
    c(paramContext, "onDestroy");
    c(paramContext, "onResume");
    c(paramContext, "onPause");
    c(paramContext, "moreBlock");
    c(paramContext, "onClick");
    c(paramContext, "onCheckedChange");
    c(paramContext, "onItemSelected");
    c(paramContext, "onItemClicked");
    c(paramContext, "onItemLongClicked");
    c(paramContext, "onTextChanged");
    c(paramContext, "onPageStarted");
    c(paramContext, "onPageFinished");
    c(paramContext, "onProgressChanged");
    c(paramContext, "onStartTrackingTouch");
    c(paramContext, "onStopTrackingTouch");
    c(paramContext, "onAnimationStart");
    c(paramContext, "onAnimationEnd");
    c(paramContext, "onAnimationCancel");
    c(paramContext, "onBindCustomView");
    c(paramContext, "onDateChange");
    c(paramContext, "onChildAdded");
    c(paramContext, "onChildChanged");
    c(paramContext, "onChildRemoved");
    c(paramContext, "onCancelled");
    c(paramContext, "onSensorChanged");
    c(paramContext, "onCreateUserComplete");
    c(paramContext, "onSignInUserComplete");
    c(paramContext, "onResetPasswordEmailSent");
    c(paramContext, "onUploadProgress");
    c(paramContext, "onDownloadProgress");
    c(paramContext, "onUploadSuccess");
    c(paramContext, "onDownloadSuccess");
    c(paramContext, "onDeleteSuccess");
    c(paramContext, "onFailure");
    c(paramContext, "onPictureTaken");
    c(paramContext, "onPictureTakenCancel");
    c(paramContext, "onFilesPicked");
    c(paramContext, "onFilesPickedCancel");
    c(paramContext, "onAdLoaded");
    c(paramContext, "onAdFailedToLoad");
    c(paramContext, "onAdOpened");
    c(paramContext, "onAdClosed");
    c(paramContext, "onResponse");
    c(paramContext, "onErrorResponse");
    c(paramContext, "onSpeechResult");
    c(paramContext, "onSpeechError");
    c(paramContext, "onConnected");
    c(paramContext, "onDataReceived");
    c(paramContext, "onDataSent");
    c(paramContext, "onConnectionError");
    c(paramContext, "onConnectionStopped");
    c(paramContext, "onMapReady");
    c(paramContext, "onMarkerClicked");
    c(paramContext, "onLocationChanged");
    b(paramContext, "viewOnClick");
    b(paramContext, "setVarBoolean");
    b(paramContext, "setVarInt");
    b(paramContext, "increaseInt");
    b(paramContext, "decreaseInt");
    b(paramContext, "setVarString");
    b(paramContext, "mapCreateNew");
    b(paramContext, "mapPut");
    b(paramContext, "mapGet");
    b(paramContext, "mapContainKey");
    b(paramContext, "mapRemoveKey");
    b(paramContext, "mapSize");
    b(paramContext, "mapClear");
    b(paramContext, "mapIsEmpty");
    b(paramContext, "mapGetAllKeys");
    b(paramContext, "addListInt");
    b(paramContext, "insertListInt");
    b(paramContext, "deleteList");
    b(paramContext, "getAtListInt");
    b(paramContext, "indexListInt");
    b(paramContext, "lengthList");
    b(paramContext, "containListInt");
    b(paramContext, "clearList");
    b(paramContext, "addListStr");
    b(paramContext, "insertListStr");
    b(paramContext, "getAtListStr");
    b(paramContext, "indexListStr");
    b(paramContext, "containListStr");
    b(paramContext, "addListMap");
    b(paramContext, "insertListMap");
    b(paramContext, "getAtListMap");
    b(paramContext, "setListMap");
    b(paramContext, "containListMap");
    b(paramContext, "addMapToList");
    b(paramContext, "insertMapToList");
    b(paramContext, "getMapInList");
    b(paramContext, "repeat");
    b(paramContext, "forever");
    b(paramContext, "break");
    b(paramContext, "if");
    b(paramContext, "ifElse");
    b(paramContext, "else");
    b(paramContext, "true");
    b(paramContext, "false");
    b(paramContext, "<");
    b(paramContext, "=");
    b(paramContext, ">");
    b(paramContext, "&&");
    b(paramContext, "||");
    b(paramContext, "not");
    b(paramContext, "+");
    b(paramContext, "-");
    b(paramContext, "*");
    b(paramContext, "/");
    b(paramContext, "%");
    b(paramContext, "random");
    b(paramContext, "stringLength");
    b(paramContext, "stringJoin");
    b(paramContext, "stringIndex");
    b(paramContext, "stringLastIndex");
    b(paramContext, "stringSub");
    b(paramContext, "stringEquals");
    b(paramContext, "stringContains");
    b(paramContext, "stringReplace");
    b(paramContext, "stringReplaceFirst");
    b(paramContext, "stringReplaceAll");
    b(paramContext, "toNumber");
    b(paramContext, "trim");
    b(paramContext, "toUpperCase");
    b(paramContext, "toLowerCase");
    b(paramContext, "toString");
    b(paramContext, "toStringWithDecimal");
    b(paramContext, "toStringFormat");
    b(paramContext, "addSourceDirectly");
    b(paramContext, "mapToStr");
    b(paramContext, "strToMap");
    b(paramContext, "listMapToStr");
    b(paramContext, "strToListMap");
    b(paramContext, "mathGetDip");
    b(paramContext, "mathGetDisplayWidth");
    b(paramContext, "mathGetDisplayHeight");
    b(paramContext, "mathPi");
    b(paramContext, "mathE");
    b(paramContext, "mathPow");
    b(paramContext, "mathMin");
    b(paramContext, "mathMax");
    b(paramContext, "mathSqrt");
    b(paramContext, "mathAbs");
    b(paramContext, "mathRound");
    b(paramContext, "mathCeil");
    b(paramContext, "mathFloor");
    b(paramContext, "mathSin");
    b(paramContext, "mathCos");
    b(paramContext, "mathTan");
    b(paramContext, "mathAsin");
    b(paramContext, "mathAcos");
    b(paramContext, "mathAtan");
    b(paramContext, "mathExp");
    b(paramContext, "mathLog");
    b(paramContext, "mathLog10");
    b(paramContext, "mathToRadian");
    b(paramContext, "mathToDegree");
    b(paramContext, "isDrawerOpen");
    b(paramContext, "openDrawer");
    b(paramContext, "closeDrawer");
    b(paramContext, "viewOnClick");
    b(paramContext, "setEnable");
    b(paramContext, "getEnable");
    b(paramContext, "setVisible");
    b(paramContext, "setClickable");
    b(paramContext, "setText");
    b(paramContext, "setTypeface");
    b(paramContext, "getText");
    b(paramContext, "setBgColor");
    b(paramContext, "setBgResource");
    b(paramContext, "setTextColor");
    b(paramContext, "setHint");
    b(paramContext, "setHintTextColor");
    b(paramContext, "setImage");
    b(paramContext, "setColorFilter");
    b(paramContext, "requestFocus");
    b(paramContext, "setRotate");
    b(paramContext, "getRotate");
    b(paramContext, "setAlpha");
    b(paramContext, "getAlpha");
    b(paramContext, "setTranslationX");
    b(paramContext, "getTranslationX");
    b(paramContext, "setTranslationY");
    b(paramContext, "getTranslationY");
    b(paramContext, "setScaleX");
    b(paramContext, "getScaleX");
    b(paramContext, "setScaleY");
    b(paramContext, "getScaleY");
    b(paramContext, "getLocationX");
    b(paramContext, "getLocationY");
    b(paramContext, "setChecked");
    b(paramContext, "getChecked");
    b(paramContext, "setThumbResource");
    b(paramContext, "setTrackResource");
    b(paramContext, "listSetData");
    b(paramContext, "listSetCustomViewData");
    b(paramContext, "listRefresh");
    b(paramContext, "listSetItemChecked");
    b(paramContext, "listGetCheckedPosition");
    b(paramContext, "listGetCheckedPositions");
    b(paramContext, "listGetCheckedCount");
    b(paramContext, "listSmoothScrollTo");
    b(paramContext, "spnSetData");
    b(paramContext, "spnRefresh");
    b(paramContext, "spnSetSelection");
    b(paramContext, "spnGetSelection");
    b(paramContext, "webViewLoadUrl");
    b(paramContext, "webViewGetUrl");
    b(paramContext, "webViewSetCacheMode");
    b(paramContext, "webViewCanGoBack");
    b(paramContext, "webViewCanGoForward");
    b(paramContext, "webViewGoBack");
    b(paramContext, "webViewGoForward");
    b(paramContext, "webViewClearCache");
    b(paramContext, "webViewClearHistory");
    b(paramContext, "webViewStopLoading");
    b(paramContext, "webViewZoomIn");
    b(paramContext, "webViewZoomOut");
    b(paramContext, "calendarViewGetDate");
    b(paramContext, "calendarViewSetDate");
    b(paramContext, "calendarViewSetMinDate");
    b(paramContext, "calnedarViewSetMaxDate");
    b(paramContext, "adViewLoadAd");
    b(paramContext, "mapViewSetMapType");
    b(paramContext, "mapViewMoveCamera");
    b(paramContext, "mapViewZoomTo");
    b(paramContext, "mapViewZoomIn");
    b(paramContext, "mapViewZoomOut");
    b(paramContext, "mapViewAddMarker");
    b(paramContext, "mapViewSetMarkerInfo");
    b(paramContext, "mapViewSetMarkerPosition");
    b(paramContext, "mapViewSetMarkerColor");
    b(paramContext, "mapViewSetMarkerIcon");
    b(paramContext, "mapViewSetMarkerVisible");
    b(paramContext, "intentSetAction");
    b(paramContext, "intentSetData");
    b(paramContext, "intentSetScreen");
    b(paramContext, "intentPutExtra");
    b(paramContext, "intentSetFlags");
    b(paramContext, "startActivity");
    b(paramContext, "intentGetString");
    b(paramContext, "finishActivity");
    b(paramContext, "fileGetData");
    b(paramContext, "fileSetData");
    b(paramContext, "fileRemoveData");
    b(paramContext, "calendarGetNow");
    b(paramContext, "calendarAdd");
    b(paramContext, "calendarSet");
    b(paramContext, "calendarFormat");
    b(paramContext, "calendarDiff");
    b(paramContext, "calendarGetTime");
    b(paramContext, "calendarSetTime");
    b(paramContext, "vibratorAction");
    b(paramContext, "timerAfter");
    b(paramContext, "timerEvery");
    b(paramContext, "timerCancel");
    b(paramContext, "dialogSetTitle");
    b(paramContext, "dialogSetMessage");
    b(paramContext, "dialogOkButton");
    b(paramContext, "dialogCancelButton");
    b(paramContext, "dialogNeutralButton");
    b(paramContext, "dialogShow");
    b(paramContext, "dialogDismiss");
    b(paramContext, "mediaplayerCreate");
    b(paramContext, "mediaplayerStart");
    b(paramContext, "mediaplayerPause");
    b(paramContext, "mediaplayerSeek");
    b(paramContext, "mediaplayerGetCurrent");
    b(paramContext, "mediaplayerGetDuration");
    b(paramContext, "mediaplayerIsPlaying");
    b(paramContext, "mediaplayerSetLooping");
    b(paramContext, "mediaplayerIsLooping");
    b(paramContext, "mediaplayerReset");
    b(paramContext, "mediaplayerRelease");
    b(paramContext, "soundpoolCreate");
    b(paramContext, "soundpoolLoad");
    b(paramContext, "soundpoolStreamPlay");
    b(paramContext, "soundpoolStreamStop");
    b(paramContext, "objectanimatorSetTarget");
    b(paramContext, "objectanimatorSetProperty");
    b(paramContext, "objectanimatorSetValue");
    b(paramContext, "objectanimatorSetFromTo");
    b(paramContext, "objectanimatorSetDuration");
    b(paramContext, "objectanimatorSetRepeatMode");
    b(paramContext, "objectanimatorSetRepeatCount");
    b(paramContext, "objectanimatorSetInterpolator");
    b(paramContext, "objectanimatorStart");
    b(paramContext, "objectanimatorCancel");
    b(paramContext, "objectanimatorIsRunning");
    b(paramContext, "firebaseAdd");
    b(paramContext, "firebasePush");
    b(paramContext, "firebaseGetPushKey");
    b(paramContext, "firebaseDelete");
    b(paramContext, "firebaseGetChildren");
    b(paramContext, "firebaseauthCreateUser");
    b(paramContext, "firebaseauthSignInUser");
    b(paramContext, "firebaseauthSignInAnonymously");
    b(paramContext, "firebaseauthIsLoggedIn");
    b(paramContext, "firebaseauthGetCurrentUser");
    b(paramContext, "firebaseauthGetUid");
    b(paramContext, "firebaseauthResetPassword");
    b(paramContext, "firebaseauthSignOutUser");
    b(paramContext, "firebaseStartListen");
    b(paramContext, "firebaseStopListen");
    b(paramContext, "gyroscopeStartListen");
    b(paramContext, "gyroscopeStopListen");
    b(paramContext, "interstitialadCreate");
    b(paramContext, "interstitialadLoadAd");
    b(paramContext, "interstitialadShow");
    b(paramContext, "firebasestorageUploadFile");
    b(paramContext, "firebasestorageDownloadFile");
    b(paramContext, "firebasestorageDelete");
    b(paramContext, "camerastarttakepicture");
    b(paramContext, "filepickerstartpickfiles");
    b(paramContext, "requestnetworkSetParams");
    b(paramContext, "requestnetworkSetHeaders");
    b(paramContext, "requestnetworkStartRequestNetwork");
    b(paramContext, "fileutildelete");
    b(paramContext, "fileutilcopy");
    b(paramContext, "fileutilwrite");
    b(paramContext, "fileutilread");
    b(paramContext, "fileutilmove");
    b(paramContext, "fileutilisexist");
    b(paramContext, "fileutilmakedir");
    b(paramContext, "fileutillistdir");
    b(paramContext, "fileutilisdir");
    b(paramContext, "fileutilisfile");
    b(paramContext, "fileutillength");
    b(paramContext, "fileutilStartsWith");
    b(paramContext, "fileutilEndsWith");
    b(paramContext, "fileutilGetLastSegmentPath");
    b(paramContext, "doToast");
    b(paramContext, "copyToClipboard");
    b(paramContext, "setTitle");
    b(paramContext, "seekBarGetMax");
    b(paramContext, "seekBarGetProgress");
    b(paramContext, "seekBarSetMax");
    b(paramContext, "seekBarSetProgress");
    b(paramContext, "getExternalStorageDir");
    b(paramContext, "getPackageDataDir");
    b(paramContext, "getPublicDir");
    b(paramContext, "resizeBitmapFileRetainRatio");
    b(paramContext, "resizeBitmapFileToSquare");
    b(paramContext, "resizeBitmapFileToCircle");
    b(paramContext, "resizeBitmapFileWithRoundedBorder");
    b(paramContext, "cropBitmapFileFromCenter");
    b(paramContext, "rotateBitmapFile");
    b(paramContext, "scaleBitmapFile");
    b(paramContext, "skewBitmapFile");
    b(paramContext, "setBitmapFileColorFilter");
    b(paramContext, "setBitmapFileBrightness");
    b(paramContext, "setBitmapFileContrast");
    b(paramContext, "setImageFilePath");
    b(paramContext, "setImageUrl");
    b(paramContext, "getJpegRotate");
    b(paramContext, "progressBarSetIndeterminate");
    b(paramContext, "textToSpeechSetPitch");
    b(paramContext, "textToSpeechSetSpeechRate");
    b(paramContext, "textToSpeechSpeak");
    b(paramContext, "textToSpeechIsSpeaking");
    b(paramContext, "textToSpeechStop");
    b(paramContext, "textToSpeechShutdown");
    b(paramContext, "speechToTextStartListening");
    b(paramContext, "speechToTextStopListening");
    b(paramContext, "speechToTextShutdown");
    b(paramContext, "bluetoothConnectReadyConnection");
    b(paramContext, "bluetoothConnectReadyConnectionToUuid");
    b(paramContext, "bluetoothConnectStartConnection");
    b(paramContext, "bluetoothConnectStartConnectionToUuid");
    b(paramContext, "bluetoothConnectStopConnection");
    b(paramContext, "bluetoothConnectSendData");
    b(paramContext, "bluetoothConnectIsBluetoothEnabled");
    b(paramContext, "bluetoothConnectIsBluetoothActivated");
    b(paramContext, "bluetoothConnectActivateBluetooth");
    b(paramContext, "bluetoothConnectGetPairedDevices");
    b(paramContext, "bluetoothConnectGetRandomUuid");
    b(paramContext, "locationManagerRequestLocationUpdates");
    b(paramContext, "locationManagerRemoveUpdates");
  }
  
  public HashMap<String, String> b(String paramString) {
    HashMap<String, String> hashMap;
    HashMap<Object, Object> hashMap1 = new HashMap<Object, Object>();
    if (!(new File(paramString)).exists())
      return (HashMap)hashMap1; 
    try {
      hashMap = a(paramString);
    } catch (Exception exception) {
      hashMap = (HashMap)hashMap1;
    } 
    return hashMap;
  }
  
  public void b(Context paramContext, String paramString) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("block_");
    stringBuilder.append(lq.d(paramString));
    String str = stringBuilder.toString();
    ArrayList<String> arrayList = lq.a(paramString);
    this.c.put(paramString, a(paramContext, str, arrayList));
  }
  
  public boolean b(Context paramContext) {
    boolean bool;
    if (this.b == null)
      this.b = new HashMap<String, String>(); 
    HashMap<String, String> hashMap = this.b;
    if (hashMap != null)
      hashMap.clear(); 
    this.b = b(this.d);
    a(paramContext);
    if (!this.b.isEmpty() && this.e) {
      bool = true;
    } else {
      bool = false;
    } 
    return bool;
  }
  
  public void c(Context paramContext, String paramString) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("root_spec_");
    stringBuilder.append(lq.c(paramString));
    String str = stringBuilder.toString();
    ArrayList<String> arrayList = lq.b(paramString);
    this.c.put(paramString, a(paramContext, str, arrayList));
  }
}


/* Location:              C:\Users\Administrator\IdeaProjects\Sketchware-Pro\app\libs\a.a.a-notimportant-classes.jar!\a\a\a\xB.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */