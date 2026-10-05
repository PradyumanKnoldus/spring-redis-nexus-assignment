package com.nashtech.employeeredisapp.dto;

public record EmployeeResponse(
        Long id,
        String employeeCode,
        String name,
        String email,
        String department,
        Double salary
) {
}