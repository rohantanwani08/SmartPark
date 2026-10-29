package smartpark.service;

import smartpark.model.Floor;
import smartpark.model.Ticket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Business analytics and intelligence engine for SmartPark.
 * Computes executive KPIs including occupancy rates, vehicle mix distributions,
 * average transaction values, payment channel splits, and floor-by-floor utilization.
 */
public class AnalyticsService {
    private final ParkingLot parkingLot;

    public AnalyticsService(ParkingLot parkingLot) {
        this.parkingLot = parkingLot;
    }

    /**
     * Calculates the current facility occupancy rate percentage.
     *
     * @return occupancy percentage between 0.0 and 100.0
     */
    public double getOccupancyRate() {
        int total = parkingLot.getTotalCapacity();
        if (total == 0) return 0.0;
        return (parkingLot.getOccupiedSlotsCount() * 100.0) / total;
    }

    /**
     * Calculates the breakdown of actively parked vehicles by category.
     *
     * @return map of vehicle category to count
     */
    public Map<String, Integer> getActiveVehicleMix() {
        Map<String, Integer> mix = new HashMap<>();
        mix.put("Car", 0);
        mix.put("Bike", 0);
        mix.put("Truck", 0);

        for (Ticket ticket : parkingLot.getActiveTickets()) {
            String type = ticket.getVehicle().getVehicleType();
            mix.put(type, mix.getOrDefault(type, 0) + 1);
        }
        return mix;
    }

    /**
     * Calculates the occupancy percentage for each floor individually.
     *
     * @return map of floor number to occupancy rate percentage
     */
    public Map<Integer, Double> getFloorOccupancyRates() {
        Map<Integer, Double> rates = new HashMap<>();
        for (Floor floor : parkingLot.getFloors()) {
            int total = floor.getTotalSlots();
            int occupied = floor.getOccupiedCount();
            double pct = total > 0 ? (occupied * 100.0) / total : 0.0;
            rates.put(floor.getFloorNumber(), pct);
        }
        return rates;
    }

    /**
     * Computes the distribution of revenue across payment channels (Cash vs UPI).
     *
     * @return map of channel name to total collected amount
     */
    public Map<String, Double> getPaymentChannelRevenue() {
        Map<String, Double> channelRev = new HashMap<>();
        channelRev.put("Cash", 0.0);
        channelRev.put("UPI", 0.0);

        if (parkingLot.getFileManager() != null) {
            try {
                List<FileManager.AuditLogEntry> logs = parkingLot.getFileManager().readBinaryLogs();
                for (FileManager.AuditLogEntry log : logs) {
                    String mode = log.getPaymentMode();
                    channelRev.put(mode, channelRev.getOrDefault(mode, 0.0) + log.getFeePaid());
                }
            } catch (Exception ignored) {}
        }
        return channelRev;
    }

    /**
     * Calculates the total count of completed historical departures.
     */
    public int getTotalHistoricalSessions() {
        if (parkingLot.getFileManager() != null) {
            try {
                List<FileManager.AuditLogEntry> logs = parkingLot.getFileManager().readBinaryLogs();
                if (!logs.isEmpty()) {
                    return logs.size();
                }
            } catch (Exception ignored) {}
        }
        return parkingLot.getTicketHistory().size();
    }

    /**
     * Calculates the average fee collected across all historical departures.
     *
     * @return average transaction value in INR
     */
    public double getAverageTransactionValue() {
        int totalSessions = getTotalHistoricalSessions();
        if (parkingLot.getFileManager() != null) {
            try {
                List<FileManager.AuditLogEntry> logs = parkingLot.getFileManager().readBinaryLogs();
                if (!logs.isEmpty()) {
                    double total = 0.0;
                    for (FileManager.AuditLogEntry log : logs) {
                        total += log.getFeePaid();
                    }
                    return total / logs.size();
                }
            } catch (Exception ignored) {}
        }
        if (totalSessions == 0) return 0.0;
        return parkingLot.getTotalRevenue() / totalSessions;
    }

    /**
     * Generates a comprehensive analytical intelligence report string.
     *
     * @return formatted executive summary
     */
    public String getExecutiveSummary() {
        Map<String, Integer> mix = getActiveVehicleMix();
        int totalActive = parkingLot.getActiveTickets().size();

        double carPct = totalActive > 0 ? (mix.get("Car") * 100.0) / totalActive : 0.0;
        double bikePct = totalActive > 0 ? (mix.get("Bike") * 100.0) / totalActive : 0.0;
        double truckPct = totalActive > 0 ? (mix.get("Truck") * 100.0) / totalActive : 0.0;

        StringBuilder sb = new StringBuilder();
        String line = "================================================================";
        sb.append(line).append("\n");
        sb.append("          SMARTPARK EXECUTIVE BUSINESS ANALYTICS REPORT        \n");
        sb.append(line).append("\n");
        sb.append(String.format("Facility Utilization : %.1f%% (Capacity: %d, Occupied: %d)%n",
                getOccupancyRate(), parkingLot.getTotalCapacity(), parkingLot.getOccupiedSlotsCount()));
        sb.append(String.format("Total Revenue To-Date: Rs. %.2f%n", parkingLot.getTotalRevenue()));
        sb.append(String.format("Historical Departures: %d sessions settled%n", getTotalHistoricalSessions()));
        sb.append(String.format("Average Ticket Value : Rs. %.2f%n", getAverageTransactionValue()));
        sb.append("----------------------------------------------------------------\n");
        sb.append("ACTIVE VEHICLE MIX DISTRIBUTION:\n");
        sb.append(String.format(" - Passenger Cars : %d (%.1f%%)%n", mix.get("Car"), carPct));
        sb.append(String.format(" - Two-Wheelers   : %d (%.1f%%)%n", mix.get("Bike"), bikePct));
        sb.append(String.format(" - Heavy Trucks   : %d (%.1f%%)%n", mix.get("Truck"), truckPct));
        sb.append("----------------------------------------------------------------\n");
        sb.append("FLOOR-BY-FLOOR OCCUPANCY BREAKDOWN:\n");
        Map<Integer, Double> floorRates = getFloorOccupancyRates();
        for (Floor f : parkingLot.getFloors()) {
            sb.append(String.format(" - Floor %d: %.1f%% Occupied (%d / %d slots taken)%n",
                    f.getFloorNumber(), floorRates.get(f.getFloorNumber()), f.getOccupiedCount(), f.getTotalSlots()));
        }
        sb.append(line);

        return sb.toString();
    }
}
