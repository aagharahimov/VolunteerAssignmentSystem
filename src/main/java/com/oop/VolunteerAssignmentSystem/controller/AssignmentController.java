package com.oop.VolunteerAssignmentSystem.controller;

import com.oop.VolunteerAssignmentSystem.service.AssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller // Can be @RestController if no view is served directly from here
@RequestMapping("/api/assignment")
public class AssignmentController {
    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    // REST endpoint to trigger optimization
    @PostMapping("/optimize")
    public ResponseEntity<String> triggerOptimization() {
        assignmentService.triggerOptimization();
        return ResponseEntity.ok("Optimization process started. Results will be broadcast.");
    }

    // This controller is also a good place for @MessageMapping if you use STOMP over WebSocket
    // For simplicity, broadcasting is done from AssignmentService via SimpMessagingTemplate
}
