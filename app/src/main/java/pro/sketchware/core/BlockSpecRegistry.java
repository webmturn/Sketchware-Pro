package pro.sketchware.core;

import java.util.ArrayList;
import java.util.HashMap;

public class BlockSpecRegistry {
  private static final HashMap<String, ArrayList<String>> cacheA = new HashMap<>();
  private static final HashMap<String, ArrayList<String>> cacheB = new HashMap<>();
  private static final HashMap<String, String> cacheC = new HashMap<>();
  private static final HashMap<String, String> cacheD = new HashMap<>();
  public static ArrayList<String> getBlockParams(String blockName) {
    if (cacheA.containsKey(blockName)) return cacheA.get(blockName);
    short s = -1;
    ArrayList<String> params = new ArrayList<>();
    switch (blockName.hashCode()) {
      default:
        s = -1;
        break;
      case 2138225950:
        if (blockName.equals("locationManagerRequestLocationUpdates")) {
          s = 320;
          break;
        } 
      case 2130649194:
        if (blockName.equals("bluetoothConnectGetPairedDevices")) {
          s = 318;
          break;
        } 
      case 2127377128:
        if (blockName.equals("mediaplayerGetCurrent")) {
          s = 205;
          break;
        } 
      case 2090189010:
        if (blockName.equals("addListStr")) {
          s = 19;
          break;
        } 
      case 2090182653:
        if (blockName.equals("addListMap")) {
          s = 24;
          break;
        } 
      case 2090179216:
        if (blockName.equals("addListInt")) {
          s = 14;
          break;
        } 
      case 2075310296:
        if (blockName.equals("interstitialadLoadAd")) {
          s = 245;
          break;
        } 
      case 2017929665:
        if (blockName.equals("calendarViewSetMinDate")) {
          s = 158;
          break;
        } 
      case 1984984239:
        if (blockName.equals("setText")) {
          s = 108;
          break;
        } 
      case 1984630281:
        if (blockName.equals("setHint")) {
          s = 114;
          break;
        } 
      case 1976325370:
        if (blockName.equals("setImageFilePath")) {
          s = 119;
          break;
        } 
      case 1974249461:
        if (blockName.equals("skewBitmapFile")) {
          s = 294;
          break;
        } 
      case 1973523807:
        if (blockName.equals("mediaplayerIsPlaying")) {
          s = 209;
          break;
        } 
      case 1964823036:
        if (blockName.equals("bluetoothConnectStopConnection")) {
          s = 313;
          break;
        } 
      case 1948735400:
        if (blockName.equals("getAlpha")) {
          s = 124;
          break;
        } 
      case 1941634330:
        if (blockName.equals("firebaseAdd")) {
          s = 227;
          break;
        } 
      case 1923980937:
        if (blockName.equals("requestnetworkSetParams")) {
          s = 252;
          break;
        } 
      case 1908582864:
        if (blockName.equals("firebaseStopListen")) {
          s = 241;
          break;
        } 
      case 1908132964:
        if (blockName.equals("mapViewSetMarkerPosition")) {
          s = 168;
          break;
        } 
      case 1885231494:
        if (blockName.equals("webViewCanGoForward")) {
          s = 148;
          break;
        } 
      case 1883337723:
        if (blockName.equals("mathGetDisplayHeight")) {
          s = 78;
          break;
        } 
      case 1873103950:
        if (blockName.equals("locationManagerRemoveUpdates")) {
          s = 321;
          break;
        } 
      case 1848365301:
        if (blockName.equals("mapViewSetMapType")) {
          s = 161;
          break;
        } 
      case 1823151876:
        if (blockName.equals("fileGetData")) {
          s = 180;
          break;
        } 
      case 1820536363:
        if (blockName.equals("interstitialadCreate")) {
          s = 244;
          break;
        } 
      case 1814870108:
        if (blockName.equals("doToast")) {
          s = 279;
          break;
        } 
      case 1792552710:
        if (blockName.equals("rotateBitmapFile")) {
          s = 292;
          break;
        } 
      case 1779174257:
        if (blockName.equals("getChecked")) {
          s = 136;
          break;
        } 
      case 1775620400:
        if (blockName.equals("strToMap")) {
          s = 72;
          break;
        } 
      case 1764351209:
        if (blockName.equals("deleteList")) {
          s = 32;
          break;
        } 
      case 1749552744:
        if (blockName.equals("textToSpeechSpeak")) {
          s = 302;
          break;
        } 
      case 1712613410:
        if (blockName.equals("webViewZoomOut")) {
          s = 155;
          break;
        } 
      case 1695890133:
        if (blockName.equals("fileutilStartsWith")) {
          s = 266;
          break;
        } 
      case 1637498582:
        if (blockName.equals("timerEvery")) {
          s = 192;
          break;
        } 
      case 1635356258:
        if (blockName.equals("requestnetworkStartRequestNetwork")) {
          s = 254;
          break;
        } 
      case 1633341847:
        if (blockName.equals("timerAfter")) {
          s = 191;
          break;
        } 
      case 1601394299:
        if (blockName.equals("listGetCheckedPositions")) {
          s = 275;
          break;
        } 
      case 1498864168:
        if (blockName.equals("seekBarGetProgress")) {
          s = 283;
          break;
        } 
      case 1470831563:
        if (blockName.equals("intentGetString")) {
          s = 178;
          break;
        } 
      case 1437288110:
        if (blockName.equals("getPublicDir")) {
          s = 271;
          break;
        } 
      case 1431171391:
        if (blockName.equals("mapRemoveKey")) {
          s = 9;
          break;
        } 
      case 1410284340:
        if (blockName.equals("seekBarSetProgress")) {
          s = 282;
          break;
        } 
      case 1405084438:
        if (blockName.equals("setTitle")) {
          s = 281;
          break;
        } 
      case 1397501021:
        if (blockName.equals("listRefresh")) {
          s = 272;
          break;
        } 
      case 1395026457:
        if (blockName.equals("setImage")) {
          s = 116;
          break;
        } 
      case 1387622940:
        if (blockName.equals("setAlpha")) {
          s = 123;
          break;
        } 
      case 1348133645:
        if (blockName.equals("stringReplaceFirst")) {
          s = 63;
          break;
        } 
      case 1343794064:
        if (blockName.equals("listSetItemChecked")) {
          s = 273;
          break;
        } 
      case 1330354473:
        if (blockName.equals("firebaseauthSignInAnonymously")) {
          s = 234;
          break;
        } 
      case 1315302372:
        if (blockName.equals("fileutillength")) {
          s = 265;
          break;
        } 
      case 1313527577:
        if (blockName.equals("setTypeface")) {
          s = 109;
          break;
        } 
      case 1311764810:
        if (blockName.equals("setTranslationY")) {
          s = 127;
          break;
        } 
      case 1311764809:
        if (blockName.equals("setTranslationX")) {
          s = 125;
          break;
        } 
      case 1305932583:
        if (blockName.equals("spnGetSelection")) {
          s = 143;
          break;
        } 
      case 1303367340:
        if (blockName.equals("textToSpeechStop")) {
          s = 304;
          break;
        } 
      case 1280029577:
        if (blockName.equals("requestFocus")) {
          s = 118;
          break;
        } 
      case 1252547704:
        if (blockName.equals("listMapToStr")) {
          s = 75;
          break;
        } 
      case 1242107556:
        if (blockName.equals("fileutilisfile")) {
          s = 264;
          break;
        } 
      case 1240510514:
        if (blockName.equals("intentSetScreen")) {
          s = 174;
          break;
        } 
      case 1236956449:
        if (blockName.equals("mediaplayerCreate")) {
          s = 201;
          break;
        } 
      case 1220078450:
        if (blockName.equals("addSourceDirectly")) {
          s = 286;
          break;
        } 
      case 1219299503:
        if (blockName.equals("objectanimatorIsRunning")) {
          s = 226;
          break;
        } 
      case 1219071185:
        if (blockName.equals("firebasestorageUploadFile")) {
          s = 247;
          break;
        } 
      case 1216249183:
        if (blockName.equals("firebasestorageDelete")) {
          s = 249;
          break;
        } 
      case 1187505507:
        if (blockName.equals("stringReplace")) {
          s = 62;
          break;
        } 
      case 1179719371:
        if (blockName.equals("stringLastIndex")) {
          s = 58;
          break;
        } 
      case 1162069698:
        if (blockName.equals("setThumbResource")) {
          s = 137;
          break;
        } 
      case 1160674468:
        if (blockName.equals("lengthList")) {
          s = 33;
          break;
        } 
      case 1159035162:
        if (blockName.equals("mapViewZoomOut")) {
          s = 165;
          break;
        } 
      case 1156598140:
        if (blockName.equals("fileutilEndsWith")) {
          s = 267;
          break;
        } 
      case 1142897724:
        if (blockName.equals("firebaseauthSignInUser")) {
          s = 233;
          break;
        } 
      case 1129709718:
        if (blockName.equals("setImageUrl")) {
          s = 120;
          break;
        } 
      case 1102670563:
        if (blockName.equals("requestnetworkSetHeaders")) {
          s = 253;
          break;
        } 
      case 1090517587:
        if (blockName.equals("getPackageDataDir")) {
          s = 270;
          break;
        } 
      case 1088879149:
        if (blockName.equals("setHintTextColor")) {
          s = 115;
          break;
        } 
      case 1086207657:
        if (blockName.equals("fileutildelete")) {
          s = 259;
          break;
        } 
      case 1068548733:
        if (blockName.equals("mathGetDip")) {
          s = 76;
          break;
        } 
      case 1053179400:
        if (blockName.equals("mapViewSetMarkerColor")) {
          s = 169;
          break;
        } 
      case 950609198:
        if (blockName.equals("setBitmapFileColorFilter")) {
          s = 295;
          break;
        } 
      case 948234497:
        if (blockName.equals("webViewStopLoading")) {
          s = 153;
          break;
        } 
      case 937017988:
        if (blockName.equals("gyroscopeStartListen")) {
          s = 242;
          break;
        } 
      case 932259189:
        if (blockName.equals("setBgResource")) {
          s = 112;
          break;
        } 
      case 898187172:
        if (blockName.equals("mathToRadian")) {
          s = 98;
          break;
        } 
      case 858248741:
        if (blockName.equals("calendarGetTime")) {
          s = 188;
          break;
        } 
      case 848786445:
        if (blockName.equals("objectanimatorSetTarget")) {
          s = 216;
          break;
        } 
      case 845089750:
        if (blockName.equals("setVarString")) {
          s = 4;
          break;
        } 
      case 840991609:
        if (blockName.equals("mathTan")) {
          s = 91;
          break;
        } 
      case 840990896:
        if (blockName.equals("mathSin")) {
          s = 89;
          break;
        } 
      case 840988208:
        if (blockName.equals("mathPow")) {
          s = 81;
          break;
        } 
      case 840985130:
        if (blockName.equals("mathMin")) {
          s = 82;
          break;
        } 
      case 840984892:
        if (blockName.equals("mathMax")) {
          s = 83;
          break;
        } 
      case 840984348:
        if (blockName.equals("mathLog")) {
          s = 96;
          break;
        } 
      case 840977909:
        if (blockName.equals("mathExp")) {
          s = 95;
          break;
        } 
      case 840975711:
        if (blockName.equals("mathCos")) {
          s = 90;
          break;
        } 
      case 840973386:
        if (blockName.equals("mathAbs")) {
          s = 85;
          break;
        } 
      case 836692861:
        if (blockName.equals("mapSize")) {
          s = 10;
          break;
        } 
      case 797861524:
        if (blockName.equals("addMapToList")) {
          s = 29;
          break;
        } 
      case 787825477:
        if (blockName.equals("getScaleY")) {
          s = 132;
          break;
        } 
      case 787825476:
        if (blockName.equals("getScaleX")) {
          s = 130;
          break;
        } 
      case 770834513:
        if (blockName.equals("getRotate")) {
          s = 122;
          break;
        } 
      case 762292097:
        if (blockName.equals("indexListStr")) {
          s = 22;
          break;
        } 
      case 762282303:
        if (blockName.equals("indexListInt")) {
          s = 17;
          break;
        } 
      case 754442829:
        if (blockName.equals("increaseInt")) {
          s = 2;
          break;
        } 
      case 747168008:
        if (blockName.equals("mapCreateNew")) {
          s = 5;
          break;
        } 
      case 738846120:
        if (blockName.equals("textToSpeechSetPitch")) {
          s = 300;
          break;
        } 
      case 737664870:
        if (blockName.equals("mathRound")) {
          s = 86;
          break;
        } 
      case 732108347:
        if (blockName.equals("mathLog10")) {
          s = 97;
          break;
        } 
      case 726887785:
        if (blockName.equals("mapViewSetMarkerInfo")) {
          s = 167;
          break;
        } 
      case 726877492:
        if (blockName.equals("mapViewSetMarkerIcon")) {
          s = 170;
          break;
        } 
      case 726487524:
        if (blockName.equals("mathFloor")) {
          s = 88;
          break;
        } 
      case 725249532:
        if (blockName.equals("intentSetAction")) {
          s = 172;
          break;
        } 
      case 683193060:
        if (blockName.equals("bluetoothConnectStartConnection")) {
          s = 311;
          break;
        } 
      case 657721930:
        if (blockName.equals("setVarInt")) {
          s = 1;
          break;
        } 
      case 615286641:
        if (blockName.equals("dialogNeutralButton")) {
          s = 200;
          break;
        } 
      case 610313513:
        if (blockName.equals("getMapInList")) {
          s = 31;
          break;
        } 
      case 573295520:
        if (blockName.equals("listGetCheckedCount")) {
          s = 276;
          break;
        } 
      case 573208401:
        if (blockName.equals("setScaleY")) {
          s = 131;
          break;
        } 
      case 573208400:
        if (blockName.equals("setScaleX")) {
          s = 129;
          break;
        } 
      case 571046965:
        if (blockName.equals("scaleBitmapFile")) {
          s = 293;
          break;
        } 
      case 556217437:
        if (blockName.equals("setRotate")) {
          s = 121;
          break;
        } 
      case 548860462:
        if (blockName.equals("webViewClearCache")) {
          s = 151;
          break;
        } 
      case 530759231:
        if (blockName.equals("progressBarSetIndeterminate")) {
          s = 299;
          break;
        } 
      case 501171279:
        if (blockName.equals("mathToDegree")) {
          s = 99;
          break;
        } 
      case 490702942:
        if (blockName.equals("filepickerstartpickfiles")) {
          s = 251;
          break;
        } 
      case 481850295:
        if (blockName.equals("resizeBitmapFileToSquare")) {
          s = 288;
          break;
        } 
      case 475815924:
        if (blockName.equals("setTextColor")) {
          s = 113;
          break;
        } 
      case 470160234:
        if (blockName.equals("fileutilGetLastSegmentPath")) {
          s = 268;
          break;
        } 
      case 463594049:
        if (blockName.equals("objectanimatorSetFromTo")) {
          s = 219;
          break;
        } 
      case 463560551:
        if (blockName.equals("mapContainKey")) {
          s = 8;
          break;
        } 
      case 442768763:
        if (blockName.equals("mapGetAllKeys")) {
          s = 13;
          break;
        } 
      case 404265028:
        if (blockName.equals("calendarSet")) {
          s = 185;
          break;
        } 
      case 404247683:
        if (blockName.equals("calendarAdd")) {
          s = 184;
          break;
        } 
      case 401012286:
        if (blockName.equals("getTranslationY")) {
          s = 128;
          break;
        } 
      case 401012285:
        if (blockName.equals("getTranslationX")) {
          s = 126;
          break;
        } 
      case 397166713:
        if (blockName.equals("getEnable")) {
          s = 105;
          break;
        } 
      case 389111867:
        if (blockName.equals("spnSetData")) {
          s = 141;
          break;
        } 
      case 348475309:
        if (blockName.equals("soundpoolStreamStop")) {
          s = 215;
          break;
        } 
      case 348377823:
        if (blockName.equals("soundpoolStreamPlay")) {
          s = 214;
          break;
        } 
      case 342026220:
        if (blockName.equals("interstitialadShow")) {
          s = 246;
          break;
        } 
      case 317453636:
        if (blockName.equals("textToSpeechIsSpeaking")) {
          s = 303;
          break;
        } 
      case 300921928:
        if (blockName.equals("mathSqrt")) {
          s = 84;
          break;
        } 
      case 300433453:
        if (blockName.equals("mathCeil")) {
          s = 87;
          break;
        } 
      case 300388040:
        if (blockName.equals("mathAtan")) {
          s = 94;
          break;
        } 
      case 300387327:
        if (blockName.equals("mathAsin")) {
          s = 92;
          break;
        } 
      case 300372142:
        if (blockName.equals("mathAcos")) {
          s = 93;
          break;
        } 
      case 297379706:
        if (blockName.equals("textToSpeechSetSpeechRate")) {
          s = 301;
          break;
        } 
      case 276674391:
        if (blockName.equals("mapViewMoveCamera")) {
          s = 162;
          break;
        } 
      case 262073061:
        if (blockName.equals("bluetoothConnectReadyConnection")) {
          s = 309;
          break;
        } 
      case 255417137:
        if (blockName.equals("adViewLoadAd")) {
          s = 160;
          break;
        } 
      case 207764385:
        if (blockName.equals("calendarViewGetDate")) {
          s = 156;
          break;
        } 
      case 182549637:
        if (blockName.equals("setEnable")) {
          s = 104;
          break;
        } 
      case 168740282:
        if (blockName.equals("mapToStr")) {
          s = 73;
          break;
        } 
      case 163812602:
        if (blockName.equals("cropBitmapFileFromCenter")) {
          s = 291;
          break;
        } 
      case 152967761:
        if (blockName.equals("mapClear")) {
          s = 11;
          break;
        } 
      case 134874756:
        if (blockName.equals("listSetCustomViewData")) {
          s = 140;
          break;
        } 
      case 125431087:
        if (blockName.equals("speechToTextStopListening")) {
          s = 307;
          break;
        } 
      case 103668285:
        if (blockName.equals("mathE")) {
          s = 80;
          break;
        } 
      case 97196323:
        if (blockName.equals("false")) {
          s = 42;
          break;
        } 
      case 94001407:
        if (blockName.equals("break")) {
          s = 37;
          break;
        } 
      case 61585857:
        if (blockName.equals("firebasePush")) {
          s = 228;
          break;
        } 
      case 56167279:
        if (blockName.equals("setBitmapFileContrast")) {
          s = 297;
          break;
        } 
      case 27679870:
        if (blockName.equals("calendarGetNow")) {
          s = 183;
          break;
        } 
      case 25469951:
        if (blockName.equals("bluetoothConnectActivateBluetooth")) {
          s = 317;
          break;
        } 
      case 16308074:
        if (blockName.equals("resizeBitmapFileToCircle")) {
          s = 289;
          break;
        } 
      case 8255701:
        if (blockName.equals("calendarFormat")) {
          s = 186;
          break;
        } 
      case 3569038:
        if (blockName.equals("true")) {
          s = 41;
          break;
        } 
      case 3568674:
        if (blockName.equals("trim")) {
          s = 66;
          break;
        } 
      case 3116345:
        if (blockName.equals("else")) {
          s = 40;
          break;
        } 
      case 109267:
        if (blockName.equals("not")) {
          s = 48;
          break;
        } 
      case 3968:
        if (blockName.equals("||")) {
          s = 47;
          break;
        } 
      case 3357:
        if (blockName.equals("if")) {
          s = 38;
          break;
        } 
      case 1216:
        if (blockName.equals("&&")) {
          s = 46;
          break;
        } 
      case 62:
        if (blockName.equals(">")) {
          s = 45;
          break;
        } 
      case 61:
        if (blockName.equals("=")) {
          s = 44;
          break;
        } 
      case 60:
        if (blockName.equals("<")) {
          s = 43;
          break;
        } 
      case 47:
        if (blockName.equals("/")) {
          s = 52;
          break;
        } 
      case 45:
        if (blockName.equals("-")) {
          s = 50;
          break;
        } 
      case 43:
        if (blockName.equals("+")) {
          s = 49;
          break;
        } 
      case 42:
        if (blockName.equals("*")) {
          s = 51;
          break;
        } 
      case 37:
        if (blockName.equals("%")) {
          s = 53;
          break;
        } 
      case -9742826:
        if (blockName.equals("firebaseGetPushKey")) {
          s = 229;
          break;
        } 
      case -10599306:
        if (blockName.equals("firebaseauthCreateUser")) {
          s = 232;
          break;
        } 
      case -14362103:
        if (blockName.equals("bluetoothConnectIsBluetoothActivated")) {
          s = 316;
          break;
        } 
      case -24451690:
        if (blockName.equals("dialogOkButton")) {
          s = 198;
          break;
        } 
      case -60048101:
        if (blockName.equals("firebaseauthResetPassword")) {
          s = 235;
          break;
        } 
      case -60494417:
        if (blockName.equals("vibratorAction")) {
          s = 190;
          break;
        } 
      case -75125341:
        if (blockName.equals("getText")) {
          s = 110;
          break;
        } 
      case -83186725:
        if (blockName.equals("openDrawer")) {
          s = 102;
          break;
        } 
      case -83301935:
        if (blockName.equals("webViewZoomIn")) {
          s = 154;
          break;
        } 
      case -96303809:
        if (blockName.equals("containListStr")) {
          s = 23;
          break;
        } 
      case -96310166:
        if (blockName.equals("containListMap")) {
          s = 28;
          break;
        } 
      case -96313603:
        if (blockName.equals("containListInt")) {
          s = 18;
          break;
        } 
      case -133532073:
        if (blockName.equals("stringLength")) {
          s = 55;
          break;
        } 
      case -149850417:
        if (blockName.equals("fileutilisexist")) {
          s = 260;
          break;
        } 
      case -152473824:
        if (blockName.equals("firebaseauthIsLoggedIn")) {
          s = 236;
          break;
        } 
      case -189292433:
        if (blockName.equals("stringSub")) {
          s = 59;
          break;
        } 
      case -208762465:
        if (blockName.equals("toStringWithDecimal")) {
          s = 70;
          break;
        } 
      case -247015294:
        if (blockName.equals("mediaplayerRelease")) {
          s = 208;
          break;
        } 
      case -258774775:
        if (blockName.equals("closeDrawer")) {
          s = 103;
          break;
        } 
      case -283328259:
        if (blockName.equals("intentPutExtra")) {
          s = 175;
          break;
        } 
      case -322651344:
        if (blockName.equals("stringEquals")) {
          s = 60;
          break;
        } 
      case -329552966:
        if (blockName.equals("insertListStr")) {
          s = 20;
          break;
        } 
      case -329559323:
        if (blockName.equals("insertListMap")) {
          s = 25;
          break;
        } 
      case -329562760:
        if (blockName.equals("insertListInt")) {
          s = 15;
          break;
        } 
      case -353129373:
        if (blockName.equals("calendarDiff")) {
          s = 187;
          break;
        } 
      case -356866884:
        if (blockName.equals("webViewSetCacheMode")) {
          s = 146;
          break;
        } 
      case -390304998:
        if (blockName.equals("mapViewAddMarker")) {
          s = 166;
          break;
        } 
      case -399551817:
        if (blockName.equals("toUpperCase")) {
          s = 67;
          break;
        } 
      case -411705840:
        if (blockName.equals("fileSetData")) {
          s = 181;
          break;
        } 
      case -418212114:
        if (blockName.equals("firebaseGetChildren")) {
          s = 231;
          break;
        } 
      case -425293664:
        if (blockName.equals("setClickable")) {
          s = 107;
          break;
        } 
      case -437272040:
        if (blockName.equals("bluetoothConnectGetRandomUuid")) {
          s = 319;
          break;
        } 
      case -439342016:
        if (blockName.equals("webViewClearHistory")) {
          s = 152;
          break;
        } 
      case -509946902:
        if (blockName.equals("spnRefresh")) {
          s = 278;
          break;
        } 
      case -578987803:
        if (blockName.equals("setChecked")) {
          s = 135;
          break;
        } 
      case -601804268:
        if (blockName.equals("fileutilread")) {
          s = 255;
          break;
        } 
      case -601942961:
        if (blockName.equals("fileutilmove")) {
          s = 258;
          break;
        } 
      case -602241037:
        if (blockName.equals("fileutilcopy")) {
          s = 257;
          break;
        } 
      case -621198621:
        if (blockName.equals("speechToTextStartListening")) {
          s = 306;
          break;
        } 
      case -628607128:
        if (blockName.equals("webViewGoBack")) {
          s = 149;
          break;
        } 
      case -636363854:
        if (blockName.equals("webViewGetUrl")) {
          s = 145;
          break;
        } 
      case -649691581:
        if (blockName.equals("objectanimatorSetInterpolator")) {
          s = 223;
          break;
        } 
      case -664474111:
        if (blockName.equals("intentSetFlags")) {
          s = 176;
          break;
        } 
      case -668992194:
        if (blockName.equals("stringReplaceAll")) {
          s = 64;
          break;
        } 
      case -677662361:
        if (blockName.equals("forever")) {
          s = 36;
          break;
        } 
      case -697616870:
        if (blockName.equals("camerastarttakepicture")) {
          s = 250;
          break;
        } 
      case -733318734:
        if (blockName.equals("strToListMap")) {
          s = 74;
          break;
        } 
      case -831887360:
        if (blockName.equals("textToSpeechShutdown")) {
          s = 305;
          break;
        } 
      case -853550561:
        if (blockName.equals("timerCancel")) {
          s = 193;
          break;
        } 
      case -854558288:
        if (blockName.equals("setVisible")) {
          s = 106;
          break;
        } 
      case -869293886:
        if (blockName.equals("finishActivity")) {
          s = 179;
          break;
        } 
      case -883988307:
        if (blockName.equals("dialogSetMessage")) {
          s = 195;
          break;
        } 
      case -903177036:
        if (blockName.equals("resizeBitmapFileWithRoundedBorder")) {
          s = 290;
          break;
        } 
      case -911199919:
        if (blockName.equals("objectanimatorSetProperty")) {
          s = 217;
          break;
        } 
      case -917343271:
        if (blockName.equals("getJpegRotate")) {
          s = 298;
          break;
        } 
      case -918173448:
        if (blockName.equals("listGetCheckedPosition")) {
          s = 274;
          break;
        } 
      case -934531685:
        if (blockName.equals("repeat")) {
          s = 35;
          break;
        } 
      case -938285885:
        if (blockName.equals("random")) {
          s = 54;
          break;
        } 
      case -995908985:
        if (blockName.equals("soundpoolCreate")) {
          s = 212;
          break;
        } 
      case -996870276:
        if (blockName.equals("insertMapToList")) {
          s = 30;
          break;
        } 
      case -1007787615:
        if (blockName.equals("mediaplayerSetLooping")) {
          s = 210;
          break;
        } 
      case -1021852352:
        if (blockName.equals("objectanimatorCancel")) {
          s = 225;
          break;
        } 
      case -1033658254:
        if (blockName.equals("mathGetDisplayWidth")) {
          s = 77;
          break;
        } 
      case -1043233275:
        if (blockName.equals("mediaplayerGetDuration")) {
          s = 206;
          break;
        } 
      case -1063598745:
        if (blockName.equals("resizeBitmapFileRetainRatio")) {
          s = 287;
          break;
        } 
      case -1081250015:
        if (blockName.equals("mathPi")) {
          s = 79;
          break;
        } 
      case -1081391085:
        if (blockName.equals("mapPut")) {
          s = 6;
          break;
        } 
      case -1081400230:
        if (blockName.equals("mapGet")) {
          s = 7;
          break;
        } 
      case -1094491139:
        if (blockName.equals("seekBarSetMax")) {
          s = 284;
          break;
        } 
      case -1106141754:
        if (blockName.equals("webViewCanGoBack")) {
          s = 147;
          break;
        } 
      case -1107376988:
        if (blockName.equals("webViewGoForward")) {
          s = 150;
          break;
        } 
      case -1123431291:
        if (blockName.equals("calnedarViewSetMaxDate")) {
          s = 159;
          break;
        } 
      case -1137582698:
        if (blockName.equals("toLowerCase")) {
          s = 68;
          break;
        } 
      case -1139353316:
        if (blockName.equals("setListMap")) {
          s = 27;
          break;
        } 
      case -1143684675:
        if (blockName.equals("firebaseauthGetCurrentUser")) {
          s = 237;
          break;
        } 
      case -1149458632:
        if (blockName.equals("objectanimatorSetRepeatCount")) {
          s = 222;
          break;
        } 
      case -1149848189:
        if (blockName.equals("toStringFormat")) {
          s = 71;
          break;
        } 
      case -1160374245:
        if (blockName.equals("bluetoothConnectReadyConnectionToUuid")) {
          s = 310;
          break;
        } 
      case -1182878167:
        if (blockName.equals("firebaseauthGetUid")) {
          s = 238;
          break;
        } 
      case -1185284274:
        if (blockName.equals("gyroscopeStopListen")) {
          s = 243;
          break;
        } 
      case -1192544266:
        if (blockName.equals("ifElse")) {
          s = 39;
          break;
        } 
      case -1195899442:
        if (blockName.equals("bluetoothConnectSendData")) {
          s = 314;
          break;
        } 
      case -1206794098:
        if (blockName.equals("getLocationY")) {
          s = 134;
          break;
        } 
      case -1206794099:
        if (blockName.equals("getLocationX")) {
          s = 133;
          break;
        } 
      case -1217704075:
        if (blockName.equals("objectanimatorSetValue")) {
          s = 218;
          break;
        } 
      case -1271141237:
        if (blockName.equals("clearList")) {
          s = 34;
          break;
        } 
      case -1272546178:
        if (blockName.equals("dialogSetTitle")) {
          s = 194;
          break;
        } 
      case -1304067438:
        if (blockName.equals("firebaseDelete")) {
          s = 230;
          break;
        } 
      case -1348084945:
        if (blockName.equals("mapViewZoomTo")) {
          s = 163;
          break;
        } 
      case -1348085287:
        if (blockName.equals("mapViewZoomIn")) {
          s = 164;
          break;
        } 
      case -1361468284:
        if (blockName.equals("viewOnClick")) {
          s = 100;
          break;
        } 
      case -1376608975:
        if (blockName.equals("calendarSetTime")) {
          s = 189;
          break;
        } 
      case -1377080719:
        if (blockName.equals("decreaseInt")) {
          s = 3;
          break;
        } 
      case -1384851894:
        if (blockName.equals("getAtListStr")) {
          s = 21;
          break;
        } 
      case -1384858251:
        if (blockName.equals("getAtListMap")) {
          s = 26;
          break;
        } 
      case -1384861688:
        if (blockName.equals("getAtListInt")) {
          s = 16;
          break;
        } 
      case -1385076635:
        if (blockName.equals("dialogShow")) {
          s = 196;
          break;
        } 
      case -1405157727:
        if (blockName.equals("fileutilmakedir")) {
          s = 261;
          break;
        } 
      case -1422112391:
        if (blockName.equals("bluetoothConnectIsBluetoothEnabled")) {
          s = 315;
          break;
        } 
      case -1438040951:
        if (blockName.equals("seekBarGetMax")) {
          s = 285;
          break;
        } 
      case -1440042085:
        if (blockName.equals("spnSetSelection")) {
          s = 142;
          break;
        } 
      case -1462744030:
        if (blockName.equals("dialogDismiss")) {
          s = 197;
          break;
        } 
      case -1471049951:
        if (blockName.equals("fileutilwrite")) {
          s = 256;
          break;
        } 
      case -1477942289:
        if (blockName.equals("mediaplayerIsLooping")) {
          s = 211;
          break;
        } 
      case -1483954587:
        if (blockName.equals("fileutilisdir")) {
          s = 263;
          break;
        } 
      case -1513446476:
        if (blockName.equals("dialogCancelButton")) {
          s = 199;
          break;
        } 
      case -1526161572:
        if (blockName.equals("setBgColor")) {
          s = 111;
          break;
        } 
      case -1528850031:
        if (blockName.equals("startActivity")) {
          s = 177;
          break;
        } 
      case -1530840255:
        if (blockName.equals("stringIndex")) {
          s = 57;
          break;
        } 
      case -1541653284:
        if (blockName.equals("objectanimatorStart")) {
          s = 224;
          break;
        } 
      case -1573371685:
        if (blockName.equals("stringJoin")) {
          s = 56;
          break;
        } 
      case -1666623936:
        if (blockName.equals("speechToTextShutdown")) {
          s = 308;
          break;
        } 
      case -1679834825:
        if (blockName.equals("setTrackResource")) {
          s = 138;
          break;
        } 
      case -1684072208:
        if (blockName.equals("intentSetData")) {
          s = 173;
          break;
        } 
      case -1699349926:
        if (blockName.equals("objectanimatorSetRepeatMode")) {
          s = 221;
          break;
        } 
      case -1699631195:
        if (blockName.equals("isDrawerOpen")) {
          s = 101;
          break;
        } 
      case -1718917155:
        if (blockName.equals("mediaplayerSeek")) {
          s = 204;
          break;
        } 
      case -1746380899:
        if (blockName.equals("mediaplayerStart")) {
          s = 202;
          break;
        } 
      case -1747734390:
        if (blockName.equals("mediaplayerReset")) {
          s = 207;
          break;
        } 
      case -1749698255:
        if (blockName.equals("mediaplayerPause")) {
          s = 203;
          break;
        } 
      case -1776922004:
        if (blockName.equals("toString")) {
          s = 69;
          break;
        } 
      case -1778201036:
        if (blockName.equals("listSmoothScrollTo")) {
          s = 277;
          break;
        } 
      case -1812313351:
        if (blockName.equals("setColorFilter")) {
          s = 117;
          break;
        } 
      case -1834369666:
        if (blockName.equals("setBitmapFileBrightness")) {
          s = 296;
          break;
        } 
      case -1886802639:
        if (blockName.equals("soundpoolLoad")) {
          s = 213;
          break;
        } 
      case -1910071024:
        if (blockName.equals("objectanimatorSetDuration")) {
          s = 220;
          break;
        } 
      case -1919300188:
        if (blockName.equals("toNumber")) {
          s = 65;
          break;
        } 
      case -1920517885:
        if (blockName.equals("setVarBoolean")) {
          s = 0;
          break;
        } 
      case -1922362317:
        if (blockName.equals("getExternalStorageDir")) {
          s = 269;
          break;
        } 
      case -1937348542:
        if (blockName.equals("firebaseStartListen")) {
          s = 240;
          break;
        } 
      case -1966668787:
        if (blockName.equals("firebaseauthSignOutUser")) {
          s = 239;
          break;
        } 
      case -1975568730:
        if (blockName.equals("copyToClipboard")) {
          s = 280;
          break;
        } 
      case -1979147952:
        if (blockName.equals("stringContains")) {
          s = 61;
          break;
        } 
      case -1989678633:
        if (blockName.equals("mapViewSetMarkerVisible")) {
          s = 171;
          break;
        } 
      case -1998407506:
        if (blockName.equals("listSetData")) {
          s = 139;
          break;
        } 
      case -2020761366:
        if (blockName.equals("fileRemoveData")) {
          s = 182;
          break;
        } 
      case -2027093331:
        if (blockName.equals("calendarViewSetDate")) {
          s = 157;
          break;
        } 
      case -2037144358:
        if (blockName.equals("bluetoothConnectStartConnectionToUuid")) {
          s = 312;
          break;
        } 
      case -2055793167:
        if (blockName.equals("fileutillistdir")) {
          s = 262;
          break;
        } 
      case -2114384168:
        if (blockName.equals("firebasestorageDownloadFile")) {
          s = 248;
          break;
        } 
      case -2120571577:
        if (blockName.equals("mapIsEmpty")) {
          s = 12;
          break;
        } 
      case -2135695280:
        if (blockName.equals("webViewLoadUrl")) {
          s = 144;
          break;
        } 
    } 
    ArrayList<String> result = params;
    switch (s) {
      default:
        result = null;
        break;
      case 321:
        params.add("%m.locationmanager");
        result = params;
        break;
      case 320:
        params.add("%m.locationmanager");
        params.add("%m.providerType");
        params.add("%d");
        params.add("%d");
        result = params;
        break;
      case 319:
        params.add("%m.bluetoothconnect");
        result = params;
        break;
      case 318:
        params.add("%m.bluetoothconnect");
        params.add("%m.listMap");
        result = params;
        break;
      case 317:
        params.add("%m.bluetoothconnect");
        result = params;
        break;
      case 316:
        params.add("%m.bluetoothconnect");
        result = params;
        break;
      case 315:
        params.add("%m.bluetoothconnect");
        result = params;
        break;
      case 314:
        params.add("%m.bluetoothconnect");
        params.add("%s");
        params.add("%s");
        result = params;
        break;
      case 313:
        params.add("%m.bluetoothconnect");
        params.add("%s");
        result = params;
        break;
      case 312:
        params.add("%m.bluetoothconnect");
        params.add("%s");
        params.add("%s");
        params.add("%s");
        result = params;
        break;
      case 311:
        params.add("%m.bluetoothconnect");
        params.add("%s");
        params.add("%s");
        result = params;
        break;
      case 310:
        params.add("%m.bluetoothconnect");
        params.add("%s");
        params.add("%s");
        result = params;
        break;
      case 309:
        params.add("%m.bluetoothconnect");
        params.add("%s");
        result = params;
        break;
      case 308:
        params.add("%m.speechtotext");
        result = params;
        break;
      case 307:
        params.add("%m.speechtotext");
        result = params;
        break;
      case 306:
        params.add("%m.speechtotext");
        result = params;
        break;
      case 305:
        params.add("%m.texttospeech");
        result = params;
        break;
      case 304:
        params.add("%m.texttospeech");
        result = params;
        break;
      case 303:
        params.add("%m.texttospeech");
        result = params;
        break;
      case 302:
        params.add("%m.texttospeech");
        params.add("%s");
        result = params;
        break;
      case 301:
        params.add("%m.texttospeech");
        params.add("%d");
        result = params;
        break;
      case 300:
        params.add("%m.texttospeech");
        params.add("%d");
        result = params;
        break;
      case 299:
        params.add("%m.progressbar");
        params.add("%b");
        result = params;
        break;
      case 298:
        params.add("%s");
        result = params;
        break;
      case 297:
        params.add("%s");
        params.add("%s");
        params.add("%d");
        result = params;
        break;
      case 296:
        params.add("%s");
        params.add("%s");
        params.add("%d");
        result = params;
        break;
      case 295:
        params.add("%s");
        params.add("%s");
        params.add("%m.color");
        result = params;
        break;
      case 294:
        params.add("%s");
        params.add("%s");
        params.add("%d");
        params.add("%d");
        result = params;
        break;
      case 293:
        params.add("%s");
        params.add("%s");
        params.add("%d");
        params.add("%d");
        result = params;
        break;
      case 292:
        params.add("%s");
        params.add("%s");
        params.add("%d");
        result = params;
        break;
      case 291:
        params.add("%s");
        params.add("%s");
        params.add("%d");
        params.add("%d");
        result = params;
        break;
      case 290:
        params.add("%s");
        params.add("%s");
        params.add("%d");
        result = params;
        break;
      case 289:
        params.add("%s");
        params.add("%s");
        result = params;
        break;
      case 288:
        params.add("%s");
        params.add("%s");
        params.add("%d");
        result = params;
        break;
      case 287:
        params.add("%s");
        params.add("%s");
        params.add("%d");
        result = params;
        break;
      case 286:
        params.add("%s.inputOnly");
        result = params;
        break;
      case 285:
        params.add("%m.seekbar");
        result = params;
        break;
      case 284:
        params.add("%m.seekbar");
        params.add("%d");
        result = params;
        break;
      case 283:
        params.add("%m.seekbar");
        result = params;
        break;
      case 282:
        params.add("%m.seekbar");
        params.add("%d");
        result = params;
        break;
      case 281:
        params.add("%s");
        result = params;
        break;
      case 280:
        params.add("%s");
        result = params;
        break;
      case 279:
        params.add("%s");
        result = params;
        break;
      case 278:
        params.add("%m.spinner");
        result = params;
        break;
      case 277:
        params.add("%m.listview");
        params.add("%d");
        result = params;
        break;
      case 276:
        params.add("%m.listview");
        result = params;
        break;
      case 275:
        params.add("%m.listview");
        params.add("%m.listInt");
        result = params;
        break;
      case 274:
        params.add("%m.listview");
        result = params;
        break;
      case 273:
        params.add("%m.listview");
        params.add("%d");
        params.add("%b");
        result = params;
        break;
      case 272:
        params.add("%m.listview");
        result = params;
        break;
      case 271:
        params.add("%m.directoryType");
        result = params;
        break;
      case 268:
        params.add("%s");
        result = params;
        break;
      case 267:
        params.add("%s");
        params.add("%s");
        result = params;
        break;
      case 266:
        params.add("%s");
        params.add("%s");
        result = params;
        break;
      case 265:
        params.add("%s");
        result = params;
        break;
      case 264:
        params.add("%s");
        result = params;
        break;
      case 263:
        params.add("%s");
        result = params;
        break;
      case 262:
        params.add("%s");
        params.add("%m.listStr");
        result = params;
        break;
      case 261:
        params.add("%s");
        result = params;
        break;
      case 260:
        params.add("%s");
        result = params;
        break;
      case 259:
        params.add("%s");
        result = params;
        break;
      case 258:
        params.add("%s");
        params.add("%s");
        result = params;
        break;
      case 257:
        params.add("%s");
        params.add("%s");
        result = params;
        break;
      case 256:
        params.add("%s");
        params.add("%s");
        result = params;
        break;
      case 255:
        params.add("%s");
        result = params;
        break;
      case 254:
        params.add("%m.requestnetwork");
        params.add("%m.method");
        params.add("%s");
        params.add("%s");
        result = params;
        break;
      case 253:
        params.add("%m.requestnetwork");
        params.add("%m.varMap");
        result = params;
        break;
      case 252:
        params.add("%m.requestnetwork");
        params.add("%m.varMap");
        params.add("%m.requestType");
        result = params;
        break;
      case 251:
        params.add("%m.filepicker");
        result = params;
        break;
      case 250:
        params.add("%m.camera");
        result = params;
        break;
      case 249:
        params.add("%m.firebasestorage");
        params.add("%s");
        result = params;
        break;
      case 248:
        params.add("%m.firebasestorage");
        params.add("%s");
        params.add("%s");
        result = params;
        break;
      case 247:
        params.add("%m.firebasestorage");
        params.add("%s");
        params.add("%s");
        result = params;
        break;
      case 246:
        params.add("%m.interstitialad");
        result = params;
        break;
      case 245:
        params.add("%m.interstitialad");
        result = params;
        break;
      case 244:
        params.add("%m.interstitialad");
        result = params;
        break;
      case 243:
        params.add("%m.gyroscope");
        result = params;
        break;
      case 242:
        params.add("%m.gyroscope");
        result = params;
        break;
      case 241:
        params.add("%m.firebase");
        result = params;
        break;
      case 240:
        params.add("%m.firebase");
        result = params;
        break;
      case 235:
        params.add("%m.firebaseauth");
        params.add("%s");
        result = params;
        break;
      case 234:
        params.add("%m.firebaseauth");
        result = params;
        break;
      case 233:
        params.add("%m.firebaseauth");
        params.add("%s");
        params.add("%s");
        result = params;
        break;
      case 232:
        params.add("%m.firebaseauth");
        params.add("%s");
        params.add("%s");
        result = params;
        break;
      case 231:
        params.add("%m.firebase");
        params.add("%m.listMap");
        result = params;
        break;
      case 230:
        params.add("%m.firebase");
        params.add("%s");
        result = params;
        break;
      case 229:
        params.add("%m.firebase");
        result = params;
        break;
      case 228:
        params.add("%m.firebase");
        params.add("%m.varMap");
        result = params;
        break;
      case 227:
        params.add("%m.firebase");
        params.add("%s");
        params.add("%m.varMap");
        result = params;
        break;
      case 226:
        params.add("%m.objectanimator");
        result = params;
        break;
      case 225:
        params.add("%m.objectanimator");
        result = params;
        break;
      case 224:
        params.add("%m.objectanimator");
        result = params;
        break;
      case 223:
        params.add("%m.objectanimator");
        params.add("%m.aniInterpolator");
        result = params;
        break;
      case 222:
        params.add("%m.objectanimator");
        params.add("%d");
        result = params;
        break;
      case 221:
        params.add("%m.objectanimator");
        params.add("%m.aniRepeatMode");
        result = params;
        break;
      case 220:
        params.add("%m.objectanimator");
        params.add("%d");
        result = params;
        break;
      case 219:
        params.add("%m.objectanimator");
        params.add("%d");
        params.add("%d");
        result = params;
        break;
      case 218:
        params.add("%m.objectanimator");
        params.add("%d");
        result = params;
        break;
      case 217:
        params.add("%m.objectanimator");
        params.add("%m.animatorproperty");
        result = params;
        break;
      case 216:
        params.add("%m.objectanimator");
        params.add("%m.view");
        result = params;
        break;
      case 215:
        params.add("%m.soundpool");
        params.add("%d");
        result = params;
        break;
      case 214:
        params.add("%m.soundpool");
        params.add("%d");
        params.add("%d");
        result = params;
        break;
      case 213:
        params.add("%m.soundpool");
        params.add("%m.sound");
        result = params;
        break;
      case 212:
        params.add("%m.soundpool");
        params.add("%d");
        result = params;
        break;
      case 211:
        params.add("%m.mediaplayer");
        result = params;
        break;
      case 210:
        params.add("%m.mediaplayer");
        params.add("%b");
        result = params;
        break;
      case 209:
        params.add("%m.mediaplayer");
        result = params;
        break;
      case 208:
        params.add("%m.mediaplayer");
        result = params;
        break;
      case 207:
        params.add("%m.mediaplayer");
        result = params;
        break;
      case 206:
        params.add("%m.mediaplayer");
        result = params;
        break;
      case 205:
        params.add("%m.mediaplayer");
        result = params;
        break;
      case 204:
        params.add("%m.mediaplayer");
        params.add("%d");
        result = params;
        break;
      case 203:
        params.add("%m.mediaplayer");
        result = params;
        break;
      case 202:
        params.add("%m.mediaplayer");
        result = params;
        break;
      case 201:
        params.add("%m.mediaplayer");
        params.add("%m.sound");
        result = params;
        break;
      case 200:
        params.add("%m.dialog");
        params.add("%s");
        result = params;
        break;
      case 199:
        params.add("%m.dialog");
        params.add("%s");
        result = params;
        break;
      case 198:
        params.add("%m.dialog");
        params.add("%s");
        result = params;
        break;
      case 197:
        params.add("%m.dialog");
        result = params;
        break;
      case 196:
        params.add("%m.dialog");
        result = params;
        break;
      case 195:
        params.add("%m.dialog");
        params.add("%s");
        result = params;
        break;
      case 194:
        params.add("%m.dialog");
        params.add("%s");
        result = params;
        break;
      case 193:
        params.add("%m.timer");
        result = params;
        break;
      case 192:
        params.add("%m.timer");
        params.add("%d");
        params.add("%d");
        result = params;
        break;
      case 191:
        params.add("%m.timer");
        params.add("%d");
        result = params;
        break;
      case 190:
        params.add("%m.vibrator");
        params.add("%d");
        result = params;
        break;
      case 189:
        params.add("%m.calendar");
        params.add("%d");
        result = params;
        break;
      case 188:
        params.add("%m.calendar");
        result = params;
        break;
      case 187:
        params.add("%m.calendar");
        params.add("%m.calendar");
        result = params;
        break;
      case 186:
        params.add("%m.calendar");
        params.add("%s");
        result = params;
        break;
      case 185:
        params.add("%m.calendar");
        params.add("%m.calendarField");
        params.add("%d");
        result = params;
        break;
      case 184:
        params.add("%m.calendar");
        params.add("%m.calendarField");
        params.add("%d");
        result = params;
        break;
      case 183:
        params.add("%m.calendar");
        result = params;
        break;
      case 182:
        params.add("%m.file");
        params.add("%s");
        result = params;
        break;
      case 181:
        params.add("%m.file");
        params.add("%s");
        params.add("%s");
        result = params;
        break;
      case 180:
        params.add("%m.file");
        params.add("%s");
        result = params;
        break;
      case 178:
        params.add("%s");
        result = params;
        break;
      case 177:
        params.add("%m.intent");
        result = params;
        break;
      case 176:
        params.add("%m.intent");
        params.add("%m.intentFlags");
        result = params;
        break;
      case 175:
        params.add("%m.intent");
        params.add("%s");
        params.add("%s");
        result = params;
        break;
      case 174:
        params.add("%m.intent");
        params.add("%m.activity");
        result = params;
        break;
      case 173:
        params.add("%m.intent");
        params.add("%s.intentData");
        result = params;
        break;
      case 172:
        params.add("%m.intent");
        params.add("%m.intentAction");
        result = params;
        break;
      case 171:
        params.add("%m.mapview");
        params.add("%s");
        params.add("%b");
        result = params;
        break;
      case 170:
        params.add("%m.mapview");
        params.add("%s");
        params.add("%m.resource");
        result = params;
        break;
      case 169:
        params.add("%m.mapview");
        params.add("%s");
        params.add("%m.markerColor");
        params.add("%d");
        result = params;
        break;
      case 168:
        params.add("%m.mapview");
        params.add("%s");
        params.add("%d");
        params.add("%d");
        result = params;
        break;
      case 167:
        params.add("%m.mapview");
        params.add("%s");
        params.add("%s");
        params.add("%s");
        result = params;
        break;
      case 166:
        params.add("%m.mapview");
        params.add("%s");
        params.add("%d");
        params.add("%d");
        result = params;
        break;
      case 165:
        params.add("%m.mapview");
        result = params;
        break;
      case 164:
        params.add("%m.mapview");
        result = params;
        break;
      case 163:
        params.add("%m.mapview");
        params.add("%d");
        result = params;
        break;
      case 162:
        params.add("%m.mapview");
        params.add("%d");
        params.add("%d");
        result = params;
        break;
      case 161:
        params.add("%m.mapview");
        params.add("%m.mapType");
        result = params;
        break;
      case 160:
        params.add("%m.adview");
        result = params;
        break;
      case 159:
        params.add("%m.calendarview");
        params.add("%d");
        result = params;
        break;
      case 158:
        params.add("%m.calendarview");
        params.add("%d");
        result = params;
        break;
      case 157:
        params.add("%m.calendarview");
        params.add("%d");
        result = params;
        break;
      case 156:
        params.add("%m.calendarview");
        result = params;
        break;
      case 155:
        params.add("%m.webview");
        result = params;
        break;
      case 154:
        params.add("%m.webview");
        result = params;
        break;
      case 153:
        params.add("%m.webview");
        result = params;
        break;
      case 152:
        params.add("%m.webview");
        result = params;
        break;
      case 151:
        params.add("%m.webview");
        result = params;
        break;
      case 150:
        params.add("%m.webview");
        result = params;
        break;
      case 149:
        params.add("%m.webview");
        result = params;
        break;
      case 148:
        params.add("%m.webview");
        result = params;
        break;
      case 147:
        params.add("%m.webview");
        result = params;
        break;
      case 146:
        params.add("%m.webview");
        params.add("%m.cacheMode");
        result = params;
        break;
      case 145:
        params.add("%m.webview");
        result = params;
        break;
      case 144:
        params.add("%m.webview");
        params.add("%s.url");
        result = params;
        break;
      case 143:
        params.add("%m.spinner");
        result = params;
        break;
      case 142:
        params.add("%m.spinner");
        params.add("%d");
        result = params;
        break;
      case 141:
        params.add("%m.spinner");
        params.add("%m.listStr");
        result = params;
        break;
      case 140:
        params.add("%m.listview");
        params.add("%m.listMap");
        result = params;
        break;
      case 139:
        params.add("%m.listview");
        params.add("%m.listStr");
        result = params;
        break;
      case 138:
        params.add("%m.switch");
        params.add("%m.resource");
        result = params;
        break;
      case 137:
        params.add("%m.switch");
        params.add("%m.resource");
        result = params;
        break;
      case 136:
        params.add("%m.checkbox");
        result = params;
        break;
      case 135:
        params.add("%m.checkbox");
        params.add("%b");
        result = params;
        break;
      case 134:
        params.add("%m.view");
        result = params;
        break;
      case 133:
        params.add("%m.view");
        result = params;
        break;
      case 132:
        params.add("%m.view");
        result = params;
        break;
      case 131:
        params.add("%m.view");
        params.add("%d");
        result = params;
        break;
      case 130:
        params.add("%m.view");
        result = params;
        break;
      case 129:
        params.add("%m.view");
        params.add("%d");
        result = params;
        break;
      case 128:
        params.add("%m.view");
        result = params;
        break;
      case 127:
        params.add("%m.view");
        params.add("%d");
        result = params;
        break;
      case 126:
        params.add("%m.view");
        result = params;
        break;
      case 125:
        params.add("%m.view");
        params.add("%d");
        result = params;
        break;
      case 124:
        params.add("%m.view");
        result = params;
        break;
      case 123:
        params.add("%m.view");
        params.add("%d");
        result = params;
        break;
      case 122:
        params.add("%m.view");
        result = params;
        break;
      case 121:
        params.add("%m.view");
        params.add("%d");
        result = params;
        break;
      case 120:
        params.add("%m.imageview");
        params.add("%s");
        result = params;
        break;
      case 119:
        params.add("%m.imageview");
        params.add("%s");
        result = params;
        break;
      case 118:
        params.add("%m.view");
        result = params;
        break;
      case 117:
        params.add("%m.imageview");
        params.add("%m.color");
        result = params;
        break;
      case 116:
        params.add("%m.imageview");
        params.add("%m.resource");
        result = params;
        break;
      case 115:
        params.add("%m.edittext");
        params.add("%m.color");
        result = params;
        break;
      case 114:
        params.add("%m.edittext");
        params.add("%s");
        result = params;
        break;
      case 113:
        params.add("%m.textview");
        params.add("%m.color");
        result = params;
        break;
      case 112:
        params.add("%m.view");
        params.add("%m.resource_bg");
        result = params;
        break;
      case 111:
        params.add("%m.view");
        params.add("%m.color");
        result = params;
        break;
      case 110:
        params.add("%m.textview");
        result = params;
        break;
      case 109:
        params.add("%m.textview");
        params.add("%m.font");
        params.add("%m.typeface");
        result = params;
        break;
      case 108:
        params.add("%m.textview");
        params.add("%s");
        result = params;
        break;
      case 107:
        params.add("%m.view");
        params.add("%b");
        result = params;
        break;
      case 106:
        params.add("%m.view");
        params.add("%m.visible");
        result = params;
        break;
      case 105:
        params.add("%m.view");
        result = params;
        break;
      case 104:
        params.add("%m.view");
        params.add("%b");
        result = params;
        break;
      case 100:
        params.add("%m.view");
        result = params;
        break;
      case 99:
        params.add("%d");
        result = params;
        break;
      case 98:
        params.add("%d");
        result = params;
        break;
      case 97:
        params.add("%d");
        result = params;
        break;
      case 96:
        params.add("%d");
        result = params;
        break;
      case 95:
        params.add("%d");
        result = params;
        break;
      case 94:
        params.add("%d");
        result = params;
        break;
      case 93:
        params.add("%d");
        result = params;
        break;
      case 92:
        params.add("%d");
        result = params;
        break;
      case 91:
        params.add("%d");
        result = params;
        break;
      case 90:
        params.add("%d");
        result = params;
        break;
      case 89:
        params.add("%d");
        result = params;
        break;
      case 88:
        params.add("%d");
        result = params;
        break;
      case 87:
        params.add("%d");
        result = params;
        break;
      case 86:
        params.add("%d");
        result = params;
        break;
      case 85:
        params.add("%d");
        result = params;
        break;
      case 84:
        params.add("%d");
        result = params;
        break;
      case 83:
        params.add("%d");
        params.add("%d");
        result = params;
        break;
      case 82:
        params.add("%d");
        params.add("%d");
        result = params;
        break;
      case 81:
        params.add("%d");
        params.add("%d");
        result = params;
        break;
      case 76:
        params.add("%d");
        result = params;
        break;
      case 75:
        params.add("%m.listMap");
        result = params;
        break;
      case 74:
        params.add("%s");
        params.add("%m.listMap");
        result = params;
        break;
      case 73:
        params.add("%m.varMap");
        result = params;
        break;
      case 72:
        params.add("%s");
        params.add("%m.varMap");
        result = params;
        break;
      case 71:
        params.add("%d");
        params.add("%s");
        result = params;
        break;
      case 70:
        params.add("%d");
        result = params;
        break;
      case 69:
        params.add("%d");
        result = params;
        break;
      case 68:
        params.add("%s");
        result = params;
        break;
      case 67:
        params.add("%s");
        result = params;
        break;
      case 66:
        params.add("%s");
        result = params;
        break;
      case 65:
        params.add("%s");
        result = params;
        break;
      case 64:
        params.add("%s");
        params.add("%s");
        params.add("%s");
        result = params;
        break;
      case 63:
        params.add("%s");
        params.add("%s");
        params.add("%s");
        result = params;
        break;
      case 62:
        params.add("%s");
        params.add("%s");
        params.add("%s");
        result = params;
        break;
      case 61:
        params.add("%s");
        params.add("%s");
        result = params;
        break;
      case 60:
        params.add("%s");
        params.add("%s");
        result = params;
        break;
      case 59:
        params.add("%s");
        params.add("%d");
        params.add("%d");
        result = params;
        break;
      case 58:
        params.add("%s");
        params.add("%s");
        result = params;
        break;
      case 57:
        params.add("%s");
        params.add("%s");
        result = params;
        break;
      case 56:
        params.add("%s");
        params.add("%s");
        result = params;
        break;
      case 55:
        params.add("%s");
        result = params;
        break;
      case 54:
        params.add("%d");
        params.add("%d");
        result = params;
        break;
      case 53:
        params.add("%d");
        params.add("%d");
        result = params;
        break;
      case 52:
        params.add("%d");
        params.add("%d");
        result = params;
        break;
      case 51:
        params.add("%d");
        params.add("%d");
        result = params;
        break;
      case 50:
        params.add("%d");
        params.add("%d");
        result = params;
        break;
      case 49:
        params.add("%d");
        params.add("%d");
        result = params;
        break;
      case 48:
        params.add("%b");
        result = params;
        break;
      case 47:
        params.add("%b");
        params.add("%b");
        result = params;
        break;
      case 46:
        params.add("%b");
        params.add("%b");
        result = params;
        break;
      case 45:
        params.add("%d");
        params.add("%d");
        result = params;
        break;
      case 44:
        params.add("%d");
        params.add("%d");
        result = params;
        break;
      case 43:
        params.add("%d");
        params.add("%d");
        result = params;
        break;
      case 39:
        params.add("%b");
        result = params;
        break;
      case 38:
        params.add("%b");
        result = params;
        break;
      case 35:
        params.add("%d");
        result = params;
        break;
      case 34:
        params.add("%m.list");
        result = params;
        break;
      case 33:
        params.add("%m.list");
        result = params;
        break;
      case 32:
        params.add("%d");
        params.add("%m.list");
        result = params;
        break;
      case 31:
        params.add("%d");
        params.add("%m.listMap");
        params.add("%m.varMap");
        result = params;
        break;
      case 30:
        params.add("%m.varMap");
        params.add("%d");
        params.add("%m.listMap");
        result = params;
        break;
      case 29:
        params.add("%m.varMap");
        params.add("%m.listMap");
        result = params;
        break;
      case 28:
        params.add("%m.listMap");
        params.add("%d");
        params.add("%s");
        result = params;
        break;
      case 27:
        params.add("%s");
        params.add("%s");
        params.add("%d");
        params.add("%m.listMap");
        result = params;
        break;
      case 26:
        params.add("%d");
        params.add("%s");
        params.add("%m.listMap");
        result = params;
        break;
      case 25:
        params.add("%s");
        params.add("%s");
        params.add("%d");
        params.add("%m.listMap");
        result = params;
        break;
      case 24:
        params.add("%s");
        params.add("%s");
        params.add("%m.listMap");
        result = params;
        break;
      case 23:
        params.add("%m.listStr");
        params.add("%s");
        result = params;
        break;
      case 22:
        params.add("%s");
        params.add("%m.listStr");
        result = params;
        break;
      case 21:
        params.add("%d");
        params.add("%m.listStr");
        result = params;
        break;
      case 20:
        params.add("%s");
        params.add("%d");
        params.add("%m.listStr");
        result = params;
        break;
      case 19:
        params.add("%s");
        params.add("%m.listStr");
        result = params;
        break;
      case 18:
        params.add("%m.listInt");
        params.add("%d");
        result = params;
        break;
      case 17:
        params.add("%d");
        params.add("%m.listInt");
        result = params;
        break;
      case 16:
        params.add("%d");
        params.add("%m.listInt");
        result = params;
        break;
      case 15:
        params.add("%d");
        params.add("%d");
        params.add("%m.listInt");
        result = params;
        break;
      case 14:
        params.add("%d");
        params.add("%m.listInt");
        result = params;
        break;
      case 13:
        params.add("%m.varMap");
        params.add("%m.listStr");
        result = params;
        break;
      case 12:
        params.add("%m.varMap");
        result = params;
        break;
      case 11:
        params.add("%m.varMap");
        result = params;
        break;
      case 10:
        params.add("%m.varMap");
        result = params;
        break;
      case 9:
        params.add("%m.varMap");
        params.add("%s");
        result = params;
        break;
      case 8:
        params.add("%m.varMap");
        params.add("%s");
        result = params;
        break;
      case 7:
        params.add("%m.varMap");
        params.add("%s");
        result = params;
        break;
      case 6:
        params.add("%m.varMap");
        params.add("%s");
        params.add("%s");
        result = params;
        break;
      case 5:
        params.add("%m.varMap");
        result = params;
        break;
      case 4:
        params.add("%m.varStr");
        params.add("%s");
        result = params;
        break;
      case 3:
        params.add("%m.varInt");
        result = params;
        break;
      case 2:
        params.add("%m.varInt");
        result = params;
        break;
      case 1:
        params.add("%m.varInt");
        params.add("%d");
        result = params;
        break;
      case 0:
        params.add("%m.varBool");
        params.add("%b");
        result = params;
        break;
      case 36:
      case 37:
      case 40:
      case 41:
      case 42:
      case 77:
      case 78:
      case 79:
      case 80:
      case 101:
      case 102:
      case 103:
      case 179:
      case 236:
      case 237:
      case 238:
      case 239:
      case 269:
      case 270:
        break;
    } 
    cacheA.put(blockName, result);
    return result;
  }
  
