package pro.sketchware.core;

import android.text.TextUtils;

import com.besome.sketch.beans.ComponentBean;
import com.besome.sketch.beans.ViewBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import mod.hey.studios.moreblock.ReturnMoreblockManager;
import mod.hilal.saif.components.ComponentsHandler;
import mod.pranav.viewbinding.ViewBindingBuilder;
import pro.sketchware.core.codegen.BlockCodeRegistry;
import pro.sketchware.core.codegen.BlockInterpreter;
import pro.sketchware.core.codegen.CodeContext;
import pro.sketchware.core.codegen.CodeFormatter;
import pro.sketchware.core.codegen.EventCodeRegistry;
import pro.sketchware.core.codegen.ListenerCodeRegistry;

/**
 * Generates Java source code for component-related operations:
 * field declarations, initialization, event callbacks (onActivityResult, onCreate),
 * and MoreBlock method signatures.
 * <p>
 * Each method produces a code fragment (as a {@link String}) that is inserted into
 * the generated Activity Java file at the appropriate location. Component-specific
 * logic is dispatched via {@code switch} on {@link ComponentBean} type constants.
 *
 * @see BlockInterpreter
 * @see BlockCodeRegistry
 * @see ManifestGenerator
 */
public class ComponentCodeGenerator {

    /** @deprecated Use {@link GradleFileGenerator#getSettingsGradle()} directly */
    public static String getSettingsGradle() {
        return GradleFileGenerator.getSettingsGradle();
    }

    /** @deprecated Use {@link GradleFileGenerator#getBuildGradleString} directly */
    public static String getBuildGradleString(int compileSdkVersion, int minSdkVersion, String targetSdkVersion, BuildConfig metadata, boolean isViewBindingEnabled) {
        return GradleFileGenerator.getBuildGradleString(compileSdkVersion, minSdkVersion, targetSdkVersion, metadata, isViewBindingEnabled);
    }

    /**
     * @return Code to be added to <code>onActivityResult</code> for a component
     */
    public static String getOnActivityResultCode(CodeContext codeCtx, int componentId, String componentName, String onSuccessLogic, String onCancelledLogic) {
        String componentLogic = switch (componentId) {
            case ComponentBean.COMPONENT_TYPE_FILE_PICKER ->
                    "ArrayList<String> _filePath = new ArrayList<>();\r\n" +
                            "if (_data != null) {\r\n" +
                            "if (_data.getClipData() != null) {\r\n" +
                            "for (int _index = 0; _index < _data.getClipData().getItemCount(); _index++) {\r\n" +
                            "ClipData.Item _item = _data.getClipData().getItemAt(_index);\r\n" +
                            "_filePath.add(FileUtil.convertUriToFilePath(" + codeCtx.appContext() + ", _item.getUri()));\r\n" +
                            "}\r\n" +
                            "}\r\n" +
                            "else {\r\n" +
                            "_filePath.add(FileUtil.convertUriToFilePath(" + codeCtx.appContext() + ", _data.getData()));\r\n" +
                            "}\r\n" +
                            "}";
            case ComponentBean.COMPONENT_TYPE_CAMERA ->
                    " String _filePath = _file_" + componentName + ".getAbsolutePath();\r\n";
            case ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH_GOOGLE_LOGIN ->
                    "Task<GoogleSignInAccount> _task = GoogleSignIn.getSignedInAccountFromIntent(_data);\r\n";
            case ComponentBean.COMPONENT_TYPE_CAMERA_LEGACY -> "String _filePath = file_" + componentName + ".getAbsolutePath();\r\n";
            default -> "";
        };

        return "case REQ_CD_" + componentName.toUpperCase() + ":\r\n" +
                "if (_resultCode == Activity.RESULT_OK) {\r\n" +
                componentLogic + "\r\n" +
                onSuccessLogic + "\r\n" +
                "}\r\n" +
                "else {\r\n" +
                onCancelledLogic + "\r\n" +
                "}\r\n" +
                "break;";
    }

    /**
     * @return Code to initialize a widget
     */
    public static String getViewInitializerString(ViewBean bean) {
        String type;
        if (!bean.convert.isEmpty()) {
            type = bean.convert;
        } else {
            type = bean.getClassInfo().getClassName();
        }

        return "final " + type + " " + bean.id + " = _view.findViewById(R.id." + bean.id + ");";
    }

    /**
     * @param widgetName The list widget's name
     * @return The adapter's class name (e.g., List_filesAdapter from list_files)
     */
    public static String getAdapterClassName(String widgetName, boolean isViewBindingEnabled) {
        if (isViewBindingEnabled)
            widgetName = ViewBindingBuilder.generateParameterFromId(widgetName);
        return widgetName.substring(0, 1).toUpperCase() +
                widgetName.substring(1) +
                "Adapter";
    }

