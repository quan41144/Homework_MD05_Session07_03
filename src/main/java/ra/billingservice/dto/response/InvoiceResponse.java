package ra.billingservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class InvoiceResponse {
    private Long invoiceId;
    private Long appointmentId;
    private Long patientId;
    private String patientName;
    private Double amount;
    private Double coverageRate;
    private Double insurancePaid;
    private Double patientPaid;
    private String status;
    private String paymentMethod;
    private Long prescriptionId;
    private String invoiceType;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
