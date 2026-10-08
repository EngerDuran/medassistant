package com.example.medassistant.service;

import com.example.medassistant.dto.AppointmentInfo;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {

    public List<AppointmentInfo> findAvailableAppointments(String specialty, LocalDate date);
}
