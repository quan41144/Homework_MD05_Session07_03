package ra.billingservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ra.billingservice.dto.response.ApiResponse;
import ra.billingservice.service.BillingService;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {
    private final BillingService billingService;
    @PreAuthorize("hasRole('PATIENT')")
    @PostMapping("/pay/{appointmentId}")
    public ResponseEntity<ApiResponse<?>> processPayment(@PathVariable Long appointmentId,
                                                         @RequestParam String paymentMethod,
                                                         @RequestParam Boolean success) {
        return new ResponseEntity<>(new ApiResponse<>(
                success,
                success ? "Thanh toán thành công!" : "Thanh toán thất bại!",
                success ? billingService.processPayment(appointmentId, paymentMethod, true) : null,
                success ? null : "Thanh toán thất bại!",
                LocalDateTime.now()
        ), HttpStatus.OK);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/invoices")
    public ResponseEntity<ApiResponse<?>> getAllInvoices() {
        return new ResponseEntity<>(new ApiResponse<>(
                true,
                "Lấy danh sách hóa đơn thành công!",
                billingService.getAllInvoices(),
                null,
                LocalDateTime.now()
        ), HttpStatus.OK);
    }
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/invoices/{invoiceId}")
    public ResponseEntity<ApiResponse<?>> getInvoiceById(@PathVariable Long invoiceId,
                                                         org.springframework.security.core.Authentication authentication) {
        Long currentUserId = (Long) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
        return new ResponseEntity<>(new ApiResponse<>(
                true,
                "Lấy thông tin hóa đơn thành công!",
                billingService.getInvoiceById(invoiceId, currentUserId, isAdmin),
                null,
                LocalDateTime.now()
        ), HttpStatus.OK);
    }
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<ApiResponse<?>> getInvoiceByAppointment(@PathVariable Long appointmentId,
                                                                 org.springframework.security.core.Authentication authentication) {
        Long currentUserId = (Long) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
        return new ResponseEntity<>(new ApiResponse<>(
                true,
                "Lấy thông tin hóa đơn thành công!",
                billingService.getInvoiceByAppointmentId(appointmentId, currentUserId, isAdmin),
                null,
                LocalDateTime.now()
        ), HttpStatus.OK);
    }
    @PreAuthorize("hasRole('ADMIN') or #patientId == authentication.principal")
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<?>> getInvoiceByPatient(@PathVariable Long patientId) {
        return new ResponseEntity<>(new ApiResponse<>(
                true,
                "Lấy thông tin hóa đơn thành công!",
                billingService.getInvoicesByPatientId(patientId),
                null,
                LocalDateTime.now()
        ), HttpStatus.OK);
    }
}
