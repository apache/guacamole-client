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
            // Parameters for controlling client state
            clientProperties        : '=',
            
            // Parameters for initially connecting
            id                      : '=',
            connectionName          : '=', 
            connectionParameters    : '='
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

            $scope.guac = null;
            $scope.clipboard = "";

            var $window             = $injector.get('$window'),
                guacAudio           = $injector.get('guacAudio'),
                guacVideo           = $injector.get('guacVideo'),
                guacTunnelFactory   = $injector.get('guacTunnelFactory'),
                guacClientFactory   = $injector.get('guacClientFactory'),
                localStorageUtility = $injector.get('localStorageUtility');
                
            // Get elements for DOM manipulation
            var main = $element[0];
                
            var CLIENT_PROPERTY_DEFAULTS = {
                scale: 1
            };
            
            for (var propertyName in CLIENT_PROPERTY_DEFAULTS) {
                if (!(propertyName in $scope.clientProperties))
                    $scope.clientProperties[propertyName] = CLIENT_PROPERTY_DEFAULTS[propertyName];
            }
            
            /**
             * Updates the scale of the attached Guacamole.Client based on current window
             * size and "auto-fit" setting.
             */
            var updateDisplayScale = function updateDisplayScale() {

                var guac = $scope.guac;
                if (!guac)
                    return;

                // Calculate scale to fit screen
                $scope.clientProperties.minScale = Math.min(
                    main.offsetWidth / Math.max(guac.getDisplay().getWidth(), 1),
                    main.offsetHeight / Math.max(guac.getDisplay().getHeight(), 1)
                );

                // Calculate appropriate maximum zoom level
                $scope.clientProperties.maxScale = Math.max($scope.clientProperties.minScale, 3);

                // Clamp zoom level, maintain auto-fit
                if (guac.getDisplay().getScale() < $scope.clientProperties.minScale || $scope.clientProperties.autoFit)
                    $scope.clientProperties.scale = $scope.clientProperties.minScale;

                else if (guac.getDisplay().getScale() > $scope.clientProperties.maxScale)
                    $scope.clientProperties.scale = $scope.clientProperties.maxScale;

            };

            // Update active client if clipboard changes
            $scope.$watch('clipboard', function clipboardChange(data) {
                if ($scope.guac)
                    $scope.guac.setClipboard(data);
            });

            // Connect to given ID whenever ID changes
            $scope.$watch('id', function(id) {

                // If a client is already attached, ensure it is disconnected
                if ($scope.guac)
                    $scope.guac.disconnect();

                // Only proceed if a new client is attached
                if (!id)
                    return;

                // Get new client instance
                var tunnel = guacTunnelFactory.getInstance();
                var guac = guacClientFactory.getInstance(tunnel);
                $scope.guac = guac;

                /*
                 * Update the scale of the display when the client display size changes.
                 */

                guac.getDisplay().onresize = function() {
                    $scope.safeApply(updateDisplayScale);
                };

                // Get display element
                var guac_display = guac.getDisplay().getElement();

                /*
                 * Do nothing when the display element is clicked on.
                 */

                guac_display.onclick = function(e) {
                    e.preventDefault();
                    return false;
                };

                /*
                 * Handle mouse and touch events relative to the display element.
                 */

                var localCursor = false;

                // Ensure software cursor is shown
                guac.getDisplay().showCursor(true);

                // Use local cursor if possible.
                guac.getDisplay().oncursor = function(canvas, x, y) {
                    localCursor = mouse.setCursor(canvas, x, y);
                };

                // Mouse
                var mouse = new Guacamole.Mouse(guac_display);
                mouse.onmousedown = mouse.onmouseup = mouse.onmousemove = function(mouseState) {

                    // Hide software cursor if local cursor is in use
                    guac.getDisplay().showCursor(!localCursor);

                    // Scale event by current scale
                    var scaledState = new Guacamole.Mouse.State(
                            mouseState.x / guac.getDisplay().getScale(),
                            mouseState.y / guac.getDisplay().getScale(),
                            mouseState.left,
                            mouseState.middle,
                            mouseState.right,
                            mouseState.up,
                            mouseState.down);

                    // Send mouse event
                    guac.sendMouseState(scaledState);

                };

                // Hide software cursor when mouse leaves display
                mouse.onmouseout = function() {
                    guac.getDisplay().showCursor(false);
                };

                var $display = $element.find('.display');

                // Remove old client from UI, if any
                $display.html("");

                // Add client to UI
                guac_display.className = "software-cursor";
                $display.append(guac_display);

                // Mouse emulation objects
                var touchScreen = new Guacamole.Mouse.Touchscreen(guac_display);
                var touchPad    = new Guacamole.Mouse.Touchpad(guac_display);

                // Watch for changes to mouse emulation mode
                $scope.$watch('clientProperties.emulateAbsoluteMouse', function(emulateAbsoluteMouse) {

                    var handleMouseState = function handleMouseState(mouseState) {

                        // Ensure software cursor is shown
                        guac.getDisplay().showCursor(true);

                        // Determine mouse position within view
                        var guac_display = guac.getDisplay().getElement();
                        var mouse_view_x = mouseState.x + guac_display.offsetLeft - main.scrollLeft;
                        var mouse_view_y = mouseState.y + guac_display.offsetTop  - main.scrollTop;

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

                        // Scale event by current scale
                        var scaledState = new Guacamole.Mouse.State(
                                mouseState.x / guac.getDisplay().getScale(),
                                mouseState.y / guac.getDisplay().getScale(),
                                mouseState.left,
                                mouseState.middle,
                                mouseState.right,
                                mouseState.up,
                                mouseState.down);

                        // Send mouse event
                        guac.sendMouseState(scaledState);

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

                // Calculate optimal width/height for display
                var pixel_density = $window.devicePixelRatio || 1;
                var optimal_dpi = pixel_density * 96;
                var optimal_width = $window.innerWidth * pixel_density;
                var optimal_height = $window.innerHeight * pixel_density;

                // Scale width/height to be at least 600x600
                if (optimal_width < 600 || optimal_height < 600) {
                    var scale = Math.max(600 / optimal_width, 600 / optimal_height);
                    optimal_width = optimal_width * scale;
                    optimal_height = optimal_height * scale;
                }

                // Get entire query string, and pass to connect().
                // Normally, only the "id" parameter is required, but
                // all parameters should be preserved and passed on for
                // the sake of authentication.

                var connectString =
                      "id="         + encodeURIComponent($scope.id)
                    + "&authToken=" + encodeURIComponent(localStorageUtility.get('authToken'))
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

                // Connect
                guac.connect(connectString);

            });

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
                if ($scope.guac)
                    $scope.guac.getDisplay().scale(scale);
                
                if (scale !== $scope.clientProperties.scale)
                    $scope.clientProperties.scale = scale;

            });
            
            // If autofit is set, the scale should be set to the minimum scale, filling the screen
            $scope.$watch('clientProperties.autoFit', function changeAutoFit(autoFit) {
                if(autoFit)
                    $scope.clientProperties.scale = $scope.clientProperties.minScale;
            });
            
            // If the window is resized, attempt to resize client
            $window.addEventListener('resize', function onResizeWindow() {
                $scope.safeApply(updateDisplayScale);
            });
            
            var show_keyboard_gesture_possible = true;
            
            // Handle Keyboard events
            function __send_key(pressed, keysym) {
                $scope.guac.sendKeyEvent(pressed, keysym);
                return false;
            }

            $scope.keydown = function keydown (keysym, keyboard) {

                // Only handle key events if client is attached
                var guac = $scope.guac;
                if (!guac) return true;

                // Handle Ctrl-shortcuts specifically
                if (keyboard.modifiers.ctrl && !keyboard.modifiers.alt && !keyboard.modifiers.shift) {

                    // Allow event through if Ctrl+C or Ctrl+X
                    if (keyboard.pressed[0x63] || keyboard.pressed[0x78]) {
                        __send_key(1, keysym);
                        return true;
                    }

                    // If Ctrl+V, wait until after paste event (next event loop)
                    if (keyboard.pressed[0x76]) {
                        window.setTimeout(function after_paste() {
                            __send_key(1, keysym);
                        }, 10);
                        return true;
                    }

                }

                // If key is NOT one of the expected keys, gesture not possible
                if (keysym !== 0xFFE3 && keysym !== 0xFFE9 && keysym !== 0xFFE1)
                    show_keyboard_gesture_possible = false;

                // Send key event
                return __send_key(1, keysym);

            };

            $scope.keyup = function keyup(keysym, keyboard) {

                // Only handle key events if client is attached
                var guac = $scope.guac;
                if (!guac) return true;

                // If lifting up on shift, toggle menu visibility if rest of gesture
                // conditions satisfied
                if (show_keyboard_gesture_possible && keysym === 0xFFE1 
                    && keyboard.pressed[0xFFE3] && keyboard.pressed[0xFFE9]) {
                        __send_key(0, 0xFFE1);
                        __send_key(0, 0xFFE9);
                        __send_key(0, 0xFFE3);
                        
                        // Emit an event to show the menu
                        $scope.$emit('guacClientMenu', true);
                }

                // Detect if no keys are pressed
                var reset_gesture = true;
                for (var pressed in keyboard.pressed) {
                    reset_gesture = false;
                    break;
                }

                // Reset gesture state if possible
                if (reset_gesture)
                    show_keyboard_gesture_possible = true;

                // Send key event
                return __send_key(0, keysym);

            };
            
            // Listen for broadcasted keydown events and fire the appropriate listeners
            $scope.$on('guacKeydown', function keydownListener(event, keysym, keyboard) {
                if ($scope.clientProperties.keyboardEnabled) {
                    var preventDefault = $scope.keydown(keysym, keyboard);
                    if (preventDefault) {
                        event.preventDefault();
                    }
                }
            });
            
            // Listen for broadcasted keyup events and fire the appropriate listeners
            $scope.$on('guacKeyup', function keyupListener(event, keysym, keyboard) {
                if ($scope.clientProperties.keyboardEnabled) {
                    var preventDefault = $scope.keyup(keysym, keyboard);
                    if(preventDefault) {
                        event.preventDefault();
                    }
                }
            });
            
        }]
    };
}]);