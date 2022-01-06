provider "azurerm" {
  features {}
}

locals {
  app_full_name = "${var.product}-${var.component}"
  tags = (merge(
  var.common_tags,
  map(
  "Team Contact", var.team_contact,
  "Destroy Me", var.destroy_me
  )
  ))
}

resource "azurerm_resource_group" "rg" {
  name     = "${local.app_full_name}-${var.env}"
  location = var.location
  tags     = var.common_tags
}

module "key-vault" {
  source                     = "git@github.com:hmcts/cnp-module-key-vault?ref=master"
  product                    = local.app_full_name
  env                        = var.env
  tenant_id                  = var.tenant_id
  object_id                  = var.jenkins_AAD_objectId
  resource_group_name        = azurerm_resource_group.rg.name
  product_group_object_id    = "5d9cd025-a293-4b97-a0e5-6f43efce02c0"
  common_tags                = var.common_tags
  managed_identity_object_id = data.azurerm_user_assigned_identity.em-shared-identity.principal_id
}

data "azurerm_user_assigned_identity" "em-shared-identity" {
  name                = "rpa-${var.env}-mi"
  resource_group_name = "managed-identities-${var.env}-rg"
}

module "db" {
  source = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product = var.product
  component = var.component
  name = join("-", [
    var.product,
    var.component,
    "postgres-v11-db"])
  postgresql_version = 11
  location = var.location
  env = var.env
  postgresql_user = var.postgresql_user
  database_name = var.database_name
  common_tags = var.common_tags
  subscription = var.subscription
}

resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name         = "${var.component}-POSTGRES-USER"
  value        = module.db.user_name
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name         = "${var.component}-POSTGRES-PASS"
  value        = module.db.postgresql_password
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name         = "${var.component}-POSTGRES-HOST"
  value        = module.db.host_name
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  name         = "${var.component}-POSTGRES-PORT"
  value        = module.db.postgresql_listen_port
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  name         = "${var.component}-POSTGRES-DATABASE"
  value        = module.db.postgresql_database
  key_vault_id = module.key-vault.key_vault_id
}

module "storage_account" {
  source                    = "git@github.com:hmcts/cnp-module-storage-account?ref=master"
  env                       = var.env
  storage_account_name      = "emhrsapi${var.env}"
  resource_group_name       = azurerm_resource_group.rg.name
  location                  = var.location
  account_kind              = "StorageV2"
  account_tier              = "Standard"
  account_replication_type  = "ZRS"
  access_tier               = "Hot"

  enable_https_traffic_only = true

  default_action = "Allow"

  // Tags
  common_tags  = local.tags
  team_contact = var.team_contact
  destroy_me   = var.destroy_me
}

resource "azurerm_key_vault_secret" "storage_account_id" {
  name         = "storage-account-id"
  value        = module.storage_account.storageaccount_id
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "storage_account_primary_access_key" {
  name         = "storage-account-primary-access-key"
  value        = module.storage_account.storageaccount_primary_access_key
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "storage_account_secondary_access_key" {
  name         = "storage-account-secondary-access-key"
  value        = module.storage_account.storageaccount_secondary_access_key
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "storage_account_primary_connection_string" {
  name         = "storage-account-primary-connection-string"
  value = module.storage_account.storageaccount_primary_connection_string
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "storage_account_secondary_connection_string" {
  name = "storage-account-secondary-connection-string"
  value = module.storage_account.storageaccount_secondary_connection_string
  key_vault_id = module.key-vault.key_vault_id
}


module "cvp_storage_account_simulator" {
  count = var.env == "aat" ? 1 : 0

  source = "git@github.com:hmcts/cnp-module-storage-account?ref=master"
  env = var.env
  storage_account_name = "emhrscvp${var.env}"
  resource_group_name = azurerm_resource_group.rg.name
  location = var.location
  account_kind = "StorageV2"
  account_tier = "Standard"
  account_replication_type = "LRS"
  access_tier = "Hot"

  enable_https_traffic_only = true

  default_action = "Allow"

  // Tags
  common_tags = local.tags
  team_contact = var.team_contact
  destroy_me = var.destroy_me
}

//disabled as tf complains about unsupport attribute, when querying module output, despite it being in same
//format as the primary blob. have manually created it in the keyvault for now.
//TODO resolve this
//resource "azurerm_key_vault_secret" "cvp_storage_simulator_connection_string" {
//  count = var.env == "aat" ? 1 : 0
//  name = "cvp-storage-simulator-connection-string"
//  value = module.cvp_storage_account_simulator.storageaccount_primary_connection_string
//  key_vault_id = module.key-vault.key_vault_id
//}


data "azurerm_key_vault" "s2s_vault" {
  name = "s2s-${var.env}"
  resource_group_name = "rpe-service-auth-provider-${var.env}"
}

data "azurerm_key_vault_secret" "s2s_key" {
  name = "microservicekey-em-hrs-api"
  key_vault_id = data.azurerm_key_vault.s2s_vault.id
}

resource "azurerm_key_vault_secret" "local_s2s_key" {
  name         = "microservicekey-em-hrs-api"
  value        = data.azurerm_key_vault_secret.s2s_key.value
  key_vault_id = module.key-vault.key_vault_id
}
