package com.contactmanagement;

import com.contactmanagement.entity.Contact;
import com.contactmanagement.repository.ContactRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:sqlite:target/contacts-test.db",
                "spring.jpa.hibernate.ddl-auto=create"
        }
)
class ContactManagementApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ContactRepository contactRepository;

    @Test
    void healthEndpointReturnsUp() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }

    @Test
    void contactCanBeSavedAndRead() {
        Contact contact = new Contact();
        contact.setFirstName("John");
        contact.setLastName("David");
        contact.setEmail("john@example.com");
        contact.setPhone("9876543210");
        contact.setCompany("ABC Technologies");
        contact.setJobTitle("Software Engineer");
        contact.setAddress("Chennai");
        contact.setNotes("Business contact");

        Contact saved = contactRepository.save(contact);

        assertThat(saved.getId()).isNotNull();
        assertThat(contactRepository.findById(saved.getId()))
                .get()
                .extracting(Contact::getEmail)
                .isEqualTo("john@example.com");
    }
}
