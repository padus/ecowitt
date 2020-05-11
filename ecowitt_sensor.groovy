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
    
    attribute "html", "string";                                // e.g. "<p>Temperature: ${temperature}°F</p><p>Humidity: ${humidity}%</p>"
  }

  preferences {
    input(name: "htmlTemplate", type: "string", title: "<font style='font-size:12px; color:#1a77c9'>HTML Tile Template</font>", description: "<font style='font-size:12px; font-style: italic'>e.g.: &lt;p&gt;Temperature: <b>\${temperature}</b>&deg;F&lt;/p&gt;&lt;p&gt;Humidity: <b>\${humidity}</b>%&lt;/p&gt;</font>", defaultValue: "");
  }
}

// Logging --------------------------------------------------------------------------------------------------------------------

private void logError(String str) { log.error(str); }
private void logWarning(String str) { if (getParent().getLogLevel() > 0) log.warn(str); }
private void logInfo(String str) { if (getParent().getLogLevel() > 1) log.info(str); }
private void logDebug(String str) { if (getParent().getLogLevel() > 2) log.debug(str); }
private void logTrace(String str) { if (getParent().getLogLevel() > 3) log.trace(str); }

// Attribute handling ----------------------------------------------------------------------------------------------------------

private BigDecimal roundPrecision(BigDecimal val, Integer decimals = -1) {

  if (decimals >= 0) {
    // If rounding is required we use the Float one because the BigDecimal is not supported/not working on Hubitat
    val = val.toFloat().round(decimals).toBigDecimal();
  }

  BigDecimal integer = val.toBigInteger();

  // If we an integer we just return
  if (val == integer) return (integer);

  // Otherwise remove trailing zeros, if any
  return (val.stripTrailingZeros());
}

// ------------------------------------------------------------

private void updateNumber(BigDecimal val, String attribute, String measure, Integer decimals = -1) {

  val = roundPrecision(val, decimals);
  if (device.currentValue(attribute).toBigDecimal() != val) sendEvent(name: attribute, value: val, unit: measure);
}

// ------------------------------------------------------------

private void updateTemperature(String val, String attribute) {
  
  BigDecimal degrees = val.toBigDecimal();
  String measure = "°F";

  // Convert to metric if requested
  if (getParent().isSystemMetric()) {
    degrees = (degrees - 32) / 1.8;
    measure = "°C";
  }

  degrees = roundPrecision(degrees, 1);

  if (device.currentValue(attribute).toBigDecimal() != degrees) sendEvent(name: attribute, value: degrees, unit: measure);
}

// ------------------------------------------------------------

private void updatePressure(String val, String attribute) {
  
  BigDecimal length = val.toBigDecimal();
  String measure = "inHg";

  // Convert to metric if requested
  if (getParent().isSystemMetric()) {
    length = length * 25.4;
    measure = "mmHg";
  }

  length = roundPrecision(length, 2);

  if (device.currentValue(attribute).toBigDecimal() != length) sendEvent(name: attribute, value: length, unit: measure);
}

// ------------------------------------------------------------

private void updateRain(String val, String attribute, Boolean hour = false) {
  
  BigDecimal amount = val.toBigDecimal();
  String measure = hour? "in/h": "in";

  // Convert to metric if requested
  if (getParent().isSystemMetric()) {
    amount = amount * 25.4;
    measure = hour? "mm/h": "mm";
  }

  amount = roundPrecision(amount, 2);

  if (device.currentValue(attribute).toBigDecimal() != amount) sendEvent(name: attribute, value: amount, unit: measure);
}

// ------------------------------------------------------------

private void updateWind(String val, String attribute) {
  
  BigDecimal speed = val.toBigDecimal();
  String measure = "mph";

  // Convert to metric if requested
  if (getParent().isSystemMetric()) {
    speed = speed * 1.609344;
    measure = "km/h";
  }

  speed = roundPrecision(speed, 1);
    
  if (device.currentValue(attribute).toBigDecimal() != speed) sendEvent(name: attribute, value: speed, unit: measure);
}

// ------------------------------------------------------------

