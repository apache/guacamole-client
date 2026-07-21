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
 * The controller for the page used to display secondary monitors.
 */
angular.module('client').controller('secondaryMonitorController', ['$scope', '$injector', '$routeParams',
    function clientController($scope, $injector, $routeParams) {

    // Required services
    const $window           = $injector.get('$window');
    const guacFullscreen    = $injector.get('guacFullscreen');
    const guacManageMonitor = $injector.get('guacManageMonitor');

    /**
     * ID of this monitor.
     * 
     * @type {!String}
     */
    const monitorId = $routeParams.id;

    /**
     * In order to open the guacamole menu, we need to hit ctrl-alt-shift. There are
     * several possible keysysms for each key.
     */
    const SHIFT_KEYS  = {0xFFE1 : true, 0xFFE2 : true},
          ALT_KEYS    = {0xFFE9 : true, 0xFFEA : true, 0xFE03 : true,
                         0xFFE7 : true, 0xFFE8 : true},
          CTRL_KEYS   = {0xFFE3 : true, 0xFFE4 : true},
          MENU_KEYS   = angular.extend({}, SHIFT_KEYS, ALT_KEYS, CTRL_KEYS);

    guacManageMonitor.init("secondary");
    guacManageMonitor.monitorId = monitorId;

    guacManageMonitor.openConsentButton = function openConsentButton() {

        // Show button
        $scope.showFullscreenConsent = true;
        $scope.$apply();

        // Auto hide button after delay
        setTimeout(function() {
            $scope.showFullscreenConsent = false;
            $scope.$apply();
        }, 10000);

    };

    /**
     * User clicked on the consent button : switch to fullscreen mode and hide
     * the button.
     */
    $scope.enableFullscreenMode = function enableFullscreenMode() {
        guacFullscreen.setFullscreenMode(true);
        $scope.showFullscreenConsent = false;
    };

    /**
     * Returns whether the shortcut for showing/hiding the Guacamole menu
     * (Ctrl+Alt+Shift) has been pressed.
     *
     * @param {Guacamole.Keyboard} keyboard
     *     The Guacamole.Keyboard object tracking the local keyboard state.
     *
     * @returns {boolean}
     *     true if Ctrl+Alt+Shift has been pressed, false otherwise.
     */  
    const isMenuShortcutPressed = function isMenuShortcutPressed(keyboard) {

        // Ctrl+Alt+Shift has NOT been pressed if any key is currently held
        // down that isn't Ctrl, Alt, or Shift
        if (_.findKey(keyboard.pressed, (_, keysym) => !MENU_KEYS[keysym]))
            return false;

        // Verify that one of each required key is held, regardless of
        // left/right location on the keyboard
        return !!(
                _.findKey(SHIFT_KEYS, (_, keysym) => keyboard.pressed[keysym])
                && _.findKey(ALT_KEYS,   (_, keysym) => keyboard.pressed[keysym])
                && _.findKey(CTRL_KEYS,  (_, keysym) => keyboard.pressed[keysym])
        );

    };

    // Opening the Guacamole menu after Ctrl+Alt+Shift, preventing those
    // keypresses from reaching any Guacamole client
    $scope.$on('guacBeforeKeydown', function incomingKeydown(event, keysym, keyboard) {

        // Toggle menu if menu shortcut (Ctrl+Alt+Shift) is pressed
        if (isMenuShortcutPressed(keyboard)) {
        
            // Don't send this key event through to the client, and release
            // all other keys involved in performing this shortcut
            event.preventDefault();
            keyboard.reset();
            
            // Toggle the menu
            $scope.$apply(function() {
                guacManageMonitor.pushBroadcastMessage('guacMenu', true);
            });

        }

    });

    // Send monitor-close event to broadcast channel on window unload
    $window.addEventListener('beforeunload', function unloadWindow() {
        guacManageMonitor.pushBroadcastMessage('monitorClose', monitorId);
    });

}]);
