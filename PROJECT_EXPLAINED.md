# PROJECT EXPLAINED - EV Charging Slot Booking System
### DBMS Viva Preparation Guide (Sections 1-6)

---

## TABLE OF CONTENTS

1. Project Overview
2. Folder-by-Folder Map
3. Architecture and Data Flow
4. Swing GUI Explained
5. JDBC Explained
6. Database Explained

---

# SECTION 1: PROJECT OVERVIEW
===============================

## What the App Does

This is a desktop application for booking time slots at EV (Electric Vehicle) charging stations.
Think of it like booking a parking spot, but for EV chargers. A customer can:

- Browse available charging stations (filtered by name or city)
- Pick a specific charging slot at a station
- Book it for a 1-hour block at a chosen future time
- Pay for it instantly (mock payment - no real gateway)
- View their booking history
- Cancel a confirmed booking (which auto-refunds the payment)

An admin can:

- View all stations and toggle them between ACTIVE and MAINTENANCE status
- Generate a revenue report for any station (calls a stored procedure in MySQL)

## Who Are the Users?

| Role     | What they can do                                                    |
|----------|---------------------------------------------------------------------|
| CUSTOMER | Register, login, search stations, book/cancel slots, view history   |
| ADMIN    | Login only (no self-registration), manage stations, view revenue    |

## Tech Stack (from pom.xml and code)

| Layer       | Technology                                                   |
|-------------|--------------------------------------------------------------|
| Language    | Java 17 (compiled target 17; tested working on Java 24 too)  |
| GUI         | Java Swing (javax.swing - bundled in the JDK, no download)   |
| DB Driver   | MySQL Connector/J 9.2.0 (com.mysql:mysql-connector-j:9.2.0)  |
| Database    | MySQL 8.0 (InnoDB engine)                                    |
| Build Tool  | Apache Maven 3.x (pom.xml at root)                           |
| Security    | PBKDF2WithHmacSHA256 (javax.crypto, bundled in JDK)          |

From pom.xml:

    <!-- pom.xml lines 11-23 -->
    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
    </properties>
    <dependencies>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <version>9.2.0</version>
        </dependency>
    </dependencies>

The only external library is the MySQL JDBC driver.
Swing, crypto, and the JDBC API are all bundled in the JDK.

## How to Build and Run

    # Step 1: Import the database schema and seed data (run once)
    mysql -u root -p < dbcode.txt

    # Step 2: Set env variables if MySQL password is not blank
    set DB_URL=jdbc:mysql://localhost:3306/ev_charging_system?useSSL=false&serverTimezone=Asia/Kolkata
    set DB_USER=root
    set DB_PASSWORD=your_password

    # Step 3: Compile and run
    mvn clean compile
    mvn exec:java

Maven exec-maven-plugin (version 3.1.0) is configured to run com.evcharging.Main.

---

# SECTION 2: FOLDER-BY-FOLDER MAP
====================================

## Project Tree

    dbms/                              <- Project root
    |
    +-- pom.xml                        <- Maven build config. Declares Java 17 target.
    |                                     Only dependency: mysql-connector-j 9.2.0.
    +-- Main.java                      <- App entry point. Starts Swing on the EDT.
    +-- dbcode.txt                     <- ALL SQL: tables, seed data, indexes, trigger,
    |                                     stored procedure, stored function.
    +-- README.md                      <- Human-readable setup guide.
    +-- EV_Charging_Slot_Booking_SDLC_SPEC.md  <- Original planning spec document.
    |
    +-- ui/                            <- PRESENTATION LAYER (Swing frames)
    |   +-- LoginFrame.java            <- Login screen. Email + password form.
    |   +-- RegisterFrame.java         <- New customer registration screen.
    |   +-- CustomerDashboardFrame.java <- 2-tab customer screen:
    |   |                                   Tab 1: Search stations, pick slot, book.
    |   |                                   Tab 2: View booking history, cancel.
    |   +-- AdminDashboardFrame.java   <- 2-tab admin screen:
    |                                       Tab 1: View/toggle station status.
    |                                       Tab 2: Generate revenue report.
    |
    +-- service/                       <- BUSINESS LOGIC LAYER
    |   +-- AuthService.java           <- login() and registerCustomer() methods.
    |   |                                 Validates fields, checks blocked status,
    |   |                                 calls PasswordHasher, calls UserDAO.
    |   +-- BookingService.java        <- createBooking() validates 5 rules before
    |   |                                 calling BookingDAO.bookSlotTransactional().
    |   +-- AdminService.java          <- addStation(), addSlot(), updateStatus().
    |                                     getStationRevenueReport() calls stored proc.
    |
    +-- dao/                           <- DATA ACCESS LAYER (SQL + JDBC)
    |   +-- UserDAO.java               <- findByEmail(), createUser()
    |   +-- StationDAO.java            <- findActiveStations(), listAllStations(),
    |   |                                 findById(), createStation(), updateStatus()
    |   +-- SlotDAO.java               <- findByStation(), findById(),
    |   |                                 createSlot(), updateSlotStatus()
    |   +-- BookingDAO.java            <- bookSlotTransactional() - the main method.
    |   |                                 Runs a JDBC transaction with 4 SQL statements
    |   |                                 and row-level locks (FOR UPDATE).
    |   +-- PaymentDAO.java            <- findByBookingId(), updateRefunded()
    |                                     (updateRefunded is NOT used for cancellation -
    |                                      the DB trigger handles it instead)
    |
    +-- model/                         <- MODEL LAYER (Plain Java Objects = POJOs)
    |   +-- User.java                  <- Fields: userId, firstName, lastName, email,
    |   |                                 phone, passwordHash, role, status, createdAt
    |   +-- EVStation.java             <- Fields: stationId, stationName, address, city,
    |   |                                 state, postalCode, latitude, longitude,
    |   |                                 openingTime, closingTime, status, createdAt
    |   +-- ChargingSlot.java          <- Fields: slotId, stationId, slotNumber,
    |   |                                 connectorType, maxPowerKw, hourlyRate,
    |   |                                 status, createdAt
    |   +-- Payment.java               <- Fields: paymentId, bookingId, amount, method,
    |   |                                 status, transactionReference, paidAt, updatedAt
    |   +-- BookingStatus.java         <- enum: CONFIRMED, CANCELLED, COMPLETED
    |   +-- ConnectorType.java         <- enum: CCS2, CHAdeMO, TYPE2, GB_T
    |   +-- PaymentMethod.java         <- enum: MOCK
    |   +-- PaymentStatus.java         <- enum: PAID, REFUNDED
    |   +-- SlotStatus.java            <- enum: ACTIVE, MAINTENANCE, INACTIVE
    |   +-- StationStatus.java         <- enum: ACTIVE, MAINTENANCE, INACTIVE
    |   +-- UserRole.java              <- enum: CUSTOMER, ADMIN
    |   +-- UserStatus.java            <- enum: ACTIVE, BLOCKED
    |
    +-- security/
    |   +-- PasswordHasher.java        <- hashPassword() and verifyPassword().
    |                                     Uses PBKDF2WithHmacSHA256. No 3rd-party lib.
    |
    +-- exception/
    |   +-- AuthenticationException.java   <- extends Exception. Thrown on bad login
    |   |                                     or blocked account.
    |   +-- BookingConflictException.java  <- extends Exception. Thrown when slot is
    |   |                                     already booked for that time.
    |   +-- ValidationException.java       <- extends Exception. Thrown for bad input.
    |
    +-- util/
        +-- DBConnection.java          <- getConnection() via DriverManager.
        +-- AppConfig.java             <- Reads DB_URL, DB_USER, DB_PASSWORD from env.

## Why This Structure? (Layered Architecture)

The project follows a 3-tier layered architecture:

    [1] UI Layer (Swing frames in ui/)
         | calls service methods
    [2] Service Layer (business rules in service/)
         | calls DAO methods
    [3] DAO Layer (SQL + JDBC in dao/)
         | uses JDBC connection from util/
    [4] MySQL Database

Benefits of this separation:
  - Service layer enforces business rules before the DAO can touch the database.
  - DAO layer knows nothing about Swing - it only deals with Java objects and SQL.
  - Model objects are shared between layers without coupling the layers together.
  - You can change the UI without touching the DAO, or change the DB without
    touching the Swing code.

EXCEPTION: CustomerDashboardFrame.loadBookingHistory() writes SQL directly in the UI
class instead of going through a DAO. This is a deviation from the clean layered pattern.

---

# SECTION 3: ARCHITECTURE AND DATA FLOW
=========================================

## Flow A: Customer Logs In

Step 1 - User fills in the form and clicks Login

    // ui/LoginFrame.java - line 72
    loginButton.addActionListener(e -> handleLogin());

Step 2 - handleLogin() reads the fields and calls the service

    // ui/LoginFrame.java - lines 78-100
    private void handleLogin() {
        String email = emailField.getText();
        String password = new String(passwordField.getPassword());
        try {
            User user = authService.login(email, password);
            JOptionPane.showMessageDialog(this, "Welcome back, " + user.getFirstName() + "!", ...);"
            this.dispose(); // close login window
            if (user.getRole() == UserRole.ADMIN) {
                new AdminDashboardFrame(user).setVisible(true);
            } else {
                new CustomerDashboardFrame(user).setVisible(true);
            }
        } catch (AuthenticationException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Authentication Error", ...);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), ...);
        }
    }

Step 3 - AuthService.login() runs the business rules

    // service/AuthService.java - lines 26-47
    public User login(String email, String password) throws AuthenticationException, SQLException {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty())
            throw new AuthenticationException("Email and password are required.");

        Optional<User> userOpt = userDAO.findByEmail(email.trim());
        if (userOpt.isEmpty())
            throw new AuthenticationException("Invalid email or password.");

        User user = userOpt.get();
        if (user.getStatus() == UserStatus.BLOCKED)
            throw new AuthenticationException("This account is blocked.");

        if (!PasswordHasher.verifyPassword(password, user.getPasswordHash()))
            throw new AuthenticationException("Invalid email or password.");

        return user;
    }

Step 4 - UserDAO.findByEmail() runs the SQL

    // dao/UserDAO.java - lines 13-25
    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        }
        return Optional.empty();
    }

SQL executed on MySQL:
    SELECT * FROM users WHERE email = 'admin@evcharge.com';

Step 5 - Result returns to UI
  - Found and password matches: LoginFrame disposes itself, opens correct dashboard.
  - Not found or wrong password: AuthenticationException caught, shown as JOptionPane popup.
  - DB error: SQLException caught, shown as JOptionPane popup.

---

## Flow B: Customer Books a Slot (Confirm Booking button)

Step 1 - Customer clicks a station row in the stations table.
A ListSelectionListener fires immediately:

    // ui/CustomerDashboardFrame.java - lines 131-136
    stationTable.getSelectionModel().addListSelectionListener(e -> {
        if (!e.getValueIsAdjusting() && stationTable.getSelectedRow() != -1) {
            Long stationId = (Long) stationTableModel.getValueAt(stationTable.getSelectedRow(), 0);
            loadSlotsForStation(stationId);
        }
    });

loadSlotsForStation() runs this SQL:
    SELECT * FROM charging_slots WHERE station_id = ? ORDER BY slot_number

Results fill the bottom slots table.

Step 2 - Customer clicks a slot row, edits the time field, clicks Confirm Booking.

    // ui/CustomerDashboardFrame.java - lines 175-201
    private void handleBooking() {
        Long slotId = (Long) slotTableModel.getValueAt(selectedSlotRow, 0);
        LocalDateTime startTime = LocalDateTime.parse(timeText, formatter);
        Long bookingId = bookingService.createBooking(currentUser.getUserId(), slotId, startTime);
        JOptionPane.showMessageDialog(this, "Booking confirmed! Booking ID: " + bookingId, ...);
        loadBookingHistory();  // refresh the history tab
    }

Step 3 - BookingService.createBooking() validates 5 rules in sequence:

    // service/BookingService.java - lines 36-91
    // Rule 1: start time must be in the future
    if (startTime.isBefore(LocalDateTime.now()))
        throw new ValidationException("Booking time must be in the future.");

    // Rule 2: start minute, second, nano must all be 0 (hour-aligned booking)
    if (startTime.getMinute() != 0 || startTime.getSecond() != 0 || startTime.getNano() != 0)
        throw new ValidationException("Bookings must start on an exact hour (e.g., 10:00, 11:00).");

    // Rule 3: slot must exist in DB and have status = ACTIVE
    Optional<ChargingSlot> slotOpt = slotDAO.findById(slotId);
    if (slotOpt.isEmpty()) throw new ValidationException(...);
    if (slot.getStatus() != SlotStatus.ACTIVE) throw new ValidationException(...);

    // Rule 4: parent station must exist, be ACTIVE, and booking must be within operating hours
    Optional<EVStation> stationOpt = stationDAO.findById(slot.getStationId());
    // checks station.status == ACTIVE
    // checks bookingStartLocalTime >= station.openingTime
    // checks bookingEndLocalTime <= station.closingTime

    // Rule 5: all checks passed - delegate to the transactional DAO
    return bookingDAO.bookSlotTransactional(userId, slotId, startTime);

Step 4 - BookingDAO.bookSlotTransactional() runs 4 SQL statements inside one JDBC transaction:

    // dao/BookingDAO.java - lines 12-101
    public synchronized Long bookSlotTransactional(...) throws SQLException {
        LocalDateTime endTime = startTime.plusHours(1);

        // SQL 1: Lock the slot row exclusively
        "SELECT cs.slot_id, cs.status AS slot_status, cs.hourly_rate, es.status AS station_status"
        "FROM charging_slots cs JOIN ev_stations es ON es.station_id = cs.station_id"
        "WHERE cs.slot_id = ? FOR UPDATE"

        // SQL 2: Check for time overlap with existing CONFIRMED bookings
        "SELECT booking_id FROM bookings WHERE slot_id = ? AND status = 'CONFIRMED'"
        "AND start_time < ? AND end_time > ? FOR UPDATE"
        // If this returns any row -> throw SQLException with 'Booking conflict' message

        // SQL 3: Insert the booking
        "INSERT INTO bookings (user_id, slot_id, start_time, end_time, rate_per_hour, status)"
        "VALUES (?, ?, ?, ?, ?, 'CONFIRMED')"

        // SQL 4: Insert a mock payment
        "INSERT INTO payments (booking_id, amount, method, status, transaction_reference)"
        "VALUES (?, ?, 'MOCK', 'PAID', ?)"
        // transaction_reference = "TXN_MOCK_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()

        conn.setAutoCommit(false);
        // ... run SQL 1, 2, 3, 4 ...
        conn.commit();
        return bookingId;
        // on any failure: conn.rollback()
    }

