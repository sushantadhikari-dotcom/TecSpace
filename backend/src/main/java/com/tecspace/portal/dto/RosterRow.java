package com.tecspace.portal.dto;

/** One student on a daily register. status is null if attendance was not taken yet for that day. */
public record RosterRow(String studentId, String name, String status) {}
