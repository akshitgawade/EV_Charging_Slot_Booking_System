package com.evcharging.ui;

import com.evcharging.action.ActionResult;
import com.evcharging.action.GenerateRevenueReportAction;
import com.evcharging.action.LoadAllStationsAction;
import com.evcharging.action.UpdateStationStatusAction;
import com.evcharging.manager.AppManager;
import com.evcharging.model.EVStation;
import com.evcharging.model.StationStatus;
import com.evcharging.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AdminDashboardFrame extends JFrame {

    private final User adminUser;

    private JTable adminStationTable;
    private DefaultTableModel adminStationTableModel;
    private JTextArea reportTextArea;

    public AdminDashboardFrame(User user) {
        this.adminUser = user;
        initComponents();
        loadAllStations();
    }

    private void initComponents() {
        setTitle("EV Charging System - Admin Control Center (" + adminUser.getFirstName() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 550);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel stationPanel = new JPanel(new BorderLayout(10, 10));
        stationPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        adminStationTableModel = new DefaultTableModel(new String[]{"ID", "Station Name", "City", "Hours", "Status"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        adminStationTable = new JTable(adminStationTableModel);
        stationPanel.add(new JScrollPane(adminStationTable), BorderLayout.CENTER);

        JPanel stationControlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton activateBtn = new JButton("Set Active");
        JButton maintenanceBtn = new JButton("Set Maintenance");
        JButton refreshBtn = new JButton("Refresh");

        stationControlPanel.add(activateBtn);
        stationControlPanel.add(maintenanceBtn);
        stationControlPanel.add(refreshBtn);
        stationPanel.add(stationControlPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("Station Management", stationPanel);

        JPanel reportPanel = new JPanel(new BorderLayout(10, 10));
        reportPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel reportFilterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        reportFilterPanel.add(new JLabel("Select Station:"));

        JButton generateReportBtn = new JButton("Generate Revenue Report");
        reportFilterPanel.add(generateReportBtn);
        reportPanel.add(reportFilterPanel, BorderLayout.NORTH);

        reportTextArea = new JTextArea();
        reportTextArea.setEditable(false);
        reportTextArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        reportPanel.add(new JScrollPane(reportTextArea), BorderLayout.CENTER);

        tabbedPane.addTab("Revenue Analytics", reportPanel);

        refreshBtn.addActionListener(e -> loadAllStations());
        activateBtn.addActionListener(e -> updateStatus(StationStatus.ACTIVE));
        maintenanceBtn.addActionListener(e -> updateStatus(StationStatus.MAINTENANCE));
        generateReportBtn.addActionListener(e -> handleGenerateReport());

        add(tabbedPane);
    }

    private void loadAllStations() {
        adminStationTableModel.setRowCount(0);
        ActionResult<List<EVStation>> result = AppManager.getInstance()
                .executeAction(new LoadAllStationsAction());

        if (result.isSuccess()) {
            for (EVStation s : result.getData()) {
                adminStationTableModel.addRow(new Object[]{
                        s.getStationId(), s.getStationName(), s.getCity(),
                        s.getOpeningTime() + " - " + s.getClosingTime(), s.getStatus()
                });
            }
        } else {
            JOptionPane.showMessageDialog(this, result.getMessage(), "Error Fetching Stations", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateStatus(StationStatus newStatus) {
        int selectedRow = adminStationTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a station from the table.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long stationId = (Long) adminStationTableModel.getValueAt(selectedRow, 0);
        ActionResult<Boolean> result = AppManager.getInstance()
                .executeAction(new UpdateStationStatusAction(stationId, newStatus));

        if (result.isSuccess()) {
            JOptionPane.showMessageDialog(this, result.getMessage(), "Success", JOptionPane.INFORMATION_MESSAGE);
            loadAllStations();
        } else {
            JOptionPane.showMessageDialog(this, result.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleGenerateReport() {
        int selectedRow = adminStationTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a station from the 'Station Management' tab first.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long stationId = (Long) adminStationTableModel.getValueAt(selectedRow, 0);
        LocalDateTime fromDate = LocalDateTime.now().minusMonths(6);
        LocalDateTime toDate = LocalDateTime.now().plusDays(1);

        ActionResult<Map<String, Object>> result = AppManager.getInstance()
                .executeAction(new GenerateRevenueReportAction(stationId, fromDate, toDate));

        if (result.isSuccess()) {
            Map<String, Object> report = result.getData();
            if (report == null || report.isEmpty()) {
                reportTextArea.setText("No data returned for selected station.");
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("====================================================\n");
            sb.append("           STATION REVENUE REPORT                   \n");
            sb.append("====================================================\n");
            sb.append(String.format(" Station ID:            %s\n", report.get("station_id")));
            sb.append(String.format(" Station Name:          %s\n", report.get("station_name")));
            sb.append(String.format(" Paid Bookings Count:   %s\n", report.get("paid_bookings_count")));
            sb.append("----------------------------------------------------\n");
            sb.append(String.format(" Total Gross Revenue:  ₹ %s\n", report.get("total_paid_revenue")));
            sb.append(String.format(" Total Refunds Issued: ₹ %s\n", report.get("total_refunded_amount")));
            sb.append("----------------------------------------------------\n");
            sb.append(String.format(" NET REVENUE:          ₹ %s\n", report.get("net_revenue")));
            sb.append("====================================================\n");

            reportTextArea.setText(sb.toString());
        } else {
            JOptionPane.showMessageDialog(this, result.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
