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
      return "NFC Well Known"
    case .media:
      return "Media"
    case .absoluteURI:
      return "Absolute URI"
    case .nfcExternal:
      return "NFC External"
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
  func formatData (_ nfcMessages: [NFCNDEFMessage]) -> [[String: String]] {
    return nfcMessages.map({ (message) -> [[String: String]] in
      let records = message.records
      return records.map({ (payload) -> [String: String] in
        var nfcData: [String: String] = [:]

        let format = getFormatName(payload.typeNameFormat)
        let payloadD = String(data: payload.payload, encoding: String.Encoding.ascii ) as String!
        let type = String(data: payload.type, encoding: String.Encoding.ascii ) as String!
        let identifier = String(data: payload.identifier, encoding: String.Encoding.ascii ) as String!
        let payloadM = (payloadD?.split(separator: "="))
        var tid: String

        if (payloadM?.underestimatedCount ?? 0) > 1 {
          let tidString = String(payloadM![1])
          tid = String(tidString.prefix(while: getCode))
        } else {
          tid = String(payloadM![0])
        }

        nfcData["payload"] = payloadD
        nfcData["tid"] = tid
        nfcData["type"] = type
        nfcData["identifier"] = identifier
        nfcData["format"] = format
        return nfcData
      })
    }).flatMap { $0 }
  }

  // formatError:: Error -> [String: String]
  func formatError (_ error: Error) -> [String: String] {
    var errorObj: [String: String] = [:]
    errorObj["error"] = error.localizedDescription
    return errorObj
  }
}
