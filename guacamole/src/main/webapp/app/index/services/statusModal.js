/*
 * Copyright (C) 2014 Glyptodon LLC
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

/**
 * A modal for displaying the current status.
 */
angular.module('manage').factory('statusModal', ['btfModal', 
        function statusModal(btfModal) {

    var service = {};

    // Create the modal object to be used later to actually create the modal
    var modalService = btfModal({
        controller: 'statusController',
        controllerAs: 'modal',
        templateUrl: 'app/index/templates/status.html'
    });

    /**
     * Whether the status modal is currently displayed.
     *
     * @type Boolean
     */
    service.shown = false;

    /**
     * Shows or hides the status modal.
     *
     * @param {Boolean|Object} status The status to show, or false to hide the
     *                                current status.
     * @param {String} [status.title] The title of the status modal.
     * @param {String} [status.text] The body text of the status modal.
     * @param {String} [status.className] The CSS class name to apply to the
     *                                    modal, in addition to the default
     *                                    "dialog" and "status" classes.
     * @param {String[]} [status.actions] Array of action names which
     *                                    correspond to button captions. Each
     *                                    action will be displayed as a button
     *                                    within the status modal. Clickin a
     *                                    button will fire a guacStatusAction
     *                                    event.
     */
    service.showStatus = function showStatus(status) {

        // Hide any existing status
        modalService.deactivate();
        service.shown = false;

        // Show new status if requested
        if (status) {
            modalService.activate(status);
            service.shown = true;
        }

    };

    return service;

}]);
