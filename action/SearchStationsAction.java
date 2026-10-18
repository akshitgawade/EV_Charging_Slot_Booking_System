package com.evcharging.action;

import com.evcharging.dao.StationDAO;
import com.evcharging.model.EVStation;

import java.util.List;

public class SearchStationsAction implements Action<List<EVStation>> {

    private final String searchText;
    private final String city;
    private final StationDAO stationDAO;

    public SearchStationsAction(String searchText, String city) {
        this.searchText = searchText;
        this.city = city;
        this.stationDAO = new StationDAO();
    }

    @Override
    public ActionResult<List<EVStation>> execute() throws Exception {
        List<EVStation> stations = stationDAO.findActiveStations(searchText, city);
        return ActionResult.success(stations);
    }
}
