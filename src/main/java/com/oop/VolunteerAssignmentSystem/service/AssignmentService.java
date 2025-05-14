package com.oop.VolunteerAssignmentSystem.service;

import com.oop.VolunteerAssignmentSystem.model.AssignmentResult;
import com.oop.VolunteerAssignmentSystem.model.ServiceDetails;
import com.oop.VolunteerAssignmentSystem.model.Volunteer;
import com.oop.VolunteerAssignmentSystem.repository.InMemoryPreferenceRepository;
import com.oop.VolunteerAssignmentSystem.repository.InMemoryServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class AssignmentService {
    private static final Logger logger = LoggerFactory.getLogger(AssignmentService.class);

    private final GeneticAlgorithmService geneticAlgorithmService;
    private final InMemoryPreferenceRepository preferenceRepository;
    private final InMemoryServiceRepository serviceRepository;
    private final SimpMessagingTemplate messagingTemplate; // For WebSocket

    public AssignmentService(GeneticAlgorithmService geneticAlgorithmService,
                             InMemoryPreferenceRepository preferenceRepository,
                             InMemoryServiceRepository serviceRepository,
                             SimpMessagingTemplate messagingTemplate) {
        this.geneticAlgorithmService = geneticAlgorithmService;
        this.preferenceRepository = preferenceRepository;
        this.serviceRepository = serviceRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public void triggerOptimization() {
        logger.info("Optimization trigger received.");
        // Run GA in a separate thread to not block the request
        CompletableFuture.runAsync(() -> {
            try {
                logger.info("Starting Genetic Algorithm...");
                Map<String, Volunteer> volunteers = preferenceRepository.findAllVolunteersWithPreferences();
                List<ServiceDetails> services = serviceRepository.findAll().stream().collect(Collectors.toList());

                if (volunteers.isEmpty()) {
                    logger.warn("No volunteers with preferences to assign.");
                    AssignmentResult emptyResult = new AssignmentResult();
                    emptyResult.setMessage("No volunteers with preferences available for assignment.");
                    messagingTemplate.convertAndSend("/topic/assignments", emptyResult);
                    return;
                }
                if (services.isEmpty()) {
                    logger.warn("No services available for assignment.");
                    AssignmentResult emptyResult = new AssignmentResult();
                    emptyResult.setMessage("No services available for assignment.");
                    messagingTemplate.convertAndSend("/topic/assignments", emptyResult);
                    return;
                }


                AssignmentResult result = geneticAlgorithmService.run(volunteers, services);
                logger.info("Genetic Algorithm finished. Total cost: {}", result.getTotalCost());

                // Broadcast the result via WebSocket
                messagingTemplate.convertAndSend("/topic/assignments", result);
            } catch (Exception e) {
                logger.error("Error during optimization: ", e);
                AssignmentResult errorResult = new AssignmentResult();
                errorResult.setMessage("Error during optimization: " + e.getMessage());
                messagingTemplate.convertAndSend("/topic/assignments", errorResult);
            }
        }).exceptionally(ex -> {
            logger.error("Unhandled exception in async optimization: ", ex);
            AssignmentResult errorResult = new AssignmentResult();
            errorResult.setMessage("Critical error during optimization process: " + ex.getMessage());
            messagingTemplate.convertAndSend("/topic/assignments", errorResult);
            return null;
        });
    }
}

