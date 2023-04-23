#!/bin/bash

# in case you are having issues with running this script, run the command below first
# $ chmod +x script.sh

## CREATE KEYSTORES

# Specify the location and name of the keystore file
KEYSTORE_DIR="/path/to/keystore/directory"
KEYSTORE_FILE="keystore.jks"

# Concatenate the path and filename to create the full path to the truststore file
KEYSTORE_PATH="$KEYSTORE_DIR/$KEYSTORE_FILE"

# Specify the alias and password for the key pair
KEY_ALIAS="mykey"
KEY_PASSWORD="mypassword"

# Generate a new RSA key pair with a 2048-bit key size
keytool -genkeypair -alias $KEY_ALIAS -keyalg RSA -keysize 2048 -keystore $KEYSTORE_PATH -storepass $KEY_PASSWORD -keypass $KEY_PASSWORD

################################################################################################################################################################

## CREATE TRUSTSTORES

# Specify the path to the directory where the truststore file will be stored
TRUSTSTORE_DIR="/path/to/truststore/directory"

# Specify the filename for the truststore
TRUSTSTORE_FILE="mytruststore.jks"

# Specify the alias and password for the trusted certificate
CERT_ALIAS="mycert"
CERT_PASSWORD="mypassword"

# Specify the path to the trusted certificate file
CERT_FILE="/path/to/trusted/certificate.crt"

# Concatenate the path and filename to create the full path to the truststore file
TRUSTSTORE_PATH="$TRUSTSTORE_DIR/$TRUSTSTORE_FILE"

# Create an empty truststore with a default password of 'changeit'
keytool -genkey -alias temp -keystore $TRUSTSTORE_PATH -storepass changeit -keypass changeit -noprompt

# Import the trusted certificate into the truststore
keytool -importcert -alias $CERT_ALIAS -file $CERT_FILE -keystore $TRUSTSTORE_PATH -storepass $CERT_PASSWORD -noprompt

# Set the file permissions to read-only
# chmod 400 $TRUSTSTORE_PATH

################################################################################################################################################################

CERTIFICATE_PATH="path/to/certificate.crt"

# EXPORT A CERTIFICATE WITH ALIAS "mycertificate" TO A FILE NAMED "mycertificate.crt" FROM A KEYSTORE "mykeystore.jks" WITH PASSWORD "mypassword"

keytool -exportcert -alias mycertificate -keystore mykeystore.jks -storepass mypassword -rfc -file $CERTIFICATE_PATH
