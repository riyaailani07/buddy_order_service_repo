package com.example.orderservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for the order REST API.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {

    private static final String BASE = "/api/v1/orders";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String VALID_BODY = """
            {
              "customerName": "Riya Ailani",
              "customerEmail": "riya@example.com",
              "items": [
                {"productCode": "SKU-1", "productName": "Mechanical Keyboard", "quantity": 2, "unitPrice": 2499.00},
                {"productCode": "SKU-2", "productName": "Mouse Pad", "quantity": 1, "unitPrice": 499.00}
              ]
            }
            """;

    private long createOrder() throws Exception {
        MvcResult result = mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void createOrder_returns201WithComputedTotal() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.orderNumber").value(org.hamcrest.Matchers.startsWith("ORD-")))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.totalAmount").value(5497.00));
    }

    @Test
    void createOrder_withInvalidPayload_returns400() throws Exception {
        String invalid = """
                {"customerName": "", "customerEmail": "not-an-email", "items": []}
                """;
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.customerName", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.customerEmail", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.items", notNullValue()));
    }

    @Test
    void getOrderById_returnsOrder() throws Exception {
        long id = createOrder();
        mockMvc.perform(get(BASE + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.customerName").value("Riya Ailani"));
    }

    @Test
    void getOrderById_whenMissing_returns404() throws Exception {
        mockMvc.perform(get(BASE + "/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listOrders_returnsCollection() throws Exception {
        createOrder();
        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(org.hamcrest.Matchers.greaterThanOrEqualTo(1))));
    }

    @Test
    void updateOrder_recalculatesTotal() throws Exception {
        long id = createOrder();
        String updated = """
                {
                  "customerName": "Riya Ailani",
                  "customerEmail": "riya@example.com",
                  "items": [
                    {"productCode": "SKU-9", "productName": "Monitor", "quantity": 1, "unitPrice": 12000.00}
                  ]
                }
                """;
        mockMvc.perform(put(BASE + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updated))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.totalAmount").value(12000.00));
    }

    @Test
    void updateStatus_followsLifecycle() throws Exception {
        long id = createOrder();
        mockMvc.perform(patch(BASE + "/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void updateStatus_invalidTransition_returns422() throws Exception {
        long id = createOrder();
        mockMvc.perform(patch(BASE + "/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deleteOrder_returns204Then404() throws Exception {
        long id = createOrder();
        mockMvc.perform(delete(BASE + "/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(BASE + "/{id}", id))
                .andExpect(status().isNotFound());
    }
}
