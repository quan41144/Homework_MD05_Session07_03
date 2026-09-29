package ra.billingservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ra.billingservice.config.RabbitMQConfig;
import ra.billingservice.entity.Invoice;
import ra.billingservice.exception.IdInvalidException;
import ra.billingservice.exception.InvoiceNotCancelException;
import ra.billingservice.exception.ResourceNotFoundException;
import ra.billingservice.repository.InvoiceRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceCancellationListener {
    private final InvoiceRepository invoiceRepository;

    @RabbitListener(queues = RabbitMQConfig.INVOICE_CANCEL_QUEUE)
    @Transactional
    public void handleInvoiceCancellation(Long invoiceId) {
        if (invoiceId == null || invoiceId <= 0) {
            log.error("ID hóa đơn không hợp lệ!");
            throw new IdInvalidException("ID hóa đơn không hợp lệ!");
        }
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn ID: " + invoiceId));
        if ("PENDING".equalsIgnoreCase(invoice.getStatus())) {
            invoice.setStatus("CANCELLED");
            invoiceRepository.save(invoice);
            log.info("Đã xóa hóa đơn do hết hạn thời gian thanh toán!");
        }
        else {
            log.info("Không thực hiện hủy hóa đơn. Hóa đơn ID {} đang ở trạng thái {}", invoiceId, invoice.getStatus());
        }
    }
}
