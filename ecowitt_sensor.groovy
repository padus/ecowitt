/**
 * Driver:     Ecowitt Weather Sensor 
 * Author:     Mirco Caramori
 * Repository: https://github.com/mircolino/ecowitt
 * Import URL: https://raw.githubusercontent.com/mircolino/ecowitt/master/ecowitt_sensor.groovy
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
 * Change Log: shared with ecowitt_gateway.groovy
*/

metadata {
  definition(name: "Ecowitt RF Sensor", namespace: "mircolino", author: "Mirco Caramori") {
    capability "Sensor";

    capability "Battery"; 
    capability "Temperature Measurement";
    capability "Relative Humidity Measurement";
    capability "Pressure Measurement";
    capability "UltravioletIndex";
    capability "IlluminanceMeasurement";

 // attribute "battery", "number";                             // 0-100%
 // attribute "temperature", "number";                         // °F
 // attribute "humidity", "number";                            // 0-100%
 // attribute "pressure", "number";                            // inHg - relative pressure corrected to sea-level
    attribute "pressureAbs", "number";                         // inHg - absolute pressure

    attribute "rainRate", "number";                            // in/h - rainfall rate 
    attribute "rainEvent", "number";                           // in - rainfall in the current event
    attribute "rainHourly", "number";                          // in - rainfall in the current hour
    attribute "rainDaily", "number";                           // in - rainfall in the current day
    attribute "rainWeekly", "number";                          // in - rainfall in the current week
    attribute "rainMonthly", "number";                         // in - rainfall in the current month
    attribute "rainYearly", "number";                          // in - rainfall in the current year
    attribute "rainTotal", "number";                           // in - rainfall total since sensor installation

    attribute "pm25", "number";                                // µg/cm³ - current PM2.5 particle reading 
    attribute "pm25_avg_24h", "number";                        // µg/cm³ - average PM2.5 particle reading over the last 24 hours

 // attribute "ultravioletIndex", "number";                    // 0-11+ UV Index
 // attribute "illuminance", "number";                         // lux
    attribute "windDirection", "number";                       // 0-359°
    attribute "windDirection_avg_10m", "number";               // 0-359° - average over the last 10 minutes 
    attribute "windSpeed", "number";                           // mph
    attribute "windSpeed_avg_10m", "number";                   // mph - average over the last 10 minutes
    attribute "windGust", "number";                            // mph
    attribute "windGustMaxDaily", "number";                    // mph - max in the current day  
  }
}

// Logging --------------------------------------------------------------------------------------------------------------------

private void logError(String str) { log.error(str); }
private void logWarning(String str) { if (getParent().getLogLevel() > 0) log.warn(str); }
private void logInfo(String str) { if (getParent().getLogLevel() > 1) log.info(str); }
private void logDebug(String str) { if (getParent().getLogLevel() > 2) log.debug(str); }
private void logTrace(String str) { if (getParent().getLogLevel() > 3) log.trace(str); }

// Conversion -----------------------------------------------------------------------------------------------------------------

private Map convertTemperature(String fahrenheit) {
  
  Map conv = [:];

  if (getParent().isSystemMetric()) {
    Float celsius = ((fahrenheit as Float) - 32.0f) / 1.8f;
    celsius = celsius.round(2);

    conv.value = celsius.toString();
    conv.unit = "°C";
  }
  else {
    conv.value = fahrenheit;
    conv.unit = "°F";
  }
  
  return (conv);
}

// ------------------------------------------------------------

private Map convertPressure(String inch) {
  
  Map conv = [:];

  if (getParent().isSystemMetric()) {
    Float millimeter = (inch as Float) * 25.4f;
    millimeter = millimeter.round(2);

    conv.value = millimeter.toString();
    conv.unit = "mmHg";
  }
  else {
    conv.value = inch;
    conv.unit = "inHg";
  }
  
  return (conv);
}

// ------------------------------------------------------------

