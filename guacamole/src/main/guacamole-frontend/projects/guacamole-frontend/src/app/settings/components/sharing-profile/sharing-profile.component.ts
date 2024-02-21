import { Component, Input, ViewEncapsulation } from '@angular/core';
import { GroupListItem } from "../../../group-list/types/GroupListItem";

/**
 * A component which displays a single sharing profile entry within the
 * list of accessible connections and groups.
 */
@Component({
  selector: 'guac-sharing-profile',
  templateUrl: './sharing-profile.component.html',
  encapsulation: ViewEncapsulation.None
})
export class SharingProfileComponent {

    /**
     * TODO
     */
    @Input({required: true}) item!: GroupListItem;

}
