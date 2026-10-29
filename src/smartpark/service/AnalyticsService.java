package smartpark.service;

import smartpark.model.Ticket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Business analytics and intelligence engine for SmartPark.
 * Computes executive KPIs including occupancy rates, vehicle mix distributions,
 * average transaction values, and revenue performance.
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
     * Calculates the average fee collected across all historical departures.
     *
     * @return average transaction value in INR
     */
    public double getAverageTransactionValue() {
        int totalSessions = parkingLot.getTicketHistory().size();
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
     * Generates a comprehensive analytical intelligence report.
     *
     * @return multi-line executive summary
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
        sb.append("          📊 SMARTPARK BUSINESS ANALYTICS & INSIGHTS            \n");
        sb.append(line).append("\n");
        sb.append(String.format("Facility Utilization : %.1f%% (Capacity: %d, Occupied: %d)%n",
                getOccupancyRate(), parkingLot.getTotalCapacity(), parkingLot.getOccupiedSlotsCount()));
        sb.append(String.format("Total Revenue To-Date: Rs. %.2f%n", parkingLot.getTotalRevenue()));
        sb.append(String.format("Average Ticket Value : Rs. %.2f%n", getAverageTransactionValue()));
        sb.append("----------------------------------------------------------------\n");
        sb.append("ACTIVE VEHICLE MIX DISTRIBUTION:\n");
        sb.append(String.format(" - Passenger Cars : %d (%.1f%%)%n", mix.get("Car"), carPct));
        sb.append(String.format(" - Two-Wheelers   : %d (%.1f%%)%n", mix.get("Bike"), bikePct));
        sb.append(String.format(" - Heavy Trucks   : %d (%.1f%%)%n", mix.get("Truck"), truckPct));
        sb.append(line);

        return sb.toString();
    }
}
