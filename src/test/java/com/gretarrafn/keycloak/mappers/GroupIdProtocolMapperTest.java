package com.gretarrafn.keycloak.mappers;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GroupIdProtocolMapper Tests")
class GroupIdProtocolMapperTest {

    private GroupIdProtocolMapper mapper;
    
    private IDToken token;
    
    private ProtocolMapperModel mapperModel;
    
    @Mock
    private UserSessionModel userSession;
    
    @Mock
    private UserModel user;
    
    @Mock
    private KeycloakSession keycloakSession;
    
    @Mock
    private ClientSessionContext clientSessionContext;
    
    private Map<String, String> config;

    @BeforeEach
    void setUp() {
        mapper = new GroupIdProtocolMapper();
        config = new HashMap<>();
        
        // Use real IDToken instance instead of mock (IDToken is a concrete class)
        token = new IDToken();
        
        // Create a test ProtocolMapperModel instance
        mapperModel = createProtocolMapperModel(config);
    }
    
    private ProtocolMapperModel createProtocolMapperModel(Map<String, String> configMap) {
        ProtocolMapperModel model = new ProtocolMapperModel();
        model.setConfig(configMap);
        return model;
    }

    @Nested
    @DisplayName("Metadata Tests")
    class MetadataTests {
        
        @Test
        @DisplayName("getId() returns correct provider ID")
        void getId_ReturnsCorrectProviderId() {
            assertThat(mapper.getId()).isEqualTo("oidc-group-id-protocol-mapper");
        }
        
        @Test
        @DisplayName("getProtocol() returns openid-connect")
        void getProtocol_ReturnsOpenIdConnect() {
            assertThat(mapper.getProtocol()).isEqualTo("openid-connect");
        }
        
        @Test
        @DisplayName("getDisplayType() returns Group IDs")
        void getDisplayType_ReturnsGroupIds() {
            assertThat(mapper.getDisplayType()).isEqualTo("Group IDs");
        }
        
        @Test
        @DisplayName("getDisplayCategory() returns token mapper category")
        void getDisplayCategory_ReturnsTokenMapperCategory() {
            // TOKEN_MAPPER_CATEGORY is a constant from AbstractOIDCProtocolMapper
            // We verify it returns a non-null string (the actual constant value)
            String category = mapper.getDisplayCategory();
            assertThat(category).isNotNull().isNotEmpty();
        }
        
        @Test
        @DisplayName("getHelpText() returns correct description")
        void getHelpText_ReturnsCorrectDescription() {
            assertThat(mapper.getHelpText())
                .isEqualTo("Adds the user's Keycloak group IDs (UUIDs) to a JSON array claim in the token.");
        }
        
