package com.proyecto.spring.controller;

import com.azure.cosmos.models.PartitionKey;
import com.proyecto.spring.exception.RequestException;
import com.proyecto.spring.repository.UsuarioDBRepository;
import com.proyecto.spring.dto.UsuarioCrudResponse;
import com.proyecto.spring.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {
    @Autowired
    UsuarioDBRepository usuarioDBRepository;
    Logger logger = LoggerFactory.getLogger(UsuarioController.class);

    //Agregar nuevo usuario
    @PostMapping
    public ResponseEntity<UsuarioCrudResponse> createNewUsuario(@RequestBody Usuario u) {

        if (u.getNombre_usuario().equals("") || u.getNombre_usuario() == null) {
            throw new RequestException("P-400", "El nombre es requerido");
        }
        if (u.getCorreo().equals("") || u.getCorreo() == null) {
            throw new RequestException("P-101", "El correo electronico es requerido");
        }
        //Patron para validar el correo
        Pattern pattern = Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");
        Matcher mather = pattern.matcher(u.getCorreo());
        if (mather.find() == false) {
            throw new RequestException("P-102", "Ingrese un email correcto");
        }
        Pattern pattern2 = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[$@$!%*?&])([A-Za-z\\d$@$!%*?&]|[^ ]){8,15}$");
        Matcher mather2 = pattern2.matcher(u.getContrasenia());
        if (mather2.find() == false) {
            throw new RequestException("P-102", "Ingrese una contraseña correcta");
        }
        if (u.getContrasenia().equals("") || u.getContrasenia() == null) {
            throw new RequestException("P-103", "Una contraseña es requerida");
        }
        if (searchCorreo(u.getCorreo()) == true) {
            throw new RequestException("105", "El correo ingresado ya existe");
        }
        if (searchNombreUsuario(u.getNombre_usuario()) == true) {
            throw new RequestException("106", "El nombre de usuario ya existe");
        }
        u = usuarioDBRepository.save(u);
        UsuarioCrudResponse usuarioCrudResponse = new UsuarioCrudResponse();
        usuarioCrudResponse.setMessage("Nuevo usuario creado correctamente con el ID: " + u.getId());
        usuarioCrudResponse.setStatusCode("201: Created");
        return new ResponseEntity<UsuarioCrudResponse>(usuarioCrudResponse, HttpStatus.CREATED);
    }

    //Actualizar un usuario existente
    @PutMapping("/{id}")
    public ResponseEntity<String> updateExistingUsuario(@PathVariable String id, @RequestBody Usuario u) {
        if (u.getNombre_usuario().equals("") || u.getNombre_usuario() == null ||
                u.getContrasenia().equals("") || u.getContrasenia() == null ||
                u.getCorreo().equals("") || u.getCorreo() == null) {
            throw new RuntimeException("Llene todos los campos");
        }
        Optional<Usuario> usuario = usuarioDBRepository.findById(id);
        usuarioDBRepository.deleteById(id, new PartitionKey(usuario.get().getNombre_usuario()));
        u.setId(id);
        u = usuarioDBRepository.save(u);
        return new ResponseEntity<String>("", HttpStatus.NO_CONTENT);
    }

    //Traer un usuario
    @GetMapping("/{id}")
    public ResponseEntity<List<Usuario>> getUsuarios(@PathVariable String id) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("ContentType", "application/json");
        List<Usuario> usuarioList = new ArrayList<>();
        logger.info("Id is present in the GET request");
        List<Optional<Usuario>> optionaUsuarioList = Collections.singletonList(usuarioDBRepository.findById(id));
        if (!(optionaUsuarioList.get(0).isEmpty())) {
            optionaUsuarioList.stream().forEach(u -> u.ifPresent(usuario -> usuarioList.add(usuario)));
            return new ResponseEntity<List<Usuario>>(usuarioList, responseHeaders, HttpStatus.OK);
        }
        return new ResponseEntity<List<Usuario>>(usuarioList, responseHeaders, HttpStatus.NOT_FOUND);
    }

    //Traer todos los usuarios existentes
    @GetMapping
    public ResponseEntity<List<Usuario>> getAllUsuarios() {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("ContentType", "application/json");
        List<Usuario> usuarioList = new ArrayList<>();
        logger.info("Id is not present in the GET request");
        usuarioList = usuarioDBRepository.getAllUsuarios();
        return new ResponseEntity<List<Usuario>>(usuarioList, responseHeaders, HttpStatus.OK);
    }

    //eliminar un id particular
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteExistingUsuario(@PathVariable String id) {
        Optional<Usuario> usuario = usuarioDBRepository.findById(id);
        usuarioDBRepository.deleteById(id, new PartitionKey(usuario.get().getNombre_usuario()));
        return new ResponseEntity<String>("", HttpStatus.NO_CONTENT);
    }

    //
    public boolean searchCorreo(String correo) {
        List<Usuario> correoList = new ArrayList<>();
        logger.info("Id is not present in the GET request");
        correoList = usuarioDBRepository.searchCorreo(correo);  // -> [{ "nombre_usuario": null, "correo": juan@poyo.com, "contrasenia":null}]   -> []
        if (correoList != null && correoList.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean searchNombreUsuario(String nombre_usuario) {
        List<Usuario> nombre_usuarioList = new ArrayList<>();
        logger.info("Id is not present in the GET request");
        nombre_usuarioList = usuarioDBRepository.searchNombreUsuario(nombre_usuario);  // -> [{ "nombre_usuario": null, "correo": juan@poyo.com, "contrasenia":null}]   -> []
        if (nombre_usuarioList != null && nombre_usuarioList.size() > 0) {
            return true;
        } else {
            return false;
        }
    }
}