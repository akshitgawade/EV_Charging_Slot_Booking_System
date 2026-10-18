package com.evcharging.action;

import com.evcharging.manager.AppManager;
import com.evcharging.model.User;

public class RegisterAction implements Action<User> {

    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phone;
    private final String password;

    public RegisterAction(String firstName, String lastName, String email, String phone, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.password = password;
    }

    @Override
    public ActionResult<User> execute() throws Exception {
        User user = AppManager.getInstance().getAuthService()
                .registerCustomer(firstName, lastName, email, phone, password);
        return ActionResult.success(user, "Registration successful");
    }
}
