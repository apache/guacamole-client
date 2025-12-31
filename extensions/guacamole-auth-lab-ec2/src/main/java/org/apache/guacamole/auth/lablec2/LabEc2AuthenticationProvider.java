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

import java.util.*;
import java.util.stream.Collectors;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.environment.LocalEnvironment;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.net.auth.*;
import org.apache.guacamole.net.auth.simple.SimpleConnection;
import org.apache.guacamole.net.auth.permission.ObjectPermission;
import org.apache.guacamole.net.auth.permission.ObjectPermissionSet;
import org.apache.guacamole.protocol.GuacamoleClientInformation;
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
import software.amazon.awssdk.services.ec2.model.InstanceNetworkInterfaceSpecification;
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

    /**
     * If the user has multiple data sources, this extension can inject the lab
     * connection into more than one data source, resulting in duplicate
     * "My Lab VM" entries. To mitigate this without requiring configuration,
     * we temporarily "pin" each user to a single underlying data source.
     */
    private static final long DECORATION_TARGET_TTL_MS = 10L * 60L * 1000L;

    private static final class DecorationTarget {

        private final String authProviderIdentifier;
        private final long chosenAtMs;

        private DecorationTarget(String authProviderIdentifier, long chosenAtMs) {
            this.authProviderIdentifier = authProviderIdentifier;
            this.chosenAtMs = chosenAtMs;
        }

        private boolean isExpired(long nowMs) {
            return nowMs - chosenAtMs > DECORATION_TARGET_TTL_MS;
        }

    }

    /** EC2 client used to orchestrate lab instances. */
    private final Ec2Client ec2;

    private final String region;
    private final String labAmiId;
    private final String instanceType;
    private final String subnetId;
    private final List<String> securityGroupIds;
    private final boolean assignPublicIp;
    private final String launchTemplateId;
    private final String protocol;
    private final String port;
    private final String vmUsername;
    private final String vmPassword;
    private final String labGroupName;
    private final String decorateOnlyAuthProvider;

    private final java.util.concurrent.ConcurrentHashMap<String, DecorationTarget>
            decorationTargetsByUser = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Creates the authentication provider, reading configuration from the
     * local environment.
     *
     * @throws GuacamoleException
     *                            if configuration cannot be read.
     */
    @SuppressWarnings("deprecation")
    public LabEc2AuthenticationProvider() throws GuacamoleException {
        Environment env = new LocalEnvironment();

        region = env.getRequiredProperty(LabEc2Properties.LAB_EC2_REGION);
        labAmiId = env.getProperty(LabEc2Properties.LAB_EC2_AMI_ID);
        instanceType = env.getProperty(LabEc2Properties.LAB_EC2_INSTANCE_TYPE, "t3.medium");
        subnetId = env.getProperty(LabEc2Properties.LAB_EC2_SUBNET_ID);
        securityGroupIds = parseCsv(env.getProperty(LabEc2Properties.LAB_EC2_SECURITY_GROUP_IDS));
        assignPublicIp = env.getProperty(LabEc2Properties.LAB_EC2_ASSIGN_PUBLIC_IP, false);
        launchTemplateId = env.getProperty(LabEc2Properties.LAB_EC2_LAUNCH_TEMPLATE_ID);
        protocol = env.getProperty(LabEc2Properties.LAB_EC2_PROTOCOL, "rdp");
        port = env.getProperty(LabEc2Properties.LAB_EC2_PORT, "3389");
        vmUsername = env.getProperty(LabEc2Properties.LAB_EC2_USERNAME, "labuser");
        vmPassword = env.getProperty(LabEc2Properties.LAB_EC2_PASSWORD);
        labGroupName = env.getProperty(LabEc2Properties.LAB_EC2_LAB_GROUP, "lab_user");
        decorateOnlyAuthProvider = env.getProperty(LabEc2Properties.LAB_EC2_DECORATE_ONLY_AUTH_PROVIDER);

        logger.info(
                "Initializing lab-ec2 extension: region='{}' protocol='{}' port='{}' labGroup='{}' launchTemplateIdSet={} amiIdSet={} subnetIdSet={} securityGroups={}",
                region, protocol, port, labGroupName,
                launchTemplateId != null && !launchTemplateId.isEmpty(),
                labAmiId != null && !labAmiId.isEmpty(),
                subnetId != null && !subnetId.isEmpty(),
                securityGroupIds.size());
        if (decorateOnlyAuthProvider != null && !decorateOnlyAuthProvider.trim().isEmpty()) {
            logger.info("lab-ec2 will only decorate user contexts from auth provider '{}'.",
                    decorateOnlyAuthProvider.trim());
        }

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

        String baseAuthProvider = context.getAuthenticationProvider() != null
                ? context.getAuthenticationProvider().getIdentifier()
                : null;

        if (decorateOnlyAuthProvider != null && !decorateOnlyAuthProvider.trim().isEmpty()) {
            String requiredProvider = decorateOnlyAuthProvider.trim();
            if (baseAuthProvider == null || !requiredProvider.equals(baseAuthProvider)) {
                logger.debug("Skipping lab-ec2 decoration for user '{}' because base auth provider is '{}' (required '{}').",
                        authenticatedUser.getIdentifier(), baseAuthProvider, requiredProvider);
                return context;
            }
        }
        else if (baseAuthProvider != null) {
            long nowMs = System.currentTimeMillis();
            DecorationTarget target = decorationTargetsByUser.compute(authenticatedUser.getIdentifier(),
                    (key, existing) -> (existing == null || existing.isExpired(nowMs))
                            ? new DecorationTarget(baseAuthProvider, nowMs)
                            : existing);

            if (!baseAuthProvider.equals(target.authProviderIdentifier)) {
                logger.debug("Skipping lab-ec2 decoration for user '{}' in auth provider '{}' (already decorating via '{}' chosen {} ms ago).",
                        authenticatedUser.getIdentifier(), baseAuthProvider,
                        target.authProviderIdentifier, nowMs - target.chosenAtMs);
                return context;
            }
        }

        String owner = authenticatedUser.getIdentifier().toLowerCase(Locale.ROOT);

        logger.info("Ensuring lab EC2 instance for user '{}'.",
                authenticatedUser.getIdentifier());
        Instance instance = ensureLabInstance(owner);

        // Use Private IP as requested
        String hostname = instance.privateIpAddress();
        if (hostname == null || hostname.isEmpty()) {
            logger.warn("EC2 instance '{}' for user '{}' has no private IP address yet (state='{}').",
                    instance.instanceId(), authenticatedUser.getIdentifier(),
                    instance.state() != null ? instance.state().name() : "unknown");
        }

        // Determine connection ID
        String connectionId = "lab-" + owner;

        // Check if connection already exists
        Directory<Connection> baseDir = context.getConnectionDirectory();
        Connection existingConnection = null;
        try {
            existingConnection = baseDir.get(connectionId);
        } catch (GuacamoleException e) {
            logger.debug("Failed to check for existing connection '{}': {}", connectionId, e.getMessage());
        }

        if (existingConnection != null) {
            logger.info("Connection '{}' already exists for user '{}'. Using existing connection.",
                    connectionId, authenticatedUser.getIdentifier());
            return context;
        }

        logger.info("Using lab instance '{}' at host '{}' for user '{}'.",
                instance.instanceId(), hostname, authenticatedUser.getIdentifier());

        LabActiveConnectionDirectory mergedActiveConnections =
                new LabActiveConnectionDirectory(context.getActiveConnectionDirectory());

        Connection labConnection = buildLabConnection(owner, hostname,
                authenticatedUser.getIdentifier(),
                credentials != null ? credentials.getRemoteHostname() : null,
                mergedActiveConnections);
        logger.debug("Prepared lab connection '{}' (name='{}') for user '{}' using protocol='{}' port='{}'.",
                labConnection.getIdentifier(), labConnection.getName(),
                authenticatedUser.getIdentifier(), protocol, port);

        final String rootId = context.getRootConnectionGroup().getIdentifier();
        labConnection.setParentIdentifier(rootId);

        Directory<Connection> mergedDir = new LabMergingConnectionDirectory(
                baseDir, labConnection);

        final ConnectionGroup mergedRoot = new DelegatingConnectionGroup(context.getRootConnectionGroup()) {
            @Override
            public Set<String> getConnectionIdentifiers() throws GuacamoleException {
                Set<String> ids = new HashSet<>(super.getConnectionIdentifiers());
                ids.add(labConnection.getIdentifier());
                return Collections.unmodifiableSet(ids);
            }
        };

        final User mergedSelf = new DelegatingUser(context.self()) {

            @Override
            public Permissions getEffectivePermissions() throws GuacamoleException {
                Permissions effective = super.getEffectivePermissions();
                return new LabPermissions(effective, labConnection.getIdentifier());
            }

            @Override
            public ObjectPermissionSet getConnectionPermissions() throws GuacamoleException {
                return new LabObjectPermissionSet(super.getConnectionPermissions(),
                        labConnection.getIdentifier());
            }

        };

        try {
            Permissions effective = mergedSelf.getEffectivePermissions();
            boolean hasRead = effective.getConnectionPermissions().hasPermission(
                    ObjectPermission.Type.READ, labConnection.getIdentifier());
            logger.debug("Effective permissions for user '{}' include READ on injected connection '{}': {}",
                    authenticatedUser.getIdentifier(), labConnection.getIdentifier(), hasRead);
        } catch (GuacamoleException e) {
            logger.warn("Unable to verify effective permissions for injected connection '{}' for user '{}': {}",
                    labConnection.getIdentifier(), authenticatedUser.getIdentifier(), e.getMessage());
        }

        try {
            logger.debug("Merged root connection identifiers for user '{}': {}",
                    authenticatedUser.getIdentifier(), mergedRoot.getConnectionIdentifiers());
        } catch (GuacamoleException e) {
            logger.debug("Unable to enumerate merged root connections for user '{}': {}",
                    authenticatedUser.getIdentifier(), e.getMessage());
        }

        return new DelegatingUserContext(context) {
            @Override
            public Directory<Connection> getConnectionDirectory() throws GuacamoleException {
                return mergedDir;
            }

            @Override
            public ConnectionGroup getRootConnectionGroup() throws GuacamoleException {
                return mergedRoot;
            }

            @Override
            public User self() {
                return mergedSelf;
            }

            @Override
            public Directory<ActiveConnection> getActiveConnectionDirectory() throws GuacamoleException {
                return mergedActiveConnections;
            }
        };
    }

    /**
     * Permissions wrapper that ensures the injected lab connection is actually
     * visible/usable (READ permission) within the decorated user context.
     */
    private static final class LabPermissions implements Permissions {

        private final Permissions delegate;
        private final String labConnectionId;

        private LabPermissions(Permissions delegate, String labConnectionId) {
            this.delegate = delegate;
            this.labConnectionId = labConnectionId;
        }

        @Override
        public ObjectPermissionSet getConnectionPermissions() throws GuacamoleException {
            return new LabObjectPermissionSet(delegate.getConnectionPermissions(), labConnectionId);
        }

        @Override
        public ObjectPermissionSet getActiveConnectionPermissions() throws GuacamoleException {
            return delegate.getActiveConnectionPermissions();
        }

        @Override
        public ObjectPermissionSet getConnectionGroupPermissions() throws GuacamoleException {
            return delegate.getConnectionGroupPermissions();
        }

        @Override
        public ObjectPermissionSet getSharingProfilePermissions() throws GuacamoleException {
            return delegate.getSharingProfilePermissions();
        }

        @Override
        public org.apache.guacamole.net.auth.permission.SystemPermissionSet getSystemPermissions()
                throws GuacamoleException {
            return delegate.getSystemPermissions();
        }

        @Override
        public ObjectPermissionSet getUserPermissions() throws GuacamoleException {
            return delegate.getUserPermissions();
        }

        @Override
        public ObjectPermissionSet getUserGroupPermissions() throws GuacamoleException {
            return delegate.getUserGroupPermissions();
        }

    }

    /**
     * ObjectPermissionSet wrapper that grants READ permission for the injected
     * lab connection identifier.
     */
    private static final class LabObjectPermissionSet implements ObjectPermissionSet {

        private final ObjectPermissionSet delegate;
        private final String labConnectionId;

        private LabObjectPermissionSet(ObjectPermissionSet delegate, String labConnectionId) {
            this.delegate = delegate;
            this.labConnectionId = labConnectionId;
        }

        @Override
        public boolean hasPermission(ObjectPermission.Type permission, String identifier)
                throws GuacamoleException {

            if (labConnectionId.equals(identifier) && permission == ObjectPermission.Type.READ)
                return true;

            return delegate.hasPermission(permission, identifier);
        }

        @Override
        public void addPermission(ObjectPermission.Type permission, String identifier)
                throws GuacamoleException {
            delegate.addPermission(permission, identifier);
        }

        @Override
        public void removePermission(ObjectPermission.Type permission, String identifier)
                throws GuacamoleException {
            delegate.removePermission(permission, identifier);
        }

        @Override
        public Collection<String> getAccessibleObjects(
                Collection<ObjectPermission.Type> permissions,
                Collection<String> identifiers) throws GuacamoleException {

            Collection<String> accessible = new ArrayList<>(
                    delegate.getAccessibleObjects(permissions, identifiers));

            if (identifiers.contains(labConnectionId)
                    && permissions.contains(ObjectPermission.Type.READ)
                    && !accessible.contains(labConnectionId))
                accessible.add(labConnectionId);

            return accessible;
        }

        @Override
        public Set<ObjectPermission> getPermissions() throws GuacamoleException {
            Set<ObjectPermission> merged = new HashSet<>(delegate.getPermissions());
            merged.add(new ObjectPermission(ObjectPermission.Type.READ, labConnectionId));
            return merged;
        }

        @Override
        public void addPermissions(Set<ObjectPermission> permissions) throws GuacamoleException {
            delegate.addPermissions(permissions);
        }

        @Override
        public void removePermissions(Set<ObjectPermission> permissions) throws GuacamoleException {
            delegate.removePermissions(permissions);
        }

    }

    /**
     * Directory wrapper that merges the active connections tracked by the
     * underlying UserContext with active connections created through the lab
     * connection. This is required because {@link SimpleConnection} does not
     * provide active connection tracking, and Guacamole expects active
     * connection metadata to be available for an active tunnel.
     */
    private static final class LabActiveConnectionDirectory implements Directory<ActiveConnection> {

        private final Directory<ActiveConnection> base;
        private final java.util.concurrent.ConcurrentHashMap<String, ActiveConnection> labActiveConnections =
                new java.util.concurrent.ConcurrentHashMap<>();

        private LabActiveConnectionDirectory(Directory<ActiveConnection> base) {
            this.base = base;
        }

        private void register(ActiveConnection activeConnection) {
            if (activeConnection != null && activeConnection.getIdentifier() != null)
                labActiveConnections.put(activeConnection.getIdentifier(), activeConnection);
        }

        private void unregister(String identifier) {
            if (identifier != null)
                labActiveConnections.remove(identifier);
        }

        @Override
        public ActiveConnection get(String identifier) throws GuacamoleException {
            ActiveConnection local = labActiveConnections.get(identifier);
            if (local != null)
                return local;
            return base.get(identifier);
        }

        @Override
        public Collection<ActiveConnection> getAll(Collection<String> identifiers) throws GuacamoleException {

            Set<String> baseIdentifiers = new HashSet<>(identifiers);
            List<ActiveConnection> all = new ArrayList<>();

            // Add local (lab) active connections first
            for (String identifier : identifiers) {
                ActiveConnection local = labActiveConnections.get(identifier);
                if (local != null) {
                    all.add(local);
                    baseIdentifiers.remove(identifier);
                }
            }

            // Add remaining from base directory
            if (!baseIdentifiers.isEmpty())
                all.addAll(base.getAll(baseIdentifiers));

            return all;
        }

        @Override
        public Set<String> getIdentifiers() throws GuacamoleException {
            Set<String> ids = new HashSet<>(base.getIdentifiers());
            ids.addAll(labActiveConnections.keySet());
            return ids;
        }

        @Override
        public void add(ActiveConnection object) throws GuacamoleException {
            base.add(object);
        }

        @Override
        public void update(ActiveConnection object) throws GuacamoleException {
            base.update(object);
        }

        @Override
        public void remove(String identifier) throws GuacamoleException {
            if (labActiveConnections.containsKey(identifier)) {
                labActiveConnections.remove(identifier);
                return;
            }
            base.remove(identifier);
        }

    }

    /**
     * Tunnel wrapper that unregisters its active connection entry when closed.
     */
    private static final class LabTrackingTunnel implements GuacamoleTunnel {

        private final GuacamoleTunnel delegate;
        private final Runnable onClose;

        private LabTrackingTunnel(GuacamoleTunnel delegate, Runnable onClose) {
            this.delegate = delegate;
            this.onClose = onClose;
        }

        @Override
        public org.apache.guacamole.io.GuacamoleReader acquireReader() {
            return delegate.acquireReader();
        }

        @Override
        public void releaseReader() {
            delegate.releaseReader();
        }

        @Override
        public boolean hasQueuedReaderThreads() {
            return delegate.hasQueuedReaderThreads();
        }

        @Override
        public org.apache.guacamole.io.GuacamoleWriter acquireWriter() {
            return delegate.acquireWriter();
        }

        @Override
        public void releaseWriter() {
            delegate.releaseWriter();
        }

        @Override
        public boolean hasQueuedWriterThreads() {
            return delegate.hasQueuedWriterThreads();
        }

        @Override
        public java.util.UUID getUUID() {
            return delegate.getUUID();
        }

        @Override
        public org.apache.guacamole.net.GuacamoleSocket getSocket() {
            return delegate.getSocket();
        }

        @Override
        public void close() throws GuacamoleException {
            try {
                delegate.close();
            }
            finally {
                try {
                    onClose.run();
                }
                catch (RuntimeException ignored) {
                }
            }
        }

        @Override
        public boolean isOpen() {
            return delegate.isOpen();
        }

    }

    /**
     * Connection that behaves like {@link SimpleConnection} but registers an
     * {@link ActiveConnection} entry so Guacamole can resolve active tunnel
     * metadata (ex: /activeConnection/connection/sharingProfiles).
     */
    private static final class LabTrackedConnection extends SimpleConnection {

        private final String username;
        private final String remoteHost;
        private final LabActiveConnectionDirectory activeConnections;

        private LabTrackedConnection(String name, String identifier, GuacamoleConfiguration config,
                String username, String remoteHost, LabActiveConnectionDirectory activeConnections) {
            super(name, identifier, config);
            this.username = username;
            this.remoteHost = remoteHost;
            this.activeConnections = activeConnections;
        }

        @Override
        public GuacamoleTunnel connect(GuacamoleClientInformation info, Map<String, String> tokens)
                throws GuacamoleException {

            GuacamoleTunnel tunnel = super.connect(info, tokens);

            // Track this tunnel as an active connection using the tunnel UUID
            String activeConnectionId = tunnel.getUUID().toString();

            ActiveConnection activeConnection = new AbstractActiveConnection() {
                @Override
                public org.apache.guacamole.net.auth.credentials.UserCredentials getSharingCredentials(String identifier)
                        throws GuacamoleException {
                    throw new org.apache.guacamole.GuacamoleSecurityException("Permission denied.");
                }
            };

            activeConnection.setIdentifier(activeConnectionId);
            activeConnection.setConnectionIdentifier(getIdentifier());
            activeConnection.setUsername(username);
            activeConnection.setRemoteHost(remoteHost);
            activeConnection.setStartDate(new Date());

            LabTrackingTunnel trackingTunnel = new LabTrackingTunnel(tunnel,
                    () -> activeConnections.unregister(activeConnectionId));
            activeConnection.setTunnel(trackingTunnel);

            activeConnections.register(activeConnection);

            return trackingTunnel;
        }

    }

    /**
     * Ensures a per-user EC2 instance exists and is running.
     *
     * @param username
     *                 The username to tag on the instance.
     *
     * @return
     *         The running EC2 instance.
     *
     * @throws GuacamoleException
     *                            If EC2 operations fail.
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
        } else if (InstanceStateName.STOPPED.equals(instance.state().name())
                || InstanceStateName.STOPPING.equals(instance.state().name())) {
            logger.info("Found stopped lab instance '{}' for owner '{}'; starting it.",
                    instance.instanceId(), owner);
            instance = startInstance(instance.instanceId());
        } else {
            logger.info("Using existing lab instance '{}' in state '{}' for owner '{}'.",
                    instance.instanceId(), instance.state().name(), owner);
        }

        return instance;
    }

    /**
     * Starts an existing instance and waits for it to become available.
     *
     * @param instanceId
     *                   The ID of the instance to start.
     *
     * @return
     *         The updated instance details.
     *
     * @throws GuacamoleException
     *                            If the instance cannot be started.
     */
    private Instance startInstance(String instanceId) throws GuacamoleException {
        try {
            logger.debug("Starting EC2 instance '{}'.", instanceId);
            StartInstancesResponse startResponse = ec2.startInstances(builder -> builder.instanceIds(instanceId));
            if (startResponse.startingInstances().isEmpty())
                throw new GuacamoleServerException("Failed to start lab instance " + instanceId);

            long waitStart = System.currentTimeMillis();
            ec2.waiter().waitUntilInstanceRunning(waiter -> waiter.instanceIds(instanceId));
            logger.info("Waited {} ms for instance '{}' to report running.",
                    System.currentTimeMillis() - waitStart, instanceId);

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
     *              The owner tag value to assign.
     *
     * @return
     *         The new EC2 instance.
     *
     * @throws GuacamoleException
     *                            If the instance cannot be created.
     */
    private Instance runLabInstance(String owner) throws GuacamoleException {
        try {
            logger.debug("Preparing to run new lab instance for owner '{}'.", owner);
            RunInstancesRequest.Builder builder = RunInstancesRequest.builder()
                    .minCount(1)
                    .maxCount(1);

            if (launchTemplateId != null && !launchTemplateId.isEmpty()) {
                builder = builder.launchTemplate(template -> template.launchTemplateId(launchTemplateId));
            } else {
                if (labAmiId == null || labAmiId.isEmpty())
                    throw new GuacamoleServerException(
                            "lab-ec2-ami-id must be set when no launch template is provided.");

                builder = builder.imageId(labAmiId)
                        .instanceType(InstanceType.fromValue(instanceType));

                InstanceNetworkInterfaceSpecification.Builder netBuilder = InstanceNetworkInterfaceSpecification.builder()
                        .deviceIndex(0)
                        .associatePublicIpAddress(assignPublicIp);

                if (subnetId != null && !subnetId.isEmpty())
                    netBuilder.subnetId(subnetId);

                if (!securityGroupIds.isEmpty())
                    netBuilder.groups(securityGroupIds);

                builder.networkInterfaces(netBuilder.build());
            }

            builder = builder.tagSpecifications(TagSpecification.builder()
                    .resourceType(ResourceType.INSTANCE)
                    .tags(
                            Tag.builder().key("Name").value("guac-lab-" + owner).build(),
                            Tag.builder().key("LabOwner").value(owner).build(),
                            Tag.builder().key("LabEnv").value("guac").build())
                    .build());

            RunInstancesResponse runResponse = ec2.runInstances(builder.build());
            Instance instance = runResponse.instances().get(0);

            long waitStart = System.currentTimeMillis();
            ec2.waiter().waitUntilInstanceRunning(waiter -> waiter.instanceIds(instance.instanceId()));
            logger.info("Waited {} ms for newly-launched instance '{}' (owner='{}') to report running.",
                    System.currentTimeMillis() - waitStart,
                    instance.instanceId(), owner);

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
     *                   The instance ID to query.
     *
     * @return
     *         The instance details.
     *
     * @throws GuacamoleException
     *                            If the instance cannot be described.
     */
    private Instance getInstanceById(String instanceId) throws GuacamoleException {
        try {
            logger.debug("Describing EC2 instance '{}'.", instanceId);
            DescribeInstancesResponse response = ec2.describeInstances(builder -> builder.instanceIds(instanceId));

            return response.reservations().stream()
                    .flatMap(reservation -> reservation.instances().stream())
                    .findFirst()
                    .orElseThrow(() -> new GuacamoleServerException(
                            "Unable to load lab instance " + instanceId));
        } catch (SdkException e) {
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
        } catch (SdkException e) {
            throw new GuacamoleServerException("Unable to describe EC2 instances.", e);
        }
    }

    /**
     * Builds a connection targeting the given host for the specified user.
     */
    private Connection buildLabConnection(String owner, String hostname, String username,
            String remoteHost, LabActiveConnectionDirectory activeConnections) {

        String connectionId = "lab-" + owner;

        GuacamoleConfiguration config = new GuacamoleConfiguration();
        config.setProtocol(protocol);
        config.setParameter("hostname", hostname);
        config.setParameter("port", port);
        config.setParameter("username", vmUsername);
        config.setParameter("ignore-cert", "true");

        if (vmPassword != null && !vmPassword.isEmpty())
            config.setParameter("password", vmPassword);

        logger.debug(
                "Building lab connection config for '{}' -> hostname='{}' port='{}' vmUsername='{}' passwordSet={}",
                connectionId, hostname, port, vmUsername,
                vmPassword != null && !vmPassword.isEmpty());

        return new LabTrackedConnection("My Lab VM", connectionId, config,
                username, remoteHost, activeConnections);
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
