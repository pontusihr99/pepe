# Terraform deployment

This folder provisions the Cloud Run deployment for the PEPE trading bot.

## What it creates
- Enables Cloud Run, Artifact Registry, and Secret Manager APIs
- Creates an Artifact Registry Docker repository
- Creates the Kraken secrets in Secret Manager
- Creates a dedicated Cloud Run service account
- Deploys the Cloud Run v2 service

## Before applying
1. Build and push the container image.
2. Create secret versions in Secret Manager for:
   - `kraken-api-key`
   - `kraken-secret-key`
3. Copy `terraform.tfvars.example` to `terraform.tfvars` and fill in your values.

## Commands
```bash
terraform init
terraform plan
terraform apply
```
