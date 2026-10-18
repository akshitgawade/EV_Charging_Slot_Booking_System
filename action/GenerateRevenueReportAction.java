package com.evcharging.action;

import com.evcharging.manager.AppManager;

import java.time.LocalDateTime;
import java.util.Map;

public class GenerateRevenueReportAction implements Action<Map<String, Object>> {

    private final Long stationId;
    private final LocalDateTime fromDate;
    private final LocalDateTime toDate;

    public GenerateRevenueReportAction(Long stationId, LocalDateTime fromDate, LocalDateTime toDate) {
        this.stationId = stationId;
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    @Override
    public ActionResult<Map<String, Object>> execute() throws Exception {
        if (stationId == null) {
            return ActionResult.failure("Station ID is required to generate revenue report.");
        }
        Map<String, Object> report = AppManager.getInstance().getAdminService()
                .getStationRevenueReport(stationId, fromDate, toDate);
        return ActionResult.success(report);
    }
}
