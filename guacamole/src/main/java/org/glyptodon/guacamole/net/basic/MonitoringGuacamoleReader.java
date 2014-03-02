/*
 * Copyright (C) 2014 Glyptodon LLC.
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

package org.glyptodon.guacamole.net.basic;

import java.util.Collection;
import java.util.List;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.io.GuacamoleReader;
import org.glyptodon.guacamole.net.basic.event.ClipboardChangeEvent;
import org.glyptodon.guacamole.net.basic.event.SessionListenerCollection;
import org.glyptodon.guacamole.net.basic.event.listener.ClipboardChangeListener;
import org.glyptodon.guacamole.protocol.GuacamoleInstruction;

/**
 * GuacamoleReader implementation which watches for specific instructions,
 * firing server-side events when they are observed.
 * 
 * @author Michael Jumper
 */
public class MonitoringGuacamoleReader implements GuacamoleReader {

    /**
     * The underlying GuacamoleReader.
     */
    private final GuacamoleReader reader;

    /**
     * Collection of all listeners which will receive events.
     */
    private final SessionListenerCollection listeners;

    /**
     * Notifies all listeners in the given collection that clipboard data has
     * changed.
     *
     * @param listeners A collection of all listeners that should be notified.
     * @param data The new clipboard data.
     * @throws GuacamoleException If any listener throws an error while being
     *                            notified.
     */
    private static boolean notifyClipboardChange(Collection listeners, String data)
            throws GuacamoleException {

        // Build event for clipboard change
        ClipboardChangeEvent event = new ClipboardChangeEvent(data);

        // Notify all listeners
        for (Object listener : listeners) {
            if (listener instanceof ClipboardChangeListener)
                ((ClipboardChangeListener) listener).clipboardChanged(event);
        }

        return true;

    }

    /**
     * Creates a new MonitoringGuacamoleReader which watches the instructions
     * read by the given GuacamoleReader, firing events when specific
     * instructions are seen.
     * 
     * @param listeners The collection of listeners receiving events.
     * @param reader The reader to observe.
     */
    public MonitoringGuacamoleReader(SessionListenerCollection listeners,
            GuacamoleReader reader) {
        this.listeners = listeners;
        this.reader = reader;
    }

    @Override
    public boolean available() throws GuacamoleException {
        return reader.available();
    }

    @Override
    public char[] read() throws GuacamoleException {
        return readInstruction().toString().toCharArray();
    }

    @Override
    public GuacamoleInstruction readInstruction() throws GuacamoleException {

        GuacamoleInstruction instruction = reader.readInstruction();

        // If clipboard changed, notify listeners
        if (instruction.getOpcode().equals("clipboard")) {
            List<String> args = instruction.getArgs();
            if (args.size() >= 1)
                notifyClipboardChange(listeners, args.get(0));
        }
        
        return instruction;
        
    }
    
}
