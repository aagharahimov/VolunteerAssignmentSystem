package com.oop.VolunteerAssignmentSystem.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Assignment {
    private String volunteerId;
    private String serviceId;
    private double cost;
    private int preferenceRank; // 0 if not preferred, 1 for 1st choice, etc.
}
