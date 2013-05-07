
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
        8:   [0xFF08], // backspace
        9:   [0xFF09], // tab
        13:  [0xFF0D], // enter
        16:  [0xFFE1, 0xFFE1, 0xFFE2], // shift
        17:  [0xFFE3, 0xFFE3, 0xFFE4], // ctrl
        18:  [0xFFE9, 0xFFE9, 0xFFEA], // alt
        19:  [0xFF13], // pause/break
        20:  [0xFFE5], // caps lock
        27:  [0xFF1B], // escape
        32:  [0x0020], // space
        33:  [0xFF55], // page up
        34:  [0xFF56], // page down
        35:  [0xFF57], // end
        36:  [0xFF50], // home
        37:  [0xFF51], // left arrow
        38:  [0xFF52], // up arrow
        39:  [0xFF53], // right arrow
        40:  [0xFF54], // down arrow
        45:  [0xFF63], // insert
        46:  [0xFFFF], // delete
        91:  [0xFFEB], // left window key (super_l)
        92:  [0xFF67], // right window key (menu key?)
        93:  null,     // select key
        112: [0xFFBE], // f1
        113: [0xFFBF], // f2
        114: [0xFFC0], // f3
        115: [0xFFC1], // f4
        116: [0xFFC2], // f5
        117: [0xFFC3], // f6
        118: [0xFFC4], // f7
        119: [0xFFC5], // f8
        120: [0xFFC6], // f9
        121: [0xFFC7], // f10
        122: [0xFFC8], // f11
        123: [0xFFC9], // f12
        144: [0xFF7F], // num lock
        145: [0xFF14]  // scroll lock
    };

    /**
     * Map of known JavaScript keyidentifiers which do not map to typable
     * characters to their unshifted X11 keysym equivalents.
     * @private
     */
    var keyidentifier_keysym = {
        "AllCandidates": [0xFF3D],
        "Alphanumeric": [0xFF30],
        "Alt": [0xFFE9, 0xFFE9, 0xFFEA],
        "Attn": [0xFD0E],
        "AltGraph": [0xFFEA],
        "CapsLock": [0xFFE5],
        "Clear": [0xFF0B],
        "Convert": [0xFF21],
        "Copy": [0xFD15],
        "Crsel": [0xFD1C],
        "CodeInput": [0xFF37],
        "Control": [0xFFE3, 0xFFE3, 0xFFE4],
        "Down": [0xFF54],
        "End": [0xFF57],
        "Enter": [0xFF0D],
        "EraseEof": [0xFD06],
        "Execute": [0xFF62],
        "Exsel": [0xFD1D],
        "F1": [0xFFBE],
        "F2": [0xFFBF],
        "F3": [0xFFC0],
        "F4": [0xFFC1],
        "F5": [0xFFC2],
        "F6": [0xFFC3],
        "F7": [0xFFC4],
        "F8": [0xFFC5],
        "F9": [0xFFC6],
        "F10": [0xFFC7],
        "F11": [0xFFC8],
        "F12": [0xFFC9],
        "F13": [0xFFCA],
        "F14": [0xFFCB],
        "F15": [0xFFCC],
        "F16": [0xFFCD],
        "F17": [0xFFCE],
        "F18": [0xFFCF],
        "F19": [0xFFD0],
        "F20": [0xFFD1],
        "F21": [0xFFD2],
        "F22": [0xFFD3],
        "F23": [0xFFD4],
        "F24": [0xFFD5],
        "Find": [0xFF68],
        "FullWidth": null,
        "HalfWidth": null,
        "HangulMode": [0xFF31],
        "HanjaMode": [0xFF34],
        "Help": [0xFF6A],
        "Hiragana": [0xFF25],
        "Home": [0xFF50],
        "Insert": [0xFF63],
        "JapaneseHiragana": [0xFF25],
        "JapaneseKatakana": [0xFF26],
        "JapaneseRomaji": [0xFF24],
        "JunjaMode": [0xFF38],
        "KanaMode": [0xFF2D],
        "KanjiMode": [0xFF21],
        "Katakana": [0xFF26],
        "Left": [0xFF51],
        "Meta": [0xFFE7],
        "NumLock": [0xFF7F],
        "PageDown": [0xFF55],
        "PageUp": [0xFF56],
        "Pause": [0xFF13],
        "PreviousCandidate": [0xFF3E],
        "PrintScreen": [0xFD1D],
        "Right": [0xFF53],
        "RomanCharacters": null,
        "Scroll": [0xFF14],
        "Select": [0xFF60],
        "Shift": [0xFFE1, 0xFFE1, 0xFFE2],
        "Up": [0xFF52],
        "Undo": [0xFF65],
        "Win": [0xFFEB]
    };

    /**
     * Map of known JavaScript keycodes which do not map to typable characters
     * to their shifted X11 keysym equivalents. Keycodes must only be listed
     * here if their shifted X11 keysym equivalents differ from their unshifted
     * equivalents.
     * @private
     */
    var shiftedKeysym = {
        18:  [0xFFE7, 0xFFE7, 0xFFEA]  // alt
    };

    /**
     * All keysyms which should not repeat when held down.
     * @private
     */
    var no_repeat = {
        0xFFE1: true, // Left shift
        0xFFE2: true, // Right shift
        0xFFE3: true, // Left ctrl 
        0xFFE4: true, // Right ctrl 
        0xFFE9: true, // Left alt
        0xFFEA: true  // Right alt (or AltGr)
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
        "alt"  : false,
        
        /**
         * Whether meta (apple key) is currently pressed.
         */
        "meta" : false

    };

    /**
     * The state of every key, indexed by keysym. If a particular key is
     * pressed, the value of pressed for that keysym will be true. If a key
     * is not currently pressed, it will not be defined. 
     */
    this.pressed = {};

    /**
     * The keysym associated with a given keycode when keydown fired.
     * @private
     */
    var keydownChar = [];

    /**
     * Timeout before key repeat starts.
     * @private
     */
    var key_repeat_timeout = null;

    /**
     * Interval which presses and releases the last key pressed while that
     * key is still being held down.
     * @private
     */
    var key_repeat_interval = null;

    /**
     * Given an array of keysyms indexed by location, returns the keysym
     * for the given location, or the keysym for the standard location if
     * undefined.
     * 
     * @param {Array} keysyms An array of keysyms, where the index of the
     *                        keysym in the array is the location value.
     * @param {Number} location The location on the keyboard corresponding to
     *                          the key pressed, as defined at:
     *                          http://www.w3.org/TR/DOM-Level-3-Events/#events-KeyboardEvent
     */
    function get_keysym(keysyms, location) {

        if (!keysyms)
            return null;

        return keysyms[location] || keysyms[0];
    }

    function keysym_from_key_identifier(shifted, keyIdentifier, location) {

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

        return get_keysym(keyidentifier_keysym[keyIdentifier], location);

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

    function keysym_from_keycode(keyCode, location) {

        var keysyms;

        // If not shifted, just return unshifted keysym
        if (!guac_keyboard.modifiers.shift)
            keysyms = unshiftedKeysym[keyCode];

        // Otherwise, return shifted keysym, if defined
        else
            keysyms = shiftedKeysym[keyCode] || unshiftedKeysym[keyCode];

        return get_keysym(keysyms, location);

    }


    /**
     * Marks a key as pressed, firing the keydown event if registered. Key
     * repeat for the pressed key will start after a delay if that key is
     * not a modifier.
     * @private
     */
    function press_key(keysym) {

        // Don't bother with pressing the key if the key is unknown
        if (keysym == null) return;

        // Only press if released
        if (!guac_keyboard.pressed[keysym]) {

            // Mark key as pressed
            guac_keyboard.pressed[keysym] = true;

            // Send key event
            if (guac_keyboard.onkeydown) {
                guac_keyboard.onkeydown(keysym);

                // Stop any current repeat
                window.clearTimeout(key_repeat_timeout);
                window.clearInterval(key_repeat_interval);

                // Repeat after a delay as long as pressed
                if (!no_repeat[keysym])
                    key_repeat_timeout = window.setTimeout(function() {
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
     * @private
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

    /**
     * Given a keyboard event, updates the local modifier state and remote
     * key state based on the modifier flags within the event. This function
     * pays no attention to keycodes.
     * 
     * @param {KeyboardEvent} e The keyboard event containing the flags to update.
     */
    function update_modifier_state(e) {

        // Release alt if implicitly released
        if (guac_keyboard.modifiers.alt && e.altKey === false) {
            release_key(0xFFE9); // Left alt
            release_key(0xFFEA); // Right alt (or AltGr)
            guac_keyboard.modifiers.alt = false;
        }

        // Release shift if implicitly released
        if (guac_keyboard.modifiers.shift && e.shiftKey === false) {
            release_key(0xFFE1); // Left shift
            release_key(0xFFE2); // Right shift
            guac_keyboard.modifiers.shift = false;
        }

        // Release ctrl if implicitly released
        if (guac_keyboard.modifiers.ctrl && e.ctrlKey === false) {
            release_key(0xFFE3); // Left ctrl 
            release_key(0xFFE4); // Right ctrl 
            guac_keyboard.modifiers.ctrl = false;
        }

    }

    // When key pressed
    element.addEventListener("keydown", function(e) {

        // Only intercept if handler set
        if (!guac_keyboard.onkeydown) return;

        var keynum;
        if (window.event) keynum = window.event.keyCode;
        else if (e.which) keynum = e.which;

        // Get key location
        var location = e.location || e.keyLocation || 0;

        // Ignore any unknown key events
        if (keynum == 0 && !e.keyIdentifier) {
            e.preventDefault();
            return;
        }

        // Fix modifier states
        update_modifier_state(e);

        // Ctrl/Alt/Shift/Meta
        if      (keynum == 16) guac_keyboard.modifiers.shift = true;
        else if (keynum == 17) guac_keyboard.modifiers.ctrl  = true;
        else if (keynum == 18) guac_keyboard.modifiers.alt   = true;
        else if (keynum == 91) guac_keyboard.modifiers.meta  = true;

        // Try to get keysym from keycode
        var keysym = keysym_from_keycode(keynum, location);

        // By default, we expect a corresponding keypress event
        var expect_keypress = true;

        // If key is known from keycode, prevent default
        if (keysym)
            expect_keypress = false;
        
        // Also try to get get keysym from keyIdentifier
        if (e.keyIdentifier) {

            keysym = keysym ||
            keysym_from_key_identifier(guac_keyboard.modifiers.shift,
                e.keyIdentifier, location);

            // Prevent default if non-typable character or if modifier combination
            // likely to be eaten by browser otherwise (NOTE: We must not prevent
            // default for Ctrl+Alt, as that combination is commonly used for
            // AltGr. If we receive AltGr, we need to handle keypress, which
            // means we cannot cancel keydown).
            if (!isTypable(e.keyIdentifier)
                || ( guac_keyboard.modifiers.ctrl && !guac_keyboard.modifiers.alt)
                || (!guac_keyboard.modifiers.ctrl &&  guac_keyboard.modifiers.alt)
                || (guac_keyboard.modifiers.meta))
                expect_keypress = false;
            
        }

        // If we do not expect to handle via keypress, handle now
        if (!expect_keypress) {
            e.preventDefault();

            // Press key if known
            if (keysym != null) {
                keydownChar[keynum] = keysym;
                press_key(keysym);
                
                // If a key is pressed while meta is held down, the keyup will never be sent in Chrome, so send it now. (bug #108404)
                if(guac_keyboard.modifiers.meta) {
                    release_key(keysym);
                }
            }
            
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

        // Fix modifier states
        update_modifier_state(e);

        // If event identified as a typable character, and we're holding Ctrl+Alt,
        // assume Ctrl+Alt is actually AltGr, and release both.
        if (!isControlCharacter(keynum) && guac_keyboard.modifiers.ctrl && guac_keyboard.modifiers.alt) {
            release_key(0xFFE3); // Left ctrl
            release_key(0xFFE4); // Right ctrl
            release_key(0xFFE9); // Left alt
            release_key(0xFFEA); // Right alt
        }

        // Send press + release if keysym known
        if (keysym != null) {
            press_key(keysym);
            release_key(keysym);
        }

    }, true);

    // When key released
    element.addEventListener("keyup", function(e) {

        // Only intercept if handler set
        if (!guac_keyboard.onkeyup) return;

        e.preventDefault();

        var keynum;
        if (window.event) keynum = window.event.keyCode;
        else if (e.which) keynum = e.which;
        
        // Fix modifier states
        update_modifier_state(e);

        // Ctrl/Alt/Shift/Meta
        if      (keynum == 16) guac_keyboard.modifiers.shift = false;
        else if (keynum == 17) guac_keyboard.modifiers.ctrl  = false;
        else if (keynum == 18) guac_keyboard.modifiers.alt   = false;
        else if (keynum == 91) guac_keyboard.modifiers.meta  = false;

        // Send release event if original key known
        var keydown_keysym = keydownChar[keynum];
        if (keydown_keysym != null)
            release_key(keydown_keysym);

        // Clear character record
        keydownChar[keynum] = null;

    }, true);

};
