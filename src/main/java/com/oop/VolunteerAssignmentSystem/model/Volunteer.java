package com.oop.VolunteerAssignmentSystem.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Volunteer {
    private String id; // e.g., "volunteer1"
    private String name;
    // Key: preference rank (1, 2, ... Ns), Value: serviceId
    private Map<Integer, String> preferredServicesRanks; // e.g., {1: "serviceA", 2: "serviceC"}
}
