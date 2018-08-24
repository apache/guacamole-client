/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.auth.mongodb;

import java.net.UnknownHostException;
import java.util.Properties;

import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.morphia.JDBCAuthenticationProviderModule;
import org.apache.guacamole.morphia.base.ActivityRecordModel;
import org.apache.guacamole.morphia.connection.ConnectionAttributeModel;
import org.apache.guacamole.morphia.connection.ConnectionModel;
import org.apache.guacamole.morphia.connection.ConnectionParameterModel;
import org.apache.guacamole.morphia.connection.ConnectionPermissionModel;
import org.apache.guacamole.morphia.connection.ConnectionRecordModel;
import org.apache.guacamole.morphia.connectiongroup.ConnectionGroupAttributeModel;
import org.apache.guacamole.morphia.connectiongroup.ConnectionGroupModel;
import org.apache.guacamole.morphia.connectiongroup.ConnectionGroupPermissionModel;
import org.apache.guacamole.morphia.permission.SystemPermissionModel;
import org.apache.guacamole.morphia.sharingprofile.SharingProfileAttributeModel;
import org.apache.guacamole.morphia.sharingprofile.SharingProfileModel;
import org.apache.guacamole.morphia.sharingprofile.SharingProfileParameterModel;
import org.apache.guacamole.morphia.sharingprofile.SharingProfilePermissionModel;
import org.apache.guacamole.morphia.user.PasswordRecordModel;
import org.apache.guacamole.morphia.user.UserAttributeModel;
import org.apache.guacamole.morphia.user.UserModel;
import org.apache.guacamole.morphia.user.UserPermissionModel;
import org.apache.guacamole.morphia.user.UserRecordModel;
import org.codehaus.jackson.annotate.JsonProperty;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.mapping.DefaultCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

/**
 * Guice module which configures MongoDB-specific injections.
 */
public class MongoDBAuthenticationProviderModule implements Module {

    /**
     * Logger used to log unexpected outcomes.
     */
    private static final Logger logger = LoggerFactory
            .getLogger(JDBCAuthenticationProviderModule.class);

    private MongoDBEnvironment environment;

    private MongoClient mongoClient;

    /**
     * Optional "?options" string to append to the end of the connection URI.
     * See http://docs.mongodb.org/manual/reference/connection-string/
     */
    @JsonProperty
    private String options;

    /**
     * Optional flag that prevents any actual connections to the Mongo DB
     */
    @JsonProperty
    private boolean disabled;

    /**
     * MongoDB-specific driver configuration properties.
     */
    private final Properties driverProperties = new Properties();

    /**
     * Creates a new MongoDB authentication provider module that configures
     * driver and Morphia properties using the given environment.
     *
     * @param environment
     *            The environment to use when configuring Morphia and the
     *            underlying JDBC driver.
     *
     * @throws GuacamoleException
     *             If a required property is missing, or an error occurs while
     *             parsing a property.
     */
    public MongoDBAuthenticationProviderModule(MongoDBEnvironment environment) {
        this.environment = environment;

        // Use UTF-8 in database
        driverProperties.setProperty("characterEncoding", "UTF-8");
    }

    @Override
    public void configure(Binder binder) {
        // Bind JDBC driver properties
        binder.bind(Properties.class)
                .annotatedWith(Names.named("JDBC.driverProperties"))
                .toInstance(driverProperties);

        // Bind Models
        try {
            binder.bind(Datastore.class).toInstance(provideDatastore());
        } catch (UnknownHostException e) {
            logger.error(e.getMessage());
        }
    }

