/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, BaseRouteReuseStrategy } from '@angular/router';
import _ from 'lodash';

/**
 * Route reuse strategy which recreates the component of a route whenever the
 * path or query parameters of that route change, like ngRoute did for the
 * AngularJS version of this application. A route whose data sets
 * "reloadOnUrl" to false keeps its component, which then receives the new
 * parameters through its inputs, like the "reloadOnUrl" option of ngRoute.
 */
@Injectable()
export class ReloadOnUrlRouteReuseStrategy extends BaseRouteReuseStrategy {

    /**
     * Determines whether the currently-displayed route, including its
     * component, should be reused for the route being navigated to. Unlike
     * the default strategy, which compares only the route configurations,
     * this also requires the same path and query parameters, unless the
     * route sets "reloadOnUrl" to false within its data.
     *
     * @param future
     *     The route being navigated to.
     *
     * @param curr
     *     The currently-displayed route.
     *
     * @returns
     *     true if the current route should be reused, false if its component
     *     should be recreated.
     */
    override shouldReuseRoute(future: ActivatedRouteSnapshot, curr: ActivatedRouteSnapshot): boolean {

        // Routes with different configurations never share a component
        if (future.routeConfig !== curr.routeConfig)
            return false;

        // Always reuse the root of the router state, which has no
        // configuration, and routes which handle URL changes themselves
        if (!future.routeConfig || future.data['reloadOnUrl'] === false)
            return true;

        // Reload upon any change to the path or query parameters
        return _.isEqual(future.params, curr.params)
            && _.isEqual(future.queryParams, curr.queryParams);

    }

}