    /**
     * @return A component request code constant declaration
     */
    public static String getRequestCodeConstant(String componentName, int value) {
        return "public final int REQ_CD_" + componentName.toUpperCase() + " = " + value + ";";
    }

    public static String getEventCode(String targetId, String eventName, String eventLogic) {
        return EventCodeRegistry.generate(targetId, eventName, eventLogic);
    }


    /**
     * @return One or more lines which declare a Type's necessary fields.
     * Example for a Camera Component:
     * <pre>
     * private File _file_&lt;component name&gt;;
     * </pre>
     */
    public static String getFieldDeclaration(String typeName, String typeInstanceName, AccessModifier accessModifier, String... parameters) {
        return getFieldDeclaration(typeName, typeInstanceName, accessModifier, false, parameters);
    }

    public static String getFieldDeclaration(String typeName, String typeInstanceName, AccessModifier accessModifier, boolean isViewBindingEnabled, String... parameters) {
        String fieldDeclaration = "";

        if (typeName.equals("include") || typeName.equals("#")) {
            fieldDeclaration = "";
        } else {
            if (!isViewBindingEnabled) {
                fieldDeclaration = accessModifier.getName();
                String initializer = getInitializer(typeName, parameters);
                String builtInType = ComponentTypeMapper.getActualTypeName(typeName);
                if (initializer.isEmpty()) {
                    if (!(builtInType.isEmpty() || builtInType.equals("RewardedVideoAd") || builtInType.equals("FirebaseCloudMessage") || builtInType.equals("FragmentStatePagerAdapter"))) {
                        fieldDeclaration += " " + builtInType + " " + typeInstanceName + ";";
                    } else {
                        switch (typeName) {
                            case "FirebaseCloudMessage":
                                fieldDeclaration = "";
                                break;
                            case "FragmentStatePagerAdapter":
                                fieldDeclaration += " " + getAdapterClassName(typeInstanceName + "Fragment", false) + " " + typeInstanceName + ";";
                                break;
                            case "RewardedVideoAd":
                                fieldDeclaration += " RewardedAd " + typeInstanceName + ";";
                                break;
                            default:
                                fieldDeclaration += " " + typeName + " " + typeInstanceName + ";";
                                break;
                        }
                    }
                } else {
                    String typeNameOfField = builtInType;

                    if (builtInType.isEmpty() && "Videos".equals(typeName)) {
                        typeNameOfField = "Intent";
                    }

                    fieldDeclaration += " " + typeNameOfField + " " + typeInstanceName + " = " + initializer + ";";
                }
            }

            switch (typeName) {
                case "FirebaseDB":
                    fieldDeclaration += "\r\nprivate ChildEventListener _" + typeInstanceName + "_child_listener;";
                    break;

                case "Gyroscope":
                    fieldDeclaration += "\r\nprivate SensorEventListener _" + typeInstanceName + "_sensor_listener;";
                    break;

                case "FirebaseAuth":
                    fieldDeclaration += "\r\nprivate OnCompleteListener<AuthResult> _" + typeInstanceName + "_create_user_listener;\r\n" +
                            "private OnCompleteListener<AuthResult> _" + typeInstanceName + "_sign_in_listener;\r\n" +
                            "private OnCompleteListener<Void> _" + typeInstanceName + "_reset_password_listener;\r\n" +
                            // Fields/Events added by Agus
                            "private OnCompleteListener<Void> " + typeInstanceName + "_updateEmailListener;\r\n" +
                            "private OnCompleteListener<Void> " + typeInstanceName + "_updatePasswordListener;\r\n" +
                            "private OnCompleteListener<Void> " + typeInstanceName + "_emailVerificationSentListener;\r\n" +
                            "private OnCompleteListener<Void> " + typeInstanceName + "_deleteUserListener;\r\n" +
                            "private OnCompleteListener<Void> " + typeInstanceName + "_updateProfileListener;\r\n" +
                            "private OnCompleteListener<AuthResult> " + typeInstanceName + "_phoneAuthListener;\r\n" +
                            "private OnCompleteListener<AuthResult> " + typeInstanceName + "_googleSignInListener;\r\n";
                    break;

                case "InterstitialAd":
                    fieldDeclaration += "\r\nprivate InterstitialAdLoadCallback _" + typeInstanceName + "_interstitial_ad_load_callback;";
                    fieldDeclaration += "\r\nprivate FullScreenContentCallback _" + typeInstanceName + "_full_screen_content_callback;";
                    break;

                case "RewardedVideoAd":
                    fieldDeclaration += "\r\nprivate OnUserEarnedRewardListener _" + typeInstanceName + "_on_user_earned_reward_listener;";
                    fieldDeclaration += "\r\nprivate RewardedAdLoadCallback _" + typeInstanceName + "_rewarded_ad_load_callback;";
                    fieldDeclaration += "\r\nprivate FullScreenContentCallback _" + typeInstanceName + "_full_screen_content_callback;";
                    break;

                case "FirebaseStorage":
                    fieldDeclaration += "\r\nprivate OnCompleteListener<Uri> _" + typeInstanceName + "_upload_success_listener;\r\n" +
                            "private OnSuccessListener<FileDownloadTask.TaskSnapshot> _" + typeInstanceName + "_download_success_listener;\r\n" +
                            "private OnSuccessListener _" + typeInstanceName + "_delete_success_listener;\r\n" +
                            "private OnProgressListener _" + typeInstanceName + "_upload_progress_listener;\r\n" +
                            "private OnProgressListener _" + typeInstanceName + "_download_progress_listener;\r\n" +
                            "private OnFailureListener _" + typeInstanceName + "_failure_listener;\r\n";
                    break;

                case "Camera":
                    fieldDeclaration += "\r\nprivate File _file_" + typeInstanceName + ";";
                    break;

                case "RequestNetwork":
                    fieldDeclaration += "\r\nprivate RequestNetwork.RequestListener _" + typeInstanceName + "_request_listener;";
                    break;

                case "BluetoothConnect":
                    fieldDeclaration += "\r\nprivate BluetoothConnect.BluetoothConnectionListener _" + typeInstanceName + "_bluetooth_connection_listener;";
                    break;

                case "Notification":
                    fieldDeclaration += "\r\nprivate NotificationManager _nm_" + typeInstanceName + ";";
                    break;

                case "SQLiteDatabase":
                    fieldDeclaration += "\r\nprivate Cursor _" + typeInstanceName + "_cursor;";
                    break;

                case "LocationManager":
                    fieldDeclaration += "\r\nprivate LocationListener _" + typeInstanceName + "_location_listener;";
                    break;

                case "MapView":
                    fieldDeclaration += "\r\nprivate GoogleMapController _" + typeInstanceName + "_controller;";
                    break;

                case "Videos":
                    fieldDeclaration += "\r\nprivate File file_" + typeInstanceName + ";";
                    break;

                case "FirebaseCloudMessage":
                    fieldDeclaration += "\r\nprivate OnCompleteListener " + typeInstanceName + "_onCompleteListener;";
                    break;

                case "PhoneAuthProvider.OnVerificationStateChangedCallbacks":
                    fieldDeclaration += "private PhoneAuthProvider.ForceResendingToken " + typeInstanceName + "_resendToken;";
                    break;

                case "TimePickerDialog":
                    fieldDeclaration += "\r\nprivate TimePickerDialog.OnTimeSetListener " + typeInstanceName + "_listener;";
                    break;

                default:
                    fieldDeclaration = ComponentsHandler.getExtraVar(typeName, fieldDeclaration, typeInstanceName);
                    break;
            }
        }

        return fieldDeclaration.trim();
    }

