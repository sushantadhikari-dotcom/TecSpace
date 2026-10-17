package com.tecspace.portal.controller;

import com.tecspace.portal.dto.AnalyticsView;
import com.tecspace.portal.service.AnalyticsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/analytics")
public class AnalyticsController {

    private final AnalyticsService analytics;

    public AnalyticsController(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    @GetMapping
    public AnalyticsView overview() {
        return analytics.overview();
    }

    /** kind: students, enrollments, attendance, finance or grades. Returns rows; the browser turns them into a CSV. */
    @GetMapping("/export/{kind}")
    public List<List<String>> export(@PathVariable String kind) {
        return analytics.export(kind);
    }
}
