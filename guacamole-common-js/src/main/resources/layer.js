
/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is guacamole-common-js.
 *
 * The Initial Developer of the Original Code is
 * Michael Jumper.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

/**
 * Namespace for all Guacamole JavaScript objects.
 * @namespace
 */
var Guacamole = Guacamole || {};

/**
 * Abstract ordered drawing surface. Each Layer contains a canvas element and
 * provides simple drawing instructions for drawing to that canvas element,
 * however unlike the canvas element itself, drawing operations on a Layer are
 * guaranteed to run in order, even if such an operation must wait for an image
 * to load before completing.
 * 
 * @constructor
 * 
 * @param {Number} width The width of the Layer, in pixels. The canvas element
 *                       backing this Layer will be given this width.
 *                       
 * @param {Number} height The height of the Layer, in pixels. The canvas element
 *                        backing this Layer will be given this height.
 */
Guacamole.Layer = function(width, height) {

    /**
     * Reference to this Layer.
     * @private
     */
    var layer = this;

    /**
     * The canvas element backing this Layer.
     * @private
     */
    var display = document.createElement("canvas");

    /**
     * The 2D display context of the canvas element backing this Layer.
     * @private
     */
    var displayContext = display.getContext("2d");
    displayContext.save();

    /**
     * The queue of all pending Tasks. Tasks will be run in order, with new
     * tasks added at the end of the queue and old tasks removed from the
     * front of the queue (FIFO).
     * @private
     */
    var tasks = new Array();

    /**
     * Whether a new path should be started with the next path drawing
     * operations.
     * @private
     */
    var pathClosed = true;

    /**
     * The number of states on the state stack.
     * 
     * Note that there will ALWAYS be one element on the stack, but that
     * element is not exposed. It is only used to reset the layer to its
     * initial state.
     * 
     * @private
     */
    var stackSize = 0;

    /**
     * Map of all Guacamole channel masks to HTML5 canvas composite operation
     * names. Not all channel mask combinations are currently implemented.
     * @private
     */
    var compositeOperation = {
     /* 0x0 NOT IMPLEMENTED */
        0x1: "destination-in",
        0x2: "destination-out",
     /* 0x3 NOT IMPLEMENTED */
        0x4: "source-in",
     /* 0x5 NOT IMPLEMENTED */
        0x6: "source-atop",
     /* 0x7 NOT IMPLEMENTED */
        0x8: "source-out",
        0x9: "destination-atop",
        0xA: "xor",
        0xB: "destination-over",
        0xC: "copy",
     /* 0xD NOT IMPLEMENTED */
        0xE: "source-over",
        0xF: "lighter"
    };

    /**
     * Resizes the canvas element backing this Layer without testing the
     * new size. This function should only be used internally.
     * 
     * @private
     * @param {Number} newWidth The new width to assign to this Layer.
     * @param {Number} newHeight The new height to assign to this Layer.
     */
    function resize(newWidth, newHeight) {

        // Only preserve old data if width/height are both non-zero
        var oldData = null;
        if (width != 0 && height != 0) {

            // Create canvas and context for holding old data
            oldData = document.createElement("canvas");
            oldData.width = width;
            oldData.height = height;

            var oldDataContext = oldData.getContext("2d");

            // Copy image data from current
            oldDataContext.drawImage(display,
                    0, 0, width, height,
                    0, 0, width, height);

        }

        // Preserve composite operation
        var oldCompositeOperation = displayContext.globalCompositeOperation;

        // Resize canvas
        display.width = newWidth;
        display.height = newHeight;

        // Redraw old data, if any
        if (oldData)
                displayContext.drawImage(oldData, 
                    0, 0, width, height,
                    0, 0, width, height);

        // Restore composite operation
        displayContext.globalCompositeOperation = oldCompositeOperation;

        width = newWidth;
        height = newHeight;

        // Acknowledge reset of stack (happens on resize of canvas)
        stackSize = 0;
        displayContext.save();

    }

    /**
     * Given the X and Y coordinates of the upper-left corner of a rectangle
     * and the rectangle's width and height, resize the backing canvas element
     * as necessary to ensure that the rectangle fits within the canvas
     * element's coordinate space. This function will only make the canvas
     * larger. If the rectangle already fits within the canvas element's
     * coordinate space, the canvas is left unchanged.
     * 
     * @private
     * @param {Number} x The X coordinate of the upper-left corner of the
     *                   rectangle to fit.
     * @param {Number} y The Y coordinate of the upper-left corner of the
     *                   rectangle to fit.
     * @param {Number} w The width of the the rectangle to fit.
     * @param {Number} h The height of the the rectangle to fit.
     */
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

    /**
     * A container for an task handler. Each operation which must be ordered
     * is associated with a Task that goes into a task queue. Tasks in this
     * queue are executed in order once their handlers are set, while Tasks 
     * without handlers block themselves and any following Tasks from running.
     *
     * @constructor
     * @private
     * @param {function} taskHandler The function to call when this task 
     *                               runs, if any.
     * @param {boolean} blocked Whether this task should start blocked.
     */
    function Task(taskHandler, blocked) {
       
        var task = this;
       
        /**
         * Whether this Task is blocked.
         * 
         * @type boolean
         */
        this.blocked = blocked;

        /**
         * The handler this Task is associated with, if any.
         * 
         * @type function
         */
        this.handler = taskHandler;
       
        /**
         * Unblocks this Task, allowing it to run.
         */
        this.unblock = function() {
            if (task.blocked) {
                task.blocked = false;
                handlePendingTasks();
            }
        }

    }

    /**
     * If no tasks are pending or running, run the provided handler immediately,
     * if any. Otherwise, schedule a task to run immediately after all currently
     * running or pending tasks are complete.
     * 
     * @private
     * @param {function} handler The function to call when possible, if any.
     * @param {boolean} blocked Whether the task should start blocked.
     * @returns {Task} The Task created and added to the queue for future
     *                 running, if any, or null if the handler was run
     *                 immediately and no Task needed to be created.
     */
    function scheduleTask(handler, blocked) {
        
        // If no pending tasks, just call (if available) and exit
        if (layer.isReady() && !blocked) {
            if (handler) handler();
            return null;
        }

        // If tasks are pending/executing, schedule a pending task
        // and return a reference to it.
        var task = new Task(handler, blocked);
        tasks.push(task);
        return task;
        
    }

    var tasksInProgress = false;

    /**
     * Run any Tasks which were pending but are now ready to run and are not
     * blocked by other Tasks.
     * @private
     */
    function handlePendingTasks() {

        if (tasksInProgress)
            return;

        tasksInProgress = true;

        // Draw all pending tasks.
        var task;
        while ((task = tasks[0]) != null && !task.blocked) {
            tasks.shift();
            if (task.handler) task.handler();
        }

        tasksInProgress = false;

    }

    /**
     * Schedules a task within the current layer just as scheduleTast() does,
     * except that another specified layer will be blocked until this task
     * completes, and this task will not start until the other layer is
     * ready.
     * 
     * Essentially, a task is scheduled in both layers, and the specified task
     * will only be performed once both layers are ready, and neither layer may
     * proceed until this task completes.
     * 
     * Note that there is no way to specify whether the task starts blocked,
     * as whether the task is blocked depends completely on whether the
     * other layer is currently ready.
     * 
     * @private
     * @param {Guacamole.Layer} otherLayer The other layer which must be blocked
     *                          until this task completes.
     * @param {function} handler The function to call when possible.
     */
    function scheduleTaskSynced(otherLayer, handler) {

        // If we ARE the other layer, no need to sync.
        // Syncing would result in deadlock.
        if (layer === otherLayer)
            scheduleTask(handler);

        // Otherwise synchronize operation with other layer
        else {

            var drawComplete = false;
            var layerLock = null;

            function performTask() {

                // Perform task
                handler();

                // Unblock the other layer now that draw is complete
                if (layerLock != null) 
                    layerLock.unblock();

                // Flag operation as done
                drawComplete = true;

            }

            // Currently blocked draw task
            var task = scheduleTask(performTask, true);

            // Unblock draw task once source layer is ready
            otherLayer.sync(task.unblock);

            // Block other layer until draw completes
            // Note that the draw MAY have already been performed at this point,
            // in which case creating a lock on the other layer will lead to
            // deadlock (the draw task has already run and will thus never
            // clear the lock)
            if (!drawComplete)
                layerLock = otherLayer.sync(null, true);

        }
    }

    /**
     * Set to true if this Layer should resize itself to accomodate the
     * dimensions of any drawing operation, and false (the default) otherwise.
     * 
     * Note that setting this property takes effect immediately, and thus may
     * take effect on operations that were started in the past but have not
     * yet completed. If you wish the setting of this flag to only modify
     * future operations, you will need to make the setting of this flag an
     * operation with sync().
     * 
     * @example
     * // Set autosize to true for all future operations
     * layer.sync(function() {
     *     layer.autosize = true;
     * });
     * 
     * @type Boolean
     * @default false
     */
    this.autosize = false;

    /**
     * Returns the canvas element backing this Layer.
     * @returns {Element} The canvas element backing this Layer.
     */
    this.getCanvas = function() {
        return display;
    };

    /**
     * Returns whether this Layer is ready. A Layer is ready if it has no
     * pending operations and no operations in-progress.
     * 
     * @returns {Boolean} true if this Layer is ready, false otherwise.
     */
    this.isReady = function() {
        return tasks.length == 0;
    };

    /**
     * Changes the size of this Layer to the given width and height. Resizing
     * is only attempted if the new size provided is actually different from
     * the current size.
     * 
     * @param {Number} newWidth The new width to assign to this Layer.
     * @param {Number} newHeight The new height to assign to this Layer.
     */
    this.resize = function(newWidth, newHeight) {
        scheduleTask(function() {
            if (newWidth != width || newHeight != height)
                resize(newWidth, newHeight);
        });
    };

    /**
     * Draws the specified image at the given coordinates. The image specified
     * must already be loaded.
     * 
     * @param {Number} x The destination X coordinate.
     * @param {Number} y The destination Y coordinate.
     * @param {Image} image The image to draw. Note that this is an Image
     *                      object - not a URL.
     */
    this.drawImage = function(x, y, image) {
        scheduleTask(function() {
            if (layer.autosize != 0) fitRect(x, y, image.width, image.height);
            displayContext.drawImage(image, x, y);
        });
    };

    /**
     * Draws the image at the specified URL at the given coordinates. The image
     * will be loaded automatically, and this and any future operations will
     * wait for the image to finish loading.
     * 
     * @param {Number} x The destination X coordinate.
     * @param {Number} y The destination Y coordinate.
     * @param {String} url The URL of the image to draw.
     */
    this.draw = function(x, y, url) {

        var task = scheduleTask(function() {
            if (layer.autosize != 0) fitRect(x, y, image.width, image.height);
            displayContext.drawImage(image, x, y);
        }, true);

        var image = new Image();
        image.onload = task.unblock;
        image.src = url;

    };

    /**
     * Plays the video at the specified URL within this layer. The video
     * will be loaded automatically, and this and any future operations will
     * wait for the video to finish loading. Future operations will not be
     * executed until the video finishes playing.
     * 
     * @param {String} mimetype The mimetype of the video to play.
     * @param {Number} duration The duration of the video in milliseconds.
     * @param {String} url The URL of the video to play.
     */
    this.play = function(mimetype, duration, url) {

        // Start loading the video
        var video = document.createElement("video");
        video.type = mimetype;
        video.src = url;

        // Main task - playing the video
        var task = scheduleTask(function() {
            video.play();
        }, true);

        // Lock which will be cleared after video ends
        var lock = scheduleTask(null, true);

        // Start copying frames when playing
        video.addEventListener("play", function() {
            
            function render_callback() {
                displayContext.drawImage(video, 0, 0, width, height);
                if (!video.ended)
                    window.setTimeout(render_callback, 20);
                else
                    lock.unblock();
            }
            
            render_callback();
            
        }, false);

        // Unblock future operations after an error
        video.addEventListener("error", lock.unblock, false);

        // Play video as soon as current tasks are complete, now that the
        // lock has been set up.
        task.unblock();

    };

    /**
     * Run an arbitrary function as soon as currently pending operations
     * are complete.
     * 
     * @param {function} handler The function to call once all currently
     *                           pending operations are complete.
     * @param {boolean} blocked Whether the task should start blocked.
     */
    this.sync = scheduleTask;

    /**
     * Transfer a rectangle of image data from one Layer to this Layer using the
     * specified transfer function.
     * 
     * @param {Guacamole.Layer} srcLayer The Layer to copy image data from.
     * @param {Number} srcx The X coordinate of the upper-left corner of the
     *                      rectangle within the source Layer's coordinate
     *                      space to copy data from.
     * @param {Number} srcy The Y coordinate of the upper-left corner of the
     *                      rectangle within the source Layer's coordinate
     *                      space to copy data from.
     * @param {Number} srcw The width of the rectangle within the source Layer's
     *                      coordinate space to copy data from.
     * @param {Number} srch The height of the rectangle within the source
     *                      Layer's coordinate space to copy data from.
     * @param {Number} x The destination X coordinate.
     * @param {Number} y The destination Y coordinate.
     * @param {Function} transferFunction The transfer function to use to
     *                                    transfer data from source to
     *                                    destination.
     */
    this.transfer = function(srcLayer, srcx, srcy, srcw, srch, x, y, transferFunction) {
        scheduleTaskSynced(srcLayer, function() {

            var srcCanvas = srcLayer.getCanvas();

            // If entire rectangle outside source canvas, stop
            if (srcx >= srcCanvas.width || srcy >= srcCanvas.height) return;

            // Otherwise, clip rectangle to area
            if (srcx + srcw > srcCanvas.width)
                srcw = srcCanvas.width - srcx;

            if (srcy + srch > srcCanvas.height)
                srch = srcCanvas.height - srcy;

            // Stop if nothing to draw.
            if (srcw == 0 || srch == 0) return;

            if (layer.autosize != 0) fitRect(x, y, srcw, srch);

            // Get image data from src and dst
            var src = srcLayer.getCanvas().getContext("2d").getImageData(srcx, srcy, srcw, srch);
            var dst = displayContext.getImageData(x , y, srcw, srch);

            // Apply transfer for each pixel
            for (var i=0; i<srcw*srch*4; i+=4) {

                // Get source pixel environment
                var src_pixel = new Guacamole.Layer.Pixel(
                    src.data[i],
                    src.data[i+1],
                    src.data[i+2],
                    src.data[i+3]
                );
                    
                // Get destination pixel environment
                var dst_pixel = new Guacamole.Layer.Pixel(
                    dst.data[i],
                    dst.data[i+1],
                    dst.data[i+2],
                    dst.data[i+3]
                );

                // Apply transfer function
                transferFunction(src_pixel, dst_pixel);

                // Save pixel data
                dst.data[i  ] = dst_pixel.red;
                dst.data[i+1] = dst_pixel.green;
                dst.data[i+2] = dst_pixel.blue;
                dst.data[i+3] = dst_pixel.alpha;

            }

            // Draw image data
            displayContext.putImageData(dst, x, y);

        });
    };

    /**
     * Copy a rectangle of image data from one Layer to this Layer. This
     * operation will copy exactly the image data that will be drawn once all
     * operations of the source Layer that were pending at the time this
     * function was called are complete. This operation will not alter the
     * size of the source Layer even if its autosize property is set to true.
     * 
     * @param {Guacamole.Layer} srcLayer The Layer to copy image data from.
     * @param {Number} srcx The X coordinate of the upper-left corner of the
     *                      rectangle within the source Layer's coordinate
     *                      space to copy data from.
     * @param {Number} srcy The Y coordinate of the upper-left corner of the
     *                      rectangle within the source Layer's coordinate
     *                      space to copy data from.
     * @param {Number} srcw The width of the rectangle within the source Layer's
     *                      coordinate space to copy data from.
     * @param {Number} srch The height of the rectangle within the source
     *                      Layer's coordinate space to copy data from.
     * @param {Number} x The destination X coordinate.
     * @param {Number} y The destination Y coordinate.
     */
    this.copy = function(srcLayer, srcx, srcy, srcw, srch, x, y) {
        scheduleTaskSynced(srcLayer, function() {

            var srcCanvas = srcLayer.getCanvas();

            // If entire rectangle outside source canvas, stop
            if (srcx >= srcCanvas.width || srcy >= srcCanvas.height) return;

            // Otherwise, clip rectangle to area
            if (srcx + srcw > srcCanvas.width)
                srcw = srcCanvas.width - srcx;

            if (srcy + srch > srcCanvas.height)
                srch = srcCanvas.height - srcy;

            // Stop if nothing to draw.
            if (srcw == 0 || srch == 0) return;

            if (layer.autosize != 0) fitRect(x, y, srcw, srch);
            displayContext.drawImage(srcCanvas, srcx, srcy, srcw, srch, x, y, srcw, srch);

        });
    };

    /**
     * Starts a new path at the specified point.
     * 
     * @param {Number} x The X coordinate of the point to draw.
     * @param {Number} y The Y coordinate of the point to draw.
     */
    this.moveTo = function(x, y) {
        scheduleTask(function() {
            
            // Start a new path if current path is closed
            if (pathClosed) {
                displayContext.beginPath();
                pathClosed = false;
            }
            
            if (layer.autosize != 0) fitRect(x, y, 0, 0);
            displayContext.moveTo(x, y);
            
        });
    };

    /**
     * Add the specified line to the current path.
     * 
     * @param {Number} x The X coordinate of the endpoint of the line to draw.
     * @param {Number} y The Y coordinate of the endpoint of the line to draw.
     */
    this.lineTo = function(x, y) {
        scheduleTask(function() {
            
            // Start a new path if current path is closed
            if (pathClosed) {
                displayContext.beginPath();
                pathClosed = false;
            }
            
            if (layer.autosize != 0) fitRect(x, y, 0, 0);
            displayContext.lineTo(x, y);
            
        });
    };

    /**
     * Add the specified arc to the current path.
     * 
     * @param {Number} x The X coordinate of the center of the circle which
     *                   will contain the arc.
     * @param {Number} y The Y coordinate of the center of the circle which
     *                   will contain the arc.
     * @param {Number} radius The radius of the circle.
     * @param {Number} startAngle The starting angle of the arc, in radians.
     * @param {Number} endAngle The ending angle of the arc, in radians.
     * @param {Boolean} negative Whether the arc should be drawn in order of
     *                           decreasing angle.
     */
    this.arc = function(x, y, radius, startAngle, endAngle, negative) {
        scheduleTask(function() {
            
            // Start a new path if current path is closed
            if (pathClosed) {
                displayContext.beginPath();
                pathClosed = false;
            }
            
            if (layer.autosize != 0) fitRect(x, y, 0, 0);
            displayContext.arc(x, y, radius, startAngle, endAngle, negative);
            
        });
    };

    /**
     * Starts a new path at the specified point.
     * 
     * @param {Number} cp1x The X coordinate of the first control point.
     * @param {Number} cp1y The Y coordinate of the first control point.
     * @param {Number} cp2x The X coordinate of the second control point.
     * @param {Number} cp2y The Y coordinate of the second control point.
     * @param {Number} x The X coordinate of the endpoint of the curve.
     * @param {Number} y The Y coordinate of the endpoint of the curve.
     */
    this.curveTo = function(cp1x, cp1y, cp2x, cp2y, x, y) {
        scheduleTask(function() {
            
            // Start a new path if current path is closed
            if (pathClosed) {
                displayContext.beginPath();
                pathClosed = false;
            }
            
            if (layer.autosize != 0) fitRect(x, y, 0, 0);
            displayContext.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, x, y);
            
        });
    };

    /**
     * Closes the current path by connecting the end point with the start
     * point (if any) with a straight line.
     */
    this.close = function() {
        scheduleTask(function() {
            
            // Close path
            displayContext.closePath();
            pathClosed = true;
            
        });
    };

    /**
     * Add the specified rectangle to the current path.
     * 
     * @param {Number} x The X coordinate of the upper-left corner of the
     *                   rectangle to draw.
     * @param {Number} y The Y coordinate of the upper-left corner of the
     *                   rectangle to draw.
     * @param {Number} w The width of the rectangle to draw.
     * @param {Number} h The height of the rectangle to draw.
     */
    this.rect = function(x, y, w, h) {
        scheduleTask(function() {
            
            // Start a new path if current path is closed
            if (pathClosed) {
                displayContext.beginPath();
                pathClosed = false;
            }
            
            if (layer.autosize != 0) fitRect(x, y, w, h);
            displayContext.rect(x, y, w, h);
            
        });
    };

    /**
     * Clip all future drawing operations by the current path. The current path
     * is implicitly closed. The current path can continue to be reused
     * for other operations (such as fillColor()) but a new path will be started
     * once a path drawing operation (path() or rect()) is used.
     */
    this.clip = function() {
        scheduleTask(function() {

            // Set new clipping region
            displayContext.clip();

            // Path now implicitly closed
            pathClosed = true;

        });
    };

    /**
     * Stroke the current path with the specified color. The current path
     * is implicitly closed. The current path can continue to be reused
     * for other operations (such as clip()) but a new path will be started
     * once a path drawing operation (path() or rect()) is used.
     * 
     * @param {String} cap The line cap style. Can be "round", "square",
     *                     or "butt".
     * @param {String} join The line join style. Can be "round", "bevel",
     *                      or "miter".
     * @param {Number} thickness The line thickness in pixels.
     * @param {Number} r The red component of the color to fill.
     * @param {Number} g The green component of the color to fill.
     * @param {Number} b The blue component of the color to fill.
     * @param {Number} a The alpha component of the color to fill.
     */
    this.strokeColor = function(cap, join, thickness, r, g, b, a) {
        scheduleTask(function() {

            // Stroke with color
            displayContext.lineCap = cap;
            displayContext.lineJoin = join;
            displayContext.lineWidth = thickness;
            displayContext.strokeStyle = "rgba(" + r + "," + g + "," + b + "," + a/255.0 + ")";
            displayContext.stroke();

            // Path now implicitly closed
            pathClosed = true;

        });
    };

    /**
     * Fills the current path with the specified color. The current path
     * is implicitly closed. The current path can continue to be reused
     * for other operations (such as clip()) but a new path will be started
     * once a path drawing operation (path() or rect()) is used.
     * 
     * @param {Number} r The red component of the color to fill.
     * @param {Number} g The green component of the color to fill.
     * @param {Number} b The blue component of the color to fill.
     * @param {Number} a The alpha component of the color to fill.
     */
    this.fillColor = function(r, g, b, a) {
        scheduleTask(function() {

            // Fill with color
            displayContext.fillStyle = "rgba(" + r + "," + g + "," + b + "," + a/255.0 + ")";
            displayContext.fill();

            // Path now implicitly closed
            pathClosed = true;

        });
    };

    /**
     * Stroke the current path with the image within the specified layer. The
     * image data will be tiled infinitely within the stroke. The current path
     * is implicitly closed. The current path can continue to be reused
     * for other operations (such as clip()) but a new path will be started
     * once a path drawing operation (path() or rect()) is used.
     * 
     * @param {String} cap The line cap style. Can be "round", "square",
     *                     or "butt".
     * @param {String} join The line join style. Can be "round", "bevel",
     *                      or "miter".
     * @param {Number} thickness The line thickness in pixels.
     * @param {Guacamole.Layer} srcLayer The layer to use as a repeating pattern
     *                                   within the stroke.
     */
    this.strokeLayer = function(cap, join, thickness, srcLayer) {
        scheduleTaskSynced(srcLayer, function() {

            // Stroke with image data
            displayContext.lineCap = cap;
            displayContext.lineJoin = join;
            displayContext.lineWidth = thickness;
            displayContext.strokeStyle = displayContext.createPattern(
                srcLayer.getCanvas(),
                "repeat"
            );
            displayContext.stroke();

            // Path now implicitly closed
            pathClosed = true;

        });
    };

    /**
     * Fills the current path with the image within the specified layer. The
     * image data will be tiled infinitely within the stroke. The current path
     * is implicitly closed. The current path can continue to be reused
     * for other operations (such as clip()) but a new path will be started
     * once a path drawing operation (path() or rect()) is used.
     * 
     * @param {Guacamole.Layer} srcLayer The layer to use as a repeating pattern
     *                                   within the fill.
     */
    this.fillLayer = function(srcLayer) {
        scheduleTask(function() {

            // Fill with image data 
            displayContext.fillStyle = displayContext.createPattern(
                srcLayer.getCanvas(),
                "repeat"
            );
            displayContext.fill();

            // Path now implicitly closed
            pathClosed = true;

        });
    };

    /**
     * Push current layer state onto stack.
     */
    this.push = function() {
        scheduleTask(function() {

            // Save current state onto stack
            displayContext.save();
            stackSize++;

        });
    };

    /**
     * Pop layer state off stack.
     */
    this.pop = function() {
        scheduleTask(function() {

            // Restore current state from stack
            if (stackSize > 0) {
                displayContext.restore();
                stackSize--;
            }

        });
    };

    /**
     * Reset the layer, clearing the stack, the current path, and any transform
     * matrix.
     */
    this.reset = function() {
        scheduleTask(function() {

            // Clear stack
            while (stackSize > 0) {
                displayContext.restore();
                stackSize--;
            }

            // Restore to initial state
            displayContext.restore();
            displayContext.save();

            // Clear path
            displayContext.beginPath();
            pathClosed = false;

        });
    };

    /**
     * Sets the given affine transform (defined with six values from the
     * transform's matrix).
     * 
     * @param {Number} a The first value in the affine transform's matrix.
     * @param {Number} b The second value in the affine transform's matrix.
     * @param {Number} c The third value in the affine transform's matrix.
     * @param {Number} d The fourth value in the affine transform's matrix.
     * @param {Number} e The fifth value in the affine transform's matrix.
     * @param {Number} f The sixth value in the affine transform's matrix.
     */
    this.setTransform = function(a, b, c, d, e, f) {
        scheduleTask(function() {

            // Set transform
            displayContext.setTransform(
                a, b, c,
                d, e, f
              /*0, 0, 1*/
            );

        });
    };


    /**
     * Applies the given affine transform (defined with six values from the
     * transform's matrix).
     * 
     * @param {Number} a The first value in the affine transform's matrix.
     * @param {Number} b The second value in the affine transform's matrix.
     * @param {Number} c The third value in the affine transform's matrix.
     * @param {Number} d The fourth value in the affine transform's matrix.
     * @param {Number} e The fifth value in the affine transform's matrix.
     * @param {Number} f The sixth value in the affine transform's matrix.
     */
    this.transform = function(a, b, c, d, e, f) {
        scheduleTask(function() {

            // Apply transform
            displayContext.transform(
                a, b, c,
                d, e, f
              /*0, 0, 1*/
            );

        });
    };


    /**
     * Sets the channel mask for future operations on this Layer.
     * 
     * The channel mask is a Guacamole-specific compositing operation identifier
     * with a single bit representing each of four channels (in order): source
     * image where destination transparent, source where destination opaque,
     * destination where source transparent, and destination where source
     * opaque.
     * 
     * @param {Number} mask The channel mask for future operations on this
     *                      Layer.
     */
    this.setChannelMask = function(mask) {
        scheduleTask(function() {
            displayContext.globalCompositeOperation = compositeOperation[mask];
        });
    };

    /**
     * Sets the miter limit for stroke operations using the miter join. This
     * limit is the maximum ratio of the size of the miter join to the stroke
     * width. If this ratio is exceeded, the miter will not be drawn for that
     * joint of the path.
     * 
     * @param {Number} limit The miter limit for stroke operations using the
     *                       miter join.
     */
    this.setMiterLimit = function(limit) {
        scheduleTask(function() {
            displayContext.miterLimit = limit;
        });
    };

    // Initialize canvas dimensions
    display.width = width;
    display.height = height;

};

