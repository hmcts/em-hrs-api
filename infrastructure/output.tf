output "vh_vnet_private_endpoint" {
  value = azurerm_private_endpoint.vh_vnet_private_endpoint.id
  description = "The ID of the vh_vnet_private_endpoint"
}

output "cvp_vnet_private_endpoint" {
  value = azurerm_private_endpoint.cvp_vnet_private_endpoint.id
  description = "The ID of the cvp_vnet_private_endpoint"
}
