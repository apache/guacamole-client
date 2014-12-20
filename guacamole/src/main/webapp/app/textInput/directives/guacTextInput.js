/*
 * Copyright (C) 2014 Glyptodon LLC
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

/**
 * A directive which displays the Guacamole text input method.
 */
angular.module('textInput').directive('guacTextInput', [function guacTextInput() {

    return {
        restrict: 'E',
        replace: true,
        scope: {},

        templateUrl: 'app/textInput/templates/guacTextInput.html',
        controller: ['$scope', '$rootScope', '$element', '$timeout',
            function guacTextInput($scope, $rootScope, $element, $timeout) {

            /**
             * The number of characters to include on either side of text input
             * content, to allow the user room to use backspace and delete.
             *
             * @type Number
             */
            var TEXT_INPUT_PADDING = 128;

            /**
             * The Unicode codepoint of the character to use for padding on
             * either side of text input content.
             *
             * @type Number
             */
            var TEXT_INPUT_PADDING_CODEPOINT = 0x200B;

            /**
             * Recently-sent text, ordered from oldest to most recent.
             *
             * @type String[]
             */
            $scope.sentText = [];

            /**
             * Whether the "Alt" key is currently pressed within the text input
             * interface.
             * 
             * @type Boolean
             */
            $scope.altPressed = false;

            /**
             * Whether the "Ctrl" key is currently pressed within the text
             * input interface.
             * 
             * @type Boolean
             */
            $scope.ctrlPressed = false;

            /**
             * The text area input target.
             *
             * @type Element
             */
            var target = $element.find('.target')[0];

            /**
             * Whether the text input target currently has focus. Setting this
             * attribute has no effect, but any bound property will be updated
             * as focus is gained or lost.
             *
             * @type Boolean
             */
            var hasFocus = false;

            target.onfocus = function targetFocusGained() {
                hasFocus = true;
                resetTextInputTarget(TEXT_INPUT_PADDING);
            };

            target.onblur = function targetFocusLost() {
                hasFocus = false;
                target.focus();
            };

            /**
             * Whether composition is currently active within the text input
             * target element, such as when an IME is in use.
             *
             * @type Boolean
             */
            var composingText = false;

            target.addEventListener("compositionstart", function targetComposeStart(e) {
                composingText = true;
            }, false);

            target.addEventListener("compositionend", function targetComposeEnd(e) {
                composingText = false;
            }, false);

            /**
             * Translates a given Unicode codepoint into the corresponding X11
             * keysym.
             * 
             * @param {Number} codepoint
             *     The Unicode codepoint to translate.
             *
             * @returns {Number}
             *     The X11 keysym that corresponds to the given Unicode
             *     codepoint, or null if no such keysym exists.
             */
            var keysymFromCodepoint = function keysymFromCodepoint(codepoint) {

                // Keysyms for control characters
                if (codepoint <= 0x1F || (codepoint >= 0x7F && codepoint <= 0x9F))
                    return 0xFF00 | codepoint;

                // Keysyms for ASCII chars
                if (codepoint >= 0x0000 && codepoint <= 0x00FF)
                    return codepoint;

                // Keysyms for Unicode
                if (codepoint >= 0x0100 && codepoint <= 0x10FFFF)
                    return 0x01000000 | codepoint;

                return null;

            };

            /**
             * Presses and releases the key corresponding to the given keysym,
             * as if typed by the user.
             * 
             * @param {Number} keysym The keysym of the key to send.
             */
            var sendKeysym = function sendKeysym(keysym) {
                $rootScope.$broadcast('guacKeydown', keysym);
                $rootScope.$broadcast('guacKeyup', keysym);
            };

            /**
             * Presses and releases the key having the keysym corresponding to
             * the Unicode codepoint given, as if typed by the user.
             * 
             * @param {Number} codepoint
             *     The Unicode codepoint of the key to send.
             */
            var sendCodepoint = function sendCodepoint(codepoint) {

                if (codepoint === 10) {
                    sendKeysym(0xFF0D);
                    releaseStickyKeys();
                    return;
                }

                var keysym = keysymFromCodepoint(codepoint);
                if (keysym) {
                    sendKeysym(keysym);
                    releaseStickyKeys();
                }

            };

            /**
             * Translates each character within the given string to keysyms and
             * sends each, in order, as if typed by the user.
             * 
             * @param {String} content
             *     The string to send.
             */
            var sendString = function sendString(content) {

                var sentText = "";

                // Send each codepoint within the string
                for (var i=0; i<content.length; i++) {
                    var codepoint = content.charCodeAt(i);
                    if (codepoint !== TEXT_INPUT_PADDING_CODEPOINT) {
                        sentText += String.fromCharCode(codepoint);
                        sendCodepoint(codepoint);
                    }
                }

                // Display the text that was sent
                $scope.$apply(function addSentText() {
                    $scope.sentText.push(sentText);
                });

                // Remove text after one second
                $timeout(function removeSentText() {
                    $scope.sentText.shift();
                }, 1000);

            };

            /**
             * Releases all currently-held sticky keys within the text input UI.
             */
            var releaseStickyKeys = function releaseStickyKeys() {

                // Reset all sticky keys
                $scope.$apply(function clearAllStickyKeys() {
                    $scope.altPressed = false;
                    $scope.ctrlPressed = false;
                });

            };

            /**
             * Removes all content from the text input target, replacing it
             * with the given number of padding characters. Padding of the
             * requested size is added on both sides of the cursor, thus the
             * overall number of characters added will be twice the number
             * specified.
             * 
             * @param {Number} padding
             *     The number of characters to pad the text area with.
             */
            var resetTextInputTarget = function resetTextInputTarget(padding) {

                var paddingChar = String.fromCharCode(TEXT_INPUT_PADDING_CODEPOINT);

                // Pad text area with an arbitrary, non-typable character (so there is something
                // to delete with backspace or del), and position cursor in middle.
                target.value = new Array(padding*2 + 1).join(paddingChar);
                target.setSelectionRange(padding, padding);

            };

            target.addEventListener("input", function(e) {

                // Ignore input events during text composition
                if (composingText)
                    return;

                var i;
                var content = target.value;
                var expectedLength = TEXT_INPUT_PADDING*2;

                // If content removed, update
                if (content.length < expectedLength) {

                    // Calculate number of backspaces and send
                    var backspaceCount = TEXT_INPUT_PADDING - target.selectionStart;
                    for (i = 0; i < backspaceCount; i++)
                        sendKeysym(0xFF08);

                    // Calculate number of deletes and send
                    var deleteCount = expectedLength - content.length - backspaceCount;
                    for (i = 0; i < deleteCount; i++)
                        sendKeysym(0xFFFF);

                }

                else
                    sendString(content);

                // Reset content
                resetTextInputTarget(TEXT_INPUT_PADDING);
                e.preventDefault();

            }, false);

            // Do not allow event target contents to be selected during input
            target.addEventListener("selectstart", function(e) {
                e.preventDefault();
            }, false);

        }]

    };
}]);
