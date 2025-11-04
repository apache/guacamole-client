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

/* global Guacamole */

/**
 * RDPECAM (Remote Desktop Protocol Enhanced Camera) service for Guacamole.
 * This service provides camera redirection functionality for RDP connections.
 */
angular.module('client').factory('guacRDPECAM', ['$injector', function guacRDPECAM($injector) {

    // Required services

    /**
     * The mimetype of video data to be sent along the Guacamole connection if
     * camera redirection is supported.
     *
     * @constant
     * @type String
     */
    const RDPECAM_MIMETYPE = 'application/rdpecam+h264';

    /**
     * Default camera constraints for RDPECAM.
     * Using 640x480@15fps for Windows compatibility with H.264 Level 3.0
     *
     * @constant
     * @type Object
     */
    const DEFAULT_CONSTRAINTS = { width: 640, height: 480, frameRate: 15 };

    const CAPABILITY_CANDIDATES = [
        { width: 640,  height: 480,  fps: [30, 15] },
        { width: 320,  height: 240,  fps: [30, 15] },
        { width: 1280, height: 720,  fps: [30]      },
        { width: 1920, height: 1080, fps: [30]      }
    ];

    const probedClients = new WeakSet();

    // Always use a local, stable ID for each Guacamole.Client
    const clientIds = new WeakMap();
    function getLocalClientId(client) {
        if (!client)
            return null;
        var id = clientIds.get(client);
        if (id)
            return id;
        id = 'local-' + Date.now().toString(36) + '-' + Math.floor(Math.random() * 1e6).toString(36);
        clientIds.set(client, id);
        return id;
    }

    function capabilitySupportsValue(capability, value) {
        if (!capability)
            return true;

        if (Array.isArray(capability))
            return capability.indexOf(value) !== -1;

        if (typeof capability === 'object') {
            if (typeof capability.max === 'number' && value > capability.max)
                return false;
            if (typeof capability.min === 'number' && value < capability.min)
                return false;
            if (typeof capability.step === 'number' && typeof capability.min === 'number') {
                const step = capability.step;
                if (step > 0 && ((value - capability.min) % step) !== 0)
                    return false;
            }
        }

        return true;
    }

    function deriveFormatsFromCapabilities(capabilities) {
        const formats = [];
        const seen = {};

        const pushFormat = function(width, height, fpsNum, fpsDen) {
            const key = width + 'x' + height + '@' + fpsNum + '/' + fpsDen;
            if (seen[key])
                return;
            seen[key] = true;
            formats.push({
                width: width,
                height: height,
                fpsNumerator: fpsNum,
                fpsDenominator: fpsDen
            });
        };

        CAPABILITY_CANDIDATES.forEach(function(candidate) {
            if (!capabilitySupportsValue(capabilities.width, candidate.width))
                return;
            if (!capabilitySupportsValue(capabilities.height, candidate.height))
                return;

            candidate.fps.forEach(function(fps) {
                if (!capabilitySupportsValue(capabilities.frameRate, fps))
                    return;
                pushFormat(candidate.width, candidate.height, fps, 1);
            });
        });

        if (!formats.length) {
            pushFormat(DEFAULT_CONSTRAINTS.width, DEFAULT_CONSTRAINTS.height,
                DEFAULT_CONSTRAINTS.frameRate || 15, 1);
        }

        return formats;
    }

    /**
     * Sends a capability descriptor payload upstream to guacd via an "argv"
     * stream. Always uses multi-device format: "DEVICE_ID:DEVICE_NAME|FORMATS;..."
     *
     * @param {!Guacamole.Client} client
     *     The client instance handling the RDPECAM stream.
     *
     * @param {!Array.<{deviceId:string,deviceName:string,formats:Array}>} devices
     *     Array of device objects with deviceId, deviceName, and formats.
     *     Each device must have a non-empty deviceId.
     */
    function sendCapabilities(client, devices) {

        if (!client || !devices || !Array.isArray(devices) || devices.length === 0)
            return;

        try {
            var deviceEntries = devices.map(function(device) {
                if (!device || !device.formats || !device.formats.length)
                    return null;

                // Require device ID - skip devices without it
                var deviceId = (device.deviceId && device.deviceId.trim()) ? device.deviceId.trim() : '';
                if (!deviceId) {
                    return null;
                }

                var entries = device.formats.map(function(format) {
                    var width = Math.round(format.width);
                    var height = Math.round(format.height);
                    var fpsNum = Math.round(format.fpsNumerator || format.frameRate || 0);
                    var fpsDen = Math.round(format.fpsDenominator || 1);

                    if (fpsNum <= 0)
                        fpsNum = 1;
                    if (fpsDen <= 0)
                        fpsDen = 1;

                    return width + 'x' + height + '@' + fpsNum + '/' + fpsDen;
                }).join(',');

                // Format: "DEVICE_ID:DEVICE_NAME|FORMATS"
                var name = (device.deviceName && device.deviceName.trim()) ? device.deviceName.trim() : '';
                return deviceId + ':' + name + '|' + entries;
            }).filter(function(entry) { return entry !== null; });

            if (deviceEntries.length === 0) {
                return;
            }

            var payload = deviceEntries.join(';');

            var stream = client.createArgumentValueStream('text/plain', 'rdpecam-capabilities');
            var writer = new Guacamole.StringWriter(stream);
            writer.sendText(payload);
            writer.sendEnd();
        }
        catch (e) {
            // Unable to advertise camera capabilities - ignore
        }

    }

    function prefetchCapabilities(client) {
        if (typeof navigator === 'undefined' || !navigator.mediaDevices)
            return;

        if (!client || probedClients.has(client))
            return;

        probedClients.add(client);

        // Enumerate all video devices
        if (typeof navigator.mediaDevices.enumerateDevices === 'function') {
            navigator.mediaDevices.enumerateDevices({ video: true })
                .then(function(devices) {
                    var videoDevices = devices.filter(function(device) {
                        return device.kind === 'videoinput';
                    });

                    if (videoDevices.length === 0) {
                        return;
                    }

                    // Probe capabilities for each device
                    var devicePromises = videoDevices.map(function(deviceInfo) {
                        return probeDeviceCapabilities(deviceInfo.deviceId, deviceInfo.label || '');
                    });

                    Promise.all(devicePromises).then(function(deviceCapabilities) {
                        // Filter out devices with no capabilities or no device ID
                        var validDevices = deviceCapabilities.filter(function(dev) {
                            return dev && dev.deviceId && dev.deviceId.trim() && dev.formats && dev.formats.length > 0;
                        });

                        if (validDevices.length > 0) {
                            sendCapabilities(client, validDevices);
                        }
                    }).catch(function(error) {
                        // Error probing device capabilities - capabilities not sent
                    });
                })
                .catch(function(error) {
                    // Error enumerating devices - capabilities not sent
                });
        }
    }

    /**
     * Probes capabilities for a single device by device ID.
     *
     * @param {string} deviceId
     *     The device ID to probe.
     *
     * @param {string} deviceName
     *     The device label/name.
     *
     * @returns {Promise.<{deviceId:string,deviceName:string,formats:Array}>}
     *     Promise resolving to device capabilities object.
     */
    function probeDeviceCapabilities(deviceId, deviceName) {
        return new Promise(function(resolve, reject) {
            var constraints = { video: { deviceId: { exact: deviceId } } };

            navigator.mediaDevices.getUserMedia(constraints).then(function(stream) {
                try {
                    var track = stream.getVideoTracks()[0];
                    var formats = [];

                    if (track && typeof track.getCapabilities === 'function') {
                        var caps = track.getCapabilities();
                        formats = deriveFormatsFromCapabilities(caps || {});
                    } else {
                        formats = deriveFormatsFromCapabilities({});
                    }

                    // Get actual device name from track if available
                    var actualName = (track && track.label) ? track.label : deviceName;

                    resolve({
                        deviceId: deviceId,
                        deviceName: actualName,
                        formats: formats
                    });
                }
                catch (e) {
                    reject(e);
                }
                finally {
                    if (stream) {
                        try {
                            stream.getTracks().forEach(function(track) { track.stop(); });
                        }
                        catch (ignore) {}
                    }
                }
            }).catch(function(error) {
                reject(error);
            });
        });
    }


    /**
     * Starts camera redirection for the given client.
     *
     * @param {Guacamole.Client} client
     *     The Guacamole client for which camera redirection should be started.
     *
     * @param {Object} [constraints]
     *     Optional camera constraints. If not provided, default constraints
     *     will be used.
     *
     * @param {Function} [onState]
     *     Optional callback function to be invoked when the camera state changes.
     *     The callback will receive an object with the current state.
     *
     * @returns {Promise<Object>}
     *     A promise that resolves with an object containing a stop() method
     *     to stop camera redirection.
     */
    function startCamera(client, constraints, onState) {

        // Use default constraints if none provided
        constraints = constraints || DEFAULT_CONSTRAINTS;

        return new Promise((resolve, reject) => {

            try {
                // Ensure single recorder per client: stop any existing one first
                var existingClientId = getLocalClientId(client);
                if (existingClientId && (cameraControllers[existingClientId] || cameraRecorders[existingClientId])) {
                    try {
                        stopCamera(client);
                    } catch (ignore) {}
                }

                // Get client ID for tracking
                var clientId = getLocalClientId(client);

                // Create video stream for camera data
                const realStream = client.createVideoStream(RDPECAM_MIMETYPE);

                // Wrap stream with delay queue (handles both delayed and immediate modes)
                const stream = createDelayedStream(realStream, clientId);

                // Create camera recorder (will use delayed stream)
                const recorder = Guacamole.CameraRecorder.getInstance(stream, RDPECAM_MIMETYPE);
                
                if (!recorder) {
                    stream.sendEnd();
                    reject(new Error('Camera recording not supported'));
                    return;
                }

                // Capabilities are sent via prefetchCapabilities, which enumerates all devices.
                // This callback may still fire, but we rely on prefetchCapabilities for the
                // multi-device format.
                if (recorder) {
                    recorder.oncapabilities = function(formats, deviceName) {
                        // Capabilities should have been sent via prefetchCapabilities already.
                        // If this fires, it means a camera track was detected, but we rely
                        // on the enumeration-based approach for multi-device support.
                    };
                }
                
                // Set format constraints before starting (deviceId-aware)
                if (recorder && typeof recorder.setFormat === 'function') {
                    recorder.setFormat(constraints);
                }
                
                // Explicitly start camera capture now that recorder is configured.
                if (recorder && typeof recorder.start === 'function') {
                    recorder.start();
                    if (typeof recorder.resetTimeline === 'function')
                        recorder.resetTimeline();
                }

                // No pause/resume handling; throttling is credit-based on server.

                // Set up recorder event handlers
                recorder.onclose = function() {
                    // Update state to inactive
                    var clientId = getLocalClientId(client);
                    if (clientId) {
                        updateCameraState(clientId, false);
                    }
                    
                    if (onState) {
                        onState({ running: false });
                    }
                };

                recorder.onerror = function() {
                    // Update state to inactive on error
                    var clientId = getLocalClientId(client);
                    if (clientId) {
                        updateCameraState(clientId, false);
                    }
                    
                    if (onState) {
                        onState({ running: false, error: true });
                    }
                };

                // Do not override stream.onack — the underlying writer uses
                // its own ACK handler to control start/back-pressure.

                // Create stop function
                var stopFunction = function() {
                    recorder.stop();
                    stream.sendEnd();
                    
                    // Clean up recorder reference (state update handled by caller)
                    if (clientId && cameraRecorders[clientId]) {
                        delete cameraRecorders[clientId];
                    }
                    
                    if (onState) {
                        onState({ running: false });
                    }
                };

                // Update state to active and register controller IMMEDIATELY
                var clientId = getLocalClientId(client);
                if (clientId) {
                    cameraRecorders[clientId] = recorder;
                    cameraControllers[clientId] = stopFunction;  // Register BEFORE resolving promise
                    updateCameraState(clientId, true);
                }

                // Notify state change
                if (onState) {
                    onState({ 
                        running: true, 
                        width: constraints.width, 
                        height: constraints.height 
                    });
                }

                // Return control object
                resolve({
                    stop: stopFunction
                });

            } catch (error) {
                reject(error);
            }

        });

    }

    /**
     * Starts camera redirection with parameters received from server.
     *
     * When Windows sends Start Streams Request, server sends argv instructions
     * with camera configuration parameters. This function is called when all
     * parameters have been received and starts camera capture at the correct
     * protocol time.
     *
     * @param {Guacamole.Client} client
     *     The Guacamole client for which camera redirection should be started.
     *
     * @param {Object} params
     *     Camera parameters received from server via argv instructions.
     *     Expected properties: width, height, fpsNum, fpsDenom, streamIndex
     *
     * @param {Function} [onState]
     *     Optional callback function to be invoked when the camera state changes.
     *
     * @returns {Promise<Object>}
     *     A promise that resolves with an object containing a stop() method.
     */
    function startCameraWithParams(client, params, onState) {

        // Convert server parameters to camera constraints format
        const constraints = {
            width: params.width,
            height: params.height,
            frameRate: params.fpsNum / params.fpsDenom,  // Calculate actual FPS (e.g., 15/1 = 15)
            deviceId: params.deviceId || undefined
        };

        // Use the existing startCamera function with server-provided constraints
        // Controller is registered immediately inside startCamera, no need to do it here
        return startCamera(client, constraints, onState);
    }

    /**
     * Checks if camera redirection is supported by the current browser.
     *
     * @returns {boolean}
     *     true if camera redirection is supported, false otherwise.
     */
    function isSupported() {
        return Guacamole.CameraRecorder.isSupportedType(RDPECAM_MIMETYPE);
    }

    /**
     * Map of client IDs to their camera controllers/stop functions.
     * Allows stopping camera for specific client instances.
     * 
     * @type {Object.<string, Function>}
     * @private
     */
    var cameraControllers = {};

    /**
     * Map of client IDs to their active camera recorder instances.
     * Used for cleanup and state management.
     *
     * @type {Object.<string, Guacamole.CameraRecorder>}
     * @private
     */
    var cameraRecorders = {};

    /**
     * Map of client IDs to their camera state objects.
     * Tracks whether camera is active for each client.
     * 
     * @type {Object.<string, Object>}
     * @private
     */
    var cameraStates = {};

    /**
     * Map of client IDs to their UI state update callbacks.
     * Called when camera active state changes.
     *
     * @type {Object.<string, Function>}
     * @private
     */
    var stateUpdateCallbacks = {};

    /**
     * LocalStorage key for persisting video delay setting.
     *
     * @constant
     * @type {string}
     * @private
     */
    var DELAY_STORAGE_KEY = 'guac_rdpecam_video_delay';

    /**
     * Maximum queue size in bytes before overflow protection triggers.
     * Set to 500KB (4x normal max at 1000ms delay).
     *
     * @constant
     * @type {number}
     * @private
     */
    var DELAY_QUEUE_OVERFLOW_THRESHOLD = 500 * 1024;

    /**
     * Timer interval in milliseconds for processing the delay queue.
     * 20ms provides max 20ms jitter while being CPU-efficient.
     *
     * @constant
     * @type {number}
     * @private
     */
    var DELAY_TIMER_INTERVAL_MS = 20;

    /**
     * Current video delay in milliseconds (0-1000ms).
     * Loaded from localStorage on first access.
     *
     * @type {number}
     * @private
     */
    var videoDelayMs = 0;

    /**
     * Whether videoDelayMs has been loaded from localStorage.
     *
     * @type {boolean}
     * @private
     */
    var videoDelayLoaded = false;

    /**
     * Map of client IDs to their delay queues.
     * Each queue is an array of {data: Blob, timestamp: number, size: number} objects.
     *
     * @type {Object.<string, Array>}
     * @private
     */
    var delayQueues = {};

    /**
     * Map of client IDs to their queue processing timer intervals.
     *
     * @type {Object.<string, number>}
     * @private
     */
    var delayTimerIntervals = {};

    /**
     * Loads the video delay setting from localStorage.
     * Falls back to memory-only storage if localStorage is unavailable.
     *
     * @private
     */
    function loadVideoDelay() {
        if (videoDelayLoaded)
            return;

        try {
            var stored = localStorage.getItem(DELAY_STORAGE_KEY);
            if (stored !== null) {
                var parsed = parseInt(stored, 10);
                if (!isNaN(parsed) && parsed >= 0 && parsed <= 1000) {
                    videoDelayMs = parsed;
                }
            }
        }
        catch (e) {
            // localStorage unavailable, using memory-only delay storage
        }

        videoDelayLoaded = true;
    }

    /**
     * Saves the video delay setting to localStorage.
     * Silently fails if localStorage is unavailable.
     *
     * @private
     */
    function saveVideoDelay() {
        try {
            localStorage.setItem(DELAY_STORAGE_KEY, String(videoDelayMs));
        }
        catch (e) {
            // Silent fallback - localStorage may be unavailable
        }
    }

    /**
     * Gets the current video delay setting in milliseconds.
     *
     * @returns {number}
     *     The current delay in milliseconds (0-1000).
     */
    function getVideoDelay() {
        loadVideoDelay();
        return videoDelayMs;
    }

    /**
     * Sets the video delay to an absolute value.
     *
     * @param {number} delayMs
     *     The new delay value in milliseconds (0-1000).
     *
     * @returns {number}
     *     The new delay value after clamping.
     */
    function setVideoDelay(delayMs) {
        loadVideoDelay();
        videoDelayMs = delayMs;

        // Clamp to [0, 1000]
        if (videoDelayMs < 0)
            videoDelayMs = 0;
        else if (videoDelayMs > 1000)
            videoDelayMs = 1000;

        saveVideoDelay();
        return videoDelayMs;
    }

    /**
     * Creates a delayed stream wrapper that queues video frames and sends them
     * after the configured delay. This wrapper proxies the Guacamole.OutputStream
     * interface.
     *
     * @param {Guacamole.OutputStream} realStream
     *     The actual stream to send delayed data to.
     *
     * @param {string} clientId
     *     The client ID for tracking this stream's queue.
     *
     * @returns {Object}
     *     A proxy object implementing the OutputStream interface with delay.
     *
     * @private
     */
    function createDelayedStream(realStream, clientId) {

        // Initialize queue for this client
        delayQueues[clientId] = [];

        var delayedStream = {
            /**
             * The stream index (proxied from real stream).
             * @type {number}
             */
            index: realStream.index,

            /**
             * Acknowledgement handler (proxied to real stream).
             * Initially null, set by the writer.
             * @type {Function|null}
             */
            onack: null,

            /**
             * Sends a blob with optional delay.
             * If delay is 0, sends immediately. Otherwise, queues for later.
             *
             * @param {string} data
             *     Base64-encoded blob data to send.
             */
            sendBlob: function(data) {
                loadVideoDelay();

                // If no delay, send immediately (zero overhead path)
                if (videoDelayMs === 0) {
                    realStream.sendBlob(data);
                    return;
                }

                // Estimate blob size (base64 is ~4/3 of binary size)
                var estimatedSize = Math.ceil(data.length * 3 / 4);

                // Queue the blob with current timestamp
                delayQueues[clientId].push({
                    data: data,
                    timestamp: Date.now(),
                    size: estimatedSize
                });

                // Check for queue overflow
                checkQueueOverflow(clientId);

                // Ensure timer is running
                startQueueTimer(clientId, realStream);
            },

            /**
             * Ends the stream (proxied to real stream).
             * Also clears the delay queue and stops the timer.
             */
            sendEnd: function() {
                stopQueueTimer(clientId);
                clearDelayQueue(clientId);
                realStream.sendEnd();
            }
        };

        // Forward ACKs from realStream to delayedStream
        // The server sends ACKs to realStream, but ArrayBufferWriter expects them on delayedStream
        realStream.onack = function(status) {
            if (delayedStream.onack) {
                delayedStream.onack(status);
            }
        };

        return delayedStream;
    }

    /**
     * Starts the queue processing timer for a client if not already running.
     *
     * @param {string} clientId
     *     The client ID.
     *
     * @param {Guacamole.OutputStream} realStream
     *     The real stream to send delayed frames to.
     *
     * @private
     */
    function startQueueTimer(clientId, realStream) {
        // Don't start if already running
        if (delayTimerIntervals[clientId])
            return;

        delayTimerIntervals[clientId] = setInterval(function() {
            processDelayQueue(clientId, realStream);
        }, DELAY_TIMER_INTERVAL_MS);
    }

    /**
     * Stops the queue processing timer for a client.
     *
     * @param {string} clientId
     *     The client ID.
     *
     * @private
     */
    function stopQueueTimer(clientId) {
        if (delayTimerIntervals[clientId]) {
            clearInterval(delayTimerIntervals[clientId]);
            delete delayTimerIntervals[clientId];
        }
    }

    /**
     * Processes the delay queue, sending frames that have aged past the delay threshold.
     *
     * @param {string} clientId
     *     The client ID.
     *
     * @param {Guacamole.OutputStream} realStream
     *     The real stream to send frames to.
     *
     * @private
     */
    function processDelayQueue(clientId, realStream) {
        var queue = delayQueues[clientId];
        if (!queue)
            return;

        loadVideoDelay();
        var now = Date.now();

        // Process frames in FIFO order until we hit one that's not ready
        while (queue.length > 0) {
            var item = queue[0];
            var age = now - item.timestamp;

            if (age >= videoDelayMs) {
                // Frame has aged enough, send it
                queue.shift();
                realStream.sendBlob(item.data);
            } else {
                // This frame (and all after it) aren't ready yet
                break;
            }
        }
    }

    /**
     * Clears the delay queue for a client, discarding all buffered frames.
     *
     * @param {string} clientId
     *     The client ID.
     *
     * @private
     */
    function clearDelayQueue(clientId) {
        if (delayQueues[clientId]) {
            delayQueues[clientId] = [];
        }
    }

    /**
     * Checks if the delay queue has exceeded the overflow threshold and drops
     * oldest frames if necessary.
     *
     * @param {string} clientId
     *     The client ID.
     *
     * @private
     */
    function checkQueueOverflow(clientId) {
        var queue = delayQueues[clientId];
        if (!queue)
            return;

        // Calculate total queue size
        var totalSize = 0;
        for (var i = 0; i < queue.length; i++) {
            totalSize += queue[i].size;
        }

        // Drop oldest frames if over threshold
        if (totalSize > DELAY_QUEUE_OVERFLOW_THRESHOLD) {
            // Drop oldest frames until under threshold
            while (totalSize > DELAY_QUEUE_OVERFLOW_THRESHOLD && queue.length > 0) {
                var dropped = queue.shift();
                totalSize -= dropped.size;
            }
        }
    }

    /**
     * Stops camera redirection for the given client.
     * This is called when server sends Stop Streams Request via argv camera-stop signal.
     *
     * @param {Guacamole.Client} client
     *     The Guacamole client for which camera redirection should be stopped.
     */
    function stopCamera(client) {
        if (!client) {
            return;
        }

        var clientId = getLocalClientId(client);
        if (!clientId) {
            return;
        }

        // Check if camera is already stopped
        if (!cameraControllers[clientId]) {
            return;
        }

        // Stop the camera for this client
        try {
            cameraControllers[clientId]();
            delete cameraControllers[clientId];
            if (cameraRecorders[clientId])
                delete cameraRecorders[clientId];

            // Clean up delay queue and timer
            stopQueueTimer(clientId);
            clearDelayQueue(clientId);

            updateCameraState(clientId, false);
        } catch (error) {
            // Error stopping camera - ignore
        }
    }

    /**
     * Updates the camera active state and notifies any registered callbacks.
     *
     * @param {string} clientId
     *     The client ID (tunnel UUID).
     *
     * @param {boolean} active
     *     Whether the camera is active.
     *
     * @private
     */
    function updateCameraState(clientId, active) {
        if (!cameraStates[clientId]) {
            cameraStates[clientId] = {};
        }
        cameraStates[clientId].active = active;
        
        // Call any registered state update callbacks
        if (stateUpdateCallbacks[clientId]) {
            try {
                stateUpdateCallbacks[clientId]({ active: active });
            } catch (error) {
                // Error in state update callback - ignore
            }
        }
    }

    /**
     * Registers a callback to be invoked when camera state changes.
     *
     * @param {Guacamole.Client} client
     *     The Guacamole client.
     *
     * @param {Function} callback
     *     Function to call when camera state changes. Receives object with active property.
     */
    function registerStateCallback(client, callback) {
        if (!client) {
            return;
        }
        var clientId = getLocalClientId(client);
        stateUpdateCallbacks[clientId] = callback;
        
        // Immediately call with current state
        if (cameraStates[clientId]) {
            callback({ active: cameraStates[clientId].active });
        }
    }

    // Public API
    return {
        startCameraWithParams: startCameraWithParams,
        prefetchCapabilities: prefetchCapabilities,
        isSupported: isSupported,
        stopCamera: stopCamera,
        registerStateCallback: registerStateCallback,
        getVideoDelay: getVideoDelay,
        setVideoDelay: setVideoDelay
    };

}]);
