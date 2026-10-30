package com.contactmanagement.service;

import com.contactmanagement.dto.ContactRequest;
import com.contactmanagement.dto.ContactResponse;
import com.contactmanagement.entity.Contact;
import com.contactmanagement.exception.DuplicateEmailException;
import com.contactmanagement.repository.ContactRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @InjectMocks
    private ContactService contactService;

    @Test
    void createContactSavesNormalizedValuesAndReturnsResponse() {
        ContactRequest request = validRequest();
        request.setFirstName("  John  ");
        request.setEmail("  John@Example.com  ");
        request.setCompany("   ");
        request.setJobTitle("  Software Engineer  ");

        when(contactRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> {
            Contact contact = invocation.getArgument(0);
            contact.setId(1L);
            return contact;
        });

        ContactResponse response = contactService.createContact(request);

        ArgumentCaptor<Contact> saved = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository).save(saved.capture());
        Contact contact = saved.getValue();
        assertThat(contact.getFirstName()).isEqualTo("John");
        assertThat(contact.getLastName()).isEqualTo("David");
        assertThat(contact.getEmail()).isEqualTo("john@example.com");
        assertThat(contact.getPhone()).isEqualTo("9876543210");
        assertThat(contact.getCompany()).isNull();
        assertThat(contact.getJobTitle()).isEqualTo("Software Engineer");
        assertThat(contact.getAddress()).isEqualTo("Chennai");
        assertThat(contact.getNotes()).isEqualTo("Business contact");

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getFirstName()).isEqualTo("John");
    }

    @Test
    void createContactRejectsDuplicateEmailBeforeSave() {
        when(contactRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> contactService.createContact(validRequest()))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("A contact with email john@example.com already exists");

        verify(contactRepository, never()).save(any());
    }

    @Test
    void createContactTurnsDatabaseUniqueViolationIntoDuplicateEmail() {
        when(contactRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(contactRepository.save(any(Contact.class)))
                .thenThrow(new DataIntegrityViolationException("contacts.email"));

        assertThatThrownBy(() -> contactService.createContact(validRequest()))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("john@example.com");
    }

    private ContactRequest validRequest() {
        ContactRequest request = new ContactRequest();
        request.setFirstName("John");
        request.setLastName("David");
        request.setEmail("john@example.com");
        request.setPhone("9876543210");
        request.setCompany("ABC Technologies");
        request.setJobTitle("Software Engineer");
        request.setAddress("Chennai");
        request.setNotes("Business contact");
        return request;
    }
}
