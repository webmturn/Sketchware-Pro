package pro.sketchware.core;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import pro.sketchware.R;
import mod.agus.jcoderz.editor.event.ManageEvent;

public class StringResource {
  private static volatile StringResource instance;
  
  private HashMap<String, String> blockTranslations;
  
  private HashMap<String, String> eventTranslations;
  
  private final String translationDir = SketchwarePaths.getLocalizationStringsPath();
  
  private boolean isLoaded;
  
  public StringResource() {
    blockTranslations = new HashMap<>();
    eventTranslations = new HashMap<>();
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
    ensureTranslationsLoaded(context);
    String translation = eventTranslations.get(eventKey);
    return translation != null ? translation : "";
  }
  
  private void ensureTranslationsLoaded(Context context) {
    if (blockTranslations == null)
      blockTranslations = new HashMap<>();
    if (blockTranslations.isEmpty()) {
      isLoaded = false;
      blockTranslations = loadTranslationsFromFile(translationDir);
    }
    if (eventTranslations == null)
      eventTranslations = new HashMap<>();
    if (eventTranslations.isEmpty())
      loadEventTranslations(context);
  }
  
  private static final Set<String> KNOWN_ROOT_SPEC_EVENTS = new HashSet<>(Arrays.asList(
      "onClick", "onCheckedChange", "onItemSelected", "onItemClicked", "onItemLongClicked",
      "onTextChanged", "onPageStarted", "onPageFinished", "onProgressChanged",
      "onStartTrackingTouch", "onStopTrackingTouch", "onAnimationStart", "onAnimationEnd",
      "onAnimationCancel", "onBindCustomView", "onDateChange", "onChildAdded", "onChildChanged",
      "onChildRemoved", "onCancelled", "onUploadProgress", "onDownloadProgress",
      "onUploadSuccess", "onDownloadSuccess", "onDeleteSuccess", "onFailure",
      "onPictureTaken", "onPictureTakenCancel", "onFilesPicked", "onFilesPickedCancel",
      "onResponse", "onErrorResponse", "onSpeechResult", "onSpeechError",
      "onConnected", "onDataReceived", "onDataSent", "onConnectionError", "onConnectionStopped",
      "onMapReady", "onMarkerClicked", "onLocationChanged"
  ));

  public String getRootSpecTranslation(Context context, String blockType, String eventName) {
    ensureTranslationsLoaded(context);

    if (!KNOWN_ROOT_SPEC_EVENTS.contains(eventName)) {
      String result = eventTranslations.get(eventName);
      if (result == null)
        result = ManageEvent.getExtraEventSpec(blockType, eventName);
      return result;
    }

    String translation = eventTranslations.get(eventName);
    if (translation == null)
      translation = "";
    return getTranslatedString(context, R.string.root_spec_common_when)
        + " " + blockType + " " + translation;
  }
  
  public String getBlockTranslation(Context context, String blockKeyPrefix, ArrayList<String> specParts) {
    int bodyCount = specParts.size() > 1 ? specParts.size() - 1 : 0;
    if (blockTranslations == null)
      blockTranslations = new HashMap<>();
    if (blockTranslations.isEmpty()) {
      isLoaded = false;
      blockTranslations = loadTranslationsFromFile(translationDir);
    }
    StringBuilder result = new StringBuilder(1024);
    String headKey = blockKeyPrefix + "_head";
    String tailKey = blockKeyPrefix + "_tail";
    boolean useTranslation = false;
    if (blockTranslations.containsKey(headKey) && blockTranslations.containsKey(tailKey)) {
      useTranslation = true;
      for (int i = 0; i < bodyCount; i++) {
        String bk = blockKeyPrefix + "_body_" + (i + 1);
        if (!blockTranslations.containsKey(bk)) {
          useTranslation = false;
          break;
        }
      }
    }
    String headStr = resolveTranslationString(context, useTranslation, headKey);
    result.append(headStr);
    if (!specParts.isEmpty()) {
      if (result.length() > 0) result.append(" ");
      result.append(specParts.get(0));
    }
    for (int bodyIdx = 1; bodyIdx <= bodyCount; bodyIdx++) {
      String bodyKey = blockKeyPrefix + "_body_" + bodyIdx;
      String bodyStr = resolveTranslationString(context, useTranslation, bodyKey);
      if (!bodyStr.isEmpty()) result.append(" ");
      result.append(bodyStr);
      if (result.length() > 0) result.append(" ");
      result.append(specParts.get(bodyIdx));
    }
    String tailStr = resolveTranslationString(context, useTranslation, tailKey);
    if (result.length() > 0 && !tailStr.isEmpty()) result.append(" ");
    result.append(tailStr);
    return result.toString();
  }
  
  private String resolveTranslationString(Context context, boolean useTranslation, String key) {
    if (useTranslation) {
      String value = blockTranslations.get(key);
      return value != null ? value : "";
    }
    try {
      return context.getResources().getString(
          context.getResources().getIdentifier(key, "string", context.getPackageName()));
    } catch (Exception e) {
      return "";
    }
  }
  
  public String getTranslatedStringFromRes(Resources resources, int resId) {
    String resEntryName = resources.getResourceEntryName(resId);
    if (blockTranslations == null)
      blockTranslations = new HashMap<>();
    try {
      if (blockTranslations.isEmpty()) {
        isLoaded = false;
        blockTranslations = loadTranslationsFromFile(translationDir);
      }
      String value = blockTranslations.get(resEntryName);
      if (value != null && !value.isEmpty()) {
        return value.replaceAll("\\\\\\'" , "'").replaceAll("\\\\\\\"", "\"").replaceAll("\\\\n", "\\\n");
      }
      return resources.getString(resId);
    } catch (Exception exception) {
      return resources.getString(resId);
    }
  }
  
