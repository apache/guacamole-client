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
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, TemplateRef, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { GroupListModule } from '../../group-list.module';
import { GroupListItem } from '../../types/GroupListItem';

/**
 * Host component which renders a guac-group-list with one template per
 * item type, each of which displays only the name of the item.
 */
@Component({
    template  : `
        <ng-template #connection let-item="item"><span class="name">{{ item.name }}</span></ng-template>
        <ng-template #connectionGroup let-item="item"><span class="name">{{ item.name }}</span></ng-template>
        <guac-group-list [connectionGroups]="connectionGroups"
                         [templates]="templates"
                         [showRootGroup]="true"/>
    `,
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
    connectionGroups: Record<string, GroupListItem> = {};

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
            providers   : [provideHttpClient(withXhr(), withInterceptorsFromDi()), provideHttpClientTesting()]
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

});
