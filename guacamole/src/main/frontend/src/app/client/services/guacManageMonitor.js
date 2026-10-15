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
     * Handle for the recurring window-position poll started by init(), retained
     * so it can be cleared before a re-init or on window unload. Without this,
     * each init() (e.g. on reconnect within the same SPA session, with no page
     * reload) would start an additional, never-cleared 1 Hz timer.
     *
     * @type {?Number}
     */
    let positionInterval = null;

    /**
     * Upper bounds on monitor geometry accepted from a guacd layout (F-10).
     * A finite-but-enormous width/height/area would otherwise be applied
     * verbatim to the display surface, exhausting browser memory. 32768 px per
     * side and ~268 Mpx of area comfortably covers real multi-monitor desktops
     * while rejecting abusive values; offsets are bounded to a large but finite
     * span.
     *
     * @type {!Number}
     */
    const MAX_MONITOR_DIMENSION = 32768;
    const MAX_MONITOR_AREA      = 32768 * 8192; // ~268 megapixels
    const MAX_MONITOR_OFFSET    = 131072;

    /**
     * Hard ceiling on the number of secondary monitors, applied on top of the
     * connection's `secondary-monitors` parameter so an out-of-range value
     * cannot request an unbounded number of windows (F-10).
     *
     * @type {!Number}
     */
    const MAX_SECONDARY_MONITORS = 8;

    /**
     * A per-connection identifier used to namespace the broadcast channel and
     * to tag/validate messages. All windows belonging to the SAME connection
     * share this id; windows of other connections do not. This isolates
     * multi-monitor display/input traffic to a single connection, preventing
     * cross-connection leakage or injection between concurrent connections in
     * the same browser origin.
     *
     * @type String
     */
    let channelId = null;

    /**
     * Whether a cryptographically secure random source is available. The
     * channel identifier isolates a connection's multi-monitor traffic from any
     * other same-origin sender, so it must be unguessable; multi-monitor is
     * disabled outright rather than falling back to a predictable id (F-05).
     *
     * @returns {boolean}
     *     true if crypto.randomUUID or crypto.getRandomValues is available.
     */
    function hasSecureRandom() {
        return !!(window.crypto
            && (window.crypto.randomUUID || window.crypto.getRandomValues));
    }

    /**
     * Generates a new, hard-to-guess per-connection channel identifier using a
     * cryptographically secure source. Returns null if no secure source is
     * available (in which case multi-monitor is unsupported - see
     * hasSecureRandom / supported); a predictable Math.random() id is never
     * used for this security-relevant value (F-05).
     *
     * @returns {?String}
     *     A unique channel identifier, or null if no secure RNG is available.
     */
    function generateChannelId() {
        if (window.crypto && window.crypto.randomUUID)
            return window.crypto.randomUUID();
        if (window.crypto && window.crypto.getRandomValues) {
            const bytes = new Uint8Array(16);
            window.crypto.getRandomValues(bytes);
            return 'c' + Array.from(bytes,
                    b => b.toString(16).padStart(2, '0')).join('');
        }
        return null;
    }

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

    /**
     * Map tracking consecutive missing counts per monitor ID.
     *
     * @type Object.<String, Number>
     */
    const missingCounts = {};


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
     *
     * @param {String} [id]
     *     The per-connection channel identifier. Supplied to secondary windows
     *     (via their opener URL) so they join the same channel as their
     *     primary. If omitted (primary window), a new one is generated.
     */
    service.init = function init(type, id) {

        // Change the monitor type
        if (type) monitorType = type;

        // Establish the per-connection channel id. Secondaries inherit their
        // primary's id (passed via the opener URL); a primary reuses its id
        // across re-init so already-open secondary windows stay linked, and
        // otherwise mints a fresh id. Isolation comes from the unguessable,
        // cryptographically-random id combined with the same-origin nature of
        // BroadcastChannel - not from rotating the id (F-03/F-05).
        channelId = id || channelId || generateChannelId();

        if (monitorType == "primary") {
            guacFullscreen.onfullscreen = function onfullscreen(state) {
                service.pushBroadcastMessage('fullscreen', state);
            }
        }

        // Create broadcast if supported (requires BroadcastChannel and a secure
        // RNG for the channel id)
        if (!service.supported())
            return;

        // Close any channel opened by a previous init() so its message handler
        // does not outlive this one (F-03 residual).
        if (broadcast)
            broadcast.close();

        // Namespace the channel per connection so data is never mixed between
        // multiple concurrent connections in the same browser origin.
        broadcast = new BroadcastChannel('guac_monitors_' + channelId);

        /**
         * Handle messages sent by other windows of THIS connection on the
         * broadcast channel. Messages that do not carry this connection's
         * channel id are ignored (defense-in-depth against any same-origin
         * sender on the channel).
         *
         * @param {MessageEvent} e
         *     Received message event from the broadcast channel.
         */
        broadcast.onmessage = function onmessage(e) {
            if (!e || !e.data || e.data._cid !== channelId)
                return;
            messageHandlers[monitorType](e);
        };

        // Check the window position every second and send a resize event if it
        // has changed. Clear any prior interval first so repeated init() calls
        // (e.g. reconnect) do not accumulate timers.
        if (positionInterval)
            clearInterval(positionInterval);
        positionInterval = setInterval(() => updatePosition(), 1000);

    };

    /**
     * Set the maximum number of secondary monitors allowed.
     *
     * @param {Number} secondaryMonitorsAllowed
     *     The maximum number of secondary monitors allowed.
     */
    service.setMaxSecondaryMonitors = function setMaxSecondaryMonitors(amount) {
        // Clamp to a sane range (F-10): a non-numeric (e.g. NaN from parseInt)
        // or out-of-range `secondary-monitors` value must not be trusted to
        // request an unbounded number of monitor windows.
        const requested = Number.isFinite(amount) ? amount : 0;
        maxSecondaryMonitors = Math.max(0, Math.min(requested, MAX_SECONDARY_MONITORS));
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
     * Check if multi-monitor is supported: the browser must provide the
     * BroadcastChannel API and a cryptographically secure RNG for the channel
     * id (F-05). Without a secure RNG, multi-monitor is disabled rather than
     * isolating connections with a guessable channel name.
     *
     * @returns {boolean}
     *     true if multi-monitor is supported, false otherwise.
     */
    service.supported = function supported() {

        if (!window.BroadcastChannel) {
            console.warn("BroadcastChannel is not supported by this browser.");
            return false;
        }

        if (!hasSecureRandom()) {
            console.warn("Multi-monitor disabled: no secure random source "
                    + "available for channel isolation.");
            return false;
        }

        return true;
    }

    // Payload validators for broadcast messages (F-05). Every side effect that
    // forwards data to guacd (input/resize) or invokes client.runHandler is
    // gated on a shape/range check in addition to the _cid isolation tag, so a
    // malformed or hostile same-origin payload cannot reach those sinks with
    // unexpected types or values.

    const isFiniteNum   = v => typeof v === 'number' && Number.isFinite(v);
    const isPlainObject = v => !!v && typeof v === 'object' && !Array.isArray(v);

    // A monitor id is a small non-negative integer (also accepted as a numeric
    // string, since ids are used as object keys elsewhere).
    const isValidMonitorId = v =>
        (isFiniteNum(v) && v >= 0 && Number.isInteger(v))
        || (typeof v === 'string' && /^\d{1,9}$/.test(v));

    // A size payload must carry finite, non-negative dimensions and a valid
    // monitor id; offsets, when present, must be finite.
    const isValidSize = s => isPlainObject(s)
        && isFiniteNum(s.width)  && s.width  >= 0
        && isFiniteNum(s.height) && s.height >= 0
        && isValidMonitorId(s.monitorId)
        && (s.top  === undefined || isFiniteNum(s.top))
        && (s.left === undefined || isFiniteNum(s.left));

    // A mouse state must at least carry finite coordinates.
    const isValidMouseState = m => isPlainObject(m)
        && isFiniteNum(m.x) && isFiniteNum(m.y);

    // A keysym is a finite non-negative integer.
    const isValidKeysym = k => isFiniteNum(k) && k >= 0 && Number.isInteger(k);

    // A display handler must name a short string opcode and carry an array of
    // parameters. (A full per-opcode allowlist is a further hardening step; the
    // shape check already prevents non-string opcodes / non-array params from
    // reaching runHandler.)
    const isValidHandler = h => isPlainObject(h)
        && typeof h.opcode === 'string' && h.opcode.length > 0 && h.opcode.length <= 32
        && Array.isArray(h.parameters);

    /**
     * Handlers for instructions received on broadcast channel.
     */
    const messageHandlers = {

        "primary": function primary(message) {

            const data = message.data;
            if (!isPlainObject(data))
                return;

            // Send size event to guacd
            if (data.size && isValidSize(data.size))
                service.sendSize(client, data.size);

            // Mouse state changed on secondary screen
            if (data.mouseState && isValidMouseState(data.mouseState))
                client.sendMouseState(data.mouseState);

            // Key down on secondary screen
            if (data.keydown !== undefined && isValidKeysym(data.keydown))
                client.sendKeyEvent(1, data.keydown);

            // Key up on secondary screen
            if (data.keyup !== undefined && isValidKeysym(data.keyup))
                client.sendKeyEvent(0, data.keyup);

            // Additional window unloaded
            if (data.monitorClose !== undefined && isValidMonitorId(data.monitorClose))
                service.closeMonitor(data.monitorClose);

            // CTRL+ALT+SHIFT pressed on secondary window
            if (data.guacMenu && service.menuShown)
                service.menuShown();

        },

        "secondary": function secondaryMonitor(message) {

            const data = message.data;
            if (!isPlainObject(data))
                return;

            // Run the client handler to draw display
            if (data.handler && isValidHandler(data.handler))
                client.runHandler(data.handler.opcode, data.handler.parameters);

            if (data.monitorsInfos && isPlainObject(data.monitorsInfos))
                monitorsInfos = data.monitorsInfos;

            // Full screen mode instructions (must be an explicit boolean)
            if (typeof data.fullscreen === 'boolean') {

                // setFullscreenMode require explicit user action
                if (data.fullscreen) {
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

        // Format message content, tagging it with this connection's channel id
        // so receivers can reject any foreign traffic.
        const message = {
            [type]: content,
            _cid: channelId
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

        // New window parameters. The channel id is carried in the URL so the
        // secondary window joins this connection's channel (and no other).
        const windowUrl  = './#/secondaryMonitor/' + lastMonitorId + '/' + channelId;
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

        // Clear missing count tracking
        delete missingCounts[monitorId];

        // Close monitor
        if (!monitors[monitorId].closed)
            monitors[monitorId].close();

        // Delete monitor
        delete monitors[monitorId];

        // Notify guacd that a monitor has been closed
        service.sendSize(client, {
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
     * @param {Guacamole.Client} requestedClient
     *     The Guacamole client to send the size to. This is needed for
     *     connection groups with multiple clients.
     * @param {Object} size
     *     The size object containing width, height, top and monitorId.
     */
    service.sendSize = function sendSize(requestedClient, size) {

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
            requestedClient.sendSize(0, 0, monitorPosition, 0);

        // Send new size to guacd
        else
            sendAllSizes(requestedClient);

        // Push informations to all monitors
        service.pushBroadcastMessage('monitorsInfos', monitorsInfos);

    }

    /**
     * Get the X offset of the current monitor. The monitor displayed on the
     * leftmost position (lowest left value) will have an offset of 0 and for
     * other monitors, the offset is the left value of the monitor minus the left
     * value of the leftmost monitor (lowest left offset).
     * This is used to calculate the X offset to draw operations and mouse
     * events.
     *
     * @return {number}
     *     The X offset of the current monitor, in pixels.
     */
    service.getOffsetX = function getOffsetX() {
        const currentOffset = monitorsInfos.rendered[service.monitorId]?.left ?? 0;
        return currentOffset - getLowestLeftOffset();
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
     *
     * @param {Guacamole.Client} requestedClient
     *     The Guacamole client to send the sizes to.
     */
    function sendAllSizes(requestedClient) {
        // Loop through all monitors and send their sizes to guacd
        for (const [id, details] of Object.entries(monitorsInfos.details)) {
            requestedClient.sendSize(
                details.width,
                details.height,
                monitorsInfos.map[id],
                getTopOffset(id, details.top),
                getLeftOffset(id, details.left)
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
     * Get the left offset of the given monitor id and left value based on the
     * primary monitor's left value. The left offset is the difference between
     * the left value of the monitor and the left value of the primary monitor.
     * This is used to calculate the X offset to send to guacd.
     *
     * Note the sign is preserved: a monitor placed to the left of the primary
     * has a negative offset, which the server accepts.
     *
     * @param {number} id
     *     The id of the monitor.
     * @param {number} left
     *     The left value of the monitor.
     *
     * @return {number}
     *     The left offset of the monitor, in pixels.
     */
    function getLeftOffset(id, left) {

        const primaryMonitorId = 0;

        // If this is the primary monitor, return 0
        if (id === primaryMonitorId)
            return 0;

        return left - (monitorsInfos.details[primaryMonitorId]?.left ?? 0);
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
     * Get the lowest left value of all monitors. This is used to calculate the
     * X offset of the current monitor.
     *
     * @return {number}
     *     The lowest left value of all monitors, in pixels.
     */
    function getLowestLeftOffset() {
        let lowestLeftValue = monitorsInfos.rendered[0]?.left ?? 0;

        // Loop through all monitors to find the leftmost monitor
        for (const [_, rendered] of Object.entries(monitorsInfos.rendered)) {
            if (rendered?.left < lowestLeftValue) {
                lowestLeftValue = rendered.left;
            }
        }

        return lowestLeftValue;
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
            service.sendSize(client, monitorDetails);
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

        // Guard the offset/size math against a malformed or compromised layout
        // from guacd: every geometry field must be a finite number, otherwise
        // the entry would poison setMonitorSize() and the client offsets with
        // NaN. Dimensions must additionally be positive (a zero/negative
        // width or height yields a degenerate surface) and bounded above (an
        // enormous width/height/area would allocate a huge display surface -
        // F-10). Offsets (top/left) may legitimately be negative for monitors
        // placed above/left of primary, but are also bounded. An entry that
        // fails validation is treated as an absent monitor.
        const isFiniteNumber = value =>
            typeof value === 'number' && Number.isFinite(value);
        const inRange = (value, limit) =>
            isFiniteNumber(value) && Math.abs(value) <= limit;
        const isValidGeometry = geom => !!geom
            && isFiniteNumber(geom.width) && geom.width > 0 && geom.width <= MAX_MONITOR_DIMENSION
            && isFiniteNumber(geom.height) && geom.height > 0 && geom.height <= MAX_MONITOR_DIMENSION
            && (geom.width * geom.height) <= MAX_MONITOR_AREA
            && inRange(geom.top, MAX_MONITOR_OFFSET) && inRange(geom.left, MAX_MONITOR_OFFSET);

        for (const [id, pos] of Object.entries(monitorsInfos.map)) {

            // Track absence of the monitor. Only close it if it has been missing
            // for 8 consecutive layout updates. A malformed geometry entry is
            // treated exactly like an absent monitor.
            if (!isValidGeometry(layout[pos])) {
                missingCounts[id] = (missingCounts[id] || 0) + 1;
                if (missingCounts[id] >= 8) {
                    service.closeMonitor(id);
                    delete missingCounts[id];
                }
                continue;
            }

            // Reset the missing count when the monitor is present in the layout
            missingCounts[id] = 0;

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

        // Stop the position poll, close the broadcast channel, and close
        // additional monitors when the window is unloaded, so neither the
        // interval nor the channel handler outlives its window (F-03).
    function onWindowUnload() {
        if (positionInterval) {
            clearInterval(positionInterval);
            positionInterval = null;
        }
        if (broadcast) {
            broadcast.close();
            broadcast = null;
        }
        service.closeAllMonitors();
    }
    $window.addEventListener('unload', onWindowUnload);

    return service;

}]);
