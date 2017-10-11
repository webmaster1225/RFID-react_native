'use strict';

import { NativeModules, DeviceEventEmitter } from 'react-native';

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
let _registeredToEvents = false;
let _listeners = {};

let _registerToEvents = () => {
    if(!_registeredToEvents){
        ReactNativeNFC.getStartUpNfcData(_notifyListeners);
        DeviceEventEmitter.addListener(NFC_DISCOVERED, _notifyListeners);
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

NFC.addListener = (name = NFC_DISCOVERED, callback) => {
    _listeners[name] = callback;
    _registerToEvents();
    eventEmitter.addListener(name, callback);
};

NFC.removeListener = (name = NFC_DISCOVERED) => {
    delete _listeners[name];
    eventEmitter.removeAllListeners(NFC_DISCOVERED);
};

NFC.removeAllListeners = () => {
    DeviceEventEmitter.removeAllListeners(NFC_DISCOVERED);
    eventEmitter.removeAllListeners(NFC_DISCOVERED);
    _listeners = {};
    _registeredToEvents = false;
};

export default NFC;