package com.tecspace.portal.dto;

/** seats is null when no limit has been set for that faculty and session. */
public record SeatStatus(String faculty, String session, Integer seats, int allocated, int waitlisted) {}
