package com.evcharging.ui;

import com.evcharging.exception.ValidationException;
import com.evcharging.model.User;
import com.evcharging.service.AuthService;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

public class RegisterFrame extends JFrame {

    private final AuthService authService;
    private final JFrame parentFrame;

    private JTextField firstNameField;
    private JTextField lastNameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;

    public RegisterFrame(JFrame parentFrame) {
        this.authService = new AuthService();
        this.parentFrame = parentFrame;
        initComponents();
    }

    private void initComponents() {
        setTitle("EV Charging System - Account Registration");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(450, 420);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Create Customer Account", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        gbc.gridwidth = 1;

        // First Name
        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("First Name:"), gbc);
        firstNameField = new JTextField(18);
        gbc.gridx = 1; gbc.gridy = 1; panel.add(firstNameField, gbc);

        // Last Name
        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("Last Name:"), gbc);
        lastNameField = new JTextField(18);
        gbc.gridx = 1; gbc.gridy = 2; panel.add(lastNameField, gbc);

        // Email
        gbc.gridx = 0; gbc.gridy = 3; panel.add(new JLabel("Email:"), gbc);
        emailField = new JTextField(18);
        gbc.gridx = 1; gbc.gridy = 3; panel.add(emailField, gbc);

        // Phone
        gbc.gridx = 0; gbc.gridy = 4; panel.add(new JLabel("Phone (starts with 9876):"), gbc);
        phoneField = new JTextField(18);
        gbc.gridx = 1; gbc.gridy = 4; panel.add(phoneField, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 5; panel.add(new JLabel("Password:"), gbc);
        passwordField = new JPasswordField(18);
        gbc.gridx = 1; gbc.gridy = 5; panel.add(passwordField, gbc);

        // Confirm Password
        gbc.gridx = 0; gbc.gridy = 6; panel.add(new JLabel("Confirm Password:"), gbc);
        confirmPasswordField = new JPasswordField(18);
        gbc.gridx = 1; gbc.gridy = 6; panel.add(confirmPasswordField, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton registerButton = new JButton("Register");
        JButton backButton = new JButton("Back to Login");

        buttonPanel.add(registerButton);
        buttonPanel.add(backButton);

        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        panel.add(buttonPanel, gbc);

        registerButton.addActionListener(e -> handleRegistration());
        backButton.addActionListener(e -> backToLogin());

        add(panel);
    }

    private void handleRegistration() {
        String firstName = firstNameField.getText();
        String lastName = lastNameField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            User newUser = authService.registerCustomer(firstName, lastName, email, phone, password);
            JOptionPane.showMessageDialog(this, "Registration successful! You can now log in.", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            backToLogin();

        } catch (ValidationException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error during registration: " + e.getMessage(), 
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void backToLogin() {
        this.dispose();
        if (parentFrame != null) {
            parentFrame.setVisible(true);
        }
    }
}