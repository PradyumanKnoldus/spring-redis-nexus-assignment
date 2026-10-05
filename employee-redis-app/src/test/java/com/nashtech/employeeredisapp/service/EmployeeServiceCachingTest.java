package com.nashtech.employeeredisapp.service;

import com.nashtech.employeeredisapp.entity.EmployeeEntity;
import com.nashtech.employeeredisapp.dto.EmployeeRequest;
import com.nashtech.employeeredisapp.dto.EmployeeResponse;
import com.nashtech.employeeredisapp.exception.EmployeeNotFoundException;
import com.nashtech.employeeredisapp.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(EmployeeServiceCachingTest.TestConfig.class)
class EmployeeServiceCachingTest {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCacheAndMocks() {
        cacheManager.getCache("employees").clear();
        reset(employeeRepository);
    }

    @Test
    void repeatedLookupUsesCachedResponse() {
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(employee()));

        assertEquals(employeeService.getEmployeeById(7L), employeeService.getEmployeeById(7L));

        verify(employeeRepository, times(1)).findById(7L);
    }

    @Test
    void missingEmployeeIsNotCached() {
        when(employeeRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> employeeService.getEmployeeById(404L));
        assertThrows(EmployeeNotFoundException.class, () -> employeeService.getEmployeeById(404L));

        verify(employeeRepository, times(2)).findById(404L);
    }

    @Test
    void updateReplacesCachedResponseForNextLookup() {
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(employee()));
        when(employeeRepository.save(any(EmployeeEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        employeeService.getEmployeeById(7L);
        EmployeeRequest update = new EmployeeRequest(
                "Jane Smith",
                "jane.smith@example.com",
                "Product",
                91000.0
        );

        EmployeeResponse updated = employeeService.updateEmployee(7L, update);
        EmployeeResponse cachedAfterUpdate = employeeService.getEmployeeById(7L);

        assertEquals(updated, cachedAfterUpdate);
        assertEquals("Jane Smith", cachedAfterUpdate.name());
        verify(employeeRepository, times(2)).findById(7L);
        verify(employeeRepository, times(1)).save(any(EmployeeEntity.class));
    }

    @Test
    void deleteEvictsCachedResponseSoNextLookupReturnsNotFound() {
        when(employeeRepository.findById(7L)).thenReturn(
                Optional.of(employee()),
                Optional.of(employee()),
                Optional.empty()
        );

        employeeService.getEmployeeById(7L);
        employeeService.deleteEmployee(7L);

        assertThrows(EmployeeNotFoundException.class, () -> employeeService.getEmployeeById(7L));

        verify(employeeRepository, times(3)).findById(7L);
        verify(employeeRepository).delete(any(EmployeeEntity.class));
    }

    @Test
    void deletingMissingEmployeeThrowsNotFound() {
        when(employeeRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> employeeService.deleteEmployee(404L));

        verify(employeeRepository, never()).delete(any(EmployeeEntity.class));
    }

    private EmployeeEntity employee() {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setId(7L);
        employee.setName("Jane Doe");
        employee.setEmail("jane@example.com");
        employee.setDepartment("Engineering");
        employee.setSalary(85000.0);
        return employee;
    }

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        EmployeeRepository employeeRepository() {
            return mock(EmployeeRepository.class);
        }

        @Bean
        EmployeeService employeeService(EmployeeRepository employeeRepository) {
            return new EmployeeService(employeeRepository);
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("employees");
        }
    }
}