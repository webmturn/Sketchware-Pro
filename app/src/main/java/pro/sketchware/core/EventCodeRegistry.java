package pro.sketchware.core;

import java.util.HashMap;
import java.util.Map;

import mod.agus.jcoderz.editor.event.ManageEvent;

/**
 * Maps event names to {@link EventCodeHandler} implementations that generate listener code.
 * Replaced the former giant switch in
 * {@link ComponentCodeGenerator#getEventCode(String, String, String)}.
 */
public class EventCodeRegistry {

    private static final Map<String, EventCodeHandler> handlers = new HashMap<>(64);

    static {
        registerLifecycleEvents();
        registerViewEvents();
        registerTextEvents();
        registerListEvents();
        registerSeekBarEvents();
        registerCalendarWebViewEvents();
        registerAnimationEvents();
        registerFirebaseEvents();
        registerSensorEvents();
        registerAuthEvents();
        registerStorageEvents();
        registerNetworkEvents();
        registerSpeechEvents();
        registerBluetoothEvents();
        registerMapLocationEvents();
    }

    /**
     * Registers a handler for the given event name.
     *
     * @param eventName the event name (e.g. {@code "onClick"}, {@code "onResume"})
     * @param handler   the code generation handler
     */
    public static void register(String eventName, EventCodeHandler handler) {
        handlers.put(eventName, handler);
    }

    /**
     * Returns the handler for the given event name, or {@code null} if not registered.
     *
     * @param eventName the event name
     * @return the handler, or {@code null}
     */
    public static EventCodeHandler get(String eventName) {
        return handlers.get(eventName);
    }

    /**
     * Generates the Java code for an event handler method.
     * Falls back to {@link ManageEvent#getExtraEventCode} for unregistered events.
     *
     * @param targetId   the target view/component ID
     * @param eventName  the event name
     * @param eventLogic the generated logic code body
     * @return the complete event handler method as Java source
     */
    public static String generate(String targetId, String eventName, String eventLogic) {
        EventCodeHandler handler = handlers.get(eventName);
        if (handler != null) {
            return handler.generate(targetId, eventLogic);
        }
        return ManageEvent.getExtraEventCode(targetId, eventName, eventLogic);
    }

    private static void registerLifecycleEvents() {
        register("onBackPressed", (id, logic) -> "@Override\r\npublic void onBackPressed() {\r\n" + logic + "\r\n}");
        register("onPostCreate", (id, logic) -> "@Override\r\nprotected void onPostCreate(Bundle _savedInstanceState) {\r\nsuper.onPostCreate(_savedInstanceState);\r\n" + logic + "\r\n}");
        register("onStart", (id, logic) -> "@Override\r\npublic void onStart() {\r\nsuper.onStart();\r\n" + logic + "\r\n}");
        register("onStop", (id, logic) -> "@Override\r\npublic void onStop() {\r\nsuper.onStop();\r\n" + logic + "\r\n}");
        register("onDestroy", (id, logic) -> "@Override\r\npublic void onDestroy() {\r\nsuper.onDestroy();\r\n" + logic + "\r\n}");
        register("onResume", (id, logic) -> "@Override\r\npublic void onResume() {\r\nsuper.onResume();\r\n" + logic + "\r\n}");
        register("onPause", (id, logic) -> "@Override\r\npublic void onPause() {\r\nsuper.onPause();\r\n" + logic + "\r\n}");
    }

    private static void registerViewEvents() {
        register("onClick", (id, logic) -> "@Override\r\npublic void onClick(View _view) {\r\n" + logic + "\r\n}");
        register("onLongClick", (id, logic) -> "@Override\r\npublic boolean onLongClick(View _view) {\r\n" + logic + "\r\nreturn true;\r\n}");
    }

    private static void registerTextEvents() {
        register("onCheckedChange", (id, logic) -> "@Override\r\npublic void onCheckedChanged(CompoundButton _param1, boolean _param2) {\r\nfinal boolean _isChecked = _param2;\r\n" + logic + "\r\n}");
        register("beforeTextChanged", (id, logic) -> "@Override\r\npublic void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {\r\n" + logic + "\r\n}");
        register("afterTextChanged", (id, logic) -> "@Override\r\npublic void afterTextChanged(Editable _param1) {\r\n" + logic + "\r\n}");
        register("onTextChanged", (id, logic) -> "@Override\r\npublic void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {\r\nfinal String _charSeq = _param1.toString();\r\n" + logic + "\r\n}");
    }

