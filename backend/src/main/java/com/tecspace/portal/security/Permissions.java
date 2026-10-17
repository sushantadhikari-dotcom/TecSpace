package com.tecspace.portal.security;

import java.util.LinkedHashMap;
import java.util.Map;

/** The permissions an admin can give to a staff member, and which one each admin endpoint needs. */
public final class Permissions {

    public static final String ANY_STAFF = "ANY_STAFF";

    public static final Map<String, String> ALL = new LinkedHashMap<>();
    static {
        ALL.put("MANAGE_STUDENTS", "View and edit student records, emergency contacts and medical information");
        ALL.put("MANAGE_COURSES", "Create and edit courses");
        ALL.put("MANAGE_TIMETABLE", "Schedule classes");
        ALL.put("MANAGE_EXAMS", "Schedule exams");
        ALL.put("MANAGE_ATTENDANCE", "Take attendance and see attendance reports");
        ALL.put("MANAGE_FINANCE", "Fees, invoices, scholarships, payments and ledgers");
        ALL.put("MANAGE_RESULTS", "Enter grades, lock and publish results");
        ALL.put("MANAGE_ADMISSIONS", "Process applications and seats");
        ALL.put("PROCESS_REQUESTS", "Handle student requests (certificates, leave)");
        ALL.put("VIEW_ANALYTICS", "See analytics and export reports");
        ALL.put("COMMUNICATE", "Send messages and post announcements");
    }

    private Permissions() {}

    /**
     * The permission a STAFF member needs for an /api/admin/ path.
     * Returns ANY_STAFF for harmless read-only lists, or null when the path is for administrators only.
     */
    public static String required(String method, String adminPath) {
        boolean read = "GET".equals(method);
        String segment = adminPath.split("/")[0];
        return switch (segment) {
            case "students" -> read ? ANY_STAFF : null;
            case "student-profiles" -> "MANAGE_STUDENTS";
            case "courses" -> read ? ANY_STAFF : "MANAGE_COURSES";
            case "timetable" -> "MANAGE_TIMETABLE";
            case "exams" -> "MANAGE_EXAMS";
            case "attendance" -> "MANAGE_ATTENDANCE";
            case "financials", "fee-structures", "invoices", "scholarships", "payment-plans", "ledger", "finance" -> "MANAGE_FINANCE";
            case "grades", "grading-scale", "result-terms" -> "MANAGE_RESULTS";
            case "applications", "seat-config" -> "MANAGE_ADMISSIONS";
            case "requests" -> "PROCESS_REQUESTS";
            case "analytics" -> "VIEW_ANALYTICS";
            case "messages", "announcements" -> "COMMUNICATE";
            default -> null;   // users, faculty-assignments, audit-logs
        };
    }
}