What comes back to the screen:
  Success: JOptionPane popup with the new booking_id, history tab reloads.
  Conflict: overlap SQL returns a row -> SQLException('Booking conflict') thrown from DAO
            -> BookingService catches it and re-throws as BookingConflictException
            -> UI catches BookingConflictException and shows error popup.

---

## Flow C: Customer Cancels a Booking

    // ui/CustomerDashboardFrame.java - lines 234-261
    private void handleCancelBooking() {
        Long bookingId   = (Long)   historyTableModel.getValueAt(selectedRow, 0);
        String curStatus = (String) historyTableModel.getValueAt(selectedRow, 6);

        if (!"CONFIRMED".equalsIgnoreCase(curStatus)) {
            JOptionPane.showMessageDialog(this, "Only CONFIRMED bookings can be cancelled.", ...);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure?", ...);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE bookings SET status = 'CANCELLED' WHERE booking_id = ?")) {
                stmt.setLong(1, bookingId);
                stmt.executeUpdate();
                // DB trigger fires automatically
            }
        }
    }

What happens in MySQL after that one UPDATE bookings statement:

    -- dbcode.txt - lines 206-219
    CREATE TRIGGER trg_booking_cancel_payment
    AFTER UPDATE ON bookings
    FOR EACH ROW
    BEGIN
        IF NEW.status = 'CANCELLED' AND OLD.status = 'CONFIRMED' THEN
            UPDATE payments
            SET status = 'REFUNDED', updated_at = CURRENT_TIMESTAMP
            WHERE booking_id = NEW.booking_id AND status = 'PAID';
        END IF;
    END;

Java sends only one UPDATE bookings statement.
The MySQL trigger automatically changes payments.status from PAID to REFUNDED.
Java does NOT call PaymentDAO.updateRefunded() for cancellation - the trigger handles it.

---

# SECTION 4: SWING GUI EXPLAINED
===================================

## Key Swing Concepts

JFrame          : Top-level window. Every screen in this app is a JFrame subclass.
JPanel          : Container for grouping components inside a frame.
JTable          : Displays data in a grid of rows and columns.
DefaultTableModel: Backing store for JTable data. addRow(), setRowCount(0) to clear.
JScrollPane     : Wraps JTable to add scrollbars when data overflows.
JTabbedPane     : Shows multiple panels one at a time, switching via tab labels.
JSplitPane      : Splits an area into two resizable halves.
JTextField      : Single-line text input box.
JPasswordField  : Like JTextField but displays bullet characters.
JTextArea       : Multi-line text area (used for revenue report output).
JButton         : Clickable button.
JLabel          : Non-editable text label.
JOptionPane     : Pre-built popup dialogs (showMessageDialog, showConfirmDialog).

ActionListener  : Interface with one method - actionPerformed(ActionEvent e).
                  Wired to buttons: button.addActionListener(e -> method());
                  Written as a lambda in this project.

ListSelectionListener : Interface fired when user clicks a JTable row.
                  table.getSelectionModel().addListSelectionListener(e -> method());

Event Dispatch Thread (EDT):
  Swing is single-threaded. All UI operations must run on the EDT.
  SwingUtilities.invokeLater() schedules code to run on the EDT.

    // Main.java - lines 14-18
    SwingUtilities.invokeLater(() -> {
        LoginFrame loginFrame = new LoginFrame();
        loginFrame.setVisible(true);
    });

  In this project, DB calls also happen on the EDT (inside ActionListeners).
  The UI freezes briefly during each DB query.
  In production: use SwingWorker to run DB calls in a background thread.

---

## Screen 1: LoginFrame (ui/LoginFrame.java)

Window: 420 x 320 pixels, title = 'EV Charging System - Login'
Components: JLabel (title), JLabel+JTextField (Email), JLabel+JPasswordField (Password),
            JButton (Login), JButton (Register)
Layout: GridBagLayout with GridBagConstraints (gridx, gridy, insets)

    // ui/LoginFrame.java - lines 23-76
    setSize(420, 320);
    setLocationRelativeTo(null);  // centers window on screen
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(8, 8, 8, 8);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    emailField = new JTextField(20);
    passwordField = new JPasswordField(20);
    JButton loginButton = new JButton("Login");
    loginButton.addActionListener(e -> handleLogin());

Error handling: AuthenticationException and SQLException both caught,
shown with JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE).

Routing after login:
    if (user.getRole() == UserRole.ADMIN)
        new AdminDashboardFrame(user).setVisible(true);
    else
        new CustomerDashboardFrame(user).setVisible(true);

---

## Screen 2: RegisterFrame (ui/RegisterFrame.java)

Window: 450 x 420 pixels, title = 'EV Charging System - Account Registration'
Components: 6 input fields (firstName, lastName, email, phone, password, confirmPassword),
            JButton (Register), JButton (Back to Login)
Layout: GridBagLayout

Parent-child window pattern:
  RegisterFrame stores a reference to LoginFrame (the parent).
  backToLogin() calls this.dispose() then parentFrame.setVisible(true).

    // ui/RegisterFrame.java - lines 96-121
    private void handleRegistration() {
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Validation Error", ...);
            return;
        }
        try {
            User newUser = authService.registerCustomer(firstName, lastName, email, phone, password);
            JOptionPane.showMessageDialog(this, "Registration successful!", "Success", ...);
            backToLogin();
        } catch (ValidationException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Validation Error", ...);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), ...);
        }
    }

JPasswordField.getPassword() returns char[] not String.
This is intentional: String objects linger in memory; char[] can be zeroed out.

---

## Screen 3: CustomerDashboardFrame (ui/CustomerDashboardFrame.java)

Window: 900 x 650 pixels
Layout: JTabbedPane with 2 tabs

Tab 1: 'Find & Book Charger'
  Top: JPanel (FlowLayout) with JLabel+JTextField (Station Name),
       JLabel+JTextField (City), JButton (Search)
  Center: JSplitPane (VERTICAL_SPLIT, resizeWeight=0.5)
           Top half: JScrollPane wrapping JTable (stationTable)
                     Columns: ID, Station Name, Address, City, Hours
           Bottom half: JScrollPane wrapping JTable (slotTable)
                        Columns: Slot ID, Slot#, Connector, Power(kW), Rate/Hr, Status
  Bottom: JPanel with JLabel+JTextField (booking time), JButton (Confirm Booking)

Tab 2: 'My Bookings History'
  Center: JScrollPane wrapping JTable (historyTable)
          Columns: Booking ID, Station, Slot#, Start Time, End Time, Rate, Status
  Bottom: JButton (Cancel Selected Booking), JButton (Refresh)

All three JTables use DefaultTableModel with isCellEditable overridden to return false.
This makes cells read-only - the user can see data but not edit it in the table.

Station-to-Slot drill-down:
    // ui/CustomerDashboardFrame.java - lines 131-136
    stationTable.getSelectionModel().addListSelectionListener(e -> {
        if (!e.getValueIsAdjusting() && stationTable.getSelectedRow() != -1) {
            Long stationId = (Long) stationTableModel.getValueAt(stationTable.getSelectedRow(), 0);
            loadSlotsForStation(stationId);  // runs SQL, fills slotTable
        }
    });
  getValueIsAdjusting() prevents the code firing twice per click.

Booking history SQL (written directly in the UI, not via DAO):
    // ui/CustomerDashboardFrame.java - lines 206-210
    String sql =
        "SELECT b.booking_id, es.station_name, cs.slot_number, " +
        "       b.start_time, b.end_time, b.rate_per_hour, b.status " +
        "FROM bookings b " +
        "JOIN charging_slots cs ON cs.slot_id = b.slot_id " +
        "JOIN ev_stations es ON es.station_id = cs.station_id " +
        "WHERE b.user_id = ? ORDER BY b.start_time DESC";
  Joins 3 tables to show station name and slot number alongside booking details.

---

## Screen 4: AdminDashboardFrame (ui/AdminDashboardFrame.java)

Window: 850 x 550 pixels
Layout: JTabbedPane with 2 tabs

Tab 1: 'Station Management'
  Center: JScrollPane wrapping JTable (adminStationTable)
          Columns: ID, Station Name, City, Hours, Status
  Bottom: JButton (Set Active), JButton (Set Maintenance), JButton (Refresh)

Tab 2: 'Revenue Analytics'
  Top: JButton (Generate Revenue Report)
  Center: JScrollPane wrapping JTextArea (reportTextArea)
          setEditable(false), Font: Monospaced 13pt

    // ui/AdminDashboardFrame.java - lines 84-87
    refreshBtn.addActionListener(e -> loadAllStations());
    activateBtn.addActionListener(e -> updateStatus(StationStatus.ACTIVE));
    maintenanceBtn.addActionListener(e -> updateStatus(StationStatus.MAINTENANCE));
    generateReportBtn.addActionListener(e -> handleGenerateReport());

updateStatus() flow:
  1. Gets stationId from selected row in adminStationTable.
  2. Calls adminService.updateStationStatus(stationId, newStatus).
  3. AdminService calls stationDAO.updateStationStatus() which runs:
       UPDATE ev_stations SET status = ? WHERE station_id = ?
  4. loadAllStations() refreshes the table.

handleGenerateReport() flow:
    // ui/AdminDashboardFrame.java - lines 124-161
    Long stationId = (Long) adminStationTableModel.getValueAt(selectedRow, 0);
    LocalDateTime fromDate = LocalDateTime.now().minusMonths(6);
    LocalDateTime toDate   = LocalDateTime.now().plusDays(1);
    Map<String, Object> report = adminService.getStationRevenueReport(stationId, fromDate, toDate);
    // builds formatted string and calls reportTextArea.setText(sb.toString())

Report format shown in the JTextArea:
    ====================================================
               STATION REVENUE REPORT
    ====================================================
     Station ID:            1
     Station Name:          GreenPlug Central Station
     Paid Bookings Count:   4
    ----------------------------------------------------
     Total Gross Revenue:  Rs. 1300.00
     Total Refunds Issued: Rs. 220.00
    ----------------------------------------------------
     NET REVENUE:          Rs. 1080.00
    ====================================================

---

# SECTION 5: JDBC EXPLAINED
==============================

## What is JDBC?

JDBC (Java Database Connectivity) is the standard Java API for connecting to relational
databases. You write SQL as strings in Java, send them to the database, and get results
back as a ResultSet object. The MySQL Connector/J JAR (mysql-connector-j-9.2.0.jar)
implements the JDBC interfaces for MySQL.

Key JDBC interfaces and classes used in this project:
  java.sql.Connection         : Represents a connection to MySQL.
  java.sql.PreparedStatement  : Pre-compiled SQL with ? placeholders.
  java.sql.CallableStatement  : For calling stored procedures.
  java.sql.ResultSet          : Cursor over rows returned by a SELECT.
  java.sql.DriverManager      : Factory that creates Connection objects.
  java.sql.Statement          : Used only for RETURN_GENERATED_KEYS constant.
  java.sql.Timestamp          : Converts Java LocalDateTime to SQL DATETIME.

---

## Step 1: Getting a Connection

    // util/AppConfig.java - lines 3-18
    public static String getDbUrl() {
        return System.getenv().getOrDefault(
            "DB_URL",
            "jdbc:mysql://localhost:3306/ev_charging_system?useSSL=false&serverTimezone=Asia/Kolkata"
        );
    }
    public static String getDbUser() {
        return System.getenv().getOrDefault("DB_USER", "root");
    }
    public static String getDbPassword() {
        return System.getenv().getOrDefault("DB_PASSWORD", "");
    }

    // util/DBConnection.java - lines 7-23
    static {
        Class.forName("com.mysql.cj.jdbc.Driver");  // registers the MySQL driver
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            AppConfig.getDbUrl(), AppConfig.getDbUser(), AppConfig.getDbPassword()
        );
    }

JDBC URL format:
  jdbc:mysql://localhost:3306/ev_charging_system?useSSL=false&serverTimezone=Asia/Kolkata
  |___|mysql|__|___________||__________________|
  protocol  host  port     database name

DriverManager.getConnection() opens a new TCP connection to MySQL each call.
There is NO connection pool - each DAO method opens and closes its own connection.
This is fine for a small university project; in production, use HikariCP or c3p0.

---

## Step 2: PreparedStatement - What it is and why it is used

A PreparedStatement is a pre-compiled SQL template.
? marks are placeholders for values that are set with setXxx() methods.

Example:
    String sql = "SELECT * FROM users WHERE email = ?";
    PreparedStatement stmt = conn.prepareStatement(sql);
    stmt.setString(1, email);  // sets the first ? to the email value
    ResultSet rs = stmt.executeQuery();

Why NOT use String concatenation like this:
    String sql = "SELECT * FROM users WHERE email = '" + email + "'";  // DANGEROUS!

Reason 1 - SQL Injection Prevention:
  If the user types  ' OR '1'='1  in the email field, the concatenated SQL becomes:
    SELECT * FROM users WHERE email = '' OR '1'='1'
  This returns ALL users! An attacker could log in as anyone.
  With PreparedStatement, that input is treated as a literal string value,
  not as SQL syntax. The query would look for an email that literally equals
    ' OR '1'='1  and find no results. Safe.

Reason 2 - Type Safety:
  setLong(1, userId) ensures the value is passed as a numeric type,
  not as a string that the DB has to parse.

