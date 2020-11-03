package com.tanaguru.controller;

import com.tanaguru.domain.constant.CustomError;
import com.tanaguru.domain.exception.CustomEntityNotFoundException;
import com.tanaguru.domain.exception.CustomForbiddenException;
import com.tanaguru.domain.constant.AppAuthorityName;
import com.tanaguru.domain.constant.EAppRole;
import com.tanaguru.domain.dto.ChangePasswordCommandDTO;
import com.tanaguru.domain.dto.ForgotEmailDTO;
import com.tanaguru.domain.dto.UserDTO;
import com.tanaguru.domain.entity.membership.contract.ContractAppUser;
import com.tanaguru.domain.entity.membership.project.ProjectAppUser;
import com.tanaguru.domain.entity.membership.user.User;
import com.tanaguru.domain.exception.CustomInvalidEntityException;
import com.tanaguru.factory.UserFactory;
import com.tanaguru.repository.ContractUserRepository;
import com.tanaguru.repository.ProjectUserRepository;
import com.tanaguru.repository.UserRepository;
import com.tanaguru.service.AppRoleService;
import com.tanaguru.service.MailService;
import com.tanaguru.service.MessageService;
import com.tanaguru.service.TanaguruUserDetailsService;
import com.tanaguru.service.UserService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.mail.MailException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import java.util.*;

/**
 * @author rcharre
 */
@RestController
@RequestMapping("/users")
public class UserController {
    private final TanaguruUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final UserFactory userFactory;
    private final AppRoleService appRoleService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final ContractUserRepository contractUserRepository;
    private final ProjectUserRepository projectUserRepository;
    private final MailService mailService;

    @Value("${webapp.url}")
    private String webappUrl;

    @Value("${password.tokenValidity}")
    private int passwordTokenValidity;
    
    @Inject
    private MessageService messageService;

    @Autowired
    public UserController(
            TanaguruUserDetailsService userDetailsService,
            UserRepository userRepository,
            UserService userService,
            UserFactory userFactory,
            AppRoleService appRoleService,
            BCryptPasswordEncoder bCryptPasswordEncoder,
            ContractUserRepository contractUserRepository, ProjectUserRepository projectUserRepository, MailService mailService) {
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.userService = userService;
        this.userFactory = userFactory;
        this.appRoleService = appRoleService;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.contractUserRepository = contractUserRepository;
        this.projectUserRepository = projectUserRepository;
        this.mailService = mailService;
    }

