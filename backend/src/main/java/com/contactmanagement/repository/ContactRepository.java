package com.contactmanagement.repository;

import com.contactmanagement.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    List<Contact> findAllByOrderByIdAsc();

    @Query("""
            SELECT c FROM Contact c
            WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(c.email) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :term, '%'))
            ORDER BY c.id
            """)
    List<Contact> search(@Param("term") String term);
}
