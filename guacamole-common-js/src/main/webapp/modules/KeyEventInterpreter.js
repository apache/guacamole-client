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
 * retrieved through the onBatch callback or by calling getCurrentBatch().
 *
 * NOTE: The event processing logic and output format is based on the `guaclog`
 * tool, with the addition of batching support.
 *
 * @constructor
 * @param {number} [batchSeperation=5000]
 *     The minimum number of milliseconds that must elapse between subsequent
 *     batches of key-event-generated text. If 0 or negative, no splitting will
 *     occur, resulting in a single batch for all provided key events.
 */
Guacamole.KeyEventInterpreter = function KeyEventInterpreter(batchSeperation) {

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
     * A human-readable representation of all keys pressed since the last keyframe.
     *
     * @private
     * @type {String}
     */
    var currentTypedValue = '';

    /**
     * The timestamp of the key event that started the most recent batch of
     * text content. If 0, no key events have been processed yet.
     *
     * @private
     * @type {Number}
     */
    var lastTextTimestamp = 0;

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
     * @returns
     *     Return a KeyDefinition for the provided keysym, if it it's a valid
     *     UTF-8 keysym, or null otherwise.
     */
    function getUnicodeKeyDefinition(keysym) {

        // Translate only if keysym maps to Unicode
        if (keysym < 0x00 || (keysym > 0xFF && (keysym | 0xFFFF) != 0x0100FFFF))
            return null;

        var codepoint = keysym & 0xFFFF;
        var mask;
        var bytes;

        /* Determine size and initial byte mask */
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
        return new KeyDefinition({keysym: keysym.toString(), name: name, value: name, modifier: false});

    }

    /**
     * Return a KeyDefinition corresponding to the provided keysym.
     *
     * @private
     * @param {Number} keysym
     *     The keysym to return a KeyDefinition for.
     *
     * @returns
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
     * @param {!String} text
     *     The typed text associated with the batch of text.
     *
     * @param {!number} timestamp
     *     The raw recording timestamp associated with the first key event
     *     that started this batch of text.
     */
    interpreter.onBatch = null;

    /**
     * Handles a raw key event, potentially appending typed text to the
     * current batch, and calling onBatch with the current batch, if the
     * callback is set and a new batch is about to be started.
     *
     * @param {!string[]} args
     *     The arguments of the key event.
     */
    interpreter.handleKeyEvent = function handleKeyEvent(args) {

        // The X11 keysym
        var keysym = parseInt(args[0]);

        // Either 1 or 0 for pressed or released, respectively
        var pressed = parseInt(args[1]);

        // The timestamp when this key event occured
        var timestamp = parseInt(args[2]);

        // If no current batch exists, start a new one now
        if (!lastTextTimestamp) {
            lastTextTimestamp = timestamp;
            lastKeyEvent = timestamp;
        }

        // Only switch to a new batch of text if sufficient time has passed
        // since the last key event
        var newBatch = (batchSeperation >= 0
                && (timestamp - lastKeyEvent) >= batchSeperation);
        lastKeyEvent = timestamp;

        if (newBatch) {

            // Call the handler with the current batch of text and the timestamp
            // at which the current batch started
            if (currentTypedValue && interpreter.onBatch)
                interpreter.onBatch(currentTypedValue, lastTextTimestamp);

            // Move on to the next batch of text
            currentTypedValue = '';
            lastTextTimestamp = 0;

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

            if (isShortcut()) {

                currentTypedValue += '<';

                var firstKey = true;

                // Compose entry by inspecting the state of each tracked key.
                // At least one key must be pressed when in a shortcut.
                for (var keysym in pressedKeys) {

                    var pressedKeyDefinition = pressedKeys[keysym];

                    // Print name of key
                    if (firstKey) {
                        currentTypedValue += pressedKeyDefinition.name;
                        firstKey = false;
                    }

                    else
                        currentTypedValue += ('+' + pressedKeyDefinition.name);

                }

                // Finally, append the printable key to close the shortcut
                currentTypedValue += ('+' + keyDefinition.name + '>')

            }

            // Print the key itself
            else {

                // Print the value if explicitly defined
                if (keyDefinition.value != null)
                    currentTypedValue += keyDefinition.value;

                // Otherwise print the name
                else
                    currentTypedValue += ('<' + keyDefinition.name + '>');
            }

        }

    }

    /**
     * Return the current batch of typed text. Note that the batch may be
     * incomplete, as more key events might be processed before the next
     * batch starts.
     *
     * @returns
     *     The current batch of text.
     */
    interpreter.getCurrentText = function getCurrentText() {
        return currentTypedValue;
    }

    /**
     * Return the recording timestamp associated with the start of the
     * current batch of typed text.
     *
     * @returns
     *     The recording timestamp at which the current batch started.
     */
    interpreter.getCurrentTimestamp = function getCurrentTimestamp() {
        return lastTextTimestamp;
    }

}
