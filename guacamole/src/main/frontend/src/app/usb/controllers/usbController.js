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

angular.module('usb').controller('usbController', ['$scope', '$injector', '$window',
    function usbController($scope, $injector, $window) {
        
        // Required services
        const usbService = $injector.get('usbService');
        
        // Check if WebUSB is supported
        $scope.webUsbSupported = !!navigator.usb;
        
        // Currently available devices
        $scope.availableDevices = [];

        /**
         * Request a new USB device
         */
        $scope.requestDevice = function requestDevice() {
            
            const client = $scope.client;
            if (!client)
                return;
            
            if (!$scope.webUsbSupported) {
                console.error('WebUSB not supported in this browser');
                return;
            }
            
            // Use the USB service to request a device
            usbService.requestDevice()
                .then(function deviceSelected(device) {
                    // Check if device is already connected
                    if (usbService.isDeviceConnected(client, device))
                        return null;
                    
                    return usbService.connectDevice(device, client);
                })
                .then(function deviceConnected(managedUSB) {
                    if (managedUSB)
                        refreshDeviceList();
                })
                .catch(function deviceRequestFailed(error) {
                    // Handle user cancellation differently
                    if (error.name === 'NotFoundError')
                        console.debug('User cancelled device selection');
                    else
                        console.error("Failed to connect USB device:", error);
                });
        };
        
        /**
         * Disconnect a USB device
         * 
         * @param {USBDevice} device
         *     The device to disconnect
         */
        $scope.disconnectDevice = function disconnectDevice(device) {
            const client = $scope.client;
            if (!client) {
                console.error('No client available');
                return;
            }
            
            usbService.disconnectDevice(device, client)
                .then(function deviceDisconnected() {
                    refreshDeviceList();
                })
                .catch(function deviceDisconnectFailed(error) {
                    console.error("Failed to disconnect USB device:", error);
                });
        };
        
        /**
         * Refresh the list of connected devices
         */
        function refreshDeviceList() {
            const client = $scope.client;
            if (!client) {
                $scope.availableDevices = [];
                return;
            }
            
            // Get devices from the managed client
            const connectedDevices = usbService.getConnectedDevices(client);
            
            // Update the list of available devices with the device objects
            $scope.availableDevices = connectedDevices.map(managedUSB => managedUSB.device);
        }

        // Watch for client changes
        $scope.$watch('client', function clientChanged(client) {
            refreshDeviceList();
        });
        
        // Initial refresh
        refreshDeviceList();
    }
]);