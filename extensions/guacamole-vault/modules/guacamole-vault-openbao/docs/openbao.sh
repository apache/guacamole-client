#! /bin/sh
#
# Test script to start openbao server for testing and print a test plan
#  This is my teeest scrippt , don't excpect it to work on your mmachine

pwgen() {
  _alpha=${2:-'_A-Z-a-z0-9'}
  tr -dc "$_alpha" < /dev/random 2> /dev/null | head -c "$1"
}

[ -z "$LDAP_URI" ] && { echo "Must supply a LDAP_URI with the address of the LDAP server" && exit 1; }
[ -z "$LDAP_DN" ] && { echo "Must supply a LDAP_DN with the distinguished name to use with teh vault" && exit 1; }

docker rm -f openbao > /dev/null 2>&1
docker run --detach --name openbao --publish 8200:8200 openbao/openbao:2.5 > /dev/null 2>&1
sleep 2
VAULT_TOKEN=$(docker logs openbao 2> /dev/null | grep "Root Token" | cut -d: -f2 | xargs)
UNSEAL_KEY=$(docker logs openbao 2> /dev/null | grep "Unseal" | cut -d: -f2 | xargs)
echo "# Create Guacamole limited access policy"
cat << EOF > guacamole.hcl
path "sys/mounts" {
  capabilities = ["read"]
}
path "kv1/*" {
  capabilities = ["read"]
}
path "kv2/*" {
  capabilities = ["read"]
}
path "ssh/sign/guacamole_cert" {
  capabilities = ["update", "create"]
}
path "ssh/creds/guacamole_otp" {
  capabilities = ["update"]
}
path "ldap/*" {
  capabilities = ["read"]
}
path "db/*" {
  capabilities = ["read"]
}
EOF

curl -s --header "X-Vault-Token: $VAULT_TOKEN"  \
    --request POST --data-urlencode  "policy@guacamole.hcl" \
    http://127.0.0.1:8200/v1/sys/policy/guacamole
/bin/rm guacamole.hcl

echo "# Enable KV_1 secret engine"
curl -s --header "X-Vault-Token: $VAULT_TOKEN" --header "Content-Type: application/json" \
    --request POST --data '{"type": "kv", "options": {"version": "1"}}' \
    http://127.0.0.1:8200/v1/sys/mounts/kv1 > /dev/null 2>&1
echo "# Write K-V secret: kv1/users/kali/{\"username\":  \"kali\", \"password\": \"kali\"}"
curl -s --header "X-Vault-Token: $VAULT_TOKEN" --header "Content-Type: application/json" \
    --request POST --data '{"username": "kali", "password": "kali"}' \
    http://127.0.0.1:8200/v1/kv1/users/kali

echo "# Enable KV_2 secret engine"
curl -s --header "X-Vault-Token: $VAULT_TOKEN" --header "Content-Type: application/json" \
    --request POST --data '{"type": "kv", "options": {"version": "2"}}' \
    http://127.0.0.1:8200/v1/sys/mounts/kv2 > /dev/null 2>&1
echo "# Write K-V secret: kv2/users/kali/{\"username\":  \"kali\", \"password\": \"kali\"}"
curl -s --header "X-Vault-Token: $VAULT_TOKEN" --header "Content-Type: application/json" \
    --request POST --data '{"data": {"username": "kali", "password": "kali"}}' \
    http://127.0.0.1:8200/v1/kv2/data/users/kali > /dev/null 2>&1

echo "# Enable LDAP secret engine"
curl -s --header "X-Vault-Token: $VAULT_TOKEN" --header "Content-Type: application/json" \
    --request POST --data '{"type": "ldap"}' \
    http://127.0.0.1:8200/v1/sys/mounts/ldap > /dev/null 2>&1

echo "# Create ldap password policy"
cat << EOHCL > ldap-policy.hcl
length = 20

rule "charset" {
  charset = "abcdefghijklmnopqrstuvwxyz"
  min_chars = 1
}

rule "charset" {
  charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
  min_chars = 1
}

rule "charset" {
  charset = "0123456789"
  min_chars = 1
}

rule "charset" {
  charset = "!@#%^&*"
  min_chars = 1
}
EOHCL
curl -s --header "X-Vault-Token: $VAULT_TOKEN"  \
    --request POST --data-urlencode  "policy@ldap-policy.hcl" \
    http://127.0.0.1:8200/v1/sys/policies/password/ldap-policy
rm ldap-policy.hcl

