#!/bin/bash

# in case you are having issues with running this script, run the command below first
# $ chmod +x setup.sh

# Delete all existing keystores and certificates

rm -rf src/keystores/*
rm -rf src/certificates/*

## CREATE KEYSTORES

# Specify the location of the keystore files
KEYSTORE_DIR="src/keystores"

CERTIFICATE_DIR="src/certificates"

################################################################################################################################################################

# Specify the keystore file name
SERVER_KEYSTORE_FILE="server_keyStore.jks"

# Specify the alias and password for the key pair

SERVER_KEYSTORE_PASSWORD="server_keyStore_passw"
SERVER_KEY_ALIAS="server_key_alias"

# Concatenate the path and filename to create the full path to the truststore file
SERVER_KEYSTORE_PATH="$KEYSTORE_DIR/$SERVER_KEYSTORE_FILE"

################################################################################################################################################################

# Specify the keystore file name
USER1_KEYSTORE_FILE="user1_keyStore.jks"

# Specify the alias and password for the key pair
USER1_KEYSTORE_PASSWORD="user1_keyStore_passw"
USER1_KEY_ALIAS="user1_key_alias"

# Concatenate the path and filename to create the full path to the keystore file
USER1_KEYSTORE_PATH="$KEYSTORE_DIR/$USER1_KEYSTORE_FILE"

################################################################################################################################################################

# Specify the keystore file name
USER2_KEYSTORE_FILE="user2_keyStore.jks"

# Specify the alias and password for the key pair
USER2_KEYSTORE_PASSWORD="user2_keyStore_passw"
USER2_KEY_ALIAS="user2_key_alias"

# Concatenate the path and filename to create the full path to the keystore file
USER2_KEYSTORE_PATH="$KEYSTORE_DIR/$USER2_KEYSTORE_FILE"

################################################################################################################################################################

# Specify the keystore file name
USER3_KEYSTORE_FILE="user3_keyStore.jks"

# Specify the alias and password for the key pair
USER3_KEYSTORE_PASSWORD="user3_keyStore_passw"
USER3_KEY_ALIAS="user3_key_alias"

# Concatenate the path and filename to create the full path to the keystore file
USER3_KEYSTORE_PATH="$KEYSTORE_DIR/$USER3_KEYSTORE_FILE"

################################################################################################################################################################

# Specify the keystore file name
USER4_KEYSTORE_FILE="user4_keyStore.jks"

# Specify the alias and password for the key pair
USER4_KEYSTORE_PASSWORD="user4_keyStore_passw"
USER4_KEY_ALIAS="user4_key_alias"

# Concatenate the path and filename to create the full path to the keystore file
USER4_KEYSTORE_PATH="$KEYSTORE_DIR/$USER4_KEYSTORE_FILE"

################################################################################################################################################################

# Concatenate the path and filename to create the full path to the certificate file
SERVER_CERTIFICATE_FILE="server_certificate.cert"
SERVER_CERTIFICATE_PATH="$CERTIFICATE_DIR/$SERVER_CERTIFICATE_FILE"

USER1_CERTIFICATE_FILE="user1_certificate.cert"
USER1_CERTIFICATE_PATH="$CERTIFICATE_DIR/$USER1_CERTIFICATE_FILE"

USER2_CERTIFICATE_FILE="user2_certificate.cert"
USER2_CERTIFICATE_PATH="$CERTIFICATE_DIR/$USER2_CERTIFICATE_FILE"

USER3_CERTIFICATE_FILE="user3_certificate.cert"
USER3_CERTIFICATE_PATH="$CERTIFICATE_DIR/$USER3_CERTIFICATE_FILE"

USER4_CERTIFICATE_FILE="user4_certificate.cert"
USER4_CERTIFICATE_PATH="$CERTIFICATE_DIR/$USER4_CERTIFICATE_FILE"

################################################################################################################################################################

echo "criar keySTores para server e clientes"

# Generate a new RSA key pair with a 2048-bit key size
keytool -genkeypair -alias $SERVER_KEY_ALIAS -keyalg RSA -storetype JCEKS -keysize 2048 -keystore $SERVER_KEYSTORE_PATH -storepass $SERVER_KEYSTORE_PASSWORD -keypass $SERVER_KEYSTORE_PASSWORD

keytool -genkeypair -alias $USER1_KEY_ALIAS -keyalg RSA -storetype JCEKS -keysize 2048 -keystore $USER1_KEYSTORE_PATH -storepass $USER1_KEYSTORE_PASSWORD -keypass $USER1_KEYSTORE_PASSWORD

keytool -genkeypair -alias $USER2_KEY_ALIAS -keyalg RSA -storetype JCEKS -keysize 2048 -keystore $USER2_KEYSTORE_PATH -storepass $USER2_KEYSTORE_PASSWORD -keypass $USER2_KEYSTORE_PASSWORD

keytool -genkeypair -alias $USER3_KEY_ALIAS -keyalg RSA -storetype JCEKS -keysize 2048 -keystore $USER3_KEYSTORE_PATH -storepass $USER3_KEYSTORE_PASSWORD -keypass $USER3_KEYSTORE_PASSWORD

keytool -genkeypair -alias $USER4_KEY_ALIAS -keyalg RSA -storetype JCEKS -keysize 2048 -keystore $USER4_KEYSTORE_PATH -storepass $USER4_KEYSTORE_PASSWORD -keypass $USER4_KEYSTORE_PASSWORD

# Export certificates

echo "Exporting certificates"

echo ">>>passw: server_keyStore_passw"
keytool -exportcert -alias $SERVER_KEY_ALIAS -storetype JCEKS -keystore $SERVER_KEYSTORE_PATH -file $SERVER_CERTIFICATE_PATH

echo ">>>passw: user1_keyStore_passw"
keytool -exportcert -alias $USER1_KEY_ALIAS -storetype JCEKS -keystore $USER1_KEYSTORE_PATH -file $USER1_CERTIFICATE_PATH

echo ">>>passw: user2_keyStore_passw"
keytool -exportcert -alias $USER2_KEY_ALIAS -storetype JCEKS -keystore $USER2_KEYSTORE_PATH -file $USER2_CERTIFICATE_PATH

echo ">>>passw: user3_keyStore_passw"
keytool -exportcert -alias $USER3_KEY_ALIAS -storetype JCEKS -keystore $USER3_KEYSTORE_PATH -file $USER3_CERTIFICATE_PATH

echo ">>>passw: user4_keyStore_passw"
keytool -exportcert -alias $USER4_KEY_ALIAS -storetype JCEKS -keystore $USER4_KEYSTORE_PATH -file $USER4_CERTIFICATE_PATH

################################################################################################################################################################

## CREATE TRUSTSTORE

# Specify the filename and password for the truststore
TRUSTSTORE_FILE="tintolmarket_trustStore.jks"
TRUSTSTORE_PASSWORD="changeit"

# Specify the alias for the server certificate
SERVER_CERT_ALIAS="server_cert_alias"
USER1_CERT_ALIAS="user1_cert_alias"
USER2_CERT_ALIAS="user2_cert_alias"
USER3_CERT_ALIAS="user3_cert_alias"
USER4_CERT_ALIAS="user4_cert_alias"

# Concatenate the path and filename to create the full path to the truststore file
TRUSTSTORE_PATH="$KEYSTORE_DIR/$TRUSTSTORE_FILE"

echo "create trustStore"

# Create a truststore with the server certificate

# keytool -genkeypair -alias $SERVER_CERT_ALIAS -keyalg RSA -storetype JCEKS -keysize 2048 -keystore $TRUSTSTORE_PATH -storepass $TRUSTSTORE_PASSWORD -keypass $TRUSTSTORE_PASSWORD
keytool -genkeypair -alias $SERVER_CERT_ALIAS -keyalg RSA -storetype JCEKS -keysize 2048 -keystore $TRUSTSTORE_PATH -storepass $TRUSTSTORE_PASSWORD -keypass $TRUSTSTORE_PASSWORD

# Empty the truststore
echo "emptying trustStore"
echo ">>>passw: changeit"
keytool -delete -alias $SERVER_CERT_ALIAS -storetype JCEKS -keystore $TRUSTSTORE_PATH

# Import the certificates from the certificate directory to the truststore

echo "import certificates"

echo ">>>passw: changeit"
keytool -importcert -alias $SERVER_CERT_ALIAS -file $SERVER_CERTIFICATE_PATH -storetype JCEKS -keystore $TRUSTSTORE_PATH

echo ">>>passw: changeit"
keytool -importcert -alias $USER1_CERT_ALIAS -file $USER1_CERTIFICATE_PATH -storetype JCEKS -keystore $TRUSTSTORE_PATH

echo ">>>passw: changeit"
keytool -importcert -alias $USER2_CERT_ALIAS -file $USER2_CERTIFICATE_PATH -storetype JCEKS -keystore $TRUSTSTORE_PATH

echo ">>>passw: changeit"
keytool -importcert -alias $USER3_CERT_ALIAS -file $USER3_CERTIFICATE_PATH -storetype JCEKS -keystore $TRUSTSTORE_PATH

echo ">>>passw: changeit"
keytool -importcert -alias $USER4_CERT_ALIAS -file $USER4_CERTIFICATE_PATH -storetype JCEKS -keystore $TRUSTSTORE_PATH
