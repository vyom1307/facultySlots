package com.example.facultySlots;




import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/slots")
@CrossOrigin
public class SlotController {

    @Autowired
    private SlotService slotService;

    // POST /api/slots/generate
    @PostMapping("/generate")
    public ResponseEntity<?> generateSlots(@RequestBody BookingRequest req) {
        try {
            
            int count = slotService.generateAndSaveSlots(req);
            return ResponseEntity.ok(
                    Map.of("success", true, "message", count + " slots created successfully.")
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error creating slots: " + e.getMessage()));
        }
    }

    // GET /api/slots?facultyId=F1&date=2025-11-25
    @GetMapping
    public ResponseEntity<?> getAvailableSlots(@RequestParam String facultyId, @RequestParam String date) {
        try {
            System.out.println("Entered: "+facultyId+ " " +date);
            List<Slot> slots = slotService.getAvailableSlots(facultyId, date);
            return ResponseEntity.ok(slots);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving slots: " + e.getMessage()));
        }
    }

    // POST /api/slots/book
    @PostMapping("/book")
    public ResponseEntity<?> bookSlot(@RequestBody BookingRequest req) {
        try {
            // Note: BookingRequest must contain studentUid in the body
            slotService.bookSlot(req);
            return ResponseEntity.ok(
                    Map.of("success", true, "message", "Slot booked by " + req.getStudentUid())
            );
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Booking failed: " + e.getMessage()));
        }
    }

    // POST /api/slots/cancel
    @PostMapping("/cancel")
    public ResponseEntity<?> cancelSlot(@RequestBody BookingRequest req) {
        try {
            slotService.cancelSlot(req);
            return ResponseEntity.ok(
                    Map.of("success", true, "message", "Booking cancelled.")
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Cancellation failed: " + e.getMessage()));
        }
    }
}
