package com.tecspace.portal.dto;

import com.tecspace.portal.model.ApplicationDocument;

import java.time.Instant;
import java.util.List;

public record TrackView(String trackingCode, String name, String faculty, String session, String status,
                        String remarks, List<ApplicationDocument> documents, Instant submittedAt) {}
