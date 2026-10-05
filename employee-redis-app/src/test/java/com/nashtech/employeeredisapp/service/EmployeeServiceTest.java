package com.nashtech.employeeredisapp.service;

import com.nashtech.employeeredisapp.dto.EmployeeRequest;
import com.nashtech.employeeredisapp.dto.EmployeeResponse;
import com.nashtech.employeeredisapp.entity.EmployeeEntity;
import com.nashtech.employeeredisapp.exception.EmployeeNotFoundException;
import com.nashtech.employeeredisapp.repository.EmployeeRepository;
import com.nashtech.employeelibrary.service.EmployeeUtility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void createEmployeePersistsAndReturnsResponse() {
        when(employeeRepository.save(any(EmployeeEntity.class))).thenAnswer(invocation -> {
            EmployeeEntity savedEmployee = invocation.getArgument(0);
            savedEmployee.setId(1L);
            return savedEmployee;
        });

        EmployeeResponse response = employeeService.createEmployee(request());

        assertEquals(expectedResponse(1L, "Jane Doe", "jane@example.com", "Engineering", 85000.0), response);
        verify(employeeRepository).save(any(EmployeeEntity.class));
    }

    @Test
    void getAllEmployeesMapsRepositoryResults() {
        when(employeeRepository.findAll()).thenReturn(List.of(employee(1L), employee(2L)));

        List<EmployeeResponse> responses = employeeService.getAllEmployees();

        assertEquals(List.of(
                expectedResponse(1L, "Jane Doe", "jane@example.com", "Engineering", 85000.0),
                expectedResponse(2L, "Jane Doe", "jane@example.com", "Engineering", 85000.0)
        ), responses);
        verify(employeeRepository).findAll();
    }

    @Test
    void getEmployeeByIdMapsRepositoryResult() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee(1L)));

        EmployeeResponse response = employeeService.getEmployeeById(1L);

        assertEquals(expectedResponse(1L, "Jane Doe", "jane@example.com", "Engineering", 85000.0), response);
        assertEquals(EmployeeUtility.generateEmployeeCode(1L), response.employeeCode());
        verify(employeeRepository).findById(1L);
    }

    @Test
    void updateEmployeePersistsAndReturnsUpdatedResponse() {
        EmployeeEntity existingEmployee = employee(1L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existingEmployee));
        when(employeeRepository.save(any(EmployeeEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        EmployeeRequest update = new EmployeeRequest(
                "Jane Smith", "jane.smith@example.com", "Product", 91000.0
        );

        EmployeeResponse response = employeeService.updateEmployee(1L, update);

        assertEquals(expectedResponse(1L, "Jane Smith", "jane.smith@example.com", "Product", 91000.0), response);
        verify(employeeRepository).findById(1L);
        verify(employeeRepository).save(existingEmployee);
    }

    @Test
    void deleteEmployeeRemovesRepositoryEntity() {
        EmployeeEntity existingEmployee = employee(1L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existingEmployee));

        employeeService.deleteEmployee(1L);

        verify(employeeRepository).findById(1L);
        verify(employeeRepository).delete(existingEmployee);
    }

    @Test
    void missingEmployeeThrowsNotFoundForReadUpdateAndDelete() {
        when(employeeRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> employeeService.getEmployeeById(404L));
        assertThrows(EmployeeNotFoundException.class, () -> employeeService.updateEmployee(404L, request()));
        assertThrows(EmployeeNotFoundException.class, () -> employeeService.deleteEmployee(404L));

        verify(employeeRepository, org.mockito.Mockito.times(3)).findById(404L);
    }

    @Test
    void negativeSalaryIsRejectedBeforeRepositoryAccess() {
        EmployeeRequest invalidRequest = new EmployeeRequest(
                "Jane Doe", "jane@example.com", "Engineering", -1.0
        );

        assertThrows(IllegalArgumentException.class, () -> employeeService.createEmployee(invalidRequest));
        assertThrows(IllegalArgumentException.class, () -> employeeService.updateEmployee(1L, invalidRequest));

        verifyNoInteractions(employeeRepository);
    }

    private EmployeeEntity employee(Long id) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setId(id);
        employee.setName("Jane Doe");
        employee.setEmail("jane@example.com");
        employee.setDepartment("Engineering");
        employee.setSalary(85000.0);
        return employee;
    }

    private EmployeeRequest request() {
        return new EmployeeRequest("Jane Doe", "jane@example.com", "Engineering", 85000.0);
    }

    private EmployeeResponse expectedResponse(Long id, String name, String email, String department, Double salary) {
        return new EmployeeResponse(
                id,
                EmployeeUtility.generateEmployeeCode(id),
                name,
                email,
                department,
                salary
        );
    }
}