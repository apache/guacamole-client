/*
 * Copyright (C) 2014 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/**
 * A directive for the guacamole client.
 */
angular.module('client').directive('guacClient', [function guacClient() {

    return {
        // Element only
        restrict: 'E',
        replace: true,
        scope: {

            /**
             * Parameters for controlling client state.
             * 
             * @type ClientProperties|Object
             */
            clientProperties : '=',
            
            /**
             * The ID of the Guacamole connection to connect to.
             * 
             * @type String
             */
            id : '=',

            /**
             * Arbitrary URL-encoded parameters to append to the connection
             * string when connecting.
             * 
             * @type String
             */
            connectionParameters : '='

        },
        templateUrl: 'app/client/templates/guacClient.html',
        controller: ['$scope', '$injector', '$element', function guacClientController($scope, $injector, $element) {
    
            /*
             * Safe $apply implementation from Alex Vanston:
             * https://coderwall.com/p/ngisma
             */
            $scope.safeApply = function(fn) {
                var phase = this.$root.$$phase;
                if(phase === '$apply' || phase === '$digest') {
                    if(fn && (typeof(fn) === 'function')) {
                        fn();
                    }
                } else {
                    this.$apply(fn);
                }
            };

            /**
             * Whether the local, hardware mouse cursor is in use.
             * 
             * @type Boolean
             */
            var localCursor = false;

            /**
             * The current Guacamole client instance.
             * 
             * @type Guacamole.Client 
             */
            var client = null;

            /**
             * The display of the current Guacamole client instance.
             * 
             * @type Guacamole.Display
             */
            var display = null;

            /**
             * The element associated with the display of the current
             * Guacamole client instance.
             *
             * @type Element
             */
            var displayElement = null;

            /**
             * The element which must contain the Guacamole display element.
             *
             * @type Element
             */
            var displayContainer = $element.find('.display')[0];

            /**
             * The main containing element for the entire directive.
             * 
             * @type Element
             */
            var main = $element[0];

            /**
             * The element which functions as a detector for size changes.
             * 
             * @type Element
             */
            var resizeSensor = $element.find('.resize-sensor')[0];

            /**
             * Guacamole mouse event object, wrapped around the main client
             * display.
             *
             * @type Guacamole.Mouse
             */
            var mouse = new Guacamole.Mouse(displayContainer);

            /**
             * Guacamole absolute mouse emulation object, wrapped around the
             * main client display.
             *
             * @type Guacamole.Mouse.Touchscreen
             */
            var touchScreen = new Guacamole.Mouse.Touchscreen(displayContainer);

            /**
             * Guacamole relative mouse emulation object, wrapped around the
             * main client display.
             *
             * @type Guacamole.Mouse.Touchpad
             */
            var touchPad = new Guacamole.Mouse.Touchpad(displayContainer);

            var $window               = $injector.get('$window'),
                guacAudio             = $injector.get('guacAudio'),
                guacVideo             = $injector.get('guacVideo'),
                guacHistory           = $injector.get('guacHistory'),
                guacTunnelFactory     = $injector.get('guacTunnelFactory'),
                guacClientFactory     = $injector.get('guacClientFactory'),
                authenticationService = $injector.get('authenticationService');
 
            /**
             * Updates the scale of the attached Guacamole.Client based on current window
             * size and "auto-fit" setting.
             */
            var updateDisplayScale = function updateDisplayScale() {

                if (!display) return;

                // Calculate scale to fit screen
                $scope.clientProperties.minScale = Math.min(
                    main.offsetWidth  / Math.max(display.getWidth(),  1),
                    main.offsetHeight / Math.max(display.getHeight(), 1)
                );

                // Calculate appropriate maximum zoom level
                $scope.clientProperties.maxScale = Math.max($scope.clientProperties.minScale, 3);

                // Clamp zoom level, maintain auto-fit
                if (display.getScale() < $scope.clientProperties.minScale || $scope.clientProperties.autoFit)
                    $scope.clientProperties.scale = $scope.clientProperties.minScale;

                else if (display.getScale() > $scope.clientProperties.maxScale)
                    $scope.clientProperties.scale = $scope.clientProperties.maxScale;

            };

            /**
             * Returns the string of connection parameters to be passed to the
             * Guacamole client during connection. This string generally
             * contains the desired connection ID, display resolution, and
             * supported audio/video codecs.
             * 
             * @returns {String} The string of connection parameters to be
             *                   passed to the Guacamole client.
             */
            var getConnectString = function getConnectString() {

                // Calculate optimal width/height for display
                var pixel_density = $window.devicePixelRatio || 1;
                var optimal_dpi = pixel_density * 96;
                var optimal_width = $window.innerWidth * pixel_density;
                var optimal_height = $window.innerHeight * pixel_density;

                // Build base connect string
                var connectString =
                      "id="         + encodeURIComponent($scope.id)
                    + "&authToken=" + encodeURIComponent(authenticationService.getCurrentToken())
                    + "&width="     + Math.floor(optimal_width)
                    + "&height="    + Math.floor(optimal_height)
                    + "&dpi="       + Math.floor(optimal_dpi)
                    + ($scope.connectionParameters ? '&' + $scope.connectionParameters : '');

                // Add audio mimetypes to connect_string
                guacAudio.supported.forEach(function(mimetype) {
                    connectString += "&audio=" + encodeURIComponent(mimetype);
                });

                // Add video mimetypes to connect_string
                guacVideo.supported.forEach(function(mimetype) {
                    connectString += "&video=" + encodeURIComponent(mimetype);
                });

                return connectString;

            };

            /**
             * Scrolls the client view such that the mouse cursor is visible.
             *
             * @param {Guacamole.Mouse.State} mouseState The current mouse
             *                                           state.
             */
            var scrollToMouse = function scrollToMouse(mouseState) {

                // Determine mouse position within view
                var mouse_view_x = mouseState.x + displayContainer.offsetLeft - main.scrollLeft;
                var mouse_view_y = mouseState.y + displayContainer.offsetTop  - main.scrollTop;

                // Determine viewport dimensions
                var view_width  = main.offsetWidth;
                var view_height = main.offsetHeight;

                // Determine scroll amounts based on mouse position relative to document

                var scroll_amount_x;
                if (mouse_view_x > view_width)
                    scroll_amount_x = mouse_view_x - view_width;
                else if (mouse_view_x < 0)
                    scroll_amount_x = mouse_view_x;
                else
                    scroll_amount_x = 0;

                var scroll_amount_y;
                if (mouse_view_y > view_height)
                    scroll_amount_y = mouse_view_y - view_height;
                else if (mouse_view_y < 0)
                    scroll_amount_y = mouse_view_y;
                else
                    scroll_amount_y = 0;

                // Scroll (if necessary) to keep mouse on screen.
                main.scrollLeft += scroll_amount_x;
                main.scrollTop  += scroll_amount_y;

            };

            /**
             * Sends the given mouse state to the current client.
             *
             * @param {Guacamole.Mouse.State} mouseState The mouse state to
             *                                           send.
             */
            var sendScaledMouseState = function sendScaledMouseState(mouseState) {

                // Scale event by current scale
                var scaledState = new Guacamole.Mouse.State(
                        mouseState.x / display.getScale(),
                        mouseState.y / display.getScale(),
                        mouseState.left,
                        mouseState.middle,
                        mouseState.right,
                        mouseState.up,
                        mouseState.down);

                // Send mouse event
                client.sendMouseState(scaledState);

            };

            /*
             * MOUSE
             */

            // Watch for changes to mouse emulation mode
            // Send all received mouse events to the client
            mouse.onmousedown =
            mouse.onmouseup   =
            mouse.onmousemove = function(mouseState) {

                if (!client || !display)
                    return;

                // Send mouse state, show cursor if necessary
                display.showCursor(!localCursor);
                sendScaledMouseState(mouseState);

            };

            // Hide software cursor when mouse leaves display
            mouse.onmouseout = function() {
                if (!display) return;
                display.showCursor(false);
            };

            /*
             * CLIPBOARD
             */

            // Update active client if clipboard changes
            $scope.$on('guacClipboard', function onClipboard(event, mimetype, data) {
                if (client)
                    client.setClipboard(data);
            });

            /*
             * CONNECT / RECONNECT
             */

            /**
             * Store the thumbnail of the currently connected client within
             * the connection history under the given ID. If the client is not
             * connected, or if no ID is given, this function has no effect.
             *
             * @param {String} id
             *     The ID of the history entry to update.
             */
            var updateHistoryEntry = function updateHistoryEntry(id) {

                // Update stored thumbnail of previous connection 
                if (id && display && display.getWidth() > 0 && display.getHeight() > 0) {

                    // Get screenshot
                    var canvas = display.flatten();
                    
                    // Calculate scale of thumbnail (max 320x240, max zoom 100%)
                    var scale = Math.min(320 / canvas.width, 240 / canvas.height, 1);
                    
                    // Create thumbnail canvas
                    var thumbnail = document.createElement("canvas");
                    thumbnail.width  = canvas.width*scale;
                    thumbnail.height = canvas.height*scale;
                    
                    // Scale screenshot to thumbnail
                    var context = thumbnail.getContext("2d");
                    context.drawImage(canvas,
                        0, 0, canvas.width, canvas.height,
                        0, 0, thumbnail.width, thumbnail.height
                    );

                    guacHistory.updateThumbnail(id, thumbnail.toDataURL("image/png"));

                }

            };

            // Connect to given ID whenever ID changes
            $scope.$watch('id', function(id, previousID) {

                // If a client is already attached, ensure it is disconnected
                if (client)
                    client.disconnect();

                // Update stored thumbnail of previous connection
                updateHistoryEntry(previousID);

                // Only proceed if a new client is attached
                if (!id)
                    return;

                // Get new client instance
                var tunnel = guacTunnelFactory.getInstance($scope);
                client = guacClientFactory.getInstance($scope, tunnel);

                // Init display
                display = client.getDisplay();
                display.scale($scope.clientProperties.scale);

                // Update the scale of the display when the client display size changes.
                display.onresize = function() {
                    $scope.safeApply(updateDisplayScale);
                };

                // Use local cursor if possible, update localCursor flag
                display.oncursor = function(canvas, x, y) {
                    localCursor = mouse.setCursor(canvas, x, y);
                };

                // Add display element
                displayElement = display.getElement();
                displayContainer.innerHTML = "";
                displayContainer.appendChild(displayElement);

                // Do nothing when the display element is clicked on.
                displayElement.onclick = function(e) {
                    e.preventDefault();
                    return false;
                };

                // Connect
                client.connect(getConnectString());

            });

            // Clean up when client directive is destroyed
            $scope.$on('$destroy', function destroyClient() {

                // Update stored thumbnail of current connection
                updateHistoryEntry($scope.id);

            });

            /*
             * MOUSE EMULATION
             */
                
            // Watch for changes to mouse emulation mode
            $scope.$watch('clientProperties.emulateAbsoluteMouse', function(emulateAbsoluteMouse) {

                if (!client || !display) return;

                var handleMouseState = function handleMouseState(mouseState) {

                    // Ensure software cursor is shown
                    display.showCursor(true);

                    // Send mouse state, ensure cursor is visible
                    scrollToMouse(mouseState);
                    sendScaledMouseState(mouseState);

                };

                var newMode, oldMode;

                // Switch to touchscreen if absolute
                if (emulateAbsoluteMouse) {
                    newMode = touchScreen;
                    oldMode = touchPad;
                }

                // Switch to touchpad if not absolute (relative)
                else {
                    newMode = touchPad;
                    oldMode = touchScreen;
                }

                // Set applicable mouse emulation object, unset the old one
                if (newMode) {

                    if (oldMode) {
                        oldMode.onmousedown = oldMode.onmouseup = oldMode.onmousemove = null;
                        newMode.currentState.x = oldMode.currentState.x;
                        newMode.currentState.y = oldMode.currentState.y;
                    }

                    newMode.onmousedown = newMode.onmouseup = newMode.onmousemove = handleMouseState;

                }

            });

            /*
             * DISPLAY SCALE / SIZE
             */
                
            // Adjust scale if modified externally
            $scope.$watch('clientProperties.scale', function changeScale(scale) {

                // Fix scale within limits
                scale = Math.max(scale, $scope.clientProperties.minScale);
                scale = Math.min(scale, $scope.clientProperties.maxScale);

                // If at minimum zoom level, hide scroll bars
                if (scale === $scope.clientProperties.minScale)
                    main.style.overflow = "hidden";

                // If not at minimum zoom level, show scroll bars
                else
                    main.style.overflow = "auto";

                // Apply scale if client attached
                if (display)
                    display.scale(scale);
                
                if (scale !== $scope.clientProperties.scale)
                    $scope.clientProperties.scale = scale;

            });
            
            // If autofit is set, the scale should be set to the minimum scale, filling the screen
            $scope.$watch('clientProperties.autoFit', function changeAutoFit(autoFit) {
                if(autoFit)
                    $scope.clientProperties.scale = $scope.clientProperties.minScale;
            });
            
            // If the element is resized, attempt to resize client
            resizeSensor.contentWindow.addEventListener('resize', function mainElementResized() {

                // Send new display size, if changed
                if (client && display) {

                    var pixelDensity = $window.devicePixelRatio || 1;
                    var width  = main.offsetWidth  * pixelDensity;
                    var height = main.offsetHeight * pixelDensity;

                    if (display.getWidth() !== width || display.getHeight() !== height)
                        client.sendSize(width, height);

                }

                $scope.safeApply(updateDisplayScale);

            });
            
            /*
             * KEYBOARD
             */
                
            // Listen for broadcasted keydown events and fire the appropriate listeners
            $scope.$on('guacKeydown', function keydownListener(event, keysym, keyboard) {
                if ($scope.clientProperties.keyboardEnabled && !event.defaultPrevented) {
                    client.sendKeyEvent(1, keysym);
                    event.preventDefault();
                }
            });
            
            // Listen for broadcasted keyup events and fire the appropriate listeners
            $scope.$on('guacKeyup', function keyupListener(event, keysym, keyboard) {
                if ($scope.clientProperties.keyboardEnabled && !event.defaultPrevented) {
                    client.sendKeyEvent(0, keysym);
                    event.preventDefault();
                }   
            });

            // Universally handle all synthetic keydown events
            $scope.$on('guacSyntheticKeydown', function syntheticKeydownListener(event, keysym) {
                client.sendKeyEvent(1, keysym);
            });
            
            // Universally handle all synthetic keyup events
            $scope.$on('guacSyntheticKeyup', function syntheticKeyupListener(event, keysym) {
                client.sendKeyEvent(0, keysym);
            });
            
            /**
             * Converts the given bytes to a base64-encoded string.
             * 
             * @param {Uint8Array} bytes A Uint8Array which contains the data to be
             *                           encoded as base64.
             * @return {String} The base64-encoded string.
             */
            function getBase64(bytes) {

                var data = "";

                // Produce binary string from bytes in buffer
                for (var i=0; i<bytes.byteLength; i++)
                    data += String.fromCharCode(bytes[i]);

                // Convert to base64
                return $window.btoa(data);

            }
            
            /**
             * Ignores the given event.
             * 
             * @param {Event} e The event to ignore.
             */
            function ignoreEvent(e) {
               e.preventDefault();
               e.stopPropagation();
            }
            
            /**
             * Uploads the given file to the server.
             * 
             * @param {File} file The file to upload.
             */
            function uploadFile(file) {

                // Construct reader for file
                var reader = new FileReader();
                reader.onloadend = function() {

                    // Open file for writing
                    var stream = client.createFileStream(file.type, file.name);

                    var valid = true;
                    var bytes = new Uint8Array(reader.result);
                    var offset = 0;

                    // Add upload notification
                    $scope.$emit('guacClientFileUploadStart', client, stream.index, file.type, file.name, bytes.length);

                    // Invalidate stream on all errors
                    // Continue upload when acknowledged
                    stream.onack = function(status) {

                        // Handle errors 
                        if (status.isError()) {
                            valid = false;
                            $scope.$emit('guacClientFileUploadError', client, stream.index, file.type, file.name, bytes.length, status.code);
                        }

                        // Abort upload if stream is invalid
                        if (!valid) return false;

                        // Encode packet as base64
                        var slice = bytes.subarray(offset, offset+4096);
                        var base64 = getBase64(slice);

                        // Write packet
                        stream.sendBlob(base64);

                        // Advance to next packet
                        offset += 4096;

                        // If at end, stop upload
                        if (offset >= bytes.length) {
                            stream.sendEnd();
                            $scope.$emit('guacClientFileUploadProgress', client, stream.index, file.type, file.name, bytes.length, bytes.length);
                            $scope.$emit('guacClientFileUploadEnd', client, stream.index, file.type, file.name, bytes.length);
                        }

                        // Otherwise, update progress
                        else
                            $scope.$emit('guacClientFileUploadProgress', client, stream.index, file.type, file.name, bytes.length, offset);

                    };

                };
                reader.readAsArrayBuffer(file);

            }

            // Handle and ignore dragenter/dragover
            displayContainer.addEventListener("dragenter", ignoreEvent, false);
            displayContainer.addEventListener("dragover",  ignoreEvent, false);

            // File drop event handler
            displayContainer.addEventListener("drop", function(e) {

                e.preventDefault();
                e.stopPropagation();

                // Ignore file drops if no attached client
                if (!client) return;

                // Upload each file 
                var files = e.dataTransfer.files;
                for (var i=0; i<files.length; i++)
                    uploadFile(files[i]);

            }, false);

            /*
             * END CLIENT DIRECTIVE                                           
             */
                
        }]
    };
}]);