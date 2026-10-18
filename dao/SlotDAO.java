package com.evcharging.dao;

import com.evcharging.model.ChargingSlot;
import com.evcharging.model.ConnectorType;
import com.evcharging.model.SlotStatus;
import com.evcharging.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SlotDAO {

    public List<ChargingSlot> findByStation(Long stationId) throws SQLException {
        List<ChargingSlot> slots = new ArrayList<>();
        String sql = "SELECT * FROM charging_slots WHERE station_id = ? ORDER BY slot_number";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, stationId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    slots.add(mapResultSetToSlot(rs));
                }
            }
        }
        return slots;
    }

    public Optional<ChargingSlot> findById(Long slotId) throws SQLException {
        String sql = "SELECT * FROM charging_slots WHERE slot_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, slotId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSlot(rs));
                }
            }
        }
        return Optional.empty();
    }

    public ChargingSlot createSlot(ChargingSlot slot) throws SQLException {
        String sql = "INSERT INTO charging_slots (station_id, slot_number, connector_type, max_power_kw, hourly_rate, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setLong(1, slot.getStationId());
            stmt.setInt(2, slot.getSlotNumber());
            stmt.setString(3, slot.getConnectorType().name());
            stmt.setBigDecimal(4, slot.getMaxPowerKw());
            stmt.setBigDecimal(5, slot.getHourlyRate());
            stmt.setString(6, slot.getStatus().name());

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    slot.setSlotId(rs.getLong(1));
                }
            }
        }
        return slot;
    }

    public boolean updateSlotStatus(Long slotId, SlotStatus status) throws SQLException {
        String sql = "UPDATE charging_slots SET status = ? WHERE slot_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setLong(2, slotId);
            return stmt.executeUpdate() > 0;
        }
    }

    private ChargingSlot mapResultSetToSlot(ResultSet rs) throws SQLException {
        ChargingSlot slot = new ChargingSlot();
        slot.setSlotId(rs.getLong("slot_id"));
        slot.setStationId(rs.getLong("station_id"));
        slot.setSlotNumber(rs.getInt("slot_number"));
        slot.setConnectorType(ConnectorType.valueOf(rs.getString("connector_type")));
        slot.setMaxPowerKw(rs.getBigDecimal("max_power_kw"));
        slot.setHourlyRate(rs.getBigDecimal("hourly_rate"));
        slot.setStatus(SlotStatus.valueOf(rs.getString("status")));
        slot.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return slot;
    }
}
