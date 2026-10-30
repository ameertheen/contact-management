package com.contactmanagement.controller;

import com.contactmanagement.repository.ContactRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    private ObjectMapper objectMapper;

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

    @Test
    void createContactReturnsConflictWhenPhoneAlreadyExists() throws Exception {
        createContact("john@example.com", "John", "David", "9876543210");

        mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactJson("jane@example.com", "Jane", "Roe", "9876543210")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Phone already exists"))
                .andExpect(jsonPath("$.messages", hasItem("This mobile number is already registered.")));
    }

    @Test
    void createContactsWithDifferentPhoneNumbers() throws Exception {
        mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactJson("john@example.com", "John", "David", "9876543210")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.phone").value("9876543210"));

        mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactJson("jane@example.com", "Jane", "Roe", "9123456789")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.phone").value("9123456789"));
    }

    @Test
    void getContactsReturnsAllContacts() throws Exception {
        mockMvc.perform(get("/api/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        createContact("jane@example.com", "Jane", "David", "1112223333");
        createContact("john@example.com", "John", "David", "9876543210");

        mockMvc.perform(get("/api/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].email").value("jane@example.com"))
                .andExpect(jsonPath("$[1].email").value("john@example.com"))
                .andExpect(jsonPath("$[1].firstName").value("John"));
    }

    @Test
    void getContactByIdReturnsTheContact() throws Exception {
        long id = createContact("john@example.com", "John", "David", "9876543210");

        mockMvc.perform(get("/api/contacts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.phone").value("9876543210"));
    }

    @Test
    void getContactByIdReturnsNotFoundWhenContactDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/contacts/{id}", 999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Contact not found"))
                .andExpect(jsonPath("$.messages", hasItem("Contact with id 999 was not found")));
    }

    @Test
    void updateContactReturnsUpdatedContactAndKeepsItsOwnEmail() throws Exception {
        long id = createContact("john@example.com", "John", "David", "9876543210");

        mockMvc.perform(put("/api/contacts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactJson("JOHN@example.com", "Jonathan", "David", "5555555555")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.firstName").value("Jonathan"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.phone").value("5555555555"));
    }

    @Test
    void updateContactReturnsBadRequestWhenRequiredFieldsAreInvalid() throws Exception {
        long id = createContact("john@example.com", "John", "David", "9876543210");

        mockMvc.perform(put("/api/contacts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "",
                                  "lastName": "David",
                                  "email": "not-an-email",
                                  "phone": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages", hasItem("firstName: First name is required")))
                .andExpect(jsonPath("$.messages", hasItem("email: Email must be a valid email address")))
                .andExpect(jsonPath("$.messages", hasItem("phone: Phone number is required")));
    }

    @Test
    void updateContactReturnsConflictWhenEmailBelongsToAnotherContact() throws Exception {
        createContact("jane@example.com", "Jane", "Roe", "1112223333");
        long johnId = createContact("john@example.com", "John", "David", "9876543210");

        mockMvc.perform(put("/api/contacts/{id}", johnId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactJson("JANE@example.com", "John", "David", "9876543210")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Email already exists"))
                .andExpect(jsonPath("$.messages", hasItem(
                        "A contact with email jane@example.com already exists")));

        mockMvc.perform(get("/api/contacts/{id}", johnId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void updateContactReturnsConflictWhenPhoneBelongsToAnotherContact() throws Exception {
        createContact("jane@example.com", "Jane", "Roe", "9123456789");
        long johnId = createContact("john@example.com", "John", "David", "9876543210");

        mockMvc.perform(put("/api/contacts/{id}", johnId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactJson("john@example.com", "John", "David", "9123456789")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Phone already exists"))
                .andExpect(jsonPath("$.messages", hasItem("This mobile number is already registered.")));

        mockMvc.perform(get("/api/contacts/{id}", johnId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("9876543210"));
    }

    @Test
    void updateContactKeepsItsOwnPhone() throws Exception {
        long id = createContact("john@example.com", "John", "David", "9876543210");

        mockMvc.perform(put("/api/contacts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactJson("john@example.com", "Jonathan", "David", "9876543210")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.firstName").value("Jonathan"))
                .andExpect(jsonPath("$.phone").value("9876543210"));
    }

    @Test
    void updateContactReturnsNotFoundWhenIdDoesNotExist() throws Exception {
        mockMvc.perform(put("/api/contacts/{id}", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactJson("john@example.com", "John", "David", "9876543210")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Contact not found"))
                .andExpect(jsonPath("$.messages", hasItem("Contact with id 999 was not found")));
    }

    @Test
    void deleteContactReturnsNoContentAndRemovesTheContact() throws Exception {
        long id = createContact("john@example.com", "John", "David", "9876543210");

        mockMvc.perform(delete("/api/contacts/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/contacts/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteContactReturnsNotFoundWhenContactDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/contacts/{id}", 999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.messages", hasItem("Contact with id 999 was not found")));
    }

    @Test
    void searchContactsByNameEmailAndPhone() throws Exception {
        createContact("john@example.com", "John", "David", "9876543210");
        createContact("jane@example.com", "Jane", "Roe", "1112223333");

        mockMvc.perform(get("/api/contacts").param("search", "jOhN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].firstName").value("John"));

        mockMvc.perform(get("/api/contacts").param("search", "roe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].lastName").value("Roe"));

        mockMvc.perform(get("/api/contacts").param("search", "JANE@EXAMPLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value("jane@example.com"));

        mockMvc.perform(get("/api/contacts").param("search", "98765"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].phone").value("9876543210"));
    }

    @Test
    void searchContactsReturnsEmptyListWhenNothingMatches() throws Exception {
        createContact("john@example.com", "John", "David", "9876543210");

        mockMvc.perform(get("/api/contacts").param("search", "missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private long createContact(String email, String firstName, String lastName, String phone) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactJson(email, firstName, lastName, phone)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    private String contactJson(String email, String firstName, String phone) {
        return contactJson(email, firstName, "David", phone);
    }

    private String contactJson(String email, String firstName, String lastName, String phone) {
        return """
                {
                  "firstName": "%s",
                  "lastName": "%s",
                  "email": "%s",
                  "phone": "%s",
                  "company": "ABC Technologies",
                  "jobTitle": "Software Engineer",
                  "address": "Chennai",
                  "notes": "Business contact"
                }
                """.formatted(firstName, lastName, email, phone);
    }
}
