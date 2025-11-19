package com.example.facultySlots;

import com.example.facultySlots.BookingRequest;
import com.example.facultySlots.Slot;
import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class SlotService {

    private Firestore firestore;

    private static final String COLLECTION_FACULTY = "Faculty";
    private static final String STATUS_AVAILABLE = "available";
    private static final String STATUS_BOOKED = "booked";

    @PostConstruct
    public void initializeFirebase() throws IOException {
        // If already initialized, reuse
        if (!FirebaseApp.getApps().isEmpty()) {
            this.firestore = FirestoreClient.getFirestore();
            return;
        }

        // Read required env vars
        String type = System.getenv("FIREBASE_TYPE");
        String projectId = System.getenv("FIREBASE_PROJECT_ID");
        String privateKeyId = System.getenv("FIREBASE_PRIVATE_KEY_ID");
        String privateKey = System.getenv("FIREBASE_PRIVATE_KEY");
        String clientEmail = System.getenv("FIREBASE_CLIENT_EMAIL");
        String clientId = System.getenv("FIREBASE_CLIENT_ID");
        String authUri = System.getenv("FIREBASE_AUTH_URI");
        String tokenUri = System.getenv("FIREBASE_TOKEN_URI");
        String authProviderCertUrl = System.getenv("FIREBASE_AUTH_PROVIDER_CERT_URL");
        String clientCertUrl = System.getenv("FIREBASE_CLIENT_CERT_URL");

        // Basic validation
        if (projectId == null || privateKey == null || clientEmail == null) {
            throw new IllegalStateException("Missing required Firebase environment variables. " +
                    "Make sure FIREBASE_PROJECT_ID, FIREBASE_PRIVATE_KEY and FIREBASE_CLIENT_EMAIL are set.");
        }

        // Ensure the literal "\n" escapes are converted into real newlines
        privateKey = privateKey.replace("\\n", "\n");

        // Build the JSON in-memory exactly as service-account file format
        String json = "{"
                + "\"type\":\"" + (type == null ? "service_account" : type) + "\","
                + "\"project_id\":\"" + projectId + "\","
                + "\"private_key_id\":\"" + (privateKeyId == null ? "" : privateKeyId) + "\","
                + "\"private_key\":\"" + privateKey.replace("\n", "\\n") + "\","
                + "\"client_email\":\"" + clientEmail + "\","
                + "\"client_id\":\"" + (clientId == null ? "" : clientId) + "\","
                + "\"auth_uri\":\"" + (authUri == null ? "https://accounts.google.com/o/oauth2/auth" : authUri) + "\","
                + "\"token_uri\":\"" + (tokenUri == null ? "https://oauth2.googleapis.com/token" : tokenUri) + "\","
                + "\"auth_provider_x509_cert_url\":\"" + (authProviderCertUrl == null ? "" : authProviderCertUrl) + "\","
                + "\"client_x509_cert_url\":\"" + (clientCertUrl == null ? "" : clientCertUrl) + "\""
                + "}";

        // Note: GoogleCredentials expects actual newlines in the private_key field when read from a stream,
        // so we convert \\n back to real newlines for the in-memory stream.
        String jsonForStream = json.replace("\\n", "\n");

        GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(jsonForStream.getBytes())
        );

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build();

        FirebaseApp.initializeApp(options);

        this.firestore = FirestoreClient.getFirestore();
    }

    // --- generateAndSaveSlots ---
    public int generateAndSaveSlots(BookingRequest req) throws ExecutionException, InterruptedException {
        List<Slot> generatedSlots = generateTimeSlots(req.getStartTime(), req.getEndTime(), req.getDuration());

        WriteBatch batch = firestore.batch();

        CollectionReference itemsRef = firestore
                .collection(COLLECTION_FACULTY).document(req.getFacultyId())
                .collection("slots").document(req.getDate())
                .collection("items");

        for (Slot slot : generatedSlots) {
            String slotId = slot.getStart() + "-" + slot.getEnd();
            DocumentReference docRef = itemsRef.document(slotId);

            Map<String, Object> data = new HashMap<>();
            data.put("status", slot.getStatus());
            data.put("bookedBy", null);
            data.put("start", slot.getStart());
            data.put("end", slot.getEnd());

            batch.set(docRef, data);
        }

        batch.commit().get();
        return generatedSlots.size();
    }

    private List<Slot> generateTimeSlots(String startStr, String endStr, int durationMinutes) {
        LocalTime current = LocalTime.parse(startStr);
        LocalTime end = LocalTime.parse(endStr);
        Duration duration = Duration.ofMinutes(durationMinutes);
        List<Slot> slots = new ArrayList<>();

        while (current.plus(duration).isBefore(end) || current.plus(duration).equals(end)) {
            LocalTime nextSlot = current.plus(duration);
            slots.add(new Slot(current.toString(), nextSlot.toString()));
            current = nextSlot;
        }
        return slots;
    }

    // --- getAvailableSlots ---
    public List<Slot> getAvailableSlots(String facultyId, String date) throws ExecutionException, InterruptedException {
        List<Slot> availableSlots = new ArrayList<>();

        CollectionReference itemsRef = firestore
                .collection(COLLECTION_FACULTY).document(facultyId)
                .collection("slots").document(date)
                .collection("items");

        List<QueryDocumentSnapshot> documents = itemsRef
                .whereEqualTo("status", STATUS_AVAILABLE)
                .get().get().getDocuments();

        for (QueryDocumentSnapshot doc : documents) {
            availableSlots.add(doc.toObject(Slot.class));
        }
        return availableSlots;
    }

    // --- bookSlot ---
    public boolean bookSlot(BookingRequest req) throws ExecutionException, InterruptedException, IllegalStateException {
        DocumentReference slotRef = firestore
                .collection(COLLECTION_FACULTY).document(req.getFacultyId())
                .collection("slots").document(req.getDate())
                .collection("items").document(req.getSlotId());

        firestore.runTransaction(transaction -> {
            Slot slot = transaction.get(slotRef).get().toObject(Slot.class);

            if (slot == null) {
                throw new IllegalStateException("Slot not found.");
            }
            if (STATUS_BOOKED.equals(slot.getStatus())) {
                throw new IllegalStateException("Slot is already booked.");
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", STATUS_BOOKED);
            updates.put("bookedBy", req.getStudentUid());

            transaction.update(slotRef, updates);
            return true;
        }).get();

        return true;
    }

    // --- cancelSlot ---
    public void cancelSlot(BookingRequest req) throws ExecutionException, InterruptedException {
        DocumentReference slotRef = firestore
                .collection(COLLECTION_FACULTY).document(req.getFacultyId())
                .collection("slots").document(req.getDate())
                .collection("items").document(req.getSlotId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", STATUS_AVAILABLE);
        updates.put("bookedBy", null);

        slotRef.update(updates).get();
    }
}
