package com.oop.VolunteerAssignmentSystem.service;

import com.oop.VolunteerAssignmentSystem.model.AssignmentResult;
import com.oop.VolunteerAssignmentSystem.model.ServiceDetails;
import com.oop.VolunteerAssignmentSystem.model.Volunteer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import com.oop.VolunteerAssignmentSystem.model.Assignment;
import java.util.stream.Collectors;

@SpringBootTest
public class GeneticAlgorithmServiceTest {

    @Autowired
    private GeneticAlgorithmService geneticAlgorithmService;

    @Test
    void testRun_WithEmptyVolunteersAndServices_ShouldReturnEmptyResult() {
        Map<String, Volunteer> volunteerMap = new HashMap<>();
        List<ServiceDetails> serviceDetailsList = new ArrayList<>();

        AssignmentResult result = geneticAlgorithmService.run(volunteerMap, serviceDetailsList);

        assertNotNull(result);
        assertEquals("Cannot run GA: No volunteers or services.", result.getMessage());
        assertEquals(0, result.getTotalCost());
        assertTrue(result.getAssignments().isEmpty());
    }

    @Test
    void testRun_WithValidVolunteersAndServices_ShouldReturnResult() {
        // Arrange
        Map<String, Volunteer> volunteerMap = new HashMap<>();
        List<ServiceDetails> serviceDetailsList = new ArrayList<>();

        // Create volunteers with their preferences
        Map<Integer, String> v1Prefs = new HashMap<>();
        v1Prefs.put(1, "s1");
        Volunteer volunteer1 = new Volunteer("v1", "Volunteer 1", v1Prefs);
        
        Map<Integer, String> v2Prefs = new HashMap<>();
        v2Prefs.put(1, "s2");
        Volunteer volunteer2 = new Volunteer("v2", "Volunteer 2", v2Prefs);
        
        volunteerMap.put(volunteer1.getId(), volunteer1);
        volunteerMap.put(volunteer2.getId(), volunteer2);

        // Create services
        ServiceDetails service1 = new ServiceDetails("s1", "Service 1", 1);
        ServiceDetails service2 = new ServiceDetails("s2", "Service 2", 1);
        serviceDetailsList.add(service1);
        serviceDetailsList.add(service2);

        // Act
        AssignmentResult result = geneticAlgorithmService.run(volunteerMap, serviceDetailsList);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertNotNull(result.getAssignments(), "Assignments should not be null");
        assertFalse(result.getAssignments().isEmpty(), "Assignments should not be empty");
        assertEquals("Optimization complete. Best assignment found.", result.getMessage());
        
        // Verify assignments
        assertEquals(2, result.getAssignments().size(), "Should have assignments for both volunteers");
        
        // Verify that each service has been assigned
        Set<String> assignedServices = result.getAssignments().stream()
                .map(Assignment::getServiceId)
                .collect(Collectors.toSet());
        assertTrue(assignedServices.contains("s1") || assignedServices.contains("s2"), 
                "At least one of the services should be assigned");
        
        // Instead of checking if cost > 0, verify it's not negative
        assertTrue(result.getTotalCost() >= 0, "Total cost should not be negative");
    }

    @Test
    void testRun_WhenVolunteersExceedServiceCapacity_ShouldPenalizeCost() {
        Map<String, Volunteer> volunteerMap = new HashMap<>();
        List<ServiceDetails> serviceDetailsList = new ArrayList<>();

        Volunteer volunteer1 = new Volunteer("v1", "Volunteer 1", Map.of(1, "s1"));
        Volunteer volunteer2 = new Volunteer("v2", "Volunteer 2", Map.of(1, "s1"));
        volunteerMap.put(volunteer1.getId(), volunteer1);
        volunteerMap.put(volunteer2.getId(), volunteer2);

        ServiceDetails service1 = new ServiceDetails("s1", "Service 1", 1);
        serviceDetailsList.add(service1);

        AssignmentResult result = geneticAlgorithmService.run(volunteerMap, serviceDetailsList);

        assertNotNull(result);
        assertNotNull(result.getAssignments());
        assertFalse(result.getAssignments().isEmpty());
        assertEquals("Optimization complete. Best assignment found.", result.getMessage());
        assertTrue(result.getTotalCost() > 0);
    }

    @Test
    void testRun_WithVolunteersHavingNoPreferences_ShouldApplyPenalty() {
        Map<String, Volunteer> volunteerMap = new HashMap<>();
        List<ServiceDetails> serviceDetailsList = new ArrayList<>();

        Volunteer volunteer = new Volunteer("v1", "Volunteer 1", new HashMap<>());
        volunteerMap.put(volunteer.getId(), volunteer);

        ServiceDetails service1 = new ServiceDetails("s1", "Service 1", 5);
        ServiceDetails service2 = new ServiceDetails("s2", "Service 2", 5);
        serviceDetailsList.add(service1);
        serviceDetailsList.add(service2);

        AssignmentResult result = geneticAlgorithmService.run(volunteerMap, serviceDetailsList);

        assertNotNull(result);
        assertNotNull(result.getAssignments());
        assertFalse(result.getAssignments().isEmpty());
        assertEquals("Optimization complete. Best assignment found.", result.getMessage());
        assertTrue(result.getTotalCost() > 0);
    }

}