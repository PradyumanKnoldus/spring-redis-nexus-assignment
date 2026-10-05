package com.nashtech.employeeredisapp.service;

import com.nashtech.employeeredisapp.dto.EmployeeRequest;
import com.nashtech.employeeredisapp.dto.EmployeeResponse;
import com.nashtech.employeeredisapp.entity.EmployeeEntity;
import com.nashtech.employeeredisapp.exception.EmployeeNotFoundException;
import com.nashtech.employeeredisapp.repository.EmployeeRepository;
import com.nashtech.employeelibrary.model.Employee;
import com.nashtech.employeelibrary.service.EmployeeUtility;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    @CachePut(cacheNames = "employees", key = "#result.id()")
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        Employee employee = toLibraryEmployee(request);
        validateEmployee(employee);
        return toResponse(toLibraryEmployee(employeeRepository.save(toEntity(employee))));
    }

    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(this::toLibraryEmployee)
                .map(this::toResponse)
                .toList();
    }

    @Cacheable(value = "employees", key = "#id", unless = "#result == null")
    public EmployeeResponse getEmployeeById(Long id) {
        return toResponse(toLibraryEmployee(findEmployee(id)));
    }

    @Transactional
    @CachePut(value = "employees", key = "#id")
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee employeeModel = toLibraryEmployee(request);
        validateEmployee(employeeModel);
        EmployeeEntity employeeEntity = findEmployee(id);
        applyEmployee(employeeEntity, employeeModel);
        return toResponse(toLibraryEmployee(employeeRepository.save(employeeEntity)));
    }

    @Transactional
    @CacheEvict(value = "employees", key = "#id")
    public void deleteEmployee(Long id) {
        employeeRepository.delete(findEmployee(id));
    }

    private EmployeeEntity findEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    private void validateEmployee(Employee employee) {
        if (!EmployeeUtility.isValidSalary(employee.getSalary())) {
            throw new IllegalArgumentException("Salary must be greater than or equal to 0");
        }
    }

    private Employee toLibraryEmployee(EmployeeRequest request) {
        return new Employee(null, request.name(), request.email(), request.department(), request.salary());
    }

    private Employee toLibraryEmployee(EmployeeEntity employee) {
        return new Employee(
                employee.getId(),
                employee.getName(),
                employee.getEmail(),
                employee.getDepartment(),
                employee.getSalary()
        );
    }

    private EmployeeEntity toEntity(Employee employee) {
        EmployeeEntity entity = new EmployeeEntity();
        applyEmployee(entity, employee);
        return entity;
    }

    private void applyEmployee(EmployeeEntity entity, Employee employee) {
        entity.setName(employee.getName());
        entity.setEmail(employee.getEmail());
        entity.setDepartment(employee.getDepartment());
        entity.setSalary(employee.getSalary());
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
            EmployeeUtility.generateEmployeeCode(employee.getId()),
                employee.getName(),
                employee.getEmail(),
                employee.getDepartment(),
                employee.getSalary()
        );
    }
}