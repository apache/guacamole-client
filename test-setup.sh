#!/bin/bash

# Test setup script for Guacamole OpenID extension with user-mapping.xml

set -e

echo "=========================================="
echo "Guacamole OpenID Extension Test Setup"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

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
    echo -e "${YELLOW}Creating minimal guacamole.properties...${NC}"
    cat > deployment/guacamole.properties <<'EOF'
# OpenID Configuration (UPDATE THESE VALUES)
openid-authorization-endpoint: https://keycloak.example.com/auth/realms/myrealm/protocol/openid-connect/auth
openid-jwks-endpoint: https://keycloak.example.com/auth/realms/myrealm/protocol/openid-connect/certs
openid-issuer: https://keycloak.example.com/auth/realms/myrealm
openid-client-id: guacamole-client
openid-redirect-uri: http://localhost:8080/
openid-username-claim-type: email
openid-groups-claim-type: groups
openid-scope: openid email profile

# Guacd Configuration
guacd-hostname: guacd
guacd-port: 4822
EOF
    echo -e "${YELLOW}NOTE: Please update deployment/guacamole.properties with your Keycloak settings!${NC}"
fi

echo ""
echo -e "${GREEN}Step 3: Deployment structure created:${NC}"
echo "deployment/"
echo "├── extensions/"
echo "│   └── $(basename "$JAR_FILE")"
echo "├── user-mapping.xml"
echo "└── guacamole.properties"
echo ""

echo -e "${GREEN}Step 4: Checking Keycloak setup...${NC}"
echo ""
echo -e "${YELLOW}NOTE: Keycloak should be started separately using:${NC}"
echo "  docker compose -f docker-compose-keycloak.yml up -d"
echo ""
read -p "Is Keycloak already running? (y/n) " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Please start Keycloak first, then run this script again.${NC}"
    echo "Or start it now with: docker compose -f docker-compose-keycloak.yml up -d"
    exit 0
fi

echo -e "${GREEN}Step 5: Starting Guacamole Docker containers...${NC}"
echo ""

# Use docker compose (modern syntax)
DOCKER_COMPOSE="docker compose"

$DOCKER_COMPOSE up -d

echo ""
echo -e "${GREEN}=========================================="
echo "Setup Complete!"
echo "==========================================${NC}"
echo ""
echo "Guacamole is starting up..."
echo ""
echo "Access Guacamole at: http://localhost:8080"
echo ""
echo "To view logs:"
echo "  $DOCKER_COMPOSE logs -f guacamole"
echo ""
echo "To stop containers:"
echo "  $DOCKER_COMPOSE down"
echo ""
echo -e "${YELLOW}IMPORTANT:${NC}"
echo "1. Keycloak should be running (start with: docker compose -f docker-compose-keycloak.yml up -d)"
echo "2. Update deployment/guacamole.properties with your Keycloak configuration"
echo "3. Configure Keycloak (see KEYCLOAK-SETUP.md for details):"
echo "   - Create realm, client, roles (admin, console_accesser)"
echo "   - Create test user and assign roles"
echo "   - Configure role mapper to include roles in JWT token"
echo "4. Check logs to verify extension loaded and user-mapping.xml is read"
echo ""
echo "Checking container status..."
sleep 3
$DOCKER_COMPOSE ps

