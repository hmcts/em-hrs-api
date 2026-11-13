terraform {
  backend "azurerm" {}

  required_providers {
    azuread = {
      source  = "hashicorp/azuread"
      version = "3.10.0"
    }
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.81.0"

    }
    azapi = {
      source  = "Azure/azapi"
      version = "~> 2.13.0"
    }
  }
}