echo "# Create LDAP account in the vault"
USER_DN=$(echo $LDAP_DN | cut -d, -f2-)
curl -s \
  --header "X-Vault-Token: $VAULT_TOKEN" \
  --header "Content-Type: application/json" \
  --request PUT \
  --data '{
    "schema": "openldap",
    "url": "'$LDAP_URI'",
    "binddn": "'$LDAP_DN'",
    "userdn": "'$USER_DN'",
    "userattr": "cn",
    "bindpass": "your_secret_password_here",
    "password_policy": "ldap-policy",
    "password_hash": "plaintext",
    "insecure_tls": true
  }' \
  http://127.0.0.1:8200/v1/ldap/config

echo "# Rotate the vault LDAP password"
curl -s \
  --header "X-Vault-Token: $VAULT_TOKEN" \
  --request POST \
  http://127.0.0.1:8200/v1/ldap/rotate-root

echo "# Add a static LDAP account"
curl -s \
  --header "X-Vault-Token: $VAULT_TOKEN" \
  --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "username": "testuser",
    "dn": "uid=testuser,'$USER_DN'",
    "rotation_period": "1h"
  }' \
  http://127.0.0.1:8200/v1/ldap/static-role/testuser

echo "# Add Dynamic LDAP role"
curl -s \
  --header "X-Vault-Token: $VAULT_TOKEN" \
  --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "creation_ldif": "dn:uid={{.Username}},'$USER_DN'\nobjectClass: top\nobjectClass: inetOrgPerson\nobjectClass: posixAccount\nobjectClass: shadowAccount\nobjectClass: enabledObjectClass\nuid: {{.Username}}\ncn: {{.Username}}\nsn: {{.Username}}\nuserPassword: {{.Password}}\ngidNumber: 50\nuidNumber: 2000\nhomeDirectory: /home/{{.Username}}\nloginShell: /bin/sh\nshadowLastChange: 99999\nshadowMax: 180\nshadowWarning: 7\nenabled: TRUE",
    "deletion_ldif": "dn: uid={{.Username}},'$USER_DN'\nchangetype: delete",
    "username_template": "v_{{.RoleName}}_{{random 6}}",
    "dn": "uid=guacamole,'$USER_DN'",
    "default_ttl": "1h",
    "max_ttl": "24h"
  }' \
  http://127.0.0.1:8200/v1/ldap/role/guacamole


echo "# Add LDAP service accounts"
curl -s \
  --header "X-Vault-Token: $VAULT_TOKEN" \
  --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "userdn": "'$USER_DN'",
    "userattr": "cn",
    "ttl": "1h",
    "max_ttl": "24h",
    "disable_checkin_enforcement": "true",
    "service_account_names": "testuser"
  }' \
  http://127.0.0.1:8200/v1/ldap/library/guacamole

echo "# Enable Postgres database secret engine"
curl -s --header "X-Vault-Token: $VAULT_TOKEN" --header "Content-Type: application/json" \
    --request POST --data '{"type": "database"}' \
    http://127.0.0.1:8200/v1/sys/mounts/db > /dev/null 2>&1

echo "# Enable SSH secret engine"
curl -s --header "X-Vault-Token: $VAULT_TOKEN" --header "Content-Type: application/json" \
    --request POST --data '{"type": "ssh"}' \
    http://127.0.0.1:8200/v1/sys/mounts/ssh > /dev/null 2>&1

