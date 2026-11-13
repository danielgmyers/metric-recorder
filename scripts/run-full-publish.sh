#!/bin/bash
set -e

if ! command -v aws &> /dev/null
then
  echo "Please install the AWS CLI."
  exit 1
fi

if ! command -v jq &> /dev/null
then
  echo "Please install jq."
  exit 2
fi

if ! command -v gpg &> /dev/null
then
  echo "Please install gpg."
  exit 3
fi

# you'll also need the AWS CLI to be configured with credentials for our AWS account.

AWS_DEFAULT_REGION=us-west-2

GPG_KEY_ID=$(aws secretsmanager get-secret-value --secret-id gpg-signing-key-id | jq -r .SecretString)
GPG_PASSPHRASE=$(aws secretsmanager get-secret-value --secret-id gpg-signing-key-passphrase | jq -r .SecretString)
GPG_PRIVATE_KEY=$(aws secretsmanager get-secret-value --secret-id gpg-signing-key-private-key | jq -r .SecretString)

# this gets us the raw json block that has username and password inside
MAVEN_CREDENTIALS=$(aws secretsmanager get-secret-value --secret-id MavenCentral | jq -r .SecretString)

MAVEN_CENTRAL_USERNAME=$(echo $MAVEN_CREDENTIALS | jq -r .username)
MAVEN_CENTRAL_PASSWORD=$(echo $MAVEN_CREDENTIALS | jq -r .password)

mkdir -p ~/.m2/
echo "<settings><servers><server><id>central</id>" > ~/.m2/settings.xml
echo "<username>${MAVEN_CENTRAL_USERNAME}</username><password>${MAVEN_CENTRAL_PASSWORD}</password>" >> ~/.m2/settings.xml
echo "</server></servers></settings>" >> ~/.m2/settings.xml

mvn -Dpublishing.publishAfterStaging=true deploy -P release
