package com.evcharging.action;

import com.evcharging.manager.AppManager;
import com.evcharging.model.StationStatus;

public class UpdateStationStatusAction implements Action<Boolean> {

    private final Long stationId;
    private final StationStatus newStatus;

    public UpdateStationStatusAction(Long stationId, StationStatus newStatus) {
        this.stationId = stationId;
        this.newStatus = newStatus;
    }

    @Override
    public ActionResult<Boolean> execute() throws Exception {
        if (stationId == null || newStatus == null) {
            return ActionResult.failure("Station ID and status are required.");
        }
        boolean updated = AppManager.getInstance().getAdminService().updateStationStatus(stationId, newStatus);
        if (updated) {
            return ActionResult.success(true, "Station status updated to " + newStatus);
        } else {
            return ActionResult.failure("Failed to update station status.");
        }
    }
}
