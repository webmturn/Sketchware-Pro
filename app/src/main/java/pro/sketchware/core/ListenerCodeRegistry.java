package pro.sketchware.core;

import java.util.HashMap;
import java.util.Map;

import mod.agus.jcoderz.editor.event.ManageEvent;

/**
 * Registry-based replacement for the switch in
 * {@link ComponentCodeGenerator#getListenerCode(String, String, String)}.
 * Each listener event name is mapped to a handler that generates listener setup code.
 */
public class ListenerCodeRegistry {

    @FunctionalInterface
    public interface ListenerCodeHandler {
        String generate(String componentName, String eventLogic);
    }

    private static final Map<String, ListenerCodeHandler> handlers = new HashMap<>(32);

    static {
        registerViewListeners();
        registerTextListeners();
        registerItemListeners();
        registerFirebaseListeners();
        registerStorageListeners();
        registerNetworkListeners();
        registerSensorListeners();
        registerOtherListeners();
    }

    public static String generate(String eventName, String componentName, String eventLogic) {
        ListenerCodeHandler handler = handlers.get(eventName);
        if (handler != null) {
            return handler.generate(componentName, eventLogic);
        }
        return ManageEvent.getExtraListenerCode(eventName, componentName, eventLogic);
    }

    private static void register(String eventName, ListenerCodeHandler handler) {
        handlers.put(eventName, handler);
    }

    private static void registerViewListeners() {
        register("onClickListener", (name, logic) ->
                name + ".setOnClickListener(new View.OnClickListener() {\r\n" + logic + "\r\n});");
        register("onItemClickListener", (name, logic) ->
                name + ".setOnItemClickListener(new AdapterView.OnItemClickListener() {\r\n" + logic + "\r\n});");
        register("onItemLongClickListener", (name, logic) ->
                name + ".setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {\r\n" + logic + "\r\n});");
        register("onItemSelectedListener", (name, logic) ->
                name + ".setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {\r\n" + logic + "\r\n});");
        register("onSeekBarChangeListener", (name, logic) ->
                name + ".setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {\r\n" + logic + "\r\n});");
        register("onCheckChangedListener", (name, logic) ->
                name + ".setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {\r\n" + logic + "\r\n});");
        register("onDateChangeListener", (name, logic) ->
                name + ".setOnDateChangeListener(new CalendarView.OnDateChangeListener() {\r\n" + logic + "\r\n});");
        register("animatorListener", (name, logic) ->
                name + ".addListener(new Animator.AnimatorListener() {\r\n" + logic + "\r\n});");
        register("webViewClient", (name, logic) ->
                name + ".setWebViewClient(new WebViewClient() {\r\n" + logic + "\r\n});");
    }

    private static void registerTextListeners() {
        register("onTextChangedListener", (name, logic) ->
                name + ".addTextChangedListener(new TextWatcher() {\r\n" + logic + "\r\n});");
    }

    private static void registerItemListeners() {
        // Handled in registerViewListeners
    }

    private static void registerFirebaseListeners() {
        register("childEventListener", (name, logic) -> {
            String listenerName = "_" + name + "_child_listener";
            return listenerName + " = new ChildEventListener() {\r\n"
                    + logic + "\r\n"
                    + "};\r\n"
                    + name + ".addChildEventListener(" + listenerName + ");";
        });
        register("authCreateUserComplete", (name, logic) ->
                "_" + name + "_create_user_listener = new OnCompleteListener<AuthResult>() {\r\n" + logic + "\r\n};");
        register("authSignInUserComplete", (name, logic) ->
                "_" + name + "_sign_in_listener = new OnCompleteListener<AuthResult>() {\r\n" + logic + "\r\n};");
        register("authResetEmailSent", (name, logic) ->
                "_" + name + "_reset_password_listener = new OnCompleteListener<Void>() {\r\n" + logic + "\r\n};");
    }

