#!/bin/bash -e
#
# Copyright (C) 2015 Glyptodon LLC
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.
#

##
## @fn encrypt-json.sh 
##
## Encrypts and signs JSON using the provided key, returning base64-encoded
## data ready to be submitted to Guacamole and used by the guacamole-auth-json
## authentication provider. Beware that this base64-encoded must still be
## URL-encoded prior to submission to /api/tokens via POST. Base64 encoding may
## contain + and = characters, which have special meaning in URLs.
##
## To submit the resulting data easily via curl, the following will work:
##
## curl --data-urlencode "data=$(<file_containing_result)" GUAC_URL/api/tokens
##
## @param SECRET_KEY
##     The key to encrypt and sign the JSON file with, as a 16-byte (32-digit)
##     hexadecimal value. This key must match the key specified within
##     guacamole.properties using the "json-secret-key" property.
##
## @param JSON_FILENAME
##     The filename of the JSON to encrypt and sign.
##

##
## Encryption/signing key.
##
SECRET_KEY="$1"

##
## The filename of the JSON data being signed and encrypted.
##
JSON_FILENAME="$2"

##
## A null (all zeroes) IV.
##
NULL_IV="00000000000000000000000000000000"

##
## Signs the contents of the given file using the given key. The signature is
## created using HMAC/SHA-256, and is output in binary form to STDOUT, followed
## by the raw contents of the file.
##
## @param KEY
##     The key to use to sign the contents of the given file with HMAC/SHA-256.
##
## @param FILENAME
##     The filename of the file to sign.
##
sign() {

    KEY="$1"
    FILENAME="$2"

    #
    # Write out signature
    #

    openssl dgst                                \
        -sha256 -mac HMAC -macopt hexkey:"$KEY" \
        -binary "$FILENAME"

    #
    # Write out file contents
    #

    cat "$FILENAME"

}

##
## Encrypts all data received through STDIN using the provided key. Data is
## encrypted using 128-bit AES in CBC mode (with a null IV). The encrypted
## result is printed to STDOUT encoded with base64.
##
## @param KEY
##     The key to encrypt STDIN with, as a 16-byte (32-digit) hexadecimal
##     value.
##
encrypt() {

    KEY="$1"

    #
    # Encrypt STDIN
    #

    openssl enc -aes-128-cbc -K "$KEY" -iv "$NULL_IV" -nosalt -a

}

#
# Sign and encrypt file using secret key
#

sign "$SECRET_KEY" "$JSON_FILENAME" | encrypt "$SECRET_KEY"

