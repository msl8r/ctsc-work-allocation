provider "azurerm" {}

locals {
  ase_name               = "${data.terraform_remote_state.core_apps_compute.ase_name[0]}"
  vaultName              = "${var.product}-${var.env}"
}

data "azurerm_key_vault" "workallocation_key_vault" {
  name = "${local.vaultName}"
  resource_group_name = "${var.product}-${var.env}"
}

data "azurerm_key_vault_secret" "s2s_secret" {
  name      = "CTSC-S2S-SECRET"
  vault_uri = "${data.azurerm_key_vault.workallocation_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "service_user_password" {
  name      = "CTSC-SERVICE-USER-PASSWORD"
  vault_uri = "${data.azurerm_key_vault.workallocation_key_vault.vault_uri}"
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

  app_settings = {
    LOGBACK_REQUIRE_ALERT_LEVEL = "false"
    LOGBACK_REQUIRE_ERROR_CODE  = "false"
    S2S_SECRET = "${data.azurerm_key_vault_secret.s2s_secret.value}"
    S2S_AUTH_URL = "http://${var.idam_s2s_url_prefix}-${var.env}.service.${local.ase_name}.internal"
    SERVICE_USER_EMAIL = "${var.service_user_email}"
    SERVICE_USER_PASSWORD = "${data.azurerm_key_vault_secret.service_user_password.value}"
  }
}