  public String getTranslatedStringFormattedFromRes(Resources resources, int resId, Object... formatArgs) {
    String resEntryName = resources.getResourceEntryName(resId);
    if (blockTranslations == null)
      blockTranslations = new HashMap<>();
    if (blockTranslations.isEmpty()) {
      isLoaded = false;
      blockTranslations = loadTranslationsFromFile(translationDir);
    }
    try {
      String value = blockTranslations.get(resEntryName);
      if (value != null && !value.isEmpty()) {
        String template = value.replaceAll("\\\\\\'" , "'").replaceAll("\\\\\\\"", "\"").replaceAll("\\\\n", "\\\n");
        int replaceCount = 0;
        for (Object formatArg : formatArgs) {
          if (!template.contains("%")) break;
          String argStr = formatArg.toString();
          if (argStr.equals("\\n")) argStr = "\\\\n";
          template = template.replaceFirst("%s", argStr);
          replaceCount++;
        }
        if (replaceCount == formatArgs.length && !template.contains("%"))
          return template;
      }
    } catch (Exception exception) {
      Log.e("ERROR", "Failed to load (" + resEntryName + ")", exception);
    }
    return resources.getString(resId, formatArgs);
  }
  
  public final HashMap<String, String> parseXmlTranslations(String filePath) {
    HashMap<String, String> result = new HashMap<>();
    if (!new File(filePath).exists()) {
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
      while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
        if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG && "string".equals(parser.getName())) {
          result.put(parser.getAttributeValue(null, "name"), parser.nextText());
        }
        eventType = parser.next();
      }
      isLoaded = true;
    } catch (Exception ex) {
      isLoaded = false;
    } finally {
      try { if (fis != null) fis.close(); } catch (Exception ignored) { Log.w("StringResource", "Failed to close stream", ignored); }
      try { if (isr != null) isr.close(); } catch (Exception ignored) { Log.w("StringResource", "Failed to close reader", ignored); }
    }
    return result;
  }
  
  public HashMap<String, String> parseXmlTranslationsFromBytes(byte[] data) {
    HashMap<String, String> result = new HashMap<>();
    java.io.ByteArrayInputStream bis = null;
    java.io.InputStreamReader isr = null;
    try {
      org.xmlpull.v1.XmlPullParser parser = org.xmlpull.v1.XmlPullParserFactory.newInstance().newPullParser();
      bis = new java.io.ByteArrayInputStream(data);
      isr = new java.io.InputStreamReader(bis, "UTF-8");
      parser.setInput(isr);
      int eventType = parser.getEventType();
      while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
        if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG && "string".equals(parser.getName())) {
          result.put(parser.getAttributeValue(null, "name"), parser.nextText());
        }
        eventType = parser.next();
      }
      isLoaded = true;
    } catch (Exception ex) {
      isLoaded = false;
    } finally {
      try { if (bis != null) bis.close(); } catch (Exception ignored) { Log.w("StringResource", "Failed to close stream", ignored); }
      try { if (isr != null) isr.close(); } catch (Exception ignored) { Log.w("StringResource", "Failed to close reader", ignored); }
    }
    return result;
  }
  
  public void clearTranslations() {
    if (blockTranslations != null) {
      blockTranslations.clear();
      blockTranslations = null;
    }
    if (eventTranslations != null) {
      eventTranslations.clear();
      eventTranslations = null;
    }
  }
  
  public void loadEventTranslations(Context context) {
    eventTranslations = new HashMap<>();
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
    loadBlockTranslation(context, "notifCreateChannel");
    loadBlockTranslation(context, "notifSetChannel");
    loadBlockTranslation(context, "notifSetTitle");
    loadBlockTranslation(context, "notifSetContent");
    loadBlockTranslation(context, "notifSetSmallIcon");
    loadBlockTranslation(context, "notifSetAutoCancel");
    loadBlockTranslation(context, "notifSetPriority");
    loadBlockTranslation(context, "notifSetClickIntent");
    loadBlockTranslation(context, "notifShow");
    loadBlockTranslation(context, "notifCancel");
  }
  
  public HashMap<String, String> loadTranslationsFromFile(String filePath) {
    if (!new File(filePath).exists())
      return new HashMap<>();
    try {
      return parseXmlTranslations(filePath);
    } catch (Exception exception) {
      return new HashMap<>();
    }
  }
  
  public void loadBlockTranslation(Context context, String blockName) {
    String translationKey = "block_" + BlockSpecRegistry.getBlockSpec(blockName);
    ArrayList<String> blockParams = BlockSpecRegistry.getBlockParams(blockName);
    eventTranslations.put(blockName, getBlockTranslation(context, translationKey, blockParams));
  }
  
  public boolean reloadTranslations(Context context) {
    if (blockTranslations != null)
      blockTranslations.clear();
    blockTranslations = loadTranslationsFromFile(translationDir);
    loadEventTranslations(context);
    return !blockTranslations.isEmpty() && isLoaded;
  }
  
  public void loadEventSpecTranslation(Context context, String eventName) {
    String translationKey = "root_spec_" + BlockSpecRegistry.getEventSpec(eventName);
    ArrayList<String> menuItems = BlockSpecRegistry.getBlockMenuItems(eventName);
    eventTranslations.put(eventName, getBlockTranslation(context, translationKey, menuItems));
  }
}
