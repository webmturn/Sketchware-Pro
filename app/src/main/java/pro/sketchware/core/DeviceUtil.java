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
  public static int getToolbarHeight(Context context) {
    return (int)ViewUtil.dpToPx(context, 48.0F);
  }
  
  public static String getCpuAbi() {
    String str = "";
    if (Build.VERSION.SDK_INT >= 21) {
      String[] parts = Build.SUPPORTED_ABIS;
      if (parts != null && parts.length > 0) {
        str = parts[0];
      } 
    } else {
      str = Build.CPU_ABI;
    } 
    return str;
  }
  
  public static void updateBadgeCount(Context context, int count) {
    String str = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName()).getComponent().getClassName();
    Intent intent = new Intent("android.intent.action.BADGE_COUNT_UPDATE");
    intent.putExtra("badge_count", count);
    intent.putExtra("badge_count_package_name", context.getPackageName());
    intent.putExtra("badge_count_class_name", str);
    context.sendBroadcast(intent);
  }
  
  public static boolean isGooglePlayAvailable(Activity activity) {
    GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
    int i = googleApiAvailability.isGooglePlayServicesAvailable((Context)activity);
    if (i != 0) {
      googleApiAvailability.isUserResolvableError(i);
      return false;
    } 
    return true;
  }
  
  public static String getAndroidVersionName() {
    Field[] fields = Build.VERSION_CODES.class.getFields();
    int i = fields.length;
    int b = 0;
    while (true) {
      if (b < i) {
        Field field = fields[b];
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
  
  public static String getDeviceId(Context context) {
    return "";
  }
  
  public static boolean hasSensor(Context context, int sensorType) {
    boolean bool;
    if (((SensorManager)context.getSystemService(Context.SENSOR_SERVICE)).getDefaultSensor(sensorType) != null) {
      bool = true;
    } else {
      bool = false;
    } 
    return bool;
  }
  
  public static float[] getScreenDpi(Activity activity) {
    DisplayMetrics displayMetrics = new DisplayMetrics();
    activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
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
  
  public static ArrayList<String> getGoogleAccounts(Context context) {
    ArrayList<String> arrayList = new ArrayList<>();
    Account[] accounts = AccountManager.get(context).getAccountsByType("com.google");
    for (int b = 0; b < accounts.length; b++) {
      if ((accounts[b]).type.equals("com.google"))
        arrayList.add((accounts[b]).name); 
    } 
    return arrayList;
  }
  
  public static int[] getScreenResolution(Activity activity) {
    DisplayMetrics displayMetrics = new DisplayMetrics();
    activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    return new int[] { displayMetrics.widthPixels, displayMetrics.heightPixels };
  }
  
  public static int getVersionCode(Context context) {
    int versionCode = 0;
    try {
      String packageName = context.getPackageName();
      versionCode = (context.getPackageManager().getPackageInfo(packageName, 0)).versionCode;
    } catch (android.content.pm.PackageManager.NameNotFoundException nameNotFoundException) {
      nameNotFoundException.printStackTrace();
    } 
    return versionCode;
  }
  
  public static String getVersionName(Context context) {
    String versionName;
    try {
      String packageName = context.getPackageName();
      versionName = (context.getPackageManager().getPackageInfo(packageName, 0)).versionName;
    } catch (android.content.pm.PackageManager.NameNotFoundException nameNotFoundException) {
      nameNotFoundException.printStackTrace();
      versionName = "";
    } 
    return versionName;
  }
  
  public static int getStatusBarHeight(Context context) {
    int i = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
    if (i > 0) {
      i = context.getResources().getDimensionPixelSize(i);
    } else {
      i = 0;
    } 
    return i;
  }
  
  public static Locale getLocale(Context context) {
    Locale locale;
    if (Build.VERSION.SDK_INT >= 24) {
      locale = context.getResources().getConfiguration().getLocales().get(0);
    } else {
      locale = (context.getResources().getConfiguration()).locale;
    } 
    return locale;
  }
  
  public static boolean isNetworkAvailable(Context context) {
    int[] intValues = new int[2];
    intValues[0] = 0;
    intValues[1] = 1;
    try {
      ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
      int i = intValues.length;
      for (int b = 0; b < i; b++) {
        int j = intValues[b];
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
