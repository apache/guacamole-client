import { Component, Input, OnChanges, signal, SimpleChanges, ViewEncapsulation } from '@angular/core';
import * as fuzzysort from 'fuzzysort';
import { TextBatch } from '../services/key-event-display.service';
import { PlayerTimeService } from '../services/player-time.service';

/**
 * Component which plays back session recordings.
 */
@Component({
    selector: 'guac-player-text-view',
    templateUrl: './player-text-view.component.html',
    encapsulation: ViewEncapsulation.None,
    host: {
        '[class.fullscreen]': 'fullscreenKeyLog()'
    }
})
export class PlayerTextViewComponent implements OnChanges {

    /**
     * All the batches of text extracted from this recording.
     */
    @Input({ required: true }) textBatches!: TextBatch[];

    /**
     * A callback that accepts a timestamp, and seeks the recording to
     * that provided timestamp.
     */
    @Input({ required: true }) seek!: (timestamp: number) => void;

    /**
     * The current position within the recording.
     */
    @Input({ required: true }) currentPosition!: number;

    /**
     * The phrase to search within the text batches in order to produce the
     * filtered list for display.
     */
    searchPhrase = '';

    /**
     * The text batches that match the current search phrase, or all
     * batches if no search phrase is set.
     */
    filteredBatches: TextBatch[] = this.textBatches;

    /**
     * Whether or not the key log viewer should be full-screen. False by
     * default unless explicitly enabled by user interaction.
     */
    fullscreenKeyLog = signal<boolean>(false);

    /**
     * @borrows PlayerTimeService.formatTime
     */
    formatTime = this.playerTimeService.formatTime;

    /**
     * Inject required services.
     */
    constructor(private readonly playerTimeService: PlayerTimeService) {
    }

    /**
     * Toggle whether the key log viewer should take up the whole screen.
     */
    toggleKeyLogFullscreen() {
        this.fullscreenKeyLog.update(fullscreen => !fullscreen);
    }

    /**
     * Filter the provided text batches using the provided search phrase to
     * generate the list of filtered batches, or set to all provided
     * batches if no search phrase is provided.
     *
     * @param searchPhrase
     *     The phrase to search the text batches for. If no phrase is
     *     provided, the list of batches will not be filtered.
     */
    private applyFilter(searchPhrase: string): void {

        // If there's search phrase entered, search the text within the
        // batches for it
        if (searchPhrase)
            this.filteredBatches = fuzzysort.go(
                searchPhrase, this.textBatches, { key: 'simpleValue' })
                .map(result => result.obj);

        // Otherwise, do not filter the batches
        else
            this.filteredBatches = this.textBatches;

    }

    /**
     * React to changes to the search phrase and the text batches.
     */
    ngOnChanges(changes: SimpleChanges): void {

        // Reapply the current filter to the updated text batches
        if (changes['textBatches']) {
            this.applyFilter(this.searchPhrase);
        }

        // Reapply the filter whenever the search phrase is updated
        if (changes['searchPhrase']) {
            this.applyFilter(changes['searchPhrase'].currentValue);
        }

    }

}
