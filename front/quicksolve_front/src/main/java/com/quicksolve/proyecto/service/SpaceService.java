package com.quicksolve.proyecto.service;

import com.quicksolve.proyecto.dto.DepartmentDTO;
import com.quicksolve.proyecto.dto.SpaceDTO;

import java.util.List;

public interface SpaceService {

    List<SpaceDTO> list();
    SpaceDTO findById(Long id);
}
