package com.ondrecreates.identityserver.admin;

import com.ondrecreates.identityserver.user.AppUser;

/** Plain JavaBean (not a record) -- Spring MVC's @ModelAttribute binding needs getters/setters. */
public class UserFormDto {

    private String email = "";
    private String password = "";
    private boolean enabled = true;
    private boolean roleUser = true;
    private boolean roleAdmin = false;

    public static UserFormDto from(AppUser user) {
        UserFormDto form = new UserFormDto();
        form.setEmail(user.getEmail());
        form.setPassword("");
        form.setEnabled(user.isEnabled());
        form.setRoleUser(user.getRoles().contains("USER"));
        form.setRoleAdmin(user.getRoles().contains("ADMIN"));
        return form;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRoleUser() {
        return roleUser;
    }

    public void setRoleUser(boolean roleUser) {
        this.roleUser = roleUser;
    }

    public boolean isRoleAdmin() {
        return roleAdmin;
    }

    public void setRoleAdmin(boolean roleAdmin) {
        this.roleAdmin = roleAdmin;
    }
}