    /**
     * @return Code of a More Block
     */
    public static String getMoreBlockCode(String moreBlockName, String moreBlockSpec, String moreBlockLogic) {
        StringBuilder code = new StringBuilder();
        code.append("public ").append(ReturnMoreblockManager.getMbTypeCode(moreBlockName))
                .append(" _").append(ReturnMoreblockManager.getMbName(moreBlockName)).append("(");
        ArrayList<String> parameterSpecs = FormatUtil.parseBlockSpec(moreBlockSpec);
        boolean isFirstParameter = true;

        for (String parameterSpec : parameterSpecs) {
            // Avoid label spec parts
            if (parameterSpec.length() >= 2 && parameterSpec.charAt(0) == '%') {
                char parameterType = parameterSpec.charAt(1);
                String typeName = null;
                int lastIndexOfPeriod = -1;
                switch (parameterType) {
                    case 'b':
                        typeName = "boolean";
                        break;
                    case 'd':
                        typeName = "double";
                        break;
                    case 's':
                        typeName = "String";
                        break;
                    default:
                        if (parameterType == 'm') {
                            lastIndexOfPeriod = parameterSpec.lastIndexOf(".");
                            typeName = ComponentTypeMapper.getActualTypeName(ComponentTypeMapper.getInternalTypeName(parameterSpec.substring(3, lastIndexOfPeriod)));
                        } else {
                            continue;
                        }
                }

                if (!isFirstParameter) {
                    code.append(", ");
                }
                code.append("final ").append(typeName).append(" _");
                if (parameterType == 'm') {
                    code.append(parameterSpec.substring(lastIndexOfPeriod + 1));
                } else {
                    code.append(parameterSpec.substring(3));
                }
                isFirstParameter = false;
            }
        }

        code.append(") {\r\n").append(moreBlockLogic).append("\r\n}\r\n");
        return code.toString();
    }

