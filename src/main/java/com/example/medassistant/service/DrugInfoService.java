package com.example.medassistant.service;

import com.example.medassistant.dto.DrugInfo;

public interface DrugInfoService {
    DrugInfo getDrugInfo(String drugName);
}
