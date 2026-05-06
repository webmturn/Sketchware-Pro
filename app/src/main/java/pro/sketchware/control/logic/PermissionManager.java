package pro.sketchware.control.logic;

import com.besome.sketch.beans.BlockBean;

import java.util.ArrayList;
import java.util.Map.Entry;

import pro.sketchware.core.ActivityCodeGenerator;
import pro.sketchware.core.ProjectDataManager;
import pro.sketchware.core.BuildConfig;

public class PermissionManager {

    private final String javaName;
    private final String sc_id;
    public boolean hasPermission = false;

    public PermissionManager(String id, String javaName) {
        this.javaName = javaName;
        sc_id = id;
    }

    private ArrayList<String> addedPermissions() {
        ArrayList<String> permList = new ArrayList<>();
        for (Entry<String, ArrayList<BlockBean>> blocks : ProjectDataManager.getProjectDataManager(sc_id).getBlockMap(javaName).entrySet()) {
            for (BlockBean block : blocks.getValue()) {
                if (block.opCode.equals("addPermission")) {
                    String firstParam = block.parameters.get(0);
                    if (!firstParam.trim().isEmpty()) {
                        if (firstParam.startsWith("Manifest")) {
                            permList.add(firstParam);
                        } else {
                            permList.add("Manifest.permission." + firstParam);
                        }
                    }
                }
            }
        }
        return permList;
    }

    private static String formatPermission(boolean isAppCompat, String permission) {
        if (isAppCompat) {
            return "ContextCompat.checkSelfPermission(this, " + permission + ") == PackageManager.PERMISSION_DENIED";
        } else {
            return "checkSelfPermission(" + permission + ") == PackageManager.PERMISSION_DENIED";
        }
    }

    private void addReqPermission(ArrayList<PermissionEntry> permissions) {
        for (String permission : addedPermissions()) {
            permissions.add(new PermissionEntry(permission, null));
        }
    }

    private void removePermission(ArrayList<PermissionEntry> permissions) {
        for (Entry<String, ArrayList<BlockBean>> blocks : ProjectDataManager.getProjectDataManager(sc_id).getBlockMap(javaName).entrySet()) {
            for (BlockBean block : blocks.getValue()) {
                if (block.opCode.equals("removePermission") && !block.parameters.get(0).trim().isEmpty()) {
                    String permission = block.parameters.get(0).startsWith("Manifest") ? block.parameters.get(0) : ("Manifest.permission." + block.parameters.get(0));
                    permissions.removeIf(entry -> entry.permission.equals(permission));
                }
            }
        }
    }

    public boolean hasNewPermission() {
        return !addedPermissions().isEmpty();
    }

    private static void addSystemPermissions(ArrayList<PermissionEntry> permissions, int permissionFlags) {
        String targetsSdk33OrHigher = "(getApplicationInfo().targetSdkVersion >= 33)";
        if ((permissionFlags & BuildConfig.PERMISSION_CALL_PHONE) == BuildConfig.PERMISSION_CALL_PHONE) {
            permissions.add(new PermissionEntry("Manifest.permission.CALL_PHONE", null));
        }
        if ((permissionFlags & BuildConfig.PERMISSION_CAMERA) == BuildConfig.PERMISSION_CAMERA) {
            permissions.add(new PermissionEntry("Manifest.permission.CAMERA", null));
        }
        if ((permissionFlags & BuildConfig.PERMISSION_READ_EXTERNAL_STORAGE) == BuildConfig.PERMISSION_READ_EXTERNAL_STORAGE) {
            permissions.add(new PermissionEntry("Manifest.permission.READ_EXTERNAL_STORAGE", "Build.VERSION.SDK_INT < 33 || !" + targetsSdk33OrHigher));
        }
        if ((permissionFlags & BuildConfig.PERMISSION_WRITE_EXTERNAL_STORAGE) == BuildConfig.PERMISSION_WRITE_EXTERNAL_STORAGE) {
            permissions.add(new PermissionEntry("Manifest.permission.WRITE_EXTERNAL_STORAGE", "Build.VERSION.SDK_INT < 30"));
        }
        if ((permissionFlags & BuildConfig.PERMISSION_RECORD_AUDIO) == BuildConfig.PERMISSION_RECORD_AUDIO) {
            permissions.add(new PermissionEntry("Manifest.permission.RECORD_AUDIO", null));
        }
        if ((permissionFlags & BuildConfig.PERMISSION_ACCESS_FINE_LOCATION) == BuildConfig.PERMISSION_ACCESS_FINE_LOCATION) {
            permissions.add(new PermissionEntry("Manifest.permission.ACCESS_FINE_LOCATION", null));
        }
        if ((permissionFlags & BuildConfig.PERMISSION_BLUETOOTH_CONNECT) == BuildConfig.PERMISSION_BLUETOOTH_CONNECT) {
            permissions.add(new PermissionEntry("Manifest.permission.BLUETOOTH_CONNECT", "Build.VERSION.SDK_INT >= 31"));
        }
        if ((permissionFlags & BuildConfig.PERMISSION_READ_MEDIA_IMAGES) == BuildConfig.PERMISSION_READ_MEDIA_IMAGES) {
            permissions.add(new PermissionEntry("Manifest.permission.READ_MEDIA_IMAGES", "Build.VERSION.SDK_INT >= 33 && " + targetsSdk33OrHigher));
        }
        if ((permissionFlags & BuildConfig.PERMISSION_READ_MEDIA_VIDEO) == BuildConfig.PERMISSION_READ_MEDIA_VIDEO) {
            permissions.add(new PermissionEntry("Manifest.permission.READ_MEDIA_VIDEO", "Build.VERSION.SDK_INT >= 33 && " + targetsSdk33OrHigher));
        }
        if ((permissionFlags & BuildConfig.PERMISSION_READ_MEDIA_AUDIO) == BuildConfig.PERMISSION_READ_MEDIA_AUDIO) {
            permissions.add(new PermissionEntry("Manifest.permission.READ_MEDIA_AUDIO", "Build.VERSION.SDK_INT >= 33 && " + targetsSdk33OrHigher));
        }
    }

