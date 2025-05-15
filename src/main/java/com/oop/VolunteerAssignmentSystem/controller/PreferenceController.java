package com.oop.VolunteerAssignmentSystem.controller;

import com.oop.VolunteerAssignmentSystem.model.Preference;
import com.oop.VolunteerAssignmentSystem.model.ServiceDetails;
import com.oop.VolunteerAssignmentSystem.model.Volunteer;
import com.oop.VolunteerAssignmentSystem.service.PreferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/preferences")
public class PreferenceController {
    private static final Logger logger = LoggerFactory.getLogger(PreferenceController.class);
    private final PreferenceService preferenceService;

    public PreferenceController(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping("/initial-data")
    public ResponseEntity<Map<String, Object>> getInitialData(@RequestParam String volunteerId) {
        logger.debug("Fetching initial data for volunteerId: {}", volunteerId);
        Optional<Volunteer> volunteerOptional = preferenceService.getPreferencesForVolunteer(volunteerId);
        Collection<ServiceDetails> services = preferenceService.getAllServices();

        // Create a default empty volunteer if not found, so client doesn't get null for volunteer field
        Volunteer volunteerData = volunteerOptional.orElseGet(() -> {
            Volunteer newVol = new Volunteer();
            newVol.setId(volunteerId);
            newVol.setName("New Volunteer " + volunteerId); // Or just use ID
            newVol.setPreferredServicesRanks(Map.of()); // Empty preferences
            return newVol;
        });

        return ResponseEntity.ok(Map.of("volunteer", volunteerData, "services", services));
    }


    @PostMapping("/{volunteerId}")
    public ResponseEntity<?> submitPreferences(@PathVariable String volunteerId, @RequestBody Preference preferenceDTO) {
        logger.info("Received preference submission for volunteerId: {} with preferences: {}", volunteerId, preferenceDTO.getRankedServiceIds());
        try {
            // Ensure the DTO has the volunteerId, or set it from path variable if design allows
            if (preferenceDTO.getVolunteerId() == null || !preferenceDTO.getVolunteerId().equals(volunteerId) ) {
                // Depending on your DTO design, you might set it or validate it.
                // For now, let's assume the DTO might not have it, or we prioritize path variable.
                // preferenceDTO.setVolunteerId(volunteerId); // If your DTO has this field and it's mutable.
            }
            preferenceService.submitPreferences(volunteerId, preferenceDTO);
            return ResponseEntity.ok("Preferences submitted successfully for " + volunteerId);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid preference submission for {}: {}", volunteerId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Internal error submitting preferences for {}: {}", volunteerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error submitting preferences.");
        }
    }
}
