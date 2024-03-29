package com.quicksolve.proyecto.controller;

import com.quicksolve.proyecto.dto.*;
import com.quicksolve.proyecto.entity.type.UserType;
import com.quicksolve.proyecto.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.AccessDeniedException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;

@Controller
@SessionAttributes({"userlogin"})
public class IncidenceController {

    @Autowired
    private IncidenceService incidenceService;
    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private SpaceService spaceService;
    @Autowired
    private IncidenceStateService incidenceStateService;
    @Autowired
    private IncidenceFileService incidenceFileService;
    @Autowired
    private IncidenceMessageService messageService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserIncidenceService userIncidenceService;
    @Autowired
    private HistoryService historyService;

    private final Long INCIDENCE_WAITING_STATE = 1L;
    private final Long INCIDENCE_SOLVE_STATE = 3L;
    private final Long INCIDENCE_CANCELLED_STATE = 4L;

    @GetMapping("/incidencia/nueva")
    public String showForm(Model model) {

        FullIncidenceDTO incidenceDTO = new FullIncidenceDTO();
        incidenceDTO.setDepartment(new DepartmentDTO());
        incidenceDTO.setSpace(new SpaceDTO());

        List<DepartmentDTO> departments = departmentService.list();
        List<SpaceDTO> spaces = spaceService.list();

        model.addAttribute("spaces", spaces);
        model.addAttribute("departments", departments);
        model.addAttribute("incidence", incidenceDTO);
        model.addAttribute("isNewIncidence", true);

        return "view/incidenceForm";
    }

    @GetMapping("/incidencia/modificar/{id}")
    public String showUpdateForm(@PathVariable long id, Model model) {
        model.addAttribute("departments", departmentService.list());
        model.addAttribute("spaces", spaceService.list());
        FullIncidenceDTO incidence = incidenceService.findById(id);

        if (incidence.getIncidenceState().getId() > INCIDENCE_WAITING_STATE) {
            throw new RuntimeException("No se puede modificar una incidencia que no esté en estado pendiente");
        }
        incidence.setIncidenceFiles(incidenceFileService.findAllByIncidenceId(id));
        model.addAttribute("incidence", incidence);
        model.addAttribute("isNewIncidence", false);
        return "view/incidenceForm";
    }

    @GetMapping("/public/nueva")
    public String showNoUserForm(Model model) {

        if (model.getAttribute("userlogin") != null) return "redirect:/incidencia/nueva";


        FullIncidenceDTO fullIncidenceDTO = new FullIncidenceDTO();
        fullIncidenceDTO.setDepartment(new DepartmentDTO());
        fullIncidenceDTO.setSpace(new SpaceDTO());

        model.addAttribute("departments", departmentService.list());
        model.addAttribute("spaces", spaceService.list());
        model.addAttribute("incidence", new FullIncidenceDTO());
        return "view/incidenceNoLoginForm";
    }

    @GetMapping("/incidencias")
    public String showIncidences(Model model) {

        List<FullIncidenceDTO> incidenceDTOS = incidenceService.list((FullUserDTO) model.getAttribute("userlogin"));

        incidenceDTOS.forEach(incidence -> {
            incidence.setDepartment(departmentService.findById(incidence.getDepartmentId()));
            incidence.setSpace(spaceService.findById(incidence.getSpaceId()));
            incidence.setIncidenceState(incidenceStateService.findById(incidence.getIncidenceStateId()));
            incidence.setIncidenceFiles(incidenceFileService.findAllByIncidenceId(incidence.getId()));
        });

        Collections.reverse(incidenceDTOS);

        model.addAttribute("departments", departmentService.list());
        model.addAttribute("spaces", spaceService.list());
        model.addAttribute("incidences", incidenceDTOS);
        model.addAttribute("status", incidenceStateService.list());
        model.addAttribute("isFilter", false);

        System.out.println(historyService.getHistoryByIncidenceId(4L));

        return "view/incidences";
    }

    @PostMapping("/incidencias")
    public String showFilterIncidences(@RequestParam String department, @RequestParam String space, @RequestParam String dateStart, Model model) {

        Long departmentId = null;
        Long spaceId = null;

        System.out.println(dateStart);
        try {
            departmentId = Long.parseLong(department);
            spaceId = Long.parseLong(space);
        } catch (NumberFormatException error) {
            System.out.println("Error");
            return "redirect:/incidencias";
        }

        if (!dateStart.isEmpty()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            dateFormat.setLenient(false);
            try {
                dateFormat.parse(dateStart.trim());
            } catch (ParseException e) {
                System.out.println("error 2");
                return "redirect:/incidencias";
            }
        }

        List<FullIncidenceDTO> incidences = incidenceService.list(departmentId, spaceId, dateStart, (FullUserDTO) model.getAttribute("userlogin"));
        incidences.forEach(incidence -> {
            incidence.setDepartment(departmentService.findById(incidence.getDepartmentId()));
            incidence.setSpace(spaceService.findById(incidence.getSpaceId()));
            incidence.setIncidenceState(incidenceStateService.findById(incidence.getIncidenceStateId()));
            incidence.setIncidenceFiles(incidenceFileService.findAllByIncidenceId(incidence.getId()));
        });
        System.out.println(incidences);
        model.addAttribute("departments", departmentService.list());
        model.addAttribute("spaces", spaceService.list());
        model.addAttribute("incidences", incidences);
        model.addAttribute("status", incidenceStateService.list());
        model.addAttribute("isFilter", true);

        return "view/incidences";
    }

