package pro.sketchware.control.logic;

import com.besome.sketch.beans.BlockBean;

import java.util.ArrayList;
import java.util.Map.Entry;

import a.a.a.ActivityCodeGenerator;
import a.a.a.ProjectDataManager;
import a.a.a.BuildConfig;

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
        for (Entry<String, ArrayList<BlockBean>> blocks : ProjectDataManager.getProjectDataManager(sc_id).b(javaName).entrySet()) {
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

    private String formatPermission(boolean isAppCompat, String permission) {
        if (isAppCompat) {
            return "ContextCompat.checkSelfPermission(this, " + permission + ") == PackageManager.PERMISSION_DENIED";
        } else {
            return "checkSelfPermission(" + permission + ") == PackageManager.PERMISSION_DENIED";
        }
    }

    private void addReqPermission(boolean isAppCompat, ArrayList<String> checkPerm, ArrayList<String> reqPerm) {
        for (String permission : addedPermissions()) {
            checkPerm.add(formatPermission(isAppCompat, permission));
            reqPerm.add(permission);
        }
    }

    private void removePermission(boolean isAppCompat, ArrayList<String> checkPerm, ArrayList<String> reqPerm) {
        for (Entry<String, ArrayList<BlockBean>> blocks : ProjectDataManager.getProjectDataManager(sc_id).b(javaName).entrySet()) {
            for (BlockBean block : blocks.getValue()) {
                if (block.opCode.equals("removePermission") && !block.parameters.get(0).trim().isEmpty()) {
                    String permission = block.parameters.get(0).startsWith("Manifest") ? block.parameters.get(0) : ("Manifest.permission." + block.parameters.get(0));
                    checkPerm.remove(formatPermission(isAppCompat, permission));
                    reqPerm.remove(permission);
                }
            }
        }
    }

    public boolean hasNewPermission() {
        return !addedPermissions().isEmpty();
    }

    public String writePermission(boolean isAppCompat, int var1) {
        ArrayList<String> checkPerm = new ArrayList<>();
        ArrayList<String> addPerm = new ArrayList<>();
        StringBuilder permissionCode = new StringBuilder();

        addReqPermission(isAppCompat, checkPerm, addPerm);

        if (isAppCompat) {
            if ((var1 & BuildConfig.PERMISSION_CALL_PHONE) == BuildConfig.PERMISSION_CALL_PHONE) {
                checkPerm.add("ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_DENIED");
                addPerm.add("Manifest.permission.CALL_PHONE");
            }
            if ((var1 & BuildConfig.PERMISSION_CAMERA) == BuildConfig.PERMISSION_CAMERA) {
                checkPerm.add("ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED");
                addPerm.add("Manifest.permission.CAMERA");
            }
            if ((var1 & BuildConfig.PERMISSION_READ_EXTERNAL_STORAGE) == BuildConfig.PERMISSION_READ_EXTERNAL_STORAGE) {
                checkPerm.add("ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED");
                addPerm.add("Manifest.permission.READ_EXTERNAL_STORAGE");
            }
            if ((var1 & BuildConfig.PERMISSION_WRITE_EXTERNAL_STORAGE) == BuildConfig.PERMISSION_WRITE_EXTERNAL_STORAGE) {
                checkPerm.add("ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED");
                addPerm.add("Manifest.permission.WRITE_EXTERNAL_STORAGE");
            }
            if ((var1 & BuildConfig.PERMISSION_RECORD_AUDIO) == BuildConfig.PERMISSION_RECORD_AUDIO) {
                checkPerm.add("ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED");
                addPerm.add("Manifest.permission.RECORD_AUDIO");
            }
            if ((var1 & BuildConfig.PERMISSION_ACCESS_FINE_LOCATION) == BuildConfig.PERMISSION_ACCESS_FINE_LOCATION) {
                checkPerm.add("ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED");
                addPerm.add("Manifest.permission.ACCESS_FINE_LOCATION");
            }
            removePermission(true, checkPerm, addPerm);

            if (!checkPerm.isEmpty() && !addPerm.isEmpty()) {
                permissionCode.append("if (");

                for (int i = 0; i < checkPerm.size(); i++) {
                    if (i != 0) permissionCode.append("\r\n|| ");
                    permissionCode.append(checkPerm.get(i));
                }

                permissionCode.append(") {" + ActivityCodeGenerator.EOL + "ActivityCompat.requestPermissions(this, new String[] {");

                for (int i = 0; i < addPerm.size(); i++) {
                    if (i != 0) permissionCode.append(", ");
                    permissionCode.append(addPerm.get(i));
                }

                permissionCode.append("}, 1000);" + ActivityCodeGenerator.EOL +
                        "} else {" + ActivityCodeGenerator.EOL +
                        "initializeLogic();" + ActivityCodeGenerator.EOL +
                        "}" + ActivityCodeGenerator.EOL);
            }

        } else {
            if ((var1 & BuildConfig.PERMISSION_CALL_PHONE) == BuildConfig.PERMISSION_CALL_PHONE) {
                checkPerm.add("checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_DENIED");
                addPerm.add("Manifest.permission.CALL_PHONE");
            }
            if ((var1 & BuildConfig.PERMISSION_CAMERA) == BuildConfig.PERMISSION_CAMERA) {
                checkPerm.add("checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED");
                addPerm.add("Manifest.permission.CAMERA");
            }
            if ((var1 & BuildConfig.PERMISSION_READ_EXTERNAL_STORAGE) == BuildConfig.PERMISSION_READ_EXTERNAL_STORAGE) {
                checkPerm.add("checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED");
                addPerm.add("Manifest.permission.READ_EXTERNAL_STORAGE");
            }
            if ((var1 & BuildConfig.PERMISSION_WRITE_EXTERNAL_STORAGE) == BuildConfig.PERMISSION_WRITE_EXTERNAL_STORAGE) {
                checkPerm.add("checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED");
                addPerm.add("Manifest.permission.WRITE_EXTERNAL_STORAGE");
            }
            if ((var1 & BuildConfig.PERMISSION_RECORD_AUDIO) == BuildConfig.PERMISSION_RECORD_AUDIO) {
                checkPerm.add("checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED");
                addPerm.add("Manifest.permission.RECORD_AUDIO");
            }
            if ((var1 & BuildConfig.PERMISSION_ACCESS_FINE_LOCATION) == BuildConfig.PERMISSION_ACCESS_FINE_LOCATION) {
                checkPerm.add("checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED");
                addPerm.add("Manifest.permission.ACCESS_FINE_LOCATION");
            }
            removePermission(false, checkPerm, addPerm);

            if (!checkPerm.isEmpty() && !addPerm.isEmpty()) {
                permissionCode.append("if (Build.VERSION.SDK_INT >= 23) {" + ActivityCodeGenerator.EOL + "if (");

                for (int i = 0; i < checkPerm.size(); i++) {
                    if (i != 0) permissionCode.append(ActivityCodeGenerator.EOL + "||");
                    permissionCode.append(checkPerm.get(i));
                }

                permissionCode.append(") {" + ActivityCodeGenerator.EOL + "requestPermissions(new String[] {");

                for (int i = 0; i < addPerm.size(); i++) {
                    if (i != 0) permissionCode.append(", ");
                    permissionCode.append(addPerm.get(i));
                }

                permissionCode.append("}, 1000);" + ActivityCodeGenerator.EOL +
                        "} else {" + ActivityCodeGenerator.EOL +
                        "initializeLogic();" + ActivityCodeGenerator.EOL +
                        "}" + ActivityCodeGenerator.EOL +
                        "} else {" + ActivityCodeGenerator.EOL +
                        "initializeLogic();" + ActivityCodeGenerator.EOL +
                        "}" + ActivityCodeGenerator.EOL);
            }
        }

        hasPermission = !checkPerm.isEmpty() || !addPerm.isEmpty();

        if (permissionCode.toString().trim().isEmpty()) {
            return "initializeLogic();" + ActivityCodeGenerator.EOL;
        } else {
            return ActivityCodeGenerator.EOL + permissionCode;
        }
    }
}
