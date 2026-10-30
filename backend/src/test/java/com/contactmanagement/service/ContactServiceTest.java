package com.contactmanagement.service;

import com.contactmanagement.dto.ContactRequest;
import com.contactmanagement.dto.ContactResponse;
import com.contactmanagement.entity.Contact;
import com.contactmanagement.exception.ContactNotFoundException;
import com.contactmanagement.exception.DuplicateEmailException;
import com.contactmanagement.exception.DuplicatePhoneException;
import com.contactmanagement.repository.ContactRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    void createContactRejectsDuplicatePhoneBeforeSave() {
        when(contactRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(contactRepository.existsByPhone("9876543210")).thenReturn(true);

        assertThatThrownBy(() -> contactService.createContact(validRequest()))
                .isInstanceOf(DuplicatePhoneException.class)
                .hasMessage("This mobile number is already registered.");

        verify(contactRepository, never()).save(any());
    }

    @Test
    void createContactAllowsDifferentPhoneNumbers() {
        when(contactRepository.existsByEmail(anyString())).thenReturn(false);
        when(contactRepository.existsByPhone(anyString())).thenReturn(false);
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContactRequest first = validRequest();
        ContactRequest second = validRequest();
        second.setEmail("jane@example.com");
        second.setPhone("9123456789");

        contactService.createContact(first);
        contactService.createContact(second);

        ArgumentCaptor<Contact> saved = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(Contact::getPhone)
                .containsExactly("9876543210", "9123456789");
    }

    @Test
    void createContactTurnsDatabasePhoneViolationIntoDuplicatePhone() {
        when(contactRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(contactRepository.existsByPhone("9876543210")).thenReturn(false);
        when(contactRepository.save(any(Contact.class)))
                .thenThrow(new DataIntegrityViolationException("UNIQUE constraint failed: contacts.phone"));

        assertThatThrownBy(() -> contactService.createContact(validRequest()))
                .isInstanceOf(DuplicatePhoneException.class)
                .hasMessage("This mobile number is already registered.");
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

    @Test
    void getContactsReturnsEveryContactWhenSearchIsBlank() {
        when(contactRepository.findAllByOrderByIdAsc()).thenReturn(List.of(
                contact(1L, "Jane", "jane@example.com", "1111111111"),
                contact(2L, "John", "john@example.com", "9876543210")));

        List<ContactResponse> responses = contactService.getContacts("   ");

        assertThat(responses).extracting(ContactResponse::getEmail)
                .containsExactly("jane@example.com", "john@example.com");
        verify(contactRepository, never()).search(any());
    }

    @Test
    void getContactReturnsTheSavedContact() {
        when(contactRepository.findById(1L)).thenReturn(Optional.of(
                contact(1L, "John", "john@example.com", "9876543210")));

        ContactResponse response = contactService.getContact(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void getContactThrowsWhenIdDoesNotExist() {
        when(contactRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contactService.getContact(99L))
                .isInstanceOf(ContactNotFoundException.class)
                .hasMessage("Contact with id 99 was not found");
    }

    @Test
    void updateContactKeepsItsOwnEmailAndSavesNewValues() {
        when(contactRepository.findById(1L)).thenReturn(Optional.of(
                contact(1L, "John", "john@example.com", "9876543210")));
        when(contactRepository.existsByEmailAndIdNot("john@example.com", 1L)).thenReturn(false);
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContactRequest request = validRequest();
        request.setFirstName("  Jonathan  ");
        request.setEmail("  JOHN@example.com  ");
        request.setCompany("   ");

        ContactResponse response = contactService.updateContact(1L, request);

        ArgumentCaptor<Contact> saved = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo(1L);
        assertThat(saved.getValue().getFirstName()).isEqualTo("Jonathan");
        assertThat(saved.getValue().getEmail()).isEqualTo("john@example.com");
        assertThat(saved.getValue().getCompany()).isNull();
        assertThat(response.getFirstName()).isEqualTo("Jonathan");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        verify(contactRepository, never()).existsByEmail(any());
    }

    @Test
    void updateContactKeepsItsOwnPhone() {
        when(contactRepository.findById(1L)).thenReturn(Optional.of(
                contact(1L, "John", "john@example.com", "9876543210")));
        when(contactRepository.existsByEmailAndIdNot("john@example.com", 1L)).thenReturn(false);
        when(contactRepository.existsByPhoneAndIdNot("9876543210", 1L)).thenReturn(false);
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContactRequest request = validRequest();
        request.setFirstName("Jonathan");

        ContactResponse response = contactService.updateContact(1L, request);

        assertThat(response.getPhone()).isEqualTo("9876543210");
        assertThat(response.getFirstName()).isEqualTo("Jonathan");
        verify(contactRepository).existsByPhoneAndIdNot("9876543210", 1L);
    }

    @Test
    void updateContactRejectsPhoneUsedByAnotherContact() {
        when(contactRepository.findById(1L)).thenReturn(Optional.of(
                contact(1L, "John", "john@example.com", "9876543210")));
        when(contactRepository.existsByEmailAndIdNot("john@example.com", 1L)).thenReturn(false);
        when(contactRepository.existsByPhoneAndIdNot("9123456789", 1L)).thenReturn(true);

        ContactRequest request = validRequest();
        request.setPhone("9123456789");

        assertThatThrownBy(() -> contactService.updateContact(1L, request))
                .isInstanceOf(DuplicatePhoneException.class)
                .hasMessage("This mobile number is already registered.");

        verify(contactRepository, never()).save(any());
    }

    @Test
    void updateContactRejectsEmailUsedByAnotherContact() {
        when(contactRepository.findById(1L)).thenReturn(Optional.of(
                contact(1L, "John", "john@example.com", "9876543210")));
        when(contactRepository.existsByEmailAndIdNot("jane@example.com", 1L)).thenReturn(true);

        ContactRequest request = validRequest();
        request.setEmail("jane@example.com");

        assertThatThrownBy(() -> contactService.updateContact(1L, request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("A contact with email jane@example.com already exists");

        verify(contactRepository, never()).save(any());
    }

    @Test
    void updateContactThrowsWhenIdDoesNotExist() {
        when(contactRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contactService.updateContact(99L, validRequest()))
                .isInstanceOf(ContactNotFoundException.class)
                .hasMessage("Contact with id 99 was not found");

        verify(contactRepository, never()).save(any());
    }

    @Test
    void deleteContactDeletesAnExistingContact() {
        when(contactRepository.existsById(1L)).thenReturn(true);

        contactService.deleteContact(1L);

        verify(contactRepository).deleteById(1L);
    }

    @Test
    void deleteContactThrowsWhenIdDoesNotExist() {
        when(contactRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> contactService.deleteContact(99L))
                .isInstanceOf(ContactNotFoundException.class)
                .hasMessage("Contact with id 99 was not found");

        verify(contactRepository, never()).deleteById(any());
    }

    @Test
    void searchContactsReturnsMatchesForTheTrimmedTerm() {
        when(contactRepository.search("john")).thenReturn(List.of(
                contact(1L, "John", "john@example.com", "9876543210")));

        List<ContactResponse> responses = contactService.getContacts("  john  ");

        assertThat(responses).extracting(ContactResponse::getPhone).containsExactly("9876543210");
    }

    @Test
    void searchContactsReturnsEmptyListWhenNothingMatches() {
        when(contactRepository.search("missing")).thenReturn(List.of());

        assertThat(contactService.getContacts("missing")).isEmpty();
    }

    private Contact contact(Long id, String firstName, String email, String phone) {
        Contact contact = new Contact();
        contact.setId(id);
        contact.setFirstName(firstName);
        contact.setLastName("David");
        contact.setEmail(email);
        contact.setPhone(phone);
        return contact;
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
