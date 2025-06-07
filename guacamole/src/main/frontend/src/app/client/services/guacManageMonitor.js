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

    /**
     * Additionals monitors windows opened.
     * 
     * @type Object.<Number, Window>
     */
    const monitors = {};

    /**
     * The type of this monitor (default = primary).
     * 
     * @type String
     */
    let monitorType = "primary";

    /**
     * The current Guacamole client instance.
     * 
     * @type Guacamole.Client 
     */
    let client = null;

    /**
     * The display of the current Guacamole client instance.
     * 
     * @type Guacamole.Display
     */
    let display = null;

    /**
     * The broadcast channel used for communications between all windows.
     * 
     * @type BroadcastChannel
     */
    let broadcast = null;

    /**
     * The maximum number of secondary monitors allowed.
     *
     * @type Number
     */
    let maxSecondaryMonitors = 0;

    /**
     * Store the last additional monitor id.
     * 
     * @type Number
     */
    let lastMonitorId = 0;

    /**
     * Object containing monitors informations.
     *
     * @type Object
     * @property {Number} count
     *     The number of monitors, including the main window.
     * @property {Object.<Number, Number>} map
     *     A map of monitor id to position.
     * @property {Object.<Number, Object>} details
     *     Details of each browser window, including width, height, etc.
     * @property {Object.<Number, Object>} rendered
     *     Details of each rendered monitor, including width, height, etc.
     *     This is used to display what is expected by guacd.
     */
    let monitorsInfos = {
        count: 1,
        map: {},
        details: {},
        rendered: {},
    };

    const service = {};

    /**
     * Attributes of the monitor
     * 
     * @type Object.<Number>
     */
    service.monitorId = 0;

    /**
     * Init the monitor type and broadcast channel used for bidirectionnal
     * communications between primary and secondary monitor windows.
     * 
     * @param {String} type
     *     The type of the monitor. "primary" if not given.
     */
    service.init = function init(type) {

        // Change the monitor type
        if (type) monitorType = type;

        if (monitorType == "primary") {
            guacFullscreen.onfullscreen = function onfullscreen(state) {
                service.pushBroadcastMessage('fullscreen', state);
            }
        }

        // Create broadcast if supported
        if (!service.supported())
            return;

        // TODO: ensure that there is no mixing of data between multiple connections
        broadcast = new BroadcastChannel('guac_monitors');

        /**
         * Handle messages sent by secondary monitors windows in
         * guac_monitors channel.
         * 
         * @param {Event} e
         *     Received message event from guac_monitors channel.
         */
        broadcast.onmessage = messageHandlers[monitorType];

        // Check the window position every second and send a resize event if it
        // has changed
        setInterval(() => updatePosition(), 1000);

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
     * Handlers for instructions received on broadcast channel.
     */
    const messageHandlers = {

        "primary": function primary(message) {

            // Send size event to guacd
            if (message.data.size)
                service.sendSize(message.data.size);

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
            if (message.data.guacMenu && service.menuShown)
                service.menuShown();

        },

        "secondary": function secondaryMonitor(message) {
            
            // Run the client handler to draw display
            if (message.data.handler)
                client.runHandler(message.data.handler.opcode,
                                  message.data.handler.parameters);

            if (message.data.monitorsInfos)
                monitorsInfos = message.data.monitorsInfos;

            // Full screen mode instructions
            if (message.data.fullscreen !== undefined) {

                // setFullscreenMode require explicit user action
                if (message.data.fullscreen) {
                    if (service.openConsentButton) service.openConsentButton();
                }

                // Close fullscreen mode instantly
                else
                    guacFullscreen.setFullscreenMode(false);

            }
        }

    }

    /**
     * Add button to request user consent before enabling fullscreen mode to
     * comply with the setFullscreenMode requirements that require explicit
     * user action. The button is removed after a few seconds if the user does
     * not click on it.
     */
    service.openConsentButton = null;

    /**
     * Open or close Guacamole menu (ctrl+alt+shift).
     */
    service.menuShown = null;

    /**
     * Set the current Guacamole Client
     * 
     * @param {Guacamole.Client} guac_client
     *     The guacamole client where to send instructions.
     */
    service.setClient = function setClient(guac_client) {

        client  = guac_client;
        display = client.getDisplay();

        client.onmultimonlayout = onmultimonlayout;

        // Close all secondary monitors on client disconnect
        if (monitorType === "primary")
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

        // Send only if there are other monitors to receive this message
        if (monitorType === "primary" && service.getMonitorCount() <= 1)
            return;

        // Format message content
        const message = {
            [type]: content
        };

        // Send message on the broadcast channel
        broadcast.postMessage(message);

    };

    /**
     * Open an additional monitor window.
     */
    service.addMonitor = function addMonitor() {

        // New monitor id
        lastMonitorId++;

        // New window parameters
        const windowUrl  = './#/secondaryMonitor/' + lastMonitorId;
        const windowId   = 'monitor' + lastMonitorId;
        const windowSize = 'width=800,height=600';

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

        // Notify guacd that a monitor has been closed
        service.sendSize({
            width: 0,
            height: 0,
            top: 0,
            monitorId: monitorId,
        });

    }

    /**
     * Close all additional monitors.
     */
    service.closeAllMonitors = function closeAllMonitors() {

        // Loop on all existing monitors
        for (const key in monitors)
            service.closeMonitor(key);

    };

    /**
     * Get open monitors count.
     *
     * @returns {!number}
     *     Actual count of monitors.
     */
    service.getMonitorCount = function getMonitorCount() {
        // Return additionals monitors count + 1 for the main window
        return Object.keys(monitors).length + 1;
    };

    /**
     * Send size event to guacd and update monitorsInfos object.
     *
     * @param {Object} size
     *     The size object containing width, height, top and monitorId.
     */
    service.sendSize = function sendSize(size) {

        const monitorPosition = monitorsInfos.map[size.monitorId];

        updateMonitorsInfos({
            id:     size.monitorId,
            width:  size.width,
            height: size.height,
            left:   size.left,
            top:    size.top,
        });

        // Monitor has been closed
        if (size.width === 0 || size.height === 0)
            client.sendSize(0, 0, monitorPosition, 0);

        // Send new size to guacd
        else
            sendAllSizes();

        // Push informations to all monitors
        service.pushBroadcastMessage('monitorsInfos', monitorsInfos);

    }

    /**
     * Get the X offset of the current monitor. The X offset is the
     * total width of all previous monitors.
     *
     * @return {number}
     *     The X offset of the current monitor, in pixels.
     */
    service.getOffsetX = function getOffsetX() {
        const monitorId = service.monitorId;

        if (monitorId === 0)
            return 0;
    
        const thisPosition = monitorsInfos.map[monitorId];
        let offsetX = 0;
    
        // Loop through all monitors to add their widths as offset if they
        // are before the current monitor
        for (const [id, pos] of Object.entries(monitorsInfos.map)) {
            if (pos < thisPosition) {
                const rendered = monitorsInfos.rendered[id];
                if (rendered?.width) offsetX += rendered.width;
            }
        }

        return offsetX;
    }

    /**
     * Get the Y offset of the current monitor. The monitor displayed on the
     * highest position (lowest top value) will have an offset of 0 and for
     * other monitors, the offset is the top value of the monitor minus the top
     * value of the highest monitor (lowest top offset).
     * This is used to calculate the Y offset to draw operations and mouse
     * events.
     *
     * @return {number}
     *     The Y offset of the current monitor, in pixels.
     */
    service.getOffsetY = function getOffsetY() {
        const currentOffset = monitorsInfos.rendered[service.monitorId]?.top ?? 0;
        return currentOffset - getLowestTopOffset();
    }

    /**
     * Send the size of all monitors to guacd. This is used to update the
     * monitor sizes in guacd when a new monitor is added or updated.
     *
     * This function loops through all monitors and sends their sizes to guacd
     * using the client.sendSize method. The size includes width, height,
     * monitor position and top offset.
     */
    function sendAllSizes() {
        // Loop through all monitors and send their sizes to guacd
        for (const [id, details] of Object.entries(monitorsInfos.details)) {
            client.sendSize(
                details.width,
                details.height,
                monitorsInfos.map[id],
                getTopOffset(id, details.top)
            );
        }
    }

    /**
     * Get the top offset of the given monitor id and top value based on the
     * primary monitor's top value. The top offset is the difference between the
     * top value of the monitor and the top value of the primary monitor.
     * This is used to calculate the Y offset to send to guacd.
     *
     * @param {number} id
     *     The id of the monitor.
     * @param {number} top
     *     The top value of the monitor.
     *
     * @return {number}
     *     The top offset of the monitor, in pixels.
     */
    function getTopOffset(id, top) {

        const primaryMonitorId = 0;

        // If this is the primary monitor, return 0
        if (id === primaryMonitorId)
            return 0;

        return top - Math.abs(monitorsInfos.details[primaryMonitorId].top ?? 0);
    }

    /**
     * Get the lowest top value of all monitors. This is used to calculate the
     * Y offset of the current monitor.
     *
     * @return {number}
     *     The lowest top value of all monitors, in pixels.
     */
    function getLowestTopOffset() {
        let lowestTopValue = monitorsInfos.rendered[0]?.top ?? 0;

        // Loop through all monitors to find the highest monitor
        for (const [_, rendered] of Object.entries(monitorsInfos.rendered)) {
            if (rendered?.top < lowestTopValue) {
                lowestTopValue = rendered.top;
            }
        }

        return lowestTopValue;
    }

    /**
     * Update monitorsInfos object with current monitors count and map.
     *
     * @param {Object} monitorDetails
     *     Optional monitor details to update the monitorsInfos object.
     */
    function updateMonitorsInfos(monitorDetails) {

        monitorsInfos.count = service.getMonitorCount();

        // The main window would represent 0
        let monitorPosition = 1;

        // Generate monitors map (id => position), main window is always at
        // position 0
        monitorsInfos.map[0] = 0;
        for (const monitorKey in monitors) {
            monitorsInfos.map[monitorKey] = monitorPosition++;
        }

        // Set monitor details if provided
        if (!monitorDetails)
            return;

        // If width or height is 0, remove monitor details
        if (monitorDetails.width === 0 || monitorDetails.height === 0) {
            delete monitorsInfos.details[monitorDetails.id];
            delete monitorsInfos.rendered[monitorDetails.id];
            delete monitorsInfos.map[monitorDetails.id];
        }
        // Update or add monitor details
        else {
            const monitorId = monitorDetails.id;
            monitorsInfos.details[monitorId] = {
                width:  monitorDetails.width,
                height: monitorDetails.height,
                top:    monitorDetails.top,
                // TODO: Use the left value to reorder monitors if needed
                left:   monitorDetails.left,
            };
        }        

    };

    /**
     * Check if the window position has changed since the last check.
     * This is used to avoid unnecessary updates.
     *
     * @returns {boolean}
     *     True if the position has changed, false otherwise.
     */
    function positionHasChanged() {
        const monitorDetails = monitorsInfos.details[service.monitorId];

        // Monitor not initialized
        if (!monitorDetails)
            return false;

        return monitorDetails.left !== window.screenX
            || monitorDetails.top  !== window.screenY;
    }

    /**
     * Trigger a resize event if the window position has changed.
     */
    function updatePosition() {
        if (!positionHasChanged() || !client)
            return;

        const monitorDetails = monitorsInfos.details[service.monitorId];

        // Update the position of the monitor
        monitorDetails.left      = window.screenX ?? 0;
        monitorDetails.top       = window.screenY ?? 0;
        monitorDetails.monitorId = service.monitorId;

        // Send size event to guacd and update monitorsInfos if this is the
        // primary monitor
        if (monitorType === "primary") {
            service.sendSize(monitorDetails);
            return;
        }

        // Send broadcast message to primary monitor if this is a secondary
        // monitor
        service.pushBroadcastMessage('size', monitorDetails);
    }

    /**
     * Handle the multimonitor layout event. This is used to update the
     * monitorsInfos object when the layout changes.
     *
     * @param {Object} layout
     *     An object describing the layout of monitors.
     */
    function onmultimonlayout(layout) {
        if (!layout)
            return;

        for (const [id, pos] of Object.entries(monitorsInfos.map)) {

            // If the monitor is not in the layout, it is not known by
            // guacd anymore, so we close it
            if (!layout[pos]) {
                service.closeMonitor(id);
                continue;
            }

            if (!monitorsInfos.rendered[id])
                monitorsInfos.rendered[id] = {};
                
            // Update the monitor details
            monitorsInfos.rendered[id].width  = layout[pos].width;
            monitorsInfos.rendered[id].height = layout[pos].height;
            monitorsInfos.rendered[id].top    = layout[pos].top;
            monitorsInfos.rendered[id].left   = layout[pos].left;

            // Set the monitor size in the display only if the id matches the
            // current monitor id
            if (id === String(service.monitorId)) {
                display.setMonitorSize(
                    monitorsInfos.rendered[id].width,
                    monitorsInfos.rendered[id].height,
                );
            }
    
        }

        // Update the offset of the client when monitorInfos is fully updated
        // This is needed to ensure that the client knows the correct offset
        // of each monitor
        client.offsetX = service.getOffsetX();
        client.offsetY = service.getOffsetY();

    }

    // Close additional monitors when window is unloaded
    $window.addEventListener('unload', service.closeAllMonitors);

    return service;

}]);
