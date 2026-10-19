package com.tecspace.portal.dto;

import com.tecspace.portal.model.CalendarEvent;

import java.util.List;

public record CalendarView(List<CalendarEvent> events, boolean registrationOpen, String registrationMessage) {}
