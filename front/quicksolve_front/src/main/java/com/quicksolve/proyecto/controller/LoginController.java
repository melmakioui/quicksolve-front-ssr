package com.quicksolve.proyecto.controller;

import com.quicksolve.proyecto.configuration.PasswordEncoderConf;
import com.quicksolve.proyecto.dto.FullUserDTO;
import com.quicksolve.proyecto.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@SessionAttributes({"userlogin"})
public class LoginController {

    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoderConf passwordEncoder;

    @GetMapping("/login")
    public String renderLogin(){
        return "view/login";
    }

    @PostMapping("/login")
    public String tryLogin(Model model, @RequestParam("email") String email, @RequestParam("pwd") String password){
        FullUserDTO usr = userService.getUserBy(email);
        if (usr != null && passwordEncoder.encoder().matches(password, usr.getPassword())){
            FullUserDTO totalUser = userService.getFullUser(usr.getId());
            model.addAttribute("userlogin", totalUser);
        } else {
            model.addAttribute("loginError", "No se han introducido los datos correctos para el email y/o contraseña.");
            return "view/login";
        }

        return "redirect:/incidencias";
    }

    @GetMapping("/logout")
    public String logout(Model model){
        model.addAttribute("userlogin", false);
        return "redirect:/";
    }

}
