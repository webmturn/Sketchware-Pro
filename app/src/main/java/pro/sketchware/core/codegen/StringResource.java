package pro.sketchware.core.codegen;

import android.content.Context;
import android.content.res.Resources;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import pro.sketchware.R;
import pro.sketchware.core.block.BlockSpecRegistry;
import pro.sketchware.util.locale.LanguageOverrideManager;

public final class StringResource {
    private static final StringResource INSTANCE = new StringResource();

    private final LanguageOverrideManager translations = LanguageOverrideManager.getInstance();
    private final HashMap<String, String> eventTranslations = new HashMap<>();
    private String translationLocales = "";
    private long overrideRevision = -1;

    public static StringResource getInstance() {
        return INSTANCE;
    }

    public synchronized String getEventTranslation(Context context, String eventKey) {
        ensureTranslationsLoaded(context);
        return eventTranslations.getOrDefault(eventKey, "");
    }

    private void ensureTranslationsLoaded(Context context) {
        String locales = context.getResources().getConfiguration().getLocales().toLanguageTags();
        long revision = translations.getRevision();
        if (!locales.equals(translationLocales) || revision != overrideRevision) {
            eventTranslations.clear();
            loadEventTranslations(context);
            translationLocales = locales;
            overrideRevision = revision;
        }
    }

    private static final Set<String> KNOWN_ROOT_SPEC_EVENTS =
            new HashSet<>(
                    Arrays.asList(
                            "onClick",
                            "onCheckedChange",
                            "onItemSelected",
                            "onItemClicked",
                            "onItemLongClicked",
                            "onTextChanged",
                            "onPageStarted",
                            "onPageFinished",
                            "onProgressChanged",
                            "onStartTrackingTouch",
                            "onStopTrackingTouch",
                            "onAnimationStart",
                            "onAnimationEnd",
                            "onAnimationCancel",
                            "onBindCustomView",
                            "onDateChange",
                            "onChildAdded",
                            "onChildChanged",
                            "onChildRemoved",
                            "onCancelled",
                            "onUploadProgress",
                            "onDownloadProgress",
                            "onUploadSuccess",
                            "onDownloadSuccess",
                            "onDeleteSuccess",
                            "onFailure",
                            "onPictureTaken",
                            "onPictureTakenCancel",
                            "onFilesPicked",
                            "onFilesPickedCancel",
                            "onResponse",
                            "onErrorResponse",
                            "onSpeechResult",
                            "onSpeechError",
                            "onConnected",
                            "onDataReceived",
                            "onDataSent",
                            "onConnectionError",
                            "onConnectionStopped",
                            "onMapReady",
                            "onMarkerClicked",
                            "onLocationChanged"));

    public synchronized String getRootSpecTranslation(
            Context context, String blockType, String eventName) {
        ensureTranslationsLoaded(context);

        if (!KNOWN_ROOT_SPEC_EVENTS.contains(eventName)) {
            String result = eventTranslations.get(eventName);
            if (result == null) result = ManageEvent.getExtraEventSpec(blockType, eventName);
            return result;
        }

        String translation = eventTranslations.get(eventName);
        if (translation == null) translation = "";
        return translations.getString(context, R.string.root_spec_common_when)
                + " "
                + blockType
                + " "
                + translation;
    }

    private String getBlockTranslation(Context context, String prefix, ArrayList<String> params) {
        StringBuilder spec = new StringBuilder();
        appendSpecPart(spec, getSpecPart(context, prefix + "_head"));
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) appendSpecPart(spec, getSpecPart(context, prefix + "_body_" + i));
            appendSpecPart(spec, params.get(i));
        }
        appendSpecPart(spec, getSpecPart(context, prefix + "_tail"));
        return spec.toString();
    }

    private static void appendSpecPart(StringBuilder spec, String part) {
        if (part.isEmpty()) return;
        if (spec.length() > 0) spec.append(' ');
        spec.append(part);
    }

    private String getSpecPart(Context context, String key) {
        Resources resources = context.getResources();
        int id = resources.getIdentifier(key, "string", context.getPackageName());
        return id == 0 ? "" : translations.getString(context, id);
    }

    private void loadEventTranslations(Context context) {
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
        loadBlockTranslation(context, "continue");
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
        loadBlockTranslation(context, "calendarViewSetMaxDate");
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
        loadBlockTranslation(context, "requestnetworkUploadFile");
        loadBlockTranslation(context, "googleSignInInit");
        loadBlockTranslation(context, "googleSignInLaunch");
        loadBlockTranslation(context, "googleSignOut");
        loadBlockTranslation(context, "phoneAuthSendCode");
        loadBlockTranslation(context, "phoneAuthResendCode");
        loadBlockTranslation(context, "phoneAuthSignIn");
        loadBlockTranslation(context, "fcmGetToken");
        loadBlockTranslation(context, "fcmSubscribeTopic");
        loadBlockTranslation(context, "fcmUnsubscribeTopic");
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
        loadBlockTranslation(context, "sqliteOpen");
        loadBlockTranslation(context, "sqliteClose");
        loadBlockTranslation(context, "sqliteExecSQL");
        loadBlockTranslation(context, "sqliteRawQuery");
        loadBlockTranslation(context, "sqliteMoveToFirst");
        loadBlockTranslation(context, "sqliteMoveToNext");
        loadBlockTranslation(context, "sqliteIsAfterLast");
        loadBlockTranslation(context, "sqliteGetString");
        loadBlockTranslation(context, "sqliteGetNumber");
        loadBlockTranslation(context, "sqliteGetCount");
        loadBlockTranslation(context, "sqliteCloseCursor");
        loadBlockTranslation(context, "sqliteIsOpen");
        loadBlockTranslation(context, "sqliteBeginTransaction");
        loadBlockTranslation(context, "sqliteSetTransactionSuccessful");
        loadBlockTranslation(context, "sqliteEndTransaction");
        loadBlockTranslation(context, "sqliteForEachRow");
        loadBlockTranslation(context, "sqliteEnableWAL");
        loadBlockTranslation(context, "sqliteCursorIsNull");
    }

    private void loadBlockTranslation(Context context, String opcode) {
        String prefix = "block_" + BlockSpecRegistry.getBlockSpec(opcode);
        eventTranslations.put(
                opcode,
                getBlockTranslation(context, prefix, BlockSpecRegistry.getBlockParams(opcode)));
    }

    private void loadEventSpecTranslation(Context context, String event) {
        String prefix = "root_spec_" + BlockSpecRegistry.getEventSpec(event);
        eventTranslations.put(
                event,
                getBlockTranslation(context, prefix, BlockSpecRegistry.getBlockMenuItems(event)));
    }
}
