package com.oop.VolunteerAssignmentSystem.service;

import com.oop.VolunteerAssignmentSystem.model.Preference;
import com.oop.VolunteerAssignmentSystem.model.ServiceDetails;
import com.oop.VolunteerAssignmentSystem.model.Volunteer;
import com.oop.VolunteerAssignmentSystem.repository.InMemoryPreferenceRepository;
import com.oop.VolunteerAssignmentSystem.repository.InMemoryServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PreferenceService {
    private static final Logger logger = LoggerFactory.getLogger(PreferenceService.class);
    private final InMemoryPreferenceRepository preferenceRepository;
    private final InMemoryServiceRepository serviceRepository;

    public PreferenceService(InMemoryPreferenceRepository preferenceRepository, InMemoryServiceRepository serviceRepository) {
        this.preferenceRepository = preferenceRepository;
        this.serviceRepository = serviceRepository;
    }

    public void submitPreferences(String volunteerId, Preference preferenceDTO) {
        logger.info("Processing preference submission for volunteer: {}", volunteerId);
        // Basic validation (can be expanded)
        if (volunteerId == null || volunteerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Volunteer ID cannot be empty.");
        }
        if (preferenceDTO.getRankedServiceIds() == null || preferenceDTO.getRankedServiceIds().isEmpty()) {
            throw new IllegalArgumentException("Preferences (rankedServiceIds) cannot be empty for volunteer " + volunteerId);
        }
        if (preferenceDTO.getRankedServiceIds().size() > 5) { // Ns <= 5
            throw new IllegalArgumentException("Volunteer " + volunteerId + " cannot prefer more than 5 services.");
        }
        for (String serviceId : preferenceDTO.getRankedServiceIds()) {
            if (!serviceRepository.findById(serviceId).isPresent()) {
                throw new IllegalArgumentException("Service ID " + serviceId + " (preferred by " + volunteerId + ") does not exist.");
            }
        }

        // Call the repository's save method, which handles the actual storage
        preferenceRepository.save(volunteerId, preferenceDTO);
        logger.info("Successfully submitted preferences for volunteer: {}", volunteerId);
    }

    public Map<String, Volunteer> getAllVolunteersWithPreferences() {
        return preferenceRepository.findAllVolunteersWithPreferences();
    }

    public Collection<ServiceDetails> getAllServices() {
        return serviceRepository.findAll();
    }

    public Optional<Volunteer> getPreferencesForVolunteer(String volunteerId) {
        return preferenceRepository.findById(volunteerId);
    }
}
