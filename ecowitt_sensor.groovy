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
 * Change Log:
 *
 * 2020.04.25 - Initial implementation
 *
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
private void logWarning(String str) { if (parent.getLogLevel() > 0) log.warn(str); }
private void logInfo(String str) { if (parent.getLogLevel() > 1) log.info(str); }
private void logDebug(String str) { if (parent.getLogLevel() > 2) log.debug(str); }

// State handling --------------------------------------------------------------------------------------------------------------

void updateStates(String key, String val) {
  //
  // Dispatch state changes to hub
  //
  switch (key) {

  case "wh25batt":
  case "wh26batt":
  case ~/batt[1-8]/:
  case "wh40batt": 
  case ~/pm25batt[1-4]/:
  case ~/soilbatt[1-8]/:
  case "wh65batt":
    if (state.battery != val) sendEvent(name: "battery", value: val, unit: "%");
    break;

  case "tempinf":
  case "tempf":
  case ~/temp[1-8]f/:
    if (state.temperature != val) sendEvent(name: "temperature", value: val, unit: "°F");
    break;

  case "humidityin":
  case "humidity":
  case ~/humidity[1-8]/:
  case ~/soilmoisture[1-8]/:
    if (state.humidity != val) sendEvent(name: "humidity", value: val, unit: "%");
    break;

  case "baromrelin":
    if (state.pressure != val) sendEvent(name: "pressure", value: val, unit: "inHg");
    break;

  case "baromabsin":
    if (state.pressureAbs != val) sendEvent(name: "pressureAbs", value: val, unit: "inHg");
    break;

  case "rainratein":
    if (state.rainRate != val) sendEvent(name: "rainRate", value: val, unit: "in/h");
    break;

  case "eventrainin":
    if (state.rainEvent != val) sendEvent(name: "rainEvent", value: val, unit: "in");
    break;

  case "hourlyrainin":
    if (state.rainHourly != val) sendEvent(name: "rainHourly", value: val, unit: "in");
    break;

  case "dailyrainin":
    if (state.rainDaily != val) sendEvent(name: "rainDaily", value: val, unit: "in");
    break;

  case "weeklyrainin":
    if (state.rainWeekly != val) sendEvent(name: "rainWeekly", value: val, unit: "in");
    break;

  case "monthlyrainin":
    if (state.rainMonthly != val) sendEvent(name: "rainMonthly", value: val, unit: "in");
    break;

  case "yearlyrainin":
    if (state.rainYearly != val) sendEvent(name: "rainYearly", value: val, unit: "in");
    break;

  case "totalrainin":
    if (state.rainTotal != val) sendEvent(name: "rainTotal", value: val, unit: "in");
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
    if (state.windSpeed != val) sendEvent(name: "windSpeed", value: val, unit: "mph");
    break;

  case "windspdmph_avg10m":
    if (state.windSpeed_avg_10m != val) sendEvent(name: "windSpeed_avg_10m", value: val, unit: "mph");
    break;

  case "windgustmph":
    if (state.windGust != val) sendEvent(name: "windGust", value: val, unit: "mph");
    break;

  case "maxdailygust":
    if (state.windGustMaxDaily != val) sendEvent(name: "windGustMaxDaily", value: val, unit: "mph");
    break;
  }
}

// Driver lifecycle -----------------------------------------------------------------------------------------------------------

void installed() { logDebug("installed()"); }
void updated() { logDebug("updated()"); }
void uninstalled() { logDebug("uninstalled()"); }
void parse(String msg) { logDebug("parse()"); }

// EOF ------------------------------------------------------------------------------------------------------------------------
