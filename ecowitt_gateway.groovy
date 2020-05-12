/**
 * Driver:     Ecowitt WiFi Gateway
 * Author:     Mirco Caramori
 * Repository: https://github.com/mircolino/ecowitt
 * Import URL: https://raw.githubusercontent.com/mircolino/ecowitt/master/ecowitt_gateway.groovy
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 *
 * Change Log:
 *
 * 2020.04.24 - Initial implementation
 * 2020.04.29 - Added GitHub versioning 
 *              Added support for more sensors: WH40, WH41, WH43, WS68 and WS80
 * 2020.04.29 - Added sensor battery range conversion to 0-100%
 * 2020.05.03 - Optimized state dispatch and removed unnecessary attributes
 * 2020.05.04 - Added metric/imperial unit conversion
 * 2020.05.05 - Gave child sensors a friendlier default name
 * 2020.05.08 - Further state optimization and release to stable
 * 2020.05.11 - HTML templates
 *              Normalization of floating values
*/

public static String version() { return "v1.1.10"; }

// Metadata -------------------------------------------------------------------------------------------------------------------

metadata {
  definition(name: "Ecowitt WiFi Gateway", namespace: "mircolino", author: "Mirco Caramori") {
    capability "Sensor";

    // Gateway info
    attribute "model", "string";     // Model number
    attribute "firmware", "string";  // Firmware version
    attribute "rf", "string";        // Sensors radio frequency
    attribute "time", "string";      // Time last data was posted
    attribute "passkey", "string";   // PASSKEY
  }

  preferences {
    input(name: "macAddress", type: "string", title: "<font style='font-size:12px; color:#1a77c9'>MAC Address</font>", description: "<font style='font-size:12px; font-style: italic'>Ecowitt WiFi Gateway MAC address</font>", defaultValue: "", required: true);
    input(name: "unitSystem", type: "enum", title: "<font style='font-size:12px; color:#1a77c9'>Units</font>", description: "<font style='font-size:12px; font-style: italic'>System all values are converted to</font>", options: [0:"Imperial", 1:"Metric"], multiple: false, defaultValue: 0, required: true);
    input(name: "logLevel", type: "enum", title: "<font style='font-size:12px; color:#1a77c9'>Log Verbosity</font>", description: "<font style='font-size:12px; font-style: italic'>Default: 'Debug' for 30 min and 'Info' thereafter</font>", options: [0:"Error", 1:"Warning", 2:"Info", 3:"Debug", 4:"Trace"], multiple: false, defaultValue: 3, required: true);
  }
}

/*
 * State variables used by the driver
 *
 * "driverVer" = "v1.0.3" // Current driver version
 * "driverNew" = "v1.0.5" // Latest driver version
 *
 */
 
 // Versioning -----------------------------------------------------------------------------------------------------------------

private Map extractVersion(String ver) {
  //
  // Given any version string (e.g. version 2.5.78-prerelease) will return a Map as following:
  //   Map.major version
  //   Map.minor version
  //   Map.build version
  //   Map.desc  version
  // or "null" if no version info was found in the given string
  //
  Map val = null;

  if (ver) {
    String pattern = /.*?(\d+)\.(\d+)\.(\d+).*/
    java.util.regex.Matcher matcher = ver =~ pattern;

    if (matcher.groupCount() == 3) {
      val = [:];
      val.major = matcher[0][1].toInteger();
      val.minor = matcher[0][2].toInteger();
      val.build = matcher[0][3].toInteger();
      val.desc = "v${val.major}.${val.minor}.${val.build}";
    }
  }

  return (val);
}

// ------------------------------------------------------------

void updateVersion() {
  Map verCur = null;
  Map verNew = null;

  try {
    logDebug("updateVersion()");
    
    // Retrieve current version
    verCur = extractVersion(version());
    if (verCur) {
      // Retrieve latest version from GitHub repository manifest
      // If the file is not found, it will throw an exception
      String manifestText = "https://raw.githubusercontent.com/mircolino/ecowitt/master/packageManifest.json".toURL().getText();
      if (manifestText) {
        // text -> json
        Object parser = new groovy.json.JsonSlurper();
        Object manifest = parser.parseText(manifestText);  

        verNew = extractVersion(manifest.version);
        if (verNew) {
          // Compare versions
          if (verCur.major > verNew.major) verNew = null;
          else if (verCur.major == verNew.major) {
            if (verCur.minor > verNew.minor) verNew = null;
            else if (verCur.minor == verNew.minor) {
              if (verCur.build >= verNew.build) verNew = null;
            }
          }
        }
      }
    }
  }
  catch (Exception e) {
    logError("Exception in updateVersion(): ${e}");
  }

  if (verCur) state.driverVer = verCur.desc;
  else state.remove("driverVer");

  if (verNew) state.driverNew = "<font style='color:#ff0000'>${verNew.desc}</font>";
  else state.remove("driverNew");
}

