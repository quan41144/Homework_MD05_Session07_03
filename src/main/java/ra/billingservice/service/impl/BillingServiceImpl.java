package ra.billingservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ra.billingservice.client.PatientServiceClient;
import ra.billingservice.config.RabbitMQConfig;
import ra.billingservice.dto.response.ApiResponse;
import ra.billingservice.dto.response.InvoiceResponse;
import ra.billingservice.dto.response.PatientResponse;
import ra.billingservice.entity.Invoice;
import ra.billingservice.exception.BadRequestException;
import ra.billingservice.exception.ForbiddenException;
import ra.billingservice.exception.ResourceNotFoundException;
import ra.billingservice.repository.InvoiceRepository;
import ra.billingservice.service.BillingService;
import ra.billingservice.service.HealthInsuranceAssessmentService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingServiceImpl implements BillingService {
    private final InvoiceRepository invoiceRepository;
    private final PatientServiceClient patientServiceClient;
    private final HealthInsuranceAssessmentService healthInsuranceAssessmentService;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = "invoiceList", allEntries = true),
                    @CacheEvict(value = "invoiceListByPatient", allEntries = true),
                    @CacheEvict(value = "invoiceByAppointment", allEntries = true)
            },
            put = {@CachePut(value = "invoiceDetail", key = "#result.invoiceId")}
    )
    public InvoiceResponse createInvoice(Long appointmentId, Long patientId, Double totalAmount, Boolean isInNetwork, String facilityLevel) {
        PatientResponse patientResponse = null;
        try {
            ApiResponse<PatientResponse> patientApiResponse = patientServiceClient.getPatientById(patientId);
            if (patientApiResponse != null) {
                patientResponse = patientApiResponse.getData();
            }
        }
        catch (Exception e) {
            log.warn("Không thể lấy thông tin thẻ BHYT của bệnh nhân có ID: {}. Tạo hóa đơn với mức miễn giảm BHYT mặc định (0%).", patientId);
        }
        String level = (facilityLevel != null && !facilityLevel.trim().isEmpty() ? facilityLevel : "DISTRICT");
        Double coverageRate = healthInsuranceAssessmentService.calculateCoverageRate(patientResponse, isInNetwork, level);
        Double insurancePaid = Math.round(totalAmount * coverageRate * 100.0) / 100.0;
        Double patientPaid = Math.round((totalAmount - insurancePaid) * 100.0) / 100.0;

        Invoice invoice = Invoice.builder()
                .appointmentId(appointmentId)
                .patientId(patientId)
                .totalAmount(totalAmount)
                .coverageRate(coverageRate)
                .insurancePaid(insurancePaid)
                .patientPaid(patientPaid)
                .status("PENDING")
                .paymentMethod("CHƯA_THANH_TOÁN")
                .build();
        Invoice saved = invoiceRepository.save(invoice);
        log.info("Đã tạo hóa đơn ID: {} cho lịch hẹn ID {}", saved.getId(), appointmentId);
        log.info("Tổng số tiền: {} VNĐ", totalAmount);
        log.info("BHYT chi trả ({}): {} VNĐ", (int)(coverageRate * 100), insurancePaid);
        log.info("Bệnh nhân phải trả: {} VNĐ", patientPaid);
        rabbitTemplate.convertAndSend(RabbitMQConfig.INVOICE_DELAY_EXCHANGE, RabbitMQConfig.ROUTING_INVOICE_DELAY, saved.getId());
        return InvoiceResponse.builder()
                .invoiceId(saved.getId())
                .appointmentId(saved.getAppointmentId())
                .patientId(saved.getPatientId())
                .patientName(patientResponse != null ? patientResponse.getFullName() : null)
                .amount(saved.getTotalAmount())
                .coverageRate(saved.getCoverageRate())
                .insurancePaid(saved.getInsurancePaid())
                .patientPaid(saved.getPatientPaid())
                .status(saved.getStatus())
                .paymentMethod(saved.getPaymentMethod())
                .createdAt(saved.getCreatedAt())
                .paidAt(saved.getPaidAt())
                .build();
    }

    @Override
    @Transactional
    public InvoiceResponse createConsultationInvoice(Long appointmentId, Long patientId, Double consultationFee, Boolean isInNetwork, String facilityLevel) {
        InvoiceResponse response = createInvoice(appointmentId, patientId, consultationFee, isInNetwork, facilityLevel);
        invoiceRepository.findById(response.getInvoiceId()).ifPresent(inv -> {
            inv.setInvoiceType(ra.billingservice.entity.InvoiceType.CONSULTATION);
            invoiceRepository.save(inv);
        });
        return response;
    }

    @Override
    @Transactional
    public InvoiceResponse createMedicineInvoice(Long prescriptionId, Long patientId, Double totalAmount) {
        Invoice invoice = Invoice.builder()
                .prescriptionId(prescriptionId)
                .patientId(patientId)
                .totalAmount(totalAmount)
                .coverageRate(0.0)
                .insurancePaid(0.0)
                .patientPaid(totalAmount)
                .status("PENDING")
                .invoiceType(ra.billingservice.entity.InvoiceType.MEDICINE)
                .paymentMethod("CHƯA_THANH_TOÁN")
                .createdAt(LocalDateTime.now())
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Đã tự động tạo Hóa đơn Thuốc (MEDICINE) ID {} cho Đơn thuốc ID {}", saved.getId(), prescriptionId);
        return InvoiceResponse.builder()
                .invoiceId(saved.getId())
                .patientId(saved.getPatientId())
                .amount(saved.getTotalAmount())
                .coverageRate(saved.getCoverageRate())
                .insurancePaid(saved.getInsurancePaid())
                .patientPaid(saved.getPatientPaid())
                .status(saved.getStatus())
                .paymentMethod(saved.getPaymentMethod())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    @Caching(
            put = {@CachePut(value = "invoiceDetail", key = "#result.invoiceId")},
            evict = {
                    @CacheEvict(value = "invoiceList", allEntries = true),
                    @CacheEvict(value = "invoiceListByPatient", allEntries = true),
                    @CacheEvict(value = "invoiceByAppointment", key = "#appointmentId")
            }
    )
    public InvoiceResponse updateInvoiceDraft(Long appointmentId, Double newTotalAmount, Boolean isInNetwork, String facilityLevel) {
        Invoice invoice = invoiceRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn cho lịch hẹn ID: " + appointmentId));
        if ("PAID".equalsIgnoreCase(invoice.getStatus())) {
            throw new BadRequestException("Hóa đơn đã được thanh toán với mã hóa đơn: " + invoice.getInvoiceNumber());
        }
        if ("CANCELLED".equalsIgnoreCase(invoice.getStatus())) {
            throw new BadRequestException("Hóa đơn đã bị hủy!");
        }
        PatientResponse patientResponse = null;
        try {
            ApiResponse<PatientResponse> patientApiResponse = patientServiceClient.getPatientById(invoice.getPatientId());
            if (patientApiResponse != null) {
                patientResponse = patientApiResponse.getData();
            }
        }
        catch (Exception e) {
            log.warn("Không thể lấy thông tin BHYT bệnh nhân ID {}", invoice.getPatientId());
        }
        Double coverageRate = healthInsuranceAssessmentService.calculateCoverageRate(patientResponse, isInNetwork, facilityLevel);
        Double insurancePaid = Math.round(newTotalAmount * coverageRate * 100.0) / 100.0;
        Double patientPaid = Math.round((newTotalAmount - insurancePaid) * 100.0) / 100.0;

        invoice.setTotalAmount(newTotalAmount);
        invoice.setCoverageRate(coverageRate);
        invoice.setInsurancePaid(insurancePaid);
        invoice.setPatientPaid(patientPaid);

        invoiceRepository.save(invoice);
        return InvoiceResponse.builder()
                .invoiceId(invoice.getId())
                .appointmentId(invoice.getAppointmentId())
                .patientId(invoice.getPatientId())
                .patientName(patientResponse != null ? patientResponse.getFullName() : null)
                .amount(invoice.getTotalAmount())
                .coverageRate(invoice.getCoverageRate())
                .insurancePaid(invoice.getInsurancePaid())
                .patientPaid(invoice.getPatientPaid())
                .status(invoice.getStatus())
                .paymentMethod(invoice.getPaymentMethod())
                .createdAt(invoice.getCreatedAt())
                .paidAt(invoice.getPaidAt())
                .build();
    }

    @Override
    @Transactional
    @Caching(
            put = {@CachePut(value = "invoiceDetail", key = "#result.invoiceId")},
            evict = {
                    @CacheEvict(value = "invoiceList", allEntries = true),
                    @CacheEvict(value = "invoiceListByPatient", allEntries = true),
                    @CacheEvict(value = "invoiceByAppointment", key = "#appointmentId")
            }
    )
    public InvoiceResponse processPayment(Long appointmentId, String paymentMethod, boolean success) {
        Invoice invoice = invoiceRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn cho lịch hẹn có ID: " + appointmentId));
        if ("PAID".equalsIgnoreCase(invoice.getStatus())) {
            log.info("Hóa đơn đã được thanh toán!");
            return toInvoiceResponse(invoice);
        }
        if (success) {
            String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String officialInvoiceNumber = String.format("INV-%s-%06d", datePrefix, invoice.getId());

            invoice.setInvoiceNumber(officialInvoiceNumber);
            invoice.setStatus("PAID");
            invoice.setPaymentMethod(paymentMethod);
            invoice.setPaidAt(LocalDateTime.now());
            Invoice saved = invoiceRepository.save(invoice);

            log.info("Thanh toán thành công! số tiền đã trả: {} VNĐ", invoice.getPatientPaid());

            Map<String, Object> eventPayload = Map.of(
                    "appointmentId", appointmentId,
                    "patientId", invoice.getPatientId(),
                    "paidAmount", invoice.getPatientPaid().toString(),
                    "invoiceNumber", officialInvoiceNumber
            );
            rabbitTemplate.convertAndSend(RabbitMQConfig.BILLING_EXCHANGE, RabbitMQConfig.ROUTING_PAYMENT_SUCCESS, eventPayload);
            return toInvoiceResponse(saved);
        }
        else {
            invoice.setStatus("CANCELLED");
            Invoice saved = invoiceRepository.save(invoice);

            log.info("Thanh toán thất bại!");
            Map<String, Object> eventPayload = Map.of(
                    "appointmentId", appointmentId,
                    "reason", "Người dùng từ chối thanh toán hoặc giao dịch thất bại!"
            );
            rabbitTemplate.convertAndSend(RabbitMQConfig.BILLING_EXCHANGE, RabbitMQConfig.ROUTING_PAYMENT_FAILED, eventPayload);
            return toInvoiceResponse(saved);
        }
    }

    @Override
    @Cacheable(value = "invoiceDetail", key = "#invoiceId + '_' + #currentUserId")
    public InvoiceResponse getInvoiceById(Long invoiceId, Long currentUserId, boolean isAdmin) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn ID: " + invoiceId));
        if (!isAdmin && !invoice.getPatientId().equals(currentUserId)) {
            throw new ForbiddenException("Bạn không có quyền xem thông tin hóa đơn này!");
        }
        return toInvoiceResponse(invoice);
    }

    @Override
    @Cacheable(value = "invoiceByAppointment", key = "#appointmentId + '_' + #currentUserId")
    public InvoiceResponse getInvoiceByAppointmentId(Long appointmentId, Long currentUserId, boolean isAdmin) {
        Invoice invoice = invoiceRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn cho lịch hẹn ID: " + appointmentId));
        if (!isAdmin && !invoice.getPatientId().equals(currentUserId)) {
            throw new ForbiddenException("Bạn không có quyền xem thông tin hóa đơn này!");
        }
        return toInvoiceResponse(invoice);
    }

    @Override
    @Cacheable(value = "invoiceListByPatient", key = "#patientId")
    public List<InvoiceResponse> getInvoicesByPatientId(Long patientId) {
        List<Invoice> invoices = invoiceRepository.findByPatientId(patientId);
        String patientName = null;
        try {
            ApiResponse<PatientResponse> patientApiResponse = patientServiceClient.getPatientById(patientId);
            if (patientApiResponse != null && patientApiResponse.getData() != null) {
                patientName = patientApiResponse.getData().getFullName();
            }
        }
        catch (Exception e) {
            log.warn("Không thể lấy thông tin bệnh nhân ID {}", patientId);
        }
        String finalPatientName = patientName;
        return invoices.stream()
                .map(invoice -> InvoiceResponse.builder()
                        .invoiceId(invoice.getId())
                        .appointmentId(invoice.getAppointmentId())
                        .patientId(invoice.getPatientId())
                        .patientName(finalPatientName)
                        .amount(invoice.getTotalAmount())
                        .coverageRate(invoice.getCoverageRate())
                        .insurancePaid(invoice.getInsurancePaid())
                        .patientPaid(invoice.getPatientPaid())
                        .status(invoice.getStatus())
                        .paymentMethod(invoice.getPaymentMethod())
                        .createdAt(invoice.getCreatedAt())
                        .paidAt(invoice.getPaidAt())
                        .build()
                ).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "invoiceList")
    public List<InvoiceResponse> getAllInvoices() {
        List<Invoice> invoices = invoiceRepository.findAll();
        Map<Long, String> patientNameCache = new HashMap<>();
        return invoices.stream()
                .map(invoice -> {
                    String patientName = patientNameCache.computeIfAbsent(invoice.getPatientId(), pid -> {
                        try {
                            ApiResponse<PatientResponse> patientApiResponse = patientServiceClient.getPatientById(pid);
                            return (patientApiResponse != null && patientApiResponse.getData() != null)
                                    ? patientApiResponse.getData().getFullName() : null;
                        }
                        catch (Exception e) {
                            log.warn("Không thể lấy thông tin bệnh nhân ID {}", pid);
                            return null;
                        }
                    });
                    return InvoiceResponse.builder()
                            .invoiceId(invoice.getId())
                            .appointmentId(invoice.getAppointmentId())
                            .prescriptionId(invoice.getPrescriptionId())
                            .invoiceType(invoice.getInvoiceType() != null ? invoice.getInvoiceType().name() : null)
                            .patientId(invoice.getPatientId())
                            .patientName(patientName)
                            .amount(invoice.getTotalAmount())
                            .coverageRate(invoice.getCoverageRate())
                            .insurancePaid(invoice.getInsurancePaid())
                            .patientPaid(invoice.getPatientPaid())
                            .status(invoice.getStatus())
                            .paymentMethod(invoice.getPaymentMethod())
                            .createdAt(invoice.getCreatedAt())
                            .paidAt(invoice.getPaidAt())
                            .build();
                }).toList();
    }
    private InvoiceResponse toInvoiceResponse(Invoice invoice) {
        String patientName = null;
        try {
            ApiResponse<PatientResponse> patientApiResponse = patientServiceClient.getPatientById(invoice.getPatientId());
            if (patientApiResponse != null && patientApiResponse.getData() != null) {
                patientName = patientApiResponse.getData().getFullName();
            }
        }
        catch (Exception e) {
            log.warn("Không thể lấy thông tin bệnh nhân ID {}", invoice.getPatientId());
        }
        return InvoiceResponse.builder()
                .invoiceId(invoice.getId())
                .appointmentId(invoice.getAppointmentId())
                .prescriptionId(invoice.getPrescriptionId())
                .invoiceType(invoice.getInvoiceType() != null ? invoice.getInvoiceType().name() : null)
                .patientId(invoice.getPatientId())
                .patientName(patientName)
                .amount(invoice.getTotalAmount())
                .coverageRate(invoice.getCoverageRate())
                .insurancePaid(invoice.getInsurancePaid())
                .patientPaid(invoice.getPatientPaid())
                .status(invoice.getStatus())
                .paymentMethod(invoice.getPaymentMethod())
                .createdAt(invoice.getCreatedAt())
                .paidAt(invoice.getPaidAt())
                .build();
    }
}