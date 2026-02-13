package sun.asterisk.booking_tour.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "failed_email_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FailedEmailMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_code", nullable = false)
    private String bookingCode;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(name = "message_content", columnDefinition = "TEXT")
    private String messageContent;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;
}
