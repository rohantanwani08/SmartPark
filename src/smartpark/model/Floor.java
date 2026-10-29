package smartpark.model;

import smartpark.exception.InvalidVehicleException;

/**
 * Represents a single level/floor within the parking structure.
 * Demonstrates composition: Floor "has-a" fixed array of ParkingSlot instances
 * whose lifecycles are completely managed by the Floor.
 */
public class Floor {
    private final int floorNumber;
    private final ParkingSlot[] slots;

    /**
     * Constructs a Floor and initializes its designated parking slots.
     *
     * @param floorNumber numeric floor index (e.g. 1, 2, 3)
     * @param carSlots    number of designated car slots
     * @param bikeSlots   number of designated bike slots
     * @param truckSlots  number of designated truck slots
     * @throws InvalidVehicleException if floorNumber < 1 or any slot count is negative
     */
    public Floor(int floorNumber, int carSlots, int bikeSlots, int truckSlots) throws InvalidVehicleException {
        if (floorNumber < 1) {
            throw new InvalidVehicleException("Floor number must be at least 1.");
        }
        if (carSlots < 0 || bikeSlots < 0 || truckSlots < 0) {
            throw new InvalidVehicleException("Slot allocations cannot be negative.");
        }
        int total = carSlots + bikeSlots + truckSlots;
        if (total == 0) {
            throw new InvalidVehicleException("A floor must contain at least one parking slot.");
        }

        this.floorNumber = floorNumber;
        this.slots = new ParkingSlot[total];

        int index = 0;
        // Allocate Car slots
        for (int i = 1; i <= carSlots; i++) {
            String slotId = String.format("F%d-CAR-%02d", floorNumber, i);
            this.slots[index++] = new ParkingSlot(slotId, "Car");
        }
        // Allocate Bike slots
        for (int i = 1; i <= bikeSlots; i++) {
            String slotId = String.format("F%d-BIKE-%02d", floorNumber, i);
            this.slots[index++] = new ParkingSlot(slotId, "Bike");
        }
        // Allocate Truck slots
        for (int i = 1; i <= truckSlots; i++) {
            String slotId = String.format("F%d-TRK-%02d", floorNumber, i);
            this.slots[index++] = new ParkingSlot(slotId, "Truck");
        }
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public ParkingSlot[] getSlots() {
        return slots;
    }

    public int getTotalSlots() {
        return slots.length;
    }

    /**
     * Finds the first available slot that can fit the given vehicle.
     *
     * @param vehicle vehicle looking for a spot
     * @return a free compatible ParkingSlot, or null if all compatible slots are taken
     */
    public ParkingSlot findFreeSlot(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }
        for (ParkingSlot slot : slots) {
            if (slot.canFit(vehicle)) {
                return slot;
            }
        }
        return null;
    }

    /**
     * Counts how many slots of a specific vehicle type are currently free.
     *
     * @param vehicleType type to filter by (e.g. "Car", "Bike", "Truck")
     * @return count of available slots
     */
    public int getFreeCount(String vehicleType) {
        int count = 0;
        for (ParkingSlot slot : slots) {
            if (slot.isAvailable() && slot.getSupportedType().equalsIgnoreCase(vehicleType)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Counts total currently occupied slots on this floor.
     *
     * @return count of occupied slots
     */
    public int getOccupiedCount() {
        int count = 0;
        for (ParkingSlot slot : slots) {
            if (!slot.isAvailable()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String toString() {
        return String.format("Floor %d [Total: %d | Occupied: %d | Free: %d]",
                floorNumber, slots.length, getOccupiedCount(), (slots.length - getOccupiedCount()));
    }
}
