terraform {
  backend "azurerm" {}

  required_providers {
    azuread = {
      source  = "hashicorp/azuread"
      version = "3.8.0"
    }
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.60.0"

    }
    azapi = {
      source  = "Azure/azapi"
      version = "~> 1.15.0"
    }
  }
}