Reason 3 - Code Clarity:
  SQL logic is separated from the data values, making code easier to read.

    // dao/UserDAO.java - lines 13-25
    String sql = "SELECT * FROM users WHERE email = ?";
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, email);
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return Optional.of(mapResultSetToUser(rs));
            }
        }
    }

Note the try-with-resources pattern: try (Connection conn = ...; PreparedStatement stmt = ...)
This ensures conn.close() and stmt.close() are called automatically,
even if an exception occurs. This prevents connection leaks.

---

## Step 3: Mapping ResultSet to Model Objects

A ResultSet is a cursor pointing to rows returned by a SELECT.
rs.next() advances the cursor. Read each column by name using getXxx(columnName).

    // dao/UserDAO.java - lines 50-62
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        return new User(
            rs.getLong("user_id"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getString("email"),
            rs.getString("phone"),
            rs.getString("password_hash"),
            UserRole.valueOf(rs.getString("role")),      // "CUSTOMER" -> UserRole.CUSTOMER
            UserStatus.valueOf(rs.getString("status")),  // "ACTIVE"   -> UserStatus.ACTIVE
            rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

The same pattern is in SlotDAO.mapResultSetToSlot(), StationDAO.mapResultSetToStation(),
and PaymentDAO.mapResultSetToPayment().

---

## Step 4: Transactions (setAutoCommit, commit, rollback)

By default, JDBC auto-commits every SQL statement immediately.
For the booking flow, we need 4 SQL statements to all succeed together,
or all be undone (rolled back) together. This is an ACID transaction.

ACID:
  Atomicity   : All 4 statements succeed or all are undone.
  Consistency : DB goes from one valid state to another.
  Isolation   : Other transactions cannot see partial results of this transaction.
  Durability  : Once committed, the data persists even if the server crashes.

    // dao/BookingDAO.java - lines 28-100
    Connection conn = null;
    try {
        conn = DBConnection.getConnection();
        conn.setAutoCommit(false);   // START TRANSACTION

        // Step 1: SELECT ... FOR UPDATE  (lock slot row)
        // Step 2: SELECT ... FOR UPDATE  (check overlap, lock conflicting rows)
        // Step 3: INSERT INTO bookings
        // Step 4: INSERT INTO payments

        conn.commit();   // COMMIT - make all 4 changes permanent
        return bookingId;

    } catch (SQLException e) {
        if (conn != null) conn.rollback();  // ROLLBACK - undo all changes
        throw e;
    } finally {
        if (conn != null) {
            conn.setAutoCommit(true);  // restore default
            conn.close();
        }
    }

Isolation Level:
  Default MySQL InnoDB isolation level = REPEATABLE READ.
  FOR UPDATE upgrades selected rows to exclusive write locks.
  No other transaction can UPDATE or lock those rows until this transaction commits.

---

## Step 5: Calling a Stored Procedure (CallableStatement)

    // service/AdminService.java - lines 65-89
    String sql = "{CALL sp_station_revenue(?, ?, ?)}";  // JDBC escape syntax

    try (Connection conn = DBConnection.getConnection();
         CallableStatement stmt = conn.prepareCall(sql)) {

        stmt.setLong(1, stationId);
        stmt.setTimestamp(2, Timestamp.valueOf(fromDate));
        stmt.setTimestamp(3, Timestamp.valueOf(toDate));

        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                report.put("station_id",          rs.getLong("station_id"));
                report.put("station_name",         rs.getString("station_name"));
                report.put("paid_bookings_count",  rs.getInt("paid_bookings_count"));
                report.put("total_paid_revenue",   rs.getBigDecimal("total_paid_revenue"));
                report.put("total_refunded_amount",rs.getBigDecimal("total_refunded_amount"));
                report.put("net_revenue",          rs.getBigDecimal("net_revenue"));
            }
        }
    }
    return report;  // Map<String, Object>

CallableStatement is the JDBC class for stored procedures.
{CALL proc_name(?, ?, ?)} is the JDBC-standard portable escape syntax.
The procedure returns a ResultSet, read the same way as any SELECT.
Timestamp.valueOf(localDateTime) converts Java LocalDateTime to java.sql.Timestamp
which JDBC maps to MySQL DATETIME.

---

## Step 6: Getting Generated Keys (AUTO_INCREMENT IDs)

After an INSERT, you often need the auto-generated primary key (user_id, slot_id, etc.):

    // dao/UserDAO.java - lines 27-47
    PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    stmt.executeUpdate();
    try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
            user.setUserId(rs.getLong(1));  // column 1 = the generated user_id
        }
    }

Statement.RETURN_GENERATED_KEYS is a flag telling JDBC to capture the auto-increment value.
stmt.getGeneratedKeys() returns a ResultSet with one row and one column: the new ID.

This pattern is used in:
  UserDAO.createUser() -> sets user.userId
  SlotDAO.createSlot() -> sets slot.slotId
  StationDAO.createStation() -> sets station.stationId
  BookingDAO.bookSlotTransactional() -> captures bookingId, then uses it for the payment INSERT

---

# SECTION 6: DATABASE EXPLAINED
==================================

All SQL is in dbcode.txt. Database name: ev_charging_system. Engine: InnoDB.
InnoDB is required for foreign keys and transactions (MyISAM does not support these).

The schema has 5 tables:
  users -> ev_stations -> charging_slots -> bookings -> payments

---

## Table 1: users

