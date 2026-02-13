package sun.asterisk.booking_tour.service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sun.asterisk.booking_tour.entity.User;
import sun.asterisk.booking_tour.enums.UserStatus;
import sun.asterisk.booking_tour.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCsvExportService {

    private final UserRepository userRepository;
    private static final String CSV_DIRECTORY = "exports";
    private static final String CSV_HEADER = "ID,First Name,Last Name,Email,Phone,Date of Birth,Status,Role,Verified,Created At";

    @Async("csvExportExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<String> exportAllUsersToCsv() {
        return exportUsersToCsv(null, null);
    }

    @Async("csvExportExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<String> exportUsersToCsv(String search, UserStatus status) {
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();

        log.info("[{}] Starting CSV export - search: {}, status: {}", threadName, search, status);

        try {
            Path exportPath = Paths.get(CSV_DIRECTORY);
            if (!Files.exists(exportPath)) {
                Files.createDirectories(exportPath);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = String.format("users_export_%s.csv", timestamp);
            Path filePath = exportPath.resolve(fileName);

            List<User> users = fetchUsers(search, status);
            log.info("[{}] Fetched {} users from database", threadName, users.size());

            writeToCsvFile(filePath, users);

            long duration = System.currentTimeMillis() - startTime;

            return CompletableFuture.completedFuture(filePath.toString());

        } catch (IOException e) {
            log.error("[{}] Failed to export users to CSV", threadName, e);
            return CompletableFuture.failedFuture(
                    new RuntimeException("Failed to export users to CSV: " + e.getMessage(), e)
            );
        } catch (Exception e) {
            log.error("[{}] Unexpected error during CSV export", threadName, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private List<User> fetchUsers(String search, UserStatus status) {
        boolean hasSearch = search != null && !search.trim().isEmpty();
        boolean hasStatus = status != null;

        if (hasStatus && hasSearch) {
            return userRepository.findBySearchAndStatus(search, status);
        }

        if (hasStatus) {
            return userRepository.findByStatus(status);
        }

        if (hasSearch) {
            return userRepository.findBySearch(search);
        }

        return userRepository.findAllWithRole();
    }

    private void writeToCsvFile(Path filePath, List<User> users) throws IOException {
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.append(CSV_HEADER).append("\n");

            for (User user : users) {
                writer.append(formatCsvRow(user)).append("\n");
            }

            writer.flush();
        }
    }

    private String formatCsvRow(User user) {
        return String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                user.getId(),
                escapeCsv(user.getFirstName()),
                escapeCsv(user.getLastName()),
                escapeCsv(user.getEmail()),
                escapeCsv(user.getPhone()),
                user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : "",
                user.getStatus() != null ? user.getStatus().name() : "UNKNOWN",
                user.getRole() != null ? escapeCsv(user.getRole().getName()) : "",
                user.getIsVerified() != null && user.getIsVerified() ? "Yes" : "No",
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : ""
        );
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\"", "\"\"");
    }
}
