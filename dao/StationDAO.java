package com.evcharging.dao;

import com.evcharging.model.EVStation;
import com.evcharging.model.StationStatus;
import com.evcharging.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StationDAO {

    public List<EVStation> findActiveStations(String searchText, String city) throws SQLException {
        List<EVStation> stations = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM ev_stations WHERE status = 'ACTIVE'");

        if (searchText != null && !searchText.trim().isEmpty()) {
            sql.append(" AND station_name LIKE ?");
        }
        if (city != null && !city.trim().isEmpty()) {
            sql.append(" AND city LIKE ?");
        }
        sql.append(" ORDER BY station_name");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (searchText != null && !searchText.trim().isEmpty()) {
                stmt.setString(paramIndex++, "%" + searchText.trim() + "%");
            }
            if (city != null && !city.trim().isEmpty()) {
                stmt.setString(paramIndex++, "%" + city.trim() + "%");
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    stations.add(mapResultSetToStation(rs));
                }
            }
        }
        return stations;
    }

    public List<EVStation> listAllStations() throws SQLException {
        List<EVStation> stations = new ArrayList<>();
        String sql = "SELECT * FROM ev_stations ORDER BY station_id";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                stations.add(mapResultSetToStation(rs));
            }
        }
        return stations;
    }

    public Optional<EVStation> findById(Long stationId) throws SQLException {
        String sql = "SELECT * FROM ev_stations WHERE station_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, stationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToStation(rs));
                }
            }
        }
        return Optional.empty();
    }

    public EVStation createStation(EVStation station) throws SQLException {
        String sql = "INSERT INTO ev_stations (station_name, address, city, state, postal_code, latitude, longitude, opening_time, closing_time, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, station.getStationName());
            stmt.setString(2, station.getAddress());
            stmt.setString(3, station.getCity());
            stmt.setString(4, station.getState());
            stmt.setString(5, station.getPostalCode());
            stmt.setObject(6, station.getLatitude(), Types.DECIMAL);
            stmt.setObject(7, station.getLongitude(), Types.DECIMAL);
            stmt.setTime(8, Time.valueOf(station.getOpeningTime()));
            stmt.setTime(9, Time.valueOf(station.getClosingTime()));
            stmt.setString(10, station.getStatus().name());

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    station.setStationId(rs.getLong(1));
                }
            }
        }
        return station;
    }

    public boolean updateStationStatus(Long stationId, StationStatus status) throws SQLException {
        String sql = "UPDATE ev_stations SET status = ? WHERE station_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setLong(2, stationId);
            return stmt.executeUpdate() > 0;
        }
    }

    private EVStation mapResultSetToStation(ResultSet rs) throws SQLException {
        EVStation station = new EVStation();
        station.setStationId(rs.getLong("station_id"));
        station.setStationName(rs.getString("station_name"));
        station.setAddress(rs.getString("address"));
        station.setCity(rs.getString("city"));
        station.setState(rs.getString("state"));
        station.setPostalCode(rs.getString("postal_code"));
        station.setLatitude(rs.getBigDecimal("latitude"));
        station.setLongitude(rs.getBigDecimal("longitude"));
        station.setOpeningTime(rs.getTime("opening_time").toLocalTime());
        station.setClosingTime(rs.getTime("closing_time").toLocalTime());
        station.setStatus(StationStatus.valueOf(rs.getString("status")));
        station.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return station;
    }
}
