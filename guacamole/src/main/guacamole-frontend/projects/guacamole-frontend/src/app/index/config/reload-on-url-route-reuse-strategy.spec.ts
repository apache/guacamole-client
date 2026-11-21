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

import { Component, Input } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter, RouteReuseStrategy, RouterOutlet, Routes, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { appRoutes } from '../../app-routing.module';
import { ReloadOnUrlRouteReuseStrategy } from './reload-on-url-route-reuse-strategy';

/**
 * Stands in for every routed component of the app, exposing the route
 * parameters which the router binds to its inputs.
 */
@Component({
    template: '<router-outlet></router-outlet>',
    imports: [RouterOutlet]
})
class RoutedComponent {

    /**
     * The "id" path parameter, if any.
     */
    @Input() id?: string;

    /**
     * The "dataSource" path parameter, if any.
     */
    @Input() dataSource?: string;

    /**
     * The "clone" query parameter, if any.
     */
    @Input() clone?: string;

}

/**
 * Copies the given routes, keeping paths, data and children but replacing
 * each component with RoutedComponent and dropping guards and titles.
 *
 * @param routes
 *     The routes to copy.
 *
 * @returns
 *     The copied routes.
 */
const stubRoutes = (routes: Routes): Routes => routes.map(route => ({
    path     : route.path,
    data     : route.data,
    component: RoutedComponent,
    children : route.children && stubRoutes(route.children)
}));

/**
 * Returns all routed components currently displayed, outermost first.
 *
 * @param harness
 *     The harness which displays the routed components.
 *
 * @returns
 *     The displayed routed components.
 */
const routedComponents = (harness: RouterTestingHarness): RoutedComponent[] =>
    harness.fixture.debugElement.queryAll(By.directive(RoutedComponent))
        .map(element => element.componentInstance);

describe('ReloadOnUrlRouteReuseStrategy', () => {

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                // Routes and input binding of the app routing module
                provideRouter(stubRoutes(appRoutes), withComponentInputBinding()),
                { provide: RouteReuseStrategy, useClass: ReloadOnUrlRouteReuseStrategy }
            ]
        });
    });

    it('should recreate the component of a route when a path parameter changes', async () => {
        const harness = await RouterTestingHarness.create();
        const before = await harness.navigateByUrl('/manage/postgresql/users/guacadmin', RoutedComponent);
        const after = await harness.navigateByUrl('/manage/postgresql/users/t54user', RoutedComponent);

        expect(after).not.toBe(before);
        expect(after.id).toBe('t54user');
    });

    it('should recreate the component of a route when a query parameter changes', async () => {
        const harness = await RouterTestingHarness.create();
        const before = await harness.navigateByUrl('/manage/postgresql/connections?parent=ROOT', RoutedComponent);
        const after = await harness.navigateByUrl('/manage/postgresql/connections?clone=1', RoutedComponent);

        expect(after).not.toBe(before);
        expect(after.clone).toBe('1');
    });

    it('should recreate a settings tab but keep the settings page when the data source of the tab changes', async () => {
        const harness = await RouterTestingHarness.create();
        await harness.navigateByUrl('/settings/postgresql/history');
        const [pageBefore, tabBefore] = routedComponents(harness);
        await harness.navigateByUrl('/settings/postgresql-shared/history');
        const [pageAfter, tabAfter] = routedComponents(harness);

        expect(pageAfter).toBe(pageBefore);
        expect(tabAfter).not.toBe(tabBefore);
        expect(tabAfter.dataSource).toBe('postgresql-shared');
    });

    it('should keep the client page when the connections within its URL change', async () => {
        const harness = await RouterTestingHarness.create();
        const before = await harness.navigateByUrl('/client/MQBjAHBvc3RncmVzcWw', RoutedComponent);
        const after = await harness.navigateByUrl('/client/MQBjAHBvc3RncmVzcWw.MgBjAHBvc3RncmVzcWw', RoutedComponent);
        await harness.fixture.whenStable();

        expect(after).toBe(before);
        expect(after.id).toBe('MQBjAHBvc3RncmVzcWw.MgBjAHBvc3RncmVzcWw');
    });

});
