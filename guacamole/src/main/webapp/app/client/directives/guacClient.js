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
             * The client to display within this guacClient directive.
             * 
             * @type ManagedClient
             */
            client : '='
            
        },
        templateUrl: 'app/client/templates/guacClient.html',
        controller: ['$scope', '$injector', '$element', function guacClientController($scope, $injector, $element) {
   
            // Required services
            var $window     = $injector.get('$window');
                
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

            /**
             * Updates the scale of the attached Guacamole.Client based on current window
             * size and "auto-fit" setting.
             */
            var updateDisplayScale = function updateDisplayScale() {

                if (!display) return;

                // Calculate scale to fit screen
                $scope.client.clientProperties.minScale = Math.min(
                    main.offsetWidth  / Math.max(display.getWidth(),  1),
                    main.offsetHeight / Math.max(display.getHeight(), 1)
                );

                // Calculate appropriate maximum zoom level
                $scope.client.clientProperties.maxScale = Math.max($scope.client.clientProperties.minScale, 3);

                // Clamp zoom level, maintain auto-fit
                if (display.getScale() < $scope.client.clientProperties.minScale || $scope.client.clientProperties.autoFit)
                    $scope.client.clientProperties.scale = $scope.client.clientProperties.minScale;

                else if (display.getScale() > $scope.client.clientProperties.maxScale)
                    $scope.client.clientProperties.scale = $scope.client.clientProperties.maxScale;

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

            // Attach any given managed client
            $scope.$watch('client', function attachManagedClient(managedClient) {

                // Remove any existing display
                displayContainer.innerHTML = "";

                // Only proceed if a client is given 
                if (!managedClient)
                    return;

                // Get Guacamole client instance
                client = managedClient.client;

                // Attach possibly new display
                display = client.getDisplay();
                display.scale($scope.client.clientProperties.scale);

                // Add display element
                displayElement = display.getElement();
                displayContainer.appendChild(displayElement);

                // Do nothing when the display element is clicked on
                display.getElement().onclick = function(e) {
                    e.preventDefault();
                    return false;
                };

            });

            // Update actual view scrollLeft when scroll properties change
            $scope.$watch('client.clientProperties.scrollLeft', function scrollLeftChanged(scrollLeft) {
                main.scrollLeft = scrollLeft;
                $scope.client.clientProperties.scrollLeft = main.scrollLeft;
            });

            // Update actual view scrollTop when scroll properties change
            $scope.$watch('client.clientProperties.scrollTop', function scrollTopChanged(scrollTop) {
                main.scrollTop = scrollTop;
                $scope.client.clientProperties.scrollTop = main.scrollTop;
            });

            // Update scale when display is resized
            $scope.$watch('client.managedDisplay.size', function setDisplaySize() {
                $scope.$evalAsync(updateDisplayScale);
            });

            // Keep local cursor up-to-date
            $scope.$watch('client.managedDisplay.cursor', function setCursor(cursor) {
                if (cursor)
                    localCursor = mouse.setCursor(cursor.canvas, cursor.x, cursor.y);
            });

            // Swap mouse emulation modes depending on absolute mode flag
            $scope.$watch('client.clientProperties.emulateAbsoluteMouse', function(emulateAbsoluteMouse) {

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

            // Adjust scale if modified externally
            $scope.$watch('client.clientProperties.scale', function changeScale(scale) {

                // Fix scale within limits
                scale = Math.max(scale, $scope.client.clientProperties.minScale);
                scale = Math.min(scale, $scope.client.clientProperties.maxScale);

                // If at minimum zoom level, hide scroll bars
                if (scale === $scope.client.clientProperties.minScale)
                    main.style.overflow = "hidden";

                // If not at minimum zoom level, show scroll bars
                else
                    main.style.overflow = "auto";

                // Apply scale if client attached
                if (display)
                    display.scale(scale);
                
                if (scale !== $scope.client.clientProperties.scale)
                    $scope.client.clientProperties.scale = scale;

            });
            
            // If autofit is set, the scale should be set to the minimum scale, filling the screen
            $scope.$watch('client.clientProperties.autoFit', function changeAutoFit(autoFit) {
                if(autoFit)
                    $scope.client.clientProperties.scale = $scope.client.clientProperties.minScale;
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

                $scope.$apply(updateDisplayScale);

            });

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

            // Update remote clipboard if local clipboard changes
            $scope.$on('guacClipboard', function onClipboard(event, mimetype, data) {
                if (client)
                    client.setClipboard(data);
            });

            // Translate local keydown events to remote keydown events if keyboard is enabled
            $scope.$on('guacKeydown', function keydownListener(event, keysym, keyboard) {
                if ($scope.client.clientProperties.keyboardEnabled && !event.defaultPrevented) {
                    client.sendKeyEvent(1, keysym);
                    event.preventDefault();
                }
            });
            
            // Translate local keyup events to remote keyup events if keyboard is enabled
            $scope.$on('guacKeyup', function keyupListener(event, keysym, keyboard) {
                if ($scope.client.clientProperties.keyboardEnabled && !event.defaultPrevented) {
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