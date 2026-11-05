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

describe('Guacamole.H264AnnexBUtil', function() {

    it('converts AVCC with lengthSize=1 to Annex-B', function() {
        var payload = new Uint8Array([ 0x00, 0x01, 0x65 ]); // len=1, NAL=0x65
        var config = { lengthSize: 1, sps: [], pps: [] };
        var out = Guacamole.H264AnnexBUtil.avccToAnnexB(payload, false, config);
        // 4-byte start code + 1-byte nal
        expect(out.length).toBe(5);
        expect(Array.prototype.slice.call(out)).toEqual([0,0,0,1,0x65]);
    });

    it('prepends SPS/PPS on keyframe', function() {
        var payload = new Uint8Array([ 0x00, 0x01, 0x65 ]);
        var sps = new Uint8Array([0x67, 0x64]);
        var pps = new Uint8Array([0x68, 0xEE]);
        var config = { lengthSize: 1, sps: [sps], pps: [pps] };
        var out = Guacamole.H264AnnexBUtil.avccToAnnexB(payload, true, config);
        // start+SPS + start+PPS + start+IDR
        expect(Array.prototype.slice.call(out)).toEqual([
            0,0,0,1,0x67,0x64,
            0,0,0,1,0x68,0xEE,
            0,0,0,1,0x65
        ]);
    });

    it('supports lengthSize=2 and 4', function() {
        // lengthSize=2, nal len=2 -> [00 02 AA BB]
        var payload2 = new Uint8Array([ 0x00,0x02, 0xAA,0xBB ]);
        var out2 = Guacamole.H264AnnexBUtil.avccToAnnexB(payload2, false, { lengthSize: 2, sps: [], pps: [] });
        expect(Array.prototype.slice.call(out2)).toEqual([0,0,0,1,0xAA,0xBB]);

        // lengthSize=4, nal len=1 -> [00 00 00 01 CC]
        var payload4 = new Uint8Array([ 0x00,0x00,0x00,0x01, 0xCC ]);
        var out4 = Guacamole.H264AnnexBUtil.avccToAnnexB(payload4, false, { lengthSize: 4, sps: [], pps: [] });
        expect(Array.prototype.slice.call(out4)).toEqual([0,0,0,1,0xCC]);
    });
});