    private static void registerListEvents() {
        register("onItemSelected", (id, logic) -> "@Override\r\npublic void onItemSelected(AdapterView<?> _param1, View _param2, int _param3, long _param4) {\r\nfinal int _position = _param3;\r\n" + logic + "\r\n}");
        register("onNothingSelected", (id, logic) -> "@Override\r\npublic void onNothingSelected(AdapterView<?> _param1) {\r\n" + logic + "\r\n}");
        register("onItemClicked", (id, logic) -> "@Override\r\npublic void onItemClick(AdapterView<?> _param1, View _param2, int _param3, long _param4) {\r\nfinal int _position = _param3;\r\n" + logic + "\r\n}");
        register("onItemLongClicked", (id, logic) -> "@Override\r\npublic boolean onItemLongClick(AdapterView<?> _param1, View _param2, int _param3, long _param4) {\r\nfinal int _position = _param3;\r\n" + logic + "\r\nreturn true;\r\n}");
    }

    private static void registerSeekBarEvents() {
        register("onProgressChanged", (id, logic) -> "@Override\r\npublic void onProgressChanged(SeekBar _param1, int _param2, boolean _param3) {\r\nfinal int _progressValue = _param2;\r\n" + logic + "\r\n}");
        register("onStartTrackingTouch", (id, logic) -> "@Override\r\npublic void onStartTrackingTouch(SeekBar _param1) {\r\n" + logic + "\r\n}");
        register("onStopTrackingTouch", (id, logic) -> "@Override\r\npublic void onStopTrackingTouch(SeekBar _param2) {\r\n" + logic + "\r\n}");
    }

    private static void registerCalendarWebViewEvents() {
        register("onDateChange", (id, logic) -> "@Override\r\npublic void onSelectedDayChange(CalendarView _param1, int _param2, int _param3, int _param4) {\r\nfinal int _year = _param2;\r\nfinal int _month = _param3;\r\nfinal int _day = _param4;\r\n" + logic + "\r\n}");
        register("onPageStarted", (id, logic) -> "@Override\r\npublic void onPageStarted(WebView _param1, String _param2, Bitmap _param3) {\r\nfinal String _url = _param2;\r\n" + logic + "\r\nsuper.onPageStarted(_param1, _param2, _param3);\r\n}");
        register("onPageFinished", (id, logic) -> "@Override\r\npublic void onPageFinished(WebView _param1, String _param2) {\r\nfinal String _url = _param2;\r\n" + logic + "\r\nsuper.onPageFinished(_param1, _param2);\r\n}");
    }

    private static void registerAnimationEvents() {
        register("onAnimationStart", (id, logic) -> "@Override\r\npublic void onAnimationStart(Animator _param1) {\r\n" + logic + "\r\n}");
        register("onAnimationCancel", (id, logic) -> "@Override\r\npublic void onAnimationCancel(Animator _param1) {\r\n" + logic + "\r\n}");
        register("onAnimationEnd", (id, logic) -> "@Override\r\npublic void onAnimationEnd(Animator _param1) {\r\n" + logic + "\r\n}");
        register("onAnimationRepeat", (id, logic) -> "@Override\r\npublic void onAnimationRepeat(Animator _param1) {\r\n" + logic + "\r\n}");
    }

