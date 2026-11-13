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

PROJECT_GPG_KEY_ID=$(aws secretsmanager get-secret-value --secret-id gpg-signing-key-id | jq -r .SecretString)
PROJECT_GPG_PASSPHRASE=$(aws secretsmanager get-secret-value --secret-id gpg-signing-key-passphrase | jq -r .SecretString)
PROJECT_GPG_PRIVATE_KEY=$(aws secretsmanager get-secret-value --secret-id gpg-signing-key-private-key | jq -r .SecretString)

TEMPDIR=$(mktemp -d)

gpg --homedir $TEMPDIR --batch --passphrase $PROJECT_GPG_PASSPHRASE --import -q <<EOF
$PROJECT_GPG_PRIVATE_KEY
EOF

echo "Key information:"
gpg --homedir $TEMPDIR --list-keys $PROJECT_GPG_KEY_ID | grep "expir"

rm -rf $TEMPDIR