    @Provides
    @Named("datastore")
    public Datastore provideDatastore() throws UnknownHostException {

        final Morphia morphia = new Morphia();

        morphia.map(ActivityRecordModel.class, ConnectionAttributeModel.class,
                ConnectionGroupAttributeModel.class, ConnectionGroupModel.class,
                ConnectionGroupPermissionModel.class, ConnectionModel.class,
                ConnectionParameterModel.class, ConnectionPermissionModel.class,
                ConnectionRecordModel.class, PasswordRecordModel.class,
                SharingProfileAttributeModel.class, SharingProfileModel.class,
                SharingProfileParameterModel.class,
                SharingProfilePermissionModel.class,
                SystemPermissionModel.class, UserAttributeModel.class,
                UserModel.class, UserPermissionModel.class,
                UserRecordModel.class);

        // Mongo does not store the fields that do not exist,
        // activates the option to store them with null value
        morphia.getMapper().getOptions().setStoreNulls(Boolean.TRUE);

        // Sobreescribir el creador de clases de mapeo para que encuentre las
        // clases al mapear las tablas
        morphia.getMapper().getOptions().setObjectFactory(new DefaultCreator() {

            @Override
            public ClassLoader getClassLoaderForClass() {
                return JDBCAuthenticationProviderModule.class.getClassLoader();
            }

        });

        // create the Datastore connecting to the default port on the local host
        Datastore datastore = null;

        datastore = morphia.createDatastore((MongoClient) provideMongo(),
                environment.getMongoDBDatabase());
        datastore.ensureIndexes();

        return datastore;
    }

    /**
     * @param factory
     *            The application's Mongo factory
     * @return The Mongo client, given the connection parameters defined in
     *         {@code configuration}
     * @throws UnknownHostException
     *             Thrown if the server can not be found.
     */
    @Provides
    @Named("mongo")
    public Mongo provideMongo() throws UnknownHostException {
        if (mongoClient == null) {
            mongoClient = buildClient();
        }
        return mongoClient;
    }

    /**
     * <p>
     * Builds the MongoClient from a set of connections specified in the
     * configuration file.
     * </p>
     * <p>
     * If both the {@code user} and {@code pass} are non-null and non-empty, the
     * {@code MongoClient} that this method returns will attempt to authenticate
     * against the Mongo DB.
     * </p>
     * 
     * @return A Mongo API {@code MongoClient} object.
     * @throws UnknownHostException
     *             Thrown if the server can not be found.
     */
    private MongoClient buildClient() throws UnknownHostException {
        if (this.disabled) {
            return new MongoClient();
        }

        if (this.mongoClient != null) {
            return mongoClient;
        }

        this.mongoClient = new MongoClient(buildMongoClientURI());

        return this.mongoClient;
    }

    // Visible for testing
    MongoClientURI buildMongoClientURI() {
        StringBuilder uriString = new StringBuilder("mongodb://");
        Preconditions.checkState((StringUtils
                .isEmpty(environment.getMongoDBUsername())
                && StringUtils.isEmpty(environment.getMongoDBPassword()))
                || (!StringUtils.isEmpty(environment.getMongoDBUsername())
                        && !StringUtils
                                .isEmpty(environment.getMongoDBPassword())),
                "If you define a Mongo user, you must also define a Mongo pass, and vice-versa");
        Preconditions.checkNotNull(
                environment.getMongoDBHostname().concat(":")
                        .concat(String.valueOf(environment.getMongoDBPort())),
                "Must define Mongo 'hosts' property");

        if (!StringUtils.isEmpty(environment.getMongoDBUsername())) {
            uriString.append(environment.getMongoDBUsername()).append(":")
                    .append(environment.getMongoDBPassword()).append("@");
        }
        uriString.append(environment.getMongoDBHostname()).append(":")
                .append(environment.getMongoDBPort());

        if (!StringUtils.isEmpty(environment.getMongoDBDatabase())) {
            uriString.append("/").append(environment.getMongoDBDatabase());
        }

        if (!StringUtils.isEmpty(this.options)) {
            if (StringUtils.isEmpty(environment.getMongoDBDatabase())) {
                // If we didn't slap a "/" at the end of the hosts because
                // there's no DBName defined,
                // we need to do that here before sticking the "?options" in the
                // connection URI
                uriString.append("/");
            }
            uriString.append("?").append(this.options);
        }

        return new MongoClientURI(uriString.toString());
    }

}