  public static ArrayList<String> getBlockMenuItems(String blockName) {
    if (cacheB.containsKey(blockName)) return cacheB.get(blockName);
    byte b = -1;
    ArrayList<String> menuItems = new ArrayList<>();
    switch (blockName.hashCode()) {
      default:
        b = -1;
        break;
      case 2087273080:
        if (blockName.equals("onFilesPicked")) {
          b = 43;
          break;
        } 
      case 1979400473:
        if (blockName.equals("onItemLongClicked")) {
          b = 21;
          break;
        } 
      case 1855724576:
        if (blockName.equals("onAdFailedToLoad")) {
          b = 45;
          break;
        } 
      case 1803231982:
        if (blockName.equals("onMarkerClicked")) {
          b = 56;
          break;
        } 
      case 1757061906:
        if (blockName.equals("onFilesPickedCancel")) {
          b = 44;
          break;
        } 
      case 1710477203:
        if (blockName.equals("onPageStarted")) {
          b = 22;
          break;
        } 
      case 1633718655:
        if (blockName.equals("onCreateUserComplete")) {
          b = 32;
          break;
        } 
      case 1586033095:
        if (blockName.equals("onStopTrackingTouch")) {
          b = 10;
          break;
        } 
      case 1463983852:
        if (blockName.equals("onResume")) {
          b = 6;
          break;
        } 
      case 1395209852:
        if (blockName.equals("onDownloadSuccess")) {
          b = 38;
          break;
        } 
      case 1348605570:
        if (blockName.equals("onPictureTakenCancel")) {
          b = 42;
          break;
        } 
      case 1348442836:
        if (blockName.equals("onDownloadProgress")) {
          b = 36;
          break;
        } 
      case 1170737640:
        if (blockName.equals("onPictureTaken")) {
          b = 41;
          break;
        } 
      case 948174187:
        if (blockName.equals("onAdOpened")) {
          b = 15;
          break;
        } 
      case 863618555:
        if (blockName.equals("onSensorChanged")) {
          b = 31;
          break;
        } 
      case 861234439:
        if (blockName.equals("onAdLoaded")) {
          b = 14;
          break;
        } 
      case 805710389:
        if (blockName.equals("onItemClicked")) {
          b = 20;
          break;
        } 
      case 694589214:
        if (blockName.equals("onSpeechResult")) {
          b = 48;
          break;
        } 
      case 601233006:
        if (blockName.equals("onAdClosed")) {
          b = 16;
          break;
        } 
      case 445802034:
        if (blockName.equals("onCancelled")) {
          b = 30;
          break;
        } 
      case 378110312:
        if (blockName.equals("onTextChanged")) {
          b = 19;
          break;
        } 
      case 372583555:
        if (blockName.equals("onChildAdded")) {
          b = 27;
          break;
        } 
      case 264008033:
        if (blockName.equals("onDataSent")) {
          b = 52;
          break;
        } 
      case 249705131:
        if (blockName.equals("onFailure")) {
          b = 40;
          break;
        } 
      case 204442875:
        if (blockName.equals("onPostCreate")) {
          b = 2;
          break;
        } 
      case 162093458:
        if (blockName.equals("onBindCustomView")) {
          b = 25;
          break;
        } 
      case 136827711:
        if (blockName.equals("onAnimationCancel")) {
          b = 13;
          break;
        } 
      case 80616227:
        if (blockName.equals("onUploadSuccess")) {
          b = 37;
          break;
        } 
      case -376002870:
        if (blockName.equals("onErrorResponse")) {
          b = 47;
          break;
        } 
      case -388502098:
        if (blockName.equals("initializeLogic")) {
          b = 0;
          break;
        } 
      case -484536541:
        if (blockName.equals("onChildRemoved")) {
          b = 29;
          break;
        } 
      case -505277536:
        if (blockName.equals("onPageFinished")) {
          b = 23;
          break;
        } 
      case -507667891:
        if (blockName.equals("onItemSelected")) {
          b = 18;
          break;
        } 
      case -536246231:
        if (blockName.equals("onResetPasswordEmailSent")) {
          b = 34;
          break;
        } 
      case -584901992:
        if (blockName.equals("onCheckedChange")) {
          b = 17;
          break;
        } 
      case -609996822:
        if (blockName.equals("onConnected")) {
          b = 50;
          break;
        } 
      case -672992515:
        if (blockName.equals("onAnimationStart")) {
          b = 11;
          break;
        } 
      case -719893013:
        if (blockName.equals("onConnectionError")) {
          b = 53;
          break;
        } 
      case -732782352:
        if (blockName.equals("onConnectionStopped")) {
          b = 54;
          break;
        } 
      case -749253875:
        if (blockName.equals("onUploadProgress")) {
          b = 35;
          break;
        } 
      case -821066400:
        if (blockName.equals("onLocationChanged")) {
          b = 57;
          break;
        } 
      case -837428873:
        if (blockName.equals("onChildChanged")) {
          b = 28;
          break;
        } 
      case -891988931:
        if (blockName.equals("onDateChange")) {
          b = 26;
          break;
        } 
      case -1012956543:
        if (blockName.equals("onStop")) {
          b = 4;
          break;
        } 
      case -1111243300:
        if (blockName.equals("onBackPressed")) {
          b = 1;
          break;
        } 
      case -1153785290:
        if (blockName.equals("onAnimationEnd")) {
          b = 12;
          break;
        } 
      case -1215328199:
        if (blockName.equals("onDeleteSuccess")) {
          b = 39;
          break;
        } 
      case -1336895037:
        if (blockName.equals("onStart")) {
          b = 3;
          break;
        } 
      case -1340212393:
        if (blockName.equals("onPause")) {
          b = 7;
          break;
        } 
      case -1351902487:
        if (blockName.equals("onClick")) {
          b = 8;
          break;
        } 
      case -1358405466:
        if (blockName.equals("onMapReady")) {
          b = 55;
          break;
        } 
      case -1401315045:
        if (blockName.equals("onDestroy")) {
          b = 5;
          break;
        } 
      case -1708629179:
        if (blockName.equals("onSignInUserComplete")) {
          b = 33;
          break;
        } 
      case -1779618840:
        if (blockName.equals("onProgressChanged")) {
          b = 24;
          break;
        } 
      case -1809154262:
        if (blockName.equals("onDataReceived")) {
          b = 51;
          break;
        } 
      case -1865337024:
        if (blockName.equals("onResponse")) {
          b = 46;
          break;
        } 
      case -2067423513:
        if (blockName.equals("onSpeechError")) {
          b = 49;
          break;
        } 
      case -2117913147:
        if (blockName.equals("onStartTrackingTouch")) {
          b = 9;
          break;
        } 
    } 
    switch (b) {
      case 57:
        menuItems.add("%d.lat");
        menuItems.add("%d.lng");
        menuItems.add("%d.acc");
        break;
      case 56:
        menuItems.add("%s.id");
        break;
      case 54:
        menuItems.add("%s.tag");
        break;
      case 53:
        menuItems.add("%s.tag");
        menuItems.add("%s.connectionState");
        menuItems.add("%s.errorMessage");
        break;
      case 52:
        menuItems.add("%s.tag");
        menuItems.add("%s.data");
        break;
      case 51:
        menuItems.add("%s.tag");
        menuItems.add("%s.data");
        break;
      case 50:
        menuItems.add("%s.tag");
        menuItems.add("%m.varMap.deviceData");
        break;
      case 49:
        menuItems.add("%s.errorMessage");
        break;
      case 48:
        menuItems.add("%s.result");
        break;
      case 47:
        menuItems.add("%s.tag");
        menuItems.add("%s.message");
        break;
      case 46:
        menuItems.add("%s.tag");
        menuItems.add("%s.response");
        menuItems.add("%m.varMap.responseHeaders");
        break;
      case 45:
        menuItems.add("%d.errorCode");
        break;
      case 43:
        menuItems.add("%m.listStr.filePath");
        break;
      case 41:
        menuItems.add("%s.filePath");
        break;
      case 40:
        menuItems.add("%s.message");
        break;
      case 38:
        menuItems.add("%d.totalByteCount");
        break;
      case 37:
        menuItems.add("%s.downloadUrl");
        break;
      case 35:
      case 36:
        menuItems.add("%d.progressValue");
        break;
      case 34:
        menuItems.add("%b.success");
        break;
      case 33:
        menuItems.add("%b.success");
        menuItems.add("%s.errorMessage");
        break;
      case 32:
        menuItems.add("%b.success");
        menuItems.add("%s.errorMessage");
        break;
      case 31:
        menuItems.add("%d.x");
        menuItems.add("%d.y");
        menuItems.add("%d.z");
        break;
      case 30:
        menuItems.add("%d.errorCode");
        menuItems.add("%s.errorMessage");
        break;
      case 27:
      case 28:
      case 29:
        menuItems.add("%s.childKey");
        menuItems.add("%m.varMap.childValue");
        break;
      case 26:
        menuItems.add("%d.year");
        menuItems.add("%d.month");
        menuItems.add("%d.day");
        break;
      case 25:
        menuItems.add("%m.listMap.data");
        menuItems.add("%d.position");
        break;
      case 24:
        menuItems.add("%d.progressValue");
        break;
      case 23:
        menuItems.add("%s.url");
        break;
      case 22:
        menuItems.add("%s.url");
        break;
      case 21:
        menuItems.add("%d.position");
        break;
      case 20:
        menuItems.add("%d.position");
        break;
      case 19:
        menuItems.add("%s.charSeq");
        break;
      case 18:
        menuItems.add("%d.position");
        break;
      case 17:
        menuItems.add("%b.isChecked");
        break;
    } 
    cacheB.put(blockName, menuItems);
    return menuItems;
  }
  
