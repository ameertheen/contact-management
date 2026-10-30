package com.contactmanagement.controller;

import com.contactmanagement.dto.ContactRequest;
import com.contactmanagement.dto.ContactResponse;
import com.contactmanagement.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    public ResponseEntity<ContactResponse> createContact(@Valid @RequestBody ContactRequest request) {
        ContactResponse created = contactService.createContact(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<List<ContactResponse>> getContacts(
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(contactService.getContacts(search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContactResponse> getContact(@PathVariable Long id) {
        return ResponseEntity.ok(contactService.getContact(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContactResponse> updateContact(
            @PathVariable Long id,
            @Valid @RequestBody ContactRequest request) {
        return ResponseEntity.ok(contactService.updateContact(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContact(@PathVariable Long id) {
        contactService.deleteContact(id);
        return ResponseEntity.noContent().build();
    }
}
