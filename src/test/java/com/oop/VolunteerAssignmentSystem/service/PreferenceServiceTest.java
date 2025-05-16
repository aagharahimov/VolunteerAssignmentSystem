package com.oop.VolunteerAssignmentSystem.service;

import com.oop.VolunteerAssignmentSystem.model.Preference;
import com.oop.VolunteerAssignmentSystem.model.ServiceDetails;
import com.oop.VolunteerAssignmentSystem.repository.InMemoryPreferenceRepository;
import com.oop.VolunteerAssignmentSystem.repository.InMemoryServiceRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PreferenceServiceTest {

    @Mock
    private InMemoryPreferenceRepository preferenceRepository;

    @Mock
    private InMemoryServiceRepository serviceRepository;

    @InjectMocks
    private PreferenceService preferenceService;

    public PreferenceServiceTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void submitPreferences_ShouldThrowException_WhenVolunteerIdIsNull() {
        String volunteerId = null;
        Preference preference = new Preference();
        preference.setRankedServiceIds(Collections.singletonList("service1"));

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                preferenceService.submitPreferences(volunteerId, preference));
        verify(preferenceRepository, never()).save(anyString(), any());
        verify(serviceRepository, never()).findById(anyString());
    }

    @Test
    void submitPreferences_ShouldThrowException_WhenRankedServiceIdsAreEmpty() {
        String volunteerId = "volunteer1";
        Preference preference = new Preference();
        preference.setRankedServiceIds(Collections.emptyList());

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                preferenceService.submitPreferences(volunteerId, preference));
        verify(preferenceRepository, never()).save(anyString(), any());
        verify(serviceRepository, never()).findById(anyString());
    }

    @Test
    void submitPreferences_ShouldThrowException_WhenPreferenceExceedsMaxServices() {
        String volunteerId = "volunteer1";
        Preference preference = new Preference();
        preference.setRankedServiceIds(List.of("service1", "service2", "service3", "service4", "service5", "service6"));

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                preferenceService.submitPreferences(volunteerId, preference));
        verify(preferenceRepository, never()).save(anyString(), any());
        verify(serviceRepository, never()).findById(anyString());
    }

    @Test
    void submitPreferences_ShouldThrowException_WhenServiceIdDoesNotExist() {
        String volunteerId = "volunteer1";
        Preference preference = new Preference();
        preference.setRankedServiceIds(List.of("service1"));

        when(serviceRepository.findById("service1")).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                preferenceService.submitPreferences(volunteerId, preference));
        verify(preferenceRepository, never()).save(anyString(), any());
        verify(serviceRepository).findById("service1");
    }

    @Test
    void submitPreferences_ShouldSavePreferences_WhenDataIsValid() {
        String volunteerId = "volunteer1";
        Preference preference = new Preference();
        preference.setRankedServiceIds(List.of("service1"));

        when(serviceRepository.findById("service1")).thenReturn(Optional.of(new ServiceDetails()));

        preferenceService.submitPreferences(volunteerId, preference);

        verify(preferenceRepository).save(volunteerId, preference);
        verify(serviceRepository).findById("service1");
    }
}