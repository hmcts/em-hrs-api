provider "azurerm" {
  features {
  }
}

provider "azurerm" {
  features {}
  skip_provider_registration = true
  alias                      = "cft_vnet"
  subscription_id            = var.aks_subscription_id
}

provider "azurerm" {
  features {}
  skip_provider_registration = true
  alias                      = "vh_vnet"
  subscription_id            = var.vh_subscription_id
}

provider "azurerm" {
  features {}
  skip_provider_registration = true
  alias                      = "cvp_vnet"
  subscription_id            = var.cvp_subscription_id
}

locals {
  app_full_name              = "${var.product}-${var.component}"
  tags                       = var.common_tags
  db_name                    = "${local.app_full_name}-postgres-db-v15"
  private_endpoint_rg_name   = var.businessArea == "sds" ? "ss-${var.env}-network-rg" : "${var.businessArea}-${var.env}-network-rg"
  private_endpoint_vnet_name = var.businessArea == "sds" ? "ss-${var.env}-vnet" : "${var.businessArea}-${var.env}-vnet"
  private_dns_zone_ids       = ["/subscriptions/1baf5470-1c3e-40d3-a6f7-74bfbce4b348/resourceGroups/core-infra-intsvc-rg/providers/Microsoft.Network/privateDnsZones/privatelink.blob.core.windows.net"]
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

resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name         = "${var.component}-POSTGRES-USER"
  value        = module.db-v15.username
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name         = "${var.component}-POSTGRES-PASS"
  value        = module.db-v15.password
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name         = "${var.component}-POSTGRES-HOST"
  value        = module.db-v15.fqdn
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  name         = "${var.component}-POSTGRES-PORT"
  value        = "5432"
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  name         = "${var.component}-POSTGRES-DATABASE"
  value        = "emhrs"
  key_vault_id = module.key-vault.key_vault_id
}

data "azurerm_subnet" "private_endpoints" {
  provider = azurerm.cft_vnet

  resource_group_name  = local.private_endpoint_rg_name
  virtual_network_name = local.private_endpoint_vnet_name
  name                 = "private-endpoints"
}

module "storage_account" {
  source                   = "git@github.com:hmcts/cnp-module-storage-account?ref=fix/private-endpoint-provider-4.x"
  env                      = var.env
  storage_account_name     = "emhrsapi${var.env}"
  resource_group_name      = azurerm_resource_group.rg.name
  location                 = var.location
  account_kind             = "StorageV2"
  account_tier             = "Standard"
  account_replication_type = "ZRS"
  access_tier              = "Hot"

  enable_https_traffic_only = true

  enable_data_protection = true
  enable_change_feed     = true

  default_action                = "Allow"
  public_network_access_enabled = true

  private_endpoint_subscription_id = var.aks_subscription_id
  private_endpoint_subnet_id       = data.azurerm_subnet.private_endpoints.id
  private_endpoint_rg_name         = local.private_endpoint_rg_name


  // Tags
  common_tags  = local.tags
  team_contact = var.team_contact
  destroy_me   = var.destroy_me
}

resource "azurerm_storage_container" "vh_container" {
  name                  = "vhrecordings"
  storage_account_name  = module.storage_account.storageaccount_name
  container_access_type = "private"
}

resource "azurerm_storage_container" "jurisdiction_codes" {
  name                  = "jurisdictioncodes"
  storage_account_name  = module.storage_account.storageaccount_name
  container_access_type = "private"
}

