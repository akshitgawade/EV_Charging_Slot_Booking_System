package com.evcharging.action;

import com.evcharging.dao.SlotDAO;
import com.evcharging.model.ChargingSlot;

import java.util.List;

public class LoadStationSlotsAction implements Action<List<ChargingSlot>> {

    private final Long stationId;
    private final SlotDAO slotDAO;

    public LoadStationSlotsAction(Long stationId) {
        this.stationId = stationId;
        this.slotDAO = new SlotDAO();
    }

    @Override
    public ActionResult<List<ChargingSlot>> execute() throws Exception {
        if (stationId == null) {
            return ActionResult.failure("Station ID is required.");
        }
        List<ChargingSlot> slots = slotDAO.findByStation(stationId);
        return ActionResult.success(slots);
    }
}
