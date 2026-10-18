package com.evcharging.service;

import com.evcharging.dao.UserDAO;
import com.evcharging.exception.AuthenticationException;
import com.evcharging.exception.ValidationException;
import com.evcharging.model.User;
import com.evcharging.model.UserRole;
import com.evcharging.model.UserStatus;
import com.evcharging.security.PasswordHasher;

import java.sql.SQLException;
import java.util.Optional;

public class AuthService {

    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public User login(String email, String password) throws AuthenticationException, SQLException {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new AuthenticationException("Email and password are required.");
        }

        Optional<User> userOpt = userDAO.findByEmail(email.trim());
        if (userOpt.isEmpty()) {
            throw new AuthenticationException("Invalid email or password.");
        }

        User user = userOpt.get();

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new AuthenticationException("This account is blocked. Please contact support.");
        }

        if (!PasswordHasher.verifyPassword(password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password.");
        }

        return user;
    }

    public User registerCustomer(String firstName, String lastName, String email, String phone, String password)
            throws ValidationException, SQLException {

        validateRegistrationFields(firstName, lastName, email, phone, password);

        if (userDAO.findByEmail(email.trim()).isPresent()) {
            throw new ValidationException("An account with this email already exists.");
        }

        if (userDAO.findByPhone(phone.trim()).isPresent()) {
            throw new ValidationException("An account with this phone number already exists.");
        }

        String passwordHash = PasswordHasher.hashPassword(password);

        User newUser = new User();
        newUser.setFirstName(firstName.trim());
        newUser.setLastName(lastName.trim());
        newUser.setEmail(email.trim());
        newUser.setPhone(phone.trim());
        newUser.setPasswordHash(passwordHash);
        newUser.setRole(UserRole.CUSTOMER);
        newUser.setStatus(UserStatus.ACTIVE);

        return userDAO.createUser(newUser);
    }

    private void validateRegistrationFields(String firstName, String lastName, String email, String phone, String password)
            throws ValidationException {
        if (firstName == null || firstName.trim().isEmpty()) throw new ValidationException("First name is required.");
        if (lastName == null || lastName.trim().isEmpty()) throw new ValidationException("Last name is required.");
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) throw new ValidationException("Valid email is required.");
        if (phone == null || !phone.trim().matches("(\\+91)?[6-9][0-9]{9}")) {
            throw new ValidationException("Phone number must be a 10-digit Indian mobile number starting with 9/8/7/6 (e.g., 9876543210 or +919876543210).");
        }
        if (password == null || password.length() < 6) throw new ValidationException("Password must be at least 6 characters.");
    }
}
