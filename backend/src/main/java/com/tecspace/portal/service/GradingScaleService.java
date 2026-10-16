package com.tecspace.portal.service;

import com.tecspace.portal.dto.GradingScaleRequest;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.GradeBand;
import com.tecspace.portal.model.GradingScale;
import com.tecspace.portal.repository.GradingScaleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GradingScaleService {

    private final GradingScaleRepository repo;

    public GradingScaleService(GradingScaleRepository repo) {
        this.repo = repo;
    }

    /** The institution's scale. A standard 4.0 scale is created the first time it is needed. */
    public synchronized GradingScale active() {
        return repo.findAll().stream().findFirst().orElseGet(() -> {
            GradingScale s = new GradingScale();
            s.setName("Standard 4.0");
            s.setBands(new ArrayList<>(List.of(
                    new GradeBand("A", 90, 4.0), new GradeBand("A-", 85, 3.7),
                    new GradeBand("B+", 80, 3.3), new GradeBand("B", 75, 3.0), new GradeBand("B-", 70, 2.7),
                    new GradeBand("C+", 65, 2.3), new GradeBand("C", 60, 2.0), new GradeBand("C-", 55, 1.7),
                    new GradeBand("D", 50, 1.0), new GradeBand("F", 0, 0.0))));
            return repo.save(s);
        });
    }

    public GradingScale update(GradingScaleRequest r) {
        List<GradeBand> bands = new ArrayList<>();
        Set<String> letters = new HashSet<>();
        Set<Double> mins = new HashSet<>();
        for (GradeBand b : r.bands()) {
            String letter = b.getLetter() == null ? "" : b.getLetter().trim().toUpperCase();
            if (letter.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Every grade needs a letter");
            if (b.getMinPercent() < 0 || b.getMinPercent() > 100) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Minimum percentage must be between 0 and 100");
            }
            if (b.getGradePoint() < 0 || b.getGradePoint() > 10) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Grade points must be between 0 and 10");
            }
            if (!letters.add(letter)) throw new ApiException(HttpStatus.BAD_REQUEST, "Grade " + letter + " appears twice");
            if (!mins.add(b.getMinPercent())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Two grades start at " + b.getMinPercent() + "%");
            }
            bands.add(new GradeBand(letter, b.getMinPercent(), b.getGradePoint()));
        }
        if (!mins.contains(0.0)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "One grade must start at 0% so every mark gets a grade");
        }
        bands.sort(Comparator.comparingDouble(GradeBand::getMinPercent).reversed());

        GradingScale s = active();
        s.setName(r.name().trim());
        s.setBands(bands);
        return repo.save(s);
    }

    public String letterFor(GradingScale scale, double percent) {
        return scale.getBands().stream()
                .sorted(Comparator.comparingDouble(GradeBand::getMinPercent).reversed())
                .filter(b -> percent >= b.getMinPercent())
                .map(GradeBand::getLetter)
                .findFirst()
                .orElse(scale.getBands().get(scale.getBands().size() - 1).getLetter());
    }

    /** Null if the letter is not on the scale. */
    public Double pointsFor(GradingScale scale, String letter) {
        return scale.getBands().stream()
                .filter(b -> b.getLetter().equalsIgnoreCase(letter))
                .map(GradeBand::getGradePoint)
                .findFirst().orElse(null);
    }

    public String allowedLetters(GradingScale scale) {
        return String.join(", ", scale.getBands().stream().map(GradeBand::getLetter).toList());
    }
}
