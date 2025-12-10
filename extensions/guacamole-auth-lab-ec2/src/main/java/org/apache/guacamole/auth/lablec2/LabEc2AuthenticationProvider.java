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
package org.apache.guacamole.auth.lablec2;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.environment.LocalEnvironment;
import org.apache.guacamole.net.auth.AbstractAuthenticationProvider;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.DelegatingUserContext;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.net.auth.simple.SimpleConnection;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.ResourceType;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.StartInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TagSpecification;

/**
 * Authentication provider which decorates existing user contexts with a
 * per-user EC2-backed lab connection.
 */
public class LabEc2AuthenticationProvider extends AbstractAuthenticationProvider {

    /** Logger. */
    private static final Logger logger = LoggerFactory.getLogger(
            LabEc2AuthenticationProvider.class);

    /** EC2 client used to orchestrate lab instances. */
    private final Ec2Client ec2;

    private final String region;
    private final String labAmiId;
    private final String instanceType;
    private final String subnetId;
    private final List<String> securityGroupIds;
    private final String launchTemplateId;
    private final String protocol;
    private final String port;
    private final String vmUsername;
    private final String vmPassword;
    private final String labGroupName;

    /**
     * Creates the authentication provider, reading configuration from the
     * local environment.
     *
     * @throws GuacamoleException
     *     if configuration cannot be read.
     */
    @SuppressWarnings("deprecation")
    public LabEc2AuthenticationProvider() throws GuacamoleException {
        Environment env = new LocalEnvironment();

        region = env.getRequiredProperty(LabEc2Properties.LAB_EC2_REGION);
        labAmiId = env.getProperty(LabEc2Properties.LAB_EC2_AMI_ID);
        instanceType = env.getProperty(LabEc2Properties.LAB_EC2_INSTANCE_TYPE, "t3.medium");
        subnetId = env.getProperty(LabEc2Properties.LAB_EC2_SUBNET_ID);
        securityGroupIds = parseCsv(env.getProperty(LabEc2Properties.LAB_EC2_SECURITY_GROUP_IDS));
        launchTemplateId = env.getProperty(LabEc2Properties.LAB_EC2_LAUNCH_TEMPLATE_ID);
        protocol = env.getProperty(LabEc2Properties.LAB_EC2_PROTOCOL, "rdp");
        port = env.getProperty(LabEc2Properties.LAB_EC2_PORT, "3389");
        vmUsername = env.getProperty(LabEc2Properties.LAB_EC2_USERNAME, "labuser");
        vmPassword = env.getProperty(LabEc2Properties.LAB_EC2_PASSWORD);
        labGroupName = env.getProperty(LabEc2Properties.LAB_EC2_LAB_GROUP, "lab_user");

        ec2 = Ec2Client.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .region(Region.of(region))
                .build();
    }

    @Override
    public String getIdentifier() {
        return "lab-ec2";
    }

    @Override
    public UserContext decorate(UserContext context,
            AuthenticatedUser authenticatedUser, Credentials credentials)
            throws GuacamoleException {

        if (context == null) {
            logger.debug("Skipping lab EC2 decoration because no base context was provided.");
            return null;
        }

        Set<String> groups = authenticatedUser.getEffectiveUserGroups();
        logger.debug("Evaluating lab EC2 decoration for user '{}' with groups {}.",
                authenticatedUser.getIdentifier(), groups);

        if (groups == null || !groups.contains(labGroupName)) {
            logger.debug("User '{}' is not in the lab group '{}'; leaving context unchanged.",
                    authenticatedUser.getIdentifier(), labGroupName);
            return context;
        }

        logger.info("Ensuring lab EC2 instance for user '{}'.",
                authenticatedUser.getIdentifier());
        Instance instance = ensureLabInstance(authenticatedUser.getIdentifier());

        String hostname = instance.privateDnsName();
        if (hostname == null || hostname.isEmpty())
            hostname = instance.privateIpAddress();

        logger.info("Using lab instance '{}' at host '{}' for user '{}'.",
                instance.instanceId(), hostname, authenticatedUser.getIdentifier());

        SimpleConnection labConnection = buildLabConnection(
                authenticatedUser.getIdentifier(), hostname);

        Directory<Connection> baseDir = context.getConnectionDirectory();
        Directory<Connection> mergedDir = new LabMergingConnectionDirectory(
                baseDir, labConnection);

        return new DelegatingUserContext(context) {
            @Override
            public Directory<Connection> getConnectionDirectory()
                    throws GuacamoleException {
                return mergedDir;
            }
        };
    }

    /**
     * Ensures a per-user EC2 instance exists and is running.
     *
     * @param username
     *     The username to tag on the instance.
     *
     * @return
     *     The running EC2 instance.
     *
     * @throws GuacamoleException
     *     If EC2 operations fail.
     */
    private Instance ensureLabInstance(String username) throws GuacamoleException {
        String owner = username.toLowerCase(Locale.ROOT);

        Filter ownerFilter = Filter.builder()
                .name("tag:LabOwner")
                .values(owner)
                .build();

        Filter stateFilter = Filter.builder()
                .name("instance-state-name")
                .values("pending", "running", "stopping", "stopped")
                .build();

        DescribeInstancesResponse describeResponse = describeInstances(ownerFilter,
                stateFilter);

        logger.debug("DescribeInstances returned {} reservations for owner '{}'.",
                describeResponse.reservations().size(), owner);

        Instance instance = describeResponse.reservations().stream()
                .flatMap(reservation -> reservation.instances().stream())
                .findFirst()
                .orElse(null);

        if (instance == null) {
            logger.info("No existing lab instance found for owner '{}'; launching new instance.",
                    owner);
            instance = runLabInstance(owner);
        }
        else if (InstanceStateName.STOPPED.equals(instance.state().name())
                || InstanceStateName.STOPPING.equals(instance.state().name())) {
            logger.info("Found stopped lab instance '{}' for owner '{}'; starting it.",
                    instance.instanceId(), owner);
            instance = startInstance(instance.instanceId());
        }
        else {
            logger.info("Using existing lab instance '{}' in state '{}' for owner '{}'.",
                    instance.instanceId(), instance.state().name(), owner);
        }

        return instance;
    }

