#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_dir="$(cd "${script_dir}/.." && pwd)"
key_dir="${project_dir}/.local/jwt"
environment_file="${project_dir}/.env.jwt.local"

if [[ -e "${environment_file}" || -e "${key_dir}/private.pem" ]]; then
	echo "Local JWT key files already exist. Remove them explicitly before generating a new pair." >&2
	exit 1
fi

umask 077
mkdir -p "${key_dir}"

openssl genpkey \
	-algorithm RSA \
	-pkeyopt rsa_keygen_bits:2048 \
	-out "${key_dir}/private.pem" \
	2>/dev/null
openssl pkey \
	-in "${key_dir}/private.pem" \
	-pubout \
	-out "${key_dir}/public.pem" \
	2>/dev/null

public_key_base64="$(openssl pkey \
	-pubin \
	-in "${key_dir}/public.pem" \
	-outform DER \
	2>/dev/null | openssl base64 -A)"
private_key_base64="$(openssl pkcs8 \
	-topk8 \
	-nocrypt \
	-in "${key_dir}/private.pem" \
	-outform DER \
	2>/dev/null | openssl base64 -A)"

{
	printf 'JWT_PUBLIC_KEY_BASE64=%s\n' "${public_key_base64}"
	printf 'JWT_PRIVATE_KEY_BASE64=%s\n' "${private_key_base64}"
	printf 'JWT_ISSUER=https://mydata-card-recommendation.local\n'
	printf 'JWT_AUDIENCE=mydata-card-recommendation-clients\n'
	printf 'JWT_ACCESS_TOKEN_TTL=PT15M\n'
	printf 'JWT_KEY_ID=local-primary\n'
} > "${environment_file}"

echo "Created local-only JWT keys and .env.jwt.local. These files are ignored by Git."
