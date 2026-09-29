package com.example.medassistant.repository;

import com.example.medassistant.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Data Access Layer para la gestión del personal médico.
 *
 * Expone métodos derivados de consulta (Spring Data JPA derived queries) optimizados
 * para ser consumidos directamente por Function Calling / AI Tools:
 * - Búsqueda insensible a mayúsculas/minúsculas para mitigar variaciones del LLM.
 * - Resolución difusa multicriterio (nombre, apellido o especialidad) para consultas de lenguaje natural.
 */

public interface DoctorRepository  extends JpaRepository<Doctor, Long> {

    List<Doctor> findBySpecialtyIgnoreCase(String specialty);

    List<Doctor> findByFirstNameIgnoreCaseOrLastNameContainingIgnoreCaseOrSpecialtyContainingIgnoreCase(
            String firstName, String lastName, String specialty);


}
