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
 *
*/

metadata {
  definition(name: "Ecowitt WiFi Gateway", namespace: "mircolino", author: "Mirco Caramori") {
    capability "Sensor";

    // Gateway info
    attribute "model", "string";     // Model number
    attribute "firmware", "string";  // Firmware version
    attribute "rf", "string";        // Sensors radio frequency
    attribute "time", "string";      // Time last data was posted
    attribute "passkey", "string";   // PASSKEY

    // Weather Sensor (WH32 & WH31)  
    attribute "wh32_b", "string";    // Internal/Indoor
    attribute "wh32_e", "string";    // Outdoor
    attribute "wh31_ch1", "string";  // CH1
    attribute "wh31_ch2", "string";  // CH2
    attribute "wh31_ch3", "string";  // CH3
    attribute "wh31_ch4", "string";  // CH4
    attribute "wh31_ch5", "string";  // CH5
    attribute "wh31_ch6", "string";  // CH6
    attribute "wh31_ch7", "string";  // CH7
    attribute "wh31_ch8", "string";  // CH8

    // Rain Gauge Sensor (WH40)
    attribute "wh40", "string";

    // Air Quality Sensor (WH41 & WH43)
    attribute "wh41_ch1", "string";  // CH1
    attribute "wh41_ch2", "string";  // CH2
    attribute "wh41_ch3", "string";  // CH3
    attribute "wh41_ch4", "string";  // CH4

    // Soil Moisture Sensor (WH51)
    attribute "wh51_ch1", "string";  // CH1
    attribute "wh51_ch2", "string";  // CH2
    attribute "wh51_ch3", "string";  // CH3
    attribute "wh51_ch4", "string";  // CH4
    attribute "wh51_ch5", "string";  // CH5
    attribute "wh51_ch6", "string";  // CH6
    attribute "wh51_ch7", "string";  // CH7
    attribute "wh51_ch8", "string";  // CH8

    // Wind & Light Sensor (WS80 & WS68)
    attribute "ws80", "string";
  }

  preferences {
    input(name: "macAddress", type: "string", title: "<font style='font-size:12px; color:#1a77c9'>MAC Address</font>", description: "<font style='font-size:12px; font-style: italic'>Ecowitt WiFi Gateway MAC address</font>", defaultValue: "", required: true);
    input(name: "logLevel", type: "enum", title: "<font style='font-size:12px; color:#1a77c9'>Log Verbosity</font>", description: "<font style='font-size:12px; font-style: italic'>Default: 'Debug' for 30 min and 'Info' thereafter</font>", options: [0:"Error", 1:"Warning", 2:"Info", 3:"Debug"], multiple: false, defaultValue: 3, required: true);
  }
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

  if (mac) device.deviceNetworkId = mac;  
  else logError("The MAC address entered in the driver preferences is invalid");
}

// Logging --------------------------------------------------------------------------------------------------------------------

