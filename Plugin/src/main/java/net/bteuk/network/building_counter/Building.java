package net.bteuk.network.building_counter;

import org.bukkit.Location;

import java.time.LocalDateTime;

public record Building(int buildingId, Location coordinate, String playerId, int coordinateId, LocalDateTime timeCreated, boolean isPublic, boolean playerBuilt, double lat, double lon) {
}
