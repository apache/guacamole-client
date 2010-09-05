
package net.sourceforge.guacamole.event;

/*
 *  Guacamole - Pure JavaScript/HTML VNC Client
 *  Copyright (C) 2010  Michael Jumper
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.IOException;
import java.util.PriorityQueue;

public class EventQueue<E extends Event> {

    private int nextIndex = 0;
    private final PriorityQueue<E> queue = new PriorityQueue<E>();
    private EventHandler<E> handler;

    private final int deadline;

    private AutoflushThread autoflush = new AutoflushThread();

    private class AutoflushThread extends Thread {

        private IOException error;
        private long deadline;
        private static final long ONE_YEAR = 31536000;

        private boolean killed = false;

        public AutoflushThread() {
            this.deadline = ONE_YEAR;
            start();
        }

        public void run() {
            while (!killed) {
                try {
                    if (deadline > 0) sleep(deadline);
                    dropPendingEvents();
                    flush();
                }
                catch (InterruptedException e) {
                    // Interrupt indicates event handled, or thread killed
                    if (killed) return;
                }
                catch (IOException e) {
                    error = e;
                    break;
                }
            }
        }

        public void setDeadline(long deadline) {
            this.deadline = deadline;
            interrupt();
        }

        public void checkError() throws IOException {
            if (error != null) throw error;
        }

        public void kill() {
            killed = true;
            interrupt();
        }

    }

    public void close() {
        autoflush.kill();
    }

    // Starts autoflush wait thread for any waiting events on the queue
    private void startDeadlineAutoflush() {
        synchronized (queue) {

            // No need to autoflush if nothing waiting
            if (queue.size() == 0) return;

            // Get waiting event
            E waiting = queue.peek();

            if (waiting != null) {
                long untilDeadline = deadline + waiting.getTime() - System.currentTimeMillis();

                // Start autoflush thread which waits for time remaining until next
                // event's deadline.
                autoflush.setDeadline(untilDeadline);
            }
            else
                autoflush.setDeadline(AutoflushThread.ONE_YEAR);

        }
    }

    public EventQueue(EventHandler<E> handler, int deadline) {
        this.handler = handler;
        this.deadline = deadline;
    }

    public void add(E event) throws IOException {
        synchronized (queue) {

            autoflush.checkError();

            if (event.getIndex() < nextIndex) {
                //System.err.println("Past event dropped.");
                return;
            }

            if (event == null)
                throw new Error("Cannot add null event.");

            queue.add(event);
        }

        flush();
    }

    private E next() {
        synchronized (queue) {
            // If no events, return nothing.
            if (queue.size() == 0) return null;

            // If still waiting for true next event, return nothing.
            E event = queue.peek();
            if (event.getIndex() != nextIndex)
                return null;

            // If event found, expect next event, remove and return current.
            queue.remove();
            nextIndex++;
            return event;
        }
    }

    // Return number of waiting events
    public int getWaiting() {
        synchronized (queue) {
            // If no events, then none waiting.
            if (queue.size() == 0) return 0;

            // If we have the next event, then none waiting.
            E event = queue.peek();
            if (event.getIndex() == nextIndex)
                return 0;

            // Otherwise, all events are waiting.
            return queue.size();
        }
    }

    // Stop waiting for any unreceived events
    private void dropPendingEvents() {
        synchronized (queue) {
            // If no events, nothing needs to be changed;
            if (queue.size() == 0) return;

            // Otherwise, update nextIndex to index of next event
            E event = queue.peek();
            nextIndex = event.getIndex();
        }
    }

    // Attempts to flush queue
    // If any events remain, an autoflush thread is started.
    private void flush() throws IOException {
        synchronized (queue) {
            E nextEvent;
            while ((nextEvent = next()) != null)
                handler.handle(nextEvent);
        }

        // Start autoflush thread for any remaining events.
        startDeadlineAutoflush();
    }

}