  public static String getEventSpec(String blockName) {
    if (cacheC.containsKey(blockName)) return cacheC.get(blockName);
    String resultC = computeC(blockName);
    cacheC.put(blockName, resultC);
    return resultC;
  }

  private static String computeC(String blockName) {
    byte b = -1;
    switch (blockName.hashCode()) {
      default:
        b = -1;
        break;
      case 2087273080:
        if (blockName.equals("onFilesPicked")) {
          b = 40;
          break;
        } 
      case 1979400473:
        if (blockName.equals("onItemLongClicked")) {
          b = 12;
          break;
        } 
      case 1855724576:
        if (blockName.equals("onAdFailedToLoad")) {
          b = 43;
          break;
        } 
      case 1803231982:
        if (blockName.equals("onMarkerClicked")) {
          b = 56;
          break;
        } 
      case 1757061906:
        if (blockName.equals("onFilesPickedCancel")) {
          b = 41;
          break;
        } 
      case 1710477203:
        if (blockName.equals("onPageStarted")) {
          b = 14;
          break;
        } 
      case 1633718655:
        if (blockName.equals("onCreateUserComplete")) {
          b = 29;
          break;
        } 
      case 1586033095:
        if (blockName.equals("onStopTrackingTouch")) {
          b = 18;
          break;
        } 
      case 1463983852:
        if (blockName.equals("onResume")) {
          b = 6;
          break;
        } 
      case 1395209852:
        if (blockName.equals("onDownloadSuccess")) {
          b = 35;
          break;
        } 
      case 1348605570:
        if (blockName.equals("onPictureTakenCancel")) {
          b = 39;
          break;
        } 
      case 1348442836:
        if (blockName.equals("onDownloadProgress")) {
          b = 33;
          break;
        } 
      case 1170737640:
        if (blockName.equals("onPictureTaken")) {
          b = 38;
          break;
        } 
      case 948174187:
        if (blockName.equals("onAdOpened")) {
          b = 44;
          break;
        } 
      case 863618555:
        if (blockName.equals("onSensorChanged")) {
          b = 28;
          break;
        } 
      case 861234439:
        if (blockName.equals("onAdLoaded")) {
          b = 42;
          break;
        } 
      case 805710389:
        if (blockName.equals("onItemClicked")) {
          b = 11;
          break;
        } 
      case 694589214:
        if (blockName.equals("onSpeechResult")) {
          b = 48;
          break;
        } 
      case 601233006:
        if (blockName.equals("onAdClosed")) {
          b = 45;
          break;
        } 
      case 445802034:
        if (blockName.equals("onCancelled")) {
          b = 27;
          break;
        } 
      case 378110312:
        if (blockName.equals("onTextChanged")) {
          b = 13;
          break;
        } 
      case 372583555:
        if (blockName.equals("onChildAdded")) {
          b = 24;
          break;
        } 
      case 264008033:
        if (blockName.equals("onDataSent")) {
          b = 52;
          break;
        } 
      case 249705131:
        if (blockName.equals("onFailure")) {
          b = 37;
          break;
        } 
      case 204442875:
        if (blockName.equals("onPostCreate")) {
          b = 2;
          break;
        } 
      case 162093458:
        if (blockName.equals("onBindCustomView")) {
          b = 22;
          break;
        } 
      case 136827711:
        if (blockName.equals("onAnimationCancel")) {
          b = 21;
          break;
        } 
      case 80616227:
        if (blockName.equals("onUploadSuccess")) {
          b = 34;
          break;
        } 
      case -376002870:
        if (blockName.equals("onErrorResponse")) {
          b = 47;
          break;
        } 
      case -388502098:
        if (blockName.equals("initializeLogic")) {
          b = 0;
          break;
        } 
      case -484536541:
        if (blockName.equals("onChildRemoved")) {
          b = 26;
          break;
        } 
      case -505277536:
        if (blockName.equals("onPageFinished")) {
          b = 15;
          break;
        } 
      case -507667891:
        if (blockName.equals("onItemSelected")) {
          b = 10;
          break;
        } 
      case -536246231:
        if (blockName.equals("onResetPasswordEmailSent")) {
          b = 31;
          break;
        } 
      case -584901992:
        if (blockName.equals("onCheckedChange")) {
          b = 9;
          break;
        } 
      case -609996822:
        if (blockName.equals("onConnected")) {
          b = 50;
          break;
        } 
      case -672992515:
        if (blockName.equals("onAnimationStart")) {
          b = 19;
          break;
        } 
      case -719893013:
        if (blockName.equals("onConnectionError")) {
          b = 53;
          break;
        } 
      case -732782352:
        if (blockName.equals("onConnectionStopped")) {
          b = 54;
          break;
        } 
      case -749253875:
        if (blockName.equals("onUploadProgress")) {
          b = 32;
          break;
        } 
      case -821066400:
        if (blockName.equals("onLocationChanged")) {
          b = 57;
          break;
        } 
      case -837428873:
        if (blockName.equals("onChildChanged")) {
          b = 25;
          break;
        } 
      case -891988931:
        if (blockName.equals("onDateChange")) {
          b = 23;
          break;
        } 
      case -1012956543:
        if (blockName.equals("onStop")) {
          b = 4;
          break;
        } 
      case -1111243300:
        if (blockName.equals("onBackPressed")) {
          b = 1;
          break;
        } 
      case -1153785290:
        if (blockName.equals("onAnimationEnd")) {
          b = 20;
          break;
        } 
      case -1215328199:
        if (blockName.equals("onDeleteSuccess")) {
          b = 36;
          break;
        } 
      case -1336895037:
        if (blockName.equals("onStart")) {
          b = 3;
          break;
        } 
      case -1340212393:
        if (blockName.equals("onPause")) {
          b = 7;
          break;
        } 
      case -1351902487:
        if (blockName.equals("onClick")) {
          b = 8;
          break;
        } 
      case -1358405466:
        if (blockName.equals("onMapReady")) {
          b = 55;
          break;
        } 
      case -1401315045:
        if (blockName.equals("onDestroy")) {
          b = 5;
          break;
        } 
      case -1708629179:
        if (blockName.equals("onSignInUserComplete")) {
          b = 30;
          break;
        } 
      case -1779618840:
        if (blockName.equals("onProgressChanged")) {
          b = 16;
          break;
        } 
      case -1809154262:
        if (blockName.equals("onDataReceived")) {
          b = 51;
          break;
        } 
      case -1865337024:
        if (blockName.equals("onResponse")) {
          b = 46;
          break;
        } 
      case -2067423513:
        if (blockName.equals("onSpeechError")) {
          b = 49;
          break;
        } 
      case -2117913147:
        if (blockName.equals("onStartTrackingTouch")) {
          b = 17;
          break;
        } 
    } 
    switch (b) {
      default:
        return "";
      case 57:
        return "on_location_changed";
      case 56:
        return "on_marker_clicked";
      case 55:
        return "on_map_ready";
      case 54:
        return "on_connection_stopped";
      case 53:
        return "on_connection_error";
      case 52:
        return "on_data_sent";
      case 51:
        return "on_data_received";
      case 50:
        return "on_connected";
      case 49:
        return "on_speech_error";
      case 48:
        return "on_speech_result";
      case 47:
        return "on_error_response";
      case 46:
        return "on_response";
      case 45:
        return "on_ad_closed";
      case 44:
        return "on_ad_opened";
      case 43:
        return "on_ad_failed_to_load";
      case 42:
        return "on_ad_loaded";
      case 41:
        return "on_files_picked_cancel";
      case 40:
        return "on_files_picked";
      case 39:
        return "on_picture_taken_cancel";
      case 38:
        return "on_picture_taken";
      case 37:
        return "on_failure";
      case 36:
        return "on_delete_success";
      case 35:
        return "on_download_success";
      case 34:
        return "on_upload_success";
      case 33:
        return "on_download_progress";
      case 32:
        return "on_upload_progress";
      case 31:
        return "on_reset_password_email_sent";
      case 30:
        return "on_sign_in_user_complete";
      case 29:
        return "on_create_user_complete";
      case 28:
        return "on_sensor_changed";
      case 27:
        return "on_cancelled";
      case 26:
        return "on_child_removed";
      case 25:
        return "on_child_changed";
      case 24:
        return "on_child_added";
      case 23:
        return "on_date_change";
      case 22:
        return "on_bind_custom_view";
      case 21:
        return "on_animation_cancel";
      case 20:
        return "on_animation_end";
      case 19:
        return "on_animation_start";
      case 18:
        return "on_stop_tracking_touch";
      case 17:
        return "on_start_tracking_touch";
      case 16:
        return "on_progress_changed";
      case 15:
        return "on_page_finished";
      case 14:
        return "on_page_started";
      case 13:
        return "on_text_changed";
      case 12:
        return "on_item_long_clicked";
      case 11:
        return "on_item_clicked";
      case 10:
        return "on_item_selected";
      case 9:
        return "on_check_changed";
      case 8:
        return "on_clicked";
      case 7:
        return "on_pause";
      case 6:
        return "on_resume";
      case 5:
        return "on_destroy";
      case 4:
        return "on_stop";
      case 3:
        return "on_start";
      case 2:
        return "on_post_created";
      case 1:
        return "on_back_pressed";
      case 0:
        break;
    } 
    return "initialize";
  }
  
