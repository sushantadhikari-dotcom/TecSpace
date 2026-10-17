package com.tecspace.portal.dto;

import java.time.LocalDate;

public record CertificateView(String referenceNo, String title, String studentName, String faculty,
                              String session, String text, LocalDate issuedOn) {}
