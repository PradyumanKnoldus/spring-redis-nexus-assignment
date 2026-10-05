package com.nashtech.employeelibrary.service;

public final class EmployeeUtility {
    private EmployeeUtility() {
    }

    public static String generateEmployeeCode(Long id) {
        return "EMP-" + id;
    }

    public static boolean isValidSalary(Double salary) {
        return salary != null && salary >= 0;
    }
}