private void updateHtml(String val) {

  if (settings.htmlTemplate) {
    // Create special compund/html tile  
    val = settings.htmlTemplate.toString().replaceAll( ~/\$\{([A-Za-z][A-Za-z0-9_]*)\}/ ) { java.util.ArrayList m -> device.currentValue("${m[1]}").toString(); }
    if (device.currentValue("html").toString() != val) sendEvent(name: "html", value: val);  
  }
}

// ------------------------------------------------------------

void updateAttribute(String key, String val) {
  //
  // Dispatch attributes changes to hub
  //
  switch (key) {

  case ~/soilbatt[1-8]/:
    // The soil moisture sensor returns the battery voltage which, for regular AA alkaline, ranges from 1.40V (empty) to 1.65V (full)
    // Change range from (1.40V - 1.65V) to (0% - 100%)
    BigDecimal percent = val.toBigDecimal() * 100;
    if (percent < 140) percent = 140;
    else if (percent > 165) percent = 165;
    percent = ((percent - 140) * (100 - 0)) / (165 - 140);

    updateNumber(percent, "battery", "%", 0);
    break;
  
  case ~/pm25batt[1-4]/:
    // The air quality sensor returns a battery value between 0 (empty) and 5 (full)
    BigDecimal percent = val.toBigDecimal() * 20;
    if (percent > 100) percent = 100;
    
    updateNumber(percent, "battery", "%", 0);
    break;
  
  case "wh25batt":
  case "wh26batt":
  case ~/batt[1-8]/:
  case "wh40batt": 
  case "wh65batt":
    // It seems like most sensors returns a battery value of either 0 (full) or 1 (empty)
    BigDecimal percent = val.toBigDecimal();
    percent = (percent == 0)? 100: 0;

    updateNumber(percent, "battery", "%", 0);
    break;
  
  case "tempinf":
  case "tempf":
  case ~/temp[1-8]f/:
    updateTemperature(val, "temperature");
    break;

  case "humidityin":
  case "humidity":
  case ~/humidity[1-8]/:
  case ~/soilmoisture[1-8]/:
    updateNumber(val.toBigDecimal(), "humidity", "%");
    break;

  case "baromrelin":
    updatePressure(val, "pressure");
    break;

  case "baromabsin":
    updatePressure(val, "pressureAbs");
    break;

  case "rainratein":
    updateRain(val, "rainRate", true);
    break;

  case "eventrainin":
    updateRain(val, "rainEvent");
    break;

  case "hourlyrainin":
    updateRain(val, "rainHourly");
    break;

  case "dailyrainin":
    updateRain(val, "rainDaily");
    break;

  case "weeklyrainin":
    updateRain(val, "rainWeekly");
    break;

  case "monthlyrainin":
    updateRain(val, "rainMonthly");
    break;

  case "yearlyrainin":
    updateRain(val, "rainYearly");
    break;

  case "totalrainin":
    updateRain(val, "rainTotal");
    break;

  case ~/pm25_ch[1-4]/:
    updateNumber(val.toBigDecimal(), "pm25", "µg/cm³");
    break;

  case ~/pm25_avg_24h_ch[1-4]/:
    updateNumber(val.toBigDecimal(), "pm25_avg_24h", "µg/cm³");
    break;

  case "uv":
    updateNumber(val.toBigDecimal(), "ultravioletIndex", "uvi");
    break;

  case "solarradiation":
    BigDecimal amount = val.toBigDecimal() / 0.0079;
    updateNumber(amount, "illuminance", "lux", 0);
    break;

  case "winddir":
    updateNumber(val.toBigDecimal(), "windDirection", "°");
    break;

  case "winddir_avg10m":
    updateNumber(val.toBigDecimal(), "windDirection_avg_10m", "°");
    break;

  case "windspeedmph":
    updateWind(val, "windSpeed");
    break;

  case "windspdmph_avg10m":
    updateWind(val, "windSpeed_avg_10m");
    break;

  case "windgustmph":
    updateWind(val, "windGust");
    break;

  case "maxdailygust":
    updateWind(val, "windGustMaxDaily");
    break;
  }

  updateHtml(val);
}

// Driver lifecycle -----------------------------------------------------------------------------------------------------------

void installed() { logDebug("addedSensor(${device.deviceNetworkId})"); }
void updated() {}
void uninstalled() { logDebug("deletedSensor(${device.deviceNetworkId})"); }
void parse(String msg) {}

// EOF ------------------------------------------------------------------------------------------------------------------------
