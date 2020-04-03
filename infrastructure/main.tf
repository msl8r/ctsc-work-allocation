provider "azurerm" {
  version = "1.44.0"
}

locals {
  ase_name               = "${data.terraform_remote_state.core_apps_compute.ase_name[0]}"
  vaultName              = "${var.product}-${var.env}"
  asp_name               = "${var.product}-${var.env}"
}

data "azurerm_key_vault" "workallocation_key_vault" {
  name = "${local.vaultName}"
  resource_group_name = "${var.product}-${var.env}"
}

data "azurerm_key_vault" "s2s_key_vault" {
  name = "s2s-${var.env}"
  resource_group_name = "rpe-service-auth-provider-${var.env}"
}

resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  key_vault_id = "${data.azurerm_key_vault.workallocation_key_vault.id}"
  name         = "${var.component}-POSTGRES-USER"
  value        = "${module.bar-database.user_name}"
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  key_vault_id = "${data.azurerm_key_vault.workallocation_key_vault.id}"
  name         = "${var.component}-POSTGRES-PASS"
  value        = "${module.bar-database.postgresql_password}"
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  key_vault_id = "${data.azurerm_key_vault.workallocation_key_vault.id}"
  name         = "${var.component}-POSTGRES-HOST"
  value        = "${module.bar-database.host_name}"
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  key_vault_id = "${data.azurerm_key_vault.workallocation_key_vault.id}"
  name         = "${var.component}-POSTGRES-PORT"
  value        = "5432"
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  key_vault_id = "${data.azurerm_key_vault.workallocation_key_vault.id}"
  name         = "${var.component}-POSTGRES-DATABASE"
  value        = "${module.bar-database.postgresql_database}"
}

resource "azurerm_key_vault_secret" "SERVICE-BUS-PRIMARY-CONNECTION-STRING" {
  key_vault_id = "${data.azurerm_key_vault.workallocation_key_vault.id}"
  name         = "CTSC-SERVICEBUS-CONNECTION-STRING"
  value        = "${module.servicebus-namespace.primary_send_and_listen_connection_string}"
}

# Copy orchestrator s2s secret from s2s key vault to bulkscan key vault
resource "azurerm_key_vault_secret" "bulk_scan_orchestrator_app_s2s_secret" {
  key_vault_id = "${data.azurerm_key_vault.workallocation_key_vault.id}"
  name         = "CTSC-S2S-SECRET"
  value        = "${data.azurerm_key_vault_secret.s2s_secret.value}"
}

data "azurerm_key_vault_secret" "s2s_secret" {
  name = "microservicekey-ctsc-work-allocation"
  key_vault_id = "${data.azurerm_key_vault.s2s_key_vault.id}"
}

data "azurerm_key_vault_secret" "service_user_password" {
  name      = "CTSC-SERVICE-USER-PASSWORD"
  key_vault_id = "${data.azurerm_key_vault.workallocation_key_vault.id}"
}

data "azurerm_key_vault_secret" "service_user_email" {
  name      = "CTSC-SERVICE-USER-EMAIL"
  key_vault_id = "${data.azurerm_key_vault.workallocation_key_vault.id}"
}

data "azurerm_key_vault_secret" "service_email_address" {
  name      = "wa-service-email-address"
  key_vault_id = "${data.azurerm_key_vault.workallocation_key_vault.id}"
}

data "azurerm_key_vault_secret" "idam_client_secret" {
  name      = "CTSC-IDAM-CLIENT-SECRET"
  key_vault_id = "${data.azurerm_key_vault.workallocation_key_vault.id}"
}

data "azurerm_key_vault_secret" "applicationinsights_instrumentationkey" {
  name      = "AppInsightsInstrumentationKey"
  key_vault_id = "${data.azurerm_key_vault.workallocation_key_vault.id}"
}

data "azurerm_key_vault_secret" "ctsc_wa_smtp_user" {
  name      = "CTSC-WA-SMTP-USER"
  key_vault_id = "${data.azurerm_key_vault.workallocation_key_vault.id}"
}

data "azurerm_key_vault_secret" "ctsc_wa_smtp_password" {
  name      = "CTSC-WA-SMTP-PASSWORD"
  key_vault_id = "${data.azurerm_key_vault.workallocation_key_vault.id}"
}

# Make sure the resource group exists
resource "azurerm_resource_group" "rg" {
  name     = "${local.asp_name}"
  location = "${var.location_app}"
}

module "servicebus-namespace" {
  source                = "git@github.com:hmcts/terraform-module-servicebus-namespace?ref=master"
  name                  = "${var.product}-servicebus-${var.env}"
  location              = "${var.location_app}"
  resource_group_name   = "${azurerm_resource_group.rg.name}"
  common_tags           = "${var.common_tags}"
  env                   = "${var.env}"
}

module "work-allocation-queue" {
  source = "git@github.com:hmcts/terraform-module-servicebus-queue?ref=master"
  name = "${var.product}-work-allocation-queue-${var.env}"
  namespace_name = "${module.servicebus-namespace.name}"
  resource_group_name = "${azurerm_resource_group.rg.name}"
}

module "bar-database" {
  source = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product = "${var.product}-postgres-db"
  location = "${var.location_app}"
  env = "${var.env}"
  postgresql_user = "${var.postgresql_user}"
  database_name = "${var.database_name}"
  sku_name = "GP_Gen5_2"
  sku_tier = "GeneralPurpose"
  common_tags     = "${var.common_tags}"
  subscription = "${var.subscription}"
}

