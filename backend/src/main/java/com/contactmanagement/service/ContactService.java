package com.contactmanagement.service;

import com.contactmanagement.dto.ContactRequest;
import com.contactmanagement.dto.ContactResponse;
import com.contactmanagement.entity.Contact;
import com.contactmanagement.exception.DuplicateEmailException;
import com.contactmanagement.repository.ContactRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class ContactService {

    private final ContactRepository contactRepository;

    public ContactService(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    @Transactional
    public ContactResponse createContact(ContactRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (contactRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        Contact contact = new Contact();
        contact.setFirstName(request.getFirstName().trim());
        contact.setLastName(request.getLastName().trim());
        contact.setEmail(email);
        contact.setPhone(request.getPhone().trim());
        contact.setCompany(trimToNull(request.getCompany()));
        contact.setJobTitle(trimToNull(request.getJobTitle()));
        contact.setAddress(trimToNull(request.getAddress()));
        contact.setNotes(trimToNull(request.getNotes()));

        try {
            return toResponse(contactRepository.save(contact));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException(email);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ContactResponse toResponse(Contact contact) {
        ContactResponse response = new ContactResponse();
        response.setId(contact.getId());
        response.setFirstName(contact.getFirstName());
        response.setLastName(contact.getLastName());
        response.setEmail(contact.getEmail());
        response.setPhone(contact.getPhone());
        response.setCompany(contact.getCompany());
        response.setJobTitle(contact.getJobTitle());
        response.setAddress(contact.getAddress());
        response.setNotes(contact.getNotes());
        return response;
    }
}
