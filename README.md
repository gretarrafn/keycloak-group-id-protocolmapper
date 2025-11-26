# Keycloak Group ID Protocol Mapper

A Keycloak protocol mapper addon that adds user group IDs (UUIDs) to OIDC tokens as a JSON array claim.

## Features

- Adds user's Keycloak group IDs to access tokens, ID tokens, and userinfo responses
- Configurable claim name (defaults to `group_ids`)
- Configurable JSON type
- Configurable token inclusion (access token, ID token, userinfo)
- Compatible with Keycloak 20.0.1+

## Building

```bash
mvn clean package
```

The JAR file will be generated in `target/group-id-protocol-mapper-1.0.0.jar`.

## Installation

1. Copy the JAR file to your Keycloak server's `providers` directory:
   ```bash
   cp target/group-id-protocol-mapper-1.0.0.jar $KEYCLOAK_HOME/providers/
   ```

2. Build the server to register the provider:
   ```bash
   $KEYCLOAK_HOME/bin/kc.sh build
   ```

3. Restart Keycloak

## Usage

1. Navigate to your Keycloak Admin Console
2. Go to **Clients** → Select your client → **Client scopes** → Select a scope → **Mappers**
3. Click **Add mapper** → **By configuration**
4. Select **Group IDs** from the mapper type dropdown
5. Configure:
   - **Token Claim Name**: Name of the claim (default: `group_ids`)
   - **JSON type**: Type of the claim value (e.g., `String`, `JSON`)
   - **Add to access token**: Include in access token
   - **Add to ID token**: Include in ID token
   - **Add to userinfo**: Include in userinfo response
6. Save

The group IDs will appear as a JSON array in the configured tokens:

```json
{
  "group_ids": [
    "uuid-1",
    "uuid-2",
    "uuid-3"
  ]
}
```

## Requirements

- Java 11+
- Keycloak 20.0.1+
- Maven 3.6+

## License

[Add your license here]

