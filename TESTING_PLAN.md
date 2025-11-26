# Unit Testing Plan for GroupIdProtocolMapper

## Overview
Test the `GroupIdProtocolMapper` class to ensure it correctly adds group IDs to OIDC tokens under various conditions.

## Testing Framework & Dependencies

### Required Dependencies
- **JUnit 5** (junit-jupiter) - Modern testing framework
- **Mockito** - Mocking framework for Keycloak SPI objects
- **AssertJ** (optional) - Fluent assertions
- **Keycloak test dependencies** - For SPI classes (test scope)

### Maven Dependencies to Add
```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.5.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <version>5.5.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <version>3.24.2</version>
    <scope>test</scope>
</dependency>
```

## Test Cases

### 1. Metadata Tests (Simple Getters)
**Test Class:** `GroupIdProtocolMapperTest`

- ✅ `getId()` returns correct provider ID
- ✅ `getProtocol()` returns "openid-connect"
- ✅ `getDisplayType()` returns "Group IDs"
- ✅ `getDisplayCategory()` returns token mapper category
- ✅ `getHelpText()` returns correct description
- ✅ `getConfigProperties()` returns non-empty list

### 2. Configuration Properties Tests
- ✅ Config properties list contains expected properties
- ✅ Config properties include token claim name config
- ✅ Config properties include JSON type config
- ✅ Config properties include include-in-tokens config

### 3. Core Functionality Tests - setClaim()

#### 3.1 Happy Path
- ✅ User with multiple groups → claim added with all group IDs
- ✅ Custom claim name configured → uses custom name
- ✅ Default claim name when not configured → uses "group_ids"

#### 3.2 Edge Cases
- ✅ Null userSession → no claim added, no exception
- ✅ Null user → no claim added, no exception
- ✅ User with no groups → no claim added, no exception
- ✅ Empty claim name in config → uses default "group_ids"
- ✅ Whitespace-only claim name → uses default "group_ids"
- ✅ User with single group → claim added with one ID

#### 3.3 Error Handling
- ✅ Exception during group retrieval → logged, no exception thrown
- ✅ Exception during claim setting → logged, no exception thrown
- ✅ Null token → handled gracefully

### 4. Method Signature Tests
- ✅ 5-arg `setClaim()` delegates to internal implementation
- ✅ 3-arg deprecated `setClaim()` delegates to internal implementation
- ✅ Both signatures produce same result

### 5. Integration-Style Tests
- ✅ Claim appears in token's otherClaims map
- ✅ Claim value is List<String> type
- ✅ Group IDs are correct UUIDs/strings
- ✅ Multiple mappers don't interfere with each other

## Test Structure

### Test Class Organization
```
GroupIdProtocolMapperTest
├── MetadataTests (nested)
├── ConfigurationTests (nested)
├── SetClaimHappyPathTests (nested)
├── SetClaimEdgeCaseTests (nested)
├── SetClaimErrorHandlingTests (nested)
└── MethodSignatureTests (nested)
```

### Mock Objects Needed
- `IDToken` - Token to add claims to
- `ProtocolMapperModel` - Mapper configuration
- `UserSessionModel` - User session
- `UserModel` - User with groups
- `GroupModel` - Individual groups
- `KeycloakSession` - Session (for 5-arg method)
- `ClientSessionContext` - Client context (for 5-arg method)
- `Logger` - For verifying log calls (optional)

### Helper Methods
- `createMockUserSession()` - Creates mock user session with user
- `createMockUserWithGroups(String... groupIds)` - Creates user with groups
- `createMockGroup(String id)` - Creates mock group
- `createProtocolMapperModel(String claimName)` - Creates mapper config
- `createIDToken()` - Creates fresh ID token

## Implementation Steps

1. **Add test dependencies to pom.xml**
2. **Create test directory structure:** `src/test/java/com/example/keycloak/mappers/`
3. **Create base test class with setup/teardown**
4. **Implement metadata tests** (simplest, good starting point)
5. **Implement configuration tests**
6. **Implement core functionality tests** (most important)
7. **Add test coverage reporting** (optional: JaCoCo)
8. **Update CI workflow to run tests**

## Test Coverage Goals

- **Line Coverage:** > 90%
- **Branch Coverage:** > 85%
- **Critical Paths:** 100% (setClaimInternal logic)

## Example Test Structure

```java
@ExtendWith(MockitoExtension.class)
class GroupIdProtocolMapperTest {
    
    @Mock
    private UserSessionModel userSession;
    
    @Mock
    private UserModel user;
    
    @Mock
    private IDToken token;
    
    @Mock
    private ProtocolMapperModel mapperModel;
    
    private GroupIdProtocolMapper mapper;
    private Map<String, String> config;
    private Map<String, Object> otherClaims;
    
    @BeforeEach
    void setUp() {
        mapper = new GroupIdProtocolMapper();
        config = new HashMap<>();
        otherClaims = new HashMap<>();
        
        when(mapperModel.getConfig()).thenReturn(config);
        when(token.getOtherClaims()).thenReturn(otherClaims);
    }
    
    // Test methods...
}
```

## CI Integration

- Update `.github/workflows/ci.yml` to run tests
- Add test failure as build failure condition
- Optionally add coverage reporting

## Notes

- Keycloak SPI classes may need special mocking (they're interfaces)
- `getGroupsStream()` returns a Stream - mock carefully
- Logger is static - may need PowerMockito or accept that logging can't be fully tested
- Focus on behavior, not implementation details

