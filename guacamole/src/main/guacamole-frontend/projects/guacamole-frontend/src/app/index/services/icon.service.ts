import {Injectable} from '@angular/core';
import {NavigationEnd, Router} from '@angular/router';
import {filter} from 'rxjs';
import $ from "jquery";

/**
 * A service for updating or resetting the favicon of the current page.
 */
@Injectable({
    providedIn: 'root'
})
export class IconService {

    /**
     * The URL of the image used for the low-resolution (64x64) favicon. This
     * MUST match the URL which is set statically within index.html.
     */
    private readonly DEFAULT_SMALL_ICON_URL = 'images/logo-64.png';

    /**
     * The URL of the image used for the high-resolution (144x144) favicon. This
     * MUST match the URL which is set statically within index.html.
     */
    private readonly DEFAULT_LARGE_ICON_URL = 'images/logo-144.png';

    /**
     * JQuery-wrapped array of all link tags which point to the small,
     * low-resolution page icon.
     *
     * @type Element[]
     */
    private smallIcons = $('link[rel=icon][href="' + this.DEFAULT_SMALL_ICON_URL + '"]');

    /**
     * JQuery-wrapped array of all link tags which point to the large,
     * high-resolution page icon.
     *
     * @type Element[]
     */
    private largeIcons = $('link[rel=icon][href="' + this.DEFAULT_LARGE_ICON_URL + '"]');

    constructor(private router: Router) {

        // Automatically reset page icons after navigation
        this.router.events
            .pipe(filter(event => event instanceof NavigationEnd))
            .subscribe(() => {
                this.setDefaultIcons();
            });
    }

    /**
     * Generates an icon by scaling the provided image to fit the given
     * dimensions, returning a canvas containing the generated icon.
     *
     * @param canvas
     *     A canvas element containing the image which should be scaled to
     *     produce the contents of the generated icon.
     *
     * @param width
     *     The width of the icon to generate, in pixels.
     *
     * @param height
     *     The height of the icon to generate, in pixels.
     *
     * @returns
     *     A new canvas element having the given dimensions and containing the
     *     provided image, scaled to fit.
     */
    private generateIcon(canvas: HTMLCanvasElement, width: number, height: number): HTMLCanvasElement {

        // Create icon canvas having the provided dimensions
        const icon = document.createElement('canvas');
        icon.width = width;
        icon.height = height;

        // Calculate the scale factor necessary to fit the provided image
        // within the icon dimensions
        const scale = Math.min(width / canvas.width, height / canvas.height);

        // Calculate the dimensions and position of the scaled image within
        // the icon, offsetting the image such that it is centered
        const scaledWidth = canvas.width * scale;
        const scaledHeight = canvas.height * scale;
        const offsetX = (width - scaledWidth) / 2;
        const offsetY = (height - scaledHeight) / 2;

        // Draw the icon, scaling the provided image as necessary
        const context = icon.getContext('2d');
        context?.drawImage(canvas, offsetX, offsetY, scaledWidth, scaledHeight);
        return icon;

    }

    /**
     * Temporarily sets the icon of the current page to the contents of the
     * given canvas element. The image within the canvas element will be
     * automatically scaled and centered to fit within the dimensions of the
     * page icons. The page icons will be automatically reset to their original
     * values upon navigation.
     *
     * @param canvas
     *     The canvas element containing the icon. If this value is null or
     *     undefined, this function has no effect.
     */
    setIcons(canvas: HTMLCanvasElement): void {

        // Do nothing if no canvas provided
        if (!canvas)
            return;

        // Assign low-resolution (64x64) icon
        const smallIcon = this.generateIcon(canvas, 64, 64);
        this.smallIcons.attr('href', smallIcon.toDataURL('image/png'));

        // Assign high-resolution (144x144) icon
        const largeIcon = this.generateIcon(canvas, 144, 144);
        this.largeIcons.attr('href', largeIcon.toDataURL('image/png'));

    }

    /**
     * Resets the icons of the current page to their original values, undoing
     * any previous calls to setIcons(). This function is automatically invoked
     * upon navigation.
     */
    setDefaultIcons() {
        this.smallIcons.attr('href', this.DEFAULT_SMALL_ICON_URL);
        this.largeIcons.attr('href', this.DEFAULT_LARGE_ICON_URL);
    };

}