    /**
     * @return Code of an adapter for a ListView
     */
    public static String getListAdapterCode(CodeContext codeCtx, LayoutGenerator layoutGenerator, String widgetName, String itemResourceName, ArrayList<ViewBean> views, String onBindCustomViewLogic, boolean isViewBindingEnabled) {
        String className = getAdapterClassName(widgetName, isViewBindingEnabled);

        String initializers = "";
        StringBuilder initializersBuilder = new StringBuilder(initializers);
        for (ViewBean bean : views) {
            Set<String> toNotAdd = layoutGenerator.readAttributesToReplace(bean);
            if (!toNotAdd.contains("android:id")) {
                initializersBuilder.append(getViewInitializerString(bean)).append("\r\n");
            }
        }
        initializers = initializersBuilder.toString();

        String baseCode = "public class " + className + " extends BaseAdapter {\r\n" +
                "\r\n" +
                "ArrayList<HashMap<String, Object>> _data;\r\n" +
                "\r\n" +
                "public " + className + "(ArrayList<HashMap<String, Object>> _arr) {\r\n" +
                "_data = _arr;\r\n" +
                "}\r\n" +
                "\r\n" +
                "@Override\r\n" +
                "public int getCount() {\r\n" +
                "return _data.size();\r\n" +
                "}\r\n" +
                "\r\n" +
                "@Override\r\n" +
                "public HashMap<String, Object> getItem(int _index) {\r\n" +
                "return _data.get(_index);\r\n" +
                "}\r\n" +
                "\r\n" +
                "@Override\r\n" +
                "public long getItemId(int _index) {\r\n" +
                "return _index;\r\n" +
                "}\r\n" +
                "\r\n";
        if (isViewBindingEnabled) {
            String bindingName = ViewBindingBuilder.generateFileNameForLayout(itemResourceName);
            baseCode += "@Override\r\n" +
                    "public View getView(final int _position, View _v, ViewGroup _container) {\r\n" +
                    bindingName + " binding = " + bindingName + ".inflate(" + codeCtx.layoutInflater() + ");\r\n" +
                    "View _view = binding.getRoot();\r\n";
        } else {
            baseCode += "@Override\r\n" +
                    "public View getView(final int _position, View _v, ViewGroup _container) {\r\n" +
                    "LayoutInflater _inflater = " + codeCtx.layoutInflater() + ";\r\n" +
                    "View _view = _v;\r\n" +
                    "if (_view == null) {\r\n" +
                    "_view = _inflater.inflate(R.layout." + itemResourceName + ", null);\r\n" +
                    "}\r\n";

            if (!TextUtils.isEmpty(initializers)) {
                baseCode += "\r\n" +
                        initializers;
            }
        }

        if (!TextUtils.isEmpty(onBindCustomViewLogic.trim())) {
            baseCode += "\r\n" +
                    onBindCustomViewLogic + "\r\n";
        }

        return baseCode + "\r\n" +
                "return _view;\r\n" +
                "}\r\n" +
                "}\r\n";
    }

    /**
     * @return An initializer for that component/variable.
     * Example initializer for an Intent component: <code>new Intent()</code>
     * Example initializer for a boolean variable: <code>false</code>
     */
    public static String getInitializer(String name, String... parameters) {
        switch (name) {
            case "boolean":
                return "false";

            case "double":
                return "0";

            case "String":
                return "\"\"";

            case "Map":
                return "new HashMap<>()";

            case "ListInt":
            case "ListString":
            case "ListMap":
                return "new ArrayList<>()";

            case "Intent":
                return "new Intent()";

            case "Calendar":
                return "Calendar.getInstance()";

            case "FirebaseDB":
                String reference = "";
                if (parameters[0] != null && !parameters[0].isEmpty()) {
                    reference = parameters[0].replace(";", "");
                }
                return "_firebase.getReference(\"" + reference + "\")";

            case "ObjectAnimator":
                return "new ObjectAnimator()";

            case "FirebaseStorage":
                reference = "";
                if (parameters[0] != null && !parameters[0].isEmpty()) {
                    reference = parameters[0].replace(";", "");
                }
                return "_firebase_storage.getReference(\"" + reference + "\")";

            case "Camera":
                return "new Intent(MediaStore.ACTION_IMAGE_CAPTURE)";

            case "FilePicker":
                return "new Intent(Intent.ACTION_GET_CONTENT)";

            default:
                return "";
        }
    }


