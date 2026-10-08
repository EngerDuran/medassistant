package com.example.medassistant.service;

import com.example.medassistant.dto.PatientInfo;

public interface PatientService {
    PatientInfo getPatientInfo(Long patientId);
}
