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

var Guacamole = Guacamole || {};

/**
 * Utilities for converting H.264 AVCC format to Annex-B.
 */
Guacamole.H264AnnexBUtil = (function() {

    /**
     * Converts an AVCC payload into Annex-B format. If isKey is true and the
     * provided config contains SPS/PPS, those will be prepended using start
     * codes. The config.lengthSize determines the size (in bytes) of the
     * length prefixes for NAL units within the AVCC payload.
     *
     * @param {!Uint8Array} payload
     *     The AVCC payload containing one or more NAL units with length
     *     prefixes of size config.lengthSize.
     *
     * @param {!boolean} isKey
     *     Whether this payload corresponds to a keyframe (IDR).
     *
     * @param {{ lengthSize: number, sps: Uint8Array[], pps: Uint8Array[] }|null} config
     *     Decoder configuration including SPS/PPS and the AVCC lengthSize. If
     *     null, SPS/PPS will not be included and a default lengthSize of 4 is
     *     assumed.
     *
     * @returns {!Uint8Array}
     *     The Annex-B formatted byte sequence.
     */
    function avccToAnnexB(payload, isKey, config) {
        var startCode = new Uint8Array([0x00, 0x00, 0x00, 0x01]);
        var outParts = [];

        var haveConfig = !!config;
        var lenSize = (haveConfig && config.lengthSize) ? config.lengthSize : 4;

        if (isKey && haveConfig) {
            for (var i = 0; i < (config.sps ? config.sps.length : 0); i++) {
                outParts.push(startCode);
                outParts.push(config.sps[i]);
            }
            for (var j = 0; j < (config.pps ? config.pps.length : 0); j++) {
                outParts.push(startCode);
                outParts.push(config.pps[j]);
            }
        }

        var dv = new DataView(payload.buffer, payload.byteOffset, payload.byteLength);
        var off = 0;
        while (off + lenSize <= dv.byteLength) {
            var nalLen = 0;
            for (var k = 0; k < lenSize; k++)
                nalLen = (nalLen << 8) | dv.getUint8(off + k);
            off += lenSize;
            if (nalLen <= 0)
                continue;
            if (off + nalLen > dv.byteLength)
                break;
            var nal = new Uint8Array(payload.buffer, payload.byteOffset + off, nalLen);
            outParts.push(startCode);
            outParts.push(nal);
            off += nalLen;
        }

        var total = 0;
        for (var p = 0; p < outParts.length; p++) total += outParts[p].length;
        var out = new Uint8Array(total);
        var pos = 0;
        for (var q = 0; q < outParts.length; q++) {
            out.set(outParts[q], pos);
            pos += outParts[q].length;
        }

        return out;
    }

    return {
        avccToAnnexB: avccToAnnexB
    };

})();