    public static String getDefaultActivityLifecycleCode(String eventName, String viewType, String viewId) {
        boolean isMapView = viewType.equals("MapView");
        boolean isAdView = viewType.equals("AdView");
        StringBuilder code = new StringBuilder();

        switch (eventName) {
            case "onBackPressed":
                if (viewType.equals("DrawerLayout")) {
                    code.append("if (").append(viewId).append(".isDrawerOpen(GravityCompat.START)) {\r\n");
                    code.append(viewId).append(".closeDrawer(GravityCompat.START);").append("\r\n");
                    code.append("} else {\r\n");
                    code.append("super.onBackPressed();").append("\r\n");
                    code.append("}");
                }
                break;

            case "onDestroy":
                if (isMapView) {
                    code.append(viewId).append(".onDestroy();");
                }
                if (isAdView) {
                    code.append("if (").append(viewId).append(" != null) {\r\n");
                    code.append(viewId).append(".destroy();\r\n");
                    code.append("}");
                }
                if (viewType.equals("Gyroscope")) {
                    code.append("if (_").append(viewId).append("_sensor_listener != null) {\r\n");
                    code.append(viewId).append(".unregisterListener(_").append(viewId).append("_sensor_listener);\r\n");
                    code.append("}\r\n");
                }
                if (viewType.equals("SQLiteDatabase")) {
                    code.append("if (_").append(viewId).append("_cursor != null && !_").append(viewId).append("_cursor.isClosed()) {\r\n");
                    code.append("_").append(viewId).append("_cursor.close();\r\n");
                    code.append("}\r\n");
                    code.append("if (").append(viewId).append(" != null && ").append(viewId).append(".isOpen()) {\r\n");
                    code.append(viewId).append(".close();\r\n");
                    code.append("}");
                }
                break;

            case "onPause":
                if (isMapView) {
                    code.append(viewId).append(".onPause();");
                }
                if (isAdView) {
                    code.append("if (").append(viewId).append(" != null) {\r\n");
                    code.append(viewId).append(".pause();\r\n");
                    code.append("}");
                }
                break;

            case "onStart":
                if (isMapView) {
                    code.append(viewId).append(".onStart();");
                }
                break;

            case "onResume":
                if (isMapView) {
                    code.append(viewId).append(".onResume();");
                }
                if (isAdView) {
                    code.append("if (").append(viewId).append(" != null) {\r\n");
                    code.append(viewId).append(".resume();\r\n");
                    code.append("}");
                }
                break;

            case "onStop":
                if (isMapView) {
                    code.append(viewId).append(".onStop();");
                }
                break;
        }
        return code.toString();
    }

    public static String getBindingOrViewName(String name, boolean isViewBinding) {
        if (isViewBinding) {
            return "binding." + ViewBindingBuilder.generateParameterFromId(name);
        } else {
            return name;
        }
    }

    /**
     * @return Initializer of a View to be added to _initialize(Bundle)
     */
    public static String getViewInitializer(String type, String name, boolean isInFragment, boolean viewBinding) {
        String initializer = "";

        if (!type.equals("include") && !type.equals("#")) {
            if (!viewBinding) {
                initializer = name + " = " +
                        (isInFragment ? "_view.findViewById(R.id." : "findViewById(R.id.") +
                        name + ");";
            }
        }

        return switch (type) {
            case "WebView" -> initializer + "\r\n" +
                    getBindingOrViewName(name, viewBinding) + ".getSettings().setJavaScriptEnabled(true);\r\n" +
                    getBindingOrViewName(name, viewBinding) + ".getSettings().setSupportZoom(true);";
            case "MapView" -> initializer + "\r\n" +
                    getBindingOrViewName(name, viewBinding) + ".onCreate(_savedInstanceState);\r\n";
            case "VideoView" -> {
                String mediaControllerName = name + "_controller";
                yield initializer + "\r\n" +
                        "MediaController " + mediaControllerName + " = new MediaController(this);\r\n" +
                        getBindingOrViewName(name, viewBinding) + ".setMediaController(" + mediaControllerName + ");";
            }
            default -> initializer;
        };
    }

