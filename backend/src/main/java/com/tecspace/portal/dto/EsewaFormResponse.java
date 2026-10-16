package com.tecspace.portal.dto;

import java.util.Map;

/** What the payer's browser needs to POST to eSewa. */
public record EsewaFormResponse(String action, Map<String, String> fields) {}