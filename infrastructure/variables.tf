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

variable "last_run_log_file" {
  default = "%HOME%\\LogFiles\\last_run.log"
}

