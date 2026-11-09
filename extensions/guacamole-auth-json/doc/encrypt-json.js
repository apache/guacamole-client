/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * This file provides a sample/reference implementation for
 * taking plain-text JSON data and signing and encrypting it
 * for use by the guacamole-auth-json authentication extension.
 */

// Some connection data
let json=`{
  "username" : "test",
  "connections" : {
    "My Connection" : {
      "protocol" : "rdp",
      "parameters" : {
        "hostname" : "10.10.209.63",
        "port":"3389",
        "ignore-cert":"true",
        "recording-path":"/recordings",
        "recording-name":"My-Connection-\${GUAC_USERNAME}-\${GUAC_DATE}-\${GUAC_TIME}"
      }
    },
    "My OTHER Connection" : {
      "protocol" : "rdp",
      "parameters" : {
        "hostname":"10.10.209.64",
        "port":"3389",
        "ignore-cert":"true",
        "recording-path":"/recordings",
        "recording-name":"My-OTHER-Connection-\${GUAC_USERNAME}-\${GUAC_DATE}-\${GUAC_TIME}"
      }
    }
  }
}`

// Our signing/encryption key
let key = '2ab26b6438c580e5281edd6639f22e65';

// This is a null IV
let zeroiv = new Uint8Array([ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ]);

/**
 * Encode the provided string of JSON data into an array that can be
 * signed and encrypted, returning the resultant array.
 * 
 * @param json
 *     The string of JSON data to encode.
 *
 * @return
 *     An encoded array of data.
 */
function encodeConnections(json) {
  var encoder = new TextEncoder();
  return encoder.encode(json);
}

/**
 * Take the encoded array and generate an HMAC signature for it,
 * combining the HMAC signature with the data and returning the
 * resultant array.
 *
 * @param key
 *     The 128-bit key used to sign the data, provided as a
 *     16-character hexadecimal value.
 *
 * @param encoded
 *     The encoded data to sign. This should be a Uint8Array.
 *
 * @return
 *     A Uint8Array containing the signature of the encoded
 *     data and the encoded data.
 */
async function signConnections(key, encoded) {

  // Import the key for HMAC signing.
  var keyObject = await window.crypto.subtle.importKey(
    "raw",
    Uint8Array.fromHex(key),
    {
      name: "HMAC",
      hash: "SHA-256"
    },
    false,
    [
      "sign"
    ]
  );
  
  // Generate the HMAC signature
  var signature = new Uint8Array(await window.crypto.subtle.sign(
    "HMAC",
    keyObject,
    encoded
  ));
  
  // Take the signature array and encoded data and combine them.
  var signedArray = new Uint8Array(signature.length + encoded.length);
  signedArray.set(signature);
  signedArray.set(encoded, signature.length);
  
  // Return the combined result.
  return signedArray;
}

/**
 * Given a key, IV, and array of signed data, encrypt the data and
 * return a base64 string that can be used to send the data on
 * to the JSON authentication module.
 *
 * @param key
 *     The 128-bit key to use to encrypt the data, provided as
 *     a 16-character hexadecimal string.
 *
 * @param iv
 *     The IV to use to initialize the encryption, in a
 *     Uint8Array. This can either be null (all zeroes) or
 *     a randomly-generated IV.
 *
 * @param signedData
 *     The Uint8Array containing the signed data that will
 *     be encrypted and sent on to the JSON authentication
 *     module to authenticate the user.
 *
 * @return
 *     A base64-encoded string of encrypted data that can be
 *     safely transmitted over an HTTPS connection to the
 *     JSON authentication module in order to authenticate the
 *     user to Guacamole.
 */
async function encryptData(key, iv, signedData) {

  // Import the key for AES-CBC encryption.
  var keyObject = await window.crypto.subtle.importKey(
    "raw",
    Uint8Array.fromHex(key),
    {
      name: "AES-CBC"
    },
    false,
    [
      "encrypt"
    ]
  );
  
  // Encrypt the data.
  var encrypted = await window.crypto.subtle.encrypt(
    {
      name: "AES-CBC",
      iv: iv
    },
    keyObject,
    signedData
  );
  
  // Return the base64-encoded string.
  return btoa(String.fromCharCode.apply(null, new Uint8Array(encrypted)));

}

// Run the functions, get the data.
var encoded = encodeConnections(json);
var signedData = await signConnections(key, encoded);
var b64encrypted = await encryptData(key, zeroiv, signedData);

// Print it out
console.log(b64encrypted);
