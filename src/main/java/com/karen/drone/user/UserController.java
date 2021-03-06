package com.karen.drone.user;

import com.karen.drone.comment.model.CommentDefinition;
import com.karen.drone.exceptions.AuthenticationException;
import com.karen.drone.exceptions.InvalidInputException;
import com.karen.drone.security.JwtUtil;
import com.karen.drone.security.SpringSecurityConfig;
import com.karen.drone.user.models.TokenResponse;
import com.karen.drone.user.models.UserDefinition;
import com.karen.drone.user.models.UserLogin;
import com.karen.drone.user.models.components.UserRole;
import com.karen.drone.user.models.persistence.UserDAO;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.UUID;

/**
 * @author Daniel Incicau, daniel.incicau@busymachines.com
 * @since 2019-05-17
 */
@RestController
public class UserController {

    @Value("${admin.email:admin}")
    private String adminEmail;

    @Value("${admin.password:admin}")
    private String adminPassword;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @PostConstruct
    public void init(){
        if(!userRepository.findByEmail(adminEmail).isPresent()) {
            UserDAO user = new UserDAO();

            user.setUserId(UUID.randomUUID());
            user.setEmail(adminEmail);
            String hashedUserPassword = SpringSecurityConfig.encoder().encode(adminPassword);
            user.setPassword(hashedUserPassword);
            user.setName("Administrator");
            user.setRole(UserRole.ADMIN);
            user.setCreatedAt(new Date());

            userRepository.save(user);
        }
    }

    @ApiOperation(value = "Check connection")
    @GetMapping(path="/")
    public ResponseEntity checkConnection() {
        return new ResponseEntity(HttpStatus.ACCEPTED);
    }

    @ApiOperation(value = "Login")
    @PostMapping(path="/user/login")
    public ResponseEntity<TokenResponse> login(@RequestBody UserLogin login) {
        UserDAO user = userRepository.findByEmail(login.email).orElseThrow(
                () -> new AuthenticationException("Login attempt failed")
        );

        if(!SpringSecurityConfig.encoder().matches(login.password, user.getPassword())) {
            throw new AuthenticationException("Login attempt failed");
        }

        String token = jwtUtil.generateToken(user);
        return new ResponseEntity<>(new TokenResponse(token), HttpStatus.OK);
    }

    @ApiOperation(value = "Register")
    @PostMapping(path="/user")
    public ResponseEntity<TokenResponse> register(@RequestBody UserDefinition def) {
        if(userRepository.findByEmail(def.getEmail()).isPresent()) {
            throw new InvalidInputException("Email already registered");
        }

        UserDAO dao = new UserDAO();
        dao.setUserId(UUID.randomUUID());
        dao.setEmail(def.getEmail());
        String hashedPassword = SpringSecurityConfig.encoder().encode(def.getPassword());
        dao.setPassword(hashedPassword);
        dao.setName(def.getName());
        dao.setRole(UserRole.USER);
        dao.setCreatedAt(new Date());
        userRepository.save(dao);

        String token = jwtUtil.generateToken(dao);
        return new ResponseEntity<>(new TokenResponse(token), HttpStatus.CREATED);
    }

    @GetMapping("/uuid")
    public UUID getRandomUUID() {
        return UUID.randomUUID();
    }

}
