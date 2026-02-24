package pro.sketchware.core;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.DisplayMetrics;
import com.google.android.gms.common.GoogleApiAvailability;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Locale;

public class DeviceUtil {
  public static int getToolbarHeight(Context paramContext) {
    return (int)ViewUtil.dpToPx(paramContext, 48.0F);
  }
  
  public static String getCpuAbi() {
    String str = "";
    if (Build.VERSION.SDK_INT >= 21) {
      String[] arrayOfString = Build.SUPPORTED_ABIS;
      if (arrayOfString != null && arrayOfString.length > 0) {
        str = arrayOfString[0];
      } 
    } else {
      str = Build.CPU_ABI;
    } 
    return str;
  }
  
  public static void updateBadgeCount(Context paramContext, int paramInt) {
    String str = paramContext.getPackageManager().getLaunchIntentForPackage(paramContext.getPackageName()).getComponent().getClassName();
    Intent intent = new Intent("android.intent.action.BADGE_COUNT_UPDATE");
    intent.putExtra("badge_count", paramInt);
    intent.putExtra("badge_count_package_name", paramContext.getPackageName());
    intent.putExtra("badge_count_class_name", str);
    paramContext.sendBroadcast(intent);
  }
  
  public static boolean isGooglePlayAvailable(Activity paramActivity) {
    GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
    int i = googleApiAvailability.isGooglePlayServicesAvailable((Context)paramActivity);
    if (i != 0) {
      googleApiAvailability.isUserResolvableError(i);
      return false;
    } 
    return true;
  }
  
  public static String getAndroidVersionName() {
    Field[] arrayOfField = Build.VERSION_CODES.class.getFields();
    int i = arrayOfField.length;
    int b = 0;
    while (true) {
      if (b < i) {
        Field field = arrayOfField[b];
        String str = field.getName();
        try {
          int j = field.getInt(null);
          int k = Build.VERSION.SDK_INT;
          if (j == k)
            return str; 
          b++;
          continue;
        } catch (Exception exception) {}
      } 
      return "";
    } 
  }
  
  public static String getDeviceId(Context paramContext) {
    return "";
  }
  
  public static boolean hasSensor(Context paramContext, int paramInt) {
    boolean bool;
    if (((SensorManager)paramContext.getSystemService(Context.SENSOR_SERVICE)).getDefaultSensor(paramInt) != null) {
      bool = true;
    } else {
      bool = false;
    } 
    return bool;
  }
  
  public static float[] getScreenDpi(Activity paramActivity) {
    DisplayMetrics displayMetrics = new DisplayMetrics();
    paramActivity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    return new float[] { displayMetrics.xdpi, displayMetrics.ydpi };
  }
  
  public static long getFreeStorageMB() {
    try {
      StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
      return statFs.getFreeBytes() / 1048576L;
    } catch (Exception exception) {
      return -1L;
    } 
  }
  
  public static ArrayList<String> getGoogleAccounts(Context paramContext) {
    ArrayList<String> arrayList = new ArrayList<>();
    Account[] arrayOfAccount = AccountManager.get(paramContext).getAccountsByType("com.google");
    for (int b = 0; b < arrayOfAccount.length; b++) {
      if ((arrayOfAccount[b]).type.equals("com.google"))
        arrayList.add((arrayOfAccount[b]).name); 
    } 
    return arrayList;
  }
  
  public static int[] getScreenResolution(Activity paramActivity) {
    DisplayMetrics displayMetrics = new DisplayMetrics();
    paramActivity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    return new int[] { displayMetrics.widthPixels, displayMetrics.heightPixels };
  }
  
  public static int getVersionCode(Context paramContext) {
    int i = 0;
    try {
      String str = paramContext.getPackageName();
      int j = (paramContext.getPackageManager().getPackageInfo(str, 0)).versionCode;
      i = j;
    } catch (android.content.pm.PackageManager.NameNotFoundException nameNotFoundException) {
      nameNotFoundException.printStackTrace();
    } 
    return i;
  }
  
  public static String getVersionName(Context paramContext) {
    String str;
    try {
      String str1 = paramContext.getPackageName();
      str = (paramContext.getPackageManager().getPackageInfo(str1, 0)).versionName;
    } catch (android.content.pm.PackageManager.NameNotFoundException nameNotFoundException) {
      nameNotFoundException.printStackTrace();
      str = "";
    } 
    return str;
  }
  
  public static int getStatusBarHeight(Context paramContext) {
    int i = paramContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
    if (i > 0) {
      i = paramContext.getResources().getDimensionPixelSize(i);
    } else {
      i = 0;
    } 
    return i;
  }
  
  public static Locale getLocale(Context paramContext) {
    Locale locale;
    if (Build.VERSION.SDK_INT >= 24) {
      locale = paramContext.getResources().getConfiguration().getLocales().get(0);
    } else {
      locale = (paramContext.getResources().getConfiguration()).locale;
    } 
    return locale;
  }
  
  public static boolean isNetworkAvailable(Context paramContext) {
    int[] arrayOfInt = new int[2];
    arrayOfInt[0] = 0;
    arrayOfInt[1] = 1;
    try {
      ConnectivityManager connectivityManager = (ConnectivityManager)paramContext.getSystemService(Context.CONNECTIVITY_SERVICE);
      int i = arrayOfInt.length;
      for (int b = 0; b < i; b++) {
        int j = arrayOfInt[b];
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null) {
          int k = networkInfo.getType();
          if (k == j)
            return true; 
        } 
      } 
    } catch (Exception exception) {}
    return false;
  }
}