// MAC & DNI ------------------------------------------------------------------------------------------------------------------

private String getMacAddress() {
  //
  // Get the Ecowitt MAC address from the driver preferences and validate it
  // Return null is invalid
  //
  if (settings.macAddress != null) {
    String str = settings.macAddress;
    str = str.replaceAll("[^a-fA-F0-9]", "");
    if (str.length() == 12) return (str.toUpperCase());
  }

  return (null);
}

// ------------------------------------------------------------

private void updateDNI() {
  //
  // Get the Ecowitt MAC address and, if valid, update the driver DNI
  //
  String mac = getMacAddress();

  if (mac) device.setDeviceNetworkId(mac);  
  else logError("The MAC address entered in the driver preferences is invalid");
}

// Conversion -----------------------------------------------------------------------------------------------------------------

Boolean isSystemMetric() {
  //
  // Return true if the selected unit system is metric
  // Declared public because it's being used by the child-devices
  //
  if (settings.unitSystem != null && settings.unitSystem.toInteger() != 0) return (true);
  return (false);
}

// Logging --------------------------------------------------------------------------------------------------------------------

Integer getLogLevel() {
  //
  // Get the log level as an Integer:
  //
  //   0) log only Errors
  //   1) log Errors and Warnings
  //   2) log Errors, Warnings and Info
  //   3) log Errors, Warnings, Info and Debug
  //   4) log Errors, Warnings, Info, Debug and Trace/diagnostic (everything)
  //
  // If the level is not yet set in the driver preferences, return a default of 2 (Info)
  // Declared public because it's being used by the child-devices as well
  //
  if (settings.logLevel != null) return (settings.logLevel.toInteger());
  return (2);
}

// ------------------------------------------------------------

void logDebugOff() {
  //
  // runIn() callback to disable "Debug" logging after 30 minutes
  // Cannot be private
  //
  if (getLogLevel() > 2) device.updateSetting("logLevel", [type: "enum", value: "2"]);
}

// ------------------------------------------------------------

private void logError(String str) { log.error(str); }
private void logWarning(String str) { if (getLogLevel() > 0) log.warn(str); }
private void logInfo(String str) { if (getLogLevel() > 1) log.info(str); }
private void logDebug(String str) { if (getLogLevel() > 2) log.debug(str); }
private void logTrace(String str) { if (getLogLevel() > 3) log.trace(str); }

// ------------------------------------------------------------

private void logData(Map data) {
  //
  // Log all data received from the Ecowitt gateway
  // Used only for diagnostic/debug purposes
  //
  if (getLogLevel() > 3) {
    data.each {
      logTrace("$it.key = $it.value");
    }
  }
}

// Sensor handling ------------------------------------------------------------------------------------------------------------

private void addSensorAndOrUpdate(String name, String dni, String key, String value) {
  //
  // If not present, add the child sensor corresponding to the specified key
  // and, if child sensor is present, update the attribute
  //
  try {
    com.hubitat.app.ChildDeviceWrapper sensor = getChildDevice(dni);

    if (sensor == null) {
      // Sensor doesn't exist: we need to create it
      sensor = addChildDevice("Ecowitt RF Sensor", dni, [name: "${name}"]);
    }

    if (sensor) {
      // Sensor exists: update it
      sensor.updateAttribute(key, value);
    }
  }
  catch (Exception e) {
    logError("Exception in addSensor(${dni}): ${e}");
  }
}

// Attribute handling ---------------------------------------------------------------------------------------------------------

