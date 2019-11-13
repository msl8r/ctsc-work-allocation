variable "product" {
  type    = "string"
  default = "ctsc-work-allocation"
}

variable "component" {
  type = "string"
}

variable "location_app" {
  type    = "string"
  default = "UK South"
}

variable "env" {
  type = "string"
}

variable "ilbIp" {}

variable "subscription" {}

variable "capacity" {
  default = "1"
}

variable "common_tags" {
  type = "map"
}

variable "idam_s2s_url_prefix" {
  default = "rpe-service-auth-provider"
}

variable "ctsc_server_url_prefix" {
  default = "ctsc-work-allocation"
}

variable "ccd_api_url_prefix" {
  default = "ccd-data-store-api"
}

variable "idam_api_url" {
  default = "https://idam-api.aat.platform.hmcts.net"
}

variable "idam_client_id" {
  default = "ctsc_work_allocation"
}

variable "database_name" {
  type    = "string"
  default = "workallocation"
}

variable "postgresql_user" {
  type    = "string"
  default = "workallocation"
}

variable "liquibase_enabled" {
  default = "false"
}

variable "deeplink_base_url" {
  default = "https://www-ccd.demo.platform.hmcts.net/case/"
}

variable "service_bus_queue_name" {
  default = "ctsc-work-allocation-queue-demo"
}

variable "s2s_microservice_name" {
  default = "ctsc_work_allocation"
}

variable "smtp_enabled" {
  default = "false"
}

variable "minus_time_from_current" {
  default = "0"
}

variable "test_enpooints_enabled" {
  default = "false"
}

variable "smtp_host" {
  default = "smtp.office365.com"
}

variable "smtp_port" {
  default = "587"
}

variable "poll_interval_minutes" {
  default = "30"
}

variable "poll_cron" {
  default = "0 */30 * * * *"
}

variable "last_modified_minus_minutes" {
  default = "0"
}

variable "ccd_dry_run" {
  default = "true"
}
