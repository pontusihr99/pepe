terraform {
  required_version = ">= 1.5.0"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

locals {
  services = [
    "run.googleapis.com",
    "artifactregistry.googleapis.com",
    "secretmanager.googleapis.com"
  ]
}

resource "google_project_service" "enabled" {
  for_each = toset(local.services)

  project            = var.project_id
  service            = each.value
  disable_on_destroy = false
}

resource "google_artifact_registry_repository" "app" {
  project       = var.project_id
  location      = var.artifact_registry_location
  repository_id = var.artifact_registry_repository_id
  description   = "Container repository for ${var.service_name}"
  format        = "DOCKER"

  depends_on = [google_project_service.enabled]
}

resource "google_secret_manager_secret" "kraken_api_key" {
  project   = var.project_id
  secret_id = var.kraken_api_key_secret_id

  replication {
    auto {}
  }

  depends_on = [google_project_service.enabled]
}

resource "google_secret_manager_secret" "kraken_secret_key" {
  project   = var.project_id
  secret_id = var.kraken_secret_key_secret_id

  replication {
    auto {}
  }

  depends_on = [google_project_service.enabled]
}

resource "google_service_account" "cloud_run" {
  project      = var.project_id
  account_id   = var.service_account_id
  display_name = "Cloud Run service account for ${var.service_name}"
}

resource "google_secret_manager_secret_iam_member" "kraken_api_key_access" {
  project   = var.project_id
  secret_id = google_secret_manager_secret.kraken_api_key.secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.cloud_run.email}"
}

resource "google_secret_manager_secret_iam_member" "kraken_secret_key_access" {
  project   = var.project_id
  secret_id = google_secret_manager_secret.kraken_secret_key.secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.cloud_run.email}"
}

resource "google_cloud_run_v2_service" "app" {
  project  = var.project_id
  name     = var.service_name
  location = var.region

  ingress = "INGRESS_TRAFFIC_ALL"

  template {
    service_account = google_service_account.cloud_run.email

    scaling {
      min_instance_count = 0
      max_instance_count = 1
    }

    containers {
      image = var.image

      resources {
        limits = {
          cpu    = "1"
          memory = "512Mi"
        }
      }

      env {
        name = "KRAKEN_API_KEY"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.kraken_api_key.secret_id
            version = "latest"
          }
        }
      }

      env {
        name = "KRAKEN_SECRET_KEY"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.kraken_secret_key.secret_id
            version = "latest"
          }
        }
      }
    }
  }

  depends_on = [
    google_project_service.enabled,
    google_secret_manager_secret_iam_member.kraken_api_key_access,
    google_secret_manager_secret_iam_member.kraken_secret_key_access,
  ]
}

resource "google_cloud_run_v2_service_iam_member" "public_invoker" {
  project  = var.project_id
  location = var.region
  name     = google_cloud_run_v2_service.app.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}
