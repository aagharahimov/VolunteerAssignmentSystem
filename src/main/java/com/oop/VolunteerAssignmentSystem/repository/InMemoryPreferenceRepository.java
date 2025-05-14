package com.oop.VolunteerAssignmentSystem.repository;

import com.oop.VolunteerAssignmentSystem.model.Preference;
import com.oop.VolunteerAssignmentSystem.model.ServiceDetails;
import com.oop.VolunteerAssignmentSystem.model.Volunteer;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryPreferenceRepository {
    // Store Volunteer objects which contain their preferences
    private final Map<String, Volunteer> volunteers = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private final int TOTAL_VOLUNTEERS = 30;
    private final int MAX_PREFERRED_SERVICES = 5; // Ns <= 5

    public InMemoryPreferenceRepository(InMemoryServiceRepository serviceRepository) {
        List<String> allServiceIds = serviceRepository.findAll().stream()
                .map(ServiceDetails::getId)
                .collect(Collectors.toList());

        // Initialize 30 volunteers with random preferences
        for (int i = 1; i <= TOTAL_VOLUNTEERS; i++) {
            String volunteerId = "volunteer" + i;
            Map<Integer, String> preferredServices = new ConcurrentHashMap<>();
            int ns = random.nextInt(MAX_PREFERRED_SERVICES) + 1; // Ns can be 1 to 5

            // Create a mutable copy of service IDs to pick from
            List<String> availableServiceIds = new java.util.ArrayList<>(allServiceIds);

            for (int rank = 1; rank <= ns && !availableServiceIds.isEmpty(); rank++) {
                String serviceId = availableServiceIds.remove(random.nextInt(availableServiceIds.size()));
                preferredServices.put(rank, serviceId);
            }
            volunteers.put(volunteerId, new Volunteer(volunteerId, "Volunteer " + i, preferredServices));
        }
    }


    public void save(String volunteerId, Preference preference) {
        Map<Integer, String> rankedPreferences = new ConcurrentHashMap<>();
        for (int i = 0; i < preference.getRankedServiceIds().size(); i++) {
            rankedPreferences.put(i + 1, preference.getRankedServiceIds().get(i));
        }
        Volunteer volunteer = volunteers.getOrDefault(volunteerId, new Volunteer());
        volunteer.setId(volunteerId);
        // Assuming name is set elsewhere or not crucial for this part
        if (volunteer.getName() == null) volunteer.setName(volunteerId);
        volunteer.setPreferredServicesRanks(rankedPreferences);
        volunteers.put(volunteerId, volunteer);
        System.out.println("Saved preferences for " + volunteerId + ": " + rankedPreferences);
    }

    public Optional<Volunteer> findById(String volunteerId) {
        return Optional.ofNullable(volunteers.get(volunteerId));
    }

    public Map<String, Volunteer> findAllVolunteersWithPreferences() {
        return new ConcurrentHashMap<>(volunteers); // Return a copy
    }

    public int getTotalVolunteers() {
        return volunteers.size();
    }
}