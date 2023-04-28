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

# Specify the keystore file name
USER2_KEYSTORE_FILE="user2_keyStore.jks"

# Specify the alias and password for the key pair
USER2_KEYSTORE_PASSWORD="user2_keyStore_passw"
USER2_KEY_ALIAS="user2_key_alias"

# Concatenate the path and filename to create the full path to the keystore file
USER2_KEYSTORE_PATH="$KEYSTORE_DIR/$USER2_KEYSTORE_FILE"

################################################################################################################################################################

# Concatenate the path and filename to create the full path to the certificate file
SERVER_CERTIFICATE_FILE="server_certificate.cer"
SERVER_CERTIFICATE_PATH="$CERTIFICATE_DIR/$SERVER_CERTIFICATE_FILE"

USER1_CERTIFICATE_FILE="user1_certificate.cer"
USER1_CERTIFICATE_PATH="$CERTIFICATE_DIR/$USER1_CERTIFICATE_FILE"

USER2_CERTIFICATE_FILE="user2_certificate.cer"
USER2_CERTIFICATE_PATH="$CERTIFICATE_DIR/$USER2_CERTIFICATE_FILE"

################################################################################################################################################################

echo "criar keySTores para server e clientes"

# Generate a new RSA key pair with a 2048-bit key size
keytool -genkeypair -alias $SERVER_KEY_ALIAS -keyalg RSA -storetype JCEKS -keysize 2048 -keystore $SERVER_KEYSTORE_PATH -storepass $SERVER_KEYSTORE_PASSWORD -keypass $SERVER_KEYSTORE_PASSWORD

keytool -genkeypair -alias $USER1_KEY_ALIAS -keyalg RSA -storetype JCEKS -keysize 2048 -keystore $USER1_KEYSTORE_PATH -storepass $USER1_KEYSTORE_PASSWORD -keypass $USER1_KEYSTORE_PASSWORD

keytool -genkeypair -alias $USER2_KEY_ALIAS -keyalg RSA -storetype JCEKS -keysize 2048 -keystore $USER2_KEYSTORE_PATH -storepass $USER2_KEYSTORE_PASSWORD -keypass $USER2_KEYSTORE_PASSWORD

# Export certificates

echo "Exporting certificates"

echo ">>>passw: server_keyStore_passw"
keytool -exportcert -alias $SERVER_KEY_ALIAS -storetype JCEKS -keystore $SERVER_KEYSTORE_PATH -file $SERVER_CERTIFICATE_PATH

echo ">>>passw: user1_keyStore_passw"
keytool -exportcert -alias $USER1_KEY_ALIAS -storetype JCEKS -keystore $USER1_KEYSTORE_PATH -file $USER1_CERTIFICATE_PATH

echo ">>>passw: user2_keyStore_passw"
keytool -exportcert -alias $USER2_KEY_ALIAS -storetype JCEKS -keystore $USER2_KEYSTORE_PATH -file $USER2_CERTIFICATE_PATH

################################################################################################################################################################

## CREATE TRUSTSTORE

# Specify the filename and password for the truststore
TRUSTSTORE_FILE="tintolmarket_trustStore.jks"
TRUSTSTORE_PASSWORD="changeit"

# Specify the alias for the server certificate
SERVER_CERT_ALIAS="server_cert_alias"
USER1_CERT_ALIAS="user1_cert_alias"
USER2_CERT_ALIAS="user2_cert_alias"

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
