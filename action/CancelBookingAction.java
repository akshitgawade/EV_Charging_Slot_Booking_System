package com.evcharging.action;

import com.evcharging.manager.AppManager;

public class CancelBookingAction implements Action<Boolean> {

    private final Long bookingId;

    public CancelBookingAction(Long bookingId) {
        this.bookingId = bookingId;
    }

    @Override
    public ActionResult<Boolean> execute() throws Exception {
        boolean cancelled = AppManager.getInstance().getBookingService().cancelBooking(bookingId);
        if (cancelled) {
            return ActionResult.success(true, "Booking cancelled successfully. Payment marked as REFUNDED.");
        } else {
            return ActionResult.failure("Booking could not be cancelled. Only CONFIRMED bookings can be cancelled.");
        }
    }
}
