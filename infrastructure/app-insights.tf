data "azurerm_application_insights" "ai" {
  name                = "rpa-${var.env}"
  resource_group_name = "rpa-${var.env}"
}

locals {
  app_insights_config = jsonencode(
    merge(
      jsondecode(
      file("${path.module}/../lib/applicationinsights.json")), { connectionString = data.azurerm_application_insights.ai.connection_string }
    )
  )
}

resource "azurerm_key_vault_secret" "app_insights_config" {
  name         = "${var.component}-app-insights-config"
  value        = local.app_insights_config
  key_vault_id = module.key-vault.key_vault_id
}
