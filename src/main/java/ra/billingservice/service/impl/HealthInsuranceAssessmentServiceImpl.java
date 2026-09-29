package ra.billingservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ra.billingservice.dto.response.PatientResponse;
import ra.billingservice.service.HealthInsuranceAssessmentService;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthInsuranceAssessmentServiceImpl implements HealthInsuranceAssessmentService {

    @Override
    public Double calculateCoverageRate(PatientResponse patientResponse, Boolean isInNetwork, String facilityLevel) {
        if (patientResponse == null || patientResponse.getHealthInsuranceCode() == null || patientResponse.getHealthInsuranceCode().trim().length() < 2) {
            log.info("Bệnh nhân không có thẻ bảo hiểm y tế!");
            return 0.0;
        }
        LocalDate expiredDate = patientResponse.getHealthInsuranceExpiredDate();
        if (expiredDate != null && expiredDate.isBefore(LocalDate.now())) {
            log.warn("Thẻ bảo hiểm y tế có mã {} của bệnh nhân đã hết hạn ngày {}", patientResponse.getHealthInsuranceCode(), patientResponse.getHealthInsuranceExpiredDate());
            return 0.0;
        }
        String codePrefix = patientResponse.getHealthInsuranceCode().trim().substring(0, 2).toUpperCase();
        Double baseCoverageRate = 0.80;

        switch (codePrefix) {
            case "TE":
            case "CC":
            case "CA":
            case "QN":
            case "HN":
                baseCoverageRate = 1.0;
                break;
            case "HT":
            case "CN":
                baseCoverageRate = 0.95;
                break;
            case "DN":
            case "HS":
            case "GD":
            default:
                baseCoverageRate = 0.80;
                break;
        }
        if (!isInNetwork) {
            if ("DISTRICT".equalsIgnoreCase(facilityLevel)) {
                log.info("Khám trái tuyến Huyện! Hưởng 100% mức đúng tuyến ({})", baseCoverageRate);
            }
            else if("PROVINCIAL".equalsIgnoreCase(facilityLevel)) {
                log.info("Khám ngoại trú trái tuyến tỉnh! 0% BHYT");
                return 0.0;
            }
            else if ("CENTRAL".equalsIgnoreCase(facilityLevel)) {
                baseCoverageRate = baseCoverageRate * 0.40;
                log.info("Khám trái tuyến Trung ương! Hưởng 40% mức đúng tuyến ({})", baseCoverageRate);
            }
        }
        log.info("Mã thẻ: {} | Họ và tên: {} | Mức hưởng BHYT: {}%", patientResponse.getHealthInsuranceCode(), patientResponse.getFullName(), (int)(baseCoverageRate * 100));

        return baseCoverageRate;
    }
}
