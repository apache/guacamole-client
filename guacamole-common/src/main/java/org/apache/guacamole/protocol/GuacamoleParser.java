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

package org.apache.guacamole.protocol;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;

/**
 * Parser for the Guacamole protocol. Arbitrary instruction data is appended,
 * and instructions are returned as a result. Invalid instructions result in
 * exceptions.
 */
public class GuacamoleParser implements Iterator<GuacamoleInstruction> {

    /**
     * The maximum number of characters per instruction.
     */
    public static final int INSTRUCTION_MAX_LENGTH = 8192;

    /**
     * The maximum number of digits to allow per length prefix.
     */
    public static final int INSTRUCTION_MAX_DIGITS = 5;

    /**
     * The maximum number of elements per instruction, including the opcode.
     */
    public static final int INSTRUCTION_MAX_ELEMENTS = 128;

    /**
     * All possible states of the instruction parser.
     */ 
    private enum State {

        /**
         * The parser is currently waiting for data to complete the length prefix
         * of the current element of the instruction.
         */
        PARSING_LENGTH,

        /**
         * The parser has finished reading the length prefix and is currently
         * waiting for data to complete the content of the instruction.
         */
        PARSING_CONTENT,

        /**
         * The instruction has been fully parsed.
         */
        COMPLETE,

        /**
         * The instruction cannot be parsed because of a protocol error.
         */
        ERROR
            
    }

    /**
     * The latest parsed instruction, if any.
     */
    private GuacamoleInstruction parsedInstruction;

    /**
     * The parse state of the instruction.
     */
    private State state = State.PARSING_LENGTH;

    /**
     * The length of the current element, if known, in Java characters. This
     * value may be adjusted as an element is parsed to take surrogates into
     * account.
     */
    private int elementLength = 0;

    /**
     * The length of the current element, if known, in Unicode codepoints. This
     * value will NOT change as an element is parsed.
     */
    private int elementCodepoints;

    /**
     * The number of elements currently parsed.
     */
    private int elementCount = 0;

    /**
     * All currently parsed elements.
     */
    private final String elements[] = new String[INSTRUCTION_MAX_ELEMENTS];

    /**
     * A copy of the raw protocol data that has been parsed for the current
     * instruction. This value is maintained by {@link #append(char[], int, int)}.
     */
    private final char rawInstruction[] = new char[INSTRUCTION_MAX_LENGTH];

    /**
     * The offset within {@link #rawInstruction} that new data should be
     * appended. This value is maintained by {@link #append(char[], int, int)}.
     */
    private int rawInstructionOffset = 0;

    /**
     * GuacamoleInstruction that efficiently exposes the originally parsed
     * character buffer for calls to {@link #toString()} and {@link #toCharArray()}
     * rather than regenerate the buffer from scratch.
     */
    private static class ParsedGuacamoleInstruction extends GuacamoleInstruction {

        /**
         * The original data parsed to produce this GuacamoleInstruction.
         */
        private final char[] rawChars;

        /**
         * A String containing the original data parsed to produce this
         * GuacamoleInstruction.
         */
        private String rawString = null;

        /**
         * Creates a new GuacamoleInstruction that efficiently exposes the
         * originally parsed character buffer rather than regenerating that
         * buffer from scratch for {@link #toString()} and {@link #toCharArray()}.
         *
         * @param opcode
         *     The opcode of the instruction to create.
         *
         * @param args
         *     The list of argument values to provide in the new instruction, if
         *     any.
         *
         * @param raw
         *     The underlying representation of this instruction as would be sent
         *     over the network via the Guacamole protocol.
         */
        public ParsedGuacamoleInstruction(String opcode, List<String> args, char[] raw) {
            super(opcode, args);
            this.rawChars = raw;
        }

        @Override
        public String toString() {

            if (rawString == null)
                rawString = new String(rawChars);

            return rawString;

        }

        @Override
        public char[] toCharArray() {
            return rawChars;
        }

    }

    /**
     * Appends data from the given buffer to the current instruction.
     * 
     * @param chunk
     *     The buffer containing the data to append.
     *
     * @param offset
     *     The offset within the buffer where the data begins.
     *
     * @param length
     *     The length of the data to append.
     *
     * @return
     *     The number of characters appended, or 0 if complete instructions
     *     have already been parsed and must be read via next() before more
     *     data can be appended.
     *
     * @throws GuacamoleException
     *     If an error occurs while parsing the new data.
     */
    public int append(char chunk[], int offset, int length) throws GuacamoleException {

        int originalOffset = offset;
        int originalLength = length;

        // Process as much of the received chunk as possible
        while (length > 0) {

            int appended = processElement(chunk, offset, length);
            if (appended == 0)
                break;

            length -= appended;
            offset += appended;
        }

        // Update the raw copy of the received instruction with whatever data
        // has now been processed
        int charsParsed = originalLength - length;
        if (charsParsed > 0) {

            System.arraycopy(chunk, originalOffset, rawInstruction, rawInstructionOffset, charsParsed);
            rawInstructionOffset += charsParsed;

            // If the instruction is now complete, we're good to store the
            // parsed instruction for future retrieval via next()
            if (state == State.COMPLETE) {
                parsedInstruction = new ParsedGuacamoleInstruction(elements[0],
                        Arrays.asList(elements).subList(1, elementCount),
                        Arrays.copyOf(rawInstruction, rawInstructionOffset));
                rawInstructionOffset = 0;
            }

        }

        return charsParsed;

    }

