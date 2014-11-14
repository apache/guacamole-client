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
 * A service for creating Guacamole tunnels.
 */
angular.module('client').factory('guacTunnelFactory', ['$rootScope', '$window',
        function guacTunnelFactory($rootScope, $window) {

    var service = {};

    /**
     * Returns a new Guacamole tunnel instance, using an implementation that is
     * supported by the web browser.
     *
     * @param {Scope} [$scope] The current scope. If ommitted, the root scope
     *                         will be used.
     * @returns {Guacamole.Tunnel} A new Guacamole tunnel instance.
     */
    service.getInstance = function getTunnelInstance($scope) {

        // Use root scope if no other scope provided
        $scope = $scope || $rootScope;

        var tunnel;
        
        // If WebSocket available, try to use it.
        if ($window.WebSocket)
            tunnel = new Guacamole.ChainedTunnel(
                new Guacamole.WebSocketTunnel('websocket-tunnel'),
                new Guacamole.HTTPTunnel('tunnel')
            );
        
        // If no WebSocket, then use HTTP.
        else
            tunnel = new Guacamole.HTTPTunnel('tunnel');
        
        // Fire events for tunnel errors
        tunnel.onerror = function onTunnelError(status) {
            $rootScope.$broadcast('guacTunnelError', tunnel, status.code);
        };
        
        // Fire events for tunnel state changes
        tunnel.onstatechange = function onTunnelStateChange(state) {
            
            switch (state) {
                
                case Guacamole.Tunnel.State.CONNECTING:
                    $rootScope.$broadcast('guacTunnelStateChange', tunnel, 'connecting');
                    break;
                
                case Guacamole.Tunnel.State.OPEN:
                    $rootScope.$broadcast('guacTunnelStateChange', tunnel, 'open');
                    break;
                
                case Guacamole.Tunnel.State.CLOSED:
                    $rootScope.$broadcast('guacTunnelStateChange', tunnel, 'closed');
                    break;
                
            }
            
        };
        
        return tunnel;
        
    };

    return service;

}]);
