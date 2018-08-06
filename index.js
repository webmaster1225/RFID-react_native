'use strict';

import { 
  NativeModules, 
  DeviceEventEmitter, 
  NativeEventEmitter,
  Platform 
} from 'react-native';

export const NfcDataType = {
    NDEF : "NDEF",
    TAG : "TAG"
};

export const NdefRecordType = {
    TEXT : "TEXT",
    URI : "URI",
    MIME : "MIME"
};

const { ReactNativeNFC } = NativeModules;
const eventEmitter = new NativeEventEmitter(ReactNativeNFC);
const NFC_DISCOVERED = '__NFC_DISCOVERED';
const NFC_ERROR = '__NFC_ERROR';
const NFC_MISSING = '__NFC_MISSING';
const NFC_ENABLED = '__NFC_ENABLED';
const startUpNfcData = ReactNativeNFC.getStartUpNfcData || (() => ({}));

let _enabled = true;
let _registeredToEvents = false;
let _listeners = {};
let _loading = false;

if (Platform.OS == "ios") {
    eventEmitter.addListener(NFC_MISSING, ()=>{_enabled = false; _loading = false;});
    eventEmitter.addListener(NFC_ENABLED, ()=>{_enabled = true; _loading = false;});
    ReactNativeNFC.isSupported()
    _loading = true;
}

let _registerToEvents = () => {
    if(!_registeredToEvents){
        try{
            startUpNfcData(_notifyListeners);
            DeviceEventEmitter.addListener(NFC_DISCOVERED, _notifyListeners);
            DeviceEventEmitter.addListener(NFC_ERROR, _notifyListeners);
        }catch(androidErr){
            console.log("androidErr", androidErr);
        }
        try{
            eventEmitter.addListener(NFC_DISCOVERED, _notifyListeners);
            eventEmitter.addListener(NFC_ERROR, _notifyListeners);
        }catch(iosErr){
            console.log("iosErr", iosErr);
        }
        _registeredToEvents = true;
    }
};

let _notifyListeners = (data) => {
    if(data){
        for(let _listener in _listeners){
            _listeners[_listener](data);
        }
    }
};

const NFC = {};

NFC.initialize = () => {
    const init = ReactNativeNFC.initialize || (() => ({}));
    if(_enabled && !_loading){
        init();
    }else {
        throw new Error("NFC Controller object is not ready");
    }
};

NFC.isEnabled = ()=>{
    return _enabled && !_loading;
}

NFC.addListener = (name, callback) => {
    _listeners[name] = callback;
    _registerToEvents();
};

NFC.removeListener = (name) => {
    delete _listeners[name];
};

NFC.removeAllListeners = () => {
    try{
        DeviceEventEmitter.removeAllListeners(NFC_DISCOVERED);
        DeviceEventEmitter.removeAllListeners(NFC_ERROR);
    }catch(androidErr){
        console.log("androidErr", androidErr);
    }
    try{
        eventEmitter.removeAllListeners(NFC_DISCOVERED);
        eventEmitter.removeAllListeners(NFC_ERROR);
    }catch(iosErr){
        console.log("iosErr", iosErr);
    }
    _listeners = {};
    _registeredToEvents = false;
};

export default NFC;