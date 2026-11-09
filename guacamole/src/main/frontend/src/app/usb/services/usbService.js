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
 * Service for interacting with USB devices through WebUSB API
 */
angular.module('usb').factory('usbService', ['$injector', 
    function usbService($injector) {
        
        const $q = $injector.get('$q');
        const $rootScope = $injector.get('$rootScope');
        const ManagedClient = $injector.get('ManagedClient');
        const service = {};
        
        /**
         * Whether WebUSB is supported in the current browser.
         * 
         * @type {Boolean}
         */
        service.isWebUSBSupported = !!navigator.usb;
        
        /**
         * Checks if a USB device is already connected to a client.
         * 
         * @param {ManagedClient} client
         *     The client to check.
         *     
         * @param {USBDevice} device
         *     The USB device to check.
         *
         * @returns {Boolean}
         *     true if the device is already connected to the client, false otherwise.
         */
        service.isDeviceConnected = function isDeviceConnected(client, device) {
            if (!client || !client.usbDevices || !device)
                return false;
            
            return client.usbDevices.some(managedUSB => 
                managedUSB.device && managedUSB.device.serialNumber === device.serialNumber);
        };
        
        /**
         * Finds a ManagedUSB instance for a device in a client.
         * 
         * @param {ManagedClient} client
         *     The client to search in.
         *     
         * @param {USBDevice} device
         *     The device to find.
         *     
         * @returns {ManagedUSB|null}
         *     The ManagedUSB instance if found, null otherwise.
         */
        service.findManagedUSB = function findManagedUSB(client, device) {
            if (!client || !client.usbDevices || !device)
                return null;
            
            return client.usbDevices.find(managedUSB => 
                managedUSB.device && managedUSB.device.serialNumber === device.serialNumber);
        };
        
        /**
         * Request permission to access a specific USB device
         * 
         * @param {Object[]} [filters]
         *     USB device filters to limit selection
         *     
         * @returns {Promise.<USBDevice>}
         *     A promise that resolves with the selected USB device
         */
        service.requestDevice = function requestDevice(filters) {
            const deferred = $q.defer();
            
            if (!service.isWebUSBSupported) {
                deferred.reject(
                        new Error("WebUSB API is not available in this browser"));
                return deferred.promise;
            }
            
            navigator.usb.requestDevice({ filters: filters || [] })
                .then(device => {
                    deferred.resolve(device);
                })
                .catch(error => {
                    deferred.reject(error);
                });
                
            return deferred.promise;
        };
        
        /**
         * Connect to a USB device and associate it with a ManagedClient
         * 
         * @param {USBDevice} device
         *     The USB device to connect
         *     
         * @param {ManagedClient} managedClient
         *     The managed client that will use this device
         *     
         * @returns {Promise.<ManagedUSB>}
         *     A promise that resolves with the ManagedUSB instance
         */
        service.connectDevice = function connectDevice(
                device, managedClient) {
            const deferred = $q.defer();
            
            if (!device) {
                deferred.reject(new Error("No device provided"));
                return deferred.promise;
            }
            
            if (!managedClient) {
                deferred.reject(new Error("No client provided"));
                return deferred.promise;
            }
            
            // Check if device is already connected to this client
            if (service.isDeviceConnected(managedClient, device)) {
                const existingUSB = service.findManagedUSB(
                        managedClient, device);
                deferred.resolve(existingUSB);
                return deferred.promise;
            }
            
            // Use ManagedClient's method to connect the device
            ManagedClient.connectUSBDevice(managedClient, device)
                .then(managedUSB => {
                    deferred.resolve(managedUSB);
                })
                .catch(error => {
                    console.error("Failed to connect USB device:", error);
                    deferred.reject(error);
                });
                
            return deferred.promise;
        };
        
        /**
         * Disconnect a USB device from a managed client
         * 
         * @param {USBDevice} device
         *     The USB device to disconnect
         *     
         * @param {ManagedClient} managedClient
         *     The managed client the device is connected to
         *     
         * @returns {Promise}
         *     A promise that resolves when the device is disconnected
         */
        service.disconnectDevice = function disconnectDevice(
                device, managedClient) {
            const deferred = $q.defer();
            
            if (!device || !managedClient) {
                deferred.resolve();
                return deferred.promise;
            }
            
            // Find the ManagedUSB instance for this device
            const managedUSB = service.findManagedUSB(managedClient, device);
            
            if (!managedUSB) {
                deferred.resolve();
                return deferred.promise;
            }
            
            // Use ManagedClient's method to disconnect the device
            ManagedClient.disconnectUSBDevice(managedClient, managedUSB)
                .then(() => {
                    deferred.resolve();
                })
                .catch(error => {
                    console.error("Error disconnecting USB device:", error);
                    deferred.reject(error);
                });
            
            return deferred.promise;
        };
        
        /**
         * Get all USB devices connected to a client
         * 
         * @param {ManagedClient} managedClient
         *     The client to get devices for
         *     
         * @returns {ManagedUSB[]}
         *     Array of connected USB devices
         */
        service.getConnectedDevices = function getConnectedDevices(managedClient) {
            if (!managedClient || !managedClient.usbDevices)
                return [];
            
            return managedClient.usbDevices;
        };
        
        return service;
    }
]);