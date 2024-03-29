package com.quicksolve.proyecto.service.implementation;

import com.quicksolve.proyecto.dto.DepartmentDTO;
import com.quicksolve.proyecto.dto.FullIncidenceDTO;
import com.quicksolve.proyecto.dto.FullUserDTO;
import com.quicksolve.proyecto.entity.*;
import com.quicksolve.proyecto.entity.type.MethodType;
import com.quicksolve.proyecto.entity.type.UserType;
import com.quicksolve.proyecto.mapper.IncidenceMapper;
import com.quicksolve.proyecto.mapper.UserMapper;
import com.quicksolve.proyecto.repository.*;
import com.quicksolve.proyecto.service.IncidenceService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class IncidenceServiceImpl implements IncidenceService {

    private final Long INCIDENCE_WAITING_STATE = 1L;
    private final Long INCIDENCE_SOLVED_STATE = 3L;
    private final Long INCIDENCE_CANCELLED_STATE = 4L;
    private int rrCount = 0;
    private FullIncidenceDTO lastIncidence = null;
    private FullIncidenceDTO lastUpdatedIncidence = null;

    @Autowired
    private IncidenceRepository incidenceRepository;

    @Autowired
    private UserRepository userRepo;
    @Autowired
    private UserIncidenceRepository userIncidenceRepo;

    @Autowired
    private GeneralDepartmentConfigurationRepository genDepConfRepo;

    @Autowired
    private IncidenceStateRepository incidenceStateRepository;

    @Autowired
    private IncidenceFileRepository incidenceFileRepository;

    @Autowired
    private DepartmentRepository departmentRepo;

    public List<FullIncidenceDTO> list() {
        return incidenceRepository.findAll().stream().map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FullIncidenceDTO> list(FullUserDTO userDTO) {

        System.out.println(UserMapper.INSTANCE.DTOtoUser(userDTO));

        List<Incidence> incidences = userIncidenceRepo.findAllByUser(UserMapper.INSTANCE.DTOtoUser(userDTO))
                .stream()
                .map(UserIncidence::getIncidence)
                .toList();

        return incidences.stream().map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FullIncidenceDTO> listByAssignedTech(FullUserDTO userDTO){
        List<Incidence> incidences = userIncidenceRepo.findAllByTech(UserMapper.INSTANCE.DTOtoUser(userDTO))
                .stream()
                .map(UserIncidence::getIncidence)
                .toList();
        List<FullIncidenceDTO> incidencesDTO = incidences.stream().map(this::convertToDTO).toList();
        incidencesDTO.forEach(i -> i.setPriority(getPriority(i.getUser())));
        return incidencesDTO;
    }

    private long getPriority(FullUserDTO userDTO){
        if (userDTO == null) return -1;
        return userDTO.getService() == null ? 0L : userDTO.getService().getId();
    }

    @Override
    public List<FullIncidenceDTO> list(Long departmentId, Long spaceId, String startDate, FullUserDTO userDTO) {

        List<Incidence> incidences = userIncidenceRepo.findAllByUser(UserMapper.INSTANCE.DTOtoUser(userDTO))
                .stream()
                .map(UserIncidence::getIncidence)
                .toList();

        List<Incidence> result = incidences.stream().filter( incidence -> {
            if (startDate == null || startDate.isEmpty()) return true;
            LocalDateTime dateStart = incidence.getDateStart();
            LocalDate localDate = dateStart.toLocalDate();
            LocalDate parse = LocalDate.parse(startDate);
            return localDate.isEqual(parse);
        })
                .filter(i -> {
                    if (departmentId == 0) return true;
                    return i.getDepartment() != null && i.getDepartment().getId() == departmentId;
                })

                .filter(i -> {
                    if (spaceId == 0) return true;
                    return i.getSpace() != null && i.getSpace().getId() == spaceId;
                }).toList();


        return result.stream().map(this::convertToDTO)
                .collect(Collectors.toList());
    }


    @Override
    public FullIncidenceDTO findById(long id) {
        Incidence incidence = incidenceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontro la incidencia"));
        return convertToDTO(incidence);
    }

    @Override
    public List<FullIncidenceDTO> listIncidencesByStateAndSearch(long id, String search){
        List<Incidence> incidences = (userIncidenceRepo.findByIncidenceState(id, search))
                .stream()
                .map(UserIncidence::getIncidence)
                .toList();
        return incidences.stream().map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void save(FullIncidenceDTO fullIncidenceDTO) {
        Incidence incidence = saveIncidence(fullIncidenceDTO);
        UserIncidence userIncidence = new UserIncidence();
        userIncidence = assignIncidenceToTech(userIncidence, fullIncidenceDTO);

        userIncidence.setIncidence(incidence);
        userIncidenceRepo.save(userIncidence);
        lastIncidence = convertToDTO(incidence);
    }

    @Override
    public void save(FullIncidenceDTO fullIncidenceDTO, FullUserDTO userDTO) {
        Incidence incidence = saveIncidence(fullIncidenceDTO);
        lastIncidence = convertToDTO(incidence);
        UserIncidence userIncidence = new UserIncidence();
        userIncidence = assignIncidenceToTech(userIncidence, fullIncidenceDTO);

        userIncidence.setIncidence(incidence);
        userIncidence.setUser(UserMapper.INSTANCE.DTOtoUser(userDTO));
        userIncidenceRepo.save(userIncidence);
    }

    private UserIncidence assignIncidenceToTech(UserIncidence userIncidence, FullIncidenceDTO fullIncidenceDTO){
        userIncidence = fullIncidenceDTO.getDepartment() == null ?
                assignIncidence(userIncidence, null) :
                assignIncidence(userIncidence, departmentRepo.getReferenceById(fullIncidenceDTO.getDepartment().getId()));
        return userIncidence;
    }

    private UserIncidence assignIncidence(UserIncidence userIncidence, Department department){
        String method = department != null && !department.getUsers().isEmpty() ?
                department.getType().name :
                genDepConfRepo.getReferenceById(1L).getType().name;

        if (method.equals(MethodType.ROUND_ROBIN.name)){
            return rrMethod(department, userIncidence);
        }
        if (method.equals(MethodType.LESS_INCIDENCES_TECH.name)){
            return lessIncidencesTechMethod(department, userIncidence);
        }

        return new UserIncidence();
    }

    private UserIncidence rrMethod(Department department, UserIncidence userIncidence){
        List<User> usersDep = department == null ?
                userRepo.getUsersByAnyDepartment() :
                userRepo.getUsersByDepartment(department.getId());
        userIncidence.setTech(usersDep.get(rrCount));

        if (rrCount < (usersDep.size() - 1)){
            rrCount++;
        } else if (rrCount == (usersDep.size() - 1)){
            rrCount = 0;
        }
        return userIncidence;
    }

    private UserIncidence lessIncidencesTechMethod(Department department, UserIncidence userIncidence){
        userIncidence.setTech(department == null ?
                userIncidenceRepo.findByLessIncidencesTech() :
                userIncidenceRepo.findByLessIncidencesTech(department.getId()));
        return userIncidence;
    }

    /**
     * @param fullIncidenceDTO
     * Comprueba si la incidencia tiene espacio y departamento asignado, si no los tiene los asigna null
     * Assigna el estado de la incidencia a "Esperando" y la fecha de creacion a la fecha actual
     * * @return Incidence
     */

    private Incidence saveIncidence(FullIncidenceDTO fullIncidenceDTO) {
        checkDepartmentAndSpace(fullIncidenceDTO);
        Incidence incidence = IncidenceMapper.INSTANCE.dtoToIncidence(fullIncidenceDTO);
        incidence.setDateStart(LocalDateTime.now());
        IncidenceState waitingState = incidenceStateRepository.getReferenceById(INCIDENCE_WAITING_STATE);
        incidence.setIncidenceState(waitingState);
        return incidenceRepository.save(incidence);
    }

    @Override
    @Transactional
    public void delete(long id) {
        Incidence incidence = incidenceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontro la incidencia"));

        if (incidence.getIncidenceState().getId() != INCIDENCE_WAITING_STATE) {
            throw new RuntimeException("No se puede eliminar una incidencia que no esta en estado de espera");
        }

        userIncidenceRepo.deleteByIncidenceId(incidence.getId());
        incidenceFileRepository.deleteAllByIncidenceId(incidence.getId());
    }

    @Override
    public void cancel(long id) {
        Incidence incidence = incidenceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontro la incidencia"));
        incidence.setIncidenceState(incidenceStateRepository.getReferenceById(INCIDENCE_CANCELLED_STATE));
        incidence.setDateEnd(LocalDateTime.now());
        incidenceRepository.save(incidence);
    }

    @Override
    public void update(FullIncidenceDTO fullIncidenceDTO) {

        checkDepartmentAndSpace(fullIncidenceDTO);

        Incidence incidence = incidenceRepository.findById(fullIncidenceDTO.getId())
                .orElseThrow(() -> new RuntimeException("No se encontro la incidencia"));

        if (incidence.getIncidenceState().getId() != INCIDENCE_WAITING_STATE) {
            throw new RuntimeException("No se puede modificar una incidencia que no esta en estado de espera");
        }

        Incidence incidenceToUpdate = IncidenceMapper.INSTANCE.dtoToIncidence(fullIncidenceDTO);
        incidenceToUpdate.setDateStart(incidence.getDateStart());
        incidenceToUpdate.setIncidenceState(incidence.getIncidenceState());
        Incidence updatedIncidence = incidenceRepository.save(incidenceToUpdate);
        lastUpdatedIncidence = convertToDTO(updatedIncidence);
    }

    @Override
    public FullIncidenceDTO getLastIncidence() {
        return lastIncidence;
    }

    @Override
    public FullIncidenceDTO getLastUpdatedIncidence() {
        return lastUpdatedIncidence;
    }

    @Override
    public FullIncidenceDTO findIncidenceByIdAndUserId(long incidenceId, FullUserDTO userDTO) {
        UserIncidence userIncidence = userIncidenceRepo.findByIncidenceIdAndUserId(incidenceId,userDTO.getId());

        if (userDTO.getType() == UserType.DEP_HEAD){
            UserIncidence incidence = userIncidenceRepo.findByIncidenceId(incidenceId);
            return convertToDTO(incidence.getIncidence());
        }

        if(userDTO.getType() == UserType.TECH && userIncidence == null){
            UserIncidence techIncidence = userIncidenceRepo.findByIncidenceIdAndTechId(incidenceId,userDTO.getId());
            if(techIncidence == null){
                throw new RuntimeException("No esta asignado a esta incidencia");
            }
            return convertToDTO(techIncidence.getIncidence());
        }

        if (userIncidence == null){
            throw new RuntimeException("No es usuario de la incidencia");
        }

        return convertToDTO(userIncidence.getIncidence());
    }

    @Override
    public void changeState(long incidenceId, long stateId) {

        Incidence incidence = incidenceRepository.findById(incidenceId)
                .orElseThrow(() -> new RuntimeException("No se encontro la incidencia"));

        IncidenceState state = incidenceStateRepository.findById(stateId)
                        .orElseThrow(() -> new RuntimeException("No se encontro el estado"));

        if (stateId < state.getId())
            throw new RuntimeException("No se puede cambiar a un estado anterior");

        if (stateId == INCIDENCE_SOLVED_STATE || stateId == INCIDENCE_CANCELLED_STATE) {
            incidence.setDateEnd(LocalDateTime.now());
        }

        incidence.setIncidenceState(state);
        incidenceRepository.save(incidence);
    }

    private void checkDepartmentAndSpace(FullIncidenceDTO fullIncidenceDTO) {
        long departmentId = fullIncidenceDTO.getDepartment().getId();
        long spaceId = fullIncidenceDTO.getSpace().getId();

        if (departmentId == -1) fullIncidenceDTO.setDepartment(null);
        if (spaceId == -1) fullIncidenceDTO.setSpace(null);
    }

    private FullIncidenceDTO convertToDTO(Incidence incidence) {

        FullIncidenceDTO fullIncidenceDTO = IncidenceMapper.INSTANCE.incidenceToDTO(incidence);
        UserIncidence ui = userIncidenceRepo.findByIncidenceId(incidence.getId());

        if (ui == null){
            fullIncidenceDTO.setUser(null);
        }else{
            fullIncidenceDTO.setUser(UserMapper.INSTANCE.userToDTO(ui.getUser()));
        }

        return fullIncidenceDTO;
    }
}