private void updateAttributes(Map data) {
  //
  // Dispatch parent/childs attribute changes to hub
  //
  String dni;
  String channel;

  data.each {
    switch (it.key) {
    //
    // Gateway attributes
    //
    case "model":
      // Eg: model = GW1000_Pro
      if (device.currentValue("model").toString() != it.value) sendEvent(name: "model", value: it.value);
      break;

    case "stationtype":
      // Eg: firmware = GW1000B_V1.5.7
      if (device.currentValue("firmware").toString() != it.value) sendEvent(name: "firmware", value: it.value);
      break;

    case "freq":
      // Eg: rf = 915M
      if (device.currentValue("rf").toString() != it.value) sendEvent(name: "rf", value: it.value);
      break;

    case "dateutc":
      // Eg: time = 2020-04-25+05:03:56
      if (device.currentValue("time").toString() != it.value) sendEvent(name: "time", value: it.value);
      break;

    case "PASSKEY":
      // Eg: passkey = 15CF2C872932F570B34AC469540099A4
      if (device.currentValue("passkey").toString() != it.value) sendEvent(name: "passkey", value: it.value);
      break;

    //
    // Integrated/Indoor Weather Sensor (WH32B)
    //
    case "wh25batt":
    case "tempinf":
    case "humidityin":
    case "baromrelin":
    case "baromabsin":
      addSensorAndOrUpdate("Ecowitt Indoor Weather Sensor", "WH32B", it.key, it.value);
      break;

    //
    // Outdoor Weather Sensor (WH32E)
    //
    case "wh26batt":
    case "tempf":
    case "humidity":
      addSensorAndOrUpdate("Ecowitt Outdoor Weather Sensor", "WH32E", it.key, it.value);
      break;

    //
    // Multi-channel Weather Sensor (WH31B)
    //
    case ~/batt([1-8])/:
    case ~/temp([1-8])f/:
    case ~/humidity([1-8])/:
      channel = java.util.regex.Matcher.lastMatcher.group(1); 
      addSensorAndOrUpdate("Ecowitt Weather Sensor ${channel}", "WH31B_CH${channel}", it.key, it.value);
      break;

    //
    // Rain Gauge Sensor (WH40, WH69E)
    //
    case "wh40batt": 
    case "rainratein":
    case "eventrainin":
    case "hourlyrainin":
    case "dailyrainin":
    case "weeklyrainin":
    case "monthlyrainin":
    case "yearlyrainin":
    case "totalrainin":
      addSensorAndOrUpdate("Ecowitt Rain Gauge Sensor", "WH40", it.key, it.value);
      break;

    //
    // Multi-channel Air Quality Sensor (WH41)
    //
    case ~/pm25batt([1-4])/:
    case ~/pm25_ch([1-4])/:
    case ~/pm25_avg_24h_ch([1-4])/:
      channel = java.util.regex.Matcher.lastMatcher.group(1); 
      addSensorAndOrUpdate("Ecowitt Air Quality Sensor ${channel}", "WH41_CH${channel}", it.key, it.value);
      break;

    //
    // Multi-channel Soil Moisture Sensor (WH51)
    //
    case ~/soilbatt([1-8])/:
    case ~/soilmoisture([1-8])/:
      channel = java.util.regex.Matcher.lastMatcher.group(1);
      addSensorAndOrUpdate("Ecowitt Soil Moisture Sensor ${channel}", "WH51_CH${channel}", it.key, it.value);
      break;

    //
    // Wind & Solar Sensor (WH80, WH69E)
    //
    case "wh65batt":
    case "winddir":
    case "winddir_avg10m":
    case "windspeedmph":
    case "windspdmph_avg10m":
    case "windgustmph":
    case "maxdailygust":
    case "uv":
    case "solarradiation":
      addSensorAndOrUpdate("Ecowitt Wind Solar Sensor", "WH80", it.key, it.value);
      break;

    default:
      logDebug("Unrecognized attribute: ${it.key} = ${it.value}");
      break;
    }
  }
}

// Driver lifecycle -----------------------------------------------------------------------------------------------------------

void installed() {
  //
  // Called once when the driver is created
  //
  try {
    logDebug("installed()");
  }
  catch (Exception e) {
    logError("Exception in installed(): ${e}");
  }
}

// ------------------------------------------------------------

void updated() {
  //
  // Called everytime the user saves the driver preferences
  //
  try {
    logDebug("updated()");

    // Clear previous states
    state.clear();

    // Unschedule possible previous runIn() calls
    unschedule();

    // Update Device Network ID
    updateDNI();
  
    // Update driver version now and every Sunday @ 2am
    updateVersion();
    schedule("0 0 2 ? * 1 *", updateVersion);

    // Turn off debug log in 30 minutes
    if (getLogLevel() > 2) runIn(1800, logDebugOff);
  }
  catch (Exception e) {
    logError("Exception in updated(): ${e}");
  }
}

// ------------------------------------------------------------

void uninstalled() {
  //
  // Called once when the driver is deleted
  //
  try {
    logDebug("uninstalled()");

    // Delete all children
    getChildDevices().each {
      deleteChildDevice(it.deviceNetworkId)
    }
  }
  catch (Exception e) {
    logError("Exception in uninstalled(): ${e}");
  }
}

// ------------------------------------------------------------

void parse(String msg) {
  //
  // Called everytime a POST message is received from the Ecowitt WiFi Gateway
  //
  try {
    logDebug("parse()");

    // Parse POST message
    Map data = parseLanMessage(msg);

    // Save only the body and discard the header
    String body = data["body"];

    // Build a map with one key/value pair for each field we receive
    data = [:];
    body.split("&").each {
      String[] keyValue = it.split("=");
      data[keyValue[0]] = keyValue[1];
    }

    logData(data);
    updateAttributes(data);

  }
  catch (Exception e) {
    logError("Exception in parse(): ${e}");
  }
}

// EOF ------------------------------------------------------------------------------------------------------------------------
