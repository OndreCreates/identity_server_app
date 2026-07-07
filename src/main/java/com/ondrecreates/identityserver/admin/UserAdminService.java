package com.ondrecreates.identityserver.admin;

import com.ondrecreates.identityserver.user.AppUser;
import com.ondrecreates.identityserver.user.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserAdminService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AppUser> listUsers() {
        return appUserRepository.findAll();
    }

    public AppUser require(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Uživatel neexistuje."));
    }

    public void create(UserFormDto form) {
        if (form.getPassword().isBlank()) {
            throw new IllegalArgumentException("Nový uživatel musí mít heslo.");
        }
        AppUser user = new AppUser(form.getEmail(), passwordEncoder.encode(form.getPassword()));
        user.setEnabled(form.isEnabled());
        user.setRoles(rolesFrom(form));
        appUserRepository.save(user);
    }

    /**
     * currentUserEmail is who's making the change -- used to stop an admin from editing
     * their own account into a state where they can no longer administer anything
     * (removing their own ADMIN role or disabling themselves).
     */
    public void update(Long id, UserFormDto form, String currentUserEmail) {
        AppUser user = require(id);
        boolean isSelf = isSelf(user, currentUserEmail);

        if (isSelf && !form.isRoleAdmin()) {
            throw new IllegalArgumentException("Nemůžeš si sám sobě odebrat roli ADMIN.");
        }
        if (isSelf && !form.isEnabled()) {
            throw new IllegalArgumentException("Nemůžeš deaktivovat vlastní účet.");
        }

        user.setEmail(form.getEmail());
        if (!form.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(form.getPassword()));
        }
        user.setEnabled(form.isEnabled());
        user.setRoles(rolesFrom(form));
        appUserRepository.save(user);
    }

    public void delete(Long id, String currentUserEmail) {
        AppUser user = require(id);
        if (isSelf(user, currentUserEmail)) {
            throw new IllegalArgumentException("Nemůžeš smazat vlastní účet.");
        }
        appUserRepository.delete(user);
    }

    private static boolean isSelf(AppUser user, String currentUserEmail) {
        return user.getEmail().equalsIgnoreCase(currentUserEmail);
    }

    private static Set<String> rolesFrom(UserFormDto form) {
        Set<String> roles = new HashSet<>();
        if (form.isRoleUser()) {
            roles.add("USER");
        }
        if (form.isRoleAdmin()) {
            roles.add("ADMIN");
        }
        return roles;
    }
}
