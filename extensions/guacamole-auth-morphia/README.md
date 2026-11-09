<!--
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
-->
# Deploy extension:

## guacamole-client

Need a deployed guacamole-client and guacd

## guacamole-morphia

To install the library, you have to pass the script to create the database
You have to put the mongo library in the lib folder and the new extension of
morphia in the extensions folder.

### Create dataBase in MongoDB
Inside folder initial_config there are a file, schema-guacaomle_mongo.json, with the script to initial the database with user default guacadmin:guacadmin

### Create file guacamole.properties
Inside folder initial_config there are a file example, update this file with the info connection to your MongoDB and guacd

### Add configuration extension to guacamole-client
Add to folder GUACAMOLE_HOME/extensions morphia extension
Add to folder GUACAMOLE_HOME/lib driver Mongodb
Add to folder GUACAMOLE_HOME file guacamole.properties
