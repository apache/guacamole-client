
/*
 *  Guacamole - Clientless Remote Desktop
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


function GuacamoleMouse(element) {

	/*****************************************/
	/*** Mouse Handler                     ***/
	/*****************************************/


    var mouseIndex = 0;

    var mouseLeftButton   = 0;
    var mouseMiddleButton = 0;
    var mouseRightButton  = 0;

    var mouseX = 0;
    var mouseY = 0;

    var absoluteMouseX = 0;
    var absoluteMouseY = 0;


    function getMouseState(up, down) {
        var mouseState = new MouseEvent(mouseX, mouseY,
                mouseLeftButton, mouseMiddleButton, mouseRightButton, up, down);

        return mouseState;
    }


    // Block context menu so right-click gets sent properly
    element.oncontextmenu = function(e) {return false;};

    element.onmousemove = function(e) {

        e.stopPropagation();

        absoluteMouseX = e.pageX;
        absoluteMouseY = e.pageY;

        mouseX = absoluteMouseX - element.offsetLeft;
        mouseY = absoluteMouseY - element.offsetTop;

        // This is all JUST so we can get the mouse position within the element
        var parent = element.offsetParent;
        while (parent) {
            if (parent.offsetLeft && parent.offsetTop) {
                mouseX -= parent.offsetLeft;
                mouseY -= parent.offsetTop;
            }
            parent = parent.offsetParent;
        }

        movementHandler(getMouseState(0, 0));
    };


    element.onmousedown = function(e) {

        e.stopPropagation();

        switch (e.button) {
            case 0:
                mouseLeftButton = 1;
                break;
            case 1:
                mouseMiddleButton = 1;
                break;
            case 2:
                mouseRightButton = 1;
                break;
        }

        buttonPressedHandler(getMouseState(0, 0));
    };


    element.onmouseup = function(e) {

        e.stopPropagation();

        switch (e.button) {
            case 0:
                mouseLeftButton = 0;
                break;
            case 1:
                mouseMiddleButton = 0;
                break;
            case 2:
                mouseRightButton = 0;
                break;
        }

        buttonReleasedHandler(getMouseState(0, 0));
    };

    // Override selection on mouse event element.
    element.onselectstart = function() {
        return false;
    };

    // Scroll wheel support
    function handleScroll(e) {

        var delta = 0;
        if (e.detail)
            delta = e.detail;
        else if (e.wheelDelta)
            delta = -event.wheelDelta;

        // Up
        if (delta < 0) {
            buttonPressedHandler(getMouseState(1, 0));
            buttonReleasedHandler(getMouseState(0, 0));
        }

        // Down
        if (delta > 0) {
            buttonPressedHandler(getMouseState(0, 1));
            buttonReleasedHandler(getMouseState(0, 0));
        }

        if (e.preventDefault)
            e.preventDefault();

        e.returnValue = false;
    }

    element.addEventListener('DOMMouseScroll', handleScroll, false);

    element.onmousewheel = function(e) {
        handleScroll(e);
    }

    function MouseEvent(x, y, left, middle, right, up, down) {

        this.getX = function() {
            return x;
        };

        this.getY = function() {
            return y;
        };

        this.getLeft = function() {
            return left;
        };

        this.getMiddle = function() {
            return middle;
        };

        this.getRight = function() {
            return right;
        };

        this.getUp = function() {
            return up;
        };

        this.getDown = function() {
            return down;
        };

        this.toString = function() {
            return (mouseIndex++) + "," + x + "," + y + "," + left + "," + middle + "," + right + "," + up + "," + down;
        };

    }


	var buttonPressedHandler = null;
	var buttonReleasedHandler = null;
	var movementHandler = null;

	this.setButtonPressedHandler  = function(mh) {buttonPressedHandler = mh;};
	this.setButtonReleasedHandler = function(mh) {buttonReleasedHandler = mh;};
	this.setMovementHandler = function(mh) {movementHandler = mh;};


    this.getX = function() {return mouseX;};
    this.getY = function() {return mouseY;};
    this.getLeftButton = function() {return mouseLeftButton;};
    this.getMiddleButton = function() {return mouseMiddleButton;};
    this.getRightButton = function() {return mouseRightButton;};

}