/**
 * Channel mask for the composite operation "rout".
 */
Guacamole.Layer.ROUT  = 0x2;

/**
 * Channel mask for the composite operation "atop".
 */
Guacamole.Layer.ATOP  = 0x6;

/**
 * Channel mask for the composite operation "xor".
 */
Guacamole.Layer.XOR   = 0xA;

/**
 * Channel mask for the composite operation "rover".
 */
Guacamole.Layer.ROVER = 0xB;

/**
 * Channel mask for the composite operation "over".
 */
Guacamole.Layer.OVER  = 0xE;

/**
 * Channel mask for the composite operation "plus".
 */
Guacamole.Layer.PLUS  = 0xF;

/**
 * Channel mask for the composite operation "rin".
 * Beware that WebKit-based browsers may leave the contents of the destionation
 * layer where the source layer is transparent, despite the definition of this
 * operation.
 */
Guacamole.Layer.RIN   = 0x1;

/**
 * Channel mask for the composite operation "in".
 * Beware that WebKit-based browsers may leave the contents of the destionation
 * layer where the source layer is transparent, despite the definition of this
 * operation.
 */
Guacamole.Layer.IN    = 0x4;

/**
 * Channel mask for the composite operation "out".
 * Beware that WebKit-based browsers may leave the contents of the destionation
 * layer where the source layer is transparent, despite the definition of this
 * operation.
 */
