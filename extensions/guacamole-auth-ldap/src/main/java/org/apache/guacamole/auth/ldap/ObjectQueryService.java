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

package org.apache.guacamole.auth.ldap;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.filter.AndNode;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.OrNode;
import org.apache.directory.api.ldap.model.message.Referral;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.url.LdapUrl;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.net.auth.Identifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for executing queries against an LDAP directory intended to retrieve
 * Guacamole-related objects. Referrals are automatically handled. Convenience
 * functions are provided for generating the LDAP queries typically required
 * for retrieving Guacamole objects, as well as for converting the results of a
 * query into a {@link Map} of Guacamole objects.
 */
public class ObjectQueryService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(ObjectQueryService.class);
    
    /**
     * Service for connecting to LDAP directory.
     */
    @Inject
    private LDAPConnectionService ldapService;

    /**
     * Returns the identifier of the object represented by the given LDAP
     * entry. Multiple attributes may be declared as containing the identifier
     * of the object when present on an LDAP entry. If multiple such attributes
     * are present on the same LDAP entry, the value of the attribute with
     * highest priority is used. If multiple copies of the same attribute are
     * present on the same LDAPentry, the first value of that attribute is
     * used.
     *
     * @param entry
     *     The entry representing the Guacamole object whose unique identifier
     *     should be determined.
     *
     * @param attributes
     *     A collection of all attributes which may be used to specify the
     *     unique identifier of the Guacamole object represented by an LDAP
     *     entry, in order of decreasing priority.
     *
     * @return
     *     The identifier of the object represented by the given LDAP entry, or
     *     null if no attributes declared as containing the identifier of the
     *     object are present on the entry.
     * 
     * @throws LdapInvalidAttributeValueException
     *     If an error occurs retrieving the value of the identifier attribute.
     */
    public String getIdentifier(Entry entry, Collection<String> attributes) 
            throws LdapInvalidAttributeValueException {

        // Retrieve the first value of the highest priority identifier attribute
        for (String identifierAttribute : attributes) {
            Attribute identifier = entry.get(identifierAttribute);
            if (identifier != null)
                return identifier.getString();
        }

        // No identifier attribute is present on the entry
        return null;

    }

    /**
     * Generates a properly-escaped LDAP query which finds all objects which
     * match the given LDAP filter and which have at least one of the given
     * attributes set to the specified value.
     *
     * @param filter
     *     The LDAP filter to apply to reduce the results of the query in
     *     addition to testing the values of the given attributes.
     *
     * @param attributes
     *     A collection of all attributes to test for equivalence to the given
     *     value, in order of decreasing priority.
     *
     * @param attributeValue
     *     The value that the resulting LDAP query should search for within the
     *     attributes of objects within the LDAP directory. If null, the
     *     resulting LDAP query will search for the presence of at least one of
     *     the given attributes on each object, regardless of the value of
     *     those attributes.
     *
     * @return
     *     An LDAP query which will search for arbitrary LDAP objects having at
     *     least one of the given attributes set to the specified value.
     */
    public ExprNode generateQuery(ExprNode filter,
            Collection<String> attributes, String attributeValue) {

        // Build LDAP query for objects having at least one attribute and with
        // the given search filter
        AndNode searchFilter = new AndNode();
        searchFilter.addNode(filter);

        // Include all attributes within OR clause if there are more than one
        OrNode attributeFilter = new OrNode();
       
        // Add equality comparison for each possible attribute
        attributes.forEach(attribute ->
            attributeFilter.addNode(new EqualityNode(attribute, attributeValue))
        );

        searchFilter.addNode(attributeFilter);
        
        return searchFilter;

    }

    /**
     * Executes an arbitrary LDAP query using the given connection, returning a
     * list of all results. Only objects beneath the given base DN are
     * included in the search.
     *
     * @param ldapConnection
     *     The current connection to the LDAP server, associated with the
     *     current user.
     *
     * @param baseDN
     *     The base DN to search using the given LDAP query.
     *
     * @param query
     *     The LDAP query to execute.
     * 
     * @param searchHop
     *     The level of depth for this search, used for tracking referrals.
     *
     * @return
     *     A list of all results accessible to the user currently bound under
     *     the given LDAP connection.
     *
     * @throws GuacamoleException
     *     If an error occurs executing the query, or if configuration
     *     information required to execute the query cannot be read from
     *     guacamole.properties.
     */
    public List<Entry> search(LdapNetworkConnection ldapConnection,
            Dn baseDN, ExprNode query, int searchHop) throws GuacamoleException {

        logger.debug("Searching \"{}\" for objects matching \"{}\".", baseDN, query);

        try {

            LdapConnectionConfig ldapConnectionConfig = ldapConnection.getConfig();
            
            // Search within subtree of given base DN
            SearchRequest request = ldapService.getSearchRequest(baseDN,
                    query);
            
            SearchCursor results = ldapConnection.search(request);

            // Produce list of all entries in the search result, automatically
            // following referrals if configured to do so
            List<Entry> entries = new ArrayList<>();
            while (results.next()) {

                if (results.isEntry()) {
                    entries.add(results.getEntry());
                }
                else if (results.isReferral() && request.isFollowReferrals()) {
                    
                    Referral referral = results.getReferral();
                    for (String url : referral.getLdapUrls()) {
                        LdapNetworkConnection referralConnection = ldapService.referralConnection(
                            new LdapUrl(url), ldapConnectionConfig, searchHop++);
                        entries.addAll(search(referralConnection, baseDN, query, searchHop));
                    }
                    
                }
                
            }

            return entries;

        }
        catch (CursorException | LdapException e) {
            throw new GuacamoleServerException("Unable to query list of "
                    + "objects from LDAP directory.", e);
        }

    }

    /**
     * Executes the query which would be returned by generateQuery() using the
     * given connection, returning a list of all results. Only objects beneath
     * the given base DN are included in the search.
     *
     * @param ldapConnection
     *     The current connection to the LDAP server, associated with the
     *     current user.
     *
     * @param baseDN
     *     The base DN to search using the given LDAP query.
     *
     * @param filter
     *     The LDAP filter to apply to reduce the results of the query in
     *     addition to testing the values of the given attributes.
     *
     * @param attributes
     *     A collection of all attributes to test for equivalence to the given
     *     value, in order of decreasing priority.
     *
     * @param attributeValue
     *     The value that should be searched search for within the attributes
     *     of objects within the LDAP directory. If null, the search will test
     *     only for the presence of at least one of the given attributes on
     *     each object, regardless of the value of those attributes.
     *
     * @return
     *     A list of all results accessible to the user currently bound under
     *     the given LDAP connection.
     *
     * @throws GuacamoleException
     *     If an error occurs executing the query, or if configuration
     *     information required to execute the query cannot be read from
     *     guacamole.properties.
     */
    public List<Entry> search(LdapNetworkConnection ldapConnection, Dn baseDN,
            ExprNode filter, Collection<String> attributes, String attributeValue)
            throws GuacamoleException {
        ExprNode query = generateQuery(filter, attributes, attributeValue);
        return search(ldapConnection, baseDN, query, 0);
    }

    /**
     * Converts a given list of LDAP entries to a {@link Map} of Guacamole
     * objects stored by their identifiers.
     *
     * @param <ObjectType>
     *     The type of object to store within the {@link Map}.
     *
     * @param entries
     *     A list of LDAP entries to convert to Guacamole objects.
     *
     * @param mapper
     *     A mapping function which converts a given LDAP entry to its
     *     corresponding Guacamole object. If the LDAP entry cannot be
     *     converted, null should be returned.
     *
     * @return
     *     A new {@link Map} containing Guacamole object versions of each of
     *     the given LDAP entries, where each object is stored within the
     *     {@link Map} under its corresponding identifier.
     */
    public <ObjectType extends Identifiable> Map<String, ObjectType>
        asMap(List<Entry> entries, Function<Entry, ObjectType> mapper) {

        // Convert each entry to the corresponding Guacamole API object
        Map<String, ObjectType> objects = new HashMap<>(entries.size());
        for (Entry entry : entries) {

            ObjectType object = mapper.apply(entry);
            if (object == null) {
                logger.debug("Ignoring object \"{}\".", entry.getDn().toString());
                continue;
            }

            // Attempt to add object to map, warning if the object appears
            // to be a duplicate
            String identifier = object.getIdentifier();
            if (objects.putIfAbsent(identifier, object) != null)
                logger.warn("Multiple objects ambiguously map to the "
                        + "same identifier (\"{}\"). Ignoring \"{}\" as "
                        + "a duplicate.", identifier, entry.getDn().toString());

        }

        return objects;

    }

}
