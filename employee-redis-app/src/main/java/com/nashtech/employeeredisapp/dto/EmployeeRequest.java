package com.nashtech.employeeredisapp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmployeeRequest(
        @NotBlank(message = "Name is required")
        String name,
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,
        @NotBlank(message = "Department is required")
        String department,
        @NotNull(message = "Salary is required")
        @DecimalMin(value = "0.0", message = "Salary must be greater than or equal to 0")
        Double salary
) {
}