    /**
     * @return Initializer for a Component that'd appear in <code>_initialize(Bundle)</code>
     */
    public static String getComponentInitializerCode(CodeContext codeCtx, String componentNameId, String
            componentName, String... parameters) {
        switch (componentNameId) {
            case "SharedPreferences":
                String preferenceFilename = "";
                if (parameters[0] != null && !parameters[0].isEmpty()) {
                    preferenceFilename = parameters[0].replace(";", "");
                }
                return componentName + " = " + codeCtx.sharedPreferences() + "(\"" + preferenceFilename + "\", Activity.MODE_PRIVATE);";

            case "Vibrator":
                return componentName + " = " + codeCtx.systemService("Vibrator") + "(Context.VIBRATOR_SERVICE);";

            case "Dialog":
                return componentName + " = new AlertDialog.Builder(" + codeCtx.thisActivity() + ");";

            case "Gyroscope":
                return componentName + " = " + codeCtx.systemService("SensorManager") + "(Context.SENSOR_SERVICE);\r\n" +
                        "if (" + componentName + ".getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) == null) {\r\n" +
                        "SketchwareUtil.showMessage(" + codeCtx.appContext() + ", \"Gyroscope is not supported on this device\");\r\n" +
                        "}";

            case "FirebaseAuth":
                return componentName + " = FirebaseAuth.getInstance();";

            case "FilePicker":
                String mimeType;
                if (parameters[0] != null && !parameters[0].isEmpty()) {
                    mimeType = parameters[0].replace(";", "");
                } else {
                    mimeType = "*/*";
                }
                return componentName + ".setType(\"" + mimeType + "\");\r\n" +
                        componentName + ".putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);";

            case "Camera":
                return "_file_" + componentName + " = FileUtil.createNewPictureFile(" + codeCtx.appContext() + ");\r\n" +
                        "Uri _uri_" + componentName + ";\r\n" +
                        "if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {\r\n" +
                        "_uri_" + componentName + " = FileProvider.getUriForFile(" + codeCtx.appContext() + ", " + codeCtx.appContext() + ".getPackageName() + \".provider\", _file_" + componentName + ");\r\n" +
                        "} else {\r\n" +
                        "_uri_" + componentName + " = Uri.fromFile(_file_" + componentName + ");\r\n" +
                        "}\r\n" +
                        componentName + ".putExtra(MediaStore.EXTRA_OUTPUT, _uri_" + componentName + ");\r\n" +
                        componentName + ".addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);";

            case "RequestNetwork":
                return componentName + " = new RequestNetwork(" + codeCtx.thisActivityCast() + ");";

            case "TextToSpeech":
                return componentName + " = new TextToSpeech(" + codeCtx.appContext() + ", null);";

            case "SpeechToText":
                return componentName + " = SpeechRecognizer.createSpeechRecognizer(" + codeCtx.thisContext() + ");";

            case "BluetoothConnect":
                return componentName + " = new BluetoothConnect(" + codeCtx.thisActivityCast() + ");";

            case "SQLiteDatabase":
                return "";

            case "Notification":
                return "_nm_" + componentName + " = " + codeCtx.systemService("NotificationManager") + "(Context.NOTIFICATION_SERVICE);\r\n"
                        + componentName + " = new NotificationCompat.Builder(" + codeCtx.thisContext() + ", \"default_channel\");\r\n"
                        + componentName + ".setSmallIcon(R.mipmap.ic_launcher);\r\n"
                        + "if (Build.VERSION.SDK_INT >= 33) {\r\n"
                        + "if (ContextCompat.checkSelfPermission(" + codeCtx.qualifiedThis() + ", \"android.permission.POST_NOTIFICATIONS\") != PackageManager.PERMISSION_GRANTED) {\r\n"
                        + "ActivityCompat.requestPermissions(" + codeCtx.thisActivity() + ", new String[]{\"android.permission.POST_NOTIFICATIONS\"}, 9901);\r\n"
                        + "}\r\n}";

            case "LocationManager":
                return componentName + " = " + codeCtx.systemService("LocationManager") + "(Context.LOCATION_SERVICE);";

            case "TimePickerDialog":
                return componentName + " = new TimePickerDialog(" + codeCtx.thisActivity() + ", " + componentName + "_listener, Calendar.HOUR_OF_DAY, Calendar.MINUTE, false);";

            case "FragmentStatePagerAdapter":
                return componentName + " = new " + getAdapterClassName(componentName + "Fragment", false) + "(" + codeCtx.appContext() + ", " + codeCtx.fragmentManager() + ");";

            case "Videos":
                return "file_" + componentName + " = FileUtil.createNewPictureFile(" + codeCtx.appContext() + ");\r\n"
                        + "Uri _uri_" + componentName + " = null;\r\n"
                        + "if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {\r\n"
                        + "_uri_" + componentName + " = FileProvider.getUriForFile(" + codeCtx.appContext() + ", " +
                        codeCtx.appContext() + ".getPackageName() + \".provider\", file_" + componentName + ");\r\n"
                        + "} else {\r\n"
                        + "_uri_" + componentName + " = Uri.fromFile(file_" + componentName + ");\r\n"
                        + "}\r\n"
                        + componentName + ".putExtra(MediaStore.EXTRA_OUTPUT, _uri_" + componentName + ");\r\n"
                        + componentName + ".addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)";

            case "DatePickerDialog":
                return componentName + " = new DatePickerDialog(" + codeCtx.thisActivity() + ");";

            default:
                return ComponentsHandler.getDefineExtraVar(componentNameId, componentName);

        }
    }


