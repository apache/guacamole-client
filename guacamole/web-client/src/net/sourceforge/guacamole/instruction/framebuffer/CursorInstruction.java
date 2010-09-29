package net.sourceforge.guacamole.instruction.framebuffer;

import net.sourceforge.guacamole.net.Base64;

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

public class CursorInstruction extends Instruction {

    private int x;
    private int y;
    private PNGImage image;
    
    public CursorInstruction(int x, int y, PNGImage image) {
        this.x = x;
        this.y = y;
        this.image = image;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public PNGImage getImage() {
        return image;
    }

    public int getWidth() {
        return getImage().getWidth();
    }

    public int getHeight() {
        return getImage().getHeight();
    }

    @Override
    public String toString() {
        return "cursor:"
                + getX() + ","
                + getY() + ","
                + Base64.toString(getImage().getData()) + ";";
    }


}
