//v15 DB
pgsql_sku                        = "GP_Standard_D4ds_v4"
vh_subscription_id               = "7a4e3bd5-ae3a-4d0c-b441-2188fee3ff1c" #DTS-SHAREDSERVICES-DEV
vh_environment                   = "dev"
create_vh_vnet_private_endpoint  = "false"
cvp_subscription_id              = "7a4e3bd5-ae3a-4d0c-b441-2188fee3ff1c" #DTS-SHAREDSERVICES-STG
cvp_environment                  = "stg"
create_cvp_vnet_private_endpoint = "false"
aging_rule_hot_to_cold           = false
aging_rule_in_days               = 1
