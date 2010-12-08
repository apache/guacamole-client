
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

function GuacamoleKeyboard(element) {

	/*****************************************/
	/*** Keyboard Handler                  ***/
	/*****************************************/

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
		if (keysym != null && keyPressedHandler)
			keyPressedHandler(keysym);
	}

	// Sends a single keystroke over the network
	function sendKeyReleased(keysym) {
		if (keysym != null)
			keyReleasedHandler(keysym);
	}


    var KEYDOWN = 1;
    var KEYPRESS = 2;

    var keySymSource = null;

	// When key pressed
    var keydownCode = null;
	element.onkeydown = function(e) {

        // Only intercept if handler set
        if (!keyPressedHandler) return true;

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

	};

	// When key pressed
    element.onkeypress = function(e) {

        // Only intercept if handler set
        if (!keyPressedHandler) return true;

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
        if (!keyReleasedHandler) return true;

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

	var keyPressedHandler = null;
	var keyReleasedHandler = null;

	this.setKeyPressedHandler = function(kh) { keyPressedHandler = kh; };
	this.setKeyReleasedHandler = function(kh) { keyReleasedHandler = kh; };

}
