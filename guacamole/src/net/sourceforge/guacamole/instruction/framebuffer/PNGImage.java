package net.sourceforge.guacamole.instruction.framebuffer;

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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import net.sourceforge.guacamole.GuacamoleException;

public class PNGImage {

    private int width;
    private int height;
    private byte[] data;

    public PNGImage(BufferedImage image) throws GuacamoleException {

        width = image.getWidth();
        height = image.getHeight();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
            writeImage(image, bos);
            bos.flush();
        }
        catch (IOException e) {
            throw new GuacamoleException("I/O Error while creating PNG.", e);
        }

        data = bos.toByteArray();
    }

    public byte[] getData() {
        return data;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    private static void writeImage(BufferedImage image, OutputStream outputStream) throws GuacamoleException, IOException  {

        // Obtain list of image writers
        // If no such writers exist, fail with error, exit.
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType("image/png");
        if (!writers.hasNext())
            throw new GuacamoleException("No useful image writers found.");

        // Obtain JPEG writer
        ImageWriter imageWriter = writers.next();

        // Setup image parameters (including compression quality)
        /*ImageWriteParam imageParameters = new JPEGImageWriteParam(Locale.ENGLISH);
        imageParameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        imageParameters.setCompressionQuality(0.6f); // 60% quality, currently...
        imageParameters.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);*/

        ImageOutputStream out = ImageIO.createImageOutputStream(outputStream);

        // Write image
        imageWriter.setOutput(out);
        imageWriter.write(null, new IIOImage(image, null, null), null/*imageParameters*/);
        imageWriter.dispose();

        out.flush();
    }

}