private Map convertRain(String inch, Boolean hour = false) {
  
  Map conv = [:];

  if (getParent().isSystemMetric()) {
    Float millimeter = (inch as Float) * 25.4f;
    millimeter = millimeter.round(2);

    conv.value = millimeter.toString();
    conv.unit = hour? "mm/h": "mm";
  }
  else {
    conv.value = inch;
    conv.unit = hour? "in/h": "in";
  }
  
  return (conv);
}

// ------------------------------------------------------------

private Map convertWind(String mile) {
  
  Map conv = [:];

  if (getParent().isSystemMetric()) {
    Float kilometer = (mile as Float) * 1.609344f;
    kilometer = kilometer.round(2);

    conv.value = kilometer.toString();
    conv.unit = "km/h";
  }
  else {
    conv.value = mile;
    conv.unit = "mph";
  }
  
  return (conv);
}

// State handling -------------------------------------------------------------------------------------------------------------

void updateStates(String key, String val) {
  //
  // Dispatch state changes to hub
  //
  Integer percent;
  Map convert;

  switch (key) {

  case ~/soilbatt[1-8]/:
    // The soil moisture sensor returns the battery voltage which, for regular AA alkaline, ranges from 1.40V (empty) to 1.65V (full)
    percent = Math.round((val as Float) * 100f);
    if (percent < 140) percent = 140;
    else if (percent > 165) percent = 165;

    // Change range from (1.40V - 1.65V) to (0% - 100%)
    percent = ((percent - 140) * (100 - 0)) / (165 - 140);

    // Bring back to string
    val = percent.toString();
    if (state.battery != val) sendEvent(name: "battery", value: val, unit: "%");
    break;
  
  case ~/pm25batt[1-4]/:
    // The air quality sensor returns a battery value between 0 (empty) and 5 (full)
    percent = (val as Integer) * 20;
    if (percent > 100) percent = 100;
    
    // Bring back to string
    val = percent.toString();
    if (state.battery != val) sendEvent(name: "battery", value: val, unit: "%");
    break;
  
  case "wh25batt":
  case "wh26batt":
  case ~/batt[1-8]/:
  case "wh40batt": 
  case "wh65batt":
    // It seems like most sensors returns a battery value of either 0 (full) or 1 (empty)
    val = (val == "0")? "100": "0";
    if (state.battery != val) sendEvent(name: "battery", value: val, unit: "%");
    break;
  
  case "tempinf":
  case "tempf":
  case ~/temp[1-8]f/:
    convert = convertTemperature(val);
    if (state.temperature != convert.value) sendEvent(name: "temperature", value: convert.value, unit: convert.unit);
    break;

  case "humidityin":
  case "humidity":
  case ~/humidity[1-8]/:
  case ~/soilmoisture[1-8]/:
    if (state.humidity != val) sendEvent(name: "humidity", value: val, unit: "%");
    break;

  case "baromrelin":
    convert = convertPressure(val);
    if (state.pressure != convert.value) sendEvent(name: "pressure", value: convert.value, unit: convert.unit);
    break;

  case "baromabsin":
    convert = convertPressure(val);
    if (state.pressureAbs != convert.value) sendEvent(name: "pressureAbs", value: convert.value, unit: convert.unit);
    break;

  case "rainratein":
    convert = convertRain(val, true);
    if (state.rainRate != convert.value) sendEvent(name: "rainRate", value: convert.value, unit: convert.unit);
    break;

  case "eventrainin":
    convert = convertRain(val);
    if (state.rainEvent != convert.value) sendEvent(name: "rainEvent", value: convert.value, unit: convert.unit);
    break;

  case "hourlyrainin":
    convert = convertRain(val);
    if (state.rainHourly != convert.value) sendEvent(name: "rainHourly", value: convert.value, unit: convert.unit);
    break;

  case "dailyrainin":
    convert = convertRain(val);
    if (state.rainDaily != convert.value) sendEvent(name: "rainDaily", value: convert.value, unit: convert.unit);
    break;

  case "weeklyrainin":
    convert = convertRain(val);
    if (state.rainWeekly != convert.value) sendEvent(name: "rainWeekly", value: convert.value, unit: convert.unit);
    break;

  case "monthlyrainin":
    convert = convertRain(val);
    if (state.rainMonthly != convert.value) sendEvent(name: "rainMonthly", value: convert.value, unit: convert.unit);
    break;

  case "yearlyrainin":
    convert = convertRain(val);
    if (state.rainYearly != convert.value) sendEvent(name: "rainYearly", value: convert.value, unit: convert.unit);
    break;

  case "totalrainin":
    convert = convertRain(val);
    if (state.rainTotal != convert.value) sendEvent(name: "rainTotal", value: convert.value, unit: convert.unit);
    break;

  case ~/pm25_ch[1-4]/:
    if (state.pm25 != val) sendEvent(name: "pm25", value: val, unit: "µg/cm³");
    break;

  case ~/pm25_avg_24h_ch[1-4]/:
    if (state.pm25_avg_24h != val) sendEvent(name: "pm25_avg_24h", value: val, unit: "µg/cm³");
    break;

  case "uv":
    if (state.ultravioletIndex != val) sendEvent(name: "ultravioletIndex", value: val, unit: "uvi");
    break;

  case "solarradiation":
    val = (val / 0.0079) as Long;
    if (state.illuminance != val) sendEvent(name: "illuminance", value: val, unit: "lux");
    break;

  case "winddir":
    if (state.windDirection != val) sendEvent(name: "windDirection", value: val, unit: "°");
    break;

  case "winddir_avg10m":
    if (state.windDirection_avg_10m != val) sendEvent(name: "windDirection_avg_10m", value: val, unit: "°");
    break;

  case "windspeedmph":
    convert = convertWind(val);
    if (state.windSpeed != convert.value) sendEvent(name: "windSpeed", value: convert.value, unit: convert.unit);
    break;

  case "windspdmph_avg10m":
    convert = convertWind(val);
    if (state.windSpeed_avg_10m != convert.value) sendEvent(name: "windSpeed_avg_10m", value: convert.value, unit: convert.unit);
    break;

  case "windgustmph":
    convert = convertWind(val);
    if (state.windGust != convert.value) sendEvent(name: "windGust", value: convert.value, unit: convert.unit);
    break;

  case "maxdailygust":
    convert = convertWind(val);
    if (state.windGustMaxDaily != convert.value) sendEvent(name: "windGustMaxDaily", value: convert.value, unit: convert.unit);
    break;
  }
}