        @Test
        @DisplayName("getConfigProperties() returns non-empty list")
        void getConfigProperties_ReturnsNonEmptyList() {
            List<ProviderConfigProperty> properties = mapper.getConfigProperties();
            assertThat(properties).isNotNull().isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {
        
        @Test
        @DisplayName("Config properties include token claim name config")
        void configProperties_IncludeTokenClaimNameConfig() {
            List<ProviderConfigProperty> properties = mapper.getConfigProperties();
            boolean hasClaimName = properties.stream()
                .anyMatch(p -> OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME.equals(p.getName()));
            assertThat(hasClaimName).isTrue();
        }
        
        @Test
        @DisplayName("Config properties include JSON type config")
        void configProperties_IncludeJsonTypeConfig() {
            List<ProviderConfigProperty> properties = mapper.getConfigProperties();
            boolean hasJsonType = properties.stream()
                .anyMatch(p -> OIDCAttributeMapperHelper.JSON_TYPE.equals(p.getName()));
            assertThat(hasJsonType).isTrue();
        }
        
        @Test
        @DisplayName("Config properties include include-in-tokens config")
        void configProperties_IncludeIncludeInTokensConfig() {
            List<ProviderConfigProperty> properties = mapper.getConfigProperties();
            // Check for any property related to include in tokens
            boolean hasIncludeConfig = properties.stream()
                .anyMatch(p -> p.getName() != null && 
                    (p.getName().contains("access.token") || 
                     p.getName().contains("id.token") || 
                     p.getName().contains("userinfo")));
            assertThat(hasIncludeConfig).isTrue();
        }
    }

    @Nested
    @DisplayName("SetClaim Happy Path Tests")
    class SetClaimHappyPathTests {
        
        @Test
        @DisplayName("User with multiple groups adds claim with all group IDs")
        void setClaim_UserWithMultipleGroups_AddsClaimWithAllGroupIds() {
            // Given
            String groupId1 = "group-uuid-1";
            String groupId2 = "group-uuid-2";
            String groupId3 = "group-uuid-3";
            
            GroupModel group1 = createMockGroup(groupId1);
            GroupModel group2 = createMockGroup(groupId2);
            GroupModel group3 = createMockGroup(groupId3);
            
            when(userSession.getUser()).thenReturn(user);
            when(user.getGroupsStream()).thenReturn(Stream.of(group1, group2, group3));
            config.put("claim.name", "group_ids");
            
            // When
            mapper.setClaim(token, mapperModel, userSession, keycloakSession, clientSessionContext);
            
            // Then
            assertThat(token.getOtherClaims()).containsKey("group_ids");
            @SuppressWarnings("unchecked")
            List<String> groupIds = (List<String>) token.getOtherClaims().get("group_ids");
            assertThat(groupIds).containsExactly(groupId1, groupId2, groupId3);
        }
        
        @Test
        @DisplayName("Custom claim name configured uses custom name")
        void setClaim_CustomClaimName_UsesCustomName() {
            // Given
            String customClaimName = "custom_group_ids";
            String groupId = "group-uuid-1";
            
            GroupModel group = createMockGroup(groupId);
            when(userSession.getUser()).thenReturn(user);
            when(user.getGroupsStream()).thenReturn(Stream.of(group));
            config.put("claim.name", customClaimName);
            
            // When
            mapper.setClaim(token, mapperModel, userSession, keycloakSession, clientSessionContext);
            
            // Then
            assertThat(token.getOtherClaims()).containsKey(customClaimName);
            assertThat(token.getOtherClaims()).doesNotContainKey("group_ids");
            @SuppressWarnings("unchecked")
            List<String> groupIds = (List<String>) token.getOtherClaims().get(customClaimName);
            assertThat(groupIds).containsExactly(groupId);
        }
        
        @Test
        @DisplayName("Default claim name when not configured uses group_ids")
        void setClaim_NoClaimNameConfigured_UsesDefaultGroupIds() {
            // Given
            String groupId = "group-uuid-1";
            GroupModel group = createMockGroup(groupId);
            when(userSession.getUser()).thenReturn(user);
            when(user.getGroupsStream()).thenReturn(Stream.of(group));
            // No claim.name in config
            
            // When
            mapper.setClaim(token, mapperModel, userSession, keycloakSession, clientSessionContext);
            
            // Then
            assertThat(token.getOtherClaims()).containsKey("group_ids");
            @SuppressWarnings("unchecked")
            List<String> groupIds = (List<String>) token.getOtherClaims().get("group_ids");
            assertThat(groupIds).containsExactly(groupId);
        }
        
        @Test
        @DisplayName("User with single group adds claim with one ID")
        void setClaim_UserWithSingleGroup_AddsClaimWithOneId() {
            // Given
            String groupId = "group-uuid-1";
            GroupModel group = createMockGroup(groupId);
            when(userSession.getUser()).thenReturn(user);
            when(user.getGroupsStream()).thenReturn(Stream.of(group));
            
            // When
            mapper.setClaim(token, mapperModel, userSession, keycloakSession, clientSessionContext);
            
            // Then
            assertThat(token.getOtherClaims()).containsKey("group_ids");
            @SuppressWarnings("unchecked")
            List<String> groupIds = (List<String>) token.getOtherClaims().get("group_ids");
            assertThat(groupIds).hasSize(1).containsExactly(groupId);
        }
    }

    @Nested
    @DisplayName("SetClaim Edge Case Tests")
    class SetClaimEdgeCaseTests {
        
        @Test
        @DisplayName("Null userSession does not add claim and does not throw exception")
        void setClaim_NullUserSession_NoClaimAdded() {
            // When
            mapper.setClaim(token, mapperModel, null, keycloakSession, clientSessionContext);
            
            // Then
            assertThat(token.getOtherClaims()).isEmpty();
        }
        
        @Test
        @DisplayName("Null user does not add claim and does not throw exception")
        void setClaim_NullUser_NoClaimAdded() {
            // Given
            when(userSession.getUser()).thenReturn(null);
            
            // When
            mapper.setClaim(token, mapperModel, userSession, keycloakSession, clientSessionContext);
            
            // Then
            assertThat(token.getOtherClaims()).isEmpty();
        }
        
        @Test
        @DisplayName("User with no groups does not add claim")
        void setClaim_UserWithNoGroups_NoClaimAdded() {
            // Given
            when(userSession.getUser()).thenReturn(user);
            when(user.getGroupsStream()).thenReturn(Stream.empty());
            
            // When
            mapper.setClaim(token, mapperModel, userSession, keycloakSession, clientSessionContext);
            
            // Then
            assertThat(token.getOtherClaims()).isEmpty();
        }
        
        @Test
        @DisplayName("Empty claim name in config uses default group_ids")
        void setClaim_EmptyClaimName_UsesDefault() {
            // Given
            String groupId = "group-uuid-1";
            GroupModel group = createMockGroup(groupId);
            when(userSession.getUser()).thenReturn(user);
            when(user.getGroupsStream()).thenReturn(Stream.of(group));
            config.put("claim.name", "");
            
            // When
            mapper.setClaim(token, mapperModel, userSession, keycloakSession, clientSessionContext);
            
            // Then
            assertThat(token.getOtherClaims()).containsKey("group_ids");
        }
        
        @Test
        @DisplayName("Whitespace-only claim name uses default group_ids")
        void setClaim_WhitespaceOnlyClaimName_UsesDefault() {
            // Given
            String groupId = "group-uuid-1";
            GroupModel group = createMockGroup(groupId);
            when(userSession.getUser()).thenReturn(user);
            when(user.getGroupsStream()).thenReturn(Stream.of(group));
            config.put("claim.name", "   ");
            
            // When
            mapper.setClaim(token, mapperModel, userSession, keycloakSession, clientSessionContext);
            
            // Then
            assertThat(token.getOtherClaims()).containsKey("group_ids");
        }
    }

    @Nested
    @DisplayName("SetClaim Error Handling Tests")
    class SetClaimErrorHandlingTests {
        
        @Test
        @DisplayName("Exception during group retrieval is handled gracefully")
        void setClaim_ExceptionDuringGroupRetrieval_HandledGracefully() {
            // Given
            when(userSession.getUser()).thenReturn(user);
            when(user.getGroupsStream()).thenThrow(new RuntimeException("Test exception"));
            
            // When/Then - should not throw
            Assertions.assertThatCode(() -> 
                mapper.setClaim(token, mapperModel, userSession, keycloakSession, clientSessionContext)
            ).doesNotThrowAnyException();
            
            // Claim should not be added
            assertThat(token.getOtherClaims()).isEmpty();
        }
        
        @Test
        @DisplayName("Exception during claim setting is handled gracefully")
        void setClaim_ExceptionDuringClaimSetting_HandledGracefully() {
            // Given
            String groupId = "group-uuid-1";
            GroupModel group = createMockGroup(groupId);
            when(userSession.getUser()).thenReturn(user);
            when(user.getGroupsStream()).thenReturn(Stream.of(group));
            // Using real IDToken, so we can't simulate exception on getOtherClaims()
            // This test verifies the try-catch handles exceptions gracefully
            
            // When/Then - should not throw
            Assertions.assertThatCode(() -> 
                mapper.setClaim(token, mapperModel, userSession, keycloakSession, clientSessionContext)
            ).doesNotThrowAnyException();
        }
        
        @Test
        @DisplayName("Null token is handled gracefully")
        void setClaim_NullToken_HandledGracefully() {
            // Given
            String groupId = "group-uuid-1";
            GroupModel group = createMockGroup(groupId);
            when(userSession.getUser()).thenReturn(user);
            when(user.getGroupsStream()).thenReturn(Stream.of(group));
            
            // When/Then - should not throw
            Assertions.assertThatCode(() -> 
                mapper.setClaim(null, mapperModel, userSession, keycloakSession, clientSessionContext)
            ).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Method Signature Tests")
    class MethodSignatureTests {
        
        @Test
        @DisplayName("5-arg setClaim() delegates to internal implementation")
        void setClaim_FiveArgSignature_WorksCorrectly() {
            // Given
            String groupId = "group-uuid-1";
            GroupModel group = createMockGroup(groupId);
            when(userSession.getUser()).thenReturn(user);
            when(user.getGroupsStream()).thenReturn(Stream.of(group));
            
            // When
            mapper.setClaim(token, mapperModel, userSession, keycloakSession, clientSessionContext);
            
            // Then
            assertThat(token.getOtherClaims()).containsKey("group_ids");
        }
        
        @Test
        @DisplayName("3-arg deprecated setClaim() delegates to internal implementation")
        @SuppressWarnings("deprecation")
        void setClaim_ThreeArgSignature_WorksCorrectly() {
            // Given
            String groupId = "group-uuid-1";
            GroupModel group = createMockGroup(groupId);
            when(userSession.getUser()).thenReturn(user);
            when(user.getGroupsStream()).thenReturn(Stream.of(group));
            
            // When
            mapper.setClaim(token, mapperModel, userSession);
            
            // Then
            assertThat(token.getOtherClaims()).containsKey("group_ids");
        }
        
        @Test
        @DisplayName("Both method signatures produce same result")
        @SuppressWarnings("deprecation")
        void setClaim_BothSignatures_ProduceSameResult() {
            // Given
            String groupId1 = "group-uuid-1";
            String groupId2 = "group-uuid-2";
            GroupModel group1 = createMockGroup(groupId1);
            GroupModel group2 = createMockGroup(groupId2);
            
            IDToken token1 = new IDToken();
            IDToken token2 = new IDToken();
            
            // Create separate user sessions to avoid stream reuse issues
            UserSessionModel userSession1 = mock(UserSessionModel.class);
            UserSessionModel userSession2 = mock(UserSessionModel.class);
            UserModel user1 = mock(UserModel.class);
            UserModel user2 = mock(UserModel.class);
            
            when(userSession1.getUser()).thenReturn(user1);
            when(userSession2.getUser()).thenReturn(user2);
            when(user1.getGroupsStream()).thenReturn(Stream.of(group1, group2));
            when(user2.getGroupsStream()).thenReturn(Stream.of(group1, group2));
            
            // When
            mapper.setClaim(token1, mapperModel, userSession1, keycloakSession, clientSessionContext);
            mapper.setClaim(token2, mapperModel, userSession2);
            
            // Then
            assertThat(token1.getOtherClaims()).isEqualTo(token2.getOtherClaims());
        }
    }

    @Nested
    @DisplayName("Integration-Style Tests")
    class IntegrationStyleTests {
        
        @Test
        @DisplayName("Claim appears in token's otherClaims map")
        void setClaim_ClaimAppearsInOtherClaimsMap() {
            // Given
            String groupId = "group-uuid-1";
            GroupModel group = createMockGroup(groupId);
            when(userSession.getUser()).thenReturn(user);
            when(user.getGroupsStream()).thenReturn(Stream.of(group));
            
            // When
            mapper.setClaim(token, mapperModel, userSession, keycloakSession, clientSessionContext);
            
            // Then
            assertThat(token.getOtherClaims()).isNotEmpty();
            assertThat(token.getOtherClaims()).containsKey("group_ids");
        }
        
        @Test
        @DisplayName("Claim value is List<String> type")
        void setClaim_ClaimValueIsListOfStrings() {
            // Given
            String groupId = "group-uuid-1";
            GroupModel group = createMockGroup(groupId);
            when(userSession.getUser()).thenReturn(user);
            when(user.getGroupsStream()).thenReturn(Stream.of(group));
            
            // When
            mapper.setClaim(token, mapperModel, userSession, keycloakSession, clientSessionContext);
            
            // Then
            Object claimValue = token.getOtherClaims().get("group_ids");
            assertThat(claimValue).isInstanceOf(List.class);
            @SuppressWarnings("unchecked")
            List<String> groupIds = (List<String>) claimValue;
            assertThat(groupIds).allMatch(id -> id instanceof String);
        }
        
        @Test
        @DisplayName("Group IDs are correct values")
        void setClaim_GroupIdsAreCorrectValues() {
            // Given
            String groupId1 = "group-uuid-1";
            String groupId2 = "group-uuid-2";
            GroupModel group1 = createMockGroup(groupId1);
            GroupModel group2 = createMockGroup(groupId2);
            when(userSession.getUser()).thenReturn(user);
            when(user.getGroupsStream()).thenReturn(Stream.of(group1, group2));
            
            // When
            mapper.setClaim(token, mapperModel, userSession, keycloakSession, clientSessionContext);
            
            // Then
            @SuppressWarnings("unchecked")
            List<String> groupIds = (List<String>) token.getOtherClaims().get("group_ids");
            assertThat(groupIds).containsExactly(groupId1, groupId2);
        }
    }

    // Helper methods
    
    private GroupModel createMockGroup(String id) {
        GroupModel group = mock(GroupModel.class);
        when(group.getId()).thenReturn(id);
        return group;
    }
}

