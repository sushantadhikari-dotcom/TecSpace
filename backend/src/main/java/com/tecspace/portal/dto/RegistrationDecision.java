package com.tecspace.portal.dto;

public record RegistrationDecision(boolean approve, String note, boolean overridePrerequisites) {}