// Driver lifecycle -----------------------------------------------------------------------------------------------------------

void installed() { 
  //
  // Called once when the driver is created
  //
  try {
    logDebug("installed(${device.deviceNetworkId})");
  }
  catch (Exception e) {
    logError("Exception in installed(${device.deviceNetworkId}): ${e}");
  }
}

// ------------------------------------------------------------

void updated() {
  //
  // Never called
  //
  try {
    logDebug("updated(${device.deviceNetworkId})");
  }
  catch (Exception e) {
    logError("Exception in updated(${device.deviceNetworkId}): ${e}");
  }
}

// ------------------------------------------------------------

void uninstalled() {
  //
  // Called once when the driver is deleted
  //
  try {
    logDebug("uninstalled(${device.deviceNetworkId})");

    // We are being deleted: notify the parent to remove our state (presence)
    getParent().deleteSensorState(device.deviceNetworkId);
  }
  catch (Exception e) {
    logError("Exception in uninstalled(${device.deviceNetworkId}): ${e}");
  }
}

// ------------------------------------------------------------

void parse(String msg) {
  //
  // Never called
  //
  try {
    logDebug("parse(${device.deviceNetworkId})");
  }
  catch (Exception e) {
    logError("Exception in parse(${device.deviceNetworkId}): ${e}");
  }
}

// EOF ------------------------------------------------------------------------------------------------------------------------
