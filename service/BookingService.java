package com.evcharging.service;

import com.evcharging.dao.BookingDAO;
import com.evcharging.dao.SlotDAO;
import com.evcharging.dao.StationDAO;
import com.evcharging.exception.BookingConflictException;
import com.evcharging.exception.ValidationException;
import com.evcharging.model.ChargingSlot;
import com.evcharging.model.EVStation;
import com.evcharging.model.SlotStatus;
import com.evcharging.model.StationStatus;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

public class BookingService {

    private final BookingDAO bookingDAO;
    private final SlotDAO slotDAO;
    private final StationDAO stationDAO;

    public BookingService() {
        this.bookingDAO = new BookingDAO();
        this.slotDAO = new SlotDAO();
        this.stationDAO = new StationDAO();
    }

    public BookingService(BookingDAO bookingDAO, SlotDAO slotDAO, StationDAO stationDAO) {
        this.bookingDAO = bookingDAO;
        this.slotDAO = slotDAO;
        this.stationDAO = stationDAO;
    }

    public Long createBooking(Long userId, Long slotId, LocalDateTime startTime)
            throws ValidationException, BookingConflictException, SQLException {

        if (userId == null || slotId == null || startTime == null) {
            throw new ValidationException("Invalid booking parameters.");
        }

        if (startTime.isBefore(LocalDateTime.now())) {
            throw new ValidationException("Booking time must be in the future.");
        }

        if (startTime.getMinute() != 0 || startTime.getSecond() != 0 || startTime.getNano() != 0) {
            throw new ValidationException("Bookings must start on an exact hour (e.g., 10:00, 11:00).");
        }

        Optional<ChargingSlot> slotOpt = slotDAO.findById(slotId);
        if (slotOpt.isEmpty()) {
            throw new ValidationException("The selected charging slot does not exist.");
        }
        ChargingSlot slot = slotOpt.get();
        if (slot.getStatus() != SlotStatus.ACTIVE) {
            throw new ValidationException("This charging slot is currently unavailable or under maintenance.");
        }

        Optional<EVStation> stationOpt = stationDAO.findById(slot.getStationId());
        if (stationOpt.isEmpty()) {
            throw new ValidationException("The associated EV station does not exist.");
        }
        EVStation station = stationOpt.get();
        if (station.getStatus() != StationStatus.ACTIVE) {
            throw new ValidationException("The EV station is currently closed or under maintenance.");
        }

        LocalTime bookingStartLocalTime = startTime.toLocalTime();
        LocalTime bookingEndLocalTime = startTime.plusHours(1).toLocalTime();

        if (bookingStartLocalTime.isBefore(station.getOpeningTime()) ||
            (bookingEndLocalTime.isAfter(station.getClosingTime()) && !bookingEndLocalTime.equals(LocalTime.MIDNIGHT))) {
            throw new ValidationException("Requested booking interval falls outside station operating hours (" +
                    station.getOpeningTime() + " - " + station.getClosingTime() + ").");
        }

        try {
            return bookingDAO.bookSlotTransactional(userId, slotId, startTime);
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Booking conflict")) {
                throw new BookingConflictException("The selected time slot has already been booked by another user.");
            }
            throw e;
        }
    }

    public java.util.List<com.evcharging.model.Booking> getBookingsForUser(Long userId) throws SQLException {
        if (userId == null) {
            return new java.util.ArrayList<>();
        }
        return bookingDAO.findBookingsByUserId(userId);
    }

    public boolean cancelBooking(Long bookingId) throws SQLException, ValidationException {
        if (bookingId == null) {
            throw new ValidationException("Booking ID is required for cancellation.");
        }
        return bookingDAO.cancelBooking(bookingId);
    }
}
