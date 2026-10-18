package com.evcharging.action;

import com.evcharging.manager.AppManager;
import com.evcharging.model.User;

public class LoginAction implements Action<User> {

    private final String email;
    private final String password;

    public LoginAction(String email, String password) {
        this.email = email;
        this.password = password;
    }

    @Override
    public ActionResult<User> execute() throws Exception {
        User user = AppManager.getInstance().getAuthService().login(email, password);
        AppManager.getInstance().setCurrentUser(user);
        return ActionResult.success(user, "Login successful");
    }
}
