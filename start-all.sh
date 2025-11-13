#!/bin/bash

# Script to start both Keycloak and Guacamole

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}Starting Keycloak...${NC}"
docker compose -f docker-compose-keycloak.yml up -d

echo ""
echo -e "${YELLOW}Waiting for Keycloak to be ready (this may take 30-60 seconds)...${NC}"
echo "You can monitor progress with: docker compose -f docker-compose-keycloak.yml logs -f keycloak"
echo ""

# Wait for Keycloak to be ready
timeout=120
elapsed=0
while [ $elapsed -lt $timeout ]; do
    if curl -s http://localhost:8090/health/ready > /dev/null 2>&1; then
        echo -e "${GREEN}Keycloak is ready!${NC}"
        break
    fi
    echo -n "."
    sleep 2
    elapsed=$((elapsed + 2))
done

if [ $elapsed -ge $timeout ]; then
    echo ""
    echo -e "${YELLOW}Keycloak is taking longer than expected. It may still be starting.${NC}"
    echo "You can check status with: docker compose -f docker-compose-keycloak.yml ps"
fi

echo ""
echo -e "${GREEN}Keycloak Admin Console: http://localhost:8090${NC}"
echo "Username: admin"
echo "Password: admin"
echo ""
echo -e "${YELLOW}Now run ./test-setup.sh to build and start Guacamole${NC}"

