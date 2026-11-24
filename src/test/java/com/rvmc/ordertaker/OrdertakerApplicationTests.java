package com.rvmc.ordertaker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ordertaker;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class OrdertakerApplicationTests {
    @Autowired private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void statelessSecurity_shouldAllowAnonymousRequestsWithoutCreatingSession() throws Exception {
        mockMvc.perform(get("/api/v1/restaurants"))
                .andExpect(status().isOk())
                .andExpect(result -> assertNull(result.getRequest().getSession(false)));
    }

    @Test
    void protectedCustomerRequest_shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/customers/cart"))
                .andExpect(status().isUnauthorized());
    }
}
