package net.sourceforge.guacamole.vnc;

/*
 *  Guacamole - Pure JavaScript/HTML VNC Client
 *  Copyright (C) 2010  Michael Jumper
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;

public abstract class VNCImageReader {

    public BufferedImage readCImage(DataInputStream input, int width, int height) throws IOException {

        BufferedImage image = generateBlankImage(width, height);
        // Read image

        for (int pixelY=0; pixelY<height; pixelY++) {
            for (int pixelX=0; pixelX<width; pixelX++) {
                int color = 0xFF000000 | readCPixel(input);
                image.setRGB(pixelX, pixelY, color);
            }
        }

        return image;
    }

    public BufferedImage readImage(DataInputStream input, int width, int height) throws IOException {

        BufferedImage image = generateBlankImage(width, height);
        // Read image

        for (int pixelY=0; pixelY<height; pixelY++) {
            for (int pixelX=0; pixelX<width; pixelX++) {
                int color = 0xFF000000 | readPixel(input);
                image.setRGB(pixelX, pixelY, color);
            }
        }

        return image;
    }

    public abstract BufferedImage generateBlankImage(int width, int height);
    public abstract int readPixel(DataInputStream input) throws IOException;
    public int readCPixel(DataInputStream input) throws IOException {
        return readPixel(input);
    }

}

