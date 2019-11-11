
SECRETS_KEYS_CMD="az keyvault secret list --vault-name $TARGET_KEY_VAULT | jq  --raw-output '.[]|(.id / "/")[4]'"
DEFAULTS_FILE="./defaults.var"
SUBNET_FILE="./subnet.var"
PASSWORD_KEYS_FILE="./passwords.var"
DEFAULT_SECRETS_KEYS=$(awk -F '=' '{print $1}' "$DEFAULTS_FILE")
DEFAULT_SECRETS_VALUES=$(awk -F '=' '{print $2}' "$DEFAULTS_FILE")

#read -r -p "Enter source keyvault: " SOURCE_KEY_VAULT
read -r -p "Enter target keyvault: " TARGET_KEY_VAULT

# Set default secrets
echo "Adding defaults..."
declare -A DEFAULT_SECRETS_ARRAY
while IFS=\= read key value
do
	DEFAULT_SECRETS_ARRAY[$key]=$value
done < $DEFAULTS_FILE

# Add default secrets to target keyvault
for i in "${!DEFAULT_SECRETS_ARRAY[@]}" ; do
	az keyvault secret set --vault-name $TARGET_KEY_VAULT -n $i --value "${DEFAULT_SECRETS_ARRAY[$i]}"
done

# Generate needed passwords from passwords.var
for i in $(cat $PASSWORD_KEYS_FILE); do
	PASSWORD=$(apg -m10 -x20 -M sncl -n1)
	az keyvault secret set --vault-name $TARGET_KEY_VAULT -n $i --value $PASSWORD
done
