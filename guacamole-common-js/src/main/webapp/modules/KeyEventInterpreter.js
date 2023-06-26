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

var Guacamole = Guacamole || {};

/**
 * An object that will accept raw key events and produce human readable text
 * batches, seperated by at least `batchSeperation` milliseconds, which can be
 * retrieved through the onbatch callback or by calling getCurrentBatch().
 *
 * NOTE: The event processing logic and output format is based on the `guaclog`
 * tool, with the addition of batching support.
 *
 * @constructor
 *
 * @param {number} [batchSeperation=5000]
 *     The minimum number of milliseconds that must elapse between subsequent
 *     batches of key-event-generated text. If 0 or negative, no splitting will
 *     occur, resulting in a single batch for all provided key events.
 *
 * @param {number} [startTimestamp=0]
 *     The starting timestamp for the recording being intepreted. If provided,
 *     the timestamp of each intepreted event will be relative to this timestamp.
 *     If not provided, the raw recording timestamp will be used.
 */
Guacamole.KeyEventInterpreter = function KeyEventInterpreter(batchSeperation, startTimestamp) {

    /**
     * Reference to this Guacamole.KeyEventInterpreter.
     *
     * @private
     * @type {!Guacamole.SessionRecording}
     */
    var interpreter = this;

    // Default to 5 seconds if the batch seperation was not provided
    if (batchSeperation === undefined || batchSeperation === null)
        batchSeperation = 5000;

    // Default to 0 seconds to keep the raw timestamps
    if (startTimestamp === undefined || startTimestamp === null)
        startTimestamp = 0;

    /**
     * A definition for a known key.
     *
     * @constructor
     * @private
     * @param {KEY_DEFINITION|object} [template={}]
     *     The object whose properties should be copied within the new
     *     KEY_DEFINITION.
     */
    var KeyDefinition = function KeyDefinition(template) {

        /**
         * The X11 keysym of the key.
         * @type {!number}
         */
        this.keysym = parseInt(template.keysym);

        /**
         * A human-readable name for the key.
         * @type {!String}
         */
        this.name = template.name;

        /**
         * The value which would be typed in a typical text editor, if any. If the
         * key is not associated with any typable value, or if the typable value is
         * not generally useful in an auditing context, this will be undefined.
         * @type {String}
         */
        this.value = template.value;

        /**
         * Whether this key is a modifier which may affect the interpretation of
         * other keys, and thus should be tracked as it is held down.
         * @type {!boolean}
         * @default false
         */
        this.modifier = template.modifier || false;

    };

    /**
     * A precursor array to the KNOWN_KEYS map. The objects contained within
     * will be constructed into full KeyDefinition objects.
     *
     * @constant
     * @private
     * @type {Object[]}
     */
    var _KNOWN_KEYS = [
        {keysym: 0xFE03, name: 'AltGr', value: "", modifier: true },
        {keysym: 0xFF08, name: 'Backspace' },
        {keysym: 0xFF09, name: 'Tab' },
        {keysym: 0xFF0B, name: 'Clear' },
        {keysym: 0xFF0D, name: 'Return', value: "\n" },
        {keysym: 0xFF13, name: 'Pause' },
        {keysym: 0xFF14, name: 'Scroll' },
        {keysym: 0xFF15, name: 'SysReq' },
        {keysym: 0xFF1B, name: 'Escape' },
        {keysym: 0xFF50, name: 'Home' },
        {keysym: 0xFF51, name: 'Left' },
        {keysym: 0xFF52, name: 'Up' },
        {keysym: 0xFF53, name: 'Right' },
        {keysym: 0xFF54, name: 'Down' },
        {keysym: 0xFF55, name: 'Page Up' },
        {keysym: 0xFF56, name: 'Page Down' },
        {keysym: 0xFF57, name: 'End' },
        {keysym: 0xFF63, name: 'Insert' },
        {keysym: 0xFF65, name: 'Undo' },
        {keysym: 0xFF6A, name: 'Help' },
        {keysym: 0xFF7F, name: 'Num' },
        {keysym: 0xFF80, name: 'Space', value: " " },
        {keysym: 0xFF8D, name: 'Enter', value: "\n" },
        {keysym: 0xFF95, name: 'Home' },
        {keysym: 0xFF96, name: 'Left' },
        {keysym: 0xFF97, name: 'Up' },
        {keysym: 0xFF98, name: 'Right' },
        {keysym: 0xFF99, name: 'Down' },
        {keysym: 0xFF9A, name: 'Page Up' },
        {keysym: 0xFF9B, name: 'Page Down' },
        {keysym: 0xFF9C, name: 'End' },
        {keysym: 0xFF9E, name: 'Insert' },
        {keysym: 0xFFAA, name: '*', value: "*" },
        {keysym: 0xFFAB, name: '+', value: "+" },
        {keysym: 0xFFAD, name: '-', value: "-" },
        {keysym: 0xFFAE, name: '.', value: "." },
        {keysym: 0xFFAF, name: '/', value: "/" },
        {keysym: 0xFFB0, name: '0', value: "0" },
        {keysym: 0xFFB1, name: '1', value: "1" },
        {keysym: 0xFFB2, name: '2', value: "2" },
        {keysym: 0xFFB3, name: '3', value: "3" },
        {keysym: 0xFFB4, name: '4', value: "4" },
        {keysym: 0xFFB5, name: '5', value: "5" },
        {keysym: 0xFFB6, name: '6', value: "6" },
        {keysym: 0xFFB7, name: '7', value: "7" },
        {keysym: 0xFFB8, name: '8', value: "8" },
        {keysym: 0xFFB9, name: '9', value: "9" },
        {keysym: 0xFFBE, name: 'F1' },
        {keysym: 0xFFBF, name: 'F2' },
        {keysym: 0xFFC0, name: 'F3' },
        {keysym: 0xFFC1, name: 'F4' },
        {keysym: 0xFFC2, name: 'F5' },
        {keysym: 0xFFC3, name: 'F6' },
        {keysym: 0xFFC4, name: 'F7' },
        {keysym: 0xFFC5, name: 'F8' },
        {keysym: 0xFFC6, name: 'F9' },
        {keysym: 0xFFC7, name: 'F10' },
        {keysym: 0xFFC8, name: 'F11' },
        {keysym: 0xFFC9, name: 'F12' },
        {keysym: 0xFFCA, name: 'F13' },
        {keysym: 0xFFCB, name: 'F14' },
        {keysym: 0xFFCC, name: 'F15' },
        {keysym: 0xFFCD, name: 'F16' },
        {keysym: 0xFFCE, name: 'F17' },
        {keysym: 0xFFCF, name: 'F18' },
        {keysym: 0xFFD0, name: 'F19' },
        {keysym: 0xFFD1, name: 'F20' },
        {keysym: 0xFFD2, name: 'F21' },
        {keysym: 0xFFD3, name: 'F22' },
        {keysym: 0xFFD4, name: 'F23' },
        {keysym: 0xFFD5, name: 'F24' },
        {keysym: 0xFFE1, name: 'Shift', value: "", modifier: true },
        {keysym: 0xFFE2, name: 'Shift', value: "", modifier: true },
        {keysym: 0xFFE3, name: 'Ctrl', value: null, modifier: true },
        {keysym: 0xFFE4, name: 'Ctrl', value: null, modifier: true },
        {keysym: 0xFFE5, name: 'Caps' },
        {keysym: 0xFFE7, name: 'Meta', value: null, modifier: true },
        {keysym: 0xFFE8, name: 'Meta', value: null, modifier: true },
        {keysym: 0xFFE9, name: 'Alt', value: null, modifier: true },
        {keysym: 0xFFEA, name: 'Alt', value: null, modifier: true },
        {keysym: 0xFFEB, name: 'Super', value: null, modifier: true },
        {keysym: 0xFFEC, name: 'Super', value: null, modifier: true },
        {keysym: 0xFFED, name: 'Hyper', value: null, modifier: true },
        {keysym: 0xFFEE, name: 'Hyper', value: null, modifier: true },
        {keysym: 0xFFFF, name: 'Delete' }
    ];

    /**
     * All known keys, as a map of X11 keysym to KeyDefinition.
     *
     * @constant
     * @private
     * @type {Object.<String, KeyDefinition>}
     */
    var KNOWN_KEYS = {};
    _KNOWN_KEYS.forEach(function createKeyDefinitionMap(keyDefinition) {

        // Construct a map of keysym to KeyDefinition object
        KNOWN_KEYS[keyDefinition.keysym] = new KeyDefinition(keyDefinition)

    });

    /**
     * A map of X11 keysyms to a KeyDefinition object, if the corresponding
     * key is currently pressed. If a keysym has no entry in this map at all,
     * it means that the key is not being pressed. Note that not all keysyms
     * are necessarily tracked within this map - only those that are explicitly
     * tracked.
     *
     * @private
     * @type {Object.<String,KeyDefinition> }
     */
    var pressedKeys = {};

    /**
     * The current key event batch, containing a representation of all key
     * events processed since the end of the last batch passed to onbatch.
     * Null if no key events have been processed yet.
     *
     * @private
     * @type {!KeyEventBatch}
     */
    var currentBatch = null;

    /**
     * The timestamp of the most recent key event processed.
     *
     * @private
     * @type {Number}
     */
    var lastKeyEvent = 0;

    /**
     * Returns true if the currently-pressed keys are part of a shortcut, or
     * false otherwise.
     *
     * @private
     * @returns {!boolean}
     *     True if the currently-pressed keys are part of a shortcut, or false
     *     otherwise.
     */
    function isShortcut() {

        // If one of the currently-pressed keys is non-printable, a shortcut
        // is being typed
        for (var keysym in pressedKeys) {
            if (pressedKeys[keysym].value === null)
                return true;
        }

        return false;
    }

    /**
     * If the provided keysym corresponds to a valid UTF-8 character, return
     * a KeyDefinition for that keysym. Otherwise, return null.
     *
     * @private
     * @param {Number} keysym
     *     The keysym to produce a UTF-8 KeyDefinition for, if valid.
     *
     * @returns {KeyDefinition}
     *     A KeyDefinition for the provided keysym, if it's a valid UTF-8
     *     keysym, or null otherwise.
     */
    function getUnicodeKeyDefinition(keysym) {

        // Translate only if keysym maps to Unicode
        if (keysym < 0x00 || (keysym > 0xFF && (keysym | 0xFFFF) != 0x0100FFFF))
            return null;

        var codepoint = keysym & 0xFFFF;
        var mask;
        var bytes;

        // Determine size and initial byte mask
        if (codepoint <= 0x007F) {
            mask  = 0x00;
            bytes = 1;
        }
        else if (codepoint <= 0x7FF) {
            mask  = 0xC0;
            bytes = 2;
        }
        else {
            mask  = 0xE0;
            bytes = 3;
        }

        var byteArray = new ArrayBuffer(bytes);
        var byteView = new Int8Array(byteArray);

        // Add trailing bytes, if any
        for (var i = 1; i < bytes; i++) {
            byteView[bytes - i] = 0x80 | (codepoint & 0x3F);
            codepoint >>= 6;
        }

        // Set initial byte
        byteView[0] = mask | codepoint;

        // Convert to UTF8 string
        var name = new TextDecoder("utf-8").decode(byteArray);

        // Create and return the definition
        return new KeyDefinition({keysym: keysym, name: name, value: name, modifier: false});

    }

    /**
     * Return a KeyDefinition corresponding to the provided keysym.
     *
     * @private
     * @param {Number} keysym
     *     The keysym to return a KeyDefinition for.
     *
     * @returns {KeyDefinition}
     *     A KeyDefinition corresponding to the provided keysym.
     */
    function getKeyDefinitionByKeysym(keysym) {

        // If it's a known type, return the existing definition
        if (keysym in KNOWN_KEYS)
            return KNOWN_KEYS[keysym];

        // Return a UTF-8 KeyDefinition, if valid
        var definition = getUnicodeKeyDefinition(keysym);
        if (definition != null)
            return definition;

        // If it's not UTF-8, return an unknown definition, with the name
        // just set to the hex value of the keysym
        return new KeyDefinition({
            keysym: keysym,
            name: '0x' + String(keysym.toString(16))
        })

    }

    /**
     * Fired whenever a new batch of typed text extracted from key events
     * is available. A new batch will be provided every time a new key event
     * is processed after more than batchSeperation milliseconds after the
     * previous key event.
     *
     * @event
     * @param {!Guacamole.KeyEventInterpreter.KeyEventBatch}
     */
    this.onbatch = null;

    /**
     * Handles a raw key event, potentially appending typed text to the
     * current batch, and calling onbatch with the current batch, if the
     * callback is set and a new batch is about to be started.
     *
     * @param {!string[]} args
     *     The arguments of the key event.
     */
    this.handleKeyEvent = function handleKeyEvent(args) {

        // The X11 keysym
        var keysym = parseInt(args[0]);

        // Either 1 or 0 for pressed or released, respectively
        var pressed = parseInt(args[1]);

        // The timestamp when this key event occured
        var timestamp = parseInt(args[2]);

        // If no current batch exists, start a new one now
        if (!currentBatch)
            currentBatch = new Guacamole.KeyEventInterpreter.KeyEventBatch();

        // Only switch to a new batch of text if sufficient time has passed
        // since the last key event
        var newBatch = (batchSeperation >= 0
                && (timestamp - lastKeyEvent) >= batchSeperation);
        lastKeyEvent = timestamp;

        if (newBatch) {

            // Call the handler with the current batch of text and the timestamp
            // at which the current batch started
            if (currentBatch.events.length && interpreter.onbatch)
                interpreter.onbatch(currentBatch);

            // Move on to the next batch of text
            currentBatch = new Guacamole.KeyEventInterpreter.KeyEventBatch();

        }

        var keyDefinition = getKeyDefinitionByKeysym(keysym);

        // Mark down whether the key was pressed or released
        if (keyDefinition.modifier) {
            if (pressed)
                pressedKeys[keysym] = keyDefinition;
            else
                delete pressedKeys[keysym];
        }

        // Append to the current typed value when a printable
        // (non-modifier) key is pressed
        else if (pressed) {

            var relativeTimestap = timestamp - startTimestamp;

            if (isShortcut()) {

                var shortcutText = '<';

                var firstKey = true;

                // Compose entry by inspecting the state of each tracked key.
                // At least one key must be pressed when in a shortcut.
                for (var keysym in pressedKeys) {

                    var pressedKeyDefinition = pressedKeys[keysym];

                    // Print name of key
                    if (firstKey) {
                        shortcutText += pressedKeyDefinition.name;
                        firstKey = false;
                    }

                    else
                        shortcutText += ('+' + pressedKeyDefinition.name);

                }

                // Finally, append the printable key to close the shortcut
                shortcutText += ('+' + keyDefinition.name + '>')

                // Add the shortcut to the current batch
                currentBatch.simpleValue += shortcutText;
                currentBatch.events.push(new Guacamole.KeyEventInterpreter.KeyEvent(
                        shortcutText, false, relativeTimestap));

            }

            // Print the key itself
            else {

                var keyText;
                var typed;

                // Print the value if explicitly defined
                if (keyDefinition.value != null) {

                    keyText = keyDefinition.value;
                    typed = true;

                }

                // Otherwise print the name
                else {

                    keyText = ('<' + keyDefinition.name + '>');

                    // While this is a representation for a single character,
                    // the key text is the name of the key, not the actual
                    // character itself
                    typed = false;

                }

                // Add the key to the current batch
                currentBatch.simpleValue += keyText;
                currentBatch.events.push(new Guacamole.KeyEventInterpreter.KeyEvent(
                        keyText, typed, relativeTimestap));

            }

        }

    }

    /**
     * Return the current batch of typed text. Note that the batch may be
     * incomplete, as more key events might be processed before the next
     * batch starts.
     *
     * @returns {Guacamole.KeyEventInterpreter.KeyEventBatch}
     *     The current batch of text.
     */
    this.getCurrentBatch = function getCurrentBatch() {
        return currentBatch;
    }
}

