package ra.billingservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {
    public static final String BILLING_EXCHANGE = "billing_exchange";
    public static final String PAYMENT_SUCCESS_QUEUE = "payment_success_queue";
    public static final String PAYMENT_FAILED_QUEUE = "payment_failed_queue";
    public static final String ROUTING_PAYMENT_SUCCESS = "payment.success";
    public static final String ROUTING_PAYMENT_FAILED = "payment.failed";
    public static final String INVOICE_DELAY_EXCHANGE = "invoice_delay_exchange";
    public static final String INVOICE_DELAY_QUEUE = "invoice_delay_queue";
    public static final String INVOICE_DLX_EXCHANGE = "invoice_dlx_exchange";
    public static final String INVOICE_CANCEL_QUEUE = "invoice_cancel_queue";
    public static final String ROUTING_INVOICE_DELAY = "invoice.delay";
    public static final String ROUTING_INVOICE_CANCEL = "invoice.cancel";
    public static final int INVOICE_TTL_15_MINUTES_MS = 300000;
    public static final String APPOINTMENT_EXCHANGE = "appointment_exchange";
    public static final String APPOINTMENT_CANCELLED_QUEUE = "appointment_cancelled_queue";
    public static final String ROUTING_APPOINTMENT_CANCELLED = "appointment.cancelled";

    @Bean
    public TopicExchange billingExchange() {
        return new TopicExchange(BILLING_EXCHANGE);
    }
    @Bean
    public Queue paymentSuccessQueue() {
        return new Queue(PAYMENT_SUCCESS_QUEUE, true);
    }
    @Bean
    public Queue paymentFailedQueue() {
        return new Queue(PAYMENT_FAILED_QUEUE, true);
    }
    @Bean
    public Binding bindingPaymentSuccess() {
        return BindingBuilder.bind(paymentSuccessQueue()).to(billingExchange()).with(ROUTING_PAYMENT_SUCCESS);
    }
    @Bean
    public Binding bindingPaymentFailed() {
        return BindingBuilder.bind(paymentFailedQueue()).to(billingExchange()).with(ROUTING_PAYMENT_FAILED);
    }
    @Bean
    public DirectExchange invoiceDelayExchange() {
        return new DirectExchange(INVOICE_DELAY_EXCHANGE);
    }
    @Bean
    public Queue invoiceDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", INVOICE_DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", ROUTING_INVOICE_CANCEL);
        args.put("x-message-ttl", INVOICE_TTL_15_MINUTES_MS);
        return new Queue(INVOICE_DELAY_QUEUE, true, false, false, args);
    }
    @Bean
    public Binding bindingInvoiceDelayQueue() {
        return BindingBuilder.bind(invoiceDelayQueue()).to(invoiceDelayExchange()).with(ROUTING_INVOICE_DELAY);
    }
    @Bean
    public DirectExchange invoiceDlxExchange() {
        return new DirectExchange(INVOICE_DLX_EXCHANGE);
    }
    @Bean
    public Queue invoiceCancelQueue() {
        return new Queue(INVOICE_CANCEL_QUEUE, true);
    }
    @Bean
    public Binding bindingInvoiceCancelQueue() {
        return BindingBuilder.bind(invoiceCancelQueue()).to(invoiceDlxExchange()).with(ROUTING_INVOICE_CANCEL);
    }
    public static final String APPOINTMENT_CREATED_QUEUE = "appointment_created_queue";
    public static final String ROUTING_APPOINTMENT_CREATED = "appointment.created";

    @Bean
    public TopicExchange appointmentExchange() {
        return new TopicExchange(APPOINTMENT_EXCHANGE);
    }
    @Bean
    public Queue appointmentCreatedQueue() {
        return new Queue(APPOINTMENT_CREATED_QUEUE, true);
    }
    @Bean
    public Binding bindingAppointmentCreatedQueue() {
        return BindingBuilder.bind(appointmentCreatedQueue()).to(appointmentExchange()).with(ROUTING_APPOINTMENT_CREATED);
    }
    @Bean
    public Queue appointmentCancelledQueue() {
        return new Queue(APPOINTMENT_CANCELLED_QUEUE, true);
    }
    @Bean
    public Binding bindingAppointmentCancelledQueue() {
        return BindingBuilder.bind(appointmentCancelledQueue()).to(appointmentExchange()).with(ROUTING_APPOINTMENT_CANCELLED);
    }
    public static final String PRESCRIPTION_EXCHANGE = "prescription_exchange";
    public static final String PRESCRIPTION_CREATED_QUEUE = "prescription_created_queue";
    public static final String ROUTING_PRESCRIPTION_CREATED = "prescription.created";

    @Bean
    public TopicExchange prescriptionExchange() {
        return new TopicExchange(PRESCRIPTION_EXCHANGE);
    }
    @Bean
    public Queue prescriptionCreatedQueue() {
        return new Queue(PRESCRIPTION_CREATED_QUEUE, true);
    }
    @Bean
    public Binding bindingPrescriptionCreatedQueue() {
        return BindingBuilder.bind(prescriptionCreatedQueue()).to(prescriptionExchange()).with(ROUTING_PRESCRIPTION_CREATED);
    }
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
