
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

function Layer(width, height) {

    // Off-screen buffer
    var display = document.createElement("canvas");
    var displayContext = display.getContext("2d");

    function resize(newWidth, newHeight) {
        display.style.position = "absolute";
        display.style.left = "0px";
        display.style.right = "0px";

        display.width = newWidth;
        display.height = newHeight;

        width = newWidth;
        height = newHeight;
    }

    display.resize = function(newWidth, newHeight) {
        if (newWidth != width || newHeight != height)
            resize(newWidth, newHeight);
    };

    function fitRect(x, y, w, h) {
        
        // Calculate bounds
        var opBoundX = w + x;
        var opBoundY = h + y;
        
        // Determine max width
        var resizeWidth;
        if (opBoundX > width)
            resizeWidth = opBoundX;
        else
            resizeWidth = width;

        // Determine max height
        var resizeHeight;
        if (opBoundY > height)
            resizeHeight = opBoundY;
        else
            resizeHeight = height;

        // Resize if necessary
        if (resizeWidth != width || resizeHeight != height)
            resize(resizeWidth, resizeHeight);

    }

    resize(width, height);

    var readyHandler = null;
    var nextUpdateToDraw = 0;
    var currentUpdate = 0;
    var updates = new Array();
    var autosize = 0;

    display.setAutosize = function(flag) {
        autosize = flag;
    };

    // Given an update ID, either call the provided update callback, or
    // schedule the update for later.
    function setUpdate(updateId, update) {

        // If this update is the next to draw...
        if (updateId == nextUpdateToDraw) {

            // Call provided update handler.
            update();

            // Draw all pending updates.
            var updateCallback;
            while ((updateCallback = updates[++nextUpdateToDraw])) {
                updateCallback();
                delete updates[nextUpdateToDraw];
            }

            // If done with updates, call ready handler
            if (display.isReady() && readyHandler != null)
                readyHandler();

        }

        // If not next to draw, set callback and wait.
        else
            updates[updateId] = update;

    }

    display.isReady = function() {
        return currentUpdate == nextUpdateToDraw;
    };

    display.setReadyHandler = function(handler) {
        readyHandler = handler;
    };


    display.drawImage = function(x, y, image) {
        var updateId = currentUpdate++;

        setUpdate(updateId, function() {
            if (autosize != 0) fitRect(x, y, image.width, image.height);
            displayContext.drawImage(image, x, y);
        });

    };


    display.draw = function(x, y, url) {
        var updateId = currentUpdate++;

        var image = new Image();
        image.onload = function() {
            setUpdate(updateId, function() {
                if (autosize != 0) fitRect(x, y, image.width, image.height);
                displayContext.drawImage(image, x, y);
            });
        };
        image.src = url;
    };


    display.copyRect = function(srcLayer, srcx, srcy, w, h, x, y) {
        var updateId = currentUpdate++;
    
        setUpdate(updateId, function() {
            if (autosize != 0) fitRect(x, y, w, h);
            displayContext.drawImage(srcLayer, srcx, srcy, w, h, x, y, w, h);
        });

    };

    display.clearRect = function(x, y, w, h) {
        var updateId = currentUpdate++;

        setUpdate(updateId, function() {
            if (autosize != 0) fitRect(x, y, w, h);
            displayContext.clearRect(x, y, w, h);
        });

    };

    display.filter = function(filter) {
        var updateId = currentUpdate++;

        setUpdate(updateId, function() {
            var imageData = displayContext.getImageData(0, 0, width, height);
            filter(imageData.data, width, height);
            displayContext.putImageData(imageData, 0, 0);
        });

    };

    return display;

}

