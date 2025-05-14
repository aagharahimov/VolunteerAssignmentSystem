package com.oop.VolunteerAssignmentSystem.model;

import lombok.Data;

import java.util.List;

@Data
public class AssignmentResult {
    private List<Assignment> assignments;
    private double totalCost;
    private String message; // e.g., "Optimization complete" or "Error"
}
