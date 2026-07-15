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
 * Shared utilities for normalizing and replaying pasted text.
 *
 * @namespace
 */
Guacamole.Paste = Guacamole.Paste || {};

/**
 * Normalizes clipboard text line endings to "\n".
 *
 * Used when working with text content (for example clipboard buffering or text
 * input), where LF is the preferred canonical newline.
 *
 * @param {!string} content
 *     The clipboard text to normalize.
 *
 * @returns {!string}
 *     The clipboard text with all CRLF and CR sequences converted to LF.
 */
Guacamole.Paste.normalizeToLF = function normalizeToLF(content) {
    return content.replace(/\r\n|\r/g, '\n');
};

/**
 * Normalizes clipboard text line endings to "\r".
 *
 * Used when replaying text as keyboard input. Terminals interpret CR as the
 * Enter key, while LF is just a newline and is not reliably treated as Enter.
 *
 * @param {!string} content
 *     The clipboard text to normalize.
 *
 * @returns {!string}
 *     The clipboard text with all CRLF, CR, and LF sequences converted to CR.
 */
Guacamole.Paste.normalizeToCR = function normalizeToCR(content) {
    return content.replace(/\r\n|\n/g, '\r');
};
