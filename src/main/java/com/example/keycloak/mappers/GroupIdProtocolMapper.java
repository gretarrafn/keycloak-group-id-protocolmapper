package com.example.keycloak.mappers;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GroupIdProtocolMapper extends AbstractOIDCProtocolMapper
        implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    public static final String PROVIDER_ID = "oidc-group-id-protocol-mapper";
    private static final Logger LOG = Logger.getLogger(GroupIdProtocolMapper.class);

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {
        // Expose standard options in Admin UI:
        // - Token Claim Name
        // - JSON type
        // - Include in access/ID/userinfo
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(CONFIG_PROPERTIES);
        OIDCAttributeMapperHelper.addJsonTypeConfig(CONFIG_PROPERTIES);
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(CONFIG_PROPERTIES, GroupIdProtocolMapper.class);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getProtocol() {
        return "openid-connect";
    }

    @Override
    public String getDisplayType() {
        return "Group IDs";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY; // shows up under "Token mapper"
    }

    @Override
    public String getHelpText() {
        return "Adds the user's Keycloak group IDs (UUIDs) to a JSON array claim in the token.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    /**
     * Non-deprecated method signature for Keycloak 20.x+
     */
    @Override
    protected void setClaim(
            IDToken token,
            ProtocolMapperModel mappingModel,
            UserSessionModel userSession,
            KeycloakSession keycloakSession,
            ClientSessionContext clientSessionCtx) {
        setClaimInternal(token, mappingModel, userSession);
    }

    /**
     * Deprecated 3-arg variant - delegates to internal implementation.
     * Kept for backward compatibility.
     */
    @Override
    @Deprecated
    protected void setClaim(
            IDToken token,
            ProtocolMapperModel mappingModel,
            UserSessionModel userSession) {
        setClaimInternal(token, mappingModel, userSession);
    }

    /**
     * Internal implementation shared by both method signatures.
     */
    private void setClaimInternal(
            IDToken token,
            ProtocolMapperModel mappingModel,
            UserSessionModel userSession) {

        try {
            if (userSession == null) {
                LOG.debug("GroupIdProtocolMapper: userSession is null, skipping");
                return;
            }

            UserModel user = userSession.getUser();
            if (user == null) {
                LOG.debug("GroupIdProtocolMapper: user is null, skipping");
                return;
            }

            // Keycloak 20.x: this exists on UserModel
            List<String> groupIds = user.getGroupsStream()
                    .map(GroupModel::getId)
                    .collect(Collectors.toList());

            if (groupIds.isEmpty()) {
                LOG.debug("GroupIdProtocolMapper: user has no groups, skipping");
                return;
            }

            // Get the claim name from configuration (defaults to "group_ids" if not set)
            String claimName = mappingModel.getConfig().get("claim.name");
            if (claimName == null || claimName.trim().isEmpty()) {
                claimName = "group_ids"; // default fallback
            }
            
            // Set the claim directly as a List (JSON array) to avoid type conversion issues
            token.getOtherClaims().put(claimName, groupIds);

        } catch (Exception e) {
            // Never break token issuance; just log
            LOG.error("GroupIdProtocolMapper: failed to set group IDs claim", e);
        }
    }
}

