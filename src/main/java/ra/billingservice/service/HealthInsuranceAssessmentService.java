package ra.billingservice.service;

import ra.billingservice.dto.response.PatientResponse;

public interface HealthInsuranceAssessmentService {
    Double calculateCoverageRate(PatientResponse patientResponse, Boolean isInNetwork, String facilityLevel);
}
