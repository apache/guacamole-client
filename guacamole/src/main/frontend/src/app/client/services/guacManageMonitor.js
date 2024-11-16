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
 * A service for adding additional monitors and handle instructions transfer.
 */
angular.module('client').factory('guacManageMonitor', ['$injector',
    function guacManageMonitor($injector) {

    // Required services
    const $window        = $injector.get('$window');
    const guacFullscreen = $injector.get('guacFullscreen');

    const service = {};

    // Additionals monitors windows
    const monitors = {};

    // Guacamole Client
    let client = null;

    // Broadcast channel
    let broadcast = null;

    /**
     * The maximum number of secondary monitors allowed.
     *
     * @type Number
     */
    let maxSecondaryMonitors = 0;

    // Store the last monitor id
    let lastMonitorId = 0;

    // Reference for the ctrl/alt/shift menu
    let guacMenu;

    /**
     * Init the broadcast channel used for bidirectionnal communications between
     * primary and secondary monitor windows and get the gacamole menu reference.
     * 
     * @param {!Object} menu
     *     Reference for the guacamole menu.
     */
    service.init = function init(menu) {

        guacMenu = menu;

        // Create broadcast if supported
        if (!service.supported())
            return;

        broadcast = new BroadcastChannel('guac_monitors');

        /**
         * Handle messages sent by secondary monitors windows in
         * guac_monitors channel.
         * 
         * @param {Event} e
         *     Received message event from guac_monitors channel.
         */
        broadcast.onmessage = function broadcastMessage(message) {
    
            // Send monitors infos and trigger the screen resize event
            if (message.data.resize)
                sendMonitorsInfos();

            // Mouse state changed on secondary screen
            if (message.data.mouseState)
                client.sendMouseState(message.data.mouseState);

            // Key down on secondary screen
            if (message.data.keydown)
                client.sendKeyEvent(1, message.data.keydown);

            // Key up on secondary screen
            if (message.data.keyup)
                client.sendKeyEvent(0, message.data.keyup);

            // Additional window unloaded
            if (message.data.monitorClose)
                service.closeMonitor(message.data.monitorClose);

            // CTRL+ALT+SHIFT pressed on secondary window
            if (message.data.guacMenu)
                guacMenu.shown = !guacMenu.shown;

        }

    };

    /**
     * Set the maximum number of secondary monitors allowed.
     *
     * @param {Number} secondaryMonitorsAllowed
     *     The maximum number of secondary monitors allowed.
     */
    service.setMaxSecondaryMonitors = function setMaxSecondaryMonitors(amount) {
        maxSecondaryMonitors = amount;
    }

    /**
     * Ensure that the limit of open monitors is not reached.
     * 
     * @returns {boolean}
     *     true when the limit of opened monitors is reached, false otherwise.
     */
    service.monitorLimitReached = function monitorLimitReached() {

        // Max open monitors allowed (add 1 for the primary monitor)
        const maxMonitors = maxSecondaryMonitors + 1;

        // Prevent opening of too many monitors
        return service.getMonitorCount() >= maxMonitors;

    };

    /**
     * Check if the browser supports the BroadcastChannel API.
     *
     * @returns {boolean}
     *     true if the BroadcastChannel API is supported, false otherwise.
     */
    service.supported = function supported() {

        if (!window.BroadcastChannel) {
            console.warn("BroadcastChannel is not supported by this browser.");
            return false;
        }

        return true;
    }

    /**
     * Set the current Guacamole Client
     * 
     * @param {Guacamole.Client} guac_client
     *     The guacamole client where to send instructions.
     */
    service.setClient = function setClient(guac_client) {

        client = guac_client;

        // Close all secondary monitors on client disconnect
        client.ondisconnect = service.closeAllMonitors;

    }

    /**
     * Push broadcast message containing instructions that allows additional
     * monitor windows to draw display, resize window and more.
     * 
     * @param {!string} type
     *     The type of message (ex: handler, fullscreen, resize)
     *
     * @param {*} content
     *     The content of the message, can contain any type of serializable
     *     content.
     */
    service.pushBroadcastMessage = function pushBroadcastMessage(type, content) {

        if (service.getMonitorCount() > 1) {

            // Format message content
            const message = {
                [type]: content
            };

            // Send message on the broadcast channel
            broadcast.postMessage(message);
        }

    };

    /**
     * Open or close additional monitor window.
     */
    service.addMonitor = function addMonitor() {

        // New monitor id
        lastMonitorId++;

        // New window parameters
        const windowUrl  = './#/secondaryMonitor/' + lastMonitorId;
        const windowId   = 'monitor' + lastMonitorId;
        const windowSize = 'width=' + $window.innerWidth + ',height=' + $window.innerHeight;

        // Open new window
        monitors[lastMonitorId] = $window.open(windowUrl, windowId, windowSize);

    };

    /**
     * Close an additional monitor based on its id.
     * 
     * @param {!number} monitorId
     *     The monitor ID to close.
     */
    service.closeMonitor = function closeMonitor(monitorId) {

        // Monitor not found or already closed
        if (!monitors[monitorId])
            return;

        // Close monitor
        if (!monitors[monitorId].closed)
            monitors[monitorId].close();

        // Delete monitor
        delete monitors[monitorId];

        // Send updated informations to secondary monitors and trigger the
        // resize event to notify guacd of deleted monitor.
        sendMonitorsInfos();

    }

    /**
     * Close all additional monitors.
     */
    service.closeAllMonitors = function closeAllMonitors() {

        // Loop on all existing monitors
        for (const key in monitors) {

            // Close monitor
            if (!monitors[key].closed)
                monitors[key].close();

            // Delete monitor
            delete monitors[key];

        }

        // Trigger the screen resize event to notify guacd that a monitor
        // has been removed.
        $window.dispatchEvent(new Event('monitor-count'));

    };

    /**
     * Get open monitors count. Force additional monitor to close if it's
     * window is closed.
     *
     * @returns {!number}
     *     Actual count of monitors.
     */
    service.getMonitorCount = function getMonitorCount() {

        // Loop on all existing monitors
        for (const key in monitors) {

            // Dead monitor, close it
            if (monitors[key].closed)
                service.closeMonitor(key);

        }

        // Return additionals monitors count + 1 for the main window
        return Object.keys(monitors).length + 1;

    };

    /**
     * Send monitor informations into the broadcast channel and notify guacd
     * that the monitor count has changed.
     */
    function sendMonitorsInfos() {

        const monitorsInfos = {
            count: service.getMonitorCount(),
            map: {},
        };

        // The main window would represent 0
        let monitorPosition = 1;

        // Generate monitors map (id => position)
        for (const monitorKey in monitors)
            monitorsInfos.map[monitorKey] = monitorPosition++;

        // Push informations to all monitors
        service.pushBroadcastMessage('monitorsInfos', monitorsInfos);

        // Trigger the screen resize event to notify guacd that a monitor
        // has been added or removed.
        $window.dispatchEvent(new Event('monitor-count'));

    };

    // Close additional monitors when window is unloaded
    $window.addEventListener('unload', service.closeAllMonitors);

    return service;

}]);