    @GetMapping("/incidencia/{incidenceId}")
    public String showIncidence(@PathVariable long incidenceId, Model model) {

        FullIncidenceDTO cancelledIncidence = incidenceService.findById(incidenceId);
        FullUserDTO user = (FullUserDTO) model.getAttribute("userlogin");
        FullUserDTO owner = null;

        if (userIncidenceService.findByIncidenceId(incidenceId).getUser() != null) {
            owner = userService.getUserBy(userIncidenceService.findByIncidenceId(incidenceId).getUser().getId());
        } else owner = null;

        if (cancelledIncidence.getIncidenceStateId() == INCIDENCE_SOLVE_STATE ||
                cancelledIncidence.getIncidenceStateId() == INCIDENCE_CANCELLED_STATE) {
            cancelledIncidence.setIncidenceFiles(incidenceFileService.findAllByIncidenceId(incidenceId));
            cancelledIncidence.setMessages(messageService.findAllByIncidenceId(incidenceId));
            model.addAttribute("incidence", cancelledIncidence);
            model.addAttribute("userOwner", owner);
            return "view/incidence";
        }

        FullIncidenceDTO incidenceDTO = incidenceService.findIncidenceByIdAndUserId(incidenceId, user);
        incidenceDTO.setIncidenceFiles(incidenceFileService.findAllByIncidenceId(incidenceId));
        incidenceDTO.setMessages(messageService.findAllByIncidenceId(incidenceId));
        model.addAttribute("userOwner", owner);
        model.addAttribute("incidence", incidenceDTO);
        model.addAttribute("newMessage", new IncidenceMessageDTO());
        return "view/incidence";
    }

    @GetMapping("/incidencia/cancelar/{id}")
    public String cancelIncidence(@PathVariable long id) {
        incidenceService.cancel(id);
        historyService.saveHistory(id);
        return "redirect:/incidencias";
    }

    @PostMapping("/public/nueva/incidencia")
    public String saveNoUserIncidence(@Valid FullIncidenceDTO newIncidence, BindingResult bindingResult, Model model, @RequestParam("images[]") MultipartFile[] files, RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("departments", departmentService.list());
            model.addAttribute("spaces", spaceService.list());
            model.addAttribute("incidence", newIncidence);
            model.addAttribute("isNewIncidence", true);
            return "view/incidenceNoLoginForm";
        }

        incidenceFileService.validateFiles(files);
        incidenceService.save(newIncidence);
        FullIncidenceDTO fullIncidenceDTO = incidenceService.getLastIncidence();
        incidenceFileService.saveIncidenceFiles(files, fullIncidenceDTO);

