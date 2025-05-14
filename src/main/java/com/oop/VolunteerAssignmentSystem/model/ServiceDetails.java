package com.oop.VolunteerAssignmentSystem.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDetails {
    private String id; // e.g., "reception"
    private String name;
    private int maxVolunteers; // Capacity
}
