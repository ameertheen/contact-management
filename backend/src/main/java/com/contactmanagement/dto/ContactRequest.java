package com.contactmanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must be at most 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must be at most 100 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email must be at most 255 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Size(max = 30, message = "Phone number must be at most 30 characters")
    private String phone;

    @Size(max = 150, message = "Company must be at most 150 characters")
    private String company;

    @Size(max = 150, message = "Job title must be at most 150 characters")
    private String jobTitle;

    @Size(max = 255, message = "Address must be at most 255 characters")
    private String address;

    @Size(max = 2000, message = "Notes must be at most 2000 characters")
    private String notes;
}
