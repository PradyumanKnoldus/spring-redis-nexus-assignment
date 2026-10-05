package com.nashtech.employeeredisapp.repository;

import com.nashtech.employeeredisapp.entity.EmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<EmployeeEntity, Long> {
}