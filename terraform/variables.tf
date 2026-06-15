variable "project_id" {
  type        = string
  description = "project-ed5433c8-e785-475c-a8d"
}

variable "region" {
  type        = string
  description = "Google Cloud region"
  default     = "europe-west1"
}

variable "service_name" {
  type        = string
  description = "Cloud Run service name"
  default     = "pepe-trading-bot"
}

variable "image" {
  type        = string
  description = "Full container image path"
}

variable "artifact_registry_location" {
  type        = string
  description = "Artifact Registry location"
  default     = "europe-west1"
}

variable "artifact_registry_repository_id" {
  type        = string
  description = "Artifact Registry repository id"
  default     = "pepe-trading-bot"
}

variable "service_account_id" {
  type        = string
  description = "Cloud Run service account id"
  default     = "pepe-trading-bot-runner"
}

variable "kraken_api_key_secret_id" {
  type        = string
  description = "Secret Manager secret id for the Kraken API key"
}

variable "kraken_secret_key_secret_id" {
  type        = string
  description = "Secret Manager secret id for the Kraken secret key"
}
