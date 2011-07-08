
/*
 *  Guacamole - Clientless Remote Desktop
 *  Copyright (C) 2010  Michael Jumper
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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

	// Single key state/modifier buffer
	var modShift = 0;
	var modCtrl = 0;
	var modAlt = 0;

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
		if (modShift == 0) keysym = unshiftedKeySym[keyCode];
		else {
            keysym = shiftedKeySym[keyCode];
            if (keysym == null) keysym = unshiftedKeySym[keyCode];
        }

        return keysym;

    }


	// Sends a single keystroke over the network
	function sendKeyPressed(keysym) {
		if (keysym != null && guac_keyboard.onkeydown)
			guac_keyboard.onkeydown(keysym);
	}

	// Sends a single keystroke over the network
	function sendKeyReleased(keysym) {
		if (keysym != null && guac_keyboard.onkeyup)
			guac_keyboard.onkeyup(keysym);
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
		if (keynum == 16)
			modShift = 1;
		else if (keynum == 17)
			modCtrl = 1;
		else if (keynum == 18)
			modAlt = 1;

        var keysym = getKeySymFromKeyCode(keynum);
        if (keysym) {
            // Get keysyms and events from KEYDOWN
            keySymSource = KEYDOWN;
        }

        // If modifier keys are held down, and we have keyIdentifier
        else if ((modCtrl == 1 || modAlt == 1) && e.keyIdentifier) {

            // Get keysym from keyIdentifier
            keysym = getKeySymFromKeyIdentifier(modShift, e.keyIdentifier);

            // Get keysyms and events from KEYDOWN
            keySymSource = KEYDOWN;

        }

        else
            // Get keysyms and events from KEYPRESS
            keySymSource = KEYPRESS;

        keydownCode = keynum;

        // Ignore key if we don't need to use KEYPRESS.
        // Send key event here
        if (keySymSource == KEYDOWN) {

            if (keydownChar[keynum] != keysym) {

                // Send event
                keydownChar[keynum] = keysym;
                sendKeyPressed(keysym);

                // Clear old key repeat, if any.
                stopRepeat();

                // Start repeating (if not a modifier key) after a short delay
                if (keynum != 16 && keynum != 17 && keynum != 18)
                    repeatKeyTimeoutId = setTimeout(function() { startRepeat(keysym); }, 500);
            }

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
            sendKeyPressed(keysym);

            // Start repeating (if not a modifier key) after a short delay
            repeatKeyTimeoutId = setTimeout(function() { startRepeat(keysym); }, 500);
        }

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
		if (keynum == 16)
			modShift = 0;
		else if (keynum == 17)
			modCtrl = 0;
		else if (keynum == 18)
			modAlt = 0;
        else
            stopRepeat();

        // Get corresponding character
        var lastKeyDownChar = keydownChar[keynum];

        // Clear character record
        keydownChar[keynum] = null;

        // Send release event
        sendKeyReleased(lastKeyDownChar);

		return false;
	};

	// When focus is lost, clear modifiers.
	var docOnblur = element.onblur;
	element.onblur = function() {
		modAlt = 0;
		modCtrl = 0;
		modShift = 0;
		if (docOnblur != null) docOnblur();
	};

	guac_keyboard.onkeydown = null;
	guac_keyboard.onkeyup = null;

}
