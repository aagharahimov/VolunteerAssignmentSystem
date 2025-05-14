package com.oop.VolunteerAssignmentSystem.controller;

import com.oop.VolunteerAssignmentSystem.model.Preference;
import com.oop.VolunteerAssignmentSystem.model.ServiceDetails;
import com.oop.VolunteerAssignmentSystem.model.Volunteer;
import com.oop.VolunteerAssignmentSystem.service.PreferenceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("/api/preferences")
public class PreferenceController {
    private final PreferenceService preferenceService;

    public PreferenceController(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    // Endpoint for client to fetch initial data (services, existing volunteer prefs)
    @GetMapping("/initial-data")
    public ResponseEntity<Map<String, Object>> getInitialData(@RequestParam String volunteerId) {
        // You'll need to implement logic in PreferenceService to fetch specific volunteer's prefs
        // and all available services
        Map<String, Volunteer> volunteers = preferenceService.getAllVolunteersWithPreferences();
        Volunteer currentVolunteer = volunteers.get(volunteerId); // Can be null if new
        Collection<ServiceDetails> services = preferenceService.getAllServices();
        return ResponseEntity.ok(Map.of("volunteer", currentVolunteer != null ? currentVolunteer : new Volunteer(volunteerId, "New Volunteer", Map.of()), "services", services));
    }

    @PostMapping("/{volunteerId}")
    public ResponseEntity<?> submitPreferences(@PathVariable String volunteerId, @RequestBody Preference preference) {
        try {
            preferenceService.submitPreferences(volunteerId, preference);
            return ResponseEntity.ok("Preferences submitted successfully for " + volunteerId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error submitting preferences.");
        }
    }
}
