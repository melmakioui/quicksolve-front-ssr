package com.quicksolve.proyecto.controller;

import com.quicksolve.proyecto.dto.FullUserDTO;
import com.quicksolve.proyecto.dto.UserDataDTO;
import com.quicksolve.proyecto.entity.User;
import com.quicksolve.proyecto.entity.UserData;
import com.quicksolve.proyecto.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
@SessionAttributes({"userlogin"})
@Validated
public class RegisterController {

    @Autowired
    private UserService userService;

    @GetMapping("/registro")
    public String renderRegister(Model model){
        User user = new User();
        model.addAttribute("user", user);
        return "view/registro";
    }

    @PostMapping("/registro")
    public String registerUser(
            @Valid FullUserDTO user,
            BindingResult bindingResult,
            @RequestParam("name") String name,
            @RequestParam("firstSurname") String firstSurname,
            @RequestParam(value = "secondSurname", required = false) String secondSurname,
            Model model
    ){

        if(bindingResult.hasErrors()){
            model.addAttribute("user", user);
            if (userService.existsWithEmail(user.getEmail())){
                model.addAttribute("alreadyExistsEmail", "Este email ya existe.");
            }
            if (userService.existsWithUsername(user.getUsername())){
                model.addAttribute("alreadyExistsUsername", "Este nombre ya existe.");
            }
            return "view/registro";
        }

        user.setData(new UserDataDTO(
                name,
                firstSurname,
                secondSurname,
                LocalDateTime.now()
        ));

        FullUserDTO totalUser = userService.createUser(user);
        model.addAttribute("userlogin", totalUser);
        return "redirect:/incidencias";
    }
}
