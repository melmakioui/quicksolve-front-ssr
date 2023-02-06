package com.quicksolve.proyecto.controller;

import com.quicksolve.proyecto.dto.FullUserDTO;
import com.quicksolve.proyecto.service.implementation.InvoiceServiceImpl;
import com.quicksolve.proyecto.service.implementation.ServiceServiceImpl;
import com.quicksolve.proyecto.service.implementation.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@SessionAttributes({"userlogin"})
public class PlansController {

    @Autowired
    ServiceServiceImpl serviceServ;

    @Autowired
    UserServiceImpl userService;

    @Autowired
    InvoiceServiceImpl invService;

    @GetMapping("/planes")
    public String renderPlans(Model model){
        model.addAttribute("planes", serviceServ.showAll());
        return "view/planes";
    }

    @PostMapping("/modificar/plan")
    public String modifyPlan(@RequestParam("tipoplan") String planId, Model model){
        /* TODO generar la transacción mediante REDSYS u otro método y si va bien... ---> */
        long idPlan = Long.parseLong(planId);
        if (idPlan <= serviceServ.getLastService() && idPlan >= 0){
            FullUserDTO newUser = userService.updateService(((FullUserDTO) model.getAttribute("userlogin")).getEmail(), idPlan);
            if (idPlan != 0){
                newUser.addInvoice(invService.generateNewInvoice(newUser.getEmail(), idPlan));
                System.out.println(newUser);
            }
            model.addAttribute("userlogin", newUser);
            System.out.println(model.getAttribute("userlogin"));
        }
        return "redirect:/planes";
    }
}
