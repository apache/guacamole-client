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
 * Provides the ManagedUSB class used by ManagedClient to represent
 * USB devices connected via WebUSB and tunneled to the remote desktop.
 */
angular.module('client').factory('ManagedUSB', ['$injector', 
    function defineManagedUSB($injector) {

    const $q = $injector.get('$q');

    /**
     * USB connection states.
     * @readonly
     * @enum {String}
     */
    const ConnectionState = {
        DISCONNECTED: 'disconnected',
        CONNECTING: 'connecting',
        CONNECTED: 'connected',
        DISCONNECTING: 'disconnecting'
    };

    /**
     * Object which represents a USB device connected via WebUSB and
     * tunneled to a remote connection.
     * 
     * @constructor
     * @param {ManagedUSB|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     ManagedUSB.
     */
    const ManagedUSB = function ManagedUSB(template) {
        
        template = template || {};

        /**
         * The ManagedClient that owns this USB device.
         * @type {ManagedClient}
         */
        this.client = template.client;

        /**
         * The WebUSB device object.
         * @type {USBDevice}
         */
        this.device = template.device;
        
        /**
         * Unique identifier for this USB connection session.
         * @type {String}
         */
        this.id = template.id || crypto.randomUUID();
        
        /**
         * Current state of the USB device connection.
         * @type {String}
         */
        this.state = template.state || ConnectionState.DISCONNECTED;
        
        /**
         * Array of IN endpoint information for polling.
         * Each entry contains {number, type, packetSize}.
         * @type {Array}
         */
        this.inEndpoints = [];
        
        /**
         * Flag indicating whether polling is currently active.
         * @type {Boolean}
         */
        this.pollingActive = false;
    };

    /**
     * Creates a new ManagedUSB instance from the given WebUSB device for
     * the given client.
     *
     * @param {ManagedClient} client
     *     The client that this USB device should be associated with.
     *
     * @param {USBDevice} device
     *     The WebUSB device to use.
     *
     * @returns {ManagedUSB}
     *     The newly-created ManagedUSB.
     */
    ManagedUSB.getInstance = function getInstance(client, device) {
        return new ManagedUSB({ client, device });
    };

    /**
     * Collects device information including interfaces and endpoints.
     * Claims all interfaces and builds endpoint data for polling.
     * 
     * @returns {Promise<String>}
     *     Promise that resolves with formatted interface data string.
     */
    ManagedUSB.prototype.collectDeviceInfo = function collectDeviceInfo() {
        
        const config = this.device.configuration;
        
        // Return empty string if no configuration available
        if (!config?.interfaces)
            return Promise.resolve("");

        // Attempt to claim all interfaces
        const claimPromises = config.interfaces.map(iface => 
            this.device.claimInterface(iface.interfaceNumber)
                .then(() => iface)
                .catch(() => null) // Return null for interfaces we can't claim
        );

        return Promise.all(claimPromises).then(interfaces => {
            
            // Filter out failed interface claims
            const claimed = interfaces.filter(Boolean);
            
            // Clear and rebuild IN endpoints list
            this.inEndpoints = [];
            
            // Build interface data string for protocol
            const interfaceData = claimed.map(iface => {
                
                const alt = iface.alternates[0];
                
                // Skip if no alternates available
                if (!alt)
                    return '';
                
                // Collect IN endpoints with their properties for polling
                alt.endpoints.forEach(ep => {
                    if (ep.direction === 'in') {
                        this.inEndpoints.push({
                            number: ep.endpointNumber,
                            type: ep.type,
                            packetSize: ep.packetSize || 64
                        });
                    }
                });
                
                // Format endpoints as string
                const endpoints = alt.endpoints.map(ep => 
                    `${ep.endpointNumber}:${ep.direction}:${ep.type}:${ep.packetSize}`
                ).join(';');
                
                // Return formatted interface string
                return `${iface.interfaceNumber}:${alt.interfaceClass}:${alt.interfaceSubclass}:${alt.interfaceProtocol}:${endpoints}`;
                
            }).filter(Boolean).join(',');
            
            return interfaceData;
        });
    };

    /**
     * Initiates connecting the USB device to the remote connection.
     *
     * @returns {Promise<ManagedUSB>}
     *     A promise that resolves when the device is connected, or rejects if
     *     an error occurs.
     */
    ManagedUSB.prototype.connect = function connect() {
        
        // Cannot connect if not disconnected
        if (this.state !== ConnectionState.DISCONNECTED)
            return $q.reject(new Error(`Cannot connect from state: ${this.state}`));
        
        this.state = ConnectionState.CONNECTING;
        const self = this;
        
        // Open device and collect information
        return this.device.open()
            .then(() => self.collectDeviceInfo())
            .then(interfaceData => {
                
                // Send connection info to server
                self.client.client.sendUSBConnect(
                    self.id,
                    self.device.vendorId,
                    self.device.productId,
                    self.device.productName || '',
                    self.device.serialNumber || '',
                    self.device.deviceClass,
                    self.device.deviceSubclass,
                    self.device.deviceProtocol,
                    interfaceData
                );
                
                // Mark as connected and start polling
                self.state = ConnectionState.CONNECTED;
                self.startPolling();
                
                return self;
            })
            .catch(error => {
                
                // Reset state and cleanup on error
                self.state = ConnectionState.DISCONNECTED;
                self.cleanup();
                throw error;
                
            });
    };

    /**
     * Starts polling all IN endpoints concurrently for incoming data from the 
     * USB to be sent to the server.
     */
    ManagedUSB.prototype.startPolling = function startPolling() {
        
        // Don't start if no endpoints or already polling
        if (this.inEndpoints.length === 0 || this.pollingActive)
            return;
        
        this.pollingActive = true;
        const self = this;
        
        // Start a separate polling loop for each IN endpoint
        this.inEndpoints.forEach(endpoint => {
            self.pollEndpoint(endpoint);
        });
    };
    
    /**
     * Polls a single endpoint continuously for incoming data.
     * 
     * @param {Object} endpointInfo
     *     The endpoint information object containing 
     *     {number, type, and packetSize}.
     */
    ManagedUSB.prototype.pollEndpoint = function pollEndpoint(endpointInfo) {
        
        const self = this;
        
        // Set polling intervals based on endpoint type
        const baseInterval = endpointInfo.type === 'interrupt' ? 1 : 10;
        const errorInterval = baseInterval * 10;
        
        function poll() {
            
            // Stop polling if disconnected or polling disabled
            if (!self.pollingActive || self.state !== ConnectionState.CONNECTED)
                return;
            
            // Attempt to read from endpoint
            self.device.transferIn(endpointInfo.number, endpointInfo.packetSize)
                .then(result => {
                    
                    // Send data to server if received
                    if (result.data?.byteLength > 0)
                        self.sendDataToServer(endpointInfo, result.data);
                    
                    // Continue polling with base interval
                    if (self.pollingActive)
                        setTimeout(poll, baseInterval);
                    
                })
                .catch(() => {
                    
                    // On error, wait longer before retry
                    if (self.pollingActive)
                        setTimeout(poll, errorInterval);
                    
                });
        }
        
        // Start polling this endpoint
        poll();
    };

    /**
     * Sends data from a USB device endpoint to the remote connection.
     *
     * @param {Object} endpointInfo
     *     The endpoint information object containing 
     *     {number, type, and packetSize}.
     *
     * @param {ArrayBuffer} data
     *     The data to send to the remote connection.
     */
    ManagedUSB.prototype.sendDataToServer = function sendDataToServer(
            endpointInfo, data) {
        
        // Don't send if not connected
        if (this.state !== ConnectionState.CONNECTED)
            return;
        
        try {
            
            // Convert to base64 and send to server
            const base64 = this.arrayBufferToBase64(data);
            this.client.client.sendUSBData(this.id, endpointInfo.number, base64, 
                    endpointInfo.type);
            
        } catch (error) {
            console.error("Failed to send USB data:", error);
        }
    };

    /**
     * Handles data received from the remote connection.
     *
     * @param {String} deviceId
     *     The device ID this data is intended for.
     *
     * @param {Number} endpoint
     *     The target endpoint number for this data.
     *
     * @param {String} data
     *     The base64-encoded data received from the remote connection.
     */
    ManagedUSB.prototype.handleRemoteData = function handleRemoteData(
            deviceId, endpoint, data) {
        
        // Ignore if not connected or wrong device
        if (this.state !== ConnectionState.CONNECTED || deviceId !== this.id)
            return;
        
        try {
            
            // Decode and send to USB device
            const arrayBuffer = this.base64ToArrayBuffer(data);
            
            // Send to endpoint, let WebUSB handle validation
            this.device.transferOut(endpoint, arrayBuffer)
                .catch(error => {
                    console.error(
                            `USB transfer failed for endpoint ${endpoint}:`, error);
                });
                
        } catch (error) {
            console.error("Failed to process remote data:", error);
        }
    };

    /**
     * Stops all endpoint polling loops.
     */
    ManagedUSB.prototype.stopPolling = function stopPolling() {
        
        // Set flag - polling loops will stop themselves when they check it
        this.pollingActive = false;
        
    };

    /**
     * Disconnects the USB device from this client.
     *
     * @returns {Promise}
     *     A promise that resolves when the device is disconnected.
     */
    ManagedUSB.prototype.disconnect = function disconnect() {
        
        // Already disconnected
        if (this.state === ConnectionState.DISCONNECTED)
            return $q.resolve();
        
        this.state = ConnectionState.DISCONNECTING;
        
        // Notify server of disconnection
        if (this.client?.client)
            this.client.client.sendUSBDisconnect(this.id);
        
        // Stop polling and cleanup
        this.stopPolling();
        
        return this.cleanup().then(() => {
            this.state = ConnectionState.DISCONNECTED;
        });
    };

    /**
     * Cleans up local USB device resources.
     *
     * @returns {Promise}
     *     A promise that resolves when cleanup is complete.
     */
    ManagedUSB.prototype.cleanup = function cleanup() {
        
        // Stop any active polling
        this.stopPolling();
        
        // Close device if open
        if (this.device?.opened)
            return this.device.close().catch(() => {});
            
        return $q.resolve();
    };

    /**
     * Converts an ArrayBuffer to a base64-encoded string.
     *
     * @param {ArrayBuffer} buffer
     *     The buffer to encode.
     *
     * @returns {String}
     *     The base64-encoded string.
     */
    ManagedUSB.prototype.arrayBufferToBase64 = function arrayBufferToBase64(buffer) {
        
        const bytes = new Uint8Array(buffer);
        let binary = '';
        
        for (let i = 0; i < bytes.length; i++)
            binary += String.fromCharCode(bytes[i]);
            
        return btoa(binary);
    };

    /**
     * Converts a base64-encoded string to an ArrayBuffer.
     *
     * @param {String} base64
     *     The base64 string to decode.
     *
     * @returns {ArrayBuffer}
     *     The decoded ArrayBuffer.
     */
    ManagedUSB.prototype.base64ToArrayBuffer = function base64ToArrayBuffer(base64) {
        
        const binary = atob(base64);
        const bytes = new Uint8Array(binary.length);
        
        for (let i = 0; i < binary.length; i++)
            bytes[i] = binary.charCodeAt(i);
            
        return bytes.buffer;
    };

    return ManagedUSB;
    
}]);