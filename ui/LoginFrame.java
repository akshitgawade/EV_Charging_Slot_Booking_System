package com.evcharging.ui;

import com.evcharging.action.ActionResult;
import com.evcharging.action.LoginAction;
import com.evcharging.manager.AppManager;
import com.evcharging.model.User;
import com.evcharging.model.UserRole;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    private JTextField emailField;
    private JPasswordField passwordField;

    public LoginFrame() {
        initComponents();
    }

    private void initComponents() {
        setTitle("EV Charging System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 320);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("EV Charging Reservation", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Email:"), gbc);

        emailField = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Password:"), gbc);

        passwordField = new JPasswordField(20);
        gbc.gridx = 1; gbc.gridy = 2;
        panel.add(passwordField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");

        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        panel.add(buttonPanel, gbc);

        loginButton.addActionListener(e -> handleLogin());
        registerButton.addActionListener(e -> openRegisterFrame());

        add(panel);
    }

    private void handleLogin() {
        String email = emailField.getText();
        String password = new String(passwordField.getPassword());

        ActionResult<User> result = AppManager.getInstance().executeAction(new LoginAction(email, password));

        if (result.isSuccess()) {
            User user = result.getData();
            JOptionPane.showMessageDialog(this, "Welcome back, " + user.getFirstName() + "!",
                    "Login Successful", JOptionPane.INFORMATION_MESSAGE);

            this.dispose();

            if (user.getRole() == UserRole.ADMIN) {
                new AdminDashboardFrame(user).setVisible(true);
            } else {
                new CustomerDashboardFrame(user).setVisible(true);
            }
        } else {
            JOptionPane.showMessageDialog(this, result.getMessage(), "Authentication Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openRegisterFrame() {
        new RegisterFrame(this).setVisible(true);
        this.setVisible(false);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
