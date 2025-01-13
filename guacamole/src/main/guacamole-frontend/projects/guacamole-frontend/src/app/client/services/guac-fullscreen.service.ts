import { Injectable } from '@angular/core';

/**
 * A service for providing true fullscreen and keyboard lock support.
 * Keyboard lock is currently only supported by Chromium based browsers
 * (Edge >= V79, Chrome >= V68 and Opera >= V55)
 */
@Injectable({
    providedIn: 'root'
})
export class GuacFullscreenService {

    /**
     * Check is browser in true fullscreen mode
     */
    isInFullscreenMode(): Element | null {
        return document.fullscreenElement;
    }

    /**
     * Set fullscreen mode
     */
    setFullscreenMode(state: boolean): void {
        if (document.fullscreenEnabled) {
            if (state && !this.isInFullscreenMode())
                // @ts-ignore navigator.keyboard limited availability
                document.documentElement.requestFullscreen().then(navigator.keyboard.lock());
            else if (!state && this.isInFullscreenMode())
                // @ts-ignore navigator.keyboard limited availability
                document.exitFullscreen().then(navigator.keyboard.unlock());
        }
    }

    // toggles current fullscreen mode (off if on, on if off)
    toggleFullscreenMode(): void {
        if (!this.isInFullscreenMode())
            this.setFullscreenMode(true);
        else
            this.setFullscreenMode(false);
    }

}
