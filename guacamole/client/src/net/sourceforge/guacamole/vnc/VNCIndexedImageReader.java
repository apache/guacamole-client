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
import java.awt.image.IndexColorModel;
import java.io.DataInputStream;
import java.io.IOException;


public class VNCIndexedImageReader extends VNCImageReader {

    private IndexColorModel palette;
    private byte[] red;
    private byte[] green;
    private byte[] blue;

    // Set up BGR reader
    public VNCIndexedImageReader(byte[] red, byte[] green, byte[] blue) throws VNCException {

        this.red = red;
        this.green = green;
        this.blue = blue;

        palette = new IndexColorModel(8, 256, red, green, blue);
        if (palette.getMapSize() != 256)
            throw new VNCException("Currently, only 256-color maps are supported.");

    }

    @Override
    public int readPixel(DataInputStream input) throws IOException {
        int value = input.read();
        int color = (red[value] << 16) | (green[value] << 8) | blue[value];
        return color;
    }

    @Override
    public BufferedImage generateBlankImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, palette);
        return image;
    }

    // Load color model
    public static final IndexColorModel COLOR_MODEL;
    static {
        // Construct color model
        byte[] colorShade = {0, (byte) 51, (byte) 104, (byte) 153, (byte) 204, (byte) 255};
        byte[] greyShade = new byte[39];
        for (int i=1; i<40; i++)
            greyShade[i-1] = (byte) (6*i);

        byte[] red = new byte[colorShade.length*colorShade.length*colorShade.length+greyShade.length+1];
        byte[] green = new byte[colorShade.length*colorShade.length*colorShade.length+greyShade.length+1];
        byte[] blue = new byte[colorShade.length*colorShade.length*colorShade.length+greyShade.length+1];
        byte[] alpha = new byte[colorShade.length*colorShade.length*colorShade.length+greyShade.length+1];

        int color = 0;
        for (int r=0; r<colorShade.length; r++) {
            for (int g=0; g<colorShade.length; g++) {
                for (int b=0; b<colorShade.length; b++) {
                    red[color] = colorShade[r];
                    green[color] = colorShade[g];
                    blue[color] = colorShade[b];
                    alpha[color] = (byte) 255;
                    color++;
                }
            }
        }

        for (int grey=0; grey<greyShade.length; grey++) {
            red[color] = greyShade[grey];
            green[color] = greyShade[grey];
            blue[color] = greyShade[grey];
            alpha[color] = (byte) 255;
            color++;
        }

        red[color] = 0;
        green[color] = 0;
        blue[color] = 0;
        alpha[color] = (byte) 0;

        COLOR_MODEL = new IndexColorModel(8, 256, red, green, blue, alpha);
    }

}

