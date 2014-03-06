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

/**
 * Provides central storage for a cross-connection clipboard state. This
 * clipboard state is shared only for a single HTTP session. Multiple HTTP
 * sessions will all have their own state.
 * 
 * @author Michael Jumper
 */
public class ClipboardState {

    /**
     * The current contents.
     */
    private String contents = "";

    /**
     * The timestamp of the last contents update.
     */
    private long last_update = 0;
    
    /**
     * Returns the current clipboard contents.
     * @return The current clipboard contents
     */
    public synchronized String getContents() {
        return contents;
    }

    /**
     * Sets the current clipboard contents.
     * @param contents The contents to assign to the clipboard.
     */
    public synchronized void setContents(String contents) {
        this.contents = contents;
        last_update = System.currentTimeMillis();
        this.notifyAll();
    }

    /**
     * Wait up to the given timeout for new clipboard data. If data more recent
     * than the timeout period is available, return that.
     * 
     * @param timeout The amount of time to wait, in milliseconds.
     * @return The current clipboard contents.
     */
    public synchronized String waitForContents(int timeout) {

        // Wait for new contents if it's been a while
        if (System.currentTimeMillis() - last_update > timeout) {
            try {
                this.wait(timeout);
            }
            catch (InterruptedException e) { /* ignore */ }
        }

        return getContents();

    }
    
}
