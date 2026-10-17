package com.tecspace.portal.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Used both to read and to save a student's profile. */
public record ProfileData(
        @Past LocalDate dateOfBirth,
        @Pattern(regexp = "^(MALE|FEMALE|OTHER)?$", message = "must be MALE, FEMALE or OTHER") String gender,
        @Size(max = 300) String address,
        @Size(max = 100) String nationality,
        @Size(max = 5, message = "you can add at most 5 contacts") @Valid List<Contact> emergencyContacts,
        @Valid Medical medical
) {
    public record Contact(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 50) String relationship,
            @NotBlank @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "must be a valid phone number") String phone
    ) {}

    public record Medical(
            @Size(max = 20) String bloodGroup,
            @Size(max = 1000) String allergies,
            @Size(max = 1000) String conditions,
            @Size(max = 1000) String medications,
            @Size(max = 1000) String notes
    ) {}
}
