variable "product" {}

variable "component" {}

variable "location" {
  default = "UK South"
}

variable "env" {}

variable "subscription" {}

variable "deployment_namespace" {}

variable "tenant_id" {}

variable "jenkins_AAD_objectId" {
  description = "(Required) The Azure AD object ID of a user, service principal or security group in the Azure Active Directory tenant for the vault. The object ID must be unique for the list of access policies."
}

variable "common_tags" {
  type = "map"
}

variable "postgresql_user" {
  default = "emhrs"
}

variable "database_name" {
  default = "emhrs"
}

variable "team_contact" {
  default = "#em-dev-chat"
}

variable "destroy_me" {
  default = "No"
}

variable sku_name {
  default = "GP_Gen5_2"
}

variable sku_capacity {
  default = "2"
}
