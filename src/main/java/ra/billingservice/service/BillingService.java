package ra.billingservice.service;

import ra.billingservice.dto.response.InvoiceResponse;

import java.util.List;

public interface BillingService {
    InvoiceResponse createInvoice(Long appointmentId, Long patientId, Double totalAmount, Boolean isInNetwork, String facilityLevel);
    InvoiceResponse createConsultationInvoice(Long appointmentId, Long patientId, Double consultationFee, Boolean isInNetwork, String facilityLevel);
    InvoiceResponse createMedicineInvoice(Long prescriptionId, Long patientId, Double totalAmount);
    InvoiceResponse updateInvoiceDraft(Long appointmentId, Double newTotalAmount, Boolean isInNetwork, String facilityLevel);
    InvoiceResponse processPayment(Long appointmentId, String paymentMethod, boolean success);
    InvoiceResponse getInvoiceById(Long invoiceId, Long currentUserId, boolean isAdmin);
    InvoiceResponse getInvoiceByAppointmentId(Long appointmentId, Long currentUserId, boolean isAdmin);
    List<InvoiceResponse> getInvoicesByPatientId(Long patientId);
    List<InvoiceResponse> getAllInvoices();
}
