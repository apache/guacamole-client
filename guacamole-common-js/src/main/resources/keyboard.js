
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

/**
 * Namespace for all Guacamole JavaScript objects.
 * @namespace
 */
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
     */
    this.onkeydown = null;

    /**
     * Fired whenever the user releases a key with the element associated
     * with this Guacamole.Keyboard in focus.
     * 
     * @event
     * @param {Number} keysym The keysym of the key being released.
     */
    this.onkeyup = null;

    /**
     * Map of known JavaScript keycodes which do not map to typable characters
     * to their unshifted X11 keysym equivalents.
     * @private
     */
    var unshiftedKeysym = {
        8:   0xFF08, // backspace
        9:   0xFF09, // tab
        13:  0xFF0D, // enter
        16:  0xFFE1, // shift
        17:  0xFFE3, // ctrl
        18:  0xFFE9, // alt
        19:  0xFF13, // pause/break
        20:  0xFFE5, // caps lock
        27:  0xFF1B, // escape
        32:  0x0020, // space
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
     * Map of known JavaScript keyidentifiers which do not map to typable
     * characters to their unshifted X11 keysym equivalents.
     * @private
     */
    var keyidentifier_keysym = {
        "AllCandidates": 0xFF3D,
        "Alphanumeric": 0xFF30,
        "Alt": 0xFFE9,
        "Attn": 0xFD0E,
        "AltGraph": 0xFFEA,
        "CapsLock": 0xFFE5,
        "Clear": 0xFF0B,
        "Convert": 0xFF21,
        "Copy": 0xFD15,
        "Crsel": 0xFD1C,
        "CodeInput": 0xFF37,
        "Control": 0xFFE3,
        "Down": 0xFF54,
        "End": 0xFF57,
        "Enter": 0xFF0D,
        "EraseEof": 0xFD06,
        "Execute": 0xFF62,
        "Exsel": 0xFD1D,
        "F1": 0xFFBE,
        "F2": 0xFFBF,
        "F3": 0xFFC0,
        "F4": 0xFFC1,
        "F5": 0xFFC2,
        "F6": 0xFFC3,
        "F7": 0xFFC4,
        "F8": 0xFFC5,
        "F9": 0xFFC6,
        "F10": 0xFFC7,
        "F11": 0xFFC8,
        "F12": 0xFFC9,
        "F13": 0xFFCA,
        "F14": 0xFFCB,
        "F15": 0xFFCC,
        "F16": 0xFFCD,
        "F17": 0xFFCE,
        "F18": 0xFFCF,
        "F19": 0xFFD0,
        "F20": 0xFFD1,
        "F21": 0xFFD2,
        "F22": 0xFFD3,
        "F23": 0xFFD4,
        "F24": 0xFFD5,
        "Find": 0xFF68,
        "FullWidth": null,
        "HalfWidth": null,
        "HangulMode": 0xFF31,
        "HanjaMode": 0xFF34,
        "Help": 0xFF6A,
        "Hiragana": 0xFF25,
        "Home": 0xFF50,
        "Insert": 0xFF63,
        "JapaneseHiragana": 0xFF25,
        "JapaneseKatakana": 0xFF26,
        "JapaneseRomaji": 0xFF24,
        "JunjaMode": 0xFF38,
        "KanaMode": 0xFF2D,
        "KanjiMode": 0xFF21,
        "Katakana": 0xFF26,
        "Left": 0xFF51,
        "Meta": 0xFFE7,
        "NumLock": 0xFF7F,
        "PageDown": 0xFF55,
        "PageUp": 0xFF56,
        "Pause": 0xFF13,
        "PreviousCandidate": 0xFF3E,
        "PrintScreen": 0xFD1D,
        "Right": 0xFF53,
        "RomanCharacters": null,
        "Scroll": 0xFF14,
        "Select": 0xFF60,
        "Shift": 0xFFE1,
        "Up": 0xFF52,
        "Undo": 0xFF65,
        "Win": 0xFFEB
    };

    /**
     * Map of known JavaScript keycodes which do not map to typable characters
     * to their shifted X11 keysym equivalents. Keycodes must only be listed
     * here if their shifted X11 keysym equivalents differ from their unshifted
     * equivalents.
     * @private
     */
    var shiftedKeysym = {
        18:  0xFFE7  // alt
    };

    /**
     * All keysyms which should not repeat when held down.
     */
    var no_repeat = {
        0xFFE1: true,
        0xFFE3: true,
        0xFFE9: true
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
     * is not currently pressed, it will not be defined. 
     */
    this.pressed = {};

    /**
     * The keysym associated with a given keycode when keydown fired.
     */
    var keydownChar = [];

    /**
     * Timeout before key repeat starts.
     */
    var key_repeat_timeout = null;

    /**
     * Interval which presses and releases the last key pressed while that
     * key is still being held down.
     */
    var key_repeat_interval = null;

    function keysym_from_key_identifier(shifted, keyIdentifier) {

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

            return keysym_from_charcode(codepoint);

        }

        return keyidentifier_keysym[keyIdentifier];

    }

    function isControlCharacter(codepoint) {
        return codepoint <= 0x1F || (codepoint >= 0x7F && codepoint <= 0x9F);
    }

    function keysym_from_charcode(codepoint) {

        // Keysyms for control characters
        if (isControlCharacter(codepoint)) return 0xFF00 | codepoint;

        // Keysyms for ASCII chars
        if (codepoint >= 0x0000 && codepoint <= 0x00FF)
            return codepoint;

        // Keysyms for Unicode
        if (codepoint >= 0x0100 && codepoint <= 0x10FFFF)
            return 0x01000000 | codepoint;

        return null;

    }

    function keysyom_from_keycode(keyCode) {

        var keysym = null;
        if (!guac_keyboard.modifiers.shift) keysym = unshiftedKeysym[keyCode];
        else {
            keysym = shiftedKeysym[keyCode];
            if (keysym == null) keysym = unshiftedKeysym[keyCode];
        }

        return keysym;

    }


    /**
     * Marks a key as pressed, firing the keydown event if registered. Key
     * repeat for the pressed key will start after a delay if that key is
     * not a modifier.
     */
    function press_key(keysym) {

        // Only press if released
        if (!guac_keyboard.pressed[keysym]) {

            // Mark key as pressed
            guac_keyboard.pressed[keysym] = true;

            // Send key event
            if (keysym != null && guac_keyboard.onkeydown) {
                guac_keyboard.onkeydown(keysym);

                // Stop any current repeat
                window.clearTimeout(key_repeat_timeout);
                window.clearInterval(key_repeat_interval);

                // Repeat after a delay as long as pressed
                if (!no_repeat[keysym])
                    key_repeat_timeout = setTimeout(function() {
                        key_repeat_interval = window.setInterval(function() {
                            guac_keyboard.onkeyup(keysym);
                            guac_keyboard.onkeydown(keysym);
                        }, 50);
                    }, 500);


            }
        }

    }

    /**
     * Marks a key as released, firing the keyup event if registered.
     */
    function release_key(keysym) {

        // Only release if pressed
        if (guac_keyboard.pressed[keysym]) {
            
            // Mark key as released
            delete guac_keyboard.pressed[keysym];

            // Stop repeat
            window.clearTimeout(key_repeat_timeout);
            window.clearInterval(key_repeat_interval);

            // Send key event
            if (keysym != null && guac_keyboard.onkeyup)
                guac_keyboard.onkeyup(keysym);

        }

    }

    function isTypable(keyIdentifier) {

        // Find unicode prefix
        var unicodePrefixLocation = keyIdentifier.indexOf("U+");
        if (unicodePrefixLocation == -1)
            return false;

        // Parse codepoint value
        var hex = keyIdentifier.substring(unicodePrefixLocation+2);
        var codepoint = parseInt(hex, 16);

        // If control character, not typable
        if (isControlCharacter(codepoint)) return false;

        // Otherwise, typable
        return true;

    }

    // When key pressed
    element.addEventListener("keydown", function(e) {

        // Only intercept if handler set
        if (!guac_keyboard.onkeydown) return;

        var keynum;
        if (window.event) keynum = window.event.keyCode;
        else if (e.which) keynum = e.which;

        // Ignore any unknown key events
        if (keynum == 0 && !e.keyIdentifier) {
            e.preventDefault();
            return;
        }

        // Ctrl/Alt/Shift
        if      (keynum == 16) guac_keyboard.modifiers.shift = true;
        else if (keynum == 17) guac_keyboard.modifiers.ctrl  = true;
        else if (keynum == 18) guac_keyboard.modifiers.alt   = true;

        // Try to get keysym from keycode
        var keysym = keysyom_from_keycode(keynum);

        // By default, we expect a corresponding keypress event
        var expect_keypress = true;

        // If key is known from keycode, prevent default
        if (keysym)
            expect_keypress = false;
        
        // Also try to get get keysym from keyIdentifier
        if (e.keyIdentifier) {

            keysym = keysym ||
                keysym_from_key_identifier(guac_keyboard.modifiers.shift, e.keyIdentifier);

            // Prevent default if non-typable character or if modifier combination
            // likely to be eaten by browser otherwise (NOTE: We must not prevent
            // default for Ctrl+Alt, as that combination is commonly used for
            // AltGr. If we receive AltGr, we need to handle keypress, which
            // means we cannot cancel keydown).
            if (!isTypable(e.keyIdentifier)
                    || ( guac_keyboard.modifiers.ctrl && !guac_keyboard.modifiers.alt)
                    || (!guac_keyboard.modifiers.ctrl &&  guac_keyboard.modifiers.alt))
                expect_keypress = false;
            
        }

        // If we do not expect to handle via keypress, handle now
        if (!expect_keypress) {
            e.preventDefault();
            keydownChar[keynum] = keysym;
            press_key(keysym);
        }

    }, true);

    // When key pressed
    element.addEventListener("keypress", function(e) {

        // Only intercept if handler set
        if (!guac_keyboard.onkeydown && !guac_keyboard.onkeyup) return;

        e.preventDefault();

        var keynum;
        if (window.event) keynum = window.event.keyCode;
        else if (e.which) keynum = e.which;

        var keysym = keysym_from_charcode(keynum);

        // If event identified as a typable character, and we're holding Ctrl+Alt,
        // assume Ctrl+Alt is actually AltGr, and release both.
        if (!isControlCharacter(keynum) && guac_keyboard.modifiers.ctrl && guac_keyboard.modifiers.alt) {
            release_key(0xFFE3);
            release_key(0xFFE9);
        }

        // Send press + release
        press_key(keysym);
        release_key(keysym);

    }, true);

    // When key released
    element.addEventListener("keyup", function(e) {

        // Only intercept if handler set
        if (!guac_keyboard.onkeyup) return;

        e.preventDefault();

        var keynum;
        if (window.event) keynum = window.event.keyCode;
        else if (e.which) keynum = e.which;
        
        // Ctrl/Alt/Shift
        if      (keynum == 16) guac_keyboard.modifiers.shift = false;
        else if (keynum == 17) guac_keyboard.modifiers.ctrl  = false;
        else if (keynum == 18) guac_keyboard.modifiers.alt   = false;

        // Send release event
        release_key(keydownChar[keynum]);

        // Clear character record
        keydownChar[keynum] = null;

    }, true);

    // When focus is lost, clear modifiers.
    element.addEventListener("blur", function() {
        guac_keyboard.modifiers.alt   = false;
        guac_keyboard.modifiers.ctrl  = false;
        guac_keyboard.modifiers.shift = false;
    }, true);

};
