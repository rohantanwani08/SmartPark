package smartpark.ui.gui;

import smartpark.exception.DuplicateVehicleException;
import smartpark.exception.InvalidVehicleException;
import smartpark.exception.LotFullException;
import smartpark.exception.PaymentFailedException;
import smartpark.exception.TicketNotFoundException;
import smartpark.model.Bike;
import smartpark.model.Car;
import smartpark.model.Floor;
import smartpark.model.ParkingSlot;
import smartpark.model.Ticket;
import smartpark.model.Truck;
import smartpark.model.Vehicle;
import smartpark.service.CashPayment;
import smartpark.service.FileManager;
import smartpark.service.OverstayWatcher;
import smartpark.service.ParkingLot;
import smartpark.service.Payment;
import smartpark.service.UPIPayment;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modern Swing Graphical User Interface (GUI) Dashboard for SmartPark.
 * Demonstrates:
 * 1. Strict Separation of Concerns (Presentation Layer decoupled from business logic).
 * 2. Visual slot grid with real-time status color coding (Green=Free, Red=Occupied, Orange=Overstay).
 * 3. Thread-safe GUI updates using SwingUtilities.invokeLater() on the Event Dispatch Thread (EDT).
 * 4. Interactive admission and checkout dialogs with strategy payment collection.
 */
public class MainFrame extends JFrame {
    private final ParkingLot parkingLot;
    private final Map<String, JButton> slotButtons;

    // UI Header Stat Labels
    private JLabel clockLabel;
    private JLabel totalSlotsLabel;
    private JLabel occupiedSlotsLabel;
    private JLabel freeSlotsLabel;
    private JLabel revenueLabel;

    // Tabs & Tables
    private JTabbedPane tabbedPane;
    private DefaultTableModel activeTableModel;
    private DefaultTableModel historyTableModel;
    private JPanel gridContainer;

    // Colors
    private static final Color COLOR_FREE = new Color(46, 125, 50);       // Forest Green
    private static final Color COLOR_OCCUPIED = new Color(198, 40, 40);   // Crimson Red
    private static final Color COLOR_OVERSTAY = new Color(239, 108, 0);   // Warning Orange
    private static final Color COLOR_HEADER = new Color(33, 33, 33);      // Dark Charcoal
    private static final Color COLOR_ACCENT = new Color(25, 118, 210);    // Material Blue

    public MainFrame(ParkingLot parkingLot) {
        this.parkingLot = parkingLot;
        this.slotButtons = new HashMap<>();

        // Configure system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        setTitle("SmartPark Dashboard - " + parkingLot.getLotName());
        setSize(1100, 750);
        setMinimumSize(new Dimension(950, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        initComponents();
        refreshDashboard();
        startClockAndWatcher();
    }

    private void initComponents() {
        // 1. TOP HEADER & METRICS BAR
        add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. CENTER TABBED PANE (Visual Grid + Active Table + Audit History)
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Tab 1: Interactive Slot Grid
        gridContainer = new JPanel();
        gridContainer.setLayout(new BoxLayout(gridContainer, BoxLayout.Y_AXIS));
        gridContainer.setBackground(new Color(245, 245, 245));
        JScrollPane gridScrollPane = new JScrollPane(gridContainer);
        gridScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        tabbedPane.addTab("🅿️ Visual Parking Grid", gridScrollPane);

        // Tab 2: Active Vehicles Table
        tabbedPane.addTab("📋 Active Tickets", createActiveTablePanel());

        // Tab 3: Historical Audit Ledger
        tabbedPane.addTab("📜 Financial Audit Trail", createHistoryTablePanel());

        add(tabbedPane, BorderLayout.CENTER);

        // 3. BOTTOM CONTROL ACTION BAR
        add(createActionBar(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(COLOR_HEADER);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        // Title and live clock
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);

        JLabel titleLabel = new JLabel("🚗 SmartPark Management Facility");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);

        clockLabel = new JLabel();
        clockLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        clockLabel.setForeground(new Color(200, 200, 200));

        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(clockLabel, BorderLayout.SOUTH);
        headerPanel.add(titlePanel, BorderLayout.WEST);

        // Key Metric KPI Badges
        JPanel metricsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        metricsPanel.setOpaque(false);

        totalSlotsLabel = createMetricBadge("TOTAL SLOTS", "0", new Color(66, 66, 66));
        occupiedSlotsLabel = createMetricBadge("OCCUPIED", "0", COLOR_OCCUPIED);
        freeSlotsLabel = createMetricBadge("AVAILABLE", "0", COLOR_FREE);
        revenueLabel = createMetricBadge("TOTAL REVENUE", "Rs. 0.00", COLOR_ACCENT);

        metricsPanel.add(totalSlotsLabel);
        metricsPanel.add(occupiedSlotsLabel);
        metricsPanel.add(freeSlotsLabel);
        metricsPanel.add(revenueLabel);

        headerPanel.add(metricsPanel, BorderLayout.EAST);
        return headerPanel;
    }

    private JLabel createMetricBadge(String title, String value, Color bgColor) {
        JLabel badge = new JLabel(String.format("<html><center><small>%s</small><br><b>%s</b></center></html>", title, value));
        badge.setOpaque(true);
        badge.setBackground(bgColor);
        badge.setForeground(Color.WHITE);
        badge.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        badge.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        return badge;
    }

    private JPanel createActiveTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columns = { "Ticket #", "License Plate", "Type", "Assigned Slot", "Owner Name", "Entry Time" };
        activeTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        JTable table = new JTable(activeTableModel);
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createHistoryTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columns = { "Ticket #", "License Plate", "Type", "Fee Paid", "Channel", "Departure Time" };
        historyTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        JTable table = new JTable(historyTableModel);
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 12));
        bar.setBackground(new Color(238, 238, 238));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200)));

        JButton parkBtn = createStyledButton("➕ Admit Vehicle", COLOR_FREE);
        parkBtn.addActionListener(e -> showAdmissionDialog());

        JButton exitBtn = createStyledButton("💳 Process Exit & Payment", COLOR_ACCENT);
        exitBtn.addActionListener(e -> showExitDialog());

        JButton refreshBtn = createStyledButton("🔄 Refresh View", new Color(66, 66, 66));
        refreshBtn.addActionListener(e -> refreshDashboard());

        bar.add(parkBtn);
        bar.add(exitBtn);
        bar.add(refreshBtn);
        return bar;
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        return btn;
    }

    // =========================================================================
    // Real-Time Dashboard Rendering & Synchronization
    // =========================================================================

    public void refreshDashboard() {
        // Enforce execution on the Event Dispatch Thread (EDT)
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refreshDashboard);
            return;
        }

        // 1. Update KPI Badges
        int total = parkingLot.getTotalCapacity();
        int occupied = parkingLot.getOccupiedSlotsCount();
        int free = parkingLot.getFreeSlotsCount();
        double rev = parkingLot.getTotalRevenue();

        totalSlotsLabel.setText(String.format("<html><center><small>TOTAL SLOTS</small><br><b>%d</b></center></html>", total));
        occupiedSlotsLabel.setText(String.format("<html><center><small>OCCUPIED</small><br><b>%d</b></center></html>", occupied));
        freeSlotsLabel.setText(String.format("<html><center><small>AVAILABLE</small><br><b>%d</b></center></html>", free));
        revenueLabel.setText(String.format("<html><center><small>TOTAL REVENUE</small><br><b>Rs. %.2f</b></center></html>", rev));

        // 2. Rebuild Visual Grid
        gridContainer.removeAll();
        slotButtons.clear();

        for (Floor floor : parkingLot.getFloors()) {
            JPanel floorPanel = new JPanel(new GridLayout(0, 4, 10, 10));
            floorPanel.setBackground(Color.WHITE);
            floorPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(
                            BorderFactory.createLineBorder(new Color(189, 189, 189), 1),
                            String.format(" Floor %d  (Free: %d / %d) ",
                                    floor.getFloorNumber(),
                                    (floor.getTotalSlots() - floor.getOccupiedCount()),
                                    floor.getTotalSlots()),
                            0, 0, new Font("Segoe UI", Font.BOLD, 14), COLOR_HEADER),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));

            for (ParkingSlot slot : floor.getSlots()) {
                JButton btn = new JButton();
                btn.setPreferredSize(new Dimension(140, 75));
                btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
                btn.setFocusPainted(false);
                btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

                if (slot.isAvailable()) {
                    btn.setBackground(COLOR_FREE);
                    btn.setForeground(Color.WHITE);
                    btn.setText(String.format("<html><center><b>%s</b><br><small>[%s]</small><br>AVAILABLE</center></html>",
                            slot.getSlotId(), slot.getSupportedType()));
                } else {
                    Vehicle v = slot.getCurrentVehicle();
                    btn.setBackground(COLOR_OCCUPIED);
                    btn.setForeground(Color.WHITE);
                    btn.setText(String.format("<html><center><b>%s</b><br><small>%s</small><br>%s</center></html>",
                            slot.getSlotId(), v.getVehicleType(), v.getPlateNumber()));
                }

                btn.addActionListener(e -> showSlotDetails(slot));
                slotButtons.put(slot.getSlotId(), btn);
                floorPanel.add(btn);
            }

            gridContainer.add(floorPanel);
            gridContainer.add(Box.createVerticalStrut(12));
        }

        gridContainer.revalidate();
        gridContainer.repaint();

        // 3. Update Active Table
        activeTableModel.setRowCount(0);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (Ticket t : parkingLot.getActiveTickets()) {
            activeTableModel.addRow(new Object[]{
                    "#" + t.getTicketId(),
                    t.getVehicle().getPlateNumber(),
                    t.getVehicle().getVehicleType(),
                    t.getParkingSlot().getSlotId(),
                    t.getVehicle().getOwnerName(),
                    sdf.format(new Date(t.getEntryTimeMillis()))
            });
        }

        // 4. Update Audit History Table
        historyTableModel.setRowCount(0);
        if (parkingLot.getFileManager() != null) {
            try {
                List<FileManager.AuditLogEntry> logs = parkingLot.getFileManager().readBinaryLogs();
                for (FileManager.AuditLogEntry log : logs) {
                    historyTableModel.addRow(new Object[]{
                            "#" + log.getTicketId(),
                            log.getPlateNumber(),
                            log.getVehicleType(),
                            String.format("Rs. %.2f", log.getFeePaid()),
                            log.getPaymentMode(),
                            sdf.format(new Date(log.getExitTimeMillis()))
                    });
                }
            } catch (Exception ignored) {}
        }
    }

    private void showSlotDetails(ParkingSlot slot) {
        if (slot.isAvailable()) {
            JOptionPane.showMessageDialog(this,
                    String.format("Slot ID: %s%nDesignated For: %s%nStatus: AVAILABLE",
                            slot.getSlotId(), slot.getSupportedType()),
                    "Slot Information", JOptionPane.INFORMATION_MESSAGE);
        } else {
            Vehicle v = slot.getCurrentVehicle();
            Ticket ticket = null;
            for (Ticket t : parkingLot.getActiveTickets()) {
                if (t.getParkingSlot().getSlotId().equalsIgnoreCase(slot.getSlotId())) {
                    ticket = t;
                    break;
                }
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String inTime = ticket != null ? sdf.format(new Date(ticket.getEntryTimeMillis())) : "N/A";
            int ticketId = ticket != null ? ticket.getTicketId() : 0;

            String details = String.format("Slot ID: %s%nStatus: OCCUPIED%nTicket #: %d%nPlate Number: %s%nOwner: %s%nVehicle Type: %s%nEntry Time: %s",
                    slot.getSlotId(), ticketId, v.getPlateNumber(), v.getOwnerName(), v.getVehicleType(), inTime);

            JOptionPane.showMessageDialog(this, details, "Slot Information", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // =========================================================================
    // Dialogs: Admission & Exit Modals
    // =========================================================================

    private void showAdmissionDialog() {
        JDialog dialog = new JDialog(this, "Admit Incoming Vehicle", true);
        dialog.setSize(420, 320);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridLayout(6, 2, 10, 10));
        ((JPanel) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JComboBox<String> typeCombo = new JComboBox<>(new String[]{ "Car", "Bike", "Truck" });
        JTextField plateField = new JTextField();
        JTextField ownerField = new JTextField();
        JComboBox<String> fuelCombo = new JComboBox<>(new String[]{ "Petrol", "Diesel", "Electric", "CNG" });
        JTextField tonnageField = new JTextField("0.0");
        tonnageField.setEnabled(false);

        typeCombo.addActionListener(e -> {
            boolean isTruck = "Truck".equals(typeCombo.getSelectedItem());
            tonnageField.setEnabled(isTruck);
            if (isTruck && tonnageField.getText().equals("0.0")) {
                tonnageField.setText("12.0");
            }
        });

        dialog.add(new JLabel("Vehicle Category:"));
        dialog.add(typeCombo);
        dialog.add(new JLabel("License Plate:"));
        dialog.add(plateField);
        dialog.add(new JLabel("Owner Name:"));
        dialog.add(ownerField);
        dialog.add(new JLabel("Fuel Type:"));
        dialog.add(fuelCombo);
        dialog.add(new JLabel("Truck Tonnage (if Truck):"));
        dialog.add(tonnageField);

        JButton submitBtn = createStyledButton("Confirm Entry", COLOR_FREE);
        submitBtn.addActionListener(e -> {
            String plate = plateField.getText().trim();
            String owner = ownerField.getText().trim();
            String type = (String) typeCombo.getSelectedItem();
            String fuel = (String) fuelCombo.getSelectedItem();

            try {
                Vehicle vehicle;
                if ("Car".equals(type)) {
                    vehicle = new Car(plate, owner, fuel);
                } else if ("Bike".equals(type)) {
                    vehicle = new Bike(plate, owner, fuel);
                } else {
                    double tonnage = Double.parseDouble(tonnageField.getText().trim());
                    vehicle = new Truck(plate, owner, fuel, tonnage);
                }

                Ticket ticket = parkingLot.parkVehicle(vehicle);
                dialog.dispose();
                refreshDashboard();

                JOptionPane.showMessageDialog(this,
                        String.format(">>> TICKET ISSUED <<<%nTicket ID: #%d%nVehicle: %s%nAssigned Slot: %s%nHourly Rate: Rs. %.2f/hr",
                                ticket.getTicketId(), vehicle.getPlateNumber(), ticket.getParkingSlot().getSlotId(),
                                vehicle.calculateFee(1)),
                        "Admission Confirmed", JOptionPane.INFORMATION_MESSAGE);

            } catch (DuplicateVehicleException | LotFullException | InvalidVehicleException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Admission Denied", JOptionPane.WARNING_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter a valid numeric load capacity.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.add(submitBtn);
        dialog.add(cancelBtn);
        dialog.setVisible(true);
    }

    private void showExitDialog() {
        List<Ticket> activeTickets = parkingLot.getActiveTickets();
        if (activeTickets.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No vehicles are currently parked in the lot.", "Lot Empty", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(this, "Process Vehicle Exit & Payment", true);
        dialog.setSize(440, 320);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridLayout(6, 2, 10, 10));
        ((JPanel) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        Integer[] ticketIds = activeTickets.stream().map(Ticket::getTicketId).toArray(Integer[]::new);
        JComboBox<Integer> ticketCombo = new JComboBox<>(ticketIds);

        JLabel feePreviewLabel = new JLabel("Calculating...");
        JComboBox<String> paymentChannelCombo = new JComboBox<>(new String[]{ "Cash", "UPI" });
        JLabel promptLabel = new JLabel("Cash Tendered (Rs):");
        JTextField paymentInputField = new JTextField();

        Runnable updateFeePreview = () -> {
            Integer selectedId = (Integer) ticketCombo.getSelectedItem();
            if (selectedId != null) {
                Ticket t = parkingLot.findActiveTicketById(selectedId);
                if (t != null) {
                    long now = System.currentTimeMillis();
                    int hours = t.calculateHoursParked(now);
                    double fee = t.getVehicle().calculateFee(hours);
                    feePreviewLabel.setText(String.format("Rs. %.2f (%d hr)", fee, hours));
                    if ("Cash".equals(paymentChannelCombo.getSelectedItem())) {
                        paymentInputField.setText(String.format("%.2f", fee));
                    }
                }
            }
        };

        ticketCombo.addActionListener(e -> updateFeePreview.run());
        paymentChannelCombo.addActionListener(e -> {
            boolean isCash = "Cash".equals(paymentChannelCombo.getSelectedItem());
            promptLabel.setText(isCash ? "Cash Tendered (Rs):" : "Customer UPI VPA:");
            paymentInputField.setText(isCash ? "100.00" : "driver@okhdfcbank");
        });

        updateFeePreview.run();

        dialog.add(new JLabel("Select Ticket #:"));
        dialog.add(ticketCombo);
        dialog.add(new JLabel("Calculated Fee:"));
        dialog.add(feePreviewLabel);
        dialog.add(new JLabel("Payment Method:"));
        dialog.add(paymentChannelCombo);
        dialog.add(promptLabel);
        dialog.add(paymentInputField);

        JButton checkoutBtn = createStyledButton("Confirm Payment & Exit", COLOR_ACCENT);
        checkoutBtn.addActionListener(e -> {
            Integer ticketId = (Integer) ticketCombo.getSelectedItem();
            if (ticketId == null) return;

            String channel = (String) paymentChannelCombo.getSelectedItem();
            String input = paymentInputField.getText().trim();

            try {
                Payment payment;
                if ("Cash".equals(channel)) {
                    double tendered = Double.parseDouble(input);
                    payment = new CashPayment(tendered);
                } else {
                    payment = new UPIPayment(input);
                }

                Ticket closedTicket = parkingLot.exitVehicle(ticketId, payment);
                dialog.dispose();
                refreshDashboard();

                String receipt = String.format(">>> PAYMENT RECEIPT <<<%nTicket ID: #%d%nVehicle: %s%nPayment Channel: %s%nSlot Freed: %s",
                        closedTicket.getTicketId(), closedTicket.getVehicle().getPlateNumber(), payment.getMode(),
                        closedTicket.getParkingSlot().getSlotId());

                if (payment instanceof CashPayment) {
                    receipt += String.format("%nChange Returned: Rs. %.2f", ((CashPayment) payment).getChangeGiven());
                } else if (payment instanceof UPIPayment) {
                    receipt += String.format("%nTransaction Ref: %s", ((UPIPayment) payment).getTransactionRef());
                }

                JOptionPane.showMessageDialog(this, receipt, "Exit Succeeded", JOptionPane.INFORMATION_MESSAGE);

            } catch (PaymentFailedException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Payment Declined", JOptionPane.WARNING_MESSAGE);
            } catch (TicketNotFoundException | InvalidVehicleException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Operation Error", JOptionPane.ERROR_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter a valid numeric cash amount.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.add(checkoutBtn);
        dialog.add(cancelBtn);
        dialog.setVisible(true);
    }

    private void startClockAndWatcher() {
        // Digital clock timer updating every second on EDT
        Timer clockTimer = new Timer(1000, e -> {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMM yyyy - HH:mm:ss");
            clockLabel.setText("🕒 " + sdf.format(new Date()));
        });
        clockTimer.start();
    }
}
