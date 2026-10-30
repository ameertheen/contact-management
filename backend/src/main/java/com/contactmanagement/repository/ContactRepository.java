package com.contactmanagement.repository;

import com.contactmanagement.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    boolean existsByEmail(String email);
}