Guacamole.Layer.OUT   = 0x8;

/**
 * Channel mask for the composite operation "ratop".
 * Beware that WebKit-based browsers may leave the contents of the destionation
 * layer where the source layer is transparent, despite the definition of this
 * operation.
 */
Guacamole.Layer.RATOP = 0x9;

/**
 * Channel mask for the composite operation "src".
 * Beware that WebKit-based browsers may leave the contents of the destionation
 * layer where the source layer is transparent, despite the definition of this
 * operation.
 */
Guacamole.Layer.SRC   = 0xC;


/**
 * Represents a single pixel of image data. All components have a minimum value
 * of 0 and a maximum value of 255.
 * 
 * @constructor
 * 
 * @param {Number} r The red component of this pixel.
 * @param {Number} g The green component of this pixel.
 * @param {Number} b The blue component of this pixel.
 * @param {Number} a The alpha component of this pixel.
 */
Guacamole.Layer.Pixel = function(r, g, b, a) {

    /**
     * The red component of this pixel, where 0 is the minimum value,
     * and 255 is the maximum.
     */
    this.red   = r;

    /**
     * The green component of this pixel, where 0 is the minimum value,
     * and 255 is the maximum.
     */
    this.green = g;

    /**
     * The blue component of this pixel, where 0 is the minimum value,
     * and 255 is the maximum.
     */
    this.blue  = b;

    /**
     * The alpha component of this pixel, where 0 is the minimum value,
     * and 255 is the maximum.
     */
    this.alpha = a;

};
