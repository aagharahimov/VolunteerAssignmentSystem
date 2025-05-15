package com.oop.VolunteerAssignmentSystem.repository;
import com.oop.VolunteerAssignmentSystem.model.Preference;
import com.oop.VolunteerAssignmentSystem.model.ServiceDetails;
import com.oop.VolunteerAssignmentSystem.model.Volunteer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.Random;

@Repository
public class InMemoryPreferenceRepository {
    private static final Logger logger = LoggerFactory.getLogger(InMemoryPreferenceRepository.class);

    // Store Volunteer objects which contain their preferences
    // Key: volunteerId (String), Value: Volunteer object
    private final Map<String, Volunteer> volunteersWithPreferences = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private final int TOTAL_VOLUNTEERS_FOR_DUMMY_DATA = 30; // For constructor init
    private final int MAX_PREFERRED_SERVICES_FOR_DUMMY_DATA = 5; // For constructor init

    public InMemoryPreferenceRepository(InMemoryServiceRepository serviceRepository) {
        // THIS CONSTRUCTOR IS FOR INITIAL DUMMY DATA FOR TESTING/DEMO
        // Real user data will be added/updated via the save() method.
        logger.info("Initializing InMemoryPreferenceRepository with dummy data for testing.");
        List<String> allServiceIds = serviceRepository.findAll().stream()
                .map(ServiceDetails::getId)
                .collect(Collectors.toList());

        if (allServiceIds.isEmpty()) {
            logger.warn("No services available to generate dummy preferences.");
            return;
        }

        for (int i = 1; i <= TOTAL_VOLUNTEERS_FOR_DUMMY_DATA; i++) {
            String volunteerId = "volunteer" + i;
            Map<Integer, String> rankedPreferencesMap = new ConcurrentHashMap<>(); // Rank -> ServiceID
            int ns = random.nextInt(MAX_PREFERRED_SERVICES_FOR_DUMMY_DATA) + 1; // Ns is 1 to 5

            List<String> availableServiceIdsForThisVolunteer = new java.util.ArrayList<>(allServiceIds);

            for (int rank = 1; rank <= ns && !availableServiceIdsForThisVolunteer.isEmpty(); rank++) {
                String serviceId = availableServiceIdsForThisVolunteer.remove(random.nextInt(availableServiceIdsForThisVolunteer.size()));
                rankedPreferencesMap.put(rank, serviceId);
            }
            // Create a new Volunteer object or update if one was somehow pre-existing (unlikely here)
            Volunteer dummyVolunteer = new Volunteer(volunteerId, "Dummy Volunteer " + i, rankedPreferencesMap);
            volunteersWithPreferences.put(volunteerId, dummyVolunteer);
        }
        logger.info("Generated dummy preferences for {} volunteers.", volunteersWithPreferences.size());
    }

    /**
     * Saves or updates the preferences for a specific volunteer based on user submission.
     * This method handles REAL user data and will overwrite any dummy data for the given volunteerId.
     *
     * @param volunteerId The ID of the volunteer.
     * @param preferenceDTO The Preference Data Transfer Object containing ranked service IDs from the client.
     */
    public void save(String volunteerId, Preference preferenceDTO) {
        if (volunteerId == null || volunteerId.trim().isEmpty()) {
            logger.error("Attempted to save preferences with null or empty volunteerId.");
            throw new IllegalArgumentException("Volunteer ID cannot be null or empty.");
        }
        if (preferenceDTO == null || preferenceDTO.getRankedServiceIds() == null) {
            logger.error("Attempted to save null preferences or null rankedServiceIds for volunteer: {}", volunteerId);
            throw new IllegalArgumentException("Preference data or ranked service IDs cannot be null.");
        }

        // Convert the list of ranked service IDs from the DTO into the map structure used by the Volunteer model
        Map<Integer, String> newRankedPreferences = new ConcurrentHashMap<>();
        List<String> submittedRankedIds = preferenceDTO.getRankedServiceIds();
        for (int i = 0; i < submittedRankedIds.size(); i++) {
            newRankedPreferences.put(i + 1, submittedRankedIds.get(i)); // Rank is 1-based
        }

        // Get existing volunteer or create a new one if this is the first time we see this ID
        Volunteer volunteer = volunteersWithPreferences.getOrDefault(volunteerId, new Volunteer());
        volunteer.setId(volunteerId);
        // If the name is not set from client or known, use volunteerId or a default
        if (volunteer.getName() == null || volunteer.getName().startsWith("Dummy Volunteer")) {
            // You might want a way for the client to send the volunteer's name too
            volunteer.setName("Volunteer " + volunteerId); // Or just use the ID as name
        }
        volunteer.setPreferredServicesRanks(newRankedPreferences); // Set the NEW preferences

        volunteersWithPreferences.put(volunteerId, volunteer); // Store/update the volunteer object
        logger.info("Saved/Updated REAL preferences for volunteer {}: {}", volunteerId, newRankedPreferences);
    }

    public Optional<Volunteer> findById(String volunteerId) {
        return Optional.ofNullable(volunteersWithPreferences.get(volunteerId));
    }

    /**
     * Returns all volunteers and their currently stored preferences.
     * This will include a mix of dummy data (if not overwritten) and real user-submitted data.
     */
    public Map<String, Volunteer> findAllVolunteersWithPreferences() {
        // Return a copy to prevent external modification if necessary,
        // though for this app, direct access might be fine within the same process.
        return new ConcurrentHashMap<>(volunteersWithPreferences);
    }

    public int getTotalVolunteersWithPreferences() {
        return volunteersWithPreferences.size();
    }
}