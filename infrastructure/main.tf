provider "azurerm" {}

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

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  key_vault_id = "${data.azurerm_key_vault.workallocation_key_vault.id}"
  name         = "workallocation-POSTGRES-PASS"
  value        = "${module.bar-database.postgresql_password}"
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

module "ctsc-work-allocation" {

  source              = "git@github.com:hmcts/cnp-module-webapp?ref=master"
  product             = "${var.product}-${var.component}"
  location            = "${var.location_app}"
  env                 = "${var.env}"
  ilbIp               = "${var.ilbIp}"
  subscription        = "${var.subscription}"
  capacity            = "${var.capacity}"
  common_tags         = "${var.common_tags}"
  asp_name            = "${local.asp_name}"
  asp_rg              = "${local.asp_name}"

  app_settings = {
    # db
    SPRING_DATASOURCE_USERNAME = "${module.bar-database.user_name}"
    SPRING_DATASOURCE_PASSWORD = "${module.bar-database.postgresql_password}"
    SPRING_DATASOURCE_URL      = "jdbc:postgresql://${module.bar-database.host_name}:${module.bar-database.postgresql_listen_port}/${module.bar-database.postgresql_database}?sslmode=require"
    SPRING_LIQUIBASE_ENABLED   = "${var.liquibase_enabled}"

    LOGBACK_REQUIRE_ALERT_LEVEL = "false"
    LOGBACK_REQUIRE_ERROR_CODE  = "false"
    S2S_SECRET = "${data.azurerm_key_vault_secret.s2s_secret.value}"
    S2S_MICROSERVICE_NAME = "${var.s2s_microservice_name}"
    S2S_AUTH_URL = "http://${var.idam_s2s_url_prefix}-${var.env}.service.${local.ase_name}.internal"
    SERVER_URL = "https://${var.ctsc_server_url_prefix}-${var.env}-staging.service.${local.ase_name}.internal"
    CCD_API_URL = "http://${var.ccd_api_url_prefix}-${var.env}.service.${local.ase_name}.internal"
    IDAM_CLIENT_ID = "${var.idam_client_id}"
    IDAM_CLIENT_SECRET = "${data.azurerm_key_vault_secret.idam_client_secret.value}"
    IDAM_CLIENT_BASE_URL = "${var.idam_api_url}"
    SERVICE_USER_EMAIL = "${data.azurerm_key_vault_secret.service_user_email.value}"
    SERVICE_USER_PASSWORD = "${data.azurerm_key_vault_secret.service_user_password.value}"
    SERVICE_BUS_CONNECTION_STRING = "${module.servicebus-namespace.primary_send_and_listen_connection_string}"
    SERVICE_BUS_QUEUE_NAME = "${var.service_bus_queue_name}"

    #SMTP
    SMTP_ENABLED = "${var.smtp_enabled}"
    SMTP_HOST = "${var.smtp_host}"
    SMTP_PORT = "${var.smtp_port}"
    SMTP_USER = "${data.azurerm_key_vault_secret.ctsc_wa_smtp_user.value}"
    SMTP_PASSWORD = "${data.azurerm_key_vault_secret.ctsc_wa_smtp_password.value}"
    SERVICE_EMAIL_ADDRESS = "${data.azurerm_key_vault_secret.service_email_address.value}"

    DEEPLINK_BASE_URL = "${var.deeplink_base_url}"
    MINUS_TIME_FROM_CURRENT = "${var.minus_time_from_current}"
    AZURE_APPLICATIONINSIGHTS_INSTRUMENTATIONKEY = "${data.azurerm_key_vault_secret.applicationinsights_instrumentationkey.value}"
    TEST_ENDPOINTS_ENABLED = "${var.test_enpooints_enabled}"
    POLL_INTERVAL = "${var.poll_interval_minutes}"
    POLL_CRON = "${var.poll_cron}"
    LAST_MODIFIED_MINUS_MINUTES = "${var.last_modified_minus_minutes}"
    CCD_DRY_RUN = "${var.ccd_dry_run}"
  }
}

