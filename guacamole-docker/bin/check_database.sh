#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

# Setting up  mysql database if not exists

if [ "$SLEEP_SHORT" == "" ]; then
	SLEEP_SHORT=5
fi

if [ "$SLEEP_LONG" == "" ]; then
	SLEEP_LONG=10
fi

if [ "$RETRIES" == "" ]; then
	RETRIES=5
fi

if [[ "$MYSQL_USER" != "" && "$MYSQL_PASSWORD" != "" && "$MYSQL_DATABASE" != "" && "$MYSQL_HOSTNAME" != "" ]]; then
	
	if [ "$MYSQL_PORT" == "" ]; then
		DB_PORT=3306;
	else
		DB_PORT=$MYSQL_PORT;
	fi
	
	DB_HOST=$MYSQL_HOSTNAME;
	DB_NAME=$MYSQL_DATABASE;
	DB_USER=$MYSQL_USER;
	DB_PASSWORD=$MYSQL_PASSWORD;
	DB_ENGINE="/usr/bin/mariadb"
	
	
	UUID=$(openssl rand -hex 16)
	SALT=$(echo ${UUID:0:8}-${UUID:8:4}-${UUID:12:4}-${UUID:16:4}-${UUID:20:12} | openssl sha256 -hex | awk '{print toupper($2)}');
	HASH=$(echo -n "$ADMIN_PASSWORD$SALT" | openssl sha256 -hex | awk '{print toupper($2)}')
	cp /opt/guacamole/mysql/schema/*.sql /tmp/
	sed -i "s/guacadmin/$ADMIN_NAME/g" /tmp/002-create-admin-user.sql 
	sed -i "s/FE24ADC5E11E2B25288D1704ABE67A79E342ECC26064CE69C5B3177795A82264/$SALT/" /tmp/002-create-admin-user.sql 
	sed -i "s/CA458A7D494E3BE824F5E1E175A1556C0F8EEF2C2D7DF3633BEC4A29C4411960/$HASH/" /tmp/002-create-admin-user.sql 


	# Check database access
	for retries in $(seq 0 $((RETRIES+ 1))); do
	          RESPONSE=$(echo "exit" | timeout $SLEEP_SHORT telnet $DB_HOST $DB_PORT|grep Connected);
	          echo "RESPONSE: $RESPONSE";
	          if [ "$RESPONSE" == "" ]; then 
	              if [[ $retries -le $RETRIES ]] ; then
	                  echo "Retired $retries";
	                  echo "Expected $DB_HOST currently not available, wait $SLEEP_SHORT seconds.";
	                  sleep $SLEEP_SHORT;
	              else
	                  for retries2 in $(seq 0 $((RETRIES+ 1))); do
	                    RESPONSE=$(echo "exit" | timeout $SLEEP_SHORT telnet $DB_HOST $DB_PORT|grep Connected);
	                      if [ "$RESPONSE" == "" ]; then 
	                          if [[ $retries2 -le $RETRIES ]] ; then
	                              echo "Retired $retries2";
	                              echo "Expected $DB_HOST currently not available, wait $SLEEP_LONG seconds.";
	                              sleep $SLEEP_LONG;
	                          else
	                              echo "Expected $DB_HOST currently not available, and reached all reries limit, exiting";
	                              exit 1; 
	                          fi
	                      else
			      	  echo "Expected $DB_HOST accessed successfuly, continue database init processes."
	                          break;
	                      fi  
	                  done 
	              fi
	          else
		      echo "Expected $DB_HOST accessed successfuly, continue database init processes."
	              break;
	          fi
	done

	for i in $(ls /tmp/*.sql); do 
		CREATE_DATABASE=$($DB_ENGINE -u $DB_USER -p$DB_PASSWORD $DB_NAME -h $DB_HOST --port $DB_PORT < $i);
		if [[ "$(echo $?)" == "1" ]]; then
			echo "Database already exists";
			exit 2;                
		else
			echo "Database automaticaly initialized." ;
		fi
	done
fi