int getLogLevel() {
  //
  // Get the log level as an Integer:
  //
  //   0) log only Errors
  //   1) log Errors and Warnings
  //   2) log Errors, Warnings and Info
  //   3) log Errors, Warnings, Info and Debug (everything)
  //
  // If the level is not yet set in the driver preferences, return a default of 2 (Info)
  // Declared public because it's being used by the child-devices as well
  //
  if (settings.logLevel != null) return (settings.logLevel as Integer);
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

// ------------------------------------------------------------

private void logData(Map data) {
  //
  // Log all data received from the Ecowitt gateway
  // Used only for diagnostic/debug purposes
  //
  data.each {
    logDebug("$it.key = $it.value");
  }
}

// Sensor handling ------------------------------------------------------------------------------------------------------------

private String addSensor(String type, String model) {
  String dni = model.toUpperCase();
  try {
    logDebug("addSensor(${dni})");

    addChildDevice(type, dni, [name: "${type} ${dni}"]);
    sendEvent(name: model, value: dni);
  }
  catch (Exception e) {
    logError("Exception in addSensor(): ${e}");
    dni = null;
  }

  return (dni);
}

// State handling --------------------------------------------------------------------------------------------------------------

private void updateStates(Map data) {
  //
  // Dispatch parent/childs state changes to hub
  //
  String model;

  data.each {
    switch (it.key) {
    //
    // Gateway states
    //
    case "model":
      // Eg: model = GW1000_Pro
      if (state.model != it.value) sendEvent(name: "model", value: it.value);
      break;

    case "stationtype":
      // Eg: firmware = GW1000B_V1.5.7
      if (state.firmware != it.value) sendEvent(name: "firmware", value: it.value);
      break;

    case "freq":
      // Eg: rf = 915M
      if (state.rf != it.value) sendEvent(name: "rf", value: it.value);
      break;

    case "dateutc":
      // Eg: time = 2020-04-25+05:03:56
      if (state.time != it.value) sendEvent(name: "time", value: it.value);
      break;

    case "PASSKEY":
      // Eg: passkey = 15CF2C872932F570B34AC469540099A4
      if (state.passkey != it.value) sendEvent(name: "passkey", value: it.value);
      break;

    //
    // Integrated/Indoor Weather Sensor (WH32B)
    //
    case "wh25batt":
    case "tempinf":
    case "humidityin":
    case "baromrelin":
    case "baromabsin":
      model = "wh32_b";
      if (state."${model}" == null) state."${model}" = addSensor("Ecowitt RF Sensor", model);
      if (state."${model}") getChildDevice(state."${model}").updateStates(it.key, it.value);
      break;

    //
    // Outdoor Weather Sensor (WH32E)
    //
    case "wh26batt":
    case "tempf":
    case "humidity":
      model = "wh32_e";
      if (state."${model}" == null) state."${model}" = addSensor("Ecowitt RF Sensor", model);
      if (state."${model}") getChildDevice(state."${model}").updateStates(it.key, it.value);
      break;

    //
    // Multi-channel Weather Sensor (WH31)
    //
    case ~/batt([1-8])/:
    case ~/temp([1-8])f/:
    case ~/humidity([1-8])/:
      model = "wh31_ch${java.util.regex.Matcher.lastMatcher.group(1)}";
      if (state."${model}" == null) state."${model}" = addSensor("Ecowitt RF Sensor", model);
      if (state."${model}") getChildDevice(state."${model}").updateStates(it.key, it.value);
      break;

    //
    // Rain Gauge Sensor (WH40)
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
      model = "wh40";
      if (state."${model}" == null) state."${model}" = addSensor("Ecowitt RF Sensor", model);
      if (state."${model}") getChildDevice(state."${model}").updateStates(it.key, it.value);
      break;

    //
    // Multi-channel  Air Quality Sensor (WH41 / WH43)
    //
    case ~/pm25batt([1-4])/:
    case ~/pm25_ch([1-4])/:
    case ~/pm25_avg_24h_ch([1-4])/:
      model = "wh41_ch${java.util.regex.Matcher.lastMatcher.group(1)}";
      if (state."${model}" == null) state."${model}" = addSensor("Ecowitt RF Sensor", model);
      if (state."${model}") getChildDevice(state."${model}").updateStates(it.key, it.value);
      break;

    //
    // Multi-channel Soil Moisture Sensor (WH51)
    //
    case ~/soilbatt([1-8])/:
    case ~/soilmoisture([1-8])/:
      model = "wh51_ch${java.util.regex.Matcher.lastMatcher.group(1)}";
      if (state."${model}" == null) state."${model}" = addSensor("Ecowitt RF Sensor", model);
      if (state."${model}") getChildDevice(state."${model}").updateStates(it.key, it.value);
      break;

    //
    // Wind & Solar Sensor (WS68 / WS80)
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
      model = "ws80";
      if (state."${model}" == null) state."${model}" = addSensor("Ecowitt RF Sensor", model);
      if (state."${model}") getChildDevice(state."${model}").updateStates(it.key, it.value);
      break;

    default:
      logDebug("Unrecognized state: ${it.key} = ${it.value}");
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

    // Unschedule possible previous runIn() calls
    unschedule();

    // Update Device Network ID
    updateDNI();
  
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

    // logData(data);
    updateStates(data);

  }
  catch (Exception e) {
    logError("Exception in parse(): ${e}");
  }
}

// EOF ------------------------------------------------------------------------------------------------------------------------
