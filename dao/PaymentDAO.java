package com.evcharging.dao;

import com.evcharging.model.Payment;
import com.evcharging.model.PaymentMethod;
import com.evcharging.model.PaymentStatus;
import com.evcharging.util.DBConnection;

import java.sql.*;
import java.util.Optional;

public class PaymentDAO {

    public Optional<Payment> findByBookingId(Long bookingId) throws SQLException {
        String sql = "SELECT * FROM payments WHERE booking_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, bookingId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPayment(rs));
                }
            }
        }
        return Optional.empty();
    }

    public boolean updateRefunded(Long bookingId) throws SQLException {
        String sql = "UPDATE payments SET status = 'REFUNDED' WHERE booking_id = ? AND status = 'PAID'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, bookingId);
            return stmt.executeUpdate() > 0;
        }
    }

    private Payment mapResultSetToPayment(ResultSet rs) throws SQLException {
        Payment payment = new Payment();
        payment.setPaymentId(rs.getLong("payment_id"));
        payment.setBookingId(rs.getLong("booking_id"));
        payment.setAmount(rs.getBigDecimal("amount"));
        payment.setMethod(PaymentMethod.valueOf(rs.getString("method")));
        payment.setStatus(PaymentStatus.valueOf(rs.getString("status")));
        payment.setTransactionReference(rs.getString("transaction_reference"));
        payment.setPaidAt(rs.getTimestamp("paid_at").toLocalDateTime());
        payment.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return payment;
    }
}
