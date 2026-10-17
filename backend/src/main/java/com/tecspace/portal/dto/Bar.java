package com.tecspace.portal.dto;

/** One bar in a chart. max is the full-bar value (0 if the bars should be scaled to the largest value). */
public record Bar(String label, double value, double max) {}
