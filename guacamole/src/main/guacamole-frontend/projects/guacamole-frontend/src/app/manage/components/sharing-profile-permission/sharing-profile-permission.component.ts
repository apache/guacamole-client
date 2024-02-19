import { Component, Input, ViewEncapsulation } from '@angular/core';
import { ConnectionListContext } from "../../types/ConnectionListContext";
import { GroupListItem } from "../../../group-list/types/GroupListItem";

/**
 * A component which displays a sharing profile for a specific
 * connection and allows manipulation of the sharing profile permissions.
 */
@Component({
    selector: 'guac-sharing-profile-permission',
    templateUrl: './sharing-profile-permission.component.html',
    encapsulation: ViewEncapsulation.None
})
export class SharingProfilePermissionComponent {

    /**
     * TODO
     */
    @Input({required: true}) context!: ConnectionListContext;

    /**
     * TODO
     */
    @Input({required: true}) item!: GroupListItem;

}
