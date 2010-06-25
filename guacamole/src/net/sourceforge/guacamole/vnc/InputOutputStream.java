
package net.sourceforge.guacamole.vnc;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

public class InputOutputStream extends InputStream {

    private int pos = 0;
    private byte[] current = null;
    private LinkedList<byte[]> buffer = new LinkedList<byte[]>();

    public void write(byte[] data) {

        if (data.length == 0)
            return;

        if (current == null)
            current = data;
        else
            buffer.addLast(data);
    }

    @Override
    public int read() throws IOException {

        if (pos >= current.length) {
            if (buffer.size() == 0)
                throw new IOException("Buffer underrun.");

            current = buffer.removeFirst();
            pos = 0;
        }

        return 0xFF & current[pos++];

    }

    @Override
    public int read(byte[] data) throws IOException {
        return read(data, 0, data.length);
    }

    @Override
    public int read(byte[] data, int off, int len) throws IOException {

        if (pos >= current.length) {
            if (buffer.size() == 0)
                throw new IOException("Buffer underrun.");

            current = buffer.removeFirst();
            pos = 0;
        }

        int amountRead = Math.min(current.length - pos, len);
        System.arraycopy(current, pos, data, off, amountRead);
        pos += amountRead;

        return amountRead;
    }

}