// test container for CVP
resource "azurerm_storage_container" "cvpsimulator" {
  count                 = var.env != "prod" ? 1 : 0
  name                  = "cvpsimulator"
  storage_account_name  = module.storage_account.storageaccount_name
  container_access_type = "private"
}
// test container for VH
resource "azurerm_storage_container" "vhsimulator" {
  count                 = var.env != "prod" ? 1 : 0
  name                  = "vhsimulator"
  storage_account_name  = module.storage_account.storageaccount_name
  container_access_type = "private"
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
  value        = module.storage_account.storageaccount_primary_connection_string
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "storage_account_secondary_connection_string" {
  name         = "storage-account-secondary-connection-string"
  value        = module.storage_account.storageaccount_secondary_connection_string
  key_vault_id = module.key-vault.key_vault_id
}


module "cvp_storage_account_simulator" {
  count = var.env == "aat" ? 1 : 0

  source                   = "git@github.com:hmcts/cnp-module-storage-account?ref=4.x"
  env                      = var.env
  storage_account_name     = "emhrscvp${var.env}"
  resource_group_name      = azurerm_resource_group.rg.name
  location                 = var.location
  account_kind             = "StorageV2"
  account_tier             = "Standard"
  account_replication_type = "LRS"
  access_tier              = "Hot"

  enable_https_traffic_only = true

  default_action         = "Allow"
  enable_data_protection = false
  // Tags
  common_tags  = local.tags
  team_contact = var.team_contact
  destroy_me   = var.destroy_me
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
  name                = "s2s-${var.env}"
  resource_group_name = "rpe-service-auth-provider-${var.env}"
}

data "azurerm_key_vault_secret" "s2s_key" {
  name         = "microservicekey-em-hrs-api"
  key_vault_id = data.azurerm_key_vault.s2s_vault.id
}

resource "azurerm_key_vault_secret" "local_s2s_key" {
  name         = "microservicekey-em-hrs-api"
  value        = data.azurerm_key_vault_secret.s2s_key.value
  key_vault_id = module.key-vault.key_vault_id
}

# FlexibleServer v15
module "db-v15" {
  providers = {
    azurerm.postgres_network = azurerm.cft_vnet
  }
  source                     = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  env                        = var.env
  product                    = var.product
  component                  = var.component
  common_tags                = var.common_tags
  name                       = local.db_name
  pgsql_version              = "15"
  admin_user_object_id       = var.jenkins_AAD_objectId
  business_area              = "CFT"
  action_group_name          = join("-", [local.db_name, var.action_group_name, var.env])
  email_address_key          = var.email_address_key
  email_address_key_vault_id = module.key-vault.key_vault_id
  # The original subnet is full, this is required to use the new subnet for new databases
  subnet_suffix = "expanded"
  pgsql_databases = [
    {
      name : "emhrs"
    }
  ]
  pgsql_server_configuration = [
    {
      name  = "azure.extensions"
      value = "pg_stat_statements,pg_buffercache,uuid-ossp"
    }
  ]
  //Below attributes needs to be overridden for Perftest & Prod
  pgsql_sku                      = var.pgsql_sku
  pgsql_storage_mb               = var.pgsql_storage_mb
  force_user_permissions_trigger = "2"
}

# Private Endpoint in VH Wowza subnet
data "azurerm_subnet" "vh_private_endpoints" {
  provider = azurerm.vh_vnet

  resource_group_name  = "ss-${var.vh_environment}-network-rg"
  virtual_network_name = "ss-${var.vh_environment}-vnet"
  name                 = "vh_private_endpoints"
}

resource "azurerm_private_endpoint" "vh_vnet_private_endpoint" {
  provider = azurerm.vh_vnet
  count    = var.create_vh_vnet_private_endpoint == "true" ? 1 : 0

  name                = module.storage_account.storageaccount_name
  resource_group_name = data.azurerm_subnet.vh_private_endpoints.resource_group_name
  location            = var.location
  subnet_id           = data.azurerm_subnet.vh_private_endpoints.id

  private_service_connection {
    name                           = module.storage_account.storageaccount_name
    is_manual_connection           = false
    private_connection_resource_id = module.storage_account.storageaccount_id
    subresource_names              = ["blob"]
  }

  # private_dns_zone_group {
  #   name                 = "endpoint-dnszonegroup"
  #   private_dns_zone_ids = local.private_dns_zone_ids
  # }

  tags = var.common_tags
}

# Private Endpoint in CVP Wowza subnet
data "azurerm_subnet" "cvp_private_endpoints" {
  provider = azurerm.cvp_vnet

  resource_group_name  = "cvp-recordings-${var.cvp_environment}-rg"
  virtual_network_name = "cvp-recordings-${var.cvp_environment}-vnet"
  name                 = "wowza"
}

resource "azurerm_private_endpoint" "cvp_vnet_private_endpoint" {
  provider = azurerm.cvp_vnet
  count    = var.create_cvp_vnet_private_endpoint == "true" ? 1 : 0

  name                = module.storage_account.storageaccount_name
  resource_group_name = data.azurerm_subnet.cvp_private_endpoints.resource_group_name
  location            = var.location
  subnet_id           = data.azurerm_subnet.cvp_private_endpoints.id

  private_service_connection {
    name                           = module.storage_account.storageaccount_name
    is_manual_connection           = false
    private_connection_resource_id = module.storage_account.storageaccount_id
    subresource_names              = ["blob"]
  }

  # private_dns_zone_group {
  #   name                 = "endpoint-dnszonegroup"
  #   private_dns_zone_ids = local.private_dns_zone_ids
  # }

  tags = var.common_tags
}
