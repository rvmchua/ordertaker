package com.rvmc.ordertaker.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rvmc.ordertaker.entity.enums.Role;
import com.rvmc.ordertaker.security.JwtService;
import com.rvmc.ordertaker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {
    private static final String ALLOWED_ORIGIN = "https://client.example.test";
    private static final String CUSTOMER_SIGNUP = """
            {
              "username": "customer",
              "email": "Customer@Example.Test",
              "password": "long-enough-password",
              "customerName": "Customer Name",
              "address1": "1 Main Street",
              "address2": ""
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void clearAccounts() {
        userRepository.deleteAll();
    }

    @Test
    void customerSignupNormalizesEmailAndCreatesActiveCustomer() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUSTOMER_SIGNUP))
                .andExpect(status().isCreated());

        var user = userRepository.findByEmail("customer@example.test").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(Role.CUSTOMER, user.getRole());
        org.junit.jupiter.api.Assertions.assertTrue(user.getCustomer().getIsActive());
        org.junit.jupiter.api.Assertions.assertTrue(user.getPasswordHash().startsWith("$2"));
        org.junit.jupiter.api.Assertions.assertNotEquals("long-enough-password", user.getPasswordHash());
    }

    @Test
    void restaurantSignupCreatesActiveRestaurantOwner() throws Exception {
        mockMvc.perform(post("/auth/restaurants/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "owner",
                                  "email": "owner@example.test",
                                  "password": "long-enough-password",
                                  "address1": "2 Main Street",
                                  "restaurantName": "Example Restaurant"
                                }
                                """))
                .andExpect(status().isCreated());

        var user = userRepository.findByEmail("owner@example.test").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(Role.RESTAURANT, user.getRole());
        org.junit.jupiter.api.Assertions.assertTrue(user.getRestaurant().getIsActive());
    }

    @Test
    void duplicateNormalizedEmailReturnsConflict() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUSTOMER_SIGNUP))
                .andExpect(status().isCreated());

        String duplicate = CUSTOMER_SIGNUP.replace("Customer@Example.Test", " customer@example.test ");
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicate))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidRegistrationFieldsReturnBadRequest() throws Exception {
        String invalid = CUSTOMER_SIGNUP.replace("long-enough-password", "short");
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginReturnsBearerTokenAndRejectsWrongRoleLogin() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUSTOMER_SIGNUP))
                .andExpect(status().isCreated());

        String login = """
                {"email":" customer@example.test ","password":"long-enough-password"}
                """;
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresIn", is(900)))
                .andReturn().getResponse().getContentAsString();
        JsonNode tokenResponse = objectMapper.readTree(response);
        JwtService.AuthenticatedUser authenticatedUser = jwtService.parse(
                tokenResponse.get("accessToken").asText());
        org.junit.jupiter.api.Assertions.assertEquals(Role.CUSTOMER, authenticatedUser.role());

        mockMvc.perform(post("/auth/restaurants/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void restaurantOwnerLoginIssuesRestaurantRoleToken() throws Exception {
        mockMvc.perform(post("/auth/restaurants/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "owner",
                                  "email": "owner@example.test",
                                  "password": "long-enough-password",
                                  "address1": "2 Main Street",
                                  "restaurantName": "Example Restaurant"
                                }
                                """))
                .andExpect(status().isCreated());

        String response = mockMvc.perform(post("/auth/restaurants/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"owner@example.test","password":"long-enough-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andReturn().getResponse().getContentAsString();
        JsonNode tokenResponse = objectMapper.readTree(response);

        org.junit.jupiter.api.Assertions.assertEquals(
                Role.RESTAURANT,
                jwtService.parse(tokenResponse.get("accessToken").asText()).role());
    }

    @Test
    void invalidCredentialsDoNotRevealAccountExistence() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"missing@example.test","password":"long-enough-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail", is("Invalid email or password")));
    }

    @Test
    void protectedApiRequiresBearerAndPublicRestaurantReadRemainsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/customers/profile"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/restaurants"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/restaurants/42"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/restaurants/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerTokenCannotAccessRestaurantOwnerOperations() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUSTOMER_SIGNUP))
                .andExpect(status().isCreated());
        var user = userRepository.findByEmail("customer@example.test").orElseThrow();
        String token = jwtService.issue(user.getUserId(), Role.CUSTOMER);

        mockMvc.perform(post("/api/v1/restaurants/1/menu")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutAcceptsEitherAuthenticatedRoleWithoutRevokingTokenServerSide() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUSTOMER_SIGNUP))
                .andExpect(status().isCreated());
        var user = userRepository.findByEmail("customer@example.test").orElseThrow();
        String token = jwtService.issue(user.getUserId(), Role.CUSTOMER);

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertEquals(Role.CUSTOMER, jwtService.parse(token).role());
    }

    @Test
    void allowedCorsOriginCanPreflightAndUnlistedOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/v1/customers/profile")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", (String) null));

        mockMvc.perform(options("/api/v1/customers/profile")
                        .header("Origin", "https://unlisted.example.test")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isForbidden());
    }
}
