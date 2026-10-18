package com.evcharging.dao;

import com.evcharging.model.Booking;
import com.evcharging.model.BookingStatus;
import com.evcharging.util.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BookingDAO {

    public synchronized Long bookSlotTransactional(Long userId, Long slotId, LocalDateTime startTime) throws SQLException {
        LocalDateTime endTime = startTime.plusHours(1);

        String lockSlotSql = "SELECT cs.slot_id, cs.status AS slot_status, cs.hourly_rate, es.status AS station_status " +
                             "FROM charging_slots cs JOIN ev_stations es ON es.station_id = cs.station_id " +
                             "WHERE cs.slot_id = ? FOR UPDATE";

        String checkOverlapSql = "SELECT booking_id FROM bookings WHERE slot_id = ? AND status = 'CONFIRMED' " +
                                 "AND start_time < ? AND end_time > ? FOR UPDATE";

        String insertBookingSql = "INSERT INTO bookings (user_id, slot_id, start_time, end_time, rate_per_hour, status) " +
                                  "VALUES (?, ?, ?, ?, ?, 'CONFIRMED')";

        String insertPaymentSql = "INSERT INTO payments (booking_id, amount, method, status, transaction_reference) " +
                                  "VALUES (?, ?, 'MOCK', 'PAID', ?)";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            BigDecimal hourlyRate;

            try (PreparedStatement stmt = conn.prepareStatement(lockSlotSql)) {
                stmt.setLong(1, slotId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Slot not found.");
                    }
                    if (!"ACTIVE".equals(rs.getString("slot_status")) || !"ACTIVE".equals(rs.getString("station_status"))) {
                        throw new SQLException("Slot or Station is currently inactive/under maintenance.");
                    }
                    hourlyRate = rs.getBigDecimal("hourly_rate");
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(checkOverlapSql)) {
                stmt.setLong(1, slotId);
                stmt.setTimestamp(2, Timestamp.valueOf(endTime));
                stmt.setTimestamp(3, Timestamp.valueOf(startTime));
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        throw new SQLException("Booking conflict: Selected slot is already booked for this time interval.");
                    }
                }
            }

            Long bookingId;
            try (PreparedStatement stmt = conn.prepareStatement(insertBookingSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setLong(1, userId);
                stmt.setLong(2, slotId);
                stmt.setTimestamp(3, Timestamp.valueOf(startTime));
                stmt.setTimestamp(4, Timestamp.valueOf(endTime));
                stmt.setBigDecimal(5, hourlyRate);
                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        bookingId = rs.getLong(1);
                    } else {
                        throw new SQLException("Failed to retrieve generated booking ID.");
                    }
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(insertPaymentSql)) {
                stmt.setLong(1, bookingId);
                stmt.setBigDecimal(2, hourlyRate);
                stmt.setString(3, "TXN_MOCK_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                stmt.executeUpdate();
            }

            conn.commit();
            return bookingId;

        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    public List<Booking> findBookingsByUserId(Long userId) throws SQLException {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.booking_id, b.user_id, b.slot_id, b.start_time, b.end_time, " +
                     "b.rate_per_hour, b.status, es.station_name, cs.slot_number, p.status AS payment_status " +
                     "FROM bookings b " +
                     "JOIN charging_slots cs ON cs.slot_id = b.slot_id " +
                     "JOIN ev_stations es ON es.station_id = cs.station_id " +
                     "LEFT JOIN payments p ON p.booking_id = b.booking_id " +
                     "WHERE b.user_id = ? ORDER BY b.start_time DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Booking b = new Booking();
                    b.setBookingId(rs.getLong("booking_id"));
                    b.setUserId(rs.getLong("user_id"));
                    b.setSlotId(rs.getLong("slot_id"));
                    b.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
                    b.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
                    b.setRatePerHour(rs.getBigDecimal("rate_per_hour"));
                    b.setStatus(BookingStatus.valueOf(rs.getString("status")));
                    b.setStationName(rs.getString("station_name"));
                    b.setSlotNumber(rs.getInt("slot_number"));
                    b.setPaymentStatus(rs.getString("payment_status"));
                    list.add(b);
                }
            }
        }
        return list;
    }

    public boolean cancelBooking(Long bookingId) throws SQLException {
        String sql = "UPDATE bookings SET status = 'CANCELLED' WHERE booking_id = ? AND status = 'CONFIRMED'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, bookingId);
            return stmt.executeUpdate() > 0;
        }
    }
}
