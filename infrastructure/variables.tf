variable "product" {}

variable "component" {}

variable "location" {
  default = "UK South"
}

variable "env" {}

variable "subscription" {}

variable "tenant_id" {}

variable "jenkins_AAD_objectId" {
  description = "(Required) The Azure AD object ID of a user, service principal or security group in the Azure Active Directory tenant for the vault. The object ID must be unique for the list of access policies."
}

variable "common_tags" {
  type = map(string)
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

variable "sku_name" {
  default = "GP_Gen5_2"
}

variable "sku_capacity" {
  default = "2"
}

variable "aks_subscription_id" {}

variable "pgsql_sku" {
  description = "The PGSql flexible server instance sku"
  default     = "GP_Standard_D2ds_v4"
}

variable "pgsql_storage_mb" {
  description = "Max storage allowed for the PGSql Flexibile instance"
  type        = number
  default     = 65536
}

variable "action_group_name" {
  description = "The name of the Action Group to create."
  type        = string
  default     = "em-support"
}

variable "email_address_key" {
  description = "Email address key in azure Key Vault."
  type        = string
  default     = "db-alert-monitoring-email-address"
}

variable "businessArea" {
  default = "cft"
}
