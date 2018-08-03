'use strict';

import { 
  NativeModules, 
  DeviceEventEmitter, 
  NativeEventEmitter 
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
const startUpNfcData = ReactNativeNFC.getStartUpNfcData || (() => ({}));

let _registeredToEvents = false;
let _listeners = {};
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

NFC.initialize = ReactNativeNFC.initialize || (() => ({}));

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