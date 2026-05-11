# SJSU Campus Weather Dashboard

The SJSU Campus Weather Dashboard is a JavaFX desktop application designed to provide real-time campus weather monitoring, historical trend analysis, and short-term temperature forecasting for San Jose State University. The system retrieves weather data from the Duncan Hall Roof weather station operated by the SJSU Meteorology department, stores the data locally in a CSV file, and displays it through a graphical dashboard.

---

### Installing Maven

**macOS (Homebrew)**
```bash
brew install maven
```

**Ubuntu / Debian**
```bash
sudo apt update && sudo apt install maven
```

**Windows (Chocolatey)**
```powershell
choco install maven
```

**Verify the installation**
```bash
mvn -version
```

---

## Running Tests

Run the full test suite:

```bash
mvn test
```

Run a specific test class:

```bash
mvn test -Dtest="add_class_name"
```

---

## Compiling the Application

Compile the source code without running tests:

```bash
mvn compile
```

Run the Application : 
```bash
mvn exec:java -Dexec.mainClass="com.weather.app.Main"
```

