package com.example.medassistant.agent;

public interface AppointmentChainService {
    String bookAppointmentChain(String userRequest, String model, Long userId);
    String confirmBooking(Long userId);
    String cancelBooking(Long userId);
}
