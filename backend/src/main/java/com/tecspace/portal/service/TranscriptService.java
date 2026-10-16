package com.tecspace.portal.service;

import com.tecspace.portal.dto.TranscriptView;
import com.tecspace.portal.model.GradeRecord;
import com.tecspace.portal.model.Trimester;
import com.tecspace.portal.model.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** The official transcript: only trimesters that the administration has published. */
@Service
public class TranscriptService {

    private final GradeService gradeService;
    private final ResultTermService resultTerms;

    public TranscriptService(GradeService gradeService, ResultTermService resultTerms) {
        this.gradeService = gradeService;
        this.resultTerms = resultTerms;
    }

    public TranscriptView build(User student) {
        GradeRecord record = gradeService.get(student.getId());
        Set<String> published = resultTerms.publishedNames();

        List<TranscriptView.TermView> terms = new ArrayList<>();
        double weighted = 0;
        int totalCredits = 0;

        for (Trimester t : record.getTrimesters()) {
            if (!published.contains(t.getTrimesterName())) continue;
            int credits = t.getCourses().stream().mapToInt(c -> c.getCreditHours()).sum();
            terms.add(new TranscriptView.TermView(t.getTrimesterName(), t.getGpa(), credits, t.getCourses()));
            weighted += t.getGpa() * credits;
            totalCredits += credits;
        }
        double cgpa = totalCredits == 0 ? 0 : Math.round(weighted / totalCredits * 100.0) / 100.0;

        return new TranscriptView((student.getFirstName() + " " + student.getLastName()).trim(),
                student.getUniversityEmail(), student.getFaculty(), student.getSession(),
                LocalDate.now(), terms, cgpa, totalCredits);
    }
}
