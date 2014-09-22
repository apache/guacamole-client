/*
 * Copyright (C) 2013 Glyptodon LLC
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
     * @return {Boolean} true if the key event should be allowed through to the
     *                   browser, false otherwise.
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
     * A key event having a corresponding timestamp. This event is non-specific.
     * Its subclasses should be used instead when recording specific key
     * events.
     *
     * @private
     * @constructor
     */
    var KeyEvent = function() {

        /**
         * Reference to this key event.
         */
        var key_event = this;

        /**
         * An arbitrary timestamp in milliseconds, indicating this event's
         * position in time relative to other events.
         */
        this.timestamp = new Date().getTime();

        /**
         * Returns the number of milliseconds elapsed since this event was
         * received.
         *
         * @return {Number} The number of milliseconds elapsed since this
         *                  event was received.
         */
        this.getAge = function() {
            return new Date().getTime() - key_event.timestamp;
        };

    };

    /**
     * Information related to the pressing of a key, which need not be a key
     * associated with a printable character. The presence or absence of any
     * information within this object is browser-dependent.
     *
     * @private
     * @constructor
     * @augments Guacamole.Keyboard.KeyEvent
     * @param {Number} keyCode The JavaScript key code of the key pressed.
     * @param {String} keyIdentifier The legacy DOM3 "keyIdentifier" of the key
     *                               pressed, as defined at:
     *                               http://www.w3.org/TR/2009/WD-DOM-Level-3-Events-20090908/#events-Events-KeyboardEvent
     * @param {String} key The standard name of the key pressed, as defined at:
     *                     http://www.w3.org/TR/DOM-Level-3-Events/#events-KeyboardEvent
     * @param {Number} location The location on the keyboard corresponding to
     *                          the key pressed, as defined at:
     *                          http://www.w3.org/TR/DOM-Level-3-Events/#events-KeyboardEvent
     */
    var KeydownEvent = function(keyCode, keyIdentifier, key, location) {

        // We extend KeyEvent
        KeyEvent.apply(this);

        /**
         * The JavaScript key code of the key pressed.
         *
         * @type Number
         */
        this.keyCode = keyCode;

        /**
         * The legacy DOM3 "keyIdentifier" of the key pressed, as defined at:
         * http://www.w3.org/TR/2009/WD-DOM-Level-3-Events-20090908/#events-Events-KeyboardEvent
         *
         * @type String
         */
        this.keyIdentifier = keyIdentifier;

        /**
         * The standard name of the key pressed, as defined at:
         * http://www.w3.org/TR/DOM-Level-3-Events/#events-KeyboardEvent
         * 
         * @type String
         */
        this.key = key;

        /**
         * The location on the keyboard corresponding to the key pressed, as
         * defined at:
         * http://www.w3.org/TR/DOM-Level-3-Events/#events-KeyboardEvent
         * 
         * @type Number
         */
        this.location = location;

    };

    KeydownEvent.prototype = new KeyEvent();

    /**
     * Information related to the pressing of a key, which MUST be
     * associated with a printable character. The presence or absence of any
     * information within this object is browser-dependent.
     *
     * @private
     * @constructor
     * @augments Guacamole.Keyboard.KeyEvent
     * @param {Number} charCode The Unicode codepoint of the character that
     *                          would be typed by the key pressed.
     */
    var KeypressEvent = function(charCode) {

        // We extend KeyEvent
        KeyEvent.apply(this);

        /**
         * The Unicode codepoint of the character that would be typed by the
         * key pressed.
         *
         * @type Number
         */
        this.charCode = charCode;

    };

    KeypressEvent.prototype = new KeyEvent();

    /**
     * Information related to the pressing of a key, which need not be a key
     * associated with a printable character. The presence or absence of any
     * information within this object is browser-dependent.
     *
     * @private
     * @constructor
     * @augments Guacamole.Keyboard.KeyEvent
     * @param {Number} keyCode The JavaScript key code of the key released.
     * @param {String} keyIdentifier The legacy DOM3 "keyIdentifier" of the key
     *                               released, as defined at:
     *                               http://www.w3.org/TR/2009/WD-DOM-Level-3-Events-20090908/#events-Events-KeyboardEvent
     * @param {String} key The standard name of the key released, as defined at:
     *                     http://www.w3.org/TR/DOM-Level-3-Events/#events-KeyboardEvent
     * @param {Number} location The location on the keyboard corresponding to
     *                          the key released, as defined at:
     *                          http://www.w3.org/TR/DOM-Level-3-Events/#events-KeyboardEvent
     */
    var KeyupEvent = function(keyCode, keyIdentifier, key, location) {

        // We extend KeyEvent
        KeyEvent.apply(this);

        /**
         * The JavaScript key code of the key released.
         *
         * @type Number
         */
        this.keyCode = keyCode;

        /**
         * The legacy DOM3 "keyIdentifier" of the key released, as defined at:
         * http://www.w3.org/TR/2009/WD-DOM-Level-3-Events-20090908/#events-Events-KeyboardEvent
         *
         * @type String
         */
        this.keyIdentifier = keyIdentifier;

        /**
         * The standard name of the key released, as defined at:
         * http://www.w3.org/TR/DOM-Level-3-Events/#events-KeyboardEvent
         * 
         * @type String
         */
        this.key = key;

        /**
         * The location on the keyboard corresponding to the key released, as
         * defined at:
         * http://www.w3.org/TR/DOM-Level-3-Events/#events-KeyboardEvent
         * 
         * @type Number
         */
        this.location = location;

    };

    KeyupEvent.prototype = new KeyEvent();

    /**
     * An array of recorded events, which can be instances of the private
     * KeydownEvent, KeypressEvent, and KeyupEvent classes.
     *
     * @type (KeydownEvent|KeypressEvent|KeyupEvent)[]
     */
    var eventLog = [];

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
        91:  [0xFFEB], // left window key (hyper_l)
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
        145: [0xFF14], // scroll lock
        225: [0xFE03]  // altgraph (iso_level3_shift)
    };

    /**
     * Map of known JavaScript keyidentifiers which do not map to typable
     * characters to their unshifted X11 keysym equivalents.
     * @private
     */
    var keyidentifier_keysym = {
        "Again": [0xFF66],
        "AllCandidates": [0xFF3D],
        "Alphanumeric": [0xFF30],
        "Alt": [0xFFE9, 0xFFE9, 0xFFEA],
        "Attn": [0xFD0E],
        "AltGraph": [0xFE03],
        "ArrowDown": [0xFF54],
        "ArrowLeft": [0xFF51],
        "ArrowRight": [0xFF53],
        "ArrowUp": [0xFF52],
        "Backspace": [0xFF08],
        "CapsLock": [0xFFE5],
        "Cancel": [0xFF69],
        "Clear": [0xFF0B],
        "Convert": [0xFF21],
        "Copy": [0xFD15],
        "Crsel": [0xFD1C],
        "CrSel": [0xFD1C],
        "CodeInput": [0xFF37],
        "Compose": [0xFF20],
        "Control": [0xFFE3, 0xFFE3, 0xFFE4],
        "ContextMenu": [0xFF67],
        "DeadGrave": [0xFE50],
        "DeadAcute": [0xFE51],
        "DeadCircumflex": [0xFE52],
        "DeadTilde": [0xFE53],
        "DeadMacron": [0xFE54],
        "DeadBreve": [0xFE55],
        "DeadAboveDot": [0xFE56],
        "DeadUmlaut": [0xFE57],
        "DeadAboveRing": [0xFE58],
        "DeadDoubleacute": [0xFE59],
        "DeadCaron": [0xFE5A],
        "DeadCedilla": [0xFE5B],
        "DeadOgonek": [0xFE5C],
        "DeadIota": [0xFE5D],
        "DeadVoicedSound": [0xFE5E],
        "DeadSemivoicedSound": [0xFE5F],
        "Delete": [0xFFFF],
        "Down": [0xFF54],
        "End": [0xFF57],
        "Enter": [0xFF0D],
        "EraseEof": [0xFD06],
        "Escape": [0xFF1B],
        "Execute": [0xFF62],
        "Exsel": [0xFD1D],
        "ExSel": [0xFD1D],
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
        "GroupFirst": [0xFE0C],
        "GroupLast": [0xFE0E],
        "GroupNext": [0xFE08],
        "GroupPrevious": [0xFE0A],
        "FullWidth": null,
        "HalfWidth": null,
        "HangulMode": [0xFF31],
        "Hankaku": [0xFF29],
        "HanjaMode": [0xFF34],
        "Help": [0xFF6A],
        "Hiragana": [0xFF25],
        "HiraganaKatakana": [0xFF27],
        "Home": [0xFF50],
        "Hyper": [0xFFED, 0xFFED, 0xFFEE],
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
        "ModeChange": [0xFF7E],
        "NumLock": [0xFF7F],
        "PageDown": [0xFF55],
        "PageUp": [0xFF56],
        "Pause": [0xFF13],
        "Play": [0xFD16],
        "PreviousCandidate": [0xFF3E],
        "PrintScreen": [0xFD1D],
        "Redo": [0xFF66],
        "Right": [0xFF53],
        "RomanCharacters": null,
        "Scroll": [0xFF14],
        "Select": [0xFF60],
        "Separator": [0xFFAC],
        "Shift": [0xFFE1, 0xFFE1, 0xFFE2],
        "SingleCandidate": [0xFF3C],
        "Super": [0xFFEB, 0xFFEB, 0xFFEC],
        "Tab": [0xFF09],
        "Up": [0xFF52],
        "Undo": [0xFF65],
        "Win": [0xFFEB],
        "Zenkaku": [0xFF28],
        "ZenkakuHankaku": [0xFF2A]
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
        0xFE03: true, // ISO Level 3 Shift (AltGr)
        0xFFE1: true, // Left shift
        0xFFE2: true, // Right shift
        0xFFE3: true, // Left ctrl 
        0xFFE4: true, // Right ctrl 
        0xFFE7: true, // Left meta 
        0xFFE8: true, // Right meta 
        0xFFE9: true, // Left alt
        0xFFEA: true, // Right alt
        0xFFEB: true, // Left hyper
        0xFFEC: true  // Right hyper
    };

    /**
     * All modifiers and their states.
     */
    this.modifiers = new Guacamole.Keyboard.ModifierState();
        
    /**
     * The state of every key, indexed by keysym. If a particular key is
     * pressed, the value of pressed for that keysym will be true. If a key
     * is not currently pressed, it will not be defined. 
     */
    this.pressed = {};

    /**
     * The last result of calling the onkeydown handler for each key, indexed
     * by keysym. This is used to prevent/allow default actions for key events,
     * even when the onkeydown handler cannot be called again because the key
     * is (theoretically) still pressed.
     */
    var last_keydown_result = {};

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

    function keysym_from_key_identifier(identifier, location) {

        if (!identifier)
            return null;

        var typedCharacter;

        // If identifier is U+xxxx, decode Unicode character 
        var unicodePrefixLocation = identifier.indexOf("U+");
        if (unicodePrefixLocation >= 0) {
            var hex = identifier.substring(unicodePrefixLocation+2);
            typedCharacter = String.fromCharCode(parseInt(hex, 16));
        }

        // If single character, use that as typed character
        else if (identifier.length === 1)
            typedCharacter = identifier;

        // Otherwise, look up corresponding keysym
        else
            return get_keysym(keyidentifier_keysym[identifier], location);

        // Get codepoint
        var codepoint = typedCharacter.charCodeAt(0);
        return keysym_from_charcode(codepoint);

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
     * 
     * @private
     * @param keysym The keysym of the key to press.
     * @return {Boolean} true if event should NOT be canceled, false otherwise.
     */
    function press_key(keysym) {

        // Don't bother with pressing the key if the key is unknown
        if (keysym === null) return;

        // Only press if released
        if (!guac_keyboard.pressed[keysym]) {

            // Mark key as pressed
            guac_keyboard.pressed[keysym] = true;

            // Send key event
            if (guac_keyboard.onkeydown) {
                var result = guac_keyboard.onkeydown(keysym);
                last_keydown_result[keysym] = result;

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

                return result;
            }
        }

        // Return the last keydown result by default, resort to false if unknown
        return last_keydown_result[keysym] || false;

    }

    /**
     * Marks a key as released, firing the keyup event if registered.
     * 
     * @private
     * @param keysym The keysym of the key to release.
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
            if (keysym !== null && guac_keyboard.onkeyup)
                guac_keyboard.onkeyup(keysym);

        }

    }

    /**
     * Given a keyboard event, updates the local modifier state and remote
     * key state based on the modifier flags within the event. This function
     * pays no attention to keycodes.
     * 
     * @param {KeyboardEvent} e The keyboard event containing the flags to update.
     */
    function update_modifier_state(e) {

        // Get state
        var state = Guacamole.Keyboard.ModifierState.fromKeyboardEvent(e);

        // Release alt if implicitly released
        if (guac_keyboard.modifiers.alt && state.alt === false) {
            release_key(0xFFE9); // Left alt
            release_key(0xFFEA); // Right alt (or AltGr)
        }

        // Release shift if implicitly released
        if (guac_keyboard.modifiers.shift && state.shift === false) {
            release_key(0xFFE1); // Left shift
            release_key(0xFFE2); // Right shift
        }

        // Release ctrl if implicitly released
        if (guac_keyboard.modifiers.ctrl && state.ctrl === false) {
            release_key(0xFFE3); // Left ctrl 
            release_key(0xFFE4); // Right ctrl 
        }

        // Release meta if implicitly released
        if (guac_keyboard.modifiers.meta && state.meta === false) {
            release_key(0xFFE7); // Left meta 
            release_key(0xFFE8); // Right meta 
        }

        // Release hyper if implicitly released
        if (guac_keyboard.modifiers.hyper && state.hyper === false) {
            release_key(0xFFEB); // Left hyper
            release_key(0xFFEC); // Right hyper
        }

        // Update state
        guac_keyboard.modifiers = state;

    }

    /**
     * The reinterpretation timeout handle returned via window.setTimeout() when
     * future evaluation is needed, but the necessary event may not actually be
     * generated.
     */
    var reinterpret_timeout;

    /**
     * Schedules future reinterpretation of logged key events.
     */
    function schedule_reinterpret() {
        window.clearTimeout(reinterpret_timeout);
        reinterpret_timeout = window.setTimeout(interpret_events, 100);
    }

    /**
     * Reads through the event log, removing events from the head of the log
     * when the corresponding true key presses are known (or as known as they
     * can be).
     * 
     * @return {Boolean} Whether the default action of the latest event should
     *                   be prevented.
     */
    function interpret_events() {

        // Peek at first event in log
        var first = eventLog[0];
        if (!first)
            return false;

        // Keydown event
        if (first instanceof KeydownEvent) {

            var keysym;

            // If key is known from keyCode or DOM3 alone, use that
            keysym =  keysym_from_key_identifier(first.key, first.location)
                   || keysym_from_keycode(first.keyCode, first.location);
            if (keysym) {
                eventLog.shift();
                return !press_key(keysym);
            }

            // If keydown is immediately followed by a keypress, use the indicated character
            var next = eventLog[1];
            if (next && next instanceof KeypressEvent) {

                keysym = keysym_from_charcode(next.charCode);
                if (keysym) {
                    eventLog.shift();
                    return !press_key(keysym);
                }

                // If keypress cannot be identified, then drop
                eventLog.shift();

            }

            // Drop event if completely old and uninterpretable
            else if (first.getAge() > 100)
                eventLog.shift();

            // Lacking further information, pray for a future keypress event
            else
                schedule_reinterpret();

        }

        // Keyup event
        else if (first instanceof KeyupEvent) {

            // If key is known from keyCode or DOM3 alone, use that
            var keysym =  keysym_from_key_identifier(first.key, first.location)
                       || keysym_from_keycode(first.keyCode, first.location);
            if (keysym) {
                eventLog.shift();
                release_key(keysym);
                return true;
            }

            // Drop if keyup cannot be narrowed to a specific key
            eventLog.shift();

        }

        // Dump any other type of event (keypress by itself is invalid)
        else
            eventLog.shift();

        // Do not prevent anything by default
        return false;

    }

    // When key pressed
    element.addEventListener("keydown", function(e) {

        // Only intercept if handler set
        if (!guac_keyboard.onkeydown) return;

        var keyCode;
        if (window.event) keyCode = window.event.keyCode;
        else if (e.which) keyCode = e.which;

        // Fix modifier states
        update_modifier_state(e);

        // Log event
        var keydownEvent = new KeydownEvent(keyCode, e.keyIdentifier, e.key, e.location || e.keyLocation);
        eventLog.push(keydownEvent);

        // Interpret as many events as possible, prevent default if indicated
        if (interpret_events())
            e.preventDefault();

    }, true);

    // When key pressed
    element.addEventListener("keypress", function(e) {

        // Only intercept if handler set
        if (!guac_keyboard.onkeydown && !guac_keyboard.onkeyup) return;

        var charCode;
        if (window.event) charCode = window.event.keyCode;
        else if (e.which) charCode = e.which;

        // Fix modifier states
        update_modifier_state(e);

        // Log event
        var keypressEvent = new KeypressEvent(charCode);
        eventLog.push(keypressEvent);

        // Interpret as many events as possible, prevent default if indicated
        if (interpret_events())
            e.preventDefault();

    }, true);

    // When key released
    element.addEventListener("keyup", function(e) {

        // Only intercept if handler set
        if (!guac_keyboard.onkeyup) return;

        e.preventDefault();

        var keyCode;
        if (window.event) keyCode = window.event.keyCode;
        else if (e.which) keyCode = e.which;
        
        // Fix modifier states
        update_modifier_state(e);

        // Log event, call for interpretation
        var keyupEvent = new KeyupEvent(keyCode, e.keyIdentifier, e.key, e.location || e.keyLocation);
        eventLog.push(keyupEvent);
        interpret_events();

    }, true);

};