        historyService.saveHistory(fullIncidenceDTO.getId());
        redirectAttributes.addFlashAttribute("incidenceUploaded", true);
        return "redirect:/";
    }

    @PostMapping("/incidencia/nueva")
    public String saveIncidence(@Valid FullIncidenceDTO incidenceDepartmentDTO, BindingResult bindingResult, Model model, @RequestParam("images[]") MultipartFile[] files, RedirectAttributes redirectAttributes) {

        bindingResult = excludeEmailFormValidationForUsers(bindingResult, incidenceDepartmentDTO, model);

        if (bindingResult.hasErrors()) {
            model.addAttribute("spaces", spaceService.list());
            model.addAttribute("departments", departmentService.list());
            model.addAttribute("incidence", incidenceDepartmentDTO);
            model.addAttribute("isNewIncidence", true);
            return "view/incidenceForm";
        }

        incidenceFileService.validateFiles(files);
        incidenceService.save(incidenceDepartmentDTO, (FullUserDTO) model.getAttribute("userlogin"));
        FullIncidenceDTO fullIncidenceDTO = incidenceService.getLastIncidence();
        incidenceFileService.saveIncidenceFiles(files, fullIncidenceDTO);
        historyService.saveHistory(fullIncidenceDTO.getId());
        redirectAttributes.addFlashAttribute("incidenceUploaded", true);
        return "redirect:/incidencias";
    }

    @PostMapping("/incidencia/modificar/{id}")
    public String updateIncidence(@Valid FullIncidenceDTO fullIncidenceDTO, BindingResult bindingResult, Model model, @RequestParam("images[]") MultipartFile[] files, RedirectAttributes redirectAttributes) {

        bindingResult = excludeEmailFormValidationForUsers(bindingResult, fullIncidenceDTO, model);

        if (bindingResult.hasErrors()) {
            model.addAttribute("departments", spaceService.list());
            model.addAttribute("departments", departmentService.list());
            model.addAttribute("incidence", fullIncidenceDTO);
            model.addAttribute("isNewIncidence", false);
            return "view/incidenceForm";
        }

        incidenceFileService.validateFiles(files);
        incidenceService.update(fullIncidenceDTO);
        incidenceFileService.saveIncidenceFiles(files, fullIncidenceDTO);
        historyService.saveHistory(fullIncidenceDTO.getId());

        redirectAttributes.addFlashAttribute("incidenceUpdated", true);
        return "redirect:/incidencias";
    }

    @GetMapping("/incidencia/estado/{incidenceId}/{stateId}")
    public String changeIncidenceState(@PathVariable long incidenceId, @PathVariable long stateId, Model model) throws AccessDeniedException {

        FullUserDTO user = (FullUserDTO) model.getAttribute("userlogin");

        if (user.getType() != UserType.TECH && user.getType() != UserType.DEP_HEAD)
            throw new AccessDeniedException("No tienes permisos para realizar esta acción");

        incidenceService.changeState(incidenceId, stateId);

        Long userId = userIncidenceService.findByIncidenceId(incidenceId).getUser() != null ?
                userIncidenceService.findByIncidenceId(incidenceId).getUser().getId() : null;

        FullUserDTO owner = null;
        FullIncidenceDTO incidence = incidenceService.findById(incidenceId);

        if(userId != null) owner = userService.getUserBy(userId);
        else owner = null;

        if (owner == null) emailService.sendEmail(incidence.getEmail(), incidence.getTitle());
        else emailService.sendEmail(owner.getEmail(), incidence.getTitle());

        historyService.saveHistory(incidenceId);
        return "redirect:/incidencia/" + incidenceId;
    }

    @GetMapping("/incidencia/historial/{incidenceId}")
    public String showHistory(@PathVariable long incidenceId, Model model) throws AccessDeniedException {

        FullUserDTO userDTO = (FullUserDTO) model.getAttribute("userlogin");

        if (userDTO.getType() == UserType.USER) {
            throw new AccessDeniedException("No tienes permisos para realizar esta acción");
        }

        List<HistoryDTO> history = historyService.getHistoryByIncidenceId(incidenceId);

        List<HistoryDTO> fullHistory = history.stream().map(incidence -> {
            incidence.setStateName(incidenceStateService.findById(incidence.getStateId()).getName());
            incidence.setDepartmentName(departmentService.findById(incidence.getDepartmentId()) != null ?
                    departmentService.findById(incidence.getDepartmentId()).getName() : "N/A");
            incidence.setSpaceName(spaceService.findById(incidence.getSpaceId()) != null ?
                    spaceService.findById(incidence.getSpaceId()).getName() : "N/A");
            incidence.setTechName(userService.getUserBy(incidence.getTechId()).getUsername());
            incidence.setDateEnd(incidence.getDateEnd() != null ? incidence.getDateEnd() : null);
            return incidence;
        }).toList();

        model.addAttribute("incidenceHistory", fullHistory);
        model.addAttribute("incidenceId", incidenceId);
        return "view/history";
    }

    @GetMapping("/incidencias/head")
    public String showAllIncidences(Model model) {

        FullUserDTO userDTO = (FullUserDTO) model.getAttribute("userlogin");

        if (userDTO.getType() != UserType.DEP_HEAD)
            throw new RuntimeException("No tienes permisos para realizar esta acción");

        List<FullIncidenceDTO> incidences = incidenceService.list();
        incidences.forEach(incidence -> {
            Long techId = userIncidenceService.findByIncidenceId(incidence.getId()).getTech() != null ?
                    userIncidenceService.findByIncidenceId(incidence.getId()).getTech().getId() : -1;
            incidence.setTechId(techId);
            incidence.setDepartment(departmentService.findById(incidence.getDepartmentId()));
        });

        incidences = incidences.stream().filter(incidence -> incidence.getIncidenceState().getId() < 3).toList();

        List<FullUserDTO> techs = userService.listTechs();
        techs.forEach(tech -> tech.setDepartment(departmentService.findById(tech.getDepartment().getId())));

        model.addAttribute("incidences", incidences);
        model.addAttribute("techs", techs);
        return "view/incidencesHead";
    }


    private BindingResult excludeEmailFormValidationForUsers(BindingResult bindingResult, FullIncidenceDTO fullIncidenceDTO, Model model) {
        if (model.getAttribute("userlogin") instanceof FullUserDTO) {
            List<FieldError> errorsToKeep = bindingResult.getFieldErrors().stream()
                    .filter(error -> !error.getField().equals("email"))
                    .toList();

            bindingResult = new BeanPropertyBindingResult(fullIncidenceDTO, "incidenceDepartmentDTO");

            for (FieldError fieldError : errorsToKeep) {
                bindingResult.addError(fieldError);
            }
        }
        return bindingResult;
    }
}
