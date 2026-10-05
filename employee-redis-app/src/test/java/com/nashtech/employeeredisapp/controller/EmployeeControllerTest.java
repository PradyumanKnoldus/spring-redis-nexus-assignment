package com.nashtech.employeeredisapp.controller;

import com.nashtech.employeeredisapp.dto.EmployeeResponse;
import com.nashtech.employeeredisapp.exception.EmployeeNotFoundException;
import com.nashtech.employeeredisapp.exception.GlobalExceptionHandler;
import com.nashtech.employeeredisapp.service.EmployeeService;
import com.nashtech.employeelibrary.service.EmployeeUtility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
@Import(GlobalExceptionHandler.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeService employeeService;

    @Test
    void createEmployeeReturnsCreatedAndLocation() throws Exception {
        when(employeeService.createEmployee(any())).thenReturn(employee());

        mockMvc.perform(post("/api/employees")
                        .contentType("application/json")
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/employees/1")))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getAllEmployeesReturnsList() throws Exception {
        when(employeeService.getAllEmployees()).thenReturn(List.of(employee()));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("jane@example.com"));
    }

    @Test
    void getEmployeeReturnsEmployee() throws Exception {
        when(employeeService.getEmployeeById(1L)).thenReturn(employee());

        mockMvc.perform(get("/api/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane Doe"))
                .andExpect(jsonPath("$.employeeCode").value(EmployeeUtility.generateEmployeeCode(1L)));
    }

    @Test
    void updateEmployeeReturnsUpdatedEmployee() throws Exception {
        when(employeeService.updateEmployee(eq(1L), any())).thenReturn(employee());

        mockMvc.perform(put("/api/employees/1")
                        .contentType("application/json")
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department").value("Engineering"));
    }

    @Test
    void deleteEmployeeReturnsNoContent() throws Exception {
        doNothing().when(employeeService).deleteEmployee(1L);

        mockMvc.perform(delete("/api/employees/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void missingEmployeeReturnsNotFound() throws Exception {
        when(employeeService.getEmployeeById(42L)).thenThrow(new EmployeeNotFoundException(42L));

        mockMvc.perform(get("/api/employees/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Employee not found with id: 42"))
                .andExpect(jsonPath("$.path").value("/api/employees/42"));
    }

    @Test
    void updatingMissingEmployeeReturnsNotFound() throws Exception {
        doThrow(new EmployeeNotFoundException(42L))
                .when(employeeService).updateEmployee(eq(42L), any());

        mockMvc.perform(put("/api/employees/42")
                        .contentType("application/json")
                        .content(validRequest()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingMissingEmployeeReturnsNotFound() throws Exception {
        doThrow(new EmployeeNotFoundException(42L)).when(employeeService).deleteEmployee(42L);

        mockMvc.perform(delete("/api/employees/42"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidRequestReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType("application/json")
                        .content("""
                                {"name":" ","email":"not-an-email","department":" ","salary":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("name")))
                .andExpect(jsonPath("$.message").value(containsString("email")))
                .andExpect(jsonPath("$.message").value(containsString("department")))
                .andExpect(jsonPath("$.message").value(containsString("salary")))
                .andExpect(jsonPath("$.path").value("/api/employees"));
            }

            @Test
            void malformedJsonReturnsConsistentInvalidRequestResponse() throws Exception {
            mockMvc.perform(post("/api/employees")
                    .contentType("application/json")
                    .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request"))
                .andExpect(jsonPath("$.path").value("/api/employees"));
            }

            @Test
            void unexpectedExceptionDoesNotExposeInternalDetails() throws Exception {
            when(employeeService.getEmployeeById(99L))
                .thenThrow(new IllegalStateException("database password leaked"));

            mockMvc.perform(get("/api/employees/99"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.path").value("/api/employees/99"));
    }

    private EmployeeResponse employee() {
        return new EmployeeResponse(
            1L,
            EmployeeUtility.generateEmployeeCode(1L),
            "Jane Doe",
            "jane@example.com",
            "Engineering",
            85000.0
        );
    }

    private String validRequest() {
        return """
                {"name":"Jane Doe","email":"jane@example.com","department":"Engineering","salary":85000}
                """;
    }
}