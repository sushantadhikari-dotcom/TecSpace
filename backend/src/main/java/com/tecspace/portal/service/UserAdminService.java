package com.tecspace.portal.service;

import com.tecspace.portal.dto.CreatedUser;
import com.tecspace.portal.dto.UserCreateRequest;
import com.tecspace.portal.dto.UserUpdateRequest;
import com.tecspace.portal.dto.UserView;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.User;
import com.tecspace.portal.repository.UserRepository;
import com.tecspace.portal.security.Permissions;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Accounts for administrators, staff and teachers (students sign up or are enrolled from admissions). */
@Service
public class UserAdminService {

    private static final List<String> STAFF_ROLES = List.of("ROLE_ADMIN", "ROLE_STAFF", "ROLE_TEACHER");

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserAdminService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public List<UserView> list() {
        return users.findByRoleIn(STAFF_ROLES).stream()
                .sorted(Comparator.comparing(User::getRole).thenComparing(User::getFirstName, String.CASE_INSENSITIVE_ORDER))
                .map(UserView::from).toList();
    }

    public CreatedUser create(UserCreateRequest r) {
        String email = r.universityEmail().trim().toLowerCase();
        if (users.existsByUniversityEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        String temp = TempPassword.generate();

        User u = new User();
        u.setUniversityEmail(email);
        u.setPassword(encoder.encode(temp));
        u.setFirstName(r.firstName().trim());
        u.setLastName(r.lastName().trim());
        u.setMobileNumber(r.mobileNumber());
        u.setRole("ROLE_" + r.role());
        u.setFaculty(r.role());
        u.setSession("N/A");
        u.setPermissions(cleanPermissions(r.permissions()));
        u.setMustChangePassword(true);   // the temporary password must be changed at first login
        return new CreatedUser(UserView.from(users.save(u)), temp);
    }

    public UserView update(String actorEmail, String id, UserUpdateRequest r) {
        User u = staff(id);
        if (u.getUniversityEmail().equals(actorEmail)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot change your own role or disable your own account");
        }
        u.setRole("ROLE_" + r.role());
        u.setFaculty(r.role());
        u.setPermissions(cleanPermissions(r.permissions()));
        u.setActive(r.active());
        u.setFailedLogins(0);
        u.setLockedUntil(null);
        return UserView.from(users.save(u));
    }

    public String resetPassword(String id) {
        User u = staff(id);
        String temp = TempPassword.generate();
        u.setPassword(encoder.encode(temp));
        u.setMustChangePassword(true);
        u.setFailedLogins(0);
        u.setLockedUntil(null);
        users.save(u);
        return temp;
    }

    /** Clears a lock caused by too many wrong passwords. Works for any account. */
    public void unlock(String id) {
        User u = users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        u.setFailedLogins(0);
        u.setLockedUntil(null);
        users.save(u);
    }

    public Map<String, String> permissionCatalog() {
        return Permissions.ALL;
    }

    private User staff(String id) {
        User u = users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (!STAFF_ROLES.contains(u.getRole())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "User not found");
        }
        return u;
    }

    private List<String> cleanPermissions(List<String> given) {
        List<String> out = new ArrayList<>();
        if (given == null) return out;
        for (String p : given) {
            if (!Permissions.ALL.containsKey(p)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown permission " + p);
            }
            if (!out.contains(p)) out.add(p);
        }
        return out;
    }
}