    private static void registerFirebaseEvents() {
        register("onChildAdded", (id, logic) -> "@Override\r\npublic void onChildAdded(DataSnapshot _param1, String _param2) {\r\nGenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};\r\nfinal String _childKey = _param1.getKey();\r\nfinal HashMap<String, Object> _childValue = _param1.getValue(_ind);\r\n" + logic + "\r\n}");
        register("onChildChanged", (id, logic) -> "@Override\r\npublic void onChildChanged(DataSnapshot _param1, String _param2) {\r\nGenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};\r\nfinal String _childKey = _param1.getKey();\r\nfinal HashMap<String, Object> _childValue = _param1.getValue(_ind);\r\n" + logic + "\r\n}");
        register("onChildRemoved", (id, logic) -> "@Override\r\npublic void onChildRemoved(DataSnapshot _param1) {\r\nGenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};\r\nfinal String _childKey = _param1.getKey();\r\nfinal HashMap<String, Object> _childValue = _param1.getValue(_ind);\r\n" + logic + "\r\n}");
        register("onChildMoved", (id, logic) -> "@Override\r\npublic void onChildMoved(DataSnapshot _param1, String _param2) {\r\n" + logic + "\r\n}");
        register("onCancelled", (id, logic) -> "@Override\r\npublic void onCancelled(DatabaseError _param1) {\r\nfinal int _errorCode = _param1.getCode();\r\nfinal String _errorMessage = _param1.getMessage();\r\n" + logic + "\r\n}");
    }

    private static void registerSensorEvents() {
        register("onSensorChanged", (id, logic) -> "@Override\r\npublic void onSensorChanged(SensorEvent _param1) {\r\nfloat[] _rotationMatrix = new float[16];\r\nSensorManager.getRotationMatrixFromVector(_rotationMatrix, _param1.values);\r\nfloat[] _remappedRotationMatrix = new float[16];\r\nSensorManager.remapCoordinateSystem(_rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, _remappedRotationMatrix);\r\nfloat[] _orientations = new float[3];\r\nSensorManager.getOrientation(_remappedRotationMatrix, _orientations);\r\nfor (int _i = 0; _i < 3; _i++) {\r\n_orientations[_i] = (float)(Math.toDegrees(_orientations[_i]));\r\n}\r\nfinal double _x = _orientations[0];\r\nfinal double _y = _orientations[1];\r\nfinal double _z = _orientations[2];\r\n" + logic + "\r\n}");
        register("onAccuracyChanged", (id, logic) -> "@Override\r\npublic void onAccuracyChanged(Sensor _param1, int _param2) {\r\n" + logic + "\r\n}");
    }

    private static void registerAuthEvents() {
        register("onCreateUserComplete", (id, logic) -> "@Override\r\npublic void onComplete(Task<AuthResult> _param1) {\r\nfinal boolean _success = _param1.isSuccessful();\r\nfinal String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : \"\";\r\n" + logic + "\r\n}");
        register("onSignInUserComplete", (id, logic) -> "@Override\r\npublic void onComplete(Task<AuthResult> _param1) {\r\nfinal boolean _success = _param1.isSuccessful();\r\nfinal String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : \"\";\r\n" + logic + "\r\n}");
        register("onResetPasswordEmailSent", (id, logic) -> "@Override\r\npublic void onComplete(Task<Void> _param1) {\r\nfinal boolean _success = _param1.isSuccessful();\r\n" + logic + "\r\n}");
    }

    private static void registerStorageEvents() {
        register("onUploadProgress", (id, logic) -> "@Override\r\npublic void onProgress(UploadTask.TaskSnapshot _param1) {\r\ndouble _progressValue = (100.0 * _param1.getBytesTransferred()) / _param1.getTotalByteCount();\r\n" + logic + "\r\n}");
        register("onDownloadProgress", (id, logic) -> "@Override\r\npublic void onProgress(FileDownloadTask.TaskSnapshot _param1) {\r\ndouble _progressValue = (100.0 * _param1.getBytesTransferred()) / _param1.getTotalByteCount();\r\n" + logic + "\r\n}");
        register("onUploadSuccess", (id, logic) -> "@Override\r\npublic void onComplete(Task<Uri> _param1) {\r\nfinal String _downloadUrl = _param1.getResult().toString();\r\n" + logic + "\r\n}");
        register("onDownloadSuccess", (id, logic) -> "@Override\r\npublic void onSuccess(FileDownloadTask.TaskSnapshot _param1) {\r\nfinal long _totalByteCount = _param1.getTotalByteCount();\r\n" + logic + "\r\n}");
        register("onDeleteSuccess", (id, logic) -> "@Override\r\npublic void onSuccess(Object _param1) {\r\n" + logic + "\r\n}");
        register("onFailure", (id, logic) -> "@Override\r\npublic void onFailure(Exception _param1) {\r\nfinal String _message = _param1.getMessage();\r\n" + logic + "\r\n}");
    }