    /**
     * Processes additional data from the given buffer, potentially adding
     * another element to the current instruction being parsed. This function
     * will need to be invoked multiple times per instruction until all data
     * for that instruction is ready.
     * <p>
     * This function DOES NOT update {@link #parsedInstruction}. The caller
     * ({@link #append(char[], int, int)}) must update this as necessary when
     * the parser {@link #state} indicates the instruction is complete.
     *
     * @param chunk
     *     The buffer containing the data to append.
     *
     * @param offset
     *     The offset within the buffer where the data begins.
     *
     * @param length
     *     The length of the data to append.
     *
     * @return
     *     The number of characters appended, or 0 if complete instructions
     *     have already been parsed and must be read via next() before more
     *     data can be appended.
     *
     * @throws GuacamoleException
     *     If an error occurs while parsing the new data.
     */
    private int processElement(char chunk[], int offset, int length) throws GuacamoleException {

        int charsParsed = 0;

        // Do not exceed maximum number of elements
        if (elementCount == INSTRUCTION_MAX_ELEMENTS && state != State.COMPLETE) {
            state = State.ERROR;
            throw new GuacamoleServerException("Instruction contains too many elements.");
        }

        // Parse element length
        if (state == State.PARSING_LENGTH) {

            int parsedLength = elementLength;
            while (charsParsed < length) {

                // Pull next character
                char c = chunk[offset + charsParsed++];

                // If digit, add to length
                if (c >= '0' && c <= '9')
                    parsedLength = parsedLength*10 + c - '0';

                // If period, switch to parsing content
                else if (c == '.') {
                    state = State.PARSING_CONTENT;
                    break;
                }

                // If not digit, parse error
                else {
                    state = State.ERROR;
                    throw new GuacamoleServerException("Non-numeric character in element length.");
                }

            }

            // If too long, parse error
            if (parsedLength > INSTRUCTION_MAX_LENGTH) {
                state = State.ERROR;
                throw new GuacamoleServerException("Instruction exceeds maximum length.");
            }

            // Save length
            elementCodepoints = elementLength = parsedLength;

        } // end parse length

        // Parse element content, if available
        while (state == State.PARSING_CONTENT && charsParsed + elementLength + 1 <= length) {

            // Read element (which may not match element length if surrogate
            // characters are present)
            String element = new String(chunk, offset + charsParsed, elementLength);

            // Verify element contains the number of whole Unicode characters
            // expected, scheduling a future read if we don't yet have enough
            // characters
            int codepoints = element.codePointCount(0, element.length());
            if (codepoints < elementCodepoints) {
                elementLength += elementCodepoints - codepoints;
                continue;
            }

            // If the current element ends with a character involving both
            // a high and low surrogate, elementLength points to the low
            // surrogate and NOT the element terminator. We must correct the
            // length and reevaluate.
            else if (Character.isSurrogatePair(chunk[offset + charsParsed + elementLength - 1],
                    chunk[offset + charsParsed + elementLength])) {
                elementLength++;
                continue;
            }

            charsParsed += elementLength;
            elementLength = 0;

            // Add element to currently parsed elements
            elements[elementCount++] = element;

            // Read terminator char following element
            char terminator = chunk[offset + charsParsed++];
            switch (terminator) {

                // If semicolon, store end-of-instruction
                case ';':
                    state = State.COMPLETE;
                    break;

                // If comma, move on to next element
                case ',':
                    state = State.PARSING_LENGTH;
                    break;

                // Otherwise, parse error
                default:
                    state = State.ERROR;
                    throw new GuacamoleServerException("Element terminator of instruction was not ';' nor ','");

            }

        } // end parse content

        return charsParsed;

    }

    /**
     * Appends data from the given buffer to the current instruction.
     * 
     * @param chunk The data to append.
     * @return The number of characters appended, or 0 if complete instructions
     *         have already been parsed and must be read via next() before
     *         more data can be appended.
     * @throws GuacamoleException If an error occurs while parsing the new data.
     */   
    public int append(char chunk[]) throws GuacamoleException {
        return append(chunk, 0, chunk.length);
    }

    @Override
    public boolean hasNext() {
        return state == State.COMPLETE;
    }

    @Override
    public GuacamoleInstruction next() {

        // No instruction to return if not yet complete
        if (state != State.COMPLETE)
            return null;
        
        // Reset for next instruction.
        state = State.PARSING_LENGTH;
        elementCount = 0;
        elementLength = 0;
        
        return parsedInstruction;

    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("GuacamoleParser does not support remove().");
    }

}
