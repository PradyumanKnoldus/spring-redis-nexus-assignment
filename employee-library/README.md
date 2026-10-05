# Employee Library

A reusable Java 17 Maven JAR containing an employee model and utility methods. This library does not depend on Spring Boot.

## Build

```bash
mvn clean package
```

The generated artifact is `target/employee-library-1.0.0.jar`.

## Usage

```java
Employee employee = new Employee(1L, "Alex Smith", "alex@example.com", "Engineering", 75000.0);
String employeeCode = EmployeeUtility.generateEmployeeCode(employee.getId());
boolean validSalary = EmployeeUtility.isValidSalary(employee.getSalary());
```