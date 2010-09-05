
package net.sourceforge.guacamole.instruction.framebuffer;

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

import net.sourceforge.guacamole.instruction.Instruction;

public class CopyRectInstruction extends Instruction {

    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private final int srcX;
    private final int srcY;

    public CopyRectInstruction(int x, int y, int width, int height, int srcX, int srcY) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.srcX = srcX;
        this.srcY = srcY;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getSrcX() {
        return srcX;
    }

    public int getSrcY() {
        return srcY;
    }

    @Override
    public String toString() {
        return "copy:"
                + getSrcX() + ","
                + getSrcY() + ","
                + getWidth() + ","
                + getHeight() + ","
                + getX() + ","
                + getY() + ";";
    }

}
