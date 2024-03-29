package com.quicksolve.proyecto.repository;


import com.quicksolve.proyecto.entity.Department;
import com.quicksolve.proyecto.entity.User;
import com.quicksolve.proyecto.entity.type.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT u from User u where u.department.id = ?1")
    List<User> getUsersByDepartment(long departmentId);

    @Query("SELECT u from User u where u.department.id IS NOT NULL")
    List<User> getUsersByAnyDepartment();

    List<User> findAllByType(UserType type);

    @Query("SELECT u from User u where u.username = ?1 or u.email = ?1")
    User getUserByEmailOrUsername(String emailOrUsername);
}