    /** @deprecated Use {@link GradleFileGenerator#getTopLevelBuildGradle} directly */
    public static String getTopLevelBuildGradle(String androidGradlePluginVersion, String
            googleMobileServicesVersion) {
        return GradleFileGenerator.getTopLevelBuildGradle(androidGradlePluginVersion, googleMobileServicesVersion);
    }

    /**
     * @return A single line to initialize a drawer view.
     */
    public static String getDrawerViewInitializer(String type, String viewName, String
            viewContainerName) {
        String initializer = "";
        if (!type.equals("include") && !type.equals("#")) {
            initializer = "_drawer_" + viewName + " = " + viewContainerName + ".findViewById(R.id." + viewName + ");";
        }

        return initializer;
    }

    /**
     * @return Line declaring a field required for <code>componentName</code>
     */
    public static String getComponentFieldCode(String componentName) {
        return switch (componentName) {
            case "FirebaseDB" ->
                    "private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();";
            case "Timer" -> "private Timer _timer = new Timer();";
            case "FirebaseStorage" ->
                    "private FirebaseStorage _firebase_storage = FirebaseStorage.getInstance();";
            case "InterstitialAd" -> "private String _ad_unit_id;";
            case "RewardedVideoAd" -> "private String _reward_ad_unit_id;";
            default -> "";
        };
    }

    public static String getListenerCode(String eventName, String componentName, String
            eventLogic) {
        return ListenerCodeRegistry.generate(eventName, componentName, eventLogic);
    }



    /** @deprecated Use {@link CodeFormatter#formatCode} directly */
    public static String formatCode(String code, boolean indentMultiLineComments) {
        return CodeFormatter.formatCode(code, indentMultiLineComments);
    }

    /** @deprecated Use {@link CodeFormatter#appendIndent} directly */
    public static void appendIndent(StringBuilder builder, int indentSize) {
        CodeFormatter.appendIndent(builder, indentSize);
    }

    public static String pagerAdapter(CodeContext codeCtx, LayoutGenerator layoutGenerator, String pagerName, String
                                              pagerItemLayoutName, ArrayList<ViewBean> pagerItemViews, String onBindCustomViewLogic,
                                      boolean isViewBindingEnabled) {
        String adapterName = getAdapterClassName(pagerName, isViewBindingEnabled);

        String viewsInitializer = "";
        StringBuilder viewInitBuilder = new StringBuilder(viewsInitializer);
        for (ViewBean bean : pagerItemViews) {
            Set<String> toNotAdd = layoutGenerator.readAttributesToReplace(bean);
            if (!toNotAdd.contains("android:id")) {
                viewInitBuilder.append(getViewInitializerString(bean)).append("\r\n");
            }
        }
        viewsInitializer = viewInitBuilder.toString();

        String baseCode = "public class " + adapterName + " extends PagerAdapter {\r\n" +
                "\r\n" +
                "Context _context;\r\n" +
                "ArrayList<HashMap<String, Object>> _data;\r\n" +
                "\r\n" +
                "public " + adapterName + "(Context _ctx, ArrayList<HashMap<String, Object>> _arr) {\r\n" +
                "_context = _ctx;\r\n" +
                "_data = _arr;\r\n" +
                "}\r\n" +
                "\r\n" +
                "public " + adapterName + "(ArrayList<HashMap<String, Object>> _arr) {\r\n" +
                "_context = " + codeCtx.appContext() + ";\r\n" +
                "_data = _arr;\r\n" +
                "}\r\n" +
                "\r\n" +
                "@Override\r\n" +
                "public int getCount() {\r\n" +
                "return _data.size();\r\n" +
                "}\r\n" +
                "\r\n" +
                "@Override\r\n" +
                "public boolean isViewFromObject(View _view, Object _object) {\r\n" +
                "return _view == _object;\r\n" +
                "}\r\n" +
                "\r\n" +
                "@Override\r\n" +
                "public void destroyItem(ViewGroup _container, int _position, Object _object) {\r\n" +
                "_container.removeView((View) _object);\r\n" +
                "}\r\n" +
                "\r\n" +
                "@Override\r\n" +
                "public int getItemPosition(Object _object) {\r\n" +
                "return super.getItemPosition(_object);\r\n" +
                "}\r\n" +
                "\r\n" +
                "@Override\r\n" +
                "public CharSequence getPageTitle(int pos) {\r\n" +
                "return onTabLayoutNewTabAdded(pos);\r\n" +
                "}\r\n" +
                "\r\n";
        if (isViewBindingEnabled) {
            String bindingName = ViewBindingBuilder.generateFileNameForLayout(pagerItemLayoutName);
            baseCode += "@Override\r\n" +
                    "public Object instantiateItem(ViewGroup _container, final int _position) {\r\n" +
                    bindingName + " binding = " + bindingName + ".inflate(LayoutInflater.from(_context), _container, false);\r\n";
        } else {
            baseCode += "@Override\r\n" +
                    "public Object instantiateItem(ViewGroup _container,  final int _position) {\r\n" +
                    "View _view = LayoutInflater.from(_context).inflate(R.layout." + pagerItemLayoutName + ", _container, false);\r\n";

            if (!TextUtils.isEmpty(viewsInitializer)) {
                baseCode += "\r\n" +
                        viewsInitializer;
            }
        }

        if (!TextUtils.isEmpty(onBindCustomViewLogic)) {
            baseCode += "\r\n" +
                    onBindCustomViewLogic + "\r\n";
        }

        if (isViewBindingEnabled) {
            baseCode += """
                    View _view = binding.getRoot();\r
                    """;
        }

        return baseCode +
                "\r\n" +
                "_container.addView(_view);\r\n" +
                "return _view;\r\n" +
                "}\r\n" +
                "}\r\n";
    }

    public static String recyclerViewAdapter(CodeContext codeCtx, LayoutGenerator layoutGenerator, String recyclerViewName, String
                                                     itemLayoutName, ArrayList<ViewBean> itemViews, String onBindCustomViewLogic,
                                             boolean isViewBindingEnabled) {
        String adapterName = getAdapterClassName(recyclerViewName, isViewBindingEnabled);
        String viewsInitializer = "";
        StringBuilder viewInitBuilder = new StringBuilder(viewsInitializer);
        for (ViewBean bean : itemViews) {
            Set<String> toNotAdd = layoutGenerator.readAttributesToReplace(bean);
            if (!toNotAdd.contains("android:id")) {
                viewInitBuilder.append(getViewInitializerString(bean)).append("\r\n");
            }
        }
        viewsInitializer = viewInitBuilder.toString();

        String baseCode = "public class " + adapterName + " extends RecyclerView.Adapter<" + adapterName + ".ViewHolder> {\r\n" +
                "\r\n" +
                "ArrayList<HashMap<String, Object>> _data;\r\n" +
                "\r\n" +
                "public " + adapterName + "(ArrayList<HashMap<String, Object>> _arr) {\r\n" +
                "_data = _arr;\r\n" +
                "}\r\n" +
                "\r\n" +
                "@Override\r\n" +
                "public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {\r\n" +
                "LayoutInflater _inflater = " + codeCtx.layoutInflater() + ";\r\n" +
                "View _v = _inflater.inflate(R.layout." + itemLayoutName + ", null);\r\n" +
                "RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);\r\n" +
                "_v.setLayoutParams(_lp);\r\n" +
                "return new ViewHolder(_v);\r\n" +
                "}\r\n" +
                "\r\n";
        if (isViewBindingEnabled) {
            String bindingName = ViewBindingBuilder.generateFileNameForLayout(itemLayoutName);
            baseCode += "@Override\r\n" +
                    "public void onBindViewHolder(ViewHolder _holder, final int _position) {\r\n" +
                    "View _view = _holder.itemView;\r\n" +
                    bindingName + " binding = " + bindingName + ".bind(_view);\r\n";
        } else {
            baseCode += """
                    @Override\r
                    public void onBindViewHolder(ViewHolder _holder, final int _position) {\r
                    View _view = _holder.itemView;\r
                    """;

            if (!TextUtils.isEmpty(viewsInitializer)) {
                baseCode += "\r\n" +
                        viewsInitializer;
            }
        }

        if (!TextUtils.isEmpty(onBindCustomViewLogic)) {
            baseCode += "\r\n" +
                    onBindCustomViewLogic + "\r\n";
        }

        return baseCode +
                "}\r\n" +
                "\r\n" +
                "@Override\r\n" +
                "public int getItemCount() {\r\n" +
                "return _data.size();\r\n" +
                "}\r\n" +
                "\r\n" +
                "public class ViewHolder extends RecyclerView.ViewHolder {\r\n" +
                "public ViewHolder(View v) {\r\n" +
                "super(v);\r\n" +
                "}\r\n" +
                "}\r\n" +
                "}\r\n";
    }

    /**
     * A field's access modifier. Can either be
     * <code>private</code>, <code>protected</code> or <code>public</code>.
     */
    public enum AccessModifier {
        /**
         * MODE_PRIVATE
         */
        PRIVATE("private"),
        /**
         * MODE_PROTECTED
         */
        PROTECTED("protected"),
        /**
         * MODE_PUBLIC
         */
        PUBLIC("public");

        private final String name;

        AccessModifier(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
