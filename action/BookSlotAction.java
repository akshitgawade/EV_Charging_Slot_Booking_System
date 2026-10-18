package com.evcharging.action;

import com.evcharging.manager.AppManager;

import java.time.LocalDateTime;

public class BookSlotAction implements Action<Long> {

    private final Long userId;
    private final Long slotId;
    private final LocalDateTime startTime;

    public BookSlotAction(Long userId, Long slotId, LocalDateTime startTime) {
        this.userId = userId;
        this.slotId = slotId;
        this.startTime = startTime;
    }

    @Override
    public ActionResult<Long> execute() throws Exception {
        Long bookingId = AppManager.getInstance().getBookingService()
                .createBooking(userId, slotId, startTime);
        return ActionResult.success(bookingId, "Booking confirmed successfully. Booking ID: " + bookingId);
    }
}
