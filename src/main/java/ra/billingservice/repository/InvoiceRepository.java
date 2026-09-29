package ra.billingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ra.billingservice.entity.Invoice;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByAppointmentId(Long appointmentId);
    List<Invoice> findByPatientId(Long patientId);
}
