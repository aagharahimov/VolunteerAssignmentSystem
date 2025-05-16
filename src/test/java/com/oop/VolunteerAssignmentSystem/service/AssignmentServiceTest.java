package com.oop.VolunteerAssignmentSystem.service;

import com.oop.VolunteerAssignmentSystem.model.AssignmentResult;
import com.oop.VolunteerAssignmentSystem.model.ServiceDetails;
import com.oop.VolunteerAssignmentSystem.model.Volunteer;
import com.oop.VolunteerAssignmentSystem.repository.InMemoryPreferenceRepository;
import com.oop.VolunteerAssignmentSystem.repository.InMemoryServiceRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

class AssignmentServiceTest {

    /**
     * Test class for AssignmentService that validates the triggerOptimization method.
     * The triggerOptimization method handles the initialization and execution
     * of a genetic algorithm to assign volunteers to services based on certain preferences.
     * The results are then communicated to a messaging topic.
     */

    @Mock
    private GeneticAlgorithmService geneticAlgorithmService;

    @Mock
    private InMemoryPreferenceRepository preferenceRepository;

    @Mock
    private InMemoryServiceRepository serviceRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private AssignmentService assignmentService;

    public AssignmentServiceTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testTriggerOptimization_NoVolunteers() {
        when(preferenceRepository.findAllVolunteersWithPreferences()).thenReturn(Collections.emptyMap());
        when(serviceRepository.findAll()).thenReturn(List.of(new ServiceDetails()));

        assignmentService.triggerOptimization();

        AssignmentResult expectedResult = new AssignmentResult();
        expectedResult.setMessage("No volunteers with preferences available for assignment.");
        verify(messagingTemplate, timeout(1000)).convertAndSend("/topic/assignments", expectedResult);

        verify(geneticAlgorithmService, never()).run(anyMap(), anyList());
    }

    @Test
    void testTriggerOptimization_NoServices() {
        when(preferenceRepository.findAllVolunteersWithPreferences()).thenReturn(Map.of("1", new Volunteer()));
        when(serviceRepository.findAll()).thenReturn(Collections.emptyList());

        assignmentService.triggerOptimization();

        AssignmentResult expectedResult = new AssignmentResult();
        expectedResult.setMessage("No services available for assignment.");
        verify(messagingTemplate, timeout(1000)).convertAndSend("/topic/assignments", expectedResult);

        verify(geneticAlgorithmService, never()).run(anyMap(), anyList());
    }

    @Test
    void testTriggerOptimization_SuccessfulExecution() throws Exception {
        Volunteer volunteer = new Volunteer();
        Map<String, Volunteer> volunteers = Map.of("1", volunteer);
        ServiceDetails service = new ServiceDetails();
        List<ServiceDetails> services = List.of(service);

        AssignmentResult result = new AssignmentResult();
        result.setTotalCost(100);
        result.setMessage("Optimization complete.");

        when(preferenceRepository.findAllVolunteersWithPreferences()).thenReturn(volunteers);
        when(serviceRepository.findAll()).thenReturn(services);
        when(geneticAlgorithmService.run(volunteers, services)).thenReturn(result);

        assignmentService.triggerOptimization();

        verify(messagingTemplate, timeout(1000)).convertAndSend("/topic/assignments", result);
    }

    @Test
    void testTriggerOptimization_ExceptionHandling() {
        when(preferenceRepository.findAllVolunteersWithPreferences()).thenReturn(Map.of("1", new Volunteer()));
        when(serviceRepository.findAll()).thenReturn(List.of(new ServiceDetails()));
        when(geneticAlgorithmService.run(anyMap(), anyList())).thenThrow(new RuntimeException("Test exception"));

        assignmentService.triggerOptimization();

        AssignmentResult expectedResult = new AssignmentResult();
        expectedResult.setMessage("Error during optimization: Test exception");
        verify(messagingTemplate, timeout(1000)).convertAndSend("/topic/assignments", expectedResult);
    }
}