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

variable "tenant_id" {}

variable "jenkins_AAD_objectId" {
  type                        = "string"
  description                 = "(Required) The Azure AD object ID of a user, service principal or security group in the Azure Active Directory tenant for the vault. The object ID must be unique for the list of access policies."
}

variable "product_group_object_id" {
  description = "Product group (dcd_group_ctsc_v2) object ID"
  default     = "075cc55b-c1d0-4fb9-a658-e5e0de0e13b1"
}

