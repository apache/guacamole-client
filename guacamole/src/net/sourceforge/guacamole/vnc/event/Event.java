
package net.sourceforge.guacamole.vnc.event;

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

public abstract class Event implements Comparable<Event> {

    private long time;
    private int index;

    public Event(int index) {
        this.time = System.currentTimeMillis();
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public long getTime() {
        return time;
    }

    public int hashCode() {
        return index;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (getClass() != o.getClass()) return false;
        return getIndex() == ((Event) o).getIndex();
    }

    public int compareTo(Event e) {
        return getIndex() - e.getIndex();
    }

}
