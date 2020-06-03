package com.employee.repository;

import com.employee.model.Employee;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends CrudRepository<Employee, Long> {

    List<Employee> findByName(String name);

    Employee findById(long id);

    @Query("from Employee e where e.salary between 0 and :salary")
    List<Employee> findByEmployeesBySalary(Double salary);

}