    private static void registerNetworkEvents() {
        register("onResponse", (id, logic) -> "@Override\r\npublic void onResponse(String _param1, String _param2, HashMap<String, Object> _param3) {\r\nfinal String _tag = _param1;\r\nfinal String _response = _param2;\r\nfinal HashMap<String, Object> _responseHeaders = _param3;\r\n" + logic + "\r\n}");
        register("onErrorResponse", (id, logic) -> "@Override\r\npublic void onErrorResponse(String _param1, String _param2) {\r\nfinal String _tag = _param1;\r\nfinal String _message = _param2;\r\n" + logic + "\r\n}");
    }

    private static void registerSpeechEvents() {
        register("onSpeechResult", (id, logic) -> "@Override\r\npublic void onResults(Bundle _param1) {\r\nfinal ArrayList<String> _results = _param1.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);\r\nfinal String _result = _results.get(0);\r\n" + logic + "\r\n}");
        register("onSpeechError", (id, logic) -> "@Override\r\npublic void onError(int _param1) {\r\nfinal String _errorMessage;\r\nswitch (_param1) {\r\ncase SpeechRecognizer.ERROR_AUDIO:\r\n_errorMessage = \"audio error\";\r\nbreak;\r\n\r\ncase SpeechRecognizer.ERROR_SPEECH_TIMEOUT:\r\n_errorMessage = \"speech timeout\";\r\nbreak;\r\n\r\ncase SpeechRecognizer.ERROR_NO_MATCH:\r\n_errorMessage = \"speech no match\";\r\nbreak;\r\n\r\ncase SpeechRecognizer.ERROR_RECOGNIZER_BUSY:\r\n_errorMessage = \"recognizer busy\";\r\nbreak;\r\n\r\ncase SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:\r\n_errorMessage = \"recognizer insufficient permissions\";\r\nbreak;\r\n\r\ndefault:\r\n_errorMessage = \"recognizer other error\";\r\nbreak;\r\n}\r\n" + logic + "\r\n}");
    }

    private static void registerBluetoothEvents() {
        register("onConnected", (id, logic) -> "@Override\r\npublic void onConnected(String _param1, HashMap<String, Object> _param2) {\r\nfinal String _tag = _param1;\r\nfinal HashMap<String, Object> _deviceData = _param2;\r\n" + logic + "\r\n}");
        register("onDataReceived", (id, logic) -> "@Override\r\npublic void onDataReceived(String _param1, byte[] _param2, int _param3) {\r\nfinal String _tag = _param1;\r\nfinal String _data = new String(_param2, 0, _param3);\r\n" + logic + "\r\n}");
        register("onDataSent", (id, logic) -> "@Override\r\npublic void onDataSent(String _param1, byte[] _param2) {\r\nfinal String _tag = _param1;\r\nfinal String _data = new String(_param2);\r\n" + logic + "\r\n}");
        register("onConnectionError", (id, logic) -> "@Override\r\npublic void onConnectionError(String _param1, String _param2, String _param3) {\r\nfinal String _tag = _param1;\r\nfinal String _connectionState = _param2;\r\nfinal String _errorMessage = _param3;\r\n" + logic + "\r\n}");
        register("onConnectionStopped", (id, logic) -> "@Override\r\npublic void onConnectionStopped(String _param1) {\r\nfinal String _tag = _param1;\r\n" + logic + "\r\n}");
    }

    private static void registerMapLocationEvents() {
        register("onMapReady", (id, logic) -> logic);
        register("onMarkerClicked", (id, logic) -> "@Override\r\npublic boolean onMarkerClick(Marker _param1) {\r\nfinal String _id = _param1.getTag().toString();\r\n" + logic + "\r\nreturn false;\r\n}");
        register("onLocationChanged", (id, logic) -> "@Override\r\npublic void onLocationChanged(Location _param1) {\r\nfinal double _lat = _param1.getLatitude();\r\nfinal double _lng = _param1.getLongitude();\r\nfinal double _acc = _param1.getAccuracy();\r\n" + logic + "\r\n}");
    }
}
