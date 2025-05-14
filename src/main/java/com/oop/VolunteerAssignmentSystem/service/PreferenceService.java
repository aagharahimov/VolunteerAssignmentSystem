package com.oop.VolunteerAssignmentSystem.service;

import com.oop.VolunteerAssignmentSystem.model.Preference;
import com.oop.VolunteerAssignmentSystem.model.ServiceDetails;
import com.oop.VolunteerAssignmentSystem.model.Volunteer;
import com.oop.VolunteerAssignmentSystem.repository.InMemoryPreferenceRepository;
import com.oop.VolunteerAssignmentSystem.repository.InMemoryServiceRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

@Service
public class PreferenceService {
    private final InMemoryPreferenceRepository preferenceRepository;
    private final InMemoryServiceRepository serviceRepository;

    public PreferenceService(InMemoryPreferenceRepository preferenceRepository, InMemoryServiceRepository serviceRepository) {
        this.preferenceRepository = preferenceRepository;
        this.serviceRepository = serviceRepository;
    }

    public void submitPreferences(String volunteerId, Preference preference) {
        // Basic validation: ensure services exist, volunteerId is present
        if (volunteerId == null || volunteerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Volunteer ID cannot be empty.");
        }
        if (preference.getRankedServiceIds() == null || preference.getRankedServiceIds().isEmpty()) {
            throw new IllegalArgumentException("Preferences cannot be empty.");
        }
        if (preference.getRankedServiceIds().size() > 5) { // Ns <= 5
            throw new IllegalArgumentException("Cannot prefer more than 5 services.");
        }
        for (String serviceId : preference.getRankedServiceIds()) {
            if (!serviceRepository.findById(serviceId).isPresent()) {
                throw new IllegalArgumentException("Service ID " + serviceId + " does not exist.");
            }
        }
        preferenceRepository.save(volunteerId, preference);
    }

    public Map<String, Volunteer> getAllVolunteersWithPreferences() {
        return preferenceRepository.findAllVolunteersWithPreferences();
    }

    public Collection<ServiceDetails> getAllServices() {
        return serviceRepository.findAll();
    }
}
