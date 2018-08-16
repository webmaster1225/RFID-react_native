package com.novadart.reactnativenfc;

import org.json.JSONObject;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.novadart.reactnativenfc.parser.NdefParser;
import com.novadart.reactnativenfc.parser.TagParser;

public class ReactNativeNFCModule extends ReactContextBaseJavaModule implements ActivityEventListener,LifecycleEventListener {
    public static final String MIME_TEXT_PLAIN = "text/plain";
    private static final String EVENT_NFC_DISCOVERED = "__NFC_DISCOVERED";
    private static final String EVENT_NFC_ERROR = "__NFC_ERROR";
    private static final String EVENT_NFC_MISSING = "__NFC_MISSING";
    private static final String EVENT_NFC_UNAVAILABLE = "__NFC_UNAVAILABLE";
    private static final String EVENT_NFC_ENABLED = "__NFC_ENABLED";
    private NfcAdapter adapter;

    // caches the last message received, to pass it to the listeners when it reconnects
    private WritableMap startupNfcData;
    private boolean startupNfcDataRetrieved = false;

    private boolean startupIntentProcessed = false;

    public ReactNativeNFCModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(this);
        reactContext.addLifecycleEventListener(this);
        adapter = NfcAdapter.getDefaultAdapter(reactContext);
    }

    @Override
    public String getName() {
        return "ReactNativeNFC";
    }


    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {}

    @Override
    public void onNewIntent(Intent intent) {
       handleIntent(intent,false);
    }

   /**
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     * @param nfcAdapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void setupForegroundDispatch(final Activity activity, NfcAdapter nfcAdapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
 
        final PendingIntent pendingIntent = PendingIntent.getActivity(
                activity.getApplicationContext(), 0, intent, 0);
 
        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};
 
        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            filters[0].addDataType(MIME_TEXT_PLAIN);
        } catch (MalformedMimeTypeException e) {
            Log.d("NFC_PLUGIN_LOG", "Check your mime type");
            throw new RuntimeException("Check your mime type.");
        }
         
        nfcAdapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }

    /**
     * @param activity The corresponding {@link BaseActivity} requesting to stop the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }

    private void handleIntent(Intent intent, boolean startupIntent) {
        if (intent != null && intent.getAction() != null) {

            switch (intent.getAction()){

                case NfcAdapter.ACTION_ADAPTER_STATE_CHANGED:
                    Log.d("NFC_PLUGIN_LOG", "ACTION_ADAPTER_STATE_CHANGED");
                    Parcelable[] raws = intent.getParcelableArrayExtra(
                            NfcAdapter.EXTRA_NDEF_MESSAGES);

                    for (Parcelable row : raws){
                        System.out.println(row);
                    }
                    break;

                case NfcAdapter.ACTION_NDEF_DISCOVERED:
                    Log.d("NFC_PLUGIN_LOG", "ACTION_NDEF_DISCOVERED");
                    Parcelable[] rawMessages = intent.getParcelableArrayExtra(
                            NfcAdapter.EXTRA_NDEF_MESSAGES);
                    
                    for (Parcelable row : rawMessages){
                        System.out.println(row);
                    }

                    if (rawMessages != null) {
                        NdefMessage[] messages = new NdefMessage[rawMessages.length];
                        for (int i = 0; i < rawMessages.length; i++) {
                            messages[i] = (NdefMessage) rawMessages[i];
                        }

                        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                        String serialNumber = getSerialNumber(tag);

                        processNdefMessages(serialNumber,messages,startupIntent);
                    }
                    break;

                // ACTION_TAG_DISCOVERED is an unlikely case, according to https://developer.android.com/guide/topics/connectivity/nfc/nfc.html
                case NfcAdapter.ACTION_TAG_DISCOVERED:
                case NfcAdapter.ACTION_TECH_DISCOVERED:
                    Log.d("NFC_PLUGIN_LOG", "ACTION_TECH_DISCOVERED/ACTION_TAG_DISCOVERED");
                    Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                    String serialNumber = getSerialNumber(tag);

                    processTag(serialNumber,tag,startupIntent);
                    break;

            }
            // stopForegroundDispatch(getReactApplicationContext().getCurrentActivity(), adapter);
        }
    }

    /**
     * This method is used to retrieve the NFC data was acquired before the React Native App was loaded.
     * It should be called only once, when the first listener is attached.
     * Subsequent calls will return null;
     *
     * @param callback callback passed by javascript to retrieve the nfc data
     */
    @ReactMethod
    public void getStartUpNfcData(Callback callback){
        if(!startupNfcDataRetrieved){
            callback.invoke(DataUtils.cloneWritableMap(startupNfcData));
            startupNfcData = null;
            startupNfcDataRetrieved = true;
        } else {
            callback.invoke();
        }
    }

    @ReactMethod
    public void initialize() {
        if(adapter != null) {
            if (adapter.isEnabled()) {
                Log.d("NFC_PLUGIN_LOG", "Reader should be started here, but not sure if that is how this works");
                setupForegroundDispatch(getCurrentActivity(), adapter);
                sendResponseEvent(EVENT_NFC_ENABLED, null);
            }else{
                sendResponseEvent(EVENT_NFC_MISSING, null);
            }
        }else{
            sendResponseEvent(EVENT_NFC_UNAVAILABLE, null);
        }
    }

    @ReactMethod
    public void isSupported(){
        if(adapter != null){
            if(adapter.isEnabled()){
                Log.d("NFC_PLUGIN_LOG", "EVENT_NFC_ENABLED THE THING IS ENABLED");
                sendResponseEvent(EVENT_NFC_ENABLED, null);
            }else{
                Log.d("NFC_PLUGIN_LOG", "EVENT_NFC_MISSING THE THING EXISTS BUT IS NOT ENABLED");
                sendResponseEvent(EVENT_NFC_MISSING, null);
            }
        }else{
            Log.d("NFC_PLUGIN_LOG", "EVENT_NFC_ENABLED THE THING IS SEEMS TO NOT HAVE THE THING");
            sendResponseEvent(EVENT_NFC_UNAVAILABLE, null);
        }
    }

    private void sendEvent(@Nullable WritableMap payload) {
        payload.putString("origin", "android");
        getReactApplicationContext()
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(EVENT_NFC_DISCOVERED, payload);
    }

    private void sendResponseEvent(String event, @Nullable WritableMap payload) {
        if(payload != null){
            payload.putString("origin", "android");
        }
        getReactApplicationContext()
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(event, payload);
    }

    private String getSerialNumber(Tag tag){
        byte[] id = tag.getId();
        String serialNumber = DataUtils.bytesToHex(id);

        return serialNumber;
    }

    private void processNdefMessages(String serialNumber, NdefMessage[] messages, boolean startupIntent){
        NdefProcessingTask task = new NdefProcessingTask(serialNumber, startupIntent);
        task.execute(messages);
    }

    private void processTag(String serialNumber, Tag tag, boolean startupIntent){
        TagProcessingTask task = new TagProcessingTask(serialNumber, startupIntent);
        task.execute(tag);
    }

    @Override
    public void onHostResume() {
        if(!startupIntentProcessed){
            if(getReactApplicationContext().getCurrentActivity() != null){ // it shouldn't be null but you never know
                // necessary because NFC might cause the activity to start and we need to catch that data too
                handleIntent(getReactApplicationContext().getCurrentActivity().getIntent(),true);
            }
            startupIntentProcessed = true;
        }
    }

    @Override
    public void onHostPause() {}

    @Override
    public void onHostDestroy() {}


    private class NdefProcessingTask extends AsyncTask<NdefMessage[],Void,WritableMap> {

        private final String serialNumber;
        private final boolean startupIntent;

        NdefProcessingTask(String serialNumber, boolean startupIntent) {
            this.serialNumber = serialNumber;
            this.startupIntent = startupIntent;
        }

        @Override
        protected WritableMap doInBackground(NdefMessage[]... params) {
            NdefMessage[] messages = params[0];
            return NdefParser.parse(serialNumber, messages);
        }

        @Override
        protected void onPostExecute(WritableMap ndefData) {
            if(startupIntent) {
                startupNfcData = ndefData;
            }
            sendEvent(ndefData);
        }
    }


    private class TagProcessingTask extends AsyncTask<Tag,Void,WritableMap> {

        private final String serialNumber;
        private final boolean startupIntent;

        TagProcessingTask(String serialNumber, boolean startupIntent) {
            this.serialNumber = serialNumber;
            this.startupIntent = startupIntent;
        }

        @Override
        protected WritableMap doInBackground(Tag... params) {
            Tag tag = params[0];
            return TagParser.parse(serialNumber, tag);
        }

        @Override
        protected void onPostExecute(WritableMap tagData) {
            if(startupIntent) {
                startupNfcData = tagData;
            }
            sendEvent(tagData);
        }
    }


}
