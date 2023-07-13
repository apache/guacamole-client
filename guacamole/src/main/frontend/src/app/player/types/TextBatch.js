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

/* global _ */

/**
 * Service which defines the TextBatch class.
 */
angular.module('player').factory('TextBatch', [function defineTextBatch() {

    /**
     * A batch of text associated with a recording. The batch consists of a
     * string representation of the text that would be typed based on the key
     * events in the recording, as well as a timestamp when the batch started.
     *
     * @constructor
     * @param {Guacamole.KeyEventInterpreter.KeyEvent|TextBatch|Object} [template={}]
     *     The object whose properties should be copied within the new TextBatch.
     */
    const TextBatch = function TextBatch(template) {

        /**
         * All key events for this batch, with sequences of key events having
         * the same `typed` field value combined.
         *
         * @type {!KeyEventBatch[]}
         */
        this.events = _.reduce(template.events, (consolidatedEvents, rawEvent) => {

            const currentEvent = _.last(consolidatedEvents);

            // If a current event exists with the same `typed` value, conslidate
            // the raw text event into it
            if (currentEvent && currentEvent.typed === rawEvent.typed)
                currentEvent.text += rawEvent.text;

            // Otherwise, create a new conslidated event starting now
            else
                consolidatedEvents.push(new TextBatch.ConsolidatedKeyEvent(rawEvent));

            return consolidatedEvents;

        }, []);

        /**
         * The simplified, human-readable value representing the key events for
         * this batch, equivalent to concatenating the `text` field of all key
         * events in the batch.
         *
         * @type {!String}
         */
        this.simpleValue = template.simpleValue || '';

    };

    /**
     * A granular description of an extracted key event or sequence of events.
     * Similar to the Guacamole.KeyEventInterpreter.KeyEvent type, except that
     * this KeyEventBatch may contain multiple contiguous events of the same type,
     * meaning that all event(s) that were combined into this event must have
     * had the same `typed` field value. A single timestamp for the first combined
     * event will be used for the whole batch.
     *
     * @constructor
     * @param {Guacamole.KeyEventInterpreter.KeyEventBatch|ConsolidatedKeyEvent|Object} [template={}]
     *     The object whose properties should be copied within the new KeyEventBatch.
     */
    TextBatch.ConsolidatedKeyEvent = function ConsolidatedKeyEvent(template) {

        /**
         * A human-readable representation of the event(s). If a series of printable
         * characters was directly typed, this will just be those character(s).
         * Otherwise it will be a string describing the event(s).
         *
         * @type {!String}
         */
        this.text = template.text;

        /**
         * True if this text of this event is exactly a typed character, or false
         * otherwise.
         *
         * @type {!boolean}
         */
        this.typed = template.typed;

        /**
         * The timestamp from the recording when this event occured. If a
         * `startTimestamp` value was provided to the interpreter constructor, this
         * will be relative to start of the recording. If not, it will be the raw
         * timestamp from the key event.
         *
         * @type {!Number}
         */
        this.timestamp = template.timestamp;

    };

    return TextBatch;

}]);
