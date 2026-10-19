package com.tecspace.portal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on @Scheduled jobs (the daily reminders). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
