output "cft_aks_subscription_id" {
  description = "The subscription ID for the CFT AKS cluster."
  value       = var.aks_subscription_id
}

output "cft_aks_resource_group_name" {
  description = "The resource group name for the CFT AKS cluster."
  value       = data.azurerm_subnet.private_endpoints[0].name
}

output "vh_vnet_private_endpointnt" {
  value       = azurerm_private_endpoint.vh_vnet_private_endpoint[0].id
}

output "cvp_vnet_private_endpointnt" {
  value       = azurerm_private_endpoint.cvp_vnet_private_endpoint[0].id
}