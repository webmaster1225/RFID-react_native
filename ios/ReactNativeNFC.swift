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

  @objc func isSupported() -> Void {
    if #available(iOS 11.0, *) {
      if NFCNDEFReaderSession.readingAvailable {
        sendEvent(withName: "__NFC_ENABLED")
      }
    }
    sendNFCMissingEvent({})
  }

  @objc func initialize() -> Void {
    if #available(iOS 11.0, *) {
      if NFCNDEFReaderSession.readingAvailable {
        let session = NFCNDEFReaderSession(delegate: self, queue: DispatchQueue.main, invalidateAfterFirstRead: true)
        session.begin()
      } else {
        let error = NSError(domain: "", code: -110, userInfo: nil)
        let data = nfcHelper.formatHardwareError(error)
        sendNFCMissingEvent(data)
      }
    } else {
      let error = NSError(domain: "", code: -111, userInfo: nil)
      let data = nfcHelper.formatHardwareError(error)
      sendNFCMissingEvent(data)
    }
  }

  override func supportedEvents() -> [String]! {
    return ["__NFC_DISCOVERED", "__NFC_ERROR", "__NFC_MISSING", "__NFC_ENABLED"]
  }

  func sendEvent(_ data: Any) -> Void {
    sendEvent(withName: "__NFC_DISCOVERED", body: data)
  }

  func sendErrorEvent(_ data: Any) -> Void {
    sendEvent(withName: "__NFC_ERROR", body: data)
  }

  func sendNFCMissingEvent(_ data: Any) -> Void {
    sendEvent(withName: "__NFC_MISSING", body: data)
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
