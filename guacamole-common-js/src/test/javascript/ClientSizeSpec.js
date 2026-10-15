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
 * Tests the arguments sent by Guacamole.Client.sendSize(). The optional left
 * offset must be omitted entirely when it is not supplied, since the server
 * distinguishes "not supplied" from any real coordinate -- including zero and
 * negative values -- in order to stay compatible with clients that cannot
 * describe a non-linear monitor arrangement.
 */
describe("Guacamole.Client.sendSize", function ClientSizeSpec() {

    /**
     * The client being tested.
     *
     * @type Guacamole.Client
     */
    var client;

    /**
     * All messages passed to the tunnel, as arrays of arguments.
     *
     * @type Array
     */
    var sent;

    beforeEach(function() {

        sent = [];

        var tunnel = {
            sendMessage : function() { sent.push(Array.prototype.slice.call(arguments)); },
            connect     : function() {},
            disconnect  : function() {},
            isConnected : function() { return true; },
            oninstruction : null,
            onerror       : null,
            onstatechange : null
        };

        client = new Guacamole.Client(tunnel);

        // sendSize() is a no-op unless the client believes it is connected
        tunnel.onstatechange && tunnel.onstatechange(Guacamole.Tunnel.State.OPEN);
        client.connect();
        sent = [];

    });

    it("should omit the left offset when it is not supplied", function() {
        client.sendSize(1024, 768, 0, 0);
        expect(sent.length).toBe(1);
        expect(sent[0]).toEqual([ "size", 1024, 768, 0, 0 ]);
    });

    it("should send the left offset when it is supplied", function() {
        client.sendSize(1024, 768, 1, 0, 1920);
        expect(sent.length).toBe(1);
        expect(sent[0]).toEqual([ "size", 1024, 768, 1, 0, 1920 ]);
    });

    it("should send a left offset of zero rather than omitting it", function() {
        client.sendSize(1024, 768, 1, 100, 0);
        expect(sent.length).toBe(1);
        expect(sent[0]).toEqual([ "size", 1024, 768, 1, 100, 0 ]);
    });

    it("should send a negative left offset unchanged", function() {
        client.sendSize(1024, 768, 1, 0, -1920);
        expect(sent.length).toBe(1);
        expect(sent[0]).toEqual([ "size", 1024, 768, 1, 0, -1920 ]);
    });

    it("should omit the left offset when it is explicitly null", function() {
        client.sendSize(1024, 768, 0, 0, null);
        expect(sent.length).toBe(1);
        expect(sent[0]).toEqual([ "size", 1024, 768, 0, 0 ]);
    });

});
