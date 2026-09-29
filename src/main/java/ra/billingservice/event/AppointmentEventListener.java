package ra.billingservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ra.billingservice.config.RabbitMQConfig;
import ra.billingservice.entity.Invoice;
import ra.billingservice.exception.AppointmentNotCancelException;
import ra.billingservice.exception.AppointmentNotCreateException;
import ra.billingservice.exception.ResourceNotFoundException;
import ra.billingservice.repository.InvoiceRepository;
import ra.billingservice.service.BillingService;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentEventListener {
    private final BillingService billingService;
    private final InvoiceRepository invoiceRepository;

    @RabbitListener(queues = "appointment_created_queue")
    public void handleAppointmentCreated(Map<String, Object> message) {
        if (message == null || !message.containsKey("appointmentId") || !message.containsKey("patientId")) {
            log.warn("Lỗi khi tạo lịch hẹn!");
            throw new AppointmentNotCreateException("Lỗi khi tạo lịch hẹn!");
        }
        Long appointmentId = Long.parseLong(message.get("appointmentId").toString());
        Long patientId = Long.parseLong(message.get("patientId").toString());

        Boolean isInNetwork = message.containsKey("isInNetwork") ? Boolean.parseBoolean(message.get("isInNetwork").toString()) : true;
        String facilityLevel = message.containsKey("facilityLevel") ? message.get("facilityLevel").toString() : "DISTRICT";

        Double defaultConsultationFee = 100000.0;
        billingService.createInvoice(appointmentId, patientId, defaultConsultationFee, isInNetwork, facilityLevel);
    }

    @RabbitListener(queues = RabbitMQConfig.APPOINTMENT_CANCELLED_QUEUE)
    @Transactional
    public void handleAppointmentCancelled(Map<String, Object> message) {
        if (message == null || !message.containsKey("appointmentId")) {
            log.warn("Thông điệp hủy lịch hẹn không hợp lệ (thiếu appointmentId)!");
            return;
        }
        try {
            Long appointmentId = Long.parseLong(message.get("appointmentId").toString());
            Invoice invoice = invoiceRepository.findByAppointmentId(appointmentId)
                    .orElse(null);

            if (invoice == null) {
                log.warn("Không tìm thấy hóa đơn tương ứng với lịch hẹn ID: {}", appointmentId);
                return;
            }

            if ("CANCELLED".equalsIgnoreCase(invoice.getStatus())) {
                log.info("Hóa đơn ID {} đã ở trạng thái CANCELLED.", invoice.getId());
                return;
            }

            if ("PENDING".equalsIgnoreCase(invoice.getStatus())) {
                invoice.setStatus("CANCELLED");
                invoiceRepository.save(invoice);
                log.info("Đã hủy hóa đơn ID {} cho lịch hẹn ID {}", invoice.getId(), appointmentId);
            } else {
                log.warn("Không thể hủy hóa đơn ID {} vì đang ở trạng thái: {}", invoice.getId(), invoice.getStatus());
            }
        } catch (Exception e) {
            log.error("Lỗi xảy ra khi xử lý sự kiện hủy lịch hẹn: {}", e.getMessage(), e);
        }
    }
}
