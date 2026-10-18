package com.evcharging.action;

import com.evcharging.manager.AppManager;
import com.evcharging.model.EVStation;

import java.util.List;

public class LoadAllStationsAction implements Action<List<EVStation>> {

    @Override
    public ActionResult<List<EVStation>> execute() throws Exception {
        List<EVStation> stations = AppManager.getInstance().getAdminService().listAllStations();
        return ActionResult.success(stations);
    }
}