/**
 * A granular description of an extracted key event, including a human-readable
 * text representation of the event, whether the event is directly typed or not,
 * and the timestamp when the event occured.
 *
 * @constructor
 * @param {!String} text
 *     A human-readable representation of the event.
 *
 * @param {!boolean} typed
 *     True if this event represents a directly-typed character, or false
 *     otherwise.
 *
 * @param {!Number} timestamp
 *     The timestamp from the recording when this event occured.
 */
Guacamole.KeyEventInterpreter.KeyEvent = function KeyEvent(text, typed, timestamp) {

    /**
     * A human-readable representation of the event. If a printable character
     * was directly typed, this will just be that character. Otherwise it will
     * be a string describing the event.
     *
     * @type {!String}
     */
    this.text = text;

    /**
     * True if this text of this event is exactly a typed character, or false
     * otherwise.
     *
     * @type {!boolean}
     */
    this.typed = typed;

    /**
     * The timestamp from the recording when this event occured. If a
     * `startTimestamp` value was provided to the interpreter constructor, this
     * will be relative to start of the recording. If not, it will be the raw
     * timestamp from the key event.
     *
     * @type {!Number}
     */
    this.timestamp = timestamp;

};

/**
 * A series of intepreted key events, seperated by at least the configured
 * batchSeperation value from any other key events in the recording corresponding
 * to the interpreted key events. A batch will always consist of at least one key
 * event, and an associated simplified representation of the event(s).
 *
 * @constructor
 * @param {!Guacamole.KeyEventInterpreter.KeyEvent[]} events
 *     The interpreted key events for this batch.
 *
 * @param {!String} simpleValue
 *     The simplified, human-readable value representing the key events for
 *     this batch.
 */
Guacamole.KeyEventInterpreter.KeyEventBatch = function KeyEventBatch(events, simpleValue) {

    /**
     * All key events for this batch.
     *
     * @type {!Guacamole.KeyEventInterpreter.KeyEvent[]}
     */
    this.events = events || [];

    /**
     * The simplified, human-readable value representing the key events for
     * this batch, equivalent to concatenating the `text` field of all key
     * events in the batch.
     *
     * @type {!String}
     */
    this.simpleValue = simpleValue || '';
}
