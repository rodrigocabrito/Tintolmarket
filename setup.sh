#!/bin/bash

# in case you are having issues with running this script, run the command below first
# $ chmod +x setup.sh

## CREATE KEYSTORES

# Specify the location of the keystore files
KEYSTORE_DIR="src/keystores"

################################################################################################################################################################

# Specify the keystore file name
SERVER_KEYSTORE_FILE="server_keyStore.jks"

# Specify the alias and password for the key pair

SERVER_KEYSTORE_PASSWORD="server_keyStore_passw"
SERVER_KEY_ALIAS="server_key_alias"
SERVER_KEY_PASSWORD=$SERVER_KEYSTORE_PASSWORD

# Concatenate the path and filename to create the full path to the truststore file
SERVER_KEYSTORE_PATH="$KEYSTORE_DIR/$SERVER_KEYSTORE_FILE"

################################################################################################################################################################

# Specify the keystore file name
USER1_KEYSTORE_FILE="user1_keyStore.jks"

# Specify the alias and password for the key pair
USER1_KEYSTORE_PASSWORD="user1_keyStore_passw"
USER1_KEY_ALIAS="user1_key_alias"
USER1_KEY_PASSWORD=$USER1_KEYSTORE_PASSWORD

# Concatenate the path and filename to create the full path to the truststore file
USER1_KEYSTORE_PATH="$KEYSTORE_DIR/$USER1_KEYSTORE_FILE"

################################################################################################################################################################

echo "criar keySTores para server e clientes"

# Generate a new RSA key pair with a 2048-bit key size
keytool -genkeypair -alias $SERVER_KEY_ALIAS -keyalg RSA -keysize 2048 -keystore $SERVER_KEYSTORE_PATH -storepass $SERVER_KEYSTORE_PASSWORD -keypass $SERVER_KEY_PASSWORD

keytool -genkeypair -alias $USER1_KEY_ALIAS -keyalg RSA -keysize 2048 -keystore $USER1_KEYSTORE_PATH -storepass $USER1_KEYSTORE_PASSWORD -keypass $USER1_KEY_PASSWORD

################################################################################################################################################################

## CREATE TRUSTSTORE

# Specify the path to the directory where the truststore file will be stored
TRUSTSTORE_DIR="src/keystores"

# Specify the filename and password for the truststore
TRUSTSTORE_FILE="tintolmarket_trustStore.jks"
TRUSTSTORE_PASSW="changeit"

# Specify the alias for the server certificate
SERVER_CERT_ALIAS="trustStore_alias"

# Concatenate the path and filename to create the full path to the truststore file
TRUSTSTORE_PATH="$TRUSTSTORE_DIR/$TRUSTSTORE_FILE"

echo "create trustStore"

# Create a truststore with the server certificate
keytool -genkeypair -alias $SERVER_CERT_ALIAS -keystore $TRUSTSTORE_PATH -storepass $TRUSTSTORE_PASSW

################################################################################################################################################################

# Specify the alias and password for the trusted certificate
CERT_ALIAS="certficate_alias"
CERT_PASSWORD="certificate_passw"

# Specify the path to the trusted certificate file
CERT_FILE="/path/to/trusted/certificate.crt"

echo "add certificates to the trustStore"

# Import the trusted certificate into the truststore
#keytool -importcert -alias $CERT_ALIAS -file $CERT_FILE -keystore $TRUSTSTORE_PATH -storepass $CERT_PASSWORD

# Set the file permissions to read-only
# chmod 400 $TRUSTSTORE_PATH

################################################################################################################################################################

CERTIFICATE_PATH="path/to/certificate.crt"

# EXPORT A CERTIFICATE WITH ALIAS "mycertificate" TO A FILE NAMED "mycertificate.crt" FROM A KEYSTORE "mykeystore.jks" WITH PASSWORD "mypassword"

#keytool -exportcert -alias mycertificate -keystore mykeystore.jks -storepass mypassword -rfc -file $CERTIFICATE_PATH
