package com.example.medassistant.agent;

import com.example.medassistant.dto.agent.AppointmentSelection;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class PendingBookingStore {

    private final ConcurrentHashMap<Long, AppointmentSelection> pending = new ConcurrentHashMap<>();

    public void store(Long userId, AppointmentSelection selection) {
        pending.put(userId, selection);
    }

    public AppointmentSelection retrieve(Long userId) {
        return pending.get(userId);
    }

    public AppointmentSelection remove(Long userId) {
        return pending.remove(userId);
    }
}
