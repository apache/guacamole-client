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

public class PointerEvent extends Event {

    private boolean leftButtonPressed;
    private boolean middleButtonPressed;
    private boolean rightButtonPressed;
    private boolean upButtonPressed;
    private boolean downButtonPressed;
    private int x;
    private int y;

    public PointerEvent(int index, boolean leftButtonPressed, boolean middleButtonPressed, boolean rightButtonPressed, boolean upButtonPressed, boolean downButtonPressed, int x, int y) {
        super(index);
        this.leftButtonPressed = leftButtonPressed;
        this.middleButtonPressed = middleButtonPressed;
        this.rightButtonPressed = rightButtonPressed;
        this.upButtonPressed = upButtonPressed;
        this.downButtonPressed = downButtonPressed;
        this.x = x;
        this.y = y;
    }

    public boolean isLeftButtonPressed() {
        return leftButtonPressed;
    }

    public boolean isMiddleButtonPressed() {
        return middleButtonPressed;
    }

    public boolean isRightButtonPressed() {
        return rightButtonPressed;
    }

    public boolean isUpButtonPressed() {
        return upButtonPressed;
    }

    public boolean isDownButtonPressed() {
        return downButtonPressed;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
