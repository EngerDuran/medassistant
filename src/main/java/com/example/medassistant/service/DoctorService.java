package com.example.medassistant.service;

import com.example.medassistant.dto.DoctorInfo;
import com.example.medassistant.model.Doctor;
import com.example.medassistant.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * Servicio de dominio para la búsqueda y resolución de información médica.
 * Aplica filtros multicriterio en BD y transforma las entidades JPA en DTOs DoctorInfo.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorService {

    private final DoctorRepository doctorRepository;

    public List<DoctorInfo> searchDoctors(String query) {

        return doctorRepository
                .findByFirstNameIgnoreCaseOrLastNameContainingIgnoreCaseOrSpecialtyContainingIgnoreCase(
                    query, query, query
                )
                .stream()
                .map(this::toDoctorInfo)
                .toList();


    }

    private DoctorInfo toDoctorInfo(Doctor doctor) {
        return  new DoctorInfo(
                doctor.getFirstName(),
                doctor.getLastName(),
                doctor.getSpecialty(),
                doctor.getLicenseNumber(),
                doctor.getPhone(),
                doctor.getOffice());

    }

}
