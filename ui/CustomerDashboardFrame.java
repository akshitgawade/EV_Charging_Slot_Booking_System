package com.evcharging.ui;

import com.evcharging.action.*;
import com.evcharging.manager.AppManager;
import com.evcharging.model.Booking;
import com.evcharging.model.ChargingSlot;
import com.evcharging.model.EVStation;
import com.evcharging.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CustomerDashboardFrame extends JFrame {

    private final User currentUser;

    private JTable stationTable;
    private DefaultTableModel stationTableModel;
    private JTable slotTable;
    private DefaultTableModel slotTableModel;
    private JTable historyTable;
    private DefaultTableModel historyTableModel;

    private JTextField searchField;
    private JTextField cityField;
    private JTextField bookingTimeField;

    public CustomerDashboardFrame(User user) {
        this.currentUser = user;
        initComponents();
        loadStations();
        loadBookingHistory();
    }

    private void initComponents() {
        setTitle("EV Charging - Customer Dashboard (" + currentUser.getFirstName() + " " + currentUser.getLastName() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel bookingPanel = new JPanel(new BorderLayout(10, 10));
        bookingPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.add(new JLabel("Station Name:"));
        searchField = new JTextField(12);
        searchPanel.add(searchField);

        searchPanel.add(new JLabel("City:"));
        cityField = new JTextField(10);
        searchPanel.add(cityField);

        JButton searchButton = new JButton("Search");
        searchPanel.add(searchButton);
        bookingPanel.add(searchPanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.5);

        stationTableModel = new DefaultTableModel(new String[]{"ID", "Station Name", "Address", "City", "Hours"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        stationTable = new JTable(stationTableModel);
        splitPane.setTopComponent(new JScrollPane(stationTable));

        slotTableModel = new DefaultTableModel(new String[]{"Slot ID", "Slot #", "Connector", "Power (kW)", "Rate/Hr (₹)", "Status"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        slotTable = new JTable(slotTableModel);
        splitPane.setBottomComponent(new JScrollPane(slotTable));

        bookingPanel.add(splitPane, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        actionPanel.add(new JLabel("Start Time (YYYY-MM-DD HH:00):"));
        bookingTimeField = new JTextField(15);
        bookingTimeField.setText(LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00")));
        actionPanel.add(bookingTimeField);

        JButton bookButton = new JButton("Confirm Booking");
        actionPanel.add(bookButton);
        bookingPanel.add(actionPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("Find & Book Charger", bookingPanel);

        JPanel historyPanel = new JPanel(new BorderLayout(10, 10));
        historyPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        historyTableModel = new DefaultTableModel(new String[]{"Booking ID", "Station", "Slot #", "Start Time", "End Time", "Rate (₹)", "Status"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        historyTable = new JTable(historyTableModel);
        historyPanel.add(new JScrollPane(historyTable), BorderLayout.CENTER);

        JPanel historyActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelBookingButton = new JButton("Cancel Selected Booking");
        JButton refreshHistoryButton = new JButton("Refresh");
        historyActionPanel.add(cancelBookingButton);
        historyActionPanel.add(refreshHistoryButton);
        historyPanel.add(historyActionPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("My Bookings History", historyPanel);

        searchButton.addActionListener(e -> loadStations());

        stationTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && stationTable.getSelectedRow() != -1) {
                Long stationId = (Long) stationTableModel.getValueAt(stationTable.getSelectedRow(), 0);
                loadSlotsForStation(stationId);
            }
        });

        bookButton.addActionListener(e -> handleBooking());
        cancelBookingButton.addActionListener(e -> handleCancelBooking());
        refreshHistoryButton.addActionListener(e -> loadBookingHistory());

        add(tabbedPane);
    }

    private void loadStations() {
        stationTableModel.setRowCount(0);

        ActionResult<List<EVStation>> result = AppManager.getInstance()
                .executeAction(new SearchStationsAction(searchField.getText(), cityField.getText()));

        if (result.isSuccess()) {
            for (EVStation s : result.getData()) {
                stationTableModel.addRow(new Object[]{
                        s.getStationId(), s.getStationName(), s.getAddress(), s.getCity(),
                        s.getOpeningTime() + " - " + s.getClosingTime()
                });
            }
        } else {
            JOptionPane.showMessageDialog(this, result.getMessage(), "Error Fetching Stations", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSlotsForStation(Long stationId) {
        slotTableModel.setRowCount(0);

        ActionResult<List<ChargingSlot>> result = AppManager.getInstance()
                .executeAction(new LoadStationSlotsAction(stationId));

        if (result.isSuccess()) {
            for (ChargingSlot slot : result.getData()) {
                slotTableModel.addRow(new Object[]{
                        slot.getSlotId(), slot.getSlotNumber(), slot.getConnectorType(),
                        slot.getMaxPowerKw(), slot.getHourlyRate(), slot.getStatus()
                });
            }
        } else {
            JOptionPane.showMessageDialog(this, result.getMessage(), "Error Fetching Slots", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleBooking() {
        int selectedSlotRow = slotTable.getSelectedRow();
        if (selectedSlotRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a charging slot from the table first.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long slotId = (Long) slotTableModel.getValueAt(selectedSlotRow, 0);

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String timeText = bookingTimeField.getText().trim();
            if (!timeText.contains(":")) timeText += ":00";

            LocalDateTime startTime = LocalDateTime.parse(timeText, formatter);

            ActionResult<Long> result = AppManager.getInstance()
                    .executeAction(new BookSlotAction(currentUser.getUserId(), slotId, startTime));

            if (result.isSuccess()) {
                JOptionPane.showMessageDialog(this, result.getMessage(), "Success", JOptionPane.INFORMATION_MESSAGE);
                loadBookingHistory();
            } else {
                JOptionPane.showMessageDialog(this, result.getMessage(), "Booking Failed", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid date/time format. Please use 'YYYY-MM-DD HH:00'.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadBookingHistory() {
        historyTableModel.setRowCount(0);

        ActionResult<List<Booking>> result = AppManager.getInstance()
                .executeAction(new LoadBookingHistoryAction(currentUser.getUserId()));

        if (result.isSuccess()) {
            for (Booking b : result.getData()) {
                historyTableModel.addRow(new Object[]{
                        b.getBookingId(),
                        b.getStationName() != null ? b.getStationName() : "N/A",
                        b.getSlotNumber() != null ? b.getSlotNumber() : 0,
                        b.getStartTime(),
                        b.getEndTime(),
                        b.getRatePerHour(),
                        b.getStatus()
                });
            }
        } else {
            JOptionPane.showMessageDialog(this, result.getMessage(), "Error Loading History", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleCancelBooking() {
        int selectedRow = historyTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a booking to cancel.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long bookingId = (Long) historyTableModel.getValueAt(selectedRow, 0);
        Object statusObj = historyTableModel.getValueAt(selectedRow, 6);
        String currentStatus = statusObj != null ? statusObj.toString() : "";

        if (!"CONFIRMED".equalsIgnoreCase(currentStatus)) {
            JOptionPane.showMessageDialog(this, "Only CONFIRMED bookings can be cancelled.", "Action Invalid", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to cancel booking #" + bookingId + "?", "Confirm Cancel", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {

            ActionResult<Boolean> result = AppManager.getInstance()
                    .executeAction(new CancelBookingAction(bookingId));

            if (result.isSuccess()) {
                JOptionPane.showMessageDialog(this, result.getMessage(), "Cancelled", JOptionPane.INFORMATION_MESSAGE);
                loadBookingHistory();
            } else {
                JOptionPane.showMessageDialog(this, result.getMessage(), "Cancellation Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
