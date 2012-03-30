
/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is guacamole-common-js.
 *
 * The Initial Developer of the Original Code is
 * Michael Jumper.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

// Guacamole namespace
var Guacamole = Guacamole || {};

/**
 * Provides cross-browser and cross-keyboard keyboard for a specific element.
 * Browser and keyboard layout variation is abstracted away, providing events
 * which represent keys as their corresponding X11 keysym.
 * 
 * @constructor
 * @param {Element} element The Element to use to provide keyboard events.
 */
Guacamole.Keyboard = function(element) {

    /**
     * Reference to this Guacamole.Keyboard.
     * @private
     */
    var guac_keyboard = this;

    /**
     * Fired whenever the user presses a key with the element associated
     * with this Guacamole.Keyboard in focus.
     * 
     * @event
     * @param {Number} keysym The keysym of the key being pressed.
     * @returns {Boolean} true if the originating event of this keypress should
     *                    be allowed through to the browser, false or undefined
     *                    otherwise.
     */
    this.onkeydown = null;

    /**
     * Fired whenever the user releases a key with the element associated
     * with this Guacamole.Keyboard in focus.
     * 
     * @event
     * @param {Number} keysym The keysym of the key being released.
     * @returns {Boolean} true if the originating event of this key release 
     *                    should be allowed through to the browser, false or
     *                    undefined otherwise.
     */
    this.onkeyup = null;

    /**
     * Map of known JavaScript keycodes which do not map to typable characters
     * to their unshifted X11 keysym equivalents.
     * @private
     */
    var unshiftedKeySym = {
        8:   0xFF08, // backspace
        9:   0xFF09, // tab
        13:  0xFF0D, // enter
        16:  0xFFE1, // shift
        17:  0xFFE3, // ctrl
        18:  0xFFE9, // alt
        19:  0xFF13, // pause/break
        20:  0xFFE5, // caps lock
        27:  0xFF1B, // escape
        33:  0xFF55, // page up
        34:  0xFF56, // page down
        35:  0xFF57, // end
        36:  0xFF50, // home
        37:  0xFF51, // left arrow
        38:  0xFF52, // up arrow
        39:  0xFF53, // right arrow
        40:  0xFF54, // down arrow
        45:  0xFF63, // insert
        46:  0xFFFF, // delete
        91:  0xFFEB, // left window key (super_l)
        92:  0xFF67, // right window key (menu key?)
        93:  null,   // select key
        112: 0xFFBE, // f1
        113: 0xFFBF, // f2
        114: 0xFFC0, // f3
        115: 0xFFC1, // f4
        116: 0xFFC2, // f5
        117: 0xFFC3, // f6
        118: 0xFFC4, // f7
        119: 0xFFC5, // f8
        120: 0xFFC6, // f9
        121: 0xFFC7, // f10
        122: 0xFFC8, // f11
        123: 0xFFC9, // f12
        144: 0xFF7F, // num lock
        145: 0xFF14  // scroll lock
    };

    /**
     * Map of known JavaScript keycodes which do not map to typable characters
     * to their shifted X11 keysym equivalents. Keycodes must only be listed
     * here if their shifted X11 keysym equivalents differ from their unshifted
     * equivalents.
     * @private
     */
    var shiftedKeySym = {
        18:  0xFFE7  // alt
    };

    /**
     * All modifiers and their states.
     */
    this.modifiers = {
        
        /**
         * Whether shift is currently pressed.
         */
        "shift": false,
        
        /**
         * Whether ctrl is currently pressed.
         */
        "ctrl" : false,
        
        /**
         * Whether alt is currently pressed.
         */
        "alt"  : false

    };

    /**
     * The state of every key, indexed by keysym. If a particular key is
     * pressed, the value of pressed for that keysym will be true. If a key
     * is not currently pressed, the value for that keysym may be false or
     * undefined.
     */
    this.pressed = [];

    var keydownChar = new Array();

    // ID of routine repeating keystrokes. -1 = not repeating.
    var repeatKeyTimeoutId = -1;
    var repeatKeyIntervalId = -1;

    // Starts repeating keystrokes
    function startRepeat(keySym) {
        repeatKeyIntervalId = setInterval(function() {
            sendKeyReleased(keySym);
            sendKeyPressed(keySym);
        }, 50);
    }

    // Stops repeating keystrokes
    function stopRepeat() {
        if (repeatKeyTimeoutId != -1) clearInterval(repeatKeyTimeoutId);
        if (repeatKeyIntervalId != -1) clearInterval(repeatKeyIntervalId);
    }


    function getKeySymFromKeyIdentifier(shifted, keyIdentifier) {

        var unicodePrefixLocation = keyIdentifier.indexOf("U+");
        if (unicodePrefixLocation >= 0) {

            var hex = keyIdentifier.substring(unicodePrefixLocation+2);
            var codepoint = parseInt(hex, 16);
            var typedCharacter;

            // Convert case if shifted
            if (shifted == 0)
                typedCharacter = String.fromCharCode(codepoint).toLowerCase();
            else
                typedCharacter = String.fromCharCode(codepoint).toUpperCase();

            // Get codepoint
            codepoint = typedCharacter.charCodeAt(0);

            return getKeySymFromCharCode(codepoint);

        }

        return null;

    }

    function getKeySymFromCharCode(keyCode) {

        if (keyCode >= 0x0000 && keyCode <= 0x00FF)
            return keyCode;

        if (keyCode >= 0x0100 && keyCode <= 0x10FFFF)
            return 0x01000000 | keyCode;

        return null;

    }

    function getKeySymFromKeyCode(keyCode) {

        var keysym = null;
        if (!guac_keyboard.modifiers.shift) keysym = unshiftedKeySym[keyCode];
        else {
            keysym = shiftedKeySym[keyCode];
            if (keysym == null) keysym = unshiftedKeySym[keyCode];
        }

        return keysym;

    }


    // Sends a single keystroke over the network
    function sendKeyPressed(keysym) {

        // Mark key as pressed
        guac_keyboard.pressed[keysym] = true;

        // Send key event
        if (keysym != null && guac_keyboard.onkeydown)
            return guac_keyboard.onkeydown(keysym) != false;
        
        return true;

    }

    // Sends a single keystroke over the network
    function sendKeyReleased(keysym) {

        // Mark key as released
        guac_keyboard.pressed[keysym] = false;

        // Send key event
        if (keysym != null && guac_keyboard.onkeyup)
            return guac_keyboard.onkeyup(keysym) != false;

        return true;

    }


    var KEYDOWN = 1;
    var KEYPRESS = 2;

    var keySymSource = null;

    // When key pressed
    var keydownCode = null;
    element.onkeydown = function(e) {

        // Only intercept if handler set
        if (!guac_keyboard.onkeydown) return true;

        var keynum;
        if (window.event) keynum = window.event.keyCode;
        else if (e.which) keynum = e.which;

        // Ctrl/Alt/Shift
        if (keynum == 16)      guac_keyboard.modifiers.shift = true;
        else if (keynum == 17) guac_keyboard.modifiers.ctrl  = true;
        else if (keynum == 18) guac_keyboard.modifiers.alt   = true;

        // If keysym is defined for given key code, key events can come from
        // KEYDOWN.
        var keysym = getKeySymFromKeyCode(keynum);
        if (keysym)
            keySymSource = KEYDOWN;

        // Otherwise, if modifier keys are held down, try to get from keyIdentifier
        else if ((guac_keyboard.modifiers.ctrl || guac_keyboard.modifiers.alt) && e.keyIdentifier) {

            // Get keysym from keyIdentifier
            keysym = getKeySymFromKeyIdentifier(guac_keyboard.modifiers.shift, e.keyIdentifier);

            // Get keysyms and events from KEYDOWN
            keySymSource = KEYDOWN;

        }

        // Otherwise, resort to KEYPRESS
        else
            keySymSource = KEYPRESS;

        keydownCode = keynum;

        // Ignore key if we don't need to use KEYPRESS.
        // Send key event here
        if (keySymSource == KEYDOWN) {

            if (keydownChar[keynum] != keysym) {

                // Send event
                keydownChar[keynum] = keysym;
                var returnValue = sendKeyPressed(keysym);

                // Clear old key repeat, if any.
                stopRepeat();

                // Start repeating (if not a modifier key) after a short delay
                if (keynum != 16 && keynum != 17 && keynum != 18)
                    repeatKeyTimeoutId = setTimeout(function() { startRepeat(keysym); }, 500);

                // Use return code provided by handler
                return returnValue;

            }

            // Default to canceling event if no keypress is being sent, but
            // source of events is keydown.
            return false;

        }

        return true;

    };

    // When key pressed
    element.onkeypress = function(e) {

        // Only intercept if handler set
        if (!guac_keyboard.onkeydown) return true;

        if (keySymSource != KEYPRESS) return false;

        var keynum;
        if (window.event) keynum = window.event.keyCode;
        else if (e.which) keynum = e.which;

        var keysym = getKeySymFromCharCode(keynum);
        if (keysym && keydownChar[keynum] != keysym) {

            // If this button already pressed, release first
            var lastKeyDownChar = keydownChar[keydownCode];
            if (lastKeyDownChar)
                sendKeyReleased(lastKeyDownChar);

            keydownChar[keydownCode] = keysym;

            // Clear old key repeat, if any.
            stopRepeat();

            // Send key event
            var returnValue = sendKeyPressed(keysym);

            // Start repeating (if not a modifier key) after a short delay
            repeatKeyTimeoutId = setTimeout(function() { startRepeat(keysym); }, 500);

            return returnValue;
        }

        // Default to canceling event if no keypress is being sent, but
        // source of events is keypress.
        return false;

    };

    // When key released
    element.onkeyup = function(e) {

        // Only intercept if handler set
        if (!guac_keyboard.onkeyup) return true;

        var keynum;
        if (window.event) keynum = window.event.keyCode;
        else if (e.which) keynum = e.which;
        
        // Ctrl/Alt/Shift
        if (keynum == 16)      guac_keyboard.modifiers.shift = false;
        else if (keynum == 17) guac_keyboard.modifiers.ctrl  = false;
        else if (keynum == 18) guac_keyboard.modifiers.alt   = false;
        else
            stopRepeat();

        // Get corresponding character
        var lastKeyDownChar = keydownChar[keynum];

        // Clear character record
        keydownChar[keynum] = null;

        // Send release event
        return sendKeyReleased(lastKeyDownChar);

    };

    // When focus is lost, clear modifiers.
    element.onblur = function() {
        guac_keyboard.modifiers.alt = false;
        guac_keyboard.modifiers.ctrl = false;
        guac_keyboard.modifiers.shift = false;
    };

};
