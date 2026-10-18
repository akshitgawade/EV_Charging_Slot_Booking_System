package com.evcharging.service;

import com.evcharging.dao.SlotDAO;
import com.evcharging.dao.StationDAO;
import com.evcharging.exception.ValidationException;
import com.evcharging.model.ChargingSlot;
import com.evcharging.model.EVStation;
import com.evcharging.model.SlotStatus;
import com.evcharging.model.StationStatus;
import com.evcharging.util.DBConnection;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminService {

    private final StationDAO stationDAO;
    private final SlotDAO slotDAO;

    public AdminService() {
        this.stationDAO = new StationDAO();
        this.slotDAO = new SlotDAO();
    }

    public EVStation addStation(EVStation station) throws ValidationException, SQLException {
        if (station.getStationName() == null || station.getStationName().trim().isEmpty()) {
            throw new ValidationException("Station name is required.");
        }
        if (station.getOpeningTime() == null || station.getClosingTime() == null) {
            throw new ValidationException("Opening and closing times are required.");
        }
        if (!station.getOpeningTime().isBefore(station.getClosingTime())) {
            throw new ValidationException("Opening time must be strictly before closing time.");
        }
        return stationDAO.createStation(station);
    }

    public List<EVStation> listAllStations() throws SQLException {
        return stationDAO.listAllStations();
    }

    public boolean updateStationStatus(Long stationId, StationStatus status) throws SQLException {
        return stationDAO.updateStationStatus(stationId, status);
    }

    public ChargingSlot addSlot(ChargingSlot slot) throws ValidationException, SQLException {
        if (slot.getStationId() == null) {
            throw new ValidationException("Parent station ID is required.");
        }
        if (slot.getMaxPowerKw() == null || slot.getMaxPowerKw().doubleValue() <= 0) {
            throw new ValidationException("Max power in kW must be greater than 0.");
        }
        if (slot.getHourlyRate() == null || slot.getHourlyRate().doubleValue() < 0) {
            throw new ValidationException("Hourly rate cannot be negative.");
        }
        return slotDAO.createSlot(slot);
    }

    public boolean updateSlotStatus(Long slotId, SlotStatus status) throws SQLException {
        return slotDAO.updateSlotStatus(slotId, status);
    }

    public Map<String, Object> getStationRevenueReport(Long stationId, LocalDateTime fromDate, LocalDateTime toDate)
            throws SQLException {
        Map<String, Object> report = new HashMap<>();
        String sql = "{CALL sp_station_revenue(?, ?, ?)}";

        try (Connection conn = DBConnection.getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setLong(1, stationId);
            stmt.setTimestamp(2, Timestamp.valueOf(fromDate));
            stmt.setTimestamp(3, Timestamp.valueOf(toDate));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    report.put("station_id", rs.getLong("station_id"));
                    report.put("station_name", rs.getString("station_name"));
                    report.put("paid_bookings_count", rs.getInt("paid_bookings_count"));
                    report.put("total_paid_revenue", rs.getBigDecimal("total_paid_revenue"));
                    report.put("total_refunded_amount", rs.getBigDecimal("total_refunded_amount"));
                    report.put("net_revenue", rs.getBigDecimal("net_revenue"));
                }
            }
        }
        return report;
    }
}