echo "# Generate SSH CA"
USER_CA=$(curl -s --header "X-Vault-Token: $VAULT_TOKEN" --header "Content-Type: application/json" \
    --request POST --data '{"generate_signing_key": true}' \
    http://127.0.0.1:8200/v1/ssh/config/ca | jq -rc ".data.public_key" | xargs)

echo "# Create SSH certificate signing role 'signer'"
curl -s --header "X-Vault-Token: $VAULT_TOKEN" --header "Content-Type: application/json" \
    --request POST --data '{"algorithm_signer": "rsa-sha2-256", "allow_user_certificates": true, "allowed_users": "*", "key_type": "ca", "max_ttl": "30m", "allowed_extensions": "permit-pty"}' \
    http://127.0.0.1:8200/v1/ssh/roles/guacamole_cert

echo "# Create SSH OTP for account 'testuser'"
curl -s --header "X-Vault-Token: $VAULT_TOKEN" --header "Content-Type: application/json" \
    --request POST --data '{"key_type": "otp", "default_user": "testuser", "cidr_list": "0.0.0.0/0"}' \
    http://127.0.0.1:8200/v1/ssh/roles/guacamole_otp
curl -s --header "X-Vault-Token: $VAULT_TOKEN" --header "Content-Type: application/json" \
    --request POST --data '{"roles": "guacamole_otp"}' \
    http://127.0.0.1:8200/v1/ssh/config/zeroaddress

echo "# Enable userpass authentication and create guacamole user in vault"
curl -s --header "X-Vault-Token: $VAULT_TOKEN" --header "Content-Type: application/json" \
    --request POST --data '{"type": "userpass"}' \
    http://127.0.0.1:8200/v1/sys/auth/userpass
curl -s --header "X-Vault-Token: $VAULT_TOKEN" --header "Content-Type: application/json" \
    --request POST --data '{"default_ttl_lease": "10m", "token_type": "service"}' \
    http://127.0.0.1:8200/v1/sys/auth/userpass/tune
USER_PASSWORD=$(pwgen 16)
curl -s --header "X-Vault-Token: $VAULT_TOKEN" --header "Content-Type: application/json" \
    --request POST --data '{"password": "'$USER_PASSWORD'", "policies": "guacamole"}' \
    http://127.0.0.1:8200/v1/auth/userpass/users/guacamole | jq

echo
echo "# OpenBao root token    : $VAULT_TOKEN"
echo "# Openbao Unsealing key : $UNSEAL_KEY"
echo "# Username              : guacamole"
echo "# password              : $USER_PASSWORD"

cat << EOF

# TEST PLAN
# ---------
#
#  0. Setup test kali server with a test account with username and passwrd kali/kali
#     Add an account 'testuser' to the kali machine that will be used with SSH OTP
#     utility. Setup xrdp on the kali machine
#
#     Setup a second machine with accounts managed by LDAP, and give LDAP credentials
#     to OpenBao
#  1. Add following to guacamole.properties and restart guacamole, to test with root token
#        vault-uri: http://localhost:8200
#        vault-token: $VAULT_TOKEN
#  2. Add the tokens to my test kali server with RDP
#        \${vault://kv1/users/kali/password} and \${vault://kv1/users/kali/username}
#     Test that connection kali server actually works

#  3. Test Guacamole with a token with limited rights and an infinite TTL
#     First generate the token with the zéro TTL and limited right

curl -s --header "X-Vault-Token: $VAULT_TOKEN" --header "Content-Type: application/json" \
    --request POST --data '{"policies": ["guacamole"], ttl: "0m", "renewable": true}' \
    http://127.0.0.1:8200/v1/auth/token/create | jq

#     Add the printed token to the 'vault-token' in gaucamole.properties and restart
#     Guacamole.
#
#     Test that the previous test still works. This token will be used from here on out
#     so that the limited permissions are tested
#  4. Add the tokens to my test kali server with SSH
#        \${vault://kv1/users/kali/password} and \${vault://kv1/users/kali/username}
#     Test that connection kali server actually works
#  5. Add the tokens to my test kali server with SSH
#        \${vault://kv2/users/kali/password} and \${vault://kv2/users/kali/username}
#     Test that connection kali server actually works
#  6. Add 'TrustedUserCAKeys' to test kali server,
#
#     Do the following commands on test kali machine:

cat << EOT > /etc/ssh/trusted_user_ca.pem
$USER_CA
EOT
chmod 700 /etc/ssh/trusted_user_ca.pem
chown sshd:sshd /etc/ssh/trusted_user_ca.pem
echo "TrustedUserCAKeys /etc/ssh/trusted_user_ca.pem" >> /etc/ssh/sshd_config
pkill -SIGHUP /usr/sbin/sshd

#     Use the username "kali" in SSH connection and add these tokens to
#     the private and public ssh keys
#
#        \${vault://ssh/sign/guacamole_sign/private} and \${vault://ssh/sign/guacamole_sign/public}
#
#     Test that the SSH connection to the kali machine works
#  7. Use RSA SSH certiciates. The previous tested used the default "ed25519" ssh
#     certificates. Add the following to guacamole.properties
#          vault-ssh-type: rsa
#     Restart guacamole. Test that the SSH connection to the kali machine
#     works
#
#     Run the following command on the test kali server

sed -i -e "/^TrustedUserCAKeys/d" /etc/ssh/sshd_config
pkill -SIGHUP /usr/sbin/sshd

#  8. Ensure the vault otp helper is on the test kali machine and configured
#     Add the following tokens to the test kali connection
#        \${vault://ssh/creds/guacamole_otp/username} and \${vault://ssh/creds/guacamole_otp/password}
#     Use the username 'testuser' on the SSH connection, so and this user to the test machine
#     if needed. Now setup the test machine with the code

cat << EOT >> /usr/local/sbin/verify_otp.sh
#!/bin/sh
set -u

IFS= read -r OTP

USERNAME="\${PAM_USER}"

[ -z "\${OTP}" ] && exit 1
[ -z "\${USERNAME}" ] && exit 2

RESPONSE=\$(curl -s \
  --fail \
  --request POST \
  --header "Content-Type: application/json" \
  --data '{"otp": "'\${OTP}'"}' \
  http://127.0.0.1:8200/v1/ssh/verify)
[ "\$?" -eq 0 ] || exit 3

[ "\$(echo \$RESPONSE | jq -r .data.username)" = "\$USERNAME" ] || exit 4
EOT
chmod 755 /usr/local/bin/vault_verify_otp.sh
sed -i '1s:^:auth sufficent pam_exec.so expose_authtok /usr/local/sbin/verify_otp.sh:' /etc/pam.d/sshd
pkill -SIGHUP sshd

#     Test that the connexion works. After remove the test code from the test machine like

rm /usr/local/sbin/verify_otp.sh
sed -i '1d' /etc/pam.d/sshd

#  9. Add the static ldap tokens to the SSH connection
#        \${vault://ldap/static-cred/testuser/password} and \${vault://ldap/static-cred/testuser/username}
#     Test that the SSH connection to the kali machine works
#  10. Add the dynamic ldaptokens to the SSH connection
#        \${vault://ldap/creds/guacamole/password} and \${vault://ldap/dynamic/guacamole/username}
#     Test that the SSH connection to the kali machine works
#  11. [TODO LDAP Service] Add the ldap service tokens to the SSH connection
#        \${vault://ldap/static/users/kali/password} and \${vault://ldap/static/users/kali/username}
#     Test that the SSH connection to the kali machine works
#  12. A VaultAgent can be simulated by using a token sink file as follows. First create
#     A short lived (10 minutes) non renewable token with the command

curl -s --header "X-Vault-Token: $VAULT_TOKEN" --header "Content-Type: application/json" \
    --request POST --data '{"policies": ["guacamole"], ttl: "10m", "renewable": false}' \
    http://127.0.0.1:8200/v1/auth/token/create | jq -r .auth.client_token > /etc/guacamole.token
chmod 700 /etc/guacamole.token
chown guacamole:guacamole /etc/guacamole.token

#     Change the vault of vault-token in guacamole-properties to
#         vault-token: /etc/guacamole.token
#     and restart Guacamole, and all of this within 10 minutes. Test access to kali
#     machine still works. Now generate a new infinite token with the command

curl -s --header "X-Vault-Token: $VAULT_TOKEN" --header "Content-Type: application/json" \
    --request POST --data '{"policies": ["guacamole"], ttl: "0m", "renewable": true}' \
    http://127.0.0.1:8200/v1/auth/token/create | jq -r .auth.client_token > /etc/guacamole.token

#    Wait 10 minutes and see if the access to the kali machine still works as the
#    access has beed renewed with the new token.
#  13. Test Guacamole with username and password. Remove 'token-uri' from
#     guacamole.properties and replace with
#         vault-username: guacamole
#         vault-password: $USER_PASSWORD
#     Restart guacamole and test that one of the previous connections still works
#
#     Wait 10 minutes, so as to test the static token renewal of the openbao driver
#     and retest that one of the connections above works
#  14. [TODO Database password test] Create a "guacamole.properties.vlt"  file with
#      entries for the database like
#           mysql-username: vault://db/guacamole/username
#           mysql-password: vault://db/guacamole/password
#      or adapt for your database engine. Restart guacamole. If it functions at all the
#      database is accessible
#  15. Create a file "vault-token-mapping.yml" with the tokens
#           KALI_USERNAME: vault://kv1/users/kali/password
#           KALI_PASSWORD: vault://kv1/users/kali/username
#      Add to the kali ssh connection the tokens \${KALI_USERNAME} and \${KALI_PASSWORD}
#      and see if the connection still works
#  16. In the kali machine leave the user as "kali" but the password should use the
#     token \${vault://kv1/users/{USERNAME}/password}. Test that the connection works

EOF
