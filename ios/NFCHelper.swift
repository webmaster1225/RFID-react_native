//
//  NFCHelper.swift
//
//  Created by Alexander Obi.
//  Copyright © 2017 Smartrac. All rights reserved.
//

import Foundation
import CoreNFC

class NFCHelper: NSObject {

  //getFormatName:: NFCTypeNameFormat -> String
  func getFormatName (_ format: NFCTypeNameFormat) -> String {
    switch format {
    case .empty:
      return "Empty"
    case .nfcWellKnown:
      return "NFC"
    case .media:
      return "Media"
    case .absoluteURI:
      return "Absolute URI"
    case .nfcExternal:
      return "NFC"
    case .unchanged:
      return "Unchanged"
    default:
      return "Unknown"
    }
  }

  func getCode (_ char: Character) -> Bool {
    let charArray: [Character] = ["Q", "\u{01}", "&", "/", "(", ")"]
    return !charArray.contains(char)
  }

  // formatData:: [NFCNDEFMessage] -> [[String: String]]
  @available(iOS 11.0, *)
  func formatData (_ nfcMessages: [NFCNDEFMessage]) -> [String: Any] {
    var format = ""
    let nfcDataMsg = nfcMessages.map({ (message) -> [[String: String]] in
      let records = message.records
      return records.map({ (payload) -> [String: String] in
        var nfcData: [String: String] = [:]

        format = getFormatName(payload.typeNameFormat)
        let payloadD = String(data: payload.payload.advanced(by: 3), encoding: String.Encoding.utf8 ) as String!
        let type = String(data: payload.type, encoding: String.Encoding.utf8 ) as String!
        let identifier = String(data: payload.identifier, encoding: String.Encoding.utf8 ) as String!
        let encoding = String(data: payload.payload.advanced(by: 1).prefix(through: 1), encoding: String.Encoding.utf8 ) as String!

        nfcData["locale"] = encoding
        nfcData["encoding"] = "UTF-8"
        nfcData["type"] = "TEXT"
        nfcData["data"] = payloadD
        return nfcData
      })
    }).flatMap { $0 }

    var nfcMsg:[String:Any] = [:]
    nfcMsg["origin"] = "ios"
    nfcMsg["data"] = [nfcDataMsg]
    nfcMsg["id"] = "unavailable"
    nfcMsg["type"] = format
    return nfcMsg
  }

  // formatError:: Error -> [String: String]
  func formatError (_ error: Error) -> [String: String] {
    var errorObj: [String: String] = [:]
    errorObj["error"] = error.localizedDescription
    return errorObj
  }
}
