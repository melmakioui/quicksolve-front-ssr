package com.quicksolve.proyecto.entity;

import com.quicksolve.proyecto.entity.type.MethodType;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"departmentLanguage"})
@ToString(exclude = {"departmentLanguage"})
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Enumerated(EnumType.STRING)
    private MethodType type;

    @OneToMany(mappedBy = "department")
    private Set<User> users;

    @OneToMany(mappedBy = "department")
    private Set<DepartmentLanguage> departmentLanguage;

}
