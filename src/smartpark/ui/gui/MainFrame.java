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
import smartpark.service.AnalyticsService;
import smartpark.service.CashPayment;
import smartpark.service.FileManager;
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
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modern High-Fidelity Desktop Dashboard for SmartPark.
 * Features:
 * 1. Custom-painted high-contrast Slot Cards (eliminates Windows L&F color wash bugs).
 * 2. Executive Dark Slate Header with real-time KPI metric badges and live clock.
 * 3. Modern JTabbedPane with custom zebra-striped tables and visual analytics dashboard.
 * 4. Thread-safe GUI mutations dispatched strictly via SwingUtilities.invokeLater() on EDT.
 */
public class MainFrame extends JFrame {
    private final ParkingLot parkingLot;
    private final Map<String, SlotCard> slotCards;

    // UI Header Stat Badges
    private JLabel clockLabel;
    private KPICard totalSlotsCard;
    private KPICard occupiedSlotsCard;
    private KPICard availableSlotsCard;
    private KPICard revenueCard;

    // Tabs & Containers
    private JTabbedPane tabbedPane;
    private JPanel gridContainer;
    private DefaultTableModel activeTableModel;
    private DefaultTableModel historyTableModel;

    // Visual Analytics Components (Zero raw text dump!)
    private JProgressBar occupancyProgressBar;
    private JLabel occupancyPctLabel;
    private JLabel carMixLabel;
    private JLabel bikeMixLabel;
    private JLabel truckMixLabel;
    private JLabel avgTicketLabel;

    // Advanced Visual Analytics Components
    private JPanel floorBarsContainer;
    private JLabel upiRevLabel;
    private JLabel cashRevLabel;
    private JProgressBar upiProgressBar;
    private JProgressBar cashProgressBar;
    private JLabel totalSessionsLabel;
    private JLabel watcherStatusLabel;

    // Executive Color Palette
    private static final Color THEME_HEADER_BG = new Color(15, 23, 42);      // Slate 900
    private static final Color THEME_PAGE_BG = new Color(241, 245, 249);     // Slate 100
    private static final Color THEME_CARD_BG = Color.WHITE;
    private static final Color THEME_PRIMARY = new Color(37, 99, 235);       // Royal Blue 600
    private static final Color THEME_SUCCESS = new Color(22, 163, 74);       // Emerald Green 600
    private static final Color THEME_DANGER = new Color(220, 38, 38);        // Crimson Red 600
    private static final Color THEME_TEXT_DARK = new Color(15, 23, 42);
    private static final Color THEME_TEXT_MUTED = new Color(100, 116, 139);

    public MainFrame(ParkingLot parkingLot) {
        this.parkingLot = parkingLot;
        this.slotCards = new HashMap<>();

        setTitle("SmartPark Enterprise - " + parkingLot.getLotName());
        setSize(1220, 840);
        setMinimumSize(new Dimension(1040, 700));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        initComponents();
        refreshDashboard();
        startClock();
    }

    private void initComponents() {
        // 1. TOP EXECUTIVE HEADER
        add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. CENTER TABBED CONTENT AREA
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.setBackground(Color.WHITE);

        // Tab 1: Visual Parking Slot Grid
        gridContainer = new JPanel();
        gridContainer.setLayout(new BoxLayout(gridContainer, BoxLayout.Y_AXIS));
        gridContainer.setBackground(THEME_PAGE_BG);
        gridContainer.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        JScrollPane gridScrollPane = new JScrollPane(gridContainer);
        gridScrollPane.setBorder(null);
        gridScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        tabbedPane.addTab("Visual Parking Grid", gridScrollPane);

        // Tab 2: Active Vehicles
        tabbedPane.addTab("Active Tickets Registry", createActiveTablePanel());

        // Tab 3: Historical Audit Trail
        tabbedPane.addTab("Financial Audit Ledger", createHistoryTablePanel());

        // Tab 4: Executive Analytics & Insights (Completely Visual!)
        tabbedPane.addTab("Executive Analytics", createAnalyticsPanel());

        add(tabbedPane, BorderLayout.CENTER);

        // 3. BOTTOM HIGH-CONTRAST ACTION BAR
        add(createActionBar(), BorderLayout.SOUTH);
    }

    // =========================================================================
    // Header & KPI Stat Cards
    // =========================================================================

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(THEME_HEADER_BG);
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        // Brand Title & Live Clock
        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);

        JLabel brandLabel = new JLabel("SMARTPARK  |  FACILITY OPERATIONS DASHBOARD");
        brandLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        brandLabel.setForeground(Color.WHITE);

        clockLabel = new JLabel("Synchronizing clock...");
        clockLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        clockLabel.setForeground(new Color(148, 163, 184)); // Slate 400

        titleBox.add(brandLabel);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(clockLabel);
        header.add(titleBox, BorderLayout.WEST);

        // Top KPI Metric Cards
        JPanel kpiBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        kpiBox.setOpaque(false);

        totalSlotsCard = new KPICard("TOTAL SLOTS", "0", new Color(51, 65, 85), Color.WHITE);
        occupiedSlotsCard = new KPICard("OCCUPIED", "0", new Color(153, 27, 27), new Color(254, 202, 202));
        availableSlotsCard = new KPICard("AVAILABLE", "0", new Color(20, 83, 45), new Color(187, 247, 208));
        revenueCard = new KPICard("TOTAL REVENUE", "Rs. 0.00", new Color(30, 58, 138), new Color(191, 219, 254));

        kpiBox.add(totalSlotsCard);
        kpiBox.add(occupiedSlotsCard);
        kpiBox.add(availableSlotsCard);
        kpiBox.add(revenueCard);
        header.add(kpiBox, BorderLayout.EAST);

        return header;
    }

    // =========================================================================
    // Tables: Active & Audit History (Zebra Striped, Clean High Contrast)
    // =========================================================================

    private JPanel createActiveTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(THEME_PAGE_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        String[] cols = { "Ticket ID", "License Plate", "Category", "Assigned Slot", "Owner Name", "Entry Timestamp" };
        activeTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        JTable table = createStyledTable(activeTableModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createHistoryTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(THEME_PAGE_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        String[] cols = { "Ticket ID", "License Plate", "Category", "Fee Settled", "Channel", "Departure Timestamp" };
        historyTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        JTable table = createStyledTable(historyTableModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(32);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(THEME_HEADER_BG);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));
        table.setGridColor(new Color(226, 232, 240));

        // Custom Zebra Striping Renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, isSel, hasFocus, row, col);
                if (!isSel) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(THEME_TEXT_DARK);
                } else {
                    c.setBackground(new Color(219, 234, 254));
                    c.setForeground(THEME_PRIMARY);
                }
                if (col == 0 || col == 2 || col == 3 || col == 4) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }
                return c;
            }
        });
        return table;
    }

    // =========================================================================
    // Tab 4: Modern Visual Executive Analytics Panel (Completely Visual!)
    // =========================================================================

    private JPanel createAnalyticsPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(16, 16));
        mainPanel.setBackground(THEME_PAGE_BG);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        // 1. TOP STATS STRIP (3 Primary Metric Cards)
        JPanel topStrip = new JPanel(new GridLayout(1, 3, 16, 0));
        topStrip.setOpaque(false);

        // Card A: Facility Utilization Meter
        JPanel utilCard = createAnalyticsCard("OVERALL FACILITY UTILIZATION");
        occupancyProgressBar = new JProgressBar(0, 100);
        occupancyProgressBar.setStringPainted(true);
        occupancyProgressBar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        occupancyProgressBar.setForeground(THEME_PRIMARY);
        occupancyProgressBar.setPreferredSize(new Dimension(200, 26));

        occupancyPctLabel = new JLabel("0.0% Occupancy");
        occupancyPctLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        occupancyPctLabel.setForeground(THEME_PRIMARY);

        utilCard.add(occupancyPctLabel, BorderLayout.CENTER);
        utilCard.add(occupancyProgressBar, BorderLayout.SOUTH);
        topStrip.add(utilCard);

        // Card B: Vehicle Mix Distribution
        JPanel mixCard = createAnalyticsCard("ACTIVE VEHICLE CATEGORY RATIO");
        JPanel mixLabels = new JPanel(new GridLayout(3, 1, 4, 4));
        mixLabels.setOpaque(false);
        carMixLabel = new JLabel("● Cars  : 0 (0%)");
        bikeMixLabel = new JLabel("● Bikes : 0 (0%)");
        truckMixLabel = new JLabel("● Trucks: 0 (0%)");
        carMixLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        bikeMixLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        truckMixLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        carMixLabel.setForeground(THEME_PRIMARY);
        bikeMixLabel.setForeground(THEME_SUCCESS);
        truckMixLabel.setForeground(new Color(217, 119, 6)); // Amber
        mixLabels.add(carMixLabel);
        mixLabels.add(bikeMixLabel);
        mixLabels.add(truckMixLabel);
        mixCard.add(mixLabels, BorderLayout.CENTER);
        topStrip.add(mixCard);

        // Card C: Revenue Average KPI
        JPanel revCard = createAnalyticsCard("HISTORICAL TICKET AVERAGE");
        avgTicketLabel = new JLabel("Rs. 0.00 / session");
        avgTicketLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        avgTicketLabel.setForeground(THEME_SUCCESS);
        totalSessionsLabel = new JLabel("0 historical sessions settled");
        totalSessionsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        totalSessionsLabel.setForeground(THEME_TEXT_MUTED);

        revCard.add(avgTicketLabel, BorderLayout.CENTER);
        revCard.add(totalSessionsLabel, BorderLayout.SOUTH);
        topStrip.add(revCard);

        mainPanel.add(topStrip, BorderLayout.NORTH);

        // 2. CENTER SECTION: 2-COLUMN MODERN VISUAL DASHBOARD (Zero plain text dump!)
        JPanel centerGrid = new JPanel(new GridLayout(1, 2, 16, 0));
        centerGrid.setOpaque(false);

        // Column 1: Floor-by-Floor Live Capacity Bars
        JPanel floorCard = createAnalyticsCard("FLOOR-BY-FLOOR OCCUPANCY & CAPACITY");
        floorBarsContainer = new JPanel();
        floorBarsContainer.setLayout(new BoxLayout(floorBarsContainer, BoxLayout.Y_AXIS));
        floorBarsContainer.setOpaque(false);
        JScrollPane floorScroll = new JScrollPane(floorBarsContainer);
        floorScroll.setBorder(null);
        floorScroll.setOpaque(false);
        floorScroll.getViewport().setOpaque(false);
        floorCard.add(floorScroll, BorderLayout.CENTER);
        centerGrid.add(floorCard);

        // Column 2: Financial Performance & Channel Split
        JPanel financeCard = createAnalyticsCard("PAYMENT CHANNEL PERFORMANCE & AUDIT");
        JPanel financeContent = new JPanel();
        financeContent.setLayout(new BoxLayout(financeContent, BoxLayout.Y_AXIS));
        financeContent.setOpaque(false);

        // UPI Row
        upiRevLabel = new JLabel("UPI Collections: Rs. 0.00 (0%)");
        upiRevLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        upiRevLabel.setForeground(THEME_PRIMARY);
        upiProgressBar = new JProgressBar(0, 100);
        upiProgressBar.setStringPainted(true);
        upiProgressBar.setForeground(THEME_PRIMARY);
        upiProgressBar.setPreferredSize(new Dimension(300, 22));

        // Cash Row
        cashRevLabel = new JLabel("Cash Collections: Rs. 0.00 (0%)");
        cashRevLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cashRevLabel.setForeground(THEME_SUCCESS);
        cashProgressBar = new JProgressBar(0, 100);
        cashProgressBar.setStringPainted(true);
        cashProgressBar.setForeground(THEME_SUCCESS);
        cashProgressBar.setPreferredSize(new Dimension(300, 22));

        // System Health Callout
        JPanel healthCallout = new JPanel(new BorderLayout());
        healthCallout.setBackground(new Color(240, 253, 244));
        healthCallout.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(187, 247, 208), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        watcherStatusLabel = new JLabel("OverstayWatcher Daemon: ACTIVE (Scanning background active tickets)");
        watcherStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        watcherStatusLabel.setForeground(new Color(22, 101, 52));
        healthCallout.add(watcherStatusLabel, BorderLayout.CENTER);

        financeContent.add(upiRevLabel);
        financeContent.add(Box.createVerticalStrut(6));
        financeContent.add(upiProgressBar);
        financeContent.add(Box.createVerticalStrut(14));
        financeContent.add(cashRevLabel);
        financeContent.add(Box.createVerticalStrut(6));
        financeContent.add(cashProgressBar);
        financeContent.add(Box.createVerticalStrut(20));
        financeContent.add(healthCallout);

        financeCard.add(financeContent, BorderLayout.CENTER);
        centerGrid.add(financeCard);

        mainPanel.add(centerGrid, BorderLayout.CENTER);

        // 3. BOTTOM REFRESH BAR
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomBar.setOpaque(false);
        ModernButton refreshAnalyticsBtn = new ModernButton("Refresh Intelligence", THEME_PRIMARY, new Color(59, 130, 246));
        refreshAnalyticsBtn.addActionListener(e -> refreshDashboard());
        bottomBar.add(refreshAnalyticsBtn);
        mainPanel.add(bottomBar, BorderLayout.SOUTH);

        return mainPanel;
    }

    private JPanel createAnalyticsCard(String title) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLbl.setForeground(THEME_TEXT_MUTED);
        card.add(titleLbl, BorderLayout.NORTH);
        return card;
    }

    // =========================================================================
    // Bottom Action Bar (High-Contrast, Custom-Painted Buttons)
    // =========================================================================

    private JPanel createActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 12));
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        ModernButton admitBtn = new ModernButton("+ Admit Vehicle", THEME_SUCCESS, new Color(34, 197, 94));
        admitBtn.addActionListener(e -> showAdmissionDialog());

        ModernButton exitBtn = new ModernButton("Exit & Settle Payment", THEME_PRIMARY, new Color(59, 130, 246));
        exitBtn.addActionListener(e -> showExitDialog());

        ModernButton refreshBtn = new ModernButton("Refresh Dashboard", new Color(71, 85, 105), new Color(100, 116, 139));
        refreshBtn.addActionListener(e -> refreshDashboard());

        bar.add(admitBtn);
        bar.add(exitBtn);
        bar.add(refreshBtn);
        return bar;
    }

    // =========================================================================
    // Dynamic Dashboard Refresh (Enforces Thread-Safety on EDT)
    // =========================================================================

    public void refreshDashboard() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refreshDashboard);
            return;
        }

        // 1. Update KPI Header Badges
        int total = parkingLot.getTotalCapacity();
        int occupied = parkingLot.getOccupiedSlotsCount();
        int free = parkingLot.getFreeSlotsCount();
        double rev = parkingLot.getTotalRevenue();

        totalSlotsCard.setValue(String.valueOf(total));
        occupiedSlotsCard.setValue(String.valueOf(occupied));
        availableSlotsCard.setValue(String.valueOf(free));
        revenueCard.setValue(String.format("Rs. %.2f", rev));

        // 2. Re-render Visual Grid using Custom SlotCards
        gridContainer.removeAll();
        slotCards.clear();

        for (Floor floor : parkingLot.getFloors()) {
            JPanel floorPanel = new JPanel(new GridLayout(0, 4, 12, 12));
            floorPanel.setBackground(Color.WHITE);
            floorPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(
                            BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                            String.format(" Floor %d  (Available: %d / %d) ",
                                    floor.getFloorNumber(),
                                    (floor.getTotalSlots() - floor.getOccupiedCount()),
                                    floor.getTotalSlots()),
                            0, 0, new Font("Segoe UI", Font.BOLD, 13), THEME_TEXT_DARK),
                    BorderFactory.createEmptyBorder(12, 12, 12, 12)));

            for (ParkingSlot slot : floor.getSlots()) {
                SlotCard card = new SlotCard(slot);
                card.addActionListener(e -> showSlotDetails(slot));
                slotCards.put(slot.getSlotId(), card);
                floorPanel.add(card);
            }

            gridContainer.add(floorPanel);
            gridContainer.add(Box.createVerticalStrut(14));
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

        // 5. Update Visual Analytics Panel
        AnalyticsService analytics = new AnalyticsService(parkingLot);
        double rate = analytics.getOccupancyRate();
        occupancyProgressBar.setValue((int) rate);
        occupancyProgressBar.setString(String.format("%.1f%%", rate));
        occupancyPctLabel.setText(String.format("%.1f%% Occupied", rate));

        Map<String, Integer> mix = analytics.getActiveVehicleMix();
        int activeCount = parkingLot.getActiveTickets().size();
        double cPct = activeCount > 0 ? (mix.get("Car") * 100.0) / activeCount : 0.0;
        double bPct = activeCount > 0 ? (mix.get("Bike") * 100.0) / activeCount : 0.0;
        double tPct = activeCount > 0 ? (mix.get("Truck") * 100.0) / activeCount : 0.0;

        carMixLabel.setText(String.format("● Cars  : %d (%.0f%%)", mix.get("Car"), cPct));
        bikeMixLabel.setText(String.format("● Bikes : %d (%.0f%%)", mix.get("Bike"), bPct));
        truckMixLabel.setText(String.format("● Trucks: %d (%.0f%%)", mix.get("Truck"), tPct));

        avgTicketLabel.setText(String.format("Rs. %.2f", analytics.getAverageTransactionValue()));
        totalSessionsLabel.setText(analytics.getTotalHistoricalSessions() + " departures settled to persistent audit ledger");

        // 6. Update Visual Floor Progress Bars
        if (floorBarsContainer != null) {
            floorBarsContainer.removeAll();
            Map<Integer, Double> floorRates = analytics.getFloorOccupancyRates();
            for (Floor f : parkingLot.getFloors()) {
                int fNum = f.getFloorNumber();
                double fRate = floorRates.getOrDefault(fNum, 0.0);

                JPanel fRow = new JPanel(new BorderLayout(5, 4));
                fRow.setOpaque(false);
                fRow.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));

                JLabel fLbl = new JLabel(String.format("Floor %d  (%d / %d slots occupied)",
                        fNum, f.getOccupiedCount(), f.getTotalSlots()));
                fLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                fLbl.setForeground(THEME_TEXT_DARK);

                JProgressBar fBar = new JProgressBar(0, 100);
                fBar.setValue((int) fRate);
                fBar.setStringPainted(true);
                fBar.setString(String.format("%.1f%% Occupied", fRate));
                fBar.setPreferredSize(new Dimension(240, 20));
                fBar.setFont(new Font("Segoe UI", Font.BOLD, 11));

                if (fRate >= 80.0) {
                    fBar.setForeground(THEME_DANGER);
                } else if (fRate >= 50.0) {
                    fBar.setForeground(THEME_PRIMARY);
                } else {
                    fBar.setForeground(THEME_SUCCESS);
                }

                fRow.add(fLbl, BorderLayout.NORTH);
                fRow.add(fBar, BorderLayout.CENTER);
                floorBarsContainer.add(fRow);
            }
            floorBarsContainer.revalidate();
            floorBarsContainer.repaint();
        }

        // 7. Update Financial Channel Split
        Map<String, Double> channelRev = analytics.getPaymentChannelRevenue();
        double upiAmt = channelRev.getOrDefault("UPI", 0.0);
        double cashAmt = channelRev.getOrDefault("Cash", 0.0);
        double totalRevAll = upiAmt + cashAmt;

        double upiPct = totalRevAll > 0 ? (upiAmt * 100.0) / totalRevAll : 0.0;
        double cashPct = totalRevAll > 0 ? (cashAmt * 100.0) / totalRevAll : 0.0;

        if (upiRevLabel != null) {
            upiRevLabel.setText(String.format("UPI Collections: Rs. %.2f (%.1f%%)", upiAmt, upiPct));
            upiProgressBar.setValue((int) upiPct);
            upiProgressBar.setString(String.format("%.1f%% of revenue", upiPct));

            cashRevLabel.setText(String.format("Cash Collections: Rs. %.2f (%.1f%%)", cashAmt, cashPct));
            cashProgressBar.setValue((int) cashPct);
            cashProgressBar.setString(String.format("%.1f%% of revenue", cashPct));
        }
    }

    private void showSlotDetails(ParkingSlot slot) {
        if (slot.isAvailable()) {
            JOptionPane.showMessageDialog(this,
                    String.format("Slot ID: %s%nDesignated For: %s%nStatus: AVAILABLE (Empty)",
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

            String details = String.format(
                    "SLOT DETAILS%n------------------------------------%nSlot Identifier: %s%nStatus         : OCCUPIED%nTicket ID       : #%d%nLicense Plate  : %s%nOwner Name     : %s%nVehicle Type   : %s%nEntry Timestamp: %s",
                    slot.getSlotId(), ticketId, v.getPlateNumber(), v.getOwnerName(), v.getVehicleType(), inTime);

            JOptionPane.showMessageDialog(this, details, "Slot Information", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // =========================================================================
    // Dialogs: Admission & Exit (High Contrast & Clear Inputs)
    // =========================================================================

    private void showAdmissionDialog() {
        JDialog dialog = new JDialog(this, "Admit Vehicle to SmartPark", true);
        dialog.setSize(440, 340);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridLayout(6, 2, 10, 10));
        ((JPanel) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        dialog.getContentPane().setBackground(Color.WHITE);

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
        dialog.add(new JLabel("License Plate Number:"));
        dialog.add(plateField);
        dialog.add(new JLabel("Owner Full Name:"));
        dialog.add(ownerField);
        dialog.add(new JLabel("Fuel Type:"));
        dialog.add(fuelCombo);
        dialog.add(new JLabel("Cargo Weight (Tonnes):"));
        dialog.add(tonnageField);

        ModernButton submitBtn = new ModernButton("Confirm Entry", THEME_SUCCESS, new Color(34, 197, 94));
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
                        String.format("ADMISSION SUCCESSFUL!%n----------------------------%nTicket ID     : #%d%nVehicle Plate : %s%nAssigned Slot : %s%nHourly Rate   : Rs. %.2f/hr",
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
            JOptionPane.showMessageDialog(this, "No vehicles are currently parked in the lot.", "Facility Empty", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(this, "Process Vehicle Exit & Payment", true);
        dialog.setSize(460, 340);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridLayout(6, 2, 10, 10));
        ((JPanel) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        dialog.getContentPane().setBackground(Color.WHITE);

        JComboBox<String> vehicleCombo = new JComboBox<>();
        for (Ticket t : activeTickets) {
            vehicleCombo.addItem(t.getVehicle().getPlateNumber() + "  (#" + t.getTicketId() + ")");
        }
        vehicleCombo.setEditable(true);

        JLabel feePreviewLabel = new JLabel("Calculating...");
        feePreviewLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        feePreviewLabel.setForeground(THEME_PRIMARY);

        JComboBox<String> paymentChannelCombo = new JComboBox<>(new String[]{ "Cash", "UPI" });
        JLabel promptLabel = new JLabel("Cash Tendered (Rs):");
        JTextField paymentInputField = new JTextField();

        java.util.function.Supplier<Ticket> getSelectedTicket = () -> {
            Object item = vehicleCombo.getSelectedItem();
            if (item == null) return null;
            String text = item.toString().trim();
            if (text.isEmpty()) return null;

            // 1. Match by plate directly (e.g. before whitespace)
            String plate = text.contains(" ") ? text.split("\\s+")[0].trim() : text;
            Ticket t = parkingLot.findActiveTicketByPlate(plate);
            if (t != null) return t;

            // Also check full text as plate
            t = parkingLot.findActiveTicketByPlate(text);
            if (t != null) return t;

            // 2. Try parsing numeric Ticket ID (e.g. from "#1001" or "1001")
            try {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("#?(\\d{4,})").matcher(text);
                if (m.find()) {
                    int id = Integer.parseInt(m.group(1));
                    t = parkingLot.findActiveTicketById(id);
                    if (t != null) return t;
                }
            } catch (Exception ignored) {}

            return null;
        };

        Runnable updateFeePreview = () -> {
            Ticket t = getSelectedTicket.get();
            if (t != null) {
                long now = System.currentTimeMillis();
                int hours = t.calculateHoursParked(now);
                double fee = t.getVehicle().calculateFee(hours);
                feePreviewLabel.setText(String.format("Rs. %.2f (%d hr) - %s", fee, hours, t.getVehicle().getVehicleType()));
                if ("Cash".equals(paymentChannelCombo.getSelectedItem())) {
                    paymentInputField.setText(String.format("%.2f", fee));
                }
            } else {
                feePreviewLabel.setText("Enter valid Vehicle / Ticket ID");
            }
        };

        vehicleCombo.addActionListener(e -> updateFeePreview.run());
        if (vehicleCombo.getEditor() != null) {
            vehicleCombo.getEditor().getEditorComponent().addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyReleased(java.awt.event.KeyEvent e) {
                    updateFeePreview.run();
                }
            });
        }

        paymentChannelCombo.addActionListener(e -> {
            boolean isCash = "Cash".equals(paymentChannelCombo.getSelectedItem());
            promptLabel.setText(isCash ? "Cash Tendered (Rs):" : "Customer UPI VPA:");
            paymentInputField.setText(isCash ? "100.00" : "driver@okhdfcbank");
        });

        updateFeePreview.run();

        dialog.add(new JLabel("Vehicle Number / Plate:"));
        dialog.add(vehicleCombo);
        dialog.add(new JLabel("Calculated Parking Fee:"));
        dialog.add(feePreviewLabel);
        dialog.add(new JLabel("Payment Channel:"));
        dialog.add(paymentChannelCombo);
        dialog.add(promptLabel);
        dialog.add(paymentInputField);

        ModernButton checkoutBtn = new ModernButton("Settle & Release Slot", THEME_PRIMARY, new Color(59, 130, 246));
        checkoutBtn.addActionListener(e -> {
            Ticket targetTicket = getSelectedTicket.get();
            if (targetTicket == null) {
                JOptionPane.showMessageDialog(dialog, "Please select or enter a valid active Vehicle Number.", "Vehicle Not Found", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int ticketId = targetTicket.getTicketId();

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

                String receipt = String.format("DEPARTURE RECEIPT%n----------------------------------%nTicket ID     : #%d%nVehicle Plate : %s%nPayment Mode  : %s%nSlot Freed    : %s",
                        closedTicket.getTicketId(), closedTicket.getVehicle().getPlateNumber(), payment.getMode(),
                        closedTicket.getParkingSlot().getSlotId());

                if (payment instanceof CashPayment) {
                    receipt += String.format("%nChange Returned: Rs. %.2f", ((CashPayment) payment).getChangeGiven());
                } else if (payment instanceof UPIPayment) {
                    receipt += String.format("%nTxn Reference  : %s", ((UPIPayment) payment).getTransactionRef());
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

    private void startClock() {
        Timer timer = new Timer(1000, e -> {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMM yyyy - HH:mm:ss");
            clockLabel.setText(sdf.format(new Date()));
        });
        timer.start();
    }

    // =========================================================================
    // Custom UI Components (High Contrast, Crisp Painting)
    // =========================================================================

    /**
     * Top Header KPI Card Component.
     */
    private static class KPICard extends JPanel {
        private final JLabel valueLabel;

        public KPICard(String title, String initialValue, Color bg, Color textColor) {
            setLayout(new BorderLayout());
            setBackground(bg);
            setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));

            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
            titleLabel.setForeground(new Color(203, 213, 225)); // Slate 300

            valueLabel = new JLabel(initialValue);
            valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            valueLabel.setForeground(textColor);

            add(titleLabel, BorderLayout.NORTH);
            add(valueLabel, BorderLayout.CENTER);
        }

        public void setValue(String val) {
            valueLabel.setText(val);
        }
    }

    /**
     * Custom-painted Slot Card that guarantees 100% visible, high-contrast
     * colors and avoids Windows Look & Feel button painting bugs.
     */
    public static class SlotCard extends JButton {
        private final String slotId;
        private final String slotType;
        private final boolean occupied;
        private final String plateNumber;
        private final String ownerName;

        public SlotCard(ParkingSlot slot) {
            this.slotId = slot.getSlotId();
            this.slotType = slot.getSupportedType();
            this.occupied = !slot.isAvailable();
            if (occupied && slot.getCurrentVehicle() != null) {
                this.plateNumber = slot.getCurrentVehicle().getPlateNumber();
                this.ownerName = slot.getCurrentVehicle().getOwnerName();
            } else {
                this.plateNumber = "";
                this.ownerName = "";
            }

            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(180, 88));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int arc = 12;

            Color cardBg;
            Color borderColor;
            Color slotIdColor;
            Color detailColor;
            Color badgeBg;
            Color badgeText;
            String statusText;

            if (!occupied) {
                // High-contrast clean Emerald Green card
                cardBg = getModel().isRollover() ? new Color(220, 252, 231) : new Color(240, 253, 244);
                borderColor = new Color(22, 163, 74);
                slotIdColor = new Color(20, 83, 45);
                detailColor = new Color(22, 101, 52);
                badgeBg = new Color(187, 247, 208);
                badgeText = new Color(20, 83, 45);
                statusText = "AVAILABLE";
            } else {
                // High-contrast clean Crimson Red card
                cardBg = getModel().isRollover() ? new Color(254, 226, 226) : new Color(254, 242, 242);
                borderColor = new Color(220, 38, 38);
                slotIdColor = new Color(127, 29, 29);
                detailColor = new Color(153, 27, 27);
                badgeBg = new Color(254, 202, 202);
                badgeText = new Color(127, 29, 29);
                statusText = "OCCUPIED";
            }

            // Draw Background Surface
            g2.setColor(cardBg);
            g2.fillRoundRect(2, 2, w - 4, h - 4, arc, arc);

            // Draw Crisp 2px Border
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(getModel().isRollover() ? 2.5f : 1.8f));
            g2.drawRoundRect(2, 2, w - 5, h - 5, arc, arc);

            // Draw Slot ID (Top Left)
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.setColor(slotIdColor);
            g2.drawString(slotId, 12, 23);

            // Draw Vehicle Type Tag (Top Right)
            String typeTag = "[" + slotType.toUpperCase() + "]";
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            int tagW = fm.stringWidth(typeTag);
            g2.setColor(badgeText);
            g2.drawString(typeTag, w - tagW - 12, 23);

            // Subtle Divider Line
            g2.setColor(new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), 60));
            g2.drawLine(12, 31, w - 12, 31);

            // Middle Content Area
            if (occupied) {
                // Prominent bold license plate
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                g2.setColor(detailColor);
                g2.drawString(plateNumber, 12, 51);

                // Driver owner name
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                g2.setColor(THEME_TEXT_MUTED);
                String displayOwner = ownerName.length() > 18 ? ownerName.substring(0, 16) + "..." : ownerName;
                g2.drawString(displayOwner, 12, 68);
            } else {
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.setColor(detailColor);
                g2.drawString("Ready for Parking", 12, 52);
            }

            // Bottom Right Status Badge Pill
            int pillW = 86;
            int pillH = 17;
            int pillX = w - pillW - 10;
            int pillY = h - pillH - 8;
            g2.setColor(badgeBg);
            g2.fillRoundRect(pillX, pillY, pillW, pillH, 8, 8);
            g2.setColor(badgeText);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
            FontMetrics pillFm = g2.getFontMetrics();
            String fullBadge = (occupied ? "● " : "✓ ") + statusText;
            int strX = pillX + (pillW - pillFm.stringWidth(fullBadge)) / 2;
            int strY = pillY + ((pillH - pillFm.getHeight()) / 2) + pillFm.getAscent();
            g2.drawString(fullBadge, strX, strY);

            g2.dispose();
        }
    }

    /**
     * Custom Button with smooth rounded corners and guaranteed high-contrast colors.
     */
    private static class ModernButton extends JButton {
        private final Color normalBg;
        private final Color hoverBg;

        public ModernButton(String text, Color bg, Color hover) {
            super(text);
            this.normalBg = bg;
            this.hoverBg = hover;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (getModel().isPressed()) {
                g2.setColor(normalBg.darker());
            } else if (getModel().isRollover()) {
                g2.setColor(hoverBg);
            } else {
                g2.setColor(normalBg);
            }
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