Purpose: Stores every person who can log in - both customers and the admin.

    -- dbcode.txt lines 4-14
    CREATE TABLE IF NOT EXISTS users (
        user_id       BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
        first_name    VARCHAR(50)  NOT NULL,
        last_name     VARCHAR(50)  NOT NULL,
        email         VARCHAR(120) NOT NULL UNIQUE,
        phone         VARCHAR(20)  NOT NULL UNIQUE,
        password_hash VARCHAR(255) NOT NULL,
        role          ENUM('CUSTOMER','ADMIN') NOT NULL DEFAULT 'CUSTOMER',
        status        ENUM('ACTIVE','BLOCKED')  NOT NULL DEFAULT 'ACTIVE',
        created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB;

Column by column:

  user_id       : BIGINT UNSIGNED, PK, AUTO_INCREMENT
                  Surrogate key. No natural key chosen as PK.
  first_name    : VARCHAR(50) NOT NULL. Max 50 chars, cannot be empty.
  last_name     : VARCHAR(50) NOT NULL.
  email         : VARCHAR(120) NOT NULL UNIQUE.
                  The unique constraint prevents two accounts with the same email.
                  This is the login identifier checked in UserDAO.findByEmail().
  phone         : VARCHAR(20) NOT NULL UNIQUE.
                  UNIQUE prevents phone number reuse across accounts.
  password_hash : VARCHAR(255) NOT NULL.
                  Stores the PBKDF2 hash, never the plain password.
                  Format: pbkdf2_sha256$600000$<salt_base64>$<hash_base64>
  role          : ENUM('CUSTOMER','ADMIN') NOT NULL DEFAULT 'CUSTOMER'
                  MySQL ENUM enforces that only these two values can be stored.
                  Default is CUSTOMER, so new registrations don't need to set it.
  status        : ENUM('ACTIVE','BLOCKED') NOT NULL DEFAULT 'ACTIVE'
                  BLOCKED users are rejected at login in AuthService.login().
  created_at    : TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                  Auto-set by MySQL on INSERT. Java never writes this column.

Normalization:
  1NF: No repeating groups. Each cell holds one atomic value.
  2NF: No partial dependencies (table has a single-column PK, so 2NF is automatic).
  3NF: Every non-key column (first_name, email, role, etc.) depends directly
       on user_id and on nothing else. No transitive dependencies.

---

## Table 2: ev_stations

Purpose: Represents a physical EV charging station location.

    -- dbcode.txt lines 16-32
    CREATE TABLE IF NOT EXISTS ev_stations (
        station_id   BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
        station_name VARCHAR(100) NOT NULL,
        address      VARCHAR(255) NOT NULL,
        city         VARCHAR(80)  NOT NULL,
        state        VARCHAR(80)  NOT NULL,
        postal_code  VARCHAR(15)  NOT NULL,
        latitude     DECIMAL(9,6) NULL,
        longitude    DECIMAL(9,6) NULL,
        opening_time TIME         NOT NULL,
        closing_time TIME         NOT NULL,
        status       ENUM('ACTIVE','MAINTENANCE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
        created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT chk_station_hours CHECK (opening_time < closing_time),
        CONSTRAINT chk_station_lat   CHECK (latitude  IS NULL OR (latitude  BETWEEN -90  AND 90)),
        CONSTRAINT chk_station_lon   CHECK (longitude IS NULL OR (longitude BETWEEN -180 AND 180))
    ) ENGINE=InnoDB;

Column notes:

  latitude / longitude : DECIMAL(9,6)
                  9 total digits, 6 after the decimal point.
                  Example: 18.520400 (Pune, Maharashtra)
                  NULL allowed for stations that don't have GPS coordinates yet.
  opening_time / closing_time : TIME type (HH:MM:SS).
  status        : ACTIVE = bookable. MAINTENANCE = temporarily offline.
                  INACTIVE = permanently removed. Customers see only ACTIVE stations.

CHECK Constraints:
  chk_station_hours : opening_time < closing_time
                      Cannot create a station that closes before it opens.
  chk_station_lat   : latitude must be NULL or between -90 and 90.
  chk_station_lon   : longitude must be NULL or between -180 and 180.
  If you try to INSERT a row that violates a CHECK constraint, MySQL rejects it.

Normalization: All columns describe the station directly. No repeating groups. 3NF.

---

## Table 3: charging_slots

Purpose: Each individual charging plug/point within a station. A station has many slots.

    -- dbcode.txt lines 34-50
    CREATE TABLE IF NOT EXISTS charging_slots (
        slot_id        BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
        station_id     BIGINT UNSIGNED NOT NULL,
        slot_number    INT UNSIGNED    NOT NULL,
        connector_type ENUM('CCS2','CHAdeMO','TYPE2','GB_T') NOT NULL,
        max_power_kw   DECIMAL(6,2)   NOT NULL,
        hourly_rate    DECIMAL(10,2)  NOT NULL,
        status         ENUM('ACTIVE','MAINTENANCE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
        created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT fk_slot_station
            FOREIGN KEY (station_id) REFERENCES ev_stations(station_id)
            ON UPDATE CASCADE ON DELETE RESTRICT,
        CONSTRAINT uq_slot_number UNIQUE (station_id, slot_number),
        CONSTRAINT chk_slot_power CHECK (max_power_kw > 0),
        CONSTRAINT chk_slot_rate  CHECK (hourly_rate >= 0)
    ) ENGINE=InnoDB;

Column notes:

  station_id     : FK -> ev_stations(station_id)
                   ON DELETE RESTRICT: cannot delete a station that still has slots.
                   ON UPDATE CASCADE: if station_id changes, cascades here automatically.
  connector_type : ENUM with real EV charging standards.
                   CCS2    = Combined Charging System 2. Most common in Europe and India.
                   CHAdeMO = Japanese DC fast charging standard.
                   TYPE2   = IEC 62196-2 AC charging standard.
                   GB_T    = Chinese national standard (GB/T 20234).
  max_power_kw   : DECIMAL(6,2). E.g. 60.00 = fast charger. 22.00 = medium AC charger.
  hourly_rate    : DECIMAL(10,2). Price in Indian Rupees per hour.
  UNIQUE (station_id, slot_number) : Composite unique key.
                   Prevents having two 'Slot 1' at the same station.
  chk_slot_power : max_power_kw > 0 (cannot have a zero-watt charger).
  chk_slot_rate  : hourly_rate >= 0 (rate can be zero but not negative).

Relationship: ev_stations -> charging_slots is 1:N
  (One station has many slots; each slot belongs to exactly one station.)

---

## Table 4: bookings

Purpose: Records every booking made by a customer for a specific slot at a specific time.

    -- dbcode.txt lines 52-77
    CREATE TABLE IF NOT EXISTS bookings (
        booking_id      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
        user_id         BIGINT UNSIGNED NOT NULL,
        slot_id         BIGINT UNSIGNED NOT NULL,
        start_time      DATETIME(3)    NOT NULL,
        end_time        DATETIME(3)    NOT NULL,
        rate_per_hour   DECIMAL(10,2)  NOT NULL,
        status          ENUM('CONFIRMED','CANCELLED','COMPLETED') NOT NULL DEFAULT 'CONFIRMED',
        active_start_time DATETIME(3)
            GENERATED ALWAYS AS (
                CASE WHEN status = 'CONFIRMED' THEN start_time ELSE NULL END
            ) STORED,
        created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES users(user_id)
            ON UPDATE CASCADE ON DELETE RESTRICT,
        CONSTRAINT fk_booking_slot FOREIGN KEY (slot_id) REFERENCES charging_slots(slot_id)
            ON UPDATE CASCADE ON DELETE RESTRICT,
        CONSTRAINT uq_active_slot_start UNIQUE (slot_id, active_start_time),
        CONSTRAINT chk_booking_time CHECK (end_time > start_time),
        CONSTRAINT chk_booking_rate CHECK (rate_per_hour >= 0)
    ) ENGINE=InnoDB;

Column notes:

  start_time / end_time : DATETIME(3) = datetime with millisecond precision.
                  The app always inserts these exactly 1 hour apart.
                  (endTime = startTime.plusHours(1) in BookingDAO)

  rate_per_hour : Copied from charging_slots.hourly_rate at the time of booking.
                  This is a price snapshot. If the admin later changes the slot rate,
                  old bookings still show the original price. Historically accurate.

  active_start_time : GENERATED COLUMN (also called a computed column).
                  MySQL computes this automatically - Java never inserts it.
                  STORED means MySQL physically stores the value in the row.
                  Value:
                    = start_time   when status = 'CONFIRMED'
                    = NULL         when status = 'CANCELLED' or 'COMPLETED'

  UNIQUE (slot_id, active_start_time) : This unique constraint is the key to
                  preventing double-booking at the database level.
                  CONFIRMED booking: active_start_time = real datetime.
                    -> Two CONFIRMED bookings for the same slot at the same time
                       would have the same (slot_id, active_start_time) pair -> REJECTED.
                  CANCELLED/COMPLETED: active_start_time = NULL.
                    -> NULL != NULL in SQL unique checks (NULL is not equal to anything).
                    -> So cancelled bookings do NOT block the unique constraint.
                    -> You CAN re-book a time slot that was previously cancelled.

  updated_at : TIMESTAMP ON UPDATE CURRENT_TIMESTAMP.
               MySQL automatically updates this column whenever the row is modified.
               The trigger's refunded payment sets payments.updated_at the same way.

Relationships:
  users -> bookings : 1:N (one user makes many bookings)
  charging_slots -> bookings : 1:N (one slot can have many bookings at different times)

Normalization note:
  rate_per_hour appears to be redundant since charging_slots has hourly_rate.
  But it is deliberately copied here as a historical price snapshot.
  This is a conscious and justified denormalization - not a normalization violation.

---

## Table 5: payments

Purpose: Stores the payment record for each booking. One booking has exactly one payment.

    -- dbcode.txt lines 79-93
    CREATE TABLE IF NOT EXISTS payments (
        payment_id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
        booking_id            BIGINT UNSIGNED NOT NULL UNIQUE,
        amount                DECIMAL(10,2)  NOT NULL,
        method                ENUM('MOCK')   NOT NULL DEFAULT 'MOCK',
        status                ENUM('PAID','REFUNDED') NOT NULL DEFAULT 'PAID',
        transaction_reference VARCHAR(80)    NOT NULL UNIQUE,
        paid_at               TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        CONSTRAINT fk_payment_booking
            FOREIGN KEY (booking_id) REFERENCES bookings(booking_id)
            ON UPDATE CASCADE ON DELETE RESTRICT,
        CONSTRAINT chk_payment_amount CHECK (amount >= 0)
    ) ENGINE=InnoDB;

Column notes:

  booking_id            : FK -> bookings(booking_id). Also UNIQUE.
                          The UNIQUE constraint enforces the 1:1 relationship at DB level.
                          You cannot insert two payment rows for the same booking_id.
  method                : ENUM('MOCK') only. No real payment gateway is integrated.
  status                : PAID = money collected. REFUNDED = money returned.
                          Changed from PAID to REFUNDED by trigger trg_booking_cancel_payment
                          when the corresponding booking is cancelled.
  transaction_reference : Generated in Java as 'TXN_MOCK_' + UUID substring.
                          UNIQUE prevents accidental duplicate references.
  paid_at               : Set by MySQL to CURRENT_TIMESTAMP on INSERT.
  updated_at            : Updated automatically on UPDATE (e.g. when trigger runs).

Relationship: bookings -> payments is 1:1
  (each booking has exactly one payment record, enforced by UNIQUE on booking_id).

---

## Indexes

    -- dbcode.txt lines 95-98
    CREATE INDEX idx_slots_station_status      ON charging_slots(station_id, status);
    CREATE INDEX idx_bookings_slot_time_status ON bookings(slot_id, start_time, end_time, status);
    CREATE INDEX idx_bookings_user_time        ON bookings(user_id, start_time);
    CREATE INDEX idx_payments_status_date      ON payments(status, paid_at);

Explanation:

  idx_slots_station_status
    : Covers the query: SELECT * FROM charging_slots WHERE station_id = ? AND status = ?
      Used by SlotDAO.findByStation() and the overlap check in BookingDAO.

  idx_bookings_slot_time_status
    : Covers the overlap check query:
        WHERE slot_id = ? AND status = 'CONFIRMED'
        AND start_time < ? AND end_time > ?
      Without this index, MySQL would do a full scan of the bookings table for each booking attempt.

  idx_bookings_user_time
    : Covers: WHERE b.user_id = ? ORDER BY b.start_time DESC
      Used by CustomerDashboardFrame.loadBookingHistory().

  idx_payments_status_date
    : Covers: WHERE status = ? AND paid_at BETWEEN ? AND ?
      Used by sp_station_revenue JOIN condition on payments.

---

## Stored Function: fn_booking_hours

    -- dbcode.txt lines 162-175
    CREATE FUNCTION fn_booking_hours(p_start DATETIME, p_end DATETIME)
    RETURNS DECIMAL(5,2)
    DETERMINISTIC
    BEGIN
        DECLARE v_minutes INT;
        IF p_start IS NULL OR p_end IS NULL OR p_end <= p_start THEN
            RETURN 0.00;
        END IF;
        SET v_minutes = TIMESTAMPDIFF(MINUTE, p_start, p_end);
        RETURN ROUND(v_minutes / 60.0, 2);
    END

Line-by-line:
  RETURNS DECIMAL(5,2) : Return type declared before the body.
  DETERMINISTIC        : Same inputs -> same output always.
                         Required for binary logging and query optimization.
  DECLARE v_minutes INT: Declares a local variable.
  IF p_end <= p_start THEN RETURN 0.00 : Guard clause for invalid input.
  TIMESTAMPDIFF(MINUTE, p_start, p_end) : MySQL built-in that computes difference in minutes.
  ROUND(v_minutes / 60.0, 2) : Converts minutes to hours, rounds to 2 decimal places.

Example: fn_booking_hours('2026-10-01 10:00:00', '2026-10-01 11:30:00') -> 1.50

Is it called from Java? NO.
The function is defined in dbcode.txt but no Java code calls fn_booking_hours().
It demonstrates knowledge of stored functions but is not used by the running application.

---

## Stored Procedure: sp_station_revenue

    -- dbcode.txt lines 177-203
    CREATE PROCEDURE sp_station_revenue(
        IN p_station_id BIGINT UNSIGNED,
        IN p_from_date  DATETIME,
        IN p_to_date    DATETIME
    )
    BEGIN
        SELECT
            es.station_id,
            es.station_name,
            COUNT(CASE WHEN p.status = 'PAID' THEN 1 END)     AS paid_bookings_count,
            COALESCE(SUM(CASE WHEN p.status = 'PAID'
                              THEN p.amount ELSE 0 END), 0.00) AS total_paid_revenue,
            COALESCE(SUM(CASE WHEN p.status = 'REFUNDED'
                              THEN p.amount ELSE 0 END), 0.00) AS total_refunded_amount,
            COALESCE(
                SUM(CASE WHEN p.status = 'PAID'     THEN p.amount ELSE 0 END) -
                SUM(CASE WHEN p.status = 'REFUNDED' THEN p.amount ELSE 0 END),
                0.00
            ) AS net_revenue
        FROM ev_stations es
        LEFT JOIN charging_slots cs ON cs.station_id = es.station_id
        LEFT JOIN bookings b        ON b.slot_id = cs.slot_id
        LEFT JOIN payments p        ON p.booking_id = b.booking_id
                  AND p.paid_at BETWEEN p_from_date AND p_to_date
        WHERE es.station_id = p_station_id
        GROUP BY es.station_id, es.station_name;
    END

Line-by-line explanation:

  IN p_station_id, p_from_date, p_to_date
    : Three input parameters passed from Java via CallableStatement.
      IN means input-only; the procedure cannot change these values in the caller.

  LEFT JOIN ev_stations -> charging_slots -> bookings -> payments
    : Chains all 4 tables. LEFT JOIN is used so the station row appears
      even if it has no charging slots, no bookings, or no payments.
      With INNER JOIN, a station with no bookings would be excluded from results.

  COUNT(CASE WHEN p.status = 'PAID' THEN 1 END)
    : Conditional COUNT. CASE WHEN returns 1 for PAID rows and NULL for others.
      COUNT ignores NULLs, so this effectively counts only PAID payments.

  SUM(CASE WHEN p.status = 'PAID' THEN p.amount ELSE 0 END)
    : Conditional SUM. Adds only the amounts for PAID status rows.
      ELSE 0 means non-PAID rows contribute 0 to the sum.

  COALESCE(..., 0.00)
    : Returns 0.00 if the inner expression evaluates to NULL.
      This happens when a station has no payments at all (all are NULL from the LEFT JOIN).

  net_revenue = total_paid - total_refunded
    : The business definition of net revenue (gross revenue minus refunds).

  p.paid_at BETWEEN p_from_date AND p_to_date
    : This filter is on the JOIN condition (not in the WHERE clause).
      This is intentional: it means the station row is included even if no payments
      fall in the date range - those columns just show 0. If this were in WHERE,
      stations with no in-range payments would be excluded from the results entirely.

  GROUP BY es.station_id, es.station_name
    : Aggregates all rows (one per booking/payment) into a single summary row
      per station.

SQL concepts demonstrated:
  LEFT JOIN, conditional COUNT, conditional SUM, COALESCE,
  GROUP BY, BETWEEN, date range filter, 4-table join.

How Java calls it:
  AdminService.getStationRevenueReport() uses: "{CALL sp_station_revenue(?, ?, ?)}"
  via a CallableStatement. The result is read into a Map<String, Object>.

---

## Trigger: trg_booking_cancel_payment

    -- dbcode.txt lines 206-219
    CREATE TRIGGER trg_booking_cancel_payment
    AFTER UPDATE ON bookings
    FOR EACH ROW
    BEGIN
        IF NEW.status = 'CANCELLED' AND OLD.status = 'CONFIRMED' THEN
            UPDATE payments
            SET status = 'REFUNDED',
                updated_at = CURRENT_TIMESTAMP
            WHERE booking_id = NEW.booking_id
              AND status = 'PAID';
        END IF;
    END;

Line-by-line explanation:

  AFTER UPDATE ON bookings
    : The trigger fires AFTER an UPDATE statement changes a row in bookings.
      AFTER means the booking row is already updated when the trigger runs.
      (BEFORE triggers fire before the change; AFTER fires after.)

  FOR EACH ROW
    : Row-level trigger. Fires once per updated row.
      If one UPDATE statement changes 5 rows, the trigger fires 5 times.
      (Statement-level triggers fire once per SQL statement regardless of rows.)

  NEW.status
    : The value of the status column AFTER the UPDATE.
  OLD.status
    : The value of the status column BEFORE the UPDATE.

  IF NEW.status = 'CANCELLED' AND OLD.status = 'CONFIRMED'
    : The trigger only acts when a booking transitions CONFIRMED -> CANCELLED.
      This prevents it from firing when:
        - A booking changes from CONFIRMED -> COMPLETED (e.g. end of session).
        - Other columns on the row are updated.
        - A booking changes from CANCELLED -> anything.

  UPDATE payments SET status = 'REFUNDED'
    : Marks the payment for this booking as refunded.
      AND status = 'PAID' ensures we only refund payments that were PAID,
      not payments already REFUNDED.

How it is triggered from Java:
  CustomerDashboardFrame.handleCancelBooking() sends this SQL:
    UPDATE bookings SET status = 'CANCELLED' WHERE booking_id = ?
  MySQL sees the status change CONFIRMED -> CANCELLED and fires the trigger.
  Java code does NOT explicitly refund the payment. The trigger handles it.
  PaymentDAO.updateRefunded() exists in the code but is not used for cancellation.

---

## Seed Data Summary

users (5 rows):
  ID 1 : System Admin, admin@evcharge.com, ADMIN, ACTIVE
  ID 2 : Rahul Sharma, rahul.sharma@example.com, CUSTOMER, ACTIVE
  ID 3 : Priya Patel, priya.patel@example.com, CUSTOMER, ACTIVE
  ID 4 : Amit Verma, amit.verma@example.com, CUSTOMER, ACTIVE
  ID 5 : Sneha Kulkarni, sneha.k@example.com, CUSTOMER, BLOCKED
         (Blocked user demonstrates the blocked-account check in AuthService.login())

  IMPORTANT: All seed password_hashes are placeholder strings:
    pbkdf2_sha256$600000$saltsaltsaltsalt$hashhashhashhashhashhashhashhash
  These are NOT real computed hashes. Seed users cannot log in with these.
  To test login: use the Register screen to create a new account,
  or replace the hash in MySQL with a hash computed by PasswordHasher.hashPassword().

ev_stations (4 rows):
  ID 1 : GreenPlug Central Station, Pune, ACTIVE, 06:00 to 23:00
  ID 2 : PowerGrid Hub Wakad, Pune, ACTIVE, 00:00:01 to 23:59:59 (24-hour station)
  ID 3 : ElectroStation Express, Mumbai, ACTIVE, 07:00 to 22:00
  ID 4 : SunCharge Eco Plaza, Pune, MAINTENANCE
         (Demonstrates maintenance status. Customers cannot book its slots.)

charging_slots (11 rows):
  Station 1 (GreenPlug) : 4 slots
    Slot 1: CCS2, 60kW, Rs.250/hr, ACTIVE
    Slot 2: CCS2, 120kW, Rs.400/hr, ACTIVE
    Slot 3: TYPE2, 22kW, Rs.150/hr, ACTIVE
    Slot 4: GB_T, 50kW, Rs.200/hr, MAINTENANCE (demonstrates maintenance slot)
  Station 2 (PowerGrid) : 4 slots, all ACTIVE
    Slot 5: CCS2, 150kW, Rs.450/hr
    Slot 6: CHAdeMO, 50kW, Rs.220/hr
    Slot 7: TYPE2, 22kW, Rs.150/hr
    Slot 8: GB_T, 60kW, Rs.240/hr
  Station 3 (ElectroStation) : 3 slots, all ACTIVE
    Slot 9: CCS2, 120kW, Rs.420/hr
    Slot 10: TYPE2, 22kW, Rs.180/hr
    Slot 11: CHAdeMO, 50kW, Rs.250/hr
  Station 4 (SunCharge) : 0 slots

bookings (6 rows):
  ID 1 : Rahul, Slot 1, Oct 01 10:00-11:00, Rs.250/hr, COMPLETED
  ID 2 : Rahul, Slot 2, Oct 02 14:00-15:00, Rs.400/hr, COMPLETED
  ID 3 : Priya, Slot 5, Oct 05 09:00-10:00, Rs.450/hr, COMPLETED
  ID 4 : Amit,  Slot 1, Oct 10 11:00-12:00, Rs.250/hr, CONFIRMED (upcoming)
  ID 5 : Priya, Slot 9, Oct 12 16:00-17:00, Rs.420/hr, CONFIRMED (upcoming)
  ID 6 : Rahul, Slot 6, Oct 06 11:00-12:00, Rs.220/hr, CANCELLED

payments (6 rows):
  For booking 1 : Rs.250, MOCK, PAID, TXN_MOCK_20261001_1001
  For booking 2 : Rs.400, MOCK, PAID, TXN_MOCK_20261001_1002
  For booking 3 : Rs.450, MOCK, PAID, TXN_MOCK_20261004_1003
  For booking 4 : Rs.250, MOCK, PAID, TXN_MOCK_20261008_1004
  For booking 5 : Rs.420, MOCK, PAID, TXN_MOCK_20261008_1005
  For booking 6 : Rs.220, MOCK, REFUNDED, TXN_MOCK_20261005_1006
                  (REFUNDED because booking 6 is CANCELLED - shows trigger behavior)

---

## Important Queries in the Java DAOs

### Query 1: Find active stations with optional filters (StationDAO.findActiveStations)

    SELECT * FROM ev_stations
    WHERE status = 'ACTIVE'
      [AND station_name LIKE '%search_text%']
      [AND city LIKE '%city_text%']
    ORDER BY station_name

SQL concepts:
  LIKE with % wildcard for partial/substring matching.
  Dynamic WHERE clause - the Java code uses StringBuilder to conditionally
  append AND clauses based on whether the search fields are non-empty.
  ORDER BY for alphabetical sorting.

    // dao/StationDAO.java - lines 14-44
    StringBuilder sql = new StringBuilder("SELECT * FROM ev_stations WHERE status = 'ACTIVE'");
    if (searchText != null && !searchText.trim().isEmpty()) {
        sql.append(" AND station_name LIKE ?");
    }
    if (city != null && !city.trim().isEmpty()) {
        sql.append(" AND city LIKE ?");
    }
    sql.append(" ORDER BY station_name");

---

### Query 2: Overlap check to prevent double-booking (BookingDAO.bookSlotTransactional)

    SELECT booking_id FROM bookings
    WHERE slot_id = ?
      AND status = 'CONFIRMED'
      AND start_time < ?    -- first ? = new booking's end_time
      AND end_time   > ?    -- second ? = new booking's start_time
    FOR UPDATE

SQL concept - interval overlap condition:
  Two time intervals [A, B] and [C, D] overlap if and only if: A < D AND B > C
  In our terms:
    existing.start_time < new.end_time   (existing starts before new ends)
    existing.end_time   > new.start_time (existing ends after new starts)
  If BOTH are true, the intervals overlap -> reject the booking.

FOR UPDATE: Acquires a row-level exclusive lock on the matching rows.
  Other transactions cannot lock or modify these rows until this transaction ends.

---

### Query 3: Booking history with 3-table JOIN (CustomerDashboardFrame.loadBookingHistory)

    SELECT b.booking_id, es.station_name, cs.slot_number,
           b.start_time, b.end_time, b.rate_per_hour, b.status
    FROM bookings b
    JOIN charging_slots cs ON cs.slot_id    = b.slot_id
    JOIN ev_stations    es ON es.station_id = cs.station_id
    WHERE b.user_id = ?
    ORDER BY b.start_time DESC

SQL concepts:
  INNER JOIN (written as JOIN): only returns rows where the join condition is true.
    bookings JOIN charging_slots: links booking to the slot that was booked.
    charging_slots JOIN ev_stations: links the slot to its parent station.
  WHERE b.user_id = ?: filters results to only the logged-in user's bookings.
  ORDER BY b.start_time DESC: newest bookings appear at the top of the table.

---

### Query 4: Revenue aggregation inside sp_station_revenue

    SELECT es.station_id, es.station_name,
           COUNT(CASE WHEN p.status = 'PAID' THEN 1 END) AS paid_bookings_count,
           COALESCE(SUM(CASE WHEN p.status = 'PAID'
                             THEN p.amount ELSE 0 END), 0) AS total_paid_revenue,
           ... net_revenue ...
    FROM ev_stations es
    LEFT JOIN charging_slots cs ON cs.station_id = es.station_id
    LEFT JOIN bookings b        ON b.slot_id = cs.slot_id
    LEFT JOIN payments p        ON p.booking_id = b.booking_id
              AND p.paid_at BETWEEN ? AND ?
    WHERE es.station_id = ?
    GROUP BY es.station_id, es.station_name

SQL concepts:
  LEFT JOIN: keeps station even if it has no charging slots, bookings, or payments.
  CASE WHEN inside aggregate functions: conditional aggregation.
  COUNT(CASE WHEN ...): counts only rows that satisfy the condition.
  SUM(CASE WHEN ...): sums only amounts for rows satisfying the condition.
  COALESCE(expr, 0): returns 0.00 if expr is NULL (station with no payments).
  GROUP BY: collapses all rows for the station into one summary row.
  BETWEEN: inclusive date range filter on the JOIN condition.
  4-table JOIN: ev_stations -> charging_slots -> bookings -> payments.

---
# SECTION 7: BOOKING, TRANSACTIONS & CONCURRENCY
================================================

## 1. Concurrency Challenges in Slot Booking

In an EV charging reservation system, the most dangerous race condition is a DOUBLE-BOOKING:
two different users concurrently attempting to book the exact same charging slot for the same
time interval (or overlapping intervals).

If concurrency control is absent or implemented solely in application memory (such as Java
`synchronized`), a race condition allows both transactions to read the slot as available and
subsequently insert duplicate confirmed bookings.

In this project, double-booking prevention is enforced at three distinct architectural layers:
1. Database Pessimistic Row Locking via `SELECT ... FOR UPDATE`
2. Interval Overlap SQL Query executed inside the lock window
3. Database Schema-Level Defense-in-Depth: STORED Generated Column + UNIQUE Constraint (`uq_active_slot_start`)
4. Explicit JDBC Transaction Management (`setAutoCommit(false)`, `commit()`, `rollback()`)

---

## 2. Real Code Implementation: Transactional Booking

The entire booking workflow is managed inside `com.evcharging.dao.BookingDAO`:

File: dao/BookingDAO.java (Lines 12-101)
```java
public synchronized Long bookSlotTransactional(Long userId, Long slotId, LocalDateTime startTime) throws SQLException {
    LocalDateTime endTime = startTime.plusHours(1);

    String lockSlotSql = "SELECT cs.slot_id, cs.status AS slot_status, cs.hourly_rate, es.status AS station_status " +
                         "FROM charging_slots cs JOIN ev_stations es ON es.station_id = cs.station_id " +
                         "WHERE cs.slot_id = ? FOR UPDATE";

    String checkOverlapSql = "SELECT booking_id FROM bookings WHERE slot_id = ? AND status = 'CONFIRMED' " +
                             "AND start_time < ? AND end_time > ? FOR UPDATE";

    String insertBookingSql = "INSERT INTO bookings (user_id, slot_id, start_time, end_time, rate_per_hour, status) " +
                              "VALUES (?, ?, ?, ?, ?, 'CONFIRMED')";

    String insertPaymentSql = "INSERT INTO payments (booking_id, amount, method, status, transaction_reference) " +
                              "VALUES (?, ?, 'MOCK', 'PAID', ?)";

    Connection conn = null;
    try {
        conn = DBConnection.getConnection();
        conn.setAutoCommit(false); // Begin Transaction

        BigDecimal hourlyRate;
        // 1. Lock slot and verify status
        try (PreparedStatement stmt = conn.prepareStatement(lockSlotSql)) {
            stmt.setLong(1, slotId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Slot not found.");
                }
                if (!"ACTIVE".equals(rs.getString("slot_status")) || !"ACTIVE".equals(rs.getString("station_status"))) {
                    throw new SQLException("Slot or Station is currently inactive/under maintenance.");
                }
                hourlyRate = rs.getBigDecimal("hourly_rate");
            }
        }

        // 2. Check overlap interval
        try (PreparedStatement stmt = conn.prepareStatement(checkOverlapSql)) {
            stmt.setLong(1, slotId);
            stmt.setTimestamp(2, Timestamp.valueOf(endTime));
            stmt.setTimestamp(3, Timestamp.valueOf(startTime));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    throw new SQLException("Booking conflict: Selected slot is already booked for this time interval.");
                }
            }
        }

        // 3. Insert Booking
        Long bookingId;
        try (PreparedStatement stmt = conn.prepareStatement(insertBookingSql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, slotId);
            stmt.setTimestamp(3, Timestamp.valueOf(startTime));
            stmt.setTimestamp(4, Timestamp.valueOf(endTime));
            stmt.setBigDecimal(5, hourlyRate);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    bookingId = rs.getLong(1);
                } else {
                    throw new SQLException("Failed to retrieve generated booking ID.");
                }
            }
        }

        // 4. Insert Mock Payment
        try (PreparedStatement stmt = conn.prepareStatement(insertPaymentSql)) {
            stmt.setLong(1, bookingId);
            stmt.setBigDecimal(2, hourlyRate);
            stmt.setString(3, "TXN_MOCK_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            stmt.executeUpdate();
        }

        conn.commit(); // Commit transaction
        return bookingId;

    } catch (SQLException e) {
        if (conn != null) {
            conn.rollback(); // Rollback on failure
        }
        throw e;
    } finally {
        if (conn != null) {
            conn.setAutoCommit(true);
            conn.close();
        }
    }
}
```

---

## 3. Detailed Mechanism of Double-Booking Prevention

### A. Pessimistic Row Locking (`SELECT ... FOR UPDATE`)
In MySQL InnoDB, `SELECT ... FOR UPDATE` acquires an exclusive write lock (X-lock) on the selected row:
```sql
SELECT cs.slot_id, cs.status AS slot_status, cs.hourly_rate, es.status AS station_status
FROM charging_slots cs JOIN ev_stations es ON es.station_id = cs.station_id
WHERE cs.slot_id = ? FOR UPDATE
```
When User A begins this transaction, MySQL locks the row in `charging_slots` where `slot_id = ?`.
If User B concurrently attempts to book the same slot, User B's transaction halts in a `LOCK WAIT` state at this exact SQL statement until User A commits or rolls back.
This serializes concurrent booking requests for that specific physical charging slot.

### B. The Overlap Check Query
Next, inside the lock boundary, the transaction checks for any conflicting active reservations:
```sql
SELECT booking_id FROM bookings
WHERE slot_id = ?
  AND status = 'CONFIRMED'
  AND start_time < ?
  AND end_time > ?
FOR UPDATE
```
Note the order of timestamp parameters passed in Java (dao/BookingDAO.java lines 51-52):
- Parameter 1: `slotId`
- Parameter 2: `Timestamp.valueOf(endTime)` (requested new booking end time)
- Parameter 3: `Timestamp.valueOf(startTime)` (requested new booking start time)

#### Mathematical Proof of the Overlap Condition
Let an existing confirmed booking interval be `[S_exist, E_exist]`.
Let the requested new booking interval be `[S_req, E_req]`.
Two continuous time intervals overlap if and only if:
    `S_exist < E_req`  AND  `E_exist > S_req`

Let us evaluate every possible scheduling case:
1. Exact match (e.g., Existing: 10:00–11:00, Request: 10:00–11:00):
   - `S_exist < E_req`: 10:00 < 11:00 (TRUE)
   - `E_exist > S_req`: 11:00 > 10:00 (TRUE)
   - Result: CONFLICT DETECTED.
2. Request overlaps existing start (e.g., Existing: 10:00–11:00, Request: 09:30–10:30):
   - `S_exist < E_req`: 10:00 < 10:30 (TRUE)
   - `E_exist > S_req`: 11:00 > 09:30 (TRUE)
   - Result: CONFLICT DETECTED.
3. Request overlaps existing end (e.g., Existing: 10:00–11:00, Request: 10:30–11:30):
   - `S_exist < E_req`: 10:00 < 11:30 (TRUE)
   - `E_exist > S_req`: 11:00 > 10:30 (TRUE)
   - Result: CONFLICT DETECTED.
4. Request encloses existing (e.g., Existing: 10:00–11:00, Request: 09:00–12:00):
   - `S_exist < E_req`: 10:00 < 12:00 (TRUE)
   - `E_exist > S_req`: 11:00 > 09:00 (TRUE)
   - Result: CONFLICT DETECTED.
5. Adjacent previous interval (e.g., Existing: 09:00–10:00, Request: 10:00–11:00):
   - `S_exist < E_req`: 09:00 < 11:00 (TRUE)
   - `E_exist > S_req`: 10:00 > 10:00 (FALSE — strict inequality)
   - Result: NO CONFLICT. Adjacent back-to-back bookings are cleanly allowed!
6. Adjacent next interval (e.g., Existing: 11:00–12:00, Request: 10:00–11:00):
   - `S_exist < E_req`: 11:00 < 11:00 (FALSE)
   - Result: NO CONFLICT. Allowed!

### C. Schema-Level Defense: Generated Column + UNIQUE Constraint
Even if an application developer made a mistake or another application bypassed `BookingDAO`, the MySQL database engine itself guarantees that duplicate confirmed bookings cannot be committed.

File: dbcode.txt (Lines 60-74)
```sql
CREATE TABLE IF NOT EXISTS bookings (
    booking_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    slot_id BIGINT UNSIGNED NOT NULL,
    start_time DATETIME(3) NOT NULL,
    end_time DATETIME(3) NOT NULL,
    rate_per_hour DECIMAL(10,2) NOT NULL,
    status ENUM('CONFIRMED','CANCELLED','COMPLETED') NOT NULL DEFAULT 'CONFIRMED',
    active_start_time DATETIME(3)
        GENERATED ALWAYS AS (
            CASE WHEN status = 'CONFIRMED' THEN start_time ELSE NULL END
        ) STORED,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_booking_slot FOREIGN KEY (slot_id) REFERENCES charging_slots(slot_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT uq_active_slot_start UNIQUE (slot_id, active_start_time),
    CONSTRAINT chk_booking_time CHECK (end_time > start_time),
    CONSTRAINT chk_booking_rate CHECK (rate_per_hour >= 0)
) ENGINE=InnoDB;
```

How this works:
1. `active_start_time` is a STORED GENERATED column computed automatically by MySQL.
2. When `status = 'CONFIRMED'`, `active_start_time` stores the exact `start_time`.
3. When `status = 'CANCELLED'` or `'COMPLETED'`, `active_start_time` becomes `NULL`.
4. In MySQL InnoDB (and the SQL-92 standard), `UNIQUE` indexes allow multiple `NULL` values because `NULL != NULL`.
5. Therefore:
   - Multiple cancelled bookings can exist for Slot 1 at 10:00.
   - But ONLY ONE row with `status = 'CONFIRMED'` can ever exist for `(slot_id, active_start_time)`!
   - Any second attempt to insert a `CONFIRMED` booking for the same slot and start time triggers a MySQL Error 1062 (`Duplicate entry for key 'uq_active_slot_start'`).

---

## 4. Rollback Behavior and Error Handling

If a booking conflict occurs:
1. The overlap query returns a row (`rs.next() == true`).
2. `BookingDAO` throws:
   `throw new SQLException("Booking conflict: Selected slot is already booked for this time interval.");`
3. The catch block executes `conn.rollback()`.
   Neither the `bookings` row nor the `payments` row is committed.
4. The finally block resets `conn.setAutoCommit(true)` and closes the connection.
5. In `com.evcharging.service.BookingService` (lines 83-90):
   ```java
   try {
       return bookingDAO.bookSlotTransactional(userId, slotId, startTime);
   } catch (SQLException e) {
       if (e.getMessage() != null && e.getMessage().contains("Booking conflict")) {
           throw new BookingConflictException("The selected time slot has already been booked by another user.");
       }
       throw e;
   }
   ```
6. In `com.evcharging.ui.CustomerDashboardFrame` (lines 196-198), `BookingConflictException` is caught and presented as a modal warning dialog:
   ```java
   } catch (ValidationException | BookingConflictException e) {
       JOptionPane.showMessageDialog(this, e.getMessage(), "Booking Failed", JOptionPane.ERROR_MESSAGE);
   }
   ```

---

## 5. Timeline: Two Users Booking Simultaneously

Scenario: User 1 (Rahul) and User 2 (Priya) simultaneously click "Confirm Booking" for Slot 1 at 14:00 on 2026-10-20.

```text
User 1 (Thread 1)                      MySQL InnoDB Engine                    User 2 (Thread 2)
-----------------                      -------------------                    -----------------
conn1.setAutoCommit(false);
SELECT slot_id=1 FOR UPDATE ---------> Acquires X-lock on Slot 1 row.
                                                                              conn2.setAutoCommit(false);
                                       <------------------------------------- SELECT slot_id=1 FOR UPDATE
                                       Thread 2 BLOCKS (waits for lock)...
SELECT overlap WHERE slot_id=1 ...
Result: 0 rows found.
INSERT INTO bookings (CONFIRMED) ----> Generates booking_id = 101.
INSERT INTO payments (MOCK, PAID) ---> Generates payment row.
conn1.commit(); ---------------------> Writes to Redo Log.
                                       X-lock on Slot 1 RELEASED!
                                       Thread 2 UNBLOCKS, acquires X-lock --> Thread 2 resumes execution.
                                                                              SELECT overlap WHERE slot_id=1 ...
                                       <------------------------------------- Returns User 1's committed booking!
                                                                              Conflict row found!
                                                                              Throws SQLException("Booking conflict")
                                                                              conn2.rollback();
                                                                              Lock released.
                                                                              UI displays: "Booking Failed"
```

Outcome: Exactly one user obtains the reservation. Zero double-booking occurs.

---

## 6. Why Java `synchronized` Alone Is NOT Enough

In `BookingDAO.java` line 12, the method is declared:
`public synchronized Long bookSlotTransactional(...)`

Examiners in DBMS vivas often ask: *"If you already have `synchronized` in Java, why do you need database transactions and `SELECT ... FOR UPDATE`?"*

The student must give these 4 points:
1. Scope of `synchronized`: Java `synchronized` locks a monitor in the heap memory of ONE specific JVM. It coordinates threads within a single operating system process only.
2. Multi-Process / Multi-Machine Reality: In real-world deployments—or if two users simply run the desktop application from two different terminals or computers—there are TWO independent JVM processes. JVM 1 has no knowledge of monitor locks inside JVM 2!
3. External Database Clients: Other services, web applications, or database administrators running queries in MySQL Workbench completely bypass the Java JVM.
4. Single Source of Truth: In distributed software systems, data integrity and concurrency control MUST reside in the storage tier where state is persisted—inside MySQL InnoDB through ACID transactions, row locks, and relational constraints.


================================================================================


# SECTION 8: SECURITY
===================

## 1. Password Hashing (PBKDF2WithHmacSHA256)

Plaintext passwords are never stored in the database. Password security is implemented in `com.evcharging.security.PasswordHasher`:

File: security/PasswordHasher.java (Complete class)
```java
package com.evcharging.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 600000;
    private static final int SALT_SIZE = 16;
    private static final int KEY_LENGTH = 256;

    public static String hashPassword(String password) {
        byte[] salt = new byte[SALT_SIZE];
        new SecureRandom().nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt);

        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        String hashBase64 = Base64.getEncoder().encodeToString(hash);

        return "pbkdf2_sha256$" + ITERATIONS + "$" + saltBase64 + "$" + hashBase64;
    }

    public static boolean verifyPassword(String password, String storedHash) {
        if (storedHash == null || !storedHash.startsWith("pbkdf2_sha256$")) {
            return false;
        }

        String[] parts = storedHash.split("\\$");
        if (parts.length != 4) {
            return false;
        }

        int iterations = Integer.parseInt(parts[1]);
        byte[] salt = Base64.getDecoder().decode(parts[2]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[3]);

        byte[] actualHash = pbkdf2(password.toCharArray(), salt, iterations, expectedHash.length * 8);

        return MessageDigest.isEqual(expectedHash, actualHash);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt) {
        return pbkdf2(password, salt, ITERATIONS, KEY_LENGTH);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
}
```

### Cryptographic Parameters Explained
1. Algorithm: `PBKDF2WithHmacSHA256` (Password-Based Key Derivation Function 2 using HMAC-SHA-256).
2. Salt: 16 cryptographically random bytes generated via `java.security.SecureRandom`. A unique salt per user guarantees that identical passwords hash to completely different values, completely defeating Rainbow Table precomputation attacks.
3. Iterations: 600,000 iterations (OWASP Recommended Standard). This intentional CPU work factor slows down offline dictionary and brute-force attacks via GPU clusters or ASICs.
4. Key Length: 256 bits (32 bytes).
5. Storage Format: Modular format:
   `pbkdf2_sha256$<iterations>$<base64_salt>$<base64_hash>`
   Stored in `users.password_hash` (`VARCHAR(255)`).
6. Constant-Time Verification: Line 45 uses `MessageDigest.isEqual(expectedHash, actualHash)`.
   Unlike standard `String.equals()`, which short-circuits on the first mismatched byte, `MessageDigest.isEqual()` takes constant time regardless of where discrepancies occur. This protects the system against TIMING ATTACKS.

---

## 2. SQL Injection Protection

SQL Injection is mitigated across the entire codebase through strict adherence to parameterized queries via `java.sql.PreparedStatement`.

Rules followed in every DAO (`UserDAO`, `StationDAO`, `SlotDAO`, `BookingDAO`, `PaymentDAO`):
1. No String Concatenation: User-supplied parameters are never concatenated into SQL query strings (e.g. no `sql = "... WHERE email = '" + email + "'"`).
2. Placeholder Substitution: All inputs are represented as `?` placeholders.
3. Type-Safe Parameter Binding: Parameters are bound using strongly typed JDBC setter methods:
   - `stmt.setString(1, email)`
   - `stmt.setLong(1, slotId)`
   - `stmt.setTimestamp(2, Timestamp.valueOf(endTime))`
   - `stmt.setBigDecimal(5, hourlyRate)`
4. Safe Dynamic Filtering: Even when dynamically filtering stations by name or city in `StationDAO.java` (lines 18-36), placeholders are appended conditionally and bound by index:
   ```java
   if (searchText != null && !searchText.trim().isEmpty()) {
       sql.append(" AND station_name LIKE ?");
   }
   ...
   stmt.setString(paramIndex++, "%" + searchText.trim() + "%");
   ```
   The MySQL JDBC driver automatically escapes all special characters (quotes, semicolons) before transmission.

---

## 3. Database Credentials Handling

Credentials are isolated in `com.evcharging.util.AppConfig`:

File: util/AppConfig.java (Complete class)
```java
package com.evcharging.util;

public class AppConfig {
    public static String getDbUrl() {
        return System.getenv().getOrDefault(
            "DB_URL",
            "jdbc:mysql://localhost:3306/ev_charging_system?useSSL=false&serverTimezone=Asia/Kolkata"
        );
    }

    public static String getDbUser() {
        return System.getenv().getOrDefault("DB_USER", "root");
    }

    public static String getDbPassword() {
        return System.getenv().getOrDefault("DB_PASSWORD", "");
    }
}
```

Security benefits:
- Credentials can be passed at runtime via environment variables (`DB_URL`, `DB_USER`, `DB_PASSWORD`) without modifying source files.
- Local secrets and passwords are never committed to version control repositories.


================================================================================


# SECTION 9: VALIDATION & ERROR HANDLING
=======================================

## 1. Custom Exceptions Hierarchy

The project defines three domain-specific checked exceptions in `com.evcharging.exception`:

1. `ValidationException` (exception/ValidationException.java):
   ```java
   package com.evcharging.exception;
   public class ValidationException extends Exception {
       public ValidationException(String message) { super(message); }
   }
   ```
   Thrown when user inputs violate business logic constraints (e.g., booking in the past, non-hour-aligned intervals, invalid email formats).

2. `BookingConflictException` (exception/BookingConflictException.java):
   ```java
   package com.evcharging.exception;
   public class BookingConflictException extends Exception {
       public BookingConflictException(String message) { super(message); }
   }
   ```
   Thrown when an overlap query or unique constraint detects that a slot has already been booked for the requested interval.

3. `AuthenticationException` (exception/AuthenticationException.java):
   ```java
   package com.evcharging.exception;
   public class AuthenticationException extends Exception {
       public AuthenticationException(String message) { super(message); }
   }
   ```
   Thrown when login credentials fail or when a user account status is BLOCKED.

---

## 2. Exhaustive Validation Rules in the Codebase

### A. Booking Validation (`BookingService.java` lines 36-81)
Before reaching the database transaction:
1. Null Check: `userId`, `slotId`, and `startTime` must not be null.
2. Future Booking Rule: `startTime.isBefore(LocalDateTime.now())` -> throws `ValidationException("Booking time must be in the future.")`.
3. Hour-Aligned Rule: `startTime.getMinute() != 0 || startTime.getSecond() != 0 || startTime.getNano() != 0` -> throws `ValidationException("Bookings must start on an exact hour (e.g., 10:00, 11:00).")`.
4. Slot Availability Rule: Slot must exist and have `status == SlotStatus.ACTIVE`. Otherwise throws `ValidationException("This charging slot is currently unavailable or under maintenance.")`.
5. Station Availability Rule: Parent station must exist and have `status == StationStatus.ACTIVE`. Otherwise throws `ValidationException("The EV station is currently closed or under maintenance.")`.
6. Operating Hours Rule: Requested 1-hour interval `[startTime.toLocalTime(), startTime.plusHours(1).toLocalTime()]` must fall strictly within `[station.getOpeningTime(), station.getClosingTime()]`. Otherwise throws `ValidationException("Requested booking interval falls outside station operating hours...")`.

### B. User Registration Validation (`AuthService.java` lines 49-80)
1. Required Fields: `firstName` and `lastName` must be non-null and non-empty.
2. Email Format: Evaluated using regex `^[A-Za-z0-9+_.-]+@(.+)$`.
3. Phone Format: Length must be at least 10 characters.
4. Password Length: Must be at least 6 characters.
5. Email Uniqueness: Queries `userDAO.findByEmail(email)`. If present -> throws `ValidationException("An account with this email already exists.")`.

### C. Authentication Validation (`AuthService.java` lines 26-47)
1. Required Inputs: Email and password cannot be null or blank.
2. User Existence: If no user found -> throws `AuthenticationException("Invalid email or password.")` (generic error to prevent account enumeration).
3. Account Status Check: `if (user.getStatus() == UserStatus.BLOCKED)` -> throws `AuthenticationException("This account is blocked. Please contact support.")`.
4. Hash Verification: `if (!PasswordHasher.verifyPassword(password, user.getPasswordHash()))` -> throws `AuthenticationException("Invalid email or password.")`.

### D. Booking Cancellation Validation (`CustomerDashboardFrame.java` lines 234-260)
1. Selection Check: Must have a selected row in the history table.
2. Status Check: `if (!"CONFIRMED".equalsIgnoreCase(currentStatus))` -> shows warning: "Only CONFIRMED bookings can be cancelled."
3. User Confirmation: Prompts user via `JOptionPane.showConfirmDialog`.

---

## 3. How Errors Reach and Are Displayed on the UI

All Swing action listeners catch exceptions at the boundary and translate them to modal dialog boxes:

File: ui/CustomerDashboardFrame.java (Lines 196-200)
```java
} catch (ValidationException | BookingConflictException e) {
    JOptionPane.showMessageDialog(this, e.getMessage(), "Booking Failed", JOptionPane.ERROR_MESSAGE);
} catch (Exception e) {
    JOptionPane.showMessageDialog(this, "Invalid date/time format. Please use 'YYYY-MM-DD HH:00'.", "Input Error", JOptionPane.ERROR_MESSAGE);
}
```

File: ui/LoginFrame.java (Lines 95-100)
```java
} catch (AuthenticationException e) {
    JOptionPane.showMessageDialog(this, e.getMessage(), "Authentication Error", JOptionPane.ERROR_MESSAGE);
} catch (SQLException e) {
    JOptionPane.showMessageDialog(this, "Database connection error: " + e.getMessage(), 
            "Database Error", JOptionPane.ERROR_MESSAGE);
}
```

This creates a clean hierarchy:
- Business Rule Violations: Informative messages with title "Booking Failed" or "Authentication Error".
- Technical Database Errors: Detailed SQL messages with title "Database Error".
- Input Parsing Errors: Helpful formatting tips with title "Input Error".


================================================================================


# SECTION 10: DESIGN DECISIONS ("WHY WE DID IT LIKE THAT")
=========================================================

Below are 2-4 sentence viva justifications for every major design choice in this application:

### 1. Separate Tables for Stations, Slots, Bookings, and Payments
"We separated these entities to achieve 3NF normalization and eliminate update, insertion, and deletion anomalies. Stations and charging slots exist independently of whether reservations exist, and payments represent distinct financial transactions linked 1:1 to bookings. Combining them into fewer tables would cause massive data redundancy whenever station operating hours or pricing change."

### 2. Snapshot of `rate_per_hour` Stored in the `bookings` Table
"Storing `rate_per_hour` inside the `bookings` table creates an immutable financial audit trail. If station management updates slot rates in the future from ₹250 to ₹350, past bookings must retain the exact historical rate charged at the time of reservation. Dynamically looking up the current slot price would corrupt historical financial records and distort revenue reports."

### 3. Using Mock Payment Instead of a Real Payment Gateway
"This project is an academic DBMS demonstration focused on ACID transactions, database integrity, and concurrency control. Integrating an external gateway like Razorpay or Stripe would introduce third-party network latency, webhooks, and API keys irrelevant to core DBMS evaluation. A mock payment allows complete, deterministic simulation of PAID and REFUNDED transactional states entirely within MySQL."

### 4. Layered Architecture with the DAO (Data Access Object) Pattern
"The DAO layer isolates all JDBC SQL queries away from the UI and Service layers. This separation of concerns ensures that business validation rules in `BookingService` can be modified or tested independently of SQL persistence code. It also prevents SQL logic from cluttering Swing event listener callbacks."

### 5. Using Raw JDBC Instead of an ORM (like Hibernate or JPA)
"For a university DBMS mini-project, raw JDBC provides total transparency and direct control over SQL query formulation, explicit transaction boundaries (`setAutoCommit`, `commit`, `rollback`), and pessimistic locking (`FOR UPDATE`). An ORM abstracts away and obscures the very database mechanics being evaluated, such as stored procedures, triggers, and execution plans."

### 6. PBKDF2WithHmacSHA256 with 600,000 Iterations Over MD5 or SHA-256
"Fast cryptographic hash functions like MD5 or plain SHA-256 can be brute-forced at billions of attempts per second using consumer GPUs. PBKDF2 with 600,000 iterations introduces deliberate key-stretching computation recommended by OWASP, and using a 16-byte random salt completely neutralizes Rainbow Table precomputation attacks."

### 7. Database-Enforced Unique Constraint on Active Bookings (`uq_active_slot_start`)
"We implemented a STORED generated column `active_start_time` that is non-null only when status is `CONFIRMED`. MySQL unique indexes permit multiple NULLs for cancelled bookings but enforce uniqueness for confirmed ones. This guarantees database-level defense-in-depth against double-booking even if application checks fail."

### 8. Database Stored Procedure (`sp_station_revenue`) for Reporting
"Analytics and aggregation over thousands of booking and payment records belong on the database engine, not in Java client memory. Executing `sp_station_revenue` aggregates revenue, paid counts, and refunds directly within MySQL in a single round-trip, minimizing network bandwidth and client-side processing overhead."

### 9. Database Trigger (`trg_booking_cancel_payment`) for Auto-Refunds
"The trigger ensures schema-enforced atomicity: whenever a booking's status transitions from `CONFIRMED` to `CANCELLED`, the associated payment status automatically switches from `PAID` to `REFUNDED`. Because the trigger lives in the database engine, this business rule is guaranteed even if a booking status is updated directly through MySQL Workbench or an external script."

### 10. `java.time` (LocalDateTime, LocalTime) Instead of Legacy `java.util.Date`
"Modern Java 8+ `java.time` classes are immutable and thread-safe, unlike legacy `java.util.Date` and `SimpleDateFormat` which suffer from concurrency bugs and confusing month indexing. `LocalDateTime` and `LocalTime` make hour alignment checks and opening/closing comparisons clean, explicit, and readable."


================================================================================


# SECTION 11: SPEC vs ACTUAL CODE
===============================

The file `EV_Charging_Slot_Booking_SDLC_SPEC .md` in the root directory represented the original architectural design specification.
Below is a detailed, truthful comparison of what was planned in the specification versus what is ACTUALLY implemented in the codebase.

## 1. Feature-by-Feature Comparison Matrix

| Feature / Requirement | In SDLC Spec | In Actual Code | Status & Details |
|---|---|---|---|
| Database Schema (5 Tables) | users, ev_stations, charging_slots, bookings, payments | users, ev_stations, charging_slots, bookings, payments | EXACT MATCH: Same tables, column names, constraints, and data types |
| Stored Function | fn_booking_hours(start, end) | fn_booking_hours(start, end) in dbcode.txt | EXACT MATCH: Deterministic function calculating hours rounded to 2 decimals |
| Stored Procedure | sp_station_revenue(station_id, from, to) | sp_station_revenue in dbcode.txt & called in AdminService | EXACT MATCH: Multi-table aggregation returning paid count, revenue, refunds, net revenue |
| Database Trigger | trg_booking_cancel_payment (AFTER UPDATE on bookings) | trg_booking_cancel_payment in dbcode.txt | EXACT MATCH: Automatically updates payment status to REFUNDED when booking cancelled |
| Concurrency Protection | SELECT ... FOR UPDATE + overlap query + generated unique column | Implemented in BookingDAO.java & dbcode.txt | EXACT MATCH: Full pessimistic locking and database constraint defense-in-depth |
| Password Hashing | PBKDF2WithHmacSHA256, 600,000 iterations, 16-byte salt | PasswordHasher.java | EXACT MATCH: Same iterations, salt size, and modular string format |
| Customer Registration & Login | Email regex, password length, status check | AuthService.java, LoginFrame.java, RegisterFrame.java | EXACT MATCH: Working registration with validation and authentication |
| Station & Slot Search | Filter by name and city, view slots | CustomerDashboardFrame.java, StationDAO.java, SlotDAO.java | EXACT MATCH: Interactive search and dual-table master-detail view |
| Booking & Mock Payment | Hour-aligned booking, 1-hr duration, mock transaction | BookingService.java, BookingDAO.java | EXACT MATCH: Atomic multi-table insertion in single transaction |
| Booking Cancellation | Cancel confirmed bookings, auto refund | CustomerDashboardFrame.java lines 234-260 | IMPLEMENTED WITH VARIATION (see below) |
| Admin Revenue Reports | Call sp_station_revenue and render analytics | AdminDashboardFrame.java & AdminService.java | EXACT MATCH: Renders formatted summary report |
| Admin Station Status Toggle | Toggle ACTIVE / MAINTENANCE | AdminDashboardFrame.java & AdminService.java | EXACT MATCH: Buttons to set Active or Maintenance status |
| Admin Create Station/Slot UI | Full CRUD forms for creating stations and slots | Methods exist in AdminService & DAOs, but NO Swing UI forms | PARTIALLY IMPLEMENTED (Backend methods exist; UI forms omitted) |
| Auto-Complete Past Bookings (FR-11) | Background or on-load update of past bookings to COMPLETED | Not implemented in code | NOT IMPLEMENTED (Seed data has COMPLETED, but no Java logic updates past bookings) |

---

## 2. Structural and Architectural Differences

### A. Directory Structure
- Spec expectation: Standard Maven directory hierarchy (`src/main/java/com/evcharging/...`) and SQL scripts located in `database/01_schema.sql`.
- Actual implementation: Flat source organization in the project root (`dao/`, `model/`, `service/`, `ui/`, `util/`, `security/`, `exception/`, `Main.java`).
  The Maven `pom.xml` configures `<sourceDirectory>${project.basedir}</sourceDirectory>` to compile directly from the root.
- All SQL definitions (DDL, indexes, seed data, function, procedure, trigger) are consolidated in a single root file: `dbcode.txt`.

### B. Architecture: Controller Layer
- Spec expectation: Specified a strict 6-layer architecture: `Swing UI → Controller → Service → DAO → JDBC → MySQL`.
- Actual implementation: There is NO separate `controller/` package. The Swing UI frames (`CustomerDashboardFrame`, `AdminDashboardFrame`, `LoginFrame`, `RegisterFrame`) invoke Service classes (`BookingService`, `AuthService`, `AdminService`) and DAO classes directly inside event listener callbacks.

### C. Cancellation Implementation Pattern
- Spec expectation: Proposed a dedicated `cancelBooking(userId, bookingId)` method in `BookingService` that starts a transaction and updates both tables.
- Actual implementation: `CustomerDashboardFrame.java` (lines 251-255) executes an inline PreparedStatement directly: `UPDATE bookings SET status = 'CANCELLED' WHERE booking_id = ?`. Because the MySQL trigger `trg_booking_cancel_payment` exists in the database, updating the booking row automatically updates `payments.status` to `REFUNDED` at the schema level.


================================================================================


# SECTION 12: VIVA PREP (25 LIKELY QUESTIONS WITH MODEL ANSWERS)
=============================================================

These 25 questions cover Database Management Systems (DBMS), SQL, JDBC, Java Swing, Concurrency, and Security.
Each answer is concise, technically precise, and directly references this project's actual code and schema.

---

### CATEGORY 1: DATABASE DESIGN & SQL CONCEPTS

#### Q1: What is the difference between a Primary Key and a Foreign Key in your schema?
**Answer:** A Primary Key uniquely identifies each row in a table (e.g., `station_id` in `ev_stations` or `booking_id` in `bookings`). A Foreign Key is a column that references the primary key of another table to maintain referential integrity (e.g., `bookings.slot_id` references `charging_slots.slot_id`, and `charging_slots.station_id` references `ev_stations.station_id`). Our foreign keys use `ON UPDATE CASCADE` and `ON DELETE RESTRICT` to prevent orphan records.

#### Q2: How does your database schema satisfy 1NF, 2NF, and 3NF?
**Answer:**
- 1NF: All attributes are atomic (no comma-separated lists; single phone per user, single connector type per slot) and each table has a defined primary key.
- 2NF: The schema is in 1NF and contains no partial dependencies; in composite candidate keys like `(station_id, slot_number)` in `charging_slots`, all non-key attributes (`connector_type`, `hourly_rate`) depend on the full composite key.
- 3NF: The schema is in 2NF and contains no transitive dependencies (non-key attributes depending on other non-key attributes). For example, station location attributes belong to `ev_stations`, not `bookings`.

#### Q3: Why is `rate_per_hour` duplicated in the `bookings` table if it already exists in `charging_slots`? Is this a violation of 3NF?
**Answer:** No, it is not a normalization violation. Storing `rate_per_hour` in `bookings` is a deliberate financial snapshot representing the agreed transaction price at the time of booking. Slot rates may fluctuate in the future, but historical bookings must remain immutable for accounting integrity and audit trails.

#### Q4: What anomalies would occur if stations, slots, and bookings were combined into a single table?
**Answer:**
- Insertion Anomaly: We could not record a newly built station or slot without creating a dummy or NULL booking.
- Deletion Anomaly: Deleting the only booking at a station would accidentally wipe out the station's address and operating hours.
- Update Anomaly: Changing a station's operating hours would require updating hundreds of duplicate rows across all past bookings, risking data inconsistency.

#### Q5: What is the difference between your stored function and stored procedure in dbcode.txt?
**Answer:**
- Stored Function (`fn_booking_hours`): Takes two DATETIME inputs, calculates the duration in minutes via `TIMESTAMPDIFF`, divides by 60.0, and returns a single scalar `DECIMAL(5,2)` value. It is deterministic and can be used directly inside SQL SELECT statements.
- Stored Procedure (`sp_station_revenue`): Takes input parameters (`station_id`, `from_date`, `to_date`), performs a complex multi-table LEFT JOIN across 4 tables with conditional aggregation (`SUM(CASE WHEN...)`), and returns an entire result set. It is invoked via `CALL`.

#### Q6: Explain what your trigger `trg_booking_cancel_payment` does and why an AFTER UPDATE trigger was used.
**Answer:** The trigger activates `AFTER UPDATE ON bookings FOR EACH ROW`. When `NEW.status = 'CANCELLED'` and `OLD.status = 'CONFIRMED'`, it executes an internal `UPDATE payments SET status = 'REFUNDED' WHERE booking_id = NEW.booking_id AND status = 'PAID';`. An AFTER UPDATE trigger was chosen because we only want to refund the payment once the booking row status update has successfully occurred.

---

### CATEGORY 2: TRANSACTIONS & CONCURRENCY CONTROL

#### Q7: What are ACID properties and where are they demonstrated in your code?
**Answer:**
- Atomicity: In `BookingDAO.java`, inserting a booking and inserting a payment are grouped between `conn.setAutoCommit(false)` and `conn.commit()`. If either fails, `conn.rollback()` executes so neither is saved.
- Consistency: Schema CHECK constraints (`max_power_kw > 0`, `end_time > start_time`) and foreign keys are validated on every write.
- Isolation: MySQL InnoDB executes transactions with row-level locks (`SELECT ... FOR UPDATE`), ensuring concurrent bookings do not see uncommitted intermediate states.
- Durability: Once `conn.commit()` succeeds, the transaction is written to the InnoDB redo log and persisted on disk.

#### Q8: How does your code prevent two users from booking the same slot at the same time?
**Answer:** It prevents double-booking using a 3-tier defense:
1. Pessimistic Row Lock: `SELECT ... FROM charging_slots WHERE slot_id = ? FOR UPDATE` locks the slot row, serializing concurrent requests.
2. Overlap Check Query: `SELECT booking_id FROM bookings WHERE slot_id = ? AND status = 'CONFIRMED' AND start_time < ? AND end_time > ? FOR UPDATE` detects overlapping time windows.
3. Database Unique Constraint: A stored generated column `active_start_time` enforces `UNIQUE(slot_id, active_start_time)` at the MySQL storage engine level.

#### Q9: What happens if two booking intervals are adjacent (e.g., 10:00-11:00 and 11:00-12:00)? Does your overlap query reject them?
**Answer:** No, adjacent intervals are correctly accepted. The overlap condition is `start_time < new_end AND end_time > new_start`. If an existing booking is 10:00 to 11:00 and a new booking requests 11:00 to 12:00, `end_time > new_start` evaluates to `11:00 > 11:00`, which is false (strict inequality). Thus, zero conflict rows are returned and the booking succeeds.

#### Q10: Why is Java `synchronized` not sufficient for preventing double-booking in production?
**Answer:** Java `synchronized` only synchronizes threads within a single Java Virtual Machine (JVM). If two separate instances of the application run on different machines or processes, each JVM has its own distinct monitor lock. `synchronized` has zero effect across multiple JVMs. Concurrency control must be enforced at the shared database layer using transactions and row locks.

#### Q11: How does your unique index `uq_active_slot_start` allow multiple cancelled bookings for the same slot and time?
**Answer:** The unique index is on `(slot_id, active_start_time)`. `active_start_time` is defined as: `CASE WHEN status = 'CONFIRMED' THEN start_time ELSE NULL END`. When a booking is CANCELLED, `active_start_time` evaluates to NULL. Under SQL standards and MySQL InnoDB, NULLs are treated as distinct in unique indexes, so multiple NULL entries are permitted while only one non-NULL (`CONFIRMED`) entry is allowed per slot and time.

---

### CATEGORY 3: JDBC & ARCHITECTURE

#### Q12: What is JDBC and which driver are you using?
**Answer:** JDBC (Java Database Connectivity) is the standard Java API for connecting to relational databases. In this project, we use `com.mysql:mysql-connector-j:9.2.0`, configured in `pom.xml`, loaded via `Class.forName("com.mysql.cj.jdbc.Driver")` in `DBConnection.java`.

#### Q13: Why do you use `PreparedStatement` instead of `Statement` throughout the DAO layer?
**Answer:** Two primary reasons:
1. Security: `PreparedStatement` treats parameter values strictly as data rather than executable code, completely preventing SQL Injection attacks.
2. Performance: PreparedStatement pre-compiles the SQL query template on the database server, allowing the database to reuse the query execution plan across repeated executions with different parameters.

#### Q14: How do you retrieve the auto-generated primary key after an `INSERT` statement?
**Answer:** In `conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)`, we pass the flag `RETURN_GENERATED_KEYS`. After calling `stmt.executeUpdate()`, we call `stmt.getGeneratedKeys()`, which returns a `ResultSet` containing the auto-generated ID (e.g. `bookingId = rs.getLong(1)`).

#### Q15: How do you invoke a Stored Procedure from Java code in this project?
**Answer:** In `AdminService.java` (lines 69-78), we use `java.sql.CallableStatement`. We prepare the call with `{CALL sp_station_revenue(?, ?, ?)}`, bind the input parameters via `stmt.setLong()` and `stmt.setTimestamp()`, and execute it using `stmt.executeQuery()`, processing the returned `ResultSet`.

#### Q16: What is the DAO pattern and why is it used in your project?
**Answer:** The DAO (Data Access Object) pattern isolates and encapsulates all database access and CRUD operations in dedicated classes (`UserDAO`, `StationDAO`, `SlotDAO`, `BookingDAO`, `PaymentDAO`). This decouples business logic (`BookingService`, `AuthService`) from low-level JDBC SQL queries, making the codebase modular, maintainable, and easier to unit test.

---

### CATEGORY 4: JAVA SWING & GUI CONCEPTS

#### Q17: What is the Event Dispatch Thread (EDT) in Java Swing?
**Answer:** Swing is single-threaded; all UI component creation, layout rendering, and event handling must take place on a dedicated thread called the Event Dispatch Thread (EDT). In `Main.java`, we invoke `SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true))` to guarantee that GUI initialization runs safely on the EDT.

#### Q18: How do you display tabular data and prevent users from editing cells in your tables?
**Answer:** We use `JTable` paired with `DefaultTableModel`. To make cells read-only, we override the `isCellEditable(int row, int col)` method in an anonymous inner class: `new DefaultTableModel(...) { @Override public boolean isCellEditable(int r, int c) { return false; } }`.

#### Q19: How are master-detail views implemented between stations and slots in `CustomerDashboardFrame`?
**Answer:** We register a `ListSelectionListener` on `stationTable.getSelectionModel()`. When the user selects a station row, the listener fires: if `!e.getValueIsAdjusting()`, it extracts the `stationId` from column 0 and calls `loadSlotsForStation(stationId)`, which queries `SlotDAO` and repopulates the lower `slotTable` dynamically.

#### Q20: What Layout Managers did you use and why?
**Answer:**
- `BorderLayout`: In dashboard panels, placing search bars in `NORTH`, split table panes in `CENTER`, and action buttons in `SOUTH`.
- `FlowLayout`: In button bars to align controls left or right with standard padding.
- `GridBagLayout`: In `LoginFrame` and `RegisterFrame` to achieve clean, centered label-and-input alignment in a 2-column grid.

---

### CATEGORY 5: SECURITY & PERFORMANCE

#### Q21: What password hashing algorithm does your project use and what are its parameters?
**Answer:** We use `PBKDF2WithHmacSHA256` in `PasswordHasher.java`. Its parameters are: 16-byte random salt generated by `SecureRandom`, 600,000 hashing iterations (OWASP standard), and a 256-bit derived key length. The output is stored as `pbkdf2_sha256$600000$<salt>$<hash>`.

#### Q22: What is a Salt in cryptography and why is it essential?
**Answer:** A salt is a cryptographically random sequence of bytes added to the plaintext password before hashing. It ensures that two users with identical passwords have completely different stored hashes, and it completely prevents attackers from using precomputed Rainbow Tables to look up passwords.

#### Q23: Why do you use `MessageDigest.isEqual()` when comparing password hashes?
**Answer:** Standard `String.equals()` terminates on the first non-matching character, leaking execution time information that attackers can exploit via a Timing Attack. `MessageDigest.isEqual()` performs a constant-time comparison, taking the exact same amount of time regardless of how many bytes match.

#### Q24: What database indexes were added in `dbcode.txt` and why?
**Answer:**
- `idx_slots_station_status ON charging_slots(station_id, status)`: Accelerates filtering slots by station and active state.
- `idx_bookings_slot_time_status ON bookings(slot_id, start_time, end_time, status)`: Directly speeds up the overlap check query during concurrent bookings.
- `idx_bookings_user_time ON bookings(user_id, start_time)`: Speeds up customer booking history lookup.
- `idx_payments_status_date ON payments(status, paid_at)`: Optimizes revenue report aggregation in `sp_station_revenue`.

#### Q25: How does your system handle database credentials securely?
**Answer:** In `AppConfig.java`, credentials are read from environment variables `DB_URL`, `DB_USER`, and `DB_PASSWORD` via `System.getenv().getOrDefault(...)`. This prevents sensitive passwords from ever being hardcoded into source code or tracked in version control.


================================================================================


# SECTION 13: HOW TO DEMO
==========================

This section provides an exact, step-by-step demonstration script for a viva or practical exam.

## 1. Demo Prerequisites & Launch

1. Ensure MySQL is running and database is seeded:
   ```bash
   mysql -u root -p < dbcode.txt
   ```
2. Build and run the desktop application from the project root:
   ```powershell
   mvn clean compile exec:java
   ```
   (The Login Window will appear centered on screen)

---

## 2. Step-by-Step Demo Flow

### STEP 1: Demonstrate Customer Registration & Validation
1. On the Login screen, click "Register".
2. Show validation error handling: Enter an invalid email (e.g. `bademail`) and short password (`123`). Click "Register".
   -> UI shows: "Valid email is required."
3. Enter valid details:
   - First Name: Viva
   - Last Name: Tester
   - Email: viva.tester@example.com
   - Phone: 9876543210
   - Password: password123
   - Confirm Password: password123
4. Click "Register". A message confirms: "Registration successful! Please login."
5. Point out to examiner: "The password was hashed using PBKDF2WithHmacSHA256 with 600,000 iterations before reaching MySQL."

### STEP 2: Customer Login & Master-Detail Station Browsing
1. Log in with `viva.tester@example.com` / `password123`.
2. The Customer Dashboard opens with tabs: "Find & Book Charger" and "My Bookings History".
3. In the search bar, type `Pune` in City and click "Search". Show that only Pune stations are listed.
4. Click on `GreenPlug Central Station` in the top table.
5. Notice that the lower table immediately updates to show the 4 charging slots for GreenPlug Central Station with their connector types (CCS2, TYPE2, etc.), max power in kW, and hourly rates.

### STEP 3: Successful Slot Booking & Mock Payment
1. Select Slot 1 (CCS2, Rate ₹250.00).
2. Set the Start Time to tomorrow at an exact hour, e.g., `2026-10-20 14:00`.
3. Click "Confirm Booking".
4. A dialog pops up: "Booking confirmed successfully! Booking ID: <id>".
5. Switch to "My Bookings History" tab. Show that the new booking appears at the top with status `CONFIRMED`.
6. Explain to examiner: "Under the hood, `BookingDAO.bookSlotTransactional` executed `SELECT ... FOR UPDATE`, confirmed no overlaps, created the booking row, and created the mock payment row inside one single ACID transaction."

### STEP 4: Demonstrate Double-Booking Prevention
1. Go back to "Find & Book Charger" tab.
2. Keep Slot 1 selected, with the EXACT SAME start time (`2026-10-20 14:00`).
3. Click "Confirm Booking" again.
4. A red error dialog pops up immediately:
   "Booking Failed: The selected time slot has already been booked by another user."
5. Explain to examiner: "The overlap check query detected the conflicting interval and threw a `BookingConflictException`, causing an immediate database rollback. No duplicate row was inserted."

### STEP 5: Demonstrate Booking Cancellation & Trigger Auto-Refund
1. In "My Bookings History", select the confirmed booking you just created.
2. Click "Cancel Selected Booking". Click "Yes" on the confirmation prompt.
3. A message displays: "Booking cancelled successfully. Payment will be marked as REFUNDED."
4. The table refreshes: status is now `CANCELLED`.
5. Open MySQL terminal/workbench and run: `SELECT * FROM payments WHERE booking_id = <id>;`
6. Point out: The payment status is now `REFUNDED`! Explain: "We did not update the payments table in Java code. The database trigger `trg_booking_cancel_payment` executed automatically inside MySQL upon the booking status update."

### STEP 6: Demonstrate Admin Control Center & Stored Procedure
1. Close the customer dashboard. Run the app again.
2. Log in using the Admin account:
   - Email: `admin@evcharge.com`
   (Note: in seed data, password hash is a dummy value; you can also register an admin or update its hash in MySQL)
3. In `AdminDashboardFrame`, show Tab 1 ("Station Management"):
   - Select a station and click "Set Maintenance". Show the status update in the table.
   - Click "Set Active" to restore it.
4. Switch to Tab 2 ("Revenue Analytics"):
   - Select Station 1 (`GreenPlug Central Station`) in the table.
   - Click "Generate Revenue Report".
   - An ASCII report generates instantly in the text area:
     ```text
     ====================================================
                STATION REVENUE REPORT                   
     ====================================================
      Station ID:            1
      Station Name:          GreenPlug Central Station
      Paid Bookings Count:   2
     ----------------------------------------------------
      Total Gross Revenue:  ₹ 500.00
      Total Refunds Issued: ₹ 0.00
     ----------------------------------------------------
      NET REVENUE:          ₹ 500.00
     ====================================================
     ```
5. Explain to examiner: "This report was computed by invoking the stored procedure `sp_station_revenue` via `CallableStatement`. It performed a 4-table join and conditional aggregation entirely within the MySQL database engine."

---

## 3. Which Files to Open and Highlight During the Viva

| When the examiner asks about... | Open this file | Highlight these lines |
|---|---|---|
| Database schema & constraints | `dbcode.txt` | Lines 52-77 (`bookings` table, generated column, unique constraint) |
| Stored procedure & trigger | `dbcode.txt` | Lines 178-220 (`sp_station_revenue` and `trg_booking_cancel_payment`) |
| Concurrency & Row Locking | `dao/BookingDAO.java` | Lines 15-27 (SQL queries with `FOR UPDATE`) and lines 30-88 (`setAutoCommit(false)`, `commit`, `rollback`) |
| Business Validation | `service/BookingService.java` | Lines 43-80 (future check, minute 00 check, operating hours check) |
| Password Hashing & Salt | `security/PasswordHasher.java` | Lines 13-27 (PBKDF2WithHmacSHA256, 600k iterations, SecureRandom salt) |
| Stored Procedure Call | `service/AdminService.java` | Lines 66-89 (`CallableStatement` with `{CALL sp_station_revenue(?, ?, ?)}`) |
| Database Credentials | `util/AppConfig.java` | Lines 4-17 (`System.getenv().getOrDefault` for DB_URL, DB_USER, DB_PASSWORD) |
| GUI Master-Detail Table | `ui/CustomerDashboardFrame.java` | Lines 131-136 (`ListSelectionListener` loading slots for selected station) |

================================================================================

End of PROJECT_EXPLAINED.
