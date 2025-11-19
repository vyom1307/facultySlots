package com.example.facultySlots;


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Slot {
    private String status = "available";
    private String bookedBy = null;
    private String start; // HH:mm
    private String end;   // HH:mm

    public Slot(String start, String end) {
        this.start = start;
        this.end = end;
    }
}