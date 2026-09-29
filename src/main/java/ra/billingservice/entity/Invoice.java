package ra.billingservice.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long id;
    @Column(name = "invoice_number", length = 200, unique = true)
    private String invoiceNumber;
    @Column(name = "appointment_id")
    private Long appointmentId;
    @Column(name = "prescription_id")
    private Long prescriptionId;
    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type")
    @Builder.Default
    private InvoiceType invoiceType = InvoiceType.CONSULTATION;
    @Column(name = "patient_id", nullable = false)
    private Long patientId;
    @Column(name = "total_amount", nullable = false, columnDefinition = "numeric(10,2) default 0.00")
    private Double totalAmount;
    @Column(name = "coverage_rate", nullable = false, columnDefinition = "numeric(10,2) default 0.00")
    private Double coverageRate;
    @Column(name = "insurance_paid", nullable = false, columnDefinition = "numeric(10,2) default 0.00")
    private Double insurancePaid;
    @Column(name = "patient_paid", nullable = false, columnDefinition = "numeric(10,2) default 0.00")
    private Double patientPaid;
    @Column(nullable = false)
    private String status;
    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}
