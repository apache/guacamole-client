#!/bin/bash

# Script to build extension and start both Keycloak and Guacamole together

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}=========================================="
echo "Guacamole + Keycloak Full Setup"
echo "==========================================${NC}"
echo ""

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Error: Maven is not installed. Please install Maven first.${NC}"
    exit 1
fi

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Error: Docker is not installed. Please install Docker first.${NC}"
    exit 1
fi

# Check if Docker Compose is installed
if ! docker compose version &> /dev/null; then
    echo -e "${RED}Error: Docker Compose is not installed. Please install Docker Compose first.${NC}"
    exit 1
fi

echo -e "${GREEN}Step 1: Building OpenID extension...${NC}"
cd extensions/guacamole-auth-sso
mvn clean package -DskipTests
cd ../..

# Find the JAR file
JAR_FILE=$(find extensions/guacamole-auth-sso/modules/guacamole-auth-sso-openid/target -name "guacamole-auth-sso-openid-*.jar" | head -1)

if [ -z "$JAR_FILE" ]; then
    echo -e "${RED}Error: Extension JAR file not found. Build may have failed.${NC}"
    exit 1
fi

echo -e "${GREEN}Found extension JAR: $JAR_FILE${NC}"
echo ""

echo -e "${GREEN}Step 2: Creating deployment directory structure...${NC}"
mkdir -p deployment/extensions
mkdir -p deployment

# Copy extension JAR
cp "$JAR_FILE" deployment/extensions/
echo -e "${GREEN}Copied extension JAR to deployment/extensions/${NC}"

# Copy user-mapping.xml
if [ -f "user-mapping-example.xml" ]; then
    cp user-mapping-example.xml deployment/user-mapping.xml
    echo -e "${GREEN}Copied user-mapping-example.xml to deployment/user-mapping.xml${NC}"
else
    echo -e "${YELLOW}Warning: user-mapping-example.xml not found. Creating minimal example...${NC}"
    cat > deployment/user-mapping.xml <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<user-mapping>
    <authorize username="connections" password="dummy">
        <connection name="test-vnc">
            <protocol>vnc</protocol>
            <param name="hostname">localhost</param>
            <param name="port">5900</param>
            <param name="password">testpass</param>
        </connection>
    </authorize>
</user-mapping>
EOF
fi

# Create minimal guacamole.properties if it doesn't exist
if [ ! -f "deployment/guacamole.properties" ]; then
    echo -e "${YELLOW}Creating guacamole.properties with Keycloak configuration...${NC}"
    cat > deployment/guacamole.properties <<'EOF'
# OpenID Configuration (Keycloak running in same docker-compose)
# Note: Use localhost:8090 for browser redirects, but keycloak:8080 works for internal network
openid-authorization-endpoint: http://localhost:8090/realms/myrealm/protocol/openid-connect/auth
openid-jwks-endpoint: http://keycloak:8080/realms/myrealm/protocol/openid-connect/certs
openid-issuer: http://localhost:8090/realms/myrealm
openid-client-id: guacamole-client
openid-redirect-uri: http://localhost:8080/
openid-username-claim-type: email
openid-groups-claim-type: groups
openid-scope: openid email profile

# Guacd Configuration
guacd-hostname: guacd
guacd-port: 4822
EOF
    echo -e "${YELLOW}NOTE: Update deployment/guacamole.properties if your Keycloak realm name is different!${NC}"
    echo -e "${YELLOW}NOTE: JWKS endpoint uses 'keycloak:8080' for internal Docker network access${NC}"
else
    echo -e "${GREEN}Using existing deployment/guacamole.properties${NC}"
    echo -e "${YELLOW}NOTE: For best performance, use 'keycloak:8080' for JWKS endpoint (internal network)${NC}"
fi

echo ""
echo -e "${GREEN}Step 3: Deployment structure created:${NC}"
echo "deployment/"
echo "├── extensions/"
echo "│   └── $(basename "$JAR_FILE")"
echo "├── user-mapping.xml"
echo "└── guacamole.properties"
echo ""

echo -e "${GREEN}Step 4: Starting all services (Keycloak + Guacamole)...${NC}"
echo ""

docker compose -f docker-compose-full.yml up -d

echo ""
echo -e "${GREEN}=========================================="
echo "Setup Complete!"
echo "==========================================${NC}"
echo ""
echo "Services are starting up..."
echo ""
echo -e "${GREEN}Keycloak Admin Console:${NC} http://localhost:8090"
echo "  Username: admin"
echo "  Password: admin"
echo ""
echo -e "${GREEN}Guacamole Web Interface:${NC} http://localhost:8080"
echo ""
echo "To view logs:"
echo "  docker compose -f docker-compose-full.yml logs -f"
echo ""
echo "To view specific service logs:"
echo "  docker compose -f docker-compose-full.yml logs -f keycloak"
echo "  docker compose -f docker-compose-full.yml logs -f guacamole"
echo ""
echo "To stop all services:"
echo "  docker compose -f docker-compose-full.yml down"
echo ""
echo -e "${YELLOW}IMPORTANT:${NC}"
echo "1. Wait for Keycloak to be ready (about 30-60 seconds)"
echo "2. Configure Keycloak (see KEYCLOAK-SETUP.md for details):"
echo "   - Create realm (e.g., 'myrealm')"
echo "   - Create client 'guacamole-client'"
echo "   - Create roles: 'admin', 'console_accesser'"
echo "   - Create test user and assign roles"
echo "   - Configure role mapper (Token Claim Name: 'groups')"
echo "3. Update deployment/guacamole.properties if realm name is different"
echo "4. Check logs to verify extension loaded and user-mapping.xml is read"
echo ""
echo "Checking container status..."
sleep 5
docker compose -f docker-compose-full.yml ps

