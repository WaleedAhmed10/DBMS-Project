import java.util.*;

class Vehicle {
    private String vehicleNum;
    private String ownerName;
    private long contactNum;

    public Vehicle(String vehicleNum, String ownerName, long contactNum) {
        this.vehicleNum = vehicleNum;
        this.ownerName = ownerName;
        this.contactNum = contactNum;
    }

    public String getVehicleNum() {
        return vehicleNum;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public long getContactNum() {
        return contactNum;
    }

    public double calculateFee(long duration) {
        return duration * 10;
    }

    public void displayDetails() {
        System.out.println("Vehicle Number: " + vehicleNum);
        System.out.println("Owner Name: " + ownerName);
        System.out.println("Contact Number: " + contactNum);
    }
}

class FourWheeler extends Vehicle {
    private String vehicleType;

    public FourWheeler(String vehicleNum, String ownerName, long contactNum, String vehicleType) {
        super(vehicleNum, ownerName, contactNum);
        this.vehicleType = vehicleType;
    }

    @Override
    public void displayDetails() {
        super.displayDetails();
        System.out.println("Vehicle Type: 4 Wheeler (" + vehicleType + ")");
    }
}

class TwoWheeler extends Vehicle {
    private boolean hasCarrier;

    public TwoWheeler(String vehicleNum, String ownerName, long contactNum, boolean hasCarrier) {
        super(vehicleNum, ownerName, contactNum);
        this.hasCarrier = hasCarrier;
    }

    @Override
    public void displayDetails() {
        super.displayDetails();
        System.out.println("Vehicle Type: 2 Wheeler");
        System.out.println("Has Carrier: " + (hasCarrier ? "Yes" : "No"));
    }
}

class ParkingSlot {
    private String slotType;
    private boolean isOccupied;

    public ParkingSlot(String slotType) {
        this.slotType = slotType;
        this.isOccupied = false;
    }

    public boolean isOccupied() {
        return isOccupied;
    }

    public void occupy() {
        isOccupied = true;
    }

    public void vacate() {
        isOccupied = false;
    }

    public String getSlotType() {
        return slotType;
    }

    public void displaySlotDetails() {
        System.out.println("Parking Slot: " + slotType + " | Occupied: " + (isOccupied ? "Yes" : "No"));
    }
}

class Ticket {
    private int ticketID;
    private String entryTime;
    private String exitTime;
    private Vehicle vehicle;

    public Ticket(int ticketID, String entryTime, Vehicle vehicle) {
        this.ticketID = ticketID;
        this.entryTime = entryTime;
        this.vehicle = vehicle;
    }

    public void setExitTime(String exitTime) {
        this.exitTime = exitTime;
    }

    public void displayTicketDetails() {
        System.out.println("Ticket ID: " + ticketID);
        System.out.println("Entry Time: " + entryTime);
        System.out.println("Exit Time: " + exitTime);
        vehicle.displayDetails();
    }
}

class ParkingManagementSystem {
    private List<ParkingSlot> slots;
    private List<Ticket> tickets;
    private static int totalVehicles = 0;
    private static double totalRevenue = 0;

    public ParkingManagementSystem() {
        slots = new ArrayList<>();
        tickets = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            slots.add(new ParkingSlot("Slot " + (i + 1)));
        }
    }

    public void registerVehicle(Vehicle vehicle, long duration) {
        for (ParkingSlot slot : slots) {
            if (!slot.isOccupied()) {
                slot.occupy();
                System.out.println("Vehicle registered in " + slot.getSlotType());
                if (duration <= 0) {
                    throw new IllegalArgumentException("Duration must be positive.");
                }
                double fee = vehicle.calculateFee(duration);
                totalRevenue += fee;
                Ticket ticket = new Ticket(++totalVehicles, getCurrentTime(), vehicle);
                tickets.add(ticket);
                System.out.println("Parking Fee: $" + fee);
                return;
            }
        }
        System.out.println("No available slots for the vehicle.");
    }

    public void displaySlots() {
        for (ParkingSlot slot : slots) {
            slot.displaySlotDetails();
        }
    }

    public void generateReport() {
        System.out.println("Total Vehicles: " + totalVehicles);
        System.out.println("Total Revenue: $" + totalRevenue);
    }

    public void notifyTimeAlerts() {
        System.out.println("Time alerts: Check if any vehicle is about to exceed parking time.");
    }

    private String getCurrentTime() {
        return "12:00";
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ParkingManagementSystem pms = new ParkingManagementSystem();

        while (true) {
            try {
                System.out.println("1. Register Vehicle");
                System.out.println("2. Display Slots");
                System.out.println("3. Generate Report");
                System.out.println("4. Notify Time Alerts");
                System.out.println("5. Exit");
                System.out.print("Choose an option: ");
                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1:
                        System.out.println("Enter the vehicle number: ");
                        String vehicleNum = scanner.nextLine();
                        System.out.println("Enter the owner name: ");
                        String ownerName = scanner.nextLine();
                        System.out.println("Enter the contact number: ");
                        long contactNum = scanner.nextLong();
                        scanner.nextLine();
                        System.out.println("Enter the vehicle type (4 wheeler/2 wheeler): ");
                        String vehicleType = scanner.nextLine();
                        Vehicle vehicle;

                        if (vehicleType.equalsIgnoreCase("4 wheeler") || vehicleType.equals("4")) {
                            System.out.println("Enter the vehicle type (LTV/HTV): ");
                            String type = scanner.nextLine();
                            vehicle = new FourWheeler(vehicleNum, ownerName, contactNum, type);
                        } else if (vehicleType.equalsIgnoreCase("2 wheeler") || vehicleType.equals("2")) {
                            System.out.println("Does the 2 wheeler have a carrier? (true/false): ");
                            boolean hasCarrier = scanner.nextBoolean();
                            vehicle = new TwoWheeler(vehicleNum, ownerName, contactNum, hasCarrier);
                            scanner.nextLine(); // Clear buffer
                        } else {
                            throw new IllegalArgumentException("Invalid vehicle type.");
                        }

                        System.out.print("Enter the duration of parking in hours: ");
                        long duration = scanner.nextLong();
                        pms.registerVehicle(vehicle, duration);
                        break;
                    case 2:
                        pms.displaySlots();
                        break;
                    case 3:
                        pms.generateReport();
                        break;
                    case 4:
                        pms.notifyTimeAlerts();
                        break;
                    case 5:
                        System.out.println("Exiting...");
                        scanner.close();
                        return;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a valid number.");
                scanner.nextLine(); // Clear the invalid input
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("An unexpected error occurred: " + e.getMessage());
            }
        }
    }
}