    /**
     * Starts an existing instance and waits for it to become available.
     *
     * @param instanceId
     *     The ID of the instance to start.
     *
     * @return
     *     The updated instance details.
     *
     * @throws GuacamoleException
     *     If the instance cannot be started.
     */
    private Instance startInstance(String instanceId) throws GuacamoleException {
        try {
            logger.debug("Starting EC2 instance '{}'.", instanceId);
            StartInstancesResponse startResponse = ec2.startInstances(builder ->
                    builder.instanceIds(instanceId));
            if (startResponse.startingInstances().isEmpty())
                throw new GuacamoleServerException("Failed to start lab instance " + instanceId);

            ec2.waiter().waitUntilInstanceRunning(waiter ->
                    waiter.instanceIds(instanceId));

            logger.debug("Instance '{}' reported as running; fetching details.", instanceId);
            return getInstanceById(instanceId);
        }
        catch (SdkException e) {
            throw new GuacamoleServerException("Unable to start lab EC2 instance.", e);
        }
    }

    /**
     * Launches a new EC2 instance for the given owner.
     *
     * @param owner
     *     The owner tag value to assign.
     *
     * @return
     *     The new EC2 instance.
     *
     * @throws GuacamoleException
     *     If the instance cannot be created.
     */
    private Instance runLabInstance(String owner) throws GuacamoleException {
        try {
            logger.debug("Preparing to run new lab instance for owner '{}'.", owner);
            RunInstancesRequest.Builder builder = RunInstancesRequest.builder()
                    .minCount(1)
                    .maxCount(1);

            if (launchTemplateId != null && !launchTemplateId.isEmpty()) {
                builder = builder.launchTemplate(template ->
                        template.launchTemplateId(launchTemplateId));
            }
            else {
                if (labAmiId == null || labAmiId.isEmpty())
                    throw new GuacamoleServerException("lab-ec2-ami-id must be set when no launch template is provided.");

                builder = builder.imageId(labAmiId)
                        .instanceType(InstanceType.fromValue(instanceType));

                if (subnetId != null && !subnetId.isEmpty())
                    builder = builder.subnetId(subnetId);

                if (!securityGroupIds.isEmpty())
                    builder = builder.securityGroupIds(securityGroupIds);
            }

            builder = builder.tagSpecifications(TagSpecification.builder()
                    .resourceType(ResourceType.INSTANCE)
                    .tags(
                            Tag.builder().key("Name").value("guac-lab-" + owner).build(),
                            Tag.builder().key("LabOwner").value(owner).build(),
                            Tag.builder().key("LabEnv").value("guac").build()
                    )
                    .build());

            RunInstancesResponse runResponse = ec2.runInstances(builder.build());
            Instance instance = runResponse.instances().get(0);

            ec2.waiter().waitUntilInstanceRunning(waiter ->
                    waiter.instanceIds(instance.instanceId()));

            logger.info("Launched lab instance '{}' for owner '{}'; waiting until running.",
                    instance.instanceId(), owner);
            return getInstanceById(instance.instanceId());
        }
        catch (SdkException e) {
            throw new GuacamoleServerException("Unable to create lab EC2 instance.", e);
        }
    }

    /**
     * Retrieves details for a specific instance ID.
     *
     * @param instanceId
     *     The instance ID to query.
     *
     * @return
     *     The instance details.
     *
     * @throws GuacamoleException
     *     If the instance cannot be described.
     */
    private Instance getInstanceById(String instanceId) throws GuacamoleException {
        try {
            logger.debug("Describing EC2 instance '{}'.", instanceId);
            DescribeInstancesResponse response = ec2.describeInstances(builder ->
                    builder.instanceIds(instanceId));

            return response.reservations().stream()
                    .flatMap(reservation -> reservation.instances().stream())
                    .findFirst()
                    .orElseThrow(() -> new GuacamoleServerException(
                            "Unable to load lab instance " + instanceId));
        }
        catch (SdkException e) {
            throw new GuacamoleServerException("Unable to describe lab EC2 instance.", e);
        }
    }

    /**
     * Executes an EC2 describe-instances call with the provided filters.
     */
    private DescribeInstancesResponse describeInstances(Filter... filters)
            throws GuacamoleException {
        try {
            return ec2.describeInstances(builder -> builder.filters(filters));
        }
        catch (SdkException e) {
            throw new GuacamoleServerException("Unable to describe EC2 instances.", e);
        }
    }

    /**
     * Builds a connection targeting the given host for the specified user.
     */
    private SimpleConnection buildLabConnection(String username, String hostname) {
        String connectionId = "lab-" + username;

        GuacamoleConfiguration config = new GuacamoleConfiguration();
        config.setProtocol(protocol);
        config.setParameter("hostname", hostname);
        config.setParameter("port", port);
        config.setParameter("username", vmUsername);

        if (vmPassword != null && !vmPassword.isEmpty())
            config.setParameter("password", vmPassword);

        SimpleConnection connection = new SimpleConnection("My Lab VM",
                connectionId, config);
        connection.setParentIdentifier("ROOT");

        return connection;
    }

    /**
     * Parses a comma-delimited list into a collection of trimmed values.
     */
    private List<String> parseCsv(String csv) {
        if (csv == null || csv.trim().isEmpty())
            return Collections.emptyList();

        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toList());
    }

}
