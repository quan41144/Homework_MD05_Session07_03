package ra.billingservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ra.billingservice.config.RabbitMQConfig;
import ra.billingservice.service.BillingService;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PrescriptionEventListener {
    private final BillingService billingService;

    @RabbitListener(queues = RabbitMQConfig.PRESCRIPTION_CREATED_QUEUE)
    public void handlePrescriptionCreated(Map<String, Object> message) {
        if (message == null || !message.containsKey("prescriptionId") || !message.containsKey("patientId") || !message.containsKey("totalAmount")) {
            log.warn("Thông điệp tạo đơn thuốc không hợp lệ!");
            return;
        }
        try {
            Long prescriptionId = Long.parseLong(message.get("prescriptionId").toString());
            Long patientId = Long.parseLong(message.get("patientId").toString());
            Double totalAmount = Double.parseDouble(message.get("totalAmount").toString());

            billingService.createMedicineInvoice(prescriptionId, patientId, totalAmount);
            log.info("Đã xử lý tạo Hóa đơn Thuốc cho Đơn thuốc ID {}", prescriptionId);
        } catch (Exception e) {
            log.error("Lỗi khi xử lý tạo Hóa đơn Thuốc: {}", e.getMessage(), e);
        }
    }
}
