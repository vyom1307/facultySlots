package com.example.facultySlots;
import lombok.Data;

@Data
public class BookingRequest {
    private String facultyId;
    private String date;      // e.g., "2025-11-25"
    private String slotId;    // e.g., "10:00-10:30"
    private String studentUid; // Unsecured: Coming from request body
    private Integer duration;
    private String startTime;
    private String endTime;
    
}
