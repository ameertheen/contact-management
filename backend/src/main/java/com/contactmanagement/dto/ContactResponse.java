package com.contactmanagement.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String company;
    private String jobTitle;
    private String address;
    private String notes;
}
