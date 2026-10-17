package com.tecspace.portal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EnrollApplicantRequest(@NotBlank @Email String universityEmail) {}
