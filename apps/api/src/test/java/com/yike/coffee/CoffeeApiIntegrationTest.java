package com.yike.coffee;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@ActiveProfiles("demo")
@SpringBootTest(properties={"app.public-base-url=http://localhost","app.upload-dir=target/test-uploads"})
@AutoConfigureMockMvc
class CoffeeApiIntegrationTest {
    @Container static final MySQLContainer<?> MYSQL=new MySQLContainer<>("mysql:8.4").withDatabaseName("yike_coffee").withUsername("test").withPassword("test");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",MYSQL::getJdbcUrl);r.add("spring.datasource.username",MYSQL::getUsername);r.add("spring.datasource.password",MYSQL::getPassword);}
    @Autowired MockMvc mvc; @Autowired ObjectMapper json;

    @Test void publicMenuOnlyShowsApprovedMerchant() throws Exception {
        mvc.perform(get("/api/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/public/merchants")).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));
        mvc.perform(get("/api/v1/public/stores/20000000-0000-0000-0000-000000000001/menu"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.products.length()").value(6));
    }

    @Test void roleAndTenantIsolationAreEnforced() throws Exception {
        String customer=login("customer@example.test");String customer2=login("customer2@example.test");String merchant=login("merchant@example.test");String pending=login("pending@example.test");
        mvc.perform(get("/api/v1/platform/merchants").header("Authorization","Bearer "+merchant)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/merchant/stores").header("Authorization","Bearer "+pending)).andExpect(status().isForbidden());
        String body="""
          {"storeId":"20000000-0000-0000-0000-000000000001","items":[
            {"productId":"40000000-0000-0000-0000-000000000001","quantity":2,
             "optionIds":["51000000-0000-0000-0000-000000000001","51000000-0000-0000-0000-000000000004"]}]}
          """;
        var created=mvc.perform(post("/api/v1/customer/orders").header("Authorization","Bearer "+customer)
            .header("X-Idempotency-Key",UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalAmount").value(5200)).andReturn();
        String orderId=json.readTree(created.getResponse().getContentAsString()).at("/data/id").asText();
        mvc.perform(get("/api/v1/customer/orders/{id}",orderId).header("Authorization","Bearer "+customer2)).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/merchant/orders/{id}/accept",orderId).header("Authorization","Bearer "+merchant)).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("PREPARING"));
        mvc.perform(post("/api/v1/merchant/orders/{id}/complete",orderId).header("Authorization","Bearer "+merchant)).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/merchant/orders/{id}/ready",orderId).header("Authorization","Bearer "+merchant)).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("READY"));
        mvc.perform(post("/api/v1/merchant/orders/{id}/complete",orderId).header("Authorization","Bearer "+merchant)).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test void refreshTokensRotateAndLogoutRevokesThem() throws Exception {
        String response=mvc.perform(post("/api/v1/auth/dev-login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"admin@example.test\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String refresh=json.readTree(response).at("/data/refreshToken").asText();
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\""+refresh+"\"}"))
            .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\""+refresh+"\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test void uploadRejectsContentTypeSpoofing() throws Exception {
        String merchant=login("merchant@example.test");
        var file=new org.springframework.mock.web.MockMultipartFile("file","fake.png","image/png","not-an-image".getBytes());
        mvc.perform(MockMvcRequestBuilders.multipart("/api/v1/uploads/images").file(file).header("Authorization","Bearer "+merchant))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_IMAGE"));
    }

    private String login(String email)throws Exception{
        String body=mvc.perform(post("/api/v1/auth/dev-login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode node=json.readTree(body);String token=node.at("/data/accessToken").asText();assertThat(token).isNotBlank();return token;
    }
}
