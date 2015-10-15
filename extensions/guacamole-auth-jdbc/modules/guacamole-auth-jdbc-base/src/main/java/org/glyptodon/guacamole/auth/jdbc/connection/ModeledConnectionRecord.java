/*
 * Copyright (C) 2013 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.glyptodon.guacamole.auth.jdbc.connection;


import java.util.Date;
import org.glyptodon.guacamole.net.auth.ConnectionRecord;

/**
 * A ConnectionRecord which is backed by a database model.
 *
 * @author James Muehlner
 * @author Michael Jumper
 */
public class ModeledConnectionRecord implements ConnectionRecord {

    /**
     * The model object backing this connection record.
     */
    private final ConnectionRecordModel model;

    /**
     * Creates a new ModeledConnectionRecord backed by the given model object.
     * Changes to this record will affect the backing model object, and changes
     * to the backing model object will affect this record.
     * 
     * @param model
     *     The model object to use to back this connection record.
     */
    public ModeledConnectionRecord(ConnectionRecordModel model) {
        this.model = model;
    }

    @Override
    public String getConnectionIdentifier() {
        return model.getConnectionIdentifier();
    }

    @Override
    public String getConnectionName() {
        return model.getConnectionName();
    }

    @Override
    public Date getStartDate() {
        return model.getStartDate();
    }

    @Override
    public Date getEndDate() {
        return model.getEndDate();
    }

    @Override
    public String getRemoteHost() {
        return null;
    }

    @Override
    public String getUsername() {
        return model.getUsername();
    }

    @Override
    public boolean isActive() {
        return false;
    }

}
