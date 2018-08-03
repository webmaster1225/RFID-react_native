//
//  ReactNativeNFC.swift
//
//  Created by Alexander Obi.
//  Copyright © 2017 Smartrac. All rights reserved.
//
//

import CoreNFC

@objc(ReactNativeNFC)
class ReactNativeNFC: RCTEventEmitter, NFCNDEFReaderSessionDelegate {
  
  let nfcHelper = NFCHelper()
  
  @objc func initialize() -> Void {
    if #available(iOS 11.0, *) {
      if(NFCNDEFReaderSession.readingAvailable){
        let session = NFCNDEFReaderSession(delegate: self, queue: DispatchQueue.main, invalidateAfterFirstRead: true)
        session.begin()
      }else{
        let error = NSError(domain:"", code:-110, userInfo:nil)
        let data = nfcHelper.formatError(error)
        sendErrorEvent(data)
      }
    }else{
      let error = NSError(domain:"", code:-111, userInfo:nil)
      let data = nfcHelper.formatError(error)
      sendErrorEvent(data)
    }
  }
  
  override func supportedEvents() -> [String]! {
    return ["__NFC_DISCOVERED", "__NFC_ERROR"]
  }
  
  func sendEvent(_ data : Any) -> Void {
    sendEvent(withName: "__NFC_DISCOVERED", body: data)
  }
  
  func sendErrorEvent(_ data : Any) -> Void {
    sendEvent(withName: "__NFC_ERROR", body: data)
  }
  
  @available(iOS 11.0, *)
  func readerSession(_ session: NFCNDEFReaderSession, didInvalidateWithError error: Error) -> Void {
    let data = nfcHelper.formatError(error)
    sendErrorEvent(data)
  }
  
  @available(iOS 11.0, *)
  func readerSession(_ session: NFCNDEFReaderSession, didDetectNDEFs messages: [NFCNDEFMessage]) -> Void {
    let data = nfcHelper.formatData(messages)
    sendEvent(data)
  }
  
}
