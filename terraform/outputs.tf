output "cloud_run_url" {
  value       = google_cloud_run_v2_service.app.uri
  description = "Cloud Run service URL"
}

output "artifact_registry_repository" {
  value       = google_artifact_registry_repository.app.repository_id
  description = "Artifact Registry repository id"
}