    /**
     * @return A @see Collection containing all the @see User
     */
    @ApiOperation(
            value = "Get all User",
            notes = "User must have SHOW_USER authority")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid parameters"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden for current session")
    })
    @PreAuthorize("hasAuthority(T(com.tanaguru.domain.constant.AppAuthorityName).SHOW_USER)")
    @GetMapping(value = "/", produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    Collection<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * @param id The @see User id
     * @return The @see User
     */
    @ApiOperation(
            value = "Get a User by id",
            notes = "User must have SHOW_USER authority or be the current User"
                    + "\nIf user not found, exception raise : USER_NOT_FOUND with user id")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid parameters"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden for current session"),
            @ApiResponse(code = 404, message = "User not found")
    })
    @PreAuthorize("hasAuthority(T(com.tanaguru.domain.constant.AppAuthorityName).SHOW_USER) || " +
            "(@tanaguruUserDetailsServiceImpl.getCurrentUser() != null && @tanaguruUserDetailsServiceImpl.getCurrentUser().getId() == #id)")
    @GetMapping(value = "/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    User getUser(@PathVariable long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new CustomEntityNotFoundException(CustomError.USER_NOT_FOUND, new long[] { id } ));
    }

    /**
     * @return The @see User
     */
    @ApiOperation(
            value = "Get currentu User")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid parameters"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden for current session")
    })
    @PreAuthorize("@tanaguruUserDetailsServiceImpl.getCurrentUser() != null")
    @GetMapping(value = "/me", produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    User getCurrentUser() {
        return userDetailsService.getCurrentUser();
    }

    /**
     * Find user by username
     *
     * @param username the username to find
     * @return the corresponding user
     */
    @ApiOperation(
            value = "Get User for a given username",
            notes = "User must have SHOW_USER authority"
                    + "\nIf user not found, exception raise : USER_NOT_FOUND with username")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid parameters"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden for current session"),
            @ApiResponse(code = 404, message = "User not found")
    })
    @PreAuthorize("hasAuthority(T(com.tanaguru.domain.constant.AppAuthorityName).SHOW_USER)")
    @GetMapping(value = "/by-name/{username}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    User getUser(@PathVariable String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomEntityNotFoundException(CustomError.USER_NOT_FOUND, new String[] {username}));
    }

    /**
     * Create a user
     *
     * @param user The @see UserDTO with data
     * @return The created user
     */
    @ApiOperation(
            value = "Create a User",
            notes = "User must have CREATE_USER authority"
                    + "\nIf username already exists, exception raise : USERNAME_ALREADY_EXISTS"
                    + "\nIf email already exists, exception raise : EMAIL_ALREADY_EXISTS"
                    + "\nIf invalid password, exception raise : INVALID_PASSWORD")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid parameters"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden for current session")
    })
    @PreAuthorize("hasAuthority(T(com.tanaguru.domain.constant.AppAuthorityName).CREATE_USER)")
    @PostMapping(value = "/", produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    User createUser(@RequestBody @Valid UserDTO user) {
        if (userService.checkUsernameIsUsed(user.getUsername())) {
            throw new CustomInvalidEntityException(CustomError.USERNAME_ALREADY_EXISTS);
        }

        if (userService.checkEmailIsUsed(user.getEmail())) {
            throw new CustomInvalidEntityException(CustomError.EMAIL_ALREADY_EXISTS);
        }

        if(user.getPassword() == null || user.getPassword().isEmpty()){
            throw new CustomInvalidEntityException(CustomError.INVALID_PASSWORD);
        }

        EAppRole approle = EAppRole.USER;
        if(userService.hasAuthority(userDetailsService.getCurrentUser(), AppAuthorityName.PROMOTE_USER)){
            approle = user.getAppRole();
        }

        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        return userFactory.createUser(user.getUsername(), user.getEmail(), user.getPassword(), approle, user.isEnabled());
    }

    /**
     * Modify a user
     *
     * @param user The @see UserDTo data
     * @return The modified @see User
     */
    @ApiOperation(
            value = "Modify a User",
            notes = "User must have MODIFY_USER authority"
                    + "\nIf user not found, exception raise : USER_NOT_FOUND with user id"
                    + "\nIf App role not found, exception raise : APP_ROLE_NOT_FOUND with app role")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid parameters"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden for current session"),
            @ApiResponse(code = 404, message = "User not found")
    })
    @PreAuthorize("hasAuthority(T(com.tanaguru.domain.constant.AppAuthorityName).MODIFY_USER)")
    @PutMapping(value = "/", produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    User modifyUser(@RequestBody @Valid UserDTO user) {
        User from = userRepository.findById(user.getId())
                .orElseThrow(() -> new CustomEntityNotFoundException(CustomError.USER_NOT_FOUND, new long[] { user.getId() } ));

        User to = new User();
        to.setUsername(user.getUsername());
        to.setEmail(user.getEmail());
        to.setEnabled(user.isEnabled());

        to.setAppRole(from.getAppRole());
        if(from.getAppRole().getName() != user.getAppRole() &&
                userDetailsService.getCurrentUser().getId() != from.getId() &&
                userService.hasAuthority(userDetailsService.getCurrentUser(), AppAuthorityName.PROMOTE_USER)){
            to.setAppRole(appRoleService.getAppRole(user.getAppRole())
                    .orElseThrow(() -> new CustomEntityNotFoundException(CustomError.APP_ROLE_NOT_FOUND, new String[] { user.getAppRole().toString() } )));
        }

        return userService.modifyUser(from, to);
    }

    /**
     * Modify the current user
     *
     * @param user The @see UserDTo data
     * @return The modified @see User
     */
    @ApiOperation(
            value = "Modify current User")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid parameters"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden for current session"),
            @ApiResponse(code = 404, message = "User not found")
    })
    @PutMapping(value = "/me", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("@tanaguruUserDetailsServiceImpl.getCurrentUser() != null")
    public @ResponseBody
    User modifyCurrentUser(@RequestBody @Valid UserDTO user) {
        User from = userDetailsService.getCurrentUser();
        User to = new User();
        to.setUsername(user.getUsername());
        to.setEmail(user.getEmail());

        return userService.modifyUser(from, to);

    }

    /**
     * Delete an @see User
     *
     * @param id The id of the user
     */
    @ApiOperation(
            value = "Delete current User",
            notes = "User must have DELETE_USER authority"
                    + "\nIf cannot delete current user, exception raise : CANNOT_DELETE_CURRENT_USER"
                    + "\nIf user not found, exception raise : USER_NOT_FOUND with user id")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid parameters"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden for current session or try to self delete"),
            @ApiResponse(code = 404, message = "User not found")
    })
    @PreAuthorize("hasAuthority(T(com.tanaguru.domain.constant.AppAuthorityName).DELETE_USER)")
    @DeleteMapping(value = "/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public void deleteUser(@PathVariable long id) {
        if (userDetailsService.getCurrentUser().getId() == id) {
            throw new CustomForbiddenException(CustomError.CANNOT_DELETE_CURRENT_USER);

        }

        userService.deleteUser(userRepository.findById(id)
                .orElseThrow(() -> new CustomEntityNotFoundException(CustomError.USER_NOT_FOUND, new long[] { id } )));
    }

    /**
     * @return All the @see ContractAppUser for a given @see Contract id
     */
    @ApiOperation(
            value = "Get all User for a given Contract id",
            notes = "User must have SHOW_CONTRACT authority on Contract")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid parameters"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden for current session"),
            @ApiResponse(code = 404, message = "Contract not found")
    })
    @PreAuthorize(
            "@tanaguruUserDetailsServiceImpl.currentUserHasAuthorityOnContract(" +
                    "T(com.tanaguru.domain.constant.ContractAuthorityName).SHOW_CONTRACT, " +
            "#id)")
    @GetMapping(value = "/by-contract/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    Collection<ContractAppUser> findAllByContract(@PathVariable long id) {
        return contractUserRepository.findAllByContract_Id(id);
    }

    /**
     * @return All the @see ProjectAppUser for a given @see Project id
     */
    @ApiOperation(
            value = "Get all User for a given Project id",
            notes = "User must have SHOW_PROJECT authority on Project")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid parameters"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden for current session"),
            @ApiResponse(code = 404, message = "Project not found")
    })
    @PreAuthorize(
            "@tanaguruUserDetailsServiceImpl.currentUserHasAuthorityOnProject(" +
                    "T(com.tanaguru.domain.constant.ProjectAuthorityName).SHOW_PROJECT, " +
            "#id)")
    @GetMapping(value = "/by-project/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    Collection<ProjectAppUser> findAllByProject(@PathVariable long id) {
        return projectUserRepository.findAllByProject_Id(id);
    }

    @ApiOperation(
            value = "Send a reset password email to the given email address",
            notes = "If error sending mail, exception raise : ERROR_SENDING_MAIL")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid parameters"),
            @ApiResponse(code = 500, message = "Error while sending email")
    })
    @PutMapping(value = "/forgot-password", produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    void forgotPassword(@RequestBody ForgotEmailDTO forgotEmailDTO) {
        Date date = new Date();
        Optional<User> userOpt = userRepository.findByEmail(forgotEmailDTO.getEmail());
        if(userOpt.isPresent()){
            User user = userOpt.get();
            Collection<Pair<String, Date>> tokens = user.getModificationPasswordTokens();

            String tokenContent = user.getId() + "-" + date.toString() + "-" + date.hashCode();
            String token = Base64.getEncoder().encodeToString(bCryptPasswordEncoder.encode(tokenContent).getBytes());

            tokens.add(Pair.of(token, date));
            user.setModificationPasswordTokens(tokens);
            userRepository.save(user);

            try{
                mailService.sendSimpleMessage(forgotEmailDTO.getEmail(), messageService.getMessage("mail.forgotPasswordSubject"), messageService.getMessage("mail.forgotPasswordContent") + webappUrl + "reset-password/" + user.getId() + '/' + token );
            }catch (MailException e){
                throw  new InternalError(CustomError.ERROR_SENDING_EMAIL);
            }

        }
    }

    /**
     * Modify a user's password
     *
     * @return The modified @see User
     */
    @ApiOperation(
            value = "Change a User password",
            notes = "User must have MODIFY_USER authority or must be current User"
                    + "\nIf user not found, exception raise : USER_NOT_FOUND with user id")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid parameters"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden for current session"),
            @ApiResponse(code = 404, message = "User not found")
    })
    @PutMapping(value = "/change-password", produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    User changePassword(@RequestBody ChangePasswordCommandDTO changePasswordCommandDTO) {
        User user = userRepository.findById(changePasswordCommandDTO.getUserId())
                .orElseThrow(() -> new CustomEntityNotFoundException(CustomError.USER_NOT_FOUND, new long[] { changePasswordCommandDTO.getUserId() } ));

        User current = userDetailsService.getCurrentUser();
        Calendar c = Calendar.getInstance();
        Date currentDate = new Date();
        if((current != null && userService.hasAuthority(current, AppAuthorityName.MODIFY_USER)) ||
                (current != null && current.getId() == user.getId()) ||
                (changePasswordCommandDTO.getToken() != null &&
                user.getModificationPasswordTokens().stream().anyMatch(pair -> {
                    c.setTime(pair.getSecond());
                    c.add(Calendar.SECOND, passwordTokenValidity);
                    return pair.getFirst().equals(changePasswordCommandDTO.getToken()) &&
                            currentDate.compareTo(c.getTime()) <= 0;
                })
                        )
                ){
            return userDetailsService.changeUserPassword(user, changePasswordCommandDTO.getPassword());
        }else{
            throw new CustomForbiddenException(CustomError.CANNOT_MODIFY_USER_PASSWORD, new long[] { changePasswordCommandDTO.getUserId() } );
        }
    }
}
