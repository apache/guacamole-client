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

import { provideHttpClient, withInterceptorsFromDi, withXhr } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ChangeDetectionStrategy, Component, TemplateRef, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { ActiveConnection } from '../../../rest/types/ActiveConnection';
import { Connection } from '../../../rest/types/Connection';
import { ConnectionGroup } from '../../../rest/types/ConnectionGroup';
import { GroupListModule } from '../../group-list.module';
import { GroupListItem } from '../../types/GroupListItem';

/**
 * Host component which renders a guac-group-list with one template per
 * item type, each of which displays the name of the item. The template for
 * connection groups additionally displays the number of available
 * connections.
 */
@Component({
    template  : `
        <ng-template #connection let-item="item"><span class="name">{{ item.name }}</span></ng-template>
        <ng-template #connectionGroup let-item="item">
            <span class="name">{{ item.name }}</span>
            <span class="available">{{ item.getAvailableConnections() }}</span>
        </ng-template>
        <guac-group-list [connectionGroups]="connectionGroups"
                         [templates]="templates"
                         [showRootGroup]="showRootGroup"/>
    `,
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
class HostComponent {

    /**
     * The template for connections.
     */
    @ViewChild('connection', { static: true }) connection!: TemplateRef<unknown>;

    /**
     * The template for connection groups.
     */
    @ViewChild('connectionGroup', { static: true }) connectionGroup!: TemplateRef<unknown>;

    /**
     * The root group to display, by data source identifier.
     */
    connectionGroups: Record<string, ConnectionGroup | GroupListItem> = {};

    /**
     * Whether the root group itself is displayed, rather than only its
     * children.
     */
    showRootGroup = true;

    /**
     * The templates to render items with, by item type.
     */
    get templates(): Record<string, TemplateRef<unknown>> {
        return {
            [GroupListItem.Type.CONNECTION]      : this.connection,
            [GroupListItem.Type.CONNECTION_GROUP]: this.connectionGroup
        };
    }

}

describe('GuacGroupListComponent', () => {

    let fixture: ComponentFixture<HostComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [HostComponent],
            imports     : [GroupListModule],
            providers   : [
                provideHttpClient(withXhr(), withInterceptorsFromDi()),
                provideHttpClientTesting(),
                { provide: AuthenticationService, useValue: { getCurrentUsername: () => 'alice' } }
            ]
        });

        fixture = TestBed.createComponent(HostComponent);
    });

    /**
     * Creates a connection group item which is already expanded.
     *
     * @param name
     *     The name of the group.
     *
     * @param children
     *     The items within the group.
     *
     * @returns
     *     The expanded connection group item.
     */
    function expandedGroup(name: string, children: GroupListItem[]): GroupListItem {
        return new GroupListItem({
            dataSource: 'test',
            name      : name,
            type      : GroupListItem.Type.CONNECTION_GROUP,
            expandable: true,
            expanded  : true,
            children  : children,
            wrappedItem: null
        });
    }

    /**
     * Creates a connection item.
     *
     * @param name
     *     The name of the connection.
     *
     * @returns
     *     The connection item.
     */
    function connection(name: string): GroupListItem {
        return new GroupListItem({
            dataSource: 'test',
            name      : name,
            type      : GroupListItem.Type.CONNECTION,
            wrappedItem: null
        });
    }

    /**
     * Returns the names of the children of the root group, in the order
     * in which they are displayed.
     */
    function displayedChildNames(): string[] {
        const names: HTMLElement[] = Array.from(
            fixture.nativeElement.querySelectorAll('.children > .list-item .name')
        );
        return names.map(name => name.textContent!.trim());
    }

    it('should sort the children of an expanded group without regard to case', () => {
        fixture.componentInstance.connectionGroups = {
            test: expandedGroup('Group A', [
                expandedGroup('Group B', []),
                connection('alpha-2'),
                connection('alpha-1'),
            ])
        };

        fixture.detectChanges();

        expect(displayedChildNames()).toEqual(['alpha-1', 'alpha-2', 'Group B']);
    });

    describe('with a balancing group', () => {

        /**
         * Creates a root group containing a balancing group "balancer" with
         * the connections "a" (at most 3 connections) and "b" (at most 2
         * connections), and a nested balancing group "inner" with the
         * connection "c".
         *
         * @returns
         *     The root group.
         */
        function rootGroup(): ConnectionGroup {
            return new ConnectionGroup({
                identifier           : ConnectionGroup.ROOT_IDENTIFIER,
                name                 : ConnectionGroup.ROOT_IDENTIFIER,
                activeConnections    : 0,
                childConnectionGroups: [new ConnectionGroup({
                    identifier           : 'balancer',
                    name                 : 'balancer',
                    type                 : ConnectionGroup.Type.BALANCING,
                    activeConnections    : 0,
                    childConnections     : [
                        new Connection({ identifier: 'a', name: 'a', protocol: 'ssh', attributes: { 'max-connections': '3' } }),
                        new Connection({ identifier: 'b', name: 'b', protocol: 'ssh', attributes: { 'max-connections': '2' } })
                    ],
                    childConnectionGroups: [new ConnectionGroup({
                        identifier       : 'inner',
                        name             : 'inner',
                        type             : ConnectionGroup.Type.BALANCING,
                        activeConnections: 0,
                        childConnections : [new Connection({ identifier: 'c', name: 'c', protocol: 'ssh' })]
                    })]
                })]
            });
        }

        /**
         * Answers the pending request for the active connections of the data
         * source "test": another user uses connection "a" once, the current
         * user "alice" uses connection "c" of the nested group twice.
         */
        function flushActiveConnections(): void {
            const activeConnections: Record<string, ActiveConnection> = {
                '1': new ActiveConnection({ identifier: '1', connectionIdentifier: 'a', username: 'bob' }),
                '2': new ActiveConnection({ identifier: '2', connectionIdentifier: 'c', username: 'alice' }),
                '3': new ActiveConnection({ identifier: '3', connectionIdentifier: 'c', username: 'alice' })
            };

            TestBed.inject(HttpTestingController)
                .expectOne('api/session/data/test/activeConnections')
                .flush(activeConnections);
        }

        /**
         * Returns the number of available connections displayed for the
         * balancing group "balancer", once the active connections are
         * counted. The promise of DataSourceService.apply() is no pending task
         * of Angular, so fixture.whenStable() does not wait for it.
         */
        async function displayedAvailableConnections(): Promise<string> {
            await new Promise(resolve => setTimeout(resolve));
            fixture.detectChanges();

            const caption: HTMLElement = Array.from<HTMLElement>(fixture.nativeElement.querySelectorAll('.caption'))
                .find(caption => caption.querySelector('.name')!.textContent!.trim() === 'balancer')!;
            return caption.querySelector('.available')!.textContent!.trim();
        }

        beforeEach(() => {
            fixture.componentInstance.showRootGroup = false;
            fixture.componentInstance.connectionGroups = { test: rootGroup() };
            fixture.detectChanges();
        });

        it('should subtract the connections of the current user through nested groups', async () => {
            flushActiveConnections();

            // 5 connections at most, 2 of which are used by alice
            expect(await displayedAvailableConnections()).toBe('3');
        });

        it('should count the connections of the current user anew when the groups change', async () => {
            flushActiveConnections();
            await displayedAvailableConnections();

            fixture.componentInstance.connectionGroups = { test: rootGroup() };
            fixture.detectChanges();
            flushActiveConnections();

            expect(await displayedAvailableConnections()).toBe('3');
        });

    });

});
