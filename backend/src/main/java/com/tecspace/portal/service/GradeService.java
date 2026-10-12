package com.tecspace.portal.service;

import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.Course;
import com.tecspace.portal.model.GradeRecord;
import com.tecspace.portal.model.Trimester;
import com.tecspace.portal.repository.GradeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class GradeService {

    private static final Map<String, Double> GRADE_POINTS = Map.ofEntries(
            Map.entry("A", 4.0), Map.entry("A-", 3.7),
            Map.entry("B+", 3.3), Map.entry("B", 3.0), Map.entry("B-", 2.7),
            Map.entry("C+", 2.3), Map.entry("C", 2.0), Map.entry("C-", 1.7),
            Map.entry("D", 1.0), Map.entry("F", 0.0));

    private final GradeRepository gradeRepository;

    public GradeService(GradeRepository gradeRepository) {
        this.gradeRepository = gradeRepository;
    }

    public GradeRecord get(String studentId) {
        return gradeRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Grade record not found"));
    }

    public GradeRecord update(String studentId, List<Trimester> trimesters) {
        GradeRecord record = get(studentId);

        double totalPoints = 0;
        int totalCredits = 0;

        for (Trimester t : trimesters) {
            double points = 0;
            int credits = 0;
            for (Course c : t.getCourses()) {
                Double gp = GRADE_POINTS.get(c.getGrade().trim().toUpperCase());
                if (gp == null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                            "Unknown grade '" + c.getGrade() + "' (allowed: " + String.join(", ", GRADE_POINTS.keySet()) + ")");
                }
                c.setGrade(c.getGrade().trim().toUpperCase());
                points += gp * c.getCreditHours();
                credits += c.getCreditHours();
            }
            t.setGpa(credits == 0 ? 0 : round2(points / credits));
            totalPoints += points;
            totalCredits += credits;
        }

        record.setTrimesters(trimesters);
        record.setCgpa(totalCredits == 0 ? 0 : round2(totalPoints / totalCredits));
        return gradeRepository.save(record);
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