  public static String getBlockSpec(String blockName) {
    if (cacheD.containsKey(blockName)) return cacheD.get(blockName);
    String originalKey = blockName;
    int i = blockName.hashCode();
    String defaultValue = "false";
    switch (i) {
      default:
        i = -1;
        break;
      case 2138225950:
        if (blockName.equals("locationManagerRequestLocationUpdates")) {
          i = 320;
          break;
        } 
      case 2130649194:
        if (blockName.equals("bluetoothConnectGetPairedDevices")) {
          i = 318;
          break;
        } 
      case 2127377128:
        if (blockName.equals("mediaplayerGetCurrent")) {
          i = 213;
          break;
        } 
      case 2090189010:
        if (blockName.equals("addListStr")) {
          i = 22;
          break;
        } 
      case 2090182653:
        if (blockName.equals("addListMap")) {
          i = 27;
          break;
        } 
      case 2090179216:
        if (blockName.equals("addListInt")) {
          i = 14;
          break;
        } 
      case 2075310296:
        if (blockName.equals("interstitialadLoadAd")) {
          i = 253;
          break;
        } 
      case 2017929665:
        if (blockName.equals("calendarViewSetMinDate")) {
          i = 166;
          break;
        } 
      case 1984984239:
        if (blockName.equals("setText")) {
          i = 109;
          break;
        } 
      case 1984630281:
        if (blockName.equals("setHint")) {
          i = 115;
          break;
        } 
      case 1976325370:
        if (blockName.equals("setImageFilePath")) {
          i = 120;
          break;
        } 
      case 1974249461:
        if (blockName.equals("skewBitmapFile")) {
          i = 294;
          break;
        } 
      case 1973523807:
        if (blockName.equals("mediaplayerIsPlaying")) {
          i = 215;
          break;
        } 
      case 1964823036:
        if (blockName.equals("bluetoothConnectStopConnection")) {
          i = 313;
          break;
        } 
      case 1948735400:
        if (blockName.equals("getAlpha")) {
          i = 125;
          break;
        } 
      case 1941634330:
        if (blockName.equals("firebaseAdd")) {
          i = 235;
          break;
        } 
      case 1923980937:
        if (blockName.equals("requestnetworkSetParams")) {
          i = 260;
          break;
        } 
      case 1908582864:
        if (blockName.equals("firebaseStopListen")) {
          i = 249;
          break;
        } 
      case 1908132964:
        if (blockName.equals("mapViewSetMarkerPosition")) {
          i = 176;
          break;
        } 
      case 1885231494:
        if (blockName.equals("webViewCanGoForward")) {
          i = 156;
          break;
        } 
      case 1883337723:
        if (blockName.equals("mathGetDisplayHeight")) {
          i = 79;
          break;
        } 
      case 1873103950:
        if (blockName.equals("locationManagerRemoveUpdates")) {
          i = 321;
          break;
        } 
      case 1848365301:
        if (blockName.equals("mapViewSetMapType")) {
          i = 169;
          break;
        } 
      case 1823151876:
        if (blockName.equals("fileGetData")) {
          i = 188;
          break;
        } 
      case 1820536363:
        if (blockName.equals("interstitialadCreate")) {
          i = 252;
          break;
        } 
      case 1814870108:
        if (blockName.equals("doToast")) {
          i = 277;
          break;
        } 
      case 1792552710:
        if (blockName.equals("rotateBitmapFile")) {
          i = 292;
          break;
        } 
      case 1779174257:
        if (blockName.equals("getChecked")) {
          i = 137;
          break;
        } 
      case 1775620400:
        if (blockName.equals("strToMap")) {
          i = 73;
          break;
        } 
      case 1764351209:
        if (blockName.equals("deleteList")) {
          i = 16;
          break;
        } 
      case 1749552744:
        if (blockName.equals("textToSpeechSpeak")) {
          i = 302;
          break;
        } 
      case 1712613410:
        if (blockName.equals("webViewZoomOut")) {
          i = 163;
          break;
        } 
      case 1695890133:
        if (blockName.equals("fileutilStartsWith")) {
          i = 274;
          break;
        } 
      case 1637498582:
        if (blockName.equals("timerEvery")) {
          i = 200;
          break;
        } 
      case 1635356258:
        if (blockName.equals("requestnetworkStartRequestNetwork")) {
          i = 262;
          break;
        } 
      case 1633341847:
        if (blockName.equals("timerAfter")) {
          i = 199;
          break;
        } 
      case 1601394299:
        if (blockName.equals("listGetCheckedPositions")) {
          i = 145;
          break;
        } 
      case 1498864168:
        if (blockName.equals("seekBarGetProgress")) {
          i = 281;
          break;
        } 
      case 1470831563:
        if (blockName.equals("intentGetString")) {
          i = 186;
          break;
        } 
      case 1437288110:
        if (blockName.equals("getPublicDir")) {
          i = 286;
          break;
        } 
      case 1431171391:
        if (blockName.equals("mapRemoveKey")) {
          i = 9;
          break;
        } 
      case 1410284340:
        if (blockName.equals("seekBarSetProgress")) {
          i = 283;
          break;
        } 
      case 1405084438:
        if (blockName.equals("setTitle")) {
          i = 279;
          break;
        } 
      case 1397501021:
        if (blockName.equals("listRefresh")) {
          i = 142;
          break;
        } 
      case 1395026457:
        if (blockName.equals("setImage")) {
          i = 118;
          break;
        } 
      case 1387622940:
        if (blockName.equals("setAlpha")) {
          i = 124;
          break;
        } 
      case 1348133645:
        if (blockName.equals("stringReplaceFirst")) {
          i = 63;
          break;
        } 
      case 1343794064:
        if (blockName.equals("listSetItemChecked")) {
          i = 143;
          break;
        } 
      case 1330354473:
        if (blockName.equals("firebaseauthSignInAnonymously")) {
          i = 242;
          break;
        } 
      case 1315302372:
        if (blockName.equals("fileutillength")) {
          i = 273;
          break;
        } 
      case 1313527577:
        if (blockName.equals("setTypeface")) {
          i = 110;
          break;
        } 
      case 1311764810:
        if (blockName.equals("setTranslationY")) {
          i = 128;
          break;
        } 
      case 1311764809:
        if (blockName.equals("setTranslationX")) {
          i = 126;
          break;
        } 
      case 1305932583:
        if (blockName.equals("spnGetSelection")) {
          i = 151;
          break;
        } 
      case 1303367340:
        if (blockName.equals("textToSpeechStop")) {
          i = 304;
          break;
        } 
      case 1280029577:
        if (blockName.equals("requestFocus")) {
          i = 117;
          break;
        } 
      case 1252547704:
        if (blockName.equals("listMapToStr")) {
          i = 76;
          break;
        } 
      case 1242107556:
        if (blockName.equals("fileutilisfile")) {
          i = 272;
          break;
        } 
      case 1240510514:
        if (blockName.equals("intentSetScreen")) {
          i = 182;
          break;
        } 
      case 1236956449:
        if (blockName.equals("mediaplayerCreate")) {
          i = 209;
          break;
        } 
      case 1220078450:
        if (blockName.equals("addSourceDirectly")) {
          i = 72;
          break;
        } 
      case 1219299503:
        if (blockName.equals("objectanimatorIsRunning")) {
          i = 234;
          break;
        } 
      case 1219071185:
        if (blockName.equals("firebasestorageUploadFile")) {
          i = 255;
          break;
        } 
      case 1216249183:
        if (blockName.equals("firebasestorageDelete")) {
          i = 257;
          break;
        } 
      case 1187505507:
        if (blockName.equals("stringReplace")) {
          i = 62;
          break;
        } 
      case 1179719371:
        if (blockName.equals("stringLastIndex")) {
          i = 58;
          break;
        } 
      case 1162069698:
        if (blockName.equals("setThumbResource")) {
          i = 138;
          break;
        } 
      case 1160674468:
        if (blockName.equals("lengthList")) {
          i = 19;
          break;
        } 
      case 1159035162:
        if (blockName.equals("mapViewZoomOut")) {
          i = 173;
          break;
        } 
      case 1156598140:
        if (blockName.equals("fileutilEndsWith")) {
          i = 275;
          break;
        } 
      case 1142897724:
        if (blockName.equals("firebaseauthSignInUser")) {
          i = 241;
          break;
        } 
      case 1129709718:
        if (blockName.equals("setImageUrl")) {
          i = 121;
          break;
        } 
      case 1102670563:
        if (blockName.equals("requestnetworkSetHeaders")) {
          i = 261;
          break;
        } 
      case 1090517587:
        if (blockName.equals("getPackageDataDir")) {
          i = 285;
          break;
        } 
      case 1088879149:
        if (blockName.equals("setHintTextColor")) {
          i = 116;
          break;
        } 
      case 1086207657:
        if (blockName.equals("fileutildelete")) {
          i = 263;
          break;
        } 
      case 1068548733:
        if (blockName.equals("mathGetDip")) {
          i = 77;
          break;
        } 
      case 1053179400:
        if (blockName.equals("mapViewSetMarkerColor")) {
          i = 177;
          break;
        } 
      case 950609198:
        if (blockName.equals("setBitmapFileColorFilter")) {
          i = 295;
          break;
        } 
      case 948234497:
        if (blockName.equals("webViewStopLoading")) {
          i = 161;
          break;
        } 
      case 937017988:
        if (blockName.equals("gyroscopeStartListen")) {
          i = 250;
          break;
        } 
      case 932259189:
        if (blockName.equals("setBgResource")) {
          i = 113;
          break;
        } 
      case 898187172:
        if (blockName.equals("mathToRadian")) {
          i = 99;
          break;
        } 
      case 858248741:
        if (blockName.equals("calendarGetTime")) {
          i = 196;
          break;
        } 
      case 848786445:
        if (blockName.equals("objectanimatorSetTarget")) {
          i = 224;
          break;
        } 
      case 845089750:
        if (blockName.equals("setVarString")) {
          i = 4;
          break;
        } 
      case 840991609:
        if (blockName.equals("mathTan")) {
          i = 92;
          break;
        } 
      case 840990896:
        if (blockName.equals("mathSin")) {
          i = 90;
          break;
        } 
      case 840988208:
        if (blockName.equals("mathPow")) {
          i = 82;
          break;
        } 
      case 840985130:
        if (blockName.equals("mathMin")) {
          i = 83;
          break;
        } 
      case 840984892:
        if (blockName.equals("mathMax")) {
          i = 84;
          break;
        } 
      case 840984348:
        if (blockName.equals("mathLog")) {
          i = 97;
          break;
        } 
      case 840977909:
        if (blockName.equals("mathExp")) {
          i = 96;
          break;
        } 
      case 840975711:
        if (blockName.equals("mathCos")) {
          i = 91;
          break;
        } 
      case 840973386:
        if (blockName.equals("mathAbs")) {
          i = 86;
          break;
        } 
      case 836692861:
        if (blockName.equals("mapSize")) {
          i = 10;
          break;
        } 
      case 797861524:
        if (blockName.equals("addMapToList")) {
          i = 32;
          break;
        } 
      case 787825477:
        if (blockName.equals("getScaleY")) {
          i = 133;
          break;
        } 
      case 787825476:
        if (blockName.equals("getScaleX")) {
          i = 131;
          break;
        } 
      case 770834513:
        if (blockName.equals("getRotate")) {
          i = 123;
          break;
        } 
      case 762292097:
        if (blockName.equals("indexListStr")) {
          i = 25;
          break;
        } 
      case 762282303:
        if (blockName.equals("indexListInt")) {
          i = 18;
          break;
        } 
      case 754442829:
        if (blockName.equals("increaseInt")) {
          i = 2;
          break;
        } 
      case 747168008:
        if (blockName.equals("mapCreateNew")) {
          i = 5;
          break;
        } 
      case 738846120:
        if (blockName.equals("textToSpeechSetPitch")) {
          i = 300;
          break;
        } 
      case 737664870:
        if (blockName.equals("mathRound")) {
          i = 87;
          break;
        } 
      case 732108347:
        if (blockName.equals("mathLog10")) {
          i = 98;
          break;
        } 
      case 726887785:
        if (blockName.equals("mapViewSetMarkerInfo")) {
          i = 175;
          break;
        } 
      case 726877492:
        if (blockName.equals("mapViewSetMarkerIcon")) {
          i = 178;
          break;
        } 
      case 726487524:
        if (blockName.equals("mathFloor")) {
          i = 89;
          break;
        } 
      case 725249532:
        if (blockName.equals("intentSetAction")) {
          i = 180;
          break;
        } 
      case 683193060:
        if (blockName.equals("bluetoothConnectStartConnection")) {
          i = 311;
          break;
        } 
      case 657721930:
        if (blockName.equals("setVarInt")) {
          i = 1;
          break;
        } 
      case 615286641:
        if (blockName.equals("dialogNeutralButton")) {
          i = 206;
          break;
        } 
      case 610313513:
        if (blockName.equals("getMapInList")) {
          i = 34;
          break;
        } 
      case 573295520:
        if (blockName.equals("listGetCheckedCount")) {
          i = 146;
          break;
        } 
      case 573208401:
        if (blockName.equals("setScaleY")) {
          i = 132;
          break;
        } 
      case 573208400:
        if (blockName.equals("setScaleX")) {
          i = 130;
          break;
        } 
      case 571046965:
        if (blockName.equals("scaleBitmapFile")) {
          i = 293;
          break;
        } 
      case 556217437:
        if (blockName.equals("setRotate")) {
          i = 122;
          break;
        } 
      case 548860462:
        if (blockName.equals("webViewClearCache")) {
          i = 159;
          break;
        } 
      case 530759231:
        if (blockName.equals("progressBarSetIndeterminate")) {
          i = 299;
          break;
        } 
      case 501171279:
        if (blockName.equals("mathToDegree")) {
          i = 100;
          break;
        } 
      case 490702942:
        if (blockName.equals("filepickerstartpickfiles")) {
          i = 259;
          break;
        } 
      case 481850295:
        if (blockName.equals("resizeBitmapFileToSquare")) {
          i = 288;
          break;
        } 
      case 475815924:
        if (blockName.equals("setTextColor")) {
          i = 114;
          break;
        } 
      case 470160234:
        if (blockName.equals("fileutilGetLastSegmentPath")) {
          i = 276;
          break;
        } 
      case 463594049:
        if (blockName.equals("objectanimatorSetFromTo")) {
          i = 227;
          break;
        } 
      case 463560551:
        if (blockName.equals("mapContainKey")) {
          i = 8;
          break;
        } 
      case 442768763:
        if (blockName.equals("mapGetAllKeys")) {
          i = 13;
          break;
        } 
      case 404265028:
        if (blockName.equals("calendarSet")) {
          i = 193;
          break;
        } 
      case 404247683:
        if (blockName.equals("calendarAdd")) {
          i = 192;
          break;
        } 
      case 401012286:
        if (blockName.equals("getTranslationY")) {
          i = 129;
          break;
        } 
      case 401012285:
        if (blockName.equals("getTranslationX")) {
          i = 127;
          break;
        } 
      case 397166713:
        if (blockName.equals("getEnable")) {
          i = 106;
          break;
        } 
      case 389111867:
        if (blockName.equals("spnSetData")) {
          i = 148;
          break;
        } 
      case 348475309:
        if (blockName.equals("soundpoolStreamStop")) {
          i = 223;
          break;
        } 
      case 348377823:
        if (blockName.equals("soundpoolStreamPlay")) {
          i = 222;
          break;
        } 
      case 342026220:
        if (blockName.equals("interstitialadShow")) {
          i = 254;
          break;
        } 
      case 317453636:
        if (blockName.equals("textToSpeechIsSpeaking")) {
          i = 303;
          break;
        } 
      case 300921928:
        if (blockName.equals("mathSqrt")) {
          i = 85;
          break;
        } 
      case 300433453:
        if (blockName.equals("mathCeil")) {
          i = 88;
          break;
        } 
      case 300388040:
        if (blockName.equals("mathAtan")) {
          i = 95;
          break;
        } 
      case 300387327:
        if (blockName.equals("mathAsin")) {
          i = 93;
          break;
        } 
      case 300372142:
        if (blockName.equals("mathAcos")) {
          i = 94;
          break;
        } 
      case 297379706:
        if (blockName.equals("textToSpeechSetSpeechRate")) {
          i = 301;
          break;
        } 
      case 276674391:
        if (blockName.equals("mapViewMoveCamera")) {
          i = 170;
          break;
        } 
      case 262073061:
        if (blockName.equals("bluetoothConnectReadyConnection")) {
          i = 309;
          break;
        } 
      case 255417137:
        if (blockName.equals("adViewLoadAd")) {
          i = 168;
          break;
        } 
      case 207764385:
        if (blockName.equals("calendarViewGetDate")) {
          i = 164;
          break;
        } 
      case 182549637:
        if (blockName.equals("setEnable")) {
          i = 105;
          break;
        } 
      case 168740282:
        if (blockName.equals("mapToStr")) {
          i = 74;
          break;
        } 
      case 163812602:
        if (blockName.equals("cropBitmapFileFromCenter")) {
          i = 291;
          break;
        } 
      case 152967761:
        if (blockName.equals("mapClear")) {
          i = 11;
          break;
        } 
      case 134874756:
        if (blockName.equals("listSetCustomViewData")) {
          i = 141;
          break;
        } 
      case 125431087:
        if (blockName.equals("speechToTextStopListening")) {
          i = 307;
          break;
        } 
      case 103668285:
        if (blockName.equals("mathE")) {
          i = 81;
          break;
        } 
      case 97196323:
        if (blockName.equals("false")) {
          i = 42;
          break;
        } 
      case 94001407:
        if (blockName.equals("break")) {
          i = 37;
          break;
        } 
      case 61585857:
        if (blockName.equals("firebasePush")) {
          i = 236;
          break;
        } 
      case 56167279:
        if (blockName.equals("setBitmapFileContrast")) {
          i = 297;
          break;
        } 
      case 27679870:
        if (blockName.equals("calendarGetNow")) {
          i = 191;
          break;
        } 
      case 25469951:
        if (blockName.equals("bluetoothConnectActivateBluetooth")) {
          i = 317;
          break;
        } 
      case 16308074:
        if (blockName.equals("resizeBitmapFileToCircle")) {
          i = 289;
          break;
        } 
      case 8255701:
        if (blockName.equals("calendarFormat")) {
          i = 194;
          break;
        } 
      case 3569038:
        if (blockName.equals("true")) {
          i = 41;
          break;
        } 
      case 3568674:
        if (blockName.equals("trim")) {
          i = 66;
          break;
        } 
      case 3116345:
        if (blockName.equals("else")) {
          i = 40;
          break;
        } 
      case 109267:
        if (blockName.equals("not")) {
          i = 48;
          break;
        } 
      case 3968:
        if (blockName.equals("||")) {
          i = 47;
          break;
        } 
      case 3357:
        if (blockName.equals("if")) {
          i = 38;
          break;
        } 
      case 1216:
        if (blockName.equals("&&")) {
          i = 46;
          break;
        } 
      case 62:
        if (blockName.equals(">")) {
          i = 45;
          break;
        } 
      case 61:
        if (blockName.equals("=")) {
          i = 44;
          break;
        } 
      case 60:
        if (blockName.equals("<")) {
          i = 43;
          break;
        } 
      case 47:
        if (blockName.equals("/")) {
          i = 52;
          break;
        } 
      case 45:
        if (blockName.equals("-")) {
          i = 50;
          break;
        } 
      case 43:
        if (blockName.equals("+")) {
          i = 49;
          break;
        } 
      case 42:
        if (blockName.equals("*")) {
          i = 51;
          break;
        } 
      case 37:
        if (blockName.equals("%")) {
          i = 53;
          break;
        } 
      case -9742826:
        if (blockName.equals("firebaseGetPushKey")) {
          i = 237;
          break;
        } 
      case -10599306:
        if (blockName.equals("firebaseauthCreateUser")) {
          i = 240;
          break;
        } 
      case -14362103:
        if (blockName.equals("bluetoothConnectIsBluetoothActivated")) {
          i = 316;
          break;
        } 
      case -24451690:
        if (blockName.equals("dialogOkButton")) {
          i = 204;
          break;
        } 
      case -60048101:
        if (blockName.equals("firebaseauthResetPassword")) {
          i = 243;
          break;
        } 
      case -60494417:
        if (blockName.equals("vibratorAction")) {
          i = 198;
          break;
        } 
      case -75125341:
        if (blockName.equals("getText")) {
          i = 111;
          break;
        } 
      case -83186725:
        if (blockName.equals("openDrawer")) {
          i = 103;
          break;
        } 
      case -83301935:
        if (blockName.equals("webViewZoomIn")) {
          i = 162;
          break;
        } 
      case -96303809:
        if (blockName.equals("containListStr")) {
          i = 26;
          break;
        } 
      case -96310166:
        if (blockName.equals("containListMap")) {
          i = 31;
          break;
        } 
      case -96313603:
        if (blockName.equals("containListInt")) {
          i = 20;
          break;
        } 
      case -133532073:
        if (blockName.equals("stringLength")) {
          i = 55;
          break;
        } 
      case -149850417:
        if (blockName.equals("fileutilisexist")) {
          i = 268;
          break;
        } 
      case -152473824:
        if (blockName.equals("firebaseauthIsLoggedIn")) {
          i = 244;
          break;
        } 
      case -189292433:
        if (blockName.equals("stringSub")) {
          i = 59;
          break;
        } 
      case -208762465:
        if (blockName.equals("toStringWithDecimal")) {
          i = 70;
          break;
        } 
      case -247015294:
        if (blockName.equals("mediaplayerRelease")) {
          i = 219;
          break;
        } 
      case -258774775:
        if (blockName.equals("closeDrawer")) {
          i = 104;
          break;
        } 
      case -283328259:
        if (blockName.equals("intentPutExtra")) {
          i = 183;
          break;
        } 
      case -322651344:
        if (blockName.equals("stringEquals")) {
          i = 60;
          break;
        } 
      case -329552966:
        if (blockName.equals("insertListStr")) {
          i = 23;
          break;
        } 
      case -329559323:
        if (blockName.equals("insertListMap")) {
          i = 28;
          break;
        } 
      case -329562760:
        if (blockName.equals("insertListInt")) {
          i = 15;
          break;
        } 
      case -353129373:
        if (blockName.equals("calendarDiff")) {
          i = 195;
          break;
        } 
      case -356866884:
        if (blockName.equals("webViewSetCacheMode")) {
          i = 154;
          break;
        } 
      case -390304998:
        if (blockName.equals("mapViewAddMarker")) {
          i = 174;
          break;
        } 
      case -399551817:
        if (blockName.equals("toUpperCase")) {
          i = 67;
          break;
        } 
      case -411705840:
        if (blockName.equals("fileSetData")) {
          i = 189;
          break;
        } 
      case -418212114:
        if (blockName.equals("firebaseGetChildren")) {
          i = 239;
          break;
        } 
      case -425293664:
        if (blockName.equals("setClickable")) {
          i = 108;
          break;
        } 
      case -437272040:
        if (blockName.equals("bluetoothConnectGetRandomUuid")) {
          i = 319;
          break;
        } 
      case -439342016:
        if (blockName.equals("webViewClearHistory")) {
          i = 160;
          break;
        } 
      case -509946902:
        if (blockName.equals("spnRefresh")) {
          i = 149;
          break;
        } 
      case -578987803:
        if (blockName.equals("setChecked")) {
          i = 136;
          break;
        } 
      case -601804268:
        if (blockName.equals("fileutilread")) {
          i = 266;
          break;
        } 
      case -601942961:
        if (blockName.equals("fileutilmove")) {
          i = 267;
          break;
        } 
      case -602241037:
        if (blockName.equals("fileutilcopy")) {
          i = 264;
          break;
        } 
      case -621198621:
        if (blockName.equals("speechToTextStartListening")) {
          i = 306;
          break;
        } 
      case -628607128:
        if (blockName.equals("webViewGoBack")) {
          i = 157;
          break;
        } 
      case -636363854:
        if (blockName.equals("webViewGetUrl")) {
          i = 153;
          break;
        } 
      case -649691581:
        if (blockName.equals("objectanimatorSetInterpolator")) {
          i = 231;
          break;
        } 
      case -664474111:
        if (blockName.equals("intentSetFlags")) {
          i = 184;
          break;
        } 
      case -668992194:
        if (blockName.equals("stringReplaceAll")) {
          i = 64;
          break;
        } 
      case -677662361:
        if (blockName.equals("forever")) {
          i = 36;
          break;
        } 
      case -697616870:
        if (blockName.equals("camerastarttakepicture")) {
          i = 258;
          break;
        } 
      case -733318734:
        if (blockName.equals("strToListMap")) {
          i = 75;
          break;
        } 
      case -831887360:
        if (blockName.equals("textToSpeechShutdown")) {
          i = 305;
          break;
        } 
      case -853550561:
        if (blockName.equals("timerCancel")) {
          i = 201;
          break;
        } 
      case -854558288:
        if (blockName.equals("setVisible")) {
          i = 107;
          break;
        } 
      case -869293886:
        if (blockName.equals("finishActivity")) {
          i = 187;
          break;
        } 
      case -883988307:
        if (blockName.equals("dialogSetMessage")) {
          i = 203;
          break;
        } 
      case -903177036:
        if (blockName.equals("resizeBitmapFileWithRoundedBorder")) {
          i = 290;
          break;
        } 
      case -911199919:
        if (blockName.equals("objectanimatorSetProperty")) {
          i = 225;
          break;
        } 
      case -917343271:
        if (blockName.equals("getJpegRotate")) {
          i = 298;
          break;
        } 
      case -918173448:
        if (blockName.equals("listGetCheckedPosition")) {
          i = 144;
          break;
        } 
      case -934531685:
        if (blockName.equals("repeat")) {
          i = 35;
          break;
        } 
      case -938285885:
        if (blockName.equals("random")) {
          i = 54;
          break;
        } 
      case -995908985:
        if (blockName.equals("soundpoolCreate")) {
          i = 220;
          break;
        } 
      case -996870276:
        if (blockName.equals("insertMapToList")) {
          i = 33;
          break;
        } 
      case -1007787615:
        if (blockName.equals("mediaplayerSetLooping")) {
          i = 216;
          break;
        } 
      case -1021852352:
        if (blockName.equals("objectanimatorCancel")) {
          i = 233;
          break;
        } 
      case -1033658254:
        if (blockName.equals("mathGetDisplayWidth")) {
          i = 78;
          break;
        } 
      case -1043233275:
        if (blockName.equals("mediaplayerGetDuration")) {
          i = 214;
          break;
        } 
      case -1063598745:
        if (blockName.equals("resizeBitmapFileRetainRatio")) {
          i = 287;
          break;
        } 
      case -1081250015:
        if (blockName.equals("mathPi")) {
          i = 80;
          break;
        } 
      case -1081391085:
        if (blockName.equals("mapPut")) {
          i = 6;
          break;
        } 
      case -1081400230:
        if (blockName.equals("mapGet")) {
          i = 7;
          break;
        } 
      case -1094491139:
        if (blockName.equals("seekBarSetMax")) {
          i = 282;
          break;
        } 
      case -1106141754:
        if (blockName.equals("webViewCanGoBack")) {
          i = 155;
          break;
        } 
      case -1107376988:
        if (blockName.equals("webViewGoForward")) {
          i = 158;
          break;
        } 
      case -1123431291:
        if (blockName.equals("calnedarViewSetMaxDate")) {
          i = 167;
          break;
        } 
      case -1137582698:
        if (blockName.equals("toLowerCase")) {
          i = 68;
          break;
        } 
      case -1139353316:
        if (blockName.equals("setListMap")) {
          i = 30;
          break;
        } 
      case -1143684675:
        if (blockName.equals("firebaseauthGetCurrentUser")) {
          i = 245;
          break;
        } 
      case -1149458632:
        if (blockName.equals("objectanimatorSetRepeatCount")) {
          i = 230;
          break;
        } 
      case -1149848189:
        if (blockName.equals("toStringFormat")) {
          i = 71;
          break;
        } 
      case -1160374245:
        if (blockName.equals("bluetoothConnectReadyConnectionToUuid")) {
          i = 310;
          break;
        } 
      case -1182878167:
        if (blockName.equals("firebaseauthGetUid")) {
          i = 246;
          break;
        } 
      case -1185284274:
        if (blockName.equals("gyroscopeStopListen")) {
          i = 251;
          break;
        } 
      case -1192544266:
        if (blockName.equals("ifElse")) {
          i = 39;
          break;
        } 
      case -1195899442:
        if (blockName.equals("bluetoothConnectSendData")) {
          i = 314;
          break;
        } 
      case -1206794098:
        if (blockName.equals("getLocationY")) {
          i = 135;
          break;
        } 
      case -1206794099:
        if (blockName.equals("getLocationX")) {
          i = 134;
          break;
        } 
      case -1217704075:
        if (blockName.equals("objectanimatorSetValue")) {
          i = 226;
          break;
        } 
      case -1271141237:
        if (blockName.equals("clearList")) {
          i = 21;
          break;
        } 
      case -1272546178:
        if (blockName.equals("dialogSetTitle")) {
          i = 202;
          break;
        } 
      case -1304067438:
        if (blockName.equals("firebaseDelete")) {
          i = 238;
          break;
        } 
      case -1348084945:
        if (blockName.equals("mapViewZoomTo")) {
          i = 171;
          break;
        } 
      case -1348085287:
        if (blockName.equals("mapViewZoomIn")) {
          i = 172;
          break;
        } 
      case -1361468284:
        if (blockName.equals("viewOnClick")) {
          i = 101;
          break;
        } 
      case -1376608975:
        if (blockName.equals("calendarSetTime")) {
          i = 197;
          break;
        } 
      case -1377080719:
        if (blockName.equals("decreaseInt")) {
          i = 3;
          break;
        } 
      case -1384851894:
        if (blockName.equals("getAtListStr")) {
          i = 24;
          break;
        } 
      case -1384858251:
        if (blockName.equals("getAtListMap")) {
          i = 29;
          break;
        } 
      case -1384861688:
        if (blockName.equals("getAtListInt")) {
          i = 17;
          break;
        } 
      case -1385076635:
        if (blockName.equals("dialogShow")) {
          i = 207;
          break;
        } 
      case -1405157727:
        if (blockName.equals("fileutilmakedir")) {
          i = 269;
          break;
        } 
      case -1422112391:
        if (blockName.equals("bluetoothConnectIsBluetoothEnabled")) {
          i = 315;
          break;
        } 
      case -1438040951:
        if (blockName.equals("seekBarGetMax")) {
          i = 280;
          break;
        } 
      case -1440042085:
        if (blockName.equals("spnSetSelection")) {
          i = 150;
          break;
        } 
      case -1462744030:
        if (blockName.equals("dialogDismiss")) {
          i = 208;
          break;
        } 
      case -1471049951:
        if (blockName.equals("fileutilwrite")) {
          i = 265;
          break;
        } 
      case -1477942289:
        if (blockName.equals("mediaplayerIsLooping")) {
          i = 217;
          break;
        } 
      case -1483954587:
        if (blockName.equals("fileutilisdir")) {
          i = 271;
          break;
        } 
      case -1513446476:
        if (blockName.equals("dialogCancelButton")) {
          i = 205;
          break;
        } 
      case -1526161572:
        if (blockName.equals("setBgColor")) {
          i = 112;
          break;
        } 
      case -1528850031:
        if (blockName.equals("startActivity")) {
          i = 185;
          break;
        } 
      case -1530840255:
        if (blockName.equals("stringIndex")) {
          i = 57;
          break;
        } 
      case -1541653284:
        if (blockName.equals("objectanimatorStart")) {
          i = 232;
          break;
        } 
      case -1573371685:
        if (blockName.equals("stringJoin")) {
          i = 56;
          break;
        } 
      case -1666623936:
        if (blockName.equals("speechToTextShutdown")) {
          i = 308;
          break;
        } 
      case -1679834825:
        if (blockName.equals("setTrackResource")) {
          i = 139;
          break;
        } 
      case -1684072208:
        if (blockName.equals("intentSetData")) {
          i = 181;
          break;
        } 
      case -1699349926:
        if (blockName.equals("objectanimatorSetRepeatMode")) {
          i = 229;
          break;
        } 
      case -1699631195:
        if (blockName.equals("isDrawerOpen")) {
          i = 102;
          break;
        } 
      case -1718917155:
        if (blockName.equals("mediaplayerSeek")) {
          i = 212;
          break;
        } 
      case -1746380899:
        if (blockName.equals("mediaplayerStart")) {
          i = 210;
          break;
        } 
      case -1747734390:
        if (blockName.equals("mediaplayerReset")) {
          i = 218;
          break;
        } 
      case -1749698255:
        if (blockName.equals("mediaplayerPause")) {
          i = 211;
          break;
        } 
      case -1776922004:
        if (blockName.equals("toString")) {
          i = 69;
          break;
        } 
      case -1778201036:
        if (blockName.equals("listSmoothScrollTo")) {
          i = 147;
          break;
        } 
      case -1812313351:
        if (blockName.equals("setColorFilter")) {
          i = 119;
          break;
        } 
      case -1834369666:
        if (blockName.equals("setBitmapFileBrightness")) {
          i = 296;
          break;
        } 
      case -1886802639:
        if (blockName.equals("soundpoolLoad")) {
          i = 221;
          break;
        } 
      case -1910071024:
        if (blockName.equals("objectanimatorSetDuration")) {
          i = 228;
          break;
        } 
      case -1919300188:
        if (blockName.equals("toNumber")) {
          i = 65;
          break;
        } 
      case -1920517885:
        if (blockName.equals("setVarBoolean")) {
          i = 0;
          break;
        } 
      case -1922362317:
        if (blockName.equals("getExternalStorageDir")) {
          i = 284;
          break;
        } 
      case -1937348542:
        if (blockName.equals("firebaseStartListen")) {
          i = 248;
          break;
        } 
      case -1966668787:
        if (blockName.equals("firebaseauthSignOutUser")) {
          i = 247;
          break;
        } 
      case -1975568730:
        if (blockName.equals("copyToClipboard")) {
          i = 278;
          break;
        } 
      case -1979147952:
        if (blockName.equals("stringContains")) {
          i = 61;
          break;
        } 
      case -1989678633:
        if (blockName.equals("mapViewSetMarkerVisible")) {
          i = 179;
          break;
        } 
      case -1998407506:
        if (blockName.equals("listSetData")) {
          i = 140;
          break;
        } 
      case -2020761366:
        if (blockName.equals("fileRemoveData")) {
          i = 190;
          break;
        } 
      case -2027093331:
        if (blockName.equals("calendarViewSetDate")) {
          i = 165;
          break;
        } 
      case -2037144358:
        if (blockName.equals("bluetoothConnectStartConnectionToUuid")) {
          i = 312;
          break;
        } 
      case -2055793167:
        if (blockName.equals("fileutillistdir")) {
          i = 270;
          break;
        } 
      case -2114384168:
        if (blockName.equals("firebasestorageDownloadFile")) {
          i = 256;
          break;
        } 
      case -2120571577:
        if (blockName.equals("mapIsEmpty")) {
          i = 12;
          break;
        } 
      case -2135695280:
        if (blockName.equals("webViewLoadUrl")) {
          i = 152;
          break;
        } 
    } 
    blockName = defaultValue;
    switch (i) {
      default:
        blockName = "";
        break;
      case 321:
        blockName = "locationmanager_remove_updates";
        break;
      case 320:
        blockName = "locationmanager_request_location_updates";
        break;
      case 319:
        blockName = "bluetoothconnect_get_random_uuid";
        break;
      case 318:
        blockName = "bluetoothconnect_get_paired_devices";
        break;
      case 317:
        blockName = "bluetoothconnect_activate_bluetooth";
        break;
      case 316:
        blockName = "bluetoothconnect_is_bluetooth_activated";
        break;
      case 315:
        blockName = "bluetoothconnect_is_bluetooth_enabled";
        break;
      case 314:
        blockName = "bluetoothconnect_send_data";
        break;
      case 313:
        blockName = "bluetoothconnect_stop_connection";
        break;
      case 312:
        blockName = "bluetoothconnect_start_connection_to_uuid";
        break;
      case 311:
        blockName = "bluetoothconnect_start_connection";
        break;
      case 310:
        blockName = "bluetoothconnect_ready_connection_to_uuid";
        break;
      case 309:
        blockName = "bluetoothconnect_ready_connection";
        break;
      case 308:
        blockName = "speechtotext_shutdown";
        break;
      case 307:
        blockName = "speechtotext_stop_listening";
        break;
      case 306:
        blockName = "speechtotext_start_listening";
        break;
      case 305:
        blockName = "texttospeech_shutdown";
        break;
      case 304:
        blockName = "texttospeech_stop";
        break;
      case 303:
        blockName = "texttospeech_is_speaking";
        break;
      case 302:
        blockName = "texttospeech_speak";
        break;
      case 301:
        blockName = "texttospeech_set_speech_rate";
        break;
      case 300:
        blockName = "texttospeech_set_pitch";
        break;
      case 299:
        blockName = "progressbar_set_indeterminate";
        break;
      case 298:
        blockName = "get_jpeg_rotate";
        break;
      case 297:
        blockName = "set_bitmap_contrast";
        break;
      case 296:
        blockName = "set_bitmap_brightness";
        break;
      case 295:
        blockName = "set_bitmap_color_filter";
        break;
      case 294:
        blockName = "skew_bitmap";
        break;
      case 293:
        blockName = "scale_bitmap";
        break;
      case 292:
        blockName = "rotate_bitmap";
        break;
      case 291:
        blockName = "crop_bitmap_center";
        break;
      case 290:
        blockName = "resize_bitmap_rounded";
        break;
      case 289:
        blockName = "resize_bitmap_circle";
        break;
      case 288:
        blockName = "resize_bitmap_square";
        break;
      case 287:
        blockName = "resize_bitmap_ratio";
        break;
      case 286:
        blockName = "get_public_dir";
        break;
      case 285:
        blockName = "get_package_data_dir";
        break;
      case 284:
        blockName = "get_external_storage_dir";
        break;
      case 283:
        blockName = "seekbar_set_progress";
        break;
      case 282:
        blockName = "seekbar_set_max";
        break;
      case 281:
        blockName = "seekbar_get_progress";
        break;
      case 280:
        blockName = "seekbar_get_max";
        break;
      case 279:
        blockName = "set_title";
        break;
      case 278:
        blockName = "copy_to_clipboard";
        break;
      case 277:
        blockName = "do_toast";
        break;
      case 276:
        blockName = "fileutil_get_last_segment_path";
        break;
      case 275:
        blockName = "fileutil_ends_with";
        break;
      case 274:
        blockName = "fileutil_starts_with";
        break;
      case 273:
        blockName = "fileutil_length";
        break;
      case 272:
        blockName = "fileutil_is_file";
        break;
      case 271:
        blockName = "fileutil_is_dir";
        break;
      case 270:
        blockName = "fileutil_list_dir";
        break;
      case 269:
        blockName = "fileutil_make_dir";
        break;
      case 268:
        blockName = "fileutil_is_exist";
        break;
      case 267:
        blockName = "fileutil_move";
        break;
      case 266:
        blockName = "fileutil_read";
        break;
      case 265:
        blockName = "fileutil_write";
        break;
      case 264:
        blockName = "fileutil_copy";
        break;
      case 263:
        blockName = "fileutil_delete";
        break;
      case 262:
        blockName = "requestnetwork_start_request_network";
        break;
      case 261:
        blockName = "requestnetwork_set_headers";
        break;
      case 260:
        blockName = "requestnetwork_set_params";
        break;
      case 259:
        blockName = "file_picker_start_pick_files";
        break;
      case 258:
        blockName = "camera_start_take_picture";
        break;
      case 257:
        blockName = "firebasestorage_delete";
        break;
      case 256:
        blockName = "firebasestorage_download_file";
        break;
      case 255:
        blockName = "firebasestorage_upload_file";
        break;
      case 254:
        blockName = "interstitialad_show";
        break;
      case 253:
        blockName = "interstitialad_load_ad";
        break;
      case 252:
        blockName = "interstitialad_create";
        break;
      case 251:
        blockName = "gyroscope_stop_listen";
        break;
      case 250:
        blockName = "gyroscope_start_listen";
        break;
      case 249:
        blockName = "firebase_stop_listen";
        break;
      case 248:
        blockName = "firebase_start_listen";
        break;
      case 247:
        blockName = "firebaseauth_signout";
        break;
      case 246:
        blockName = "firebaseauth_get_uid";
        break;
      case 245:
        blockName = "firebaseauth_get_email";
        break;
      case 244:
        blockName = "firebaseauth_is_logged_in";
        break;
      case 243:
        blockName = "firebaseauth_reset_password";
        break;
      case 242:
        blockName = "firebaseauth_signin_anonymously";
        break;
      case 241:
        blockName = "firebaseauth_signin_user";
        break;
      case 240:
        blockName = "firebaseauth_create_user";
        break;
      case 239:
        blockName = "firebase_get_children";
        break;
      case 238:
        blockName = "firebase_delete";
        break;
      case 237:
        blockName = "firebase_get_key";
        break;
      case 236:
        blockName = "firebase_push";
        break;
      case 235:
        blockName = "firebase_add";
        break;
      case 234:
        blockName = "objectanimator_is_running";
        break;
      case 233:
        blockName = "objectanimator_cancel";
        break;
      case 232:
        blockName = "objectanimator_start";
        break;
      case 231:
        blockName = "objectanimator_set_interpolator";
        break;
      case 230:
        blockName = "objectanimator_set_repeat_count";
        break;
      case 229:
        blockName = "objectanimator_set_repeat_mode";
        break;
      case 228:
        blockName = "objectanimator_set_duration";
        break;
      case 227:
        blockName = "objectanimator_set_from_to";
        break;
      case 226:
        blockName = "objectanimator_set_value";
        break;
      case 225:
        blockName = "objectanimator_set_property";
        break;
      case 224:
        blockName = "objectanimator_set_target";
        break;
      case 223:
        blockName = "soundpool_stream_stop";
        break;
      case 222:
        blockName = "soundpool_stream_play";
        break;
      case 221:
        blockName = "soundpool_load";
        break;
      case 220:
        blockName = "soundpool_create";
        break;
      case 219:
        blockName = "mediaplayer_release";
        break;
      case 218:
        blockName = "mediaplayer_reset";
        break;
      case 217:
        blockName = "mediaplayer_is_looping";
        break;
      case 216:
        blockName = "mediaplayer_set_looping";
        break;
      case 215:
        blockName = "mediaplayer_is_playing";
        break;
      case 214:
        blockName = "mediaplayer_get_duration";
        break;
      case 213:
        blockName = "mediaplayer_get_current";
        break;
      case 212:
        blockName = "mediaplayer_seek";
        break;
      case 211:
        blockName = "mediaplayer_pause";
        break;
      case 210:
        blockName = "mediaplayer_start";
        break;
      case 209:
        blockName = "mediaplayer_create";
        break;
      case 208:
        blockName = "dialog_dismiss";
        break;
      case 207:
        blockName = "dialog_show";
        break;
      case 206:
        blockName = "dialog_neutral_button";
        break;
      case 205:
        blockName = "dialog_cancel_button";
        break;
      case 204:
        blockName = "dialog_ok_button";
        break;
      case 203:
        blockName = "dialog_set_message";
        break;
      case 202:
        blockName = "dialog_set_title";
        break;
      case 201:
        blockName = "timer_cancel";
        break;
      case 200:
        blockName = "timer_every";
        break;
      case 199:
        blockName = "timer_after";
        break;
      case 198:
        blockName = "vibrator_action";
        break;
      case 197:
        blockName = "calendar_set_time";
        break;
      case 196:
        blockName = "calendar_get_time";
        break;
      case 195:
        blockName = "calendar_diff";
        break;
      case 194:
        blockName = "calendar_format";
        break;
      case 193:
        blockName = "calendar_set";
        break;
      case 192:
        blockName = "calendar_add";
        break;
      case 191:
        blockName = "calendar_get_now";
        break;
      case 190:
        blockName = "file_remove_data";
        break;
      case 189:
        blockName = "file_set_data";
        break;
      case 188:
        blockName = "file_get_data";
        break;
      case 187:
        blockName = "finish_activity";
        break;
      case 186:
        blockName = "intent_get_string";
        break;
      case 185:
        blockName = "start_activity";
        break;
      case 184:
        blockName = "intent_set_flags";
        break;
      case 183:
        blockName = "intent_put_extra";
        break;
      case 182:
        blockName = "intent_set_screen";
        break;
      case 181:
        blockName = "intent_set_data";
        break;
      case 180:
        blockName = "intent_set_action";
        break;
      case 179:
        blockName = "mapview_set_marker_visible";
        break;
      case 178:
        blockName = "mapview_set_marker_icon";
        break;
      case 177:
        blockName = "mapview_set_marker_color";
        break;
      case 176:
        blockName = "mapview_set_marker_position";
        break;
      case 175:
        blockName = "mapview_set_marker_info";
        break;
      case 174:
        blockName = "mapview_add_marker";
        break;
      case 173:
        blockName = "mapview_zoom_out";
        break;
      case 172:
        blockName = "mapview_zoom_in";
        break;
      case 171:
        blockName = "mapview_zoom_to";
        break;
      case 170:
        blockName = "mapview_move_camera";
        break;
      case 169:
        blockName = "mapview_set_map_type";
        break;
      case 168:
        blockName = "adview_load_ad";
        break;
      case 167:
        blockName = "calendarview_set_max_date";
        break;
      case 166:
        blockName = "calendarview_set_min_date";
        break;
      case 165:
        blockName = "calendarview_set_date";
        break;
      case 164:
        blockName = "calendarview_get_date";
        break;
      case 163:
        blockName = "webview_zoom_out";
        break;
      case 162:
        blockName = "webview_zoom_in";
        break;
      case 161:
        blockName = "webview_stop_loading";
        break;
      case 160:
        blockName = "webview_clear_history";
        break;
      case 159:
        blockName = "webview_clear_cache";
        break;
      case 158:
        blockName = "webview_go_forward";
        break;
      case 157:
        blockName = "webview_go_back";
        break;
      case 156:
        blockName = "webview_can_go_forward";
        break;
      case 155:
        blockName = "webview_can_go_back";
        break;
      case 154:
        blockName = "webview_set_cache_mode";
        break;
      case 153:
        blockName = "webview_get_url";
        break;
      case 152:
        blockName = "webview_load_url";
        break;
      case 151:
        blockName = "spn_get_selection";
        break;
      case 150:
        blockName = "spn_set_selection";
        break;
      case 149:
        blockName = "spn_refresh";
        break;
      case 148:
        blockName = "spn_set_data";
        break;
      case 147:
        blockName = "list_smooth_scrollto";
        break;
      case 146:
        blockName = "list_get_checked_count";
        break;
      case 145:
        blockName = "list_get_checked_positions";
        break;
      case 144:
        blockName = "list_get_checked_position";
        break;
      case 143:
        blockName = "list_set_item_checked";
        break;
      case 142:
        blockName = "list_refresh";
        break;
      case 141:
        blockName = "list_set_custom_view_data";
        break;
      case 140:
        blockName = "list_set_data";
        break;
      case 139:
        blockName = "set_track_resource";
        break;
      case 138:
        blockName = "set_thumb_resource";
        break;
      case 137:
        blockName = "get_checked";
        break;
      case 136:
        blockName = "set_checked";
        break;
      case 135:
        blockName = "get_location_y";
        break;
      case 134:
        blockName = "get_location_x";
        break;
      case 133:
        blockName = "get_scale_y";
        break;
      case 132:
        blockName = "set_scale_y";
        break;
      case 131:
        blockName = "get_scale_x";
        break;
      case 130:
        blockName = "set_scale_x";
        break;
      case 129:
        blockName = "get_translation_y";
        break;
      case 128:
        blockName = "set_translation_y";
        break;
      case 127:
        blockName = "get_translation_x";
        break;
      case 126:
        blockName = "set_translation_x";
        break;
      case 125:
        blockName = "get_alpha";
        break;
      case 124:
        blockName = "set_alpha";
        break;
      case 123:
        blockName = "get_rotate";
        break;
      case 122:
        blockName = "set_rotate";
        break;
      case 121:
        blockName = "set_image_url";
        break;
      case 120:
        blockName = "set_image_file_path";
        break;
      case 119:
        blockName = "set_color_filter";
        break;
      case 118:
        blockName = "set_image";
        break;
      case 117:
        blockName = "request_focus";
        break;
      case 116:
        blockName = "set_hint_text_color";
        break;
      case 115:
        blockName = "set_hint";
        break;
      case 114:
        blockName = "set_text_color";
        break;
      case 113:
        blockName = "set_bg_resource";
        break;
      case 112:
        blockName = "set_bg_color";
        break;
      case 111:
        blockName = "get_text";
        break;
      case 110:
        blockName = "set_typeface";
        break;
      case 109:
        blockName = "set_text";
        break;
      case 108:
        blockName = "set_clickable";
        break;
      case 107:
        blockName = "set_visible";
        break;
      case 106:
        blockName = "get_enable";
        break;
      case 105:
        blockName = "set_enable";
        break;
      case 104:
        blockName = "close_drawer";
        break;
      case 103:
        blockName = "open_drawer";
        break;
      case 102:
        blockName = "is_drawer_open";
        break;
      case 101:
        blockName = "view_on_click";
        break;
      case 100:
        blockName = "math_to_degree";
        break;
      case 99:
        blockName = "math_to_radian";
        break;
      case 98:
        blockName = "math_log10";
        break;
      case 97:
        blockName = "math_log";
        break;
      case 96:
        blockName = "math_exp";
        break;
      case 95:
        blockName = "math_atan";
        break;
      case 94:
        blockName = "math_acos";
        break;
      case 93:
        blockName = "math_asin";
        break;
      case 92:
        blockName = "math_tan";
        break;
      case 91:
        blockName = "math_cos";
        break;
      case 90:
        blockName = "math_sin";
        break;
      case 89:
        blockName = "math_floor";
        break;
      case 88:
        blockName = "math_ceil";
        break;
      case 87:
        blockName = "math_round";
        break;
      case 86:
        blockName = "math_abs";
        break;
      case 85:
        blockName = "math_sqrt";
        break;
      case 84:
        blockName = "math_max";
        break;
      case 83:
        blockName = "math_min";
        break;
      case 82:
        blockName = "math_pow";
        break;
      case 81:
        blockName = "math_e";
        break;
      case 80:
        blockName = "math_pi";
        break;
      case 79:
        blockName = "math_get_display_height";
        break;
      case 78:
        blockName = "math_get_display_width";
        break;
      case 77:
        blockName = "math_get_dip";
        break;
      case 76:
        blockName = "list_map_to_str";
        break;
      case 75:
        blockName = "str_to_list_map";
        break;
      case 74:
        blockName = "map_to_str";
        break;
      case 73:
        blockName = "str_to_map";
        break;
      case 72:
        blockName = "add_source_directly";
        break;
      case 71:
        blockName = "to_string_format";
        break;
      case 70:
        blockName = "to_string_with_decimal";
        break;
      case 69:
        blockName = "to_string";
        break;
      case 68:
        blockName = "to_lower_case";
        break;
      case 67:
        blockName = "to_upper_case";
        break;
      case 66:
        blockName = "trim";
        break;
      case 65:
        blockName = "to_number";
        break;
      case 64:
        blockName = "string_replace_all";
        break;
      case 63:
        blockName = "string_replace_first";
        break;
      case 62:
        blockName = "string_replace";
        break;
      case 61:
        blockName = "string_contains";
        break;
      case 60:
        blockName = "string_equals";
        break;
      case 59:
        blockName = "string_sub";
        break;
      case 58:
        blockName = "string_last_index";
        break;
      case 57:
        blockName = "string_index";
        break;
      case 56:
        blockName = "string_join";
        break;
      case 55:
        blockName = "string_length";
        break;
      case 54:
        blockName = "random";
        break;
      case 53:
        blockName = "rest";
        break;
      case 52:
        blockName = "divide";
        break;
      case 51:
        blockName = "times";
        break;
      case 50:
        blockName = "minus";
        break;
      case 49:
        blockName = "plus";
        break;
      case 48:
        blockName = "not";
        break;
      case 47:
        blockName = "or";
        break;
      case 46:
        blockName = "and";
        break;
      case 45:
        blockName = "bigger";
        break;
      case 44:
        blockName = "equal";
        break;
      case 43:
        blockName = "smaller";
        break;
      case 41:
        blockName = "true";
        break;
      case 40:
        blockName = "else";
        break;
      case 39:
        blockName = "if_else";
        break;
      case 38:
        blockName = "if";
        break;
      case 37:
        blockName = "break";
        break;
      case 36:
        blockName = "forever";
        break;
      case 35:
        blockName = "repeat";
        break;
      case 34:
        blockName = "get_map_in_list";
        break;
      case 33:
        blockName = "insert_map_to_list";
        break;
      case 32:
        blockName = "add_map_to_list";
        break;
      case 31:
        blockName = "contain_list_map";
        break;
      case 30:
        blockName = "set_at_list_map";
        break;
      case 29:
        blockName = "get_at_list_map";
        break;
      case 28:
        blockName = "insert_list_map";
        break;
      case 27:
        blockName = "add_list_map";
        break;
      case 26:
        blockName = "contain_list_str";
        break;
      case 25:
        blockName = "indexof_list_str";
        break;
      case 24:
        blockName = "get_at_list_str";
        break;
      case 23:
        blockName = "insert_list_str";
        break;
      case 22:
        blockName = "add_list_str";
        break;
      case 21:
        blockName = "clear_list";
        break;
      case 20:
        blockName = "contain_list_int";
        break;
      case 19:
        blockName = "length_list";
        break;
      case 18:
        blockName = "indexof_list_int";
        break;
      case 17:
        blockName = "get_at_list_int";
        break;
      case 16:
        blockName = "delete_list";
        break;
      case 15:
        blockName = "insert_list_int";
        break;
      case 14:
        blockName = "add_list_int";
        break;
      case 13:
        blockName = "map_get_all_keys";
        break;
      case 12:
        blockName = "map_is_empty";
        break;
      case 11:
        blockName = "map_clear";
        break;
      case 10:
        blockName = "map_size";
        break;
      case 9:
        blockName = "map_remove_key";
        break;
      case 8:
        blockName = "map_contain_key";
        break;
      case 7:
        blockName = "map_get";
        break;
      case 6:
        blockName = "map_put";
        break;
      case 5:
        blockName = "map_create_new";
        break;
      case 4:
        blockName = "set_var_str";
        break;
      case 3:
        blockName = "decrease_int";
        break;
      case 2:
        blockName = "increase_int";
        break;
      case 1:
        blockName = "set_var_int";
        break;
      case 0:
        blockName = "set_var_bool";
        break;
      case 42:
        break;
    } 
    cacheD.put(originalKey, blockName);
    return blockName;
  }
}
