package com.tecspace.portal.service;

import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.Course;
import com.tecspace.portal.model.GradeRecord;
import com.tecspace.portal.model.GradingScale;
import com.tecspace.portal.model.ResultTermState;
import com.tecspace.portal.model.Trimester;
import com.tecspace.portal.repository.GradeRepository;
import com.tecspace.portal.repository.ResultTermStateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GradeService {

    private final GradeRepository gradeRepository;
    private final GradingScaleService scales;
    private final ResultTermStateRepository termStates;

    public GradeService(GradeRepository gradeRepository, GradingScaleService scales,
                        ResultTermStateRepository termStates) {
        this.gradeRepository = gradeRepository;
        this.scales = scales;
        this.termStates = termStates;
    }

    public GradeRecord get(String studentId) {
        return gradeRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Grade record not found"));
    }

    public GradeRecord update(String studentId, List<Trimester> trimesters) {
        GradeRecord record = get(studentId);
        GradingScale scale = scales.active();

        double totalPoints = 0;
        int totalCredits = 0;

        for (Trimester t : trimesters) {
            t.setTrimesterName(t.getTrimesterName().trim());
            double points = 0;
            int credits = 0;

            for (Course c : t.getCourses()) {
                String grade;
                if (c.getPercentage() != null) {
                    double pct = c.getPercentage();
                    if (pct < 0 || pct > 100) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Marks for " + c.getCourseCode() + " must be between 0 and 100");
                    }
                    grade = scales.letterFor(scale, pct);       // the letter comes from the marks
                } else {
                    grade = c.getGrade() == null ? "" : c.getGrade().trim().toUpperCase();
                }
                Double gp = scales.pointsFor(scale, grade);
                if (gp == null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Grade '" + c.getGrade() + "' for " + c.getCourseCode()
                            + " is not on the grading scale (allowed: " + scales.allowedLetters(scale) + ")");
                }
                c.setGrade(grade);
                points += gp * c.getCreditHours();
                credits += c.getCreditHours();
            }
            t.setGpa(credits == 0 ? 0 : round2(points / credits));
            totalPoints += points;
            totalCredits += credits;
        }

        checkLocks(record, trimesters);

        record.setTrimesters(trimesters);
        record.setCgpa(totalCredits == 0 ? 0 : round2(totalPoints / totalCredits));
        return gradeRepository.save(record);
    }

    /** A locked trimester cannot be changed, added or removed until an admin unlocks it. */
    private void checkLocks(GradeRecord record, List<Trimester> incoming) {
        for (ResultTermState locked : termStates.findByLockedTrue()) {
            String name = locked.getTrimesterName();
            Optional<Trimester> before = record.getTrimesters().stream()
                    .filter(t -> t.getTrimesterName().equals(name)).findFirst();
            Optional<Trimester> after = incoming.stream()
                    .filter(t -> t.getTrimesterName().equals(name)).findFirst();

            boolean changed = before.isPresent() != after.isPresent()
                    || (before.isPresent() && !before.get().getCourses().equals(after.get().getCourses()));
            if (changed) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "\"" + name + "\" is locked. Unlock it in Results & grading before changing its grades.");
            }
        }
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
