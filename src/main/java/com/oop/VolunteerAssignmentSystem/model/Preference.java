package com.oop.VolunteerAssignmentSystem.model;

import lombok.Data;

import java.util.List;

@Data
public class Preference {
    private String volunteerId;
    private List<String> rankedServiceIds; // Ordered list of service IDs by preference
}