    private static void registerStorageListeners() {
        register("onUploadSuccessListener", (name, logic) ->
                "_" + name + "_upload_success_listener = new OnCompleteListener<Uri>() {\r\n" + logic + "\r\n};");
        register("onUploadProgressListener", (name, logic) ->
                "_" + name + "_upload_progress_listener = new OnProgressListener<UploadTask.TaskSnapshot>() {\r\n" + logic + "\r\n};");
        register("onDownloadSuccessListener", (name, logic) ->
                "_" + name + "_download_success_listener = new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {\r\n" + logic + "\r\n};");
        register("onDownloadProgressListener", (name, logic) ->
                "_" + name + "_download_progress_listener = new OnProgressListener<FileDownloadTask.TaskSnapshot>() {\r\n" + logic + "\r\n};");
        register("onDeleteSuccessListener", (name, logic) ->
                "_" + name + "_delete_success_listener = new OnSuccessListener() {\r\n" + logic + "\r\n};");
        register("onFailureListener", (name, logic) ->
                "_" + name + "_failure_listener = new OnFailureListener() {\r\n" + logic + "\r\n};");
    }

    private static void registerNetworkListeners() {
        register("requestListener", (name, logic) ->
                "_" + name + "_request_listener = new RequestNetwork.RequestListener() {\r\n" + logic + "\r\n};");
        register("bluetoothConnectionListener", (name, logic) ->
                "_" + name + "_bluetooth_connection_listener = new BluetoothConnect.BluetoothConnectionListener() {\r\n" + logic + "\r\n};");
    }

    private static void registerSensorListeners() {
        register("sensorEventListener", (name, logic) -> {
            String listenerName = "_" + name + "_sensor_listener";
            return listenerName + " = new SensorEventListener() {\r\n"
                    + logic + "\r\n"
                    + "};\r\n"
                    + name + ".registerListener(" + listenerName + ", " + name
                    + ".getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_NORMAL);";
        });
        register("locationListener", (name, logic) ->
                "_" + name + "_location_listener = new LocationListener() {\r\n"
                        + logic + "\r\n"
                        + "\r\n"
                        + "@Override\r\n"
                        + "public void onStatusChanged(String provider, int status, Bundle extras) {\r\n"
                        + "}\r\n"
                        + "\r\n"
                        + "@Override\r\n"
                        + "public void onProviderEnabled(String provider) {\r\n"
                        + "}\r\n"
                        + "\r\n"
                        + "@Override\r\n"
                        + "public void onProviderDisabled(String provider) {\r\n"
                        + "}\r\n"
                        + "};");
    }

    private static void registerOtherListeners() {
        register("recognitionListener", (name, logic) ->
                name + ".setRecognitionListener(new RecognitionListener() {\r\n"
                        + "@Override\r\n"
                        + "public void onReadyForSpeech(Bundle _param1) {\r\n"
                        + "}\r\n"
                        + "\r\n"
                        + "@Override\r\n"
                        + "public void onBeginningOfSpeech() {\r\n"
                        + "}\r\n"
                        + "\r\n"
                        + "@Override\r\n"
                        + "public void onRmsChanged(float _param1) {\r\n"
                        + "}\r\n"
                        + "\r\n"
                        + "@Override\r\n"
                        + "public void onBufferReceived(byte[] _param1) {\r\n"
                        + "}\r\n"
                        + "\r\n"
                        + "@Override\r\n"
                        + "public void onEndOfSpeech() {\r\n"
                        + "}\r\n"
                        + "\r\n"
                        + "@Override\r\n"
                        + "public void onPartialResults(Bundle _param1) {\r\n"
                        + "}\r\n"
                        + "\r\n"
                        + "@Override\r\n"
                        + "public void onEvent(int _param1, Bundle _param2) {\r\n"
                        + "}\r\n"
                        + "\r\n"
                        + logic + "\r\n"
                        + "});");
        register("onMapReadyCallback", (name, logic) -> {
            String controllerName = "_" + name.replace("binding.", "") + "_controller";
            return controllerName + " = new GoogleMapController(" + name + ", new OnMapReadyCallback() {\r\n"
                    + "@Override\r\n"
                    + "public void onMapReady(GoogleMap _googleMap) {\r\n"
                    + controllerName + ".setGoogleMap(_googleMap);\r\n"
                    + logic + "\r\n"
                    + "}\r\n"
                    + "});";
        });
        register("onMapMarkerClickListener", (name, logic) ->
                "_" + name.replace("binding.", "") + "_controller.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {\r\n"
                        + logic + "\r\n"
                        + "});");
    }
}
