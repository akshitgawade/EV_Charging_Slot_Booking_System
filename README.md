# EV Charging Slot Booking System

This project is a Java Swing + JDBC application for EV charging slot booking with MySQL.

## Prerequisites

- Java 17 or newer
- MySQL 8.0+
- Maven

## 1) Create the database

Start MySQL and then run:

```bash
mysql -u root -p < dbcode.txt
```

If you use a different username/password, set environment variables before running the app:

```bash
set DB_URL=jdbc:mysql://localhost:3306/ev_charging_system?useSSL=false&serverTimezone=Asia/Kolkata
set DB_USER=root
set DB_PASSWORD=your_password
```

## 2) Build and run

From the project root:

```bash
mvn clean compile
mvn exec:java
```

Or, if you want to run the class directly:

```bash
javac -cp "C:\Users\Nilima\.m2\repository\com\mysql\mysql-connector-j\9.2.0\mysql-connector-j-9.2.0.jar" -d out $(find . -name "*.java")
java -cp "out;C:\Users\Nilima\.m2\repository\com\mysql\mysql-connector-j\9.2.0\mysql-connector-j-9.2.0.jar" com.evcharging.Main
```

On Windows PowerShell use:

```powershell
$jar = "C:\Users\Nilima\.m2\repository\com\mysql\mysql-connector-j\9.2.0\mysql-connector-j-9.2.0.jar"
$src = Get-ChildItem -Path . -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -cp $jar -d out $src
java -cp "out;$jar" com.evcharging.Main
```

## 3) Default login

- Admin: admin@evcharge.com
- Customer examples: rahul.sharma@example.com, priya.patel@example.com
- Passwords are hashed in the database seed data; use the app's registration flow to create a new account if needed.

## Notes

- The app uses PBKDF2 password hashing.
- The DB schema, seed data, triggers, procedures, and functions are defined in [dbcode.txt](dbcode.txt).