    public String writePermission(boolean isAppCompat, int var1) {
        ArrayList<PermissionEntry> permissions = new ArrayList<>();
        StringBuilder permissionCode = new StringBuilder();

        addReqPermission(permissions);
        addSystemPermissions(permissions, var1);

        if (isAppCompat) {
            removePermission(permissions);

            if (!permissions.isEmpty()) {
                permissionCode.append("if (");

                for (int i = 0; i < permissions.size(); i++) {
                    if (i != 0) permissionCode.append("\r\n|| ");
                    permissionCode.append(permissions.get(i).getCheckExpression(true));
                }

                permissionCode.append(") {" + ActivityCodeGenerator.EOL);
                permissionCode.append("ArrayList<String> _permissions = new ArrayList<>();" + ActivityCodeGenerator.EOL);
                for (PermissionEntry permission : permissions) {
                    permissionCode.append(permission.getRequestCode()).append(ActivityCodeGenerator.EOL);
                }
                permissionCode.append("ActivityCompat.requestPermissions(this, _permissions.toArray(new String[0]), 1000);" + ActivityCodeGenerator.EOL +
                        "} else {" + ActivityCodeGenerator.EOL +
                        "initializeLogic();" + ActivityCodeGenerator.EOL +
                        "}" + ActivityCodeGenerator.EOL);
            }

        } else {
            removePermission(permissions);

            if (!permissions.isEmpty()) {
                permissionCode.append("if (Build.VERSION.SDK_INT >= 23) {" + ActivityCodeGenerator.EOL + "if (");

                for (int i = 0; i < permissions.size(); i++) {
                    if (i != 0) permissionCode.append(ActivityCodeGenerator.EOL + "||");
                    permissionCode.append(permissions.get(i).getCheckExpression(false));
                }

                permissionCode.append(") {" + ActivityCodeGenerator.EOL);
                permissionCode.append("ArrayList<String> _permissions = new ArrayList<>();" + ActivityCodeGenerator.EOL);
                for (PermissionEntry permission : permissions) {
                    permissionCode.append(permission.getRequestCode()).append(ActivityCodeGenerator.EOL);
                }
                permissionCode.append("requestPermissions(_permissions.toArray(new String[0]), 1000);" + ActivityCodeGenerator.EOL +
                        "} else {" + ActivityCodeGenerator.EOL +
                        "initializeLogic();" + ActivityCodeGenerator.EOL +
                        "}" + ActivityCodeGenerator.EOL +
                        "} else {" + ActivityCodeGenerator.EOL +
                        "initializeLogic();" + ActivityCodeGenerator.EOL +
                        "}" + ActivityCodeGenerator.EOL);
            }
        }

        hasPermission = !permissions.isEmpty();

        if (permissionCode.toString().trim().isEmpty()) {
            return "initializeLogic();" + ActivityCodeGenerator.EOL;
        } else {
            return ActivityCodeGenerator.EOL + permissionCode;
        }
    }

    private static class PermissionEntry {
        private final String permission;
        private final String sdkCondition;

        private PermissionEntry(String permission, String sdkCondition) {
            this.permission = permission;
            this.sdkCondition = sdkCondition;
        }

        private String getCheckExpression(boolean isAppCompat) {
            String checkExpression = formatPermission(isAppCompat, permission);
            if (sdkCondition == null) {
                return checkExpression;
            }
            return "(" + sdkCondition + ") && " + checkExpression;
        }

        private String getRequestCode() {
            String requestCode = "_permissions.add(" + permission + ");";
            if (sdkCondition == null) {
                return requestCode;
            }
            return "if ((" + sdkCondition + ")) {" + ActivityCodeGenerator.EOL + requestCode + ActivityCodeGenerator.EOL + "}";
        }
    }
}
