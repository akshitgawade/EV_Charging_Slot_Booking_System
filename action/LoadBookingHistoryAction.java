package com.evcharging.action;

import com.evcharging.manager.AppManager;
import com.evcharging.model.Booking;

import java.util.List;

public class LoadBookingHistoryAction implements Action<List<Booking>> {

    private final Long userId;

    public LoadBookingHistoryAction(Long userId) {
        this.userId = userId;
    }

    @Override
    public ActionResult<List<Booking>> execute() throws Exception {
        if (userId == null) {
            return ActionResult.failure("User ID cannot be null.");
        }
        List<Booking> bookings = AppManager.getInstance().getBookingService().getBookingsForUser(userId);
        return ActionResult.success(bookings);
    }
}