/**
 * The state of all supported keyboard modifiers.
 * @constructor
 */
Guacamole.Keyboard.ModifierState = function() {
    
    /**
     * Whether shift is currently pressed.
     * @type Boolean
     */
    this.shift = false;
    
    /**
     * Whether ctrl is currently pressed.
     * @type Boolean
     */
    this.ctrl = false;
    
    /**
     * Whether alt is currently pressed.
     * @type Boolean
     */
    this.alt = false;
    
    /**
     * Whether meta (apple key) is currently pressed.
     * @type Boolean
     */
    this.meta = false;

    /**
     * Whether hyper (windows key) is currently pressed.
     * @type Boolean
     */
    this.hyper = false;
    
};

/**
 * Returns the modifier state applicable to the keyboard event given.
 * 
 * @param {KeyboardEvent} e The keyboard event to read.
 * @returns {Guacamole.Keyboard.ModifierState} The current state of keyboard
 *                                             modifiers.
 */
Guacamole.Keyboard.ModifierState.fromKeyboardEvent = function(e) {
    
    var state = new Guacamole.Keyboard.ModifierState();

    // Assign states from old flags
    state.shift = e.shiftKey;
    state.ctrl  = e.ctrlKey;
    state.alt   = e.altKey;
    state.meta  = e.metaKey;

    // Use DOM3 getModifierState() for others
    if (e.getModifierState) {
        state.hyper = e.getModifierState("OS")
                   || e.getModifierState("Super")
                   || e.getModifierState("Hyper")
                   || e.getModifierState("Win");
    }

    return state;
    
};
