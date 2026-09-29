package ra.billingservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ra.billingservice.dto.response.ApiResponse;
import ra.billingservice.dto.response.PatientResponse;

@FeignClient(name = "PATIENT-SERVICE")
public interface PatientServiceClient {
    @GetMapping("/api/v1/patients/{patientId}")
    ApiResponse<PatientResponse> getPatientById(@PathVariable Long patientId);
}