package com.evcharging.manager;

import com.evcharging.action.Action;
import com.evcharging.action.ActionResult;
import com.evcharging.exception.AuthenticationException;
import com.evcharging.exception.BookingConflictException;
import com.evcharging.exception.ValidationException;
import com.evcharging.model.User;
import com.evcharging.model.UserRole;
import com.evcharging.service.AdminService;
import com.evcharging.service.AuthService;
import com.evcharging.service.BookingService;

import java.sql.SQLException;

public class AppManager {

    private static volatile AppManager instance;

    private User currentUser;

    private final AuthService authService;
    private final BookingService bookingService;
    private final AdminService adminService;

    private AppManager() {
        this.authService = new AuthService();
        this.bookingService = new BookingService();
        this.adminService = new AdminService();
    }

    public static AppManager getInstance() {
        if (instance == null) {
            synchronized (AppManager.class) {
                if (instance == null) {
                    instance = new AppManager();
                }
            }
        }
        return instance;
    }

    public <T> ActionResult<T> executeAction(Action<T> action) {
        if (action == null) {
            return ActionResult.failure("Action cannot be null.");
        }

        try {
            return action.execute();
        } catch (AuthenticationException | ValidationException | BookingConflictException e) {
            return ActionResult.failure(e.getMessage(), e);
        } catch (SQLException e) {
            return ActionResult.failure("Database error: " + e.getMessage(), e);
        } catch (Throwable t) {
            t.printStackTrace();
            return ActionResult.failure("An unexpected error occurred: " + t.getMessage(), t);
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == UserRole.ADMIN;
    }

    public void logout() {
        this.currentUser = null;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public BookingService getBookingService() {
        return bookingService;
    }

    public AdminService getAdminService() {
        return adminService;
    }
}
