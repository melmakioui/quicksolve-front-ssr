package com.quicksolve.proyecto.controller;

import com.quicksolve.proyecto.configuration.PasswordEncoderConf;
import com.quicksolve.proyecto.dto.FullUserDTO;
import com.quicksolve.proyecto.entity.type.UserType;
import com.quicksolve.proyecto.service.TokenService;
import com.quicksolve.proyecto.service.UserService;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthRestController {

    @Autowired
    private TokenService tokenService;
    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoderConf passwordEncoder;

    @PostMapping(value = "/loginTokenGenerate")
    public @ResponseBody ResponseEntity<?> validacioLogin(@RequestBody String jsonData) throws JSONException {
        JSONObject json = new JSONObject(jsonData);
        String email = json.getString("email");
        String password = json.getString("password");

        if(userService.existsWithEmail(email)) {
            FullUserDTO userDTO = userService.getUserBy(email);
            return crearTokenPasswordValid(email, password, userDTO);
        }
        return new ResponseEntity<>("Este usuario no está autorizado.", HttpStatus.UNAUTHORIZED);
    }

    @PostMapping(value = "/loginTokenGeneratePrivate")
    public @ResponseBody ResponseEntity<?> validacioLoginPrivada(@RequestBody String jsonData) throws JSONException {
        JSONObject json = new JSONObject(jsonData);
        String email = json.getString("email");
        String password = json.getString("password");

        if(userService.existsWithEmail(email)) {
            FullUserDTO userDTO = userService.getUserBy(email);
            if (userDTO.getType() == UserType.ADMIN) return crearTokenPasswordValid(email, password, userDTO);
            return new ResponseEntity<>("El usuario no es un administrador.", HttpStatus.UNAUTHORIZED);
        } else {
            return new ResponseEntity<>("Este usuario no está autorizado.", HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping(value = "/token/validar")
    public @ResponseBody ResponseEntity<?> validacionToken(@RequestBody String token){
        return tokenService.validateToken(token) == 0 ? new ResponseEntity<>("Se ha validado el token", HttpStatus.OK) : new ResponseEntity<>("No se ha podido validar el token", HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<?> crearTokenPasswordValid(String email, String password, FullUserDTO userDTO) {
        if (passwordEncoder.encoder().matches(password, userDTO.getPassword())) {
            String rol = "";
            if (userDTO.getType() != null) rol = userDTO.getType().name();
            return new ResponseEntity<>(tokenService.createToken(email, rol), HttpStatus.OK);
        }
        return new ResponseEntity<>("Este usuario no está autorizado.", HttpStatus.UNAUTHORIZED);
    }

}
