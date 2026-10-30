package com.contactmanagement.controller;

import com.contactmanagement.repository.ContactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:target/contacts-controller-test.db",
        "spring.jpa.hibernate.ddl-auto=create"
})
@AutoConfigureMockMvc
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContactRepository contactRepository;

    @BeforeEach
    void cleanDatabase() {
        contactRepository.deleteAll();
    }

    @Test
    void createContactReturnsCreatedContact() throws Exception {
        mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactJson("john@example.com", "John", "9876543210")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/contacts/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("David"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.phone").value("9876543210"))
                .andExpect(jsonPath("$.company").value("ABC Technologies"))
                .andExpect(jsonPath("$.jobTitle").value("Software Engineer"))
                .andExpect(jsonPath("$.address").value("Chennai"))
                .andExpect(jsonPath("$.notes").value("Business contact"));
    }

    @Test
    void createContactReturnsBadRequestWhenRequiredFieldsAreInvalid() throws Exception {
        String body = """
                {
                  "firstName": " ",
                  "lastName": "",
                  "email": "not-an-email",
                  "phone": ""
                }
                """;

        mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.messages", hasItem("firstName: First name is required")))
                .andExpect(jsonPath("$.messages", hasItem("lastName: Last name is required")))
                .andExpect(jsonPath("$.messages", hasItem("email: Email must be a valid email address")))
                .andExpect(jsonPath("$.messages", hasItem("phone: Phone number is required")));
    }

    @Test
    void createContactReturnsBadRequestWhenBodyIsNotJson() throws Exception {
        mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages", hasItem("Request body must be valid JSON")));
    }

    @Test
    void createContactReturnsConflictWhenEmailAlreadyExists() throws Exception {
        String body = contactJson("john@example.com", "John", "9876543210");

        mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactJson("JOHN@example.com", "Jane", "1234567890")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Email already exists"))
                .andExpect(jsonPath("$.messages", hasItem(
                        "A contact with email john@example.com already exists")));
    }

    private String contactJson(String email, String firstName, String phone) {
        return """
                {
                  "firstName": "%s",
                  "lastName": "David",
                  "email": "%s",
                  "phone": "%s",
                  "company": "ABC Technologies",
                  "jobTitle": "Software Engineer",
                  "address": "Chennai",
                  "notes": "Business contact"
                }
                """.formatted(firstName, email, phone);
    }
}
