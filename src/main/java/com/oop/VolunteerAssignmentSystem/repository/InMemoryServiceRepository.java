package com.oop.VolunteerAssignmentSystem.repository;

import com.oop.VolunteerAssignmentSystem.model.ServiceDetails;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

@Repository
public class InMemoryServiceRepository {
    private final Map<String, ServiceDetails> services = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public InMemoryServiceRepository() {
        // Initialize 10 services as per project description
        IntStream.rangeClosed(1, 10).forEach(i -> {
            String serviceId = "service" + i;
            // Capacity between 2 and 6
            int capacity = random.nextInt(5) + 2; // (0-4) + 2 = 2-6
            services.put(serviceId, new ServiceDetails(serviceId, "Service " + i, capacity));
        });
    }

    public Collection<ServiceDetails> findAll() {
        return services.values();
    }

    public Optional<ServiceDetails> findById(String id) {
        return Optional.ofNullable(services.get(id));
    }

    public int getNumberOfServices() {
        return services.size();
    }
}