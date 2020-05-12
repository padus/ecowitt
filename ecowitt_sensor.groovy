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
    attribute "batteryOrg", "number";                          // original/un-translated battery value returned by the sensor 

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

    attribute "windDirectionStr", "string";                    // NNE
    attribute "windDirectionStr_avg_10m", "string";            // NNE - average over the last 10 minutes 
    
    attribute "html", "string";                                // e.g. "<p>Temperature: ${temperature}°F</p><p>Humidity: ${humidity}%</p>"
  }

  preferences {
    input(name: "htmlTemplate", type: "string", title: "<font style='font-size:12px; color:#1a77c9'>HTML Tile Template</font>", description: "<font style='font-size:12px; font-style: italic'>An HTML snippet formatted with one or more \${attribute} as described <a href='https://github.com/mircolino/ecowitt/blob/master/readme.md#templates' target='_blank'>here</a></font>", defaultValue: "");
  }
}

// Logging --------------------------------------------------------------------------------------------------------------------

private void logError(String str) { log.error(str); }
private void logWarning(String str) { if (getParent().getLogLevel() > 0) log.warn(str); }
private void logInfo(String str) { if (getParent().getLogLevel() > 1) log.info(str); }
private void logDebug(String str) { if (getParent().getLogLevel() > 2) log.debug(str); }
private void logTrace(String str) { if (getParent().getLogLevel() > 3) log.trace(str); }

// Attribute handling ----------------------------------------------------------------------------------------------------------

/* Not used yet, but fully functional to display sensor preferences base on capabilities
 
private Boolean isAttributeClassSupported(Integer class) {
  //
  // Return 'true' if one of the following classes of attributes is supported by the current sensor
  //
  // 0) Temperature
  // 1) Humidity (air)
  // 2) Pressure
  // 3) Rain
  // 4) Wind
  // 5) UV
  // 6) Light
  // 7) Moisture (soil)
  // 8) Quality (air)
  //                 0      1      2      3      4      5      6      7      8
  Boolean[] wh32b = [true,  true,  true,  false, false, false, false, false, false];
  Boolean[] wh32e = [true,  true,  false, false, false, false, false, false, false];
  Boolean[] wh31b = [true,  true,  false, false, false, false, false, false, false];
  Boolean[] wh40  = [false, false, false, true,  false, false, false, false, false];
  Boolean[] wh41_ = [false, false, false, false, false, false, false, false, true ];
  Boolean[] wh51_ = [false, false, false, false, false, false, false, true,  false];
  Boolean[] wh80  = [true,  true,  false, false, true,  true,  true,  false, false];
  Boolean[] wh69e = [true,  true,  false, true,  true,  true,  true,  false, false];

  String model = device.getDeviceNetworkId().take(5).toLowerCase();
  return ("${model}"[class]);
}

*/

// ------------------------------------------------------------

private BigDecimal getAttributeNumber(String attribute) {
  // This will force an implicit cast Object -> BigDecimal
  // Return 'null' if the attribute has not been initialized
  return (device.currentValue(attribute));
}

// ------------------------------------------------------------

private String getAttributeString(String attribute) {
  // This will force an implicit cast Object -> String
  // Return 'null' if the attribute has not been initialized
  return (device.currentValue(attribute));
}

// ------------------------------------------------------------

private void updateAttributeNumber(BigDecimal val, String attribute, String measure, Integer decimals = -1) {

  // If rounding is required we use the Float one because the BigDecimal is not supported/not working on Hubitat
  if (decimals >= 0) val = val.toFloat().round(decimals).toBigDecimal();

  BigDecimal integer = val.toBigInteger();

  // We don't strip zeros on an integer otherwise it gets converted to scientific exponential notation
  val = (val == integer)? integer: val.stripTrailingZeros();

  if (getAttributeNumber(attribute) != val) sendEvent(name: attribute, value: val, unit: measure);
}

// ------------------------------------------------------------

private void updateAttributeString(String val, String attribute) {

  if (getAttributeString(attribute) != val) sendEvent(name: attribute, value: val);
}

// ------------------------------------------------------------

private void updateAttributeBattery(String val, String attribute, String attributeOrg, Integer type) {
  //
  // Convert all different batteries returned values to a 0-100% range
  // Type: 1) soil moisture sensor - range from 1.40V (empty) to 1.65V (full)
  //       2) air quality sensor - range from 0 (empty) to 5 (full)
  //       0) other sensors - 0 (full) or 1 (empty)
  //
  BigDecimal original = val.toBigDecimal();
  BigDecimal percent = original;
  String unitOrg;

  switch (type) {
  case 1:
    // Change range from (1.40V - 1.65V) to (0% - 100%)
    percent = percent * 100;
    if (percent < 140) percent = 140;
    else if (percent > 165) percent = 165;
    percent = ((percent - 140) * (100 - 0)) / (165 - 140);
    unitOrg = "V";
    break;

  case 2:
    // Change range from (0 - 5) to (0% - 100%)
    percent = percent * 20;
    if (percent > 100) percent = 100;
    unitOrg = "level";
    break;

  default:
    // Change range from (0  or 1) to (100% or 0%)
    percent = (percent == 0)? 100: 0;
    unitOrg = "!bool";
  }

  updateAttributeNumber(percent, attribute, "%", 0);
  updateAttributeNumber(original, attributeOrg, unitOrg);
}

// ------------------------------------------------------------

private void updateAttributeTemperature(String val, String attribute) {
  
  BigDecimal degrees = val.toBigDecimal();
  String measure = "°F";

  // Convert to metric if requested
  if (getParent().isSystemMetric()) {
    degrees = (degrees - 32) / 1.8;
    measure = "°C";
  }

  updateAttributeNumber(degrees, attribute, measure, 1);
}

// ------------------------------------------------------------

private void updateAttributePressure(String val, String attribute) {
  
  BigDecimal length = val.toBigDecimal();
  String measure = "inHg";

  // Convert to metric if requested
  if (getParent().isSystemMetric()) {
    length = length * 25.4;
    measure = "mmHg";
  }

  updateAttributeNumber(length, attribute, measure, 2);
}

// ------------------------------------------------------------

private void updateAttributeRain(String val, String attribute, Boolean hour = false) {
  
  BigDecimal amount = val.toBigDecimal();
  String measure = hour? "in/h": "in";

  // Convert to metric if requested
  if (getParent().isSystemMetric()) {
    amount = amount * 25.4;
    measure = hour? "mm/h": "mm";
  }

  updateAttributeNumber(amount, attribute, measure, 2);
}

// ------------------------------------------------------------

private void updateAttributeWindSpeed(String val, String attribute) {
  
  BigDecimal speed = val.toBigDecimal();
  String measure = "mph";

  // Convert to metric if requested
  if (getParent().isSystemMetric()) {
    speed = speed * 1.609344;
    measure = "km/h";
  }

  updateAttributeNumber(speed, attribute, measure, 1);
}

// ------------------------------------------------------------

private void updateAttributeWindDirection(String val, String attribute, String attributeStr) {
  
  BigDecimal dir = val.toBigDecimal();

  // BigDecimal doesn't support modulo operation so we roll up our own  
  dir = dir - (dir.divideToIntegralValue(360) * 360);

  String dirStr;

  if (dir >= 348.75 || dir < 11.25) dirStr = "N";
  else if (dir < 33.75)             dirStr = "NNE";
  else if (dir < 56.25)             dirStr = "NE";
  else if (dir < 78.75)             dirStr = "ENE";
  else if (dir < 101.25)            dirStr = "E";
  else if (dir < 123.75)            dirStr = "ESE";
  else if (dir < 146.25)            dirStr = "SE";
  else if (dir < 168.75)            dirStr = "SSE";
  else if (dir < 191.25)            dirStr = "S";
  else if (dir < 213.75)            dirStr = "SSW";
  else if (dir < 236.25)            dirStr = "SW";
  else if (dir < 258.75)            dirStr = "WSW";
  else if (dir < 281.25)            dirStr = "W";
  else if (dir < 303.75)            dirStr = "WNW";
  else if (dir < 326.25)            dirStr = "NW";
  else                              dirStr = "NNW";
  
  updateAttributeNumber(dir, attribute, "°");
  updateAttributeString(dirStr, attributeStr);
}

// ------------------------------------------------------------

private void updateAttributeHtml(String val) {

  if (settings.htmlTemplate) {
    // Create special compund/html tile  
    val = settings.htmlTemplate.toString().replaceAll( ~/\$\{\s*([A-Za-z][A-Za-z0-9_]*)\s*\}/ ) { java.util.ArrayList m -> getAttributeString("${m[1]}"); }
    updateAttributeString(val, "html");
  }
}

// ------------------------------------------------------------

void updateAttribute(String key, String val) {
  //
  // Dispatch attributes changes to hub
  //
  switch (key) {

  case ~/soilbatt[1-8]/:
    updateAttributeBattery(val, "battery", "batteryOrg", 1);
    break;
  
  case ~/pm25batt[1-4]/:
    updateAttributeBattery(val, "battery", "batteryOrg", 2);
    break;
  
  case "wh25batt":
  case "wh26batt":
  case ~/batt[1-8]/:
  case "wh40batt": 
  case "wh65batt":
    updateAttributeBattery(val, "battery", "batteryOrg", 0);
    break;
  
  case "tempinf":
  case "tempf":
  case ~/temp[1-8]f/:
    updateAttributeTemperature(val, "temperature");
    break;

  case "humidityin":
  case "humidity":
  case ~/humidity[1-8]/:
  case ~/soilmoisture[1-8]/:
    updateAttributeNumber(val.toBigDecimal(), "humidity", "%");
    break;

  case "baromrelin":
    updateAttributePressure(val, "pressure");
    break;

  case "baromabsin":
    updateAttributePressure(val, "pressureAbs");
    break;

  case "rainratein":
    updateAttributeRain(val, "rainRate", true);
    break;

  case "eventrainin":
    updateAttributeRain(val, "rainEvent");
    break;

  case "hourlyrainin":
    updateAttributeRain(val, "rainHourly");
    break;

  case "dailyrainin":
    updateAttributeRain(val, "rainDaily");
    break;

  case "weeklyrainin":
    updateAttributeRain(val, "rainWeekly");
    break;

  case "monthlyrainin":
    updateAttributeRain(val, "rainMonthly");
    break;

  case "yearlyrainin":
    updateAttributeRain(val, "rainYearly");
    break;

  case "totalrainin":
    updateAttributeRain(val, "rainTotal");
    break;

  case ~/pm25_ch[1-4]/:
    updateAttributeNumber(val.toBigDecimal(), "pm25", "µg/cm³");
    break;

  case ~/pm25_avg_24h_ch[1-4]/:
    updateAttributeNumber(val.toBigDecimal(), "pm25_avg_24h", "µg/cm³");
    break;

  case "uv":
    updateAttributeNumber(val.toBigDecimal(), "ultravioletIndex", "uvi");
    break;

  case "solarradiation":
    updateAttributeNumber((val.toBigDecimal() / 0.0079), "illuminance", "lux", 0);
    break;

  case "winddir":
    updateAttributeWindDirection(val, "windDirection", "windDirectionStr");
    break;

  case "winddir_avg10m":
    updateAttributeWindDirection(val, "windDirection_avg_10m", "windDirectionStr_avg_10m");
    break;

  case "windspeedmph":
    updateAttributeWindSpeed(val, "windSpeed");
    break;

  case "windspdmph_avg10m":
    updateAttributeWindSpeed(val, "windSpeed_avg_10m");
    break;

  case "windgustmph":
    updateAttributeWindSpeed(val, "windGust");
    break;

  case "maxdailygust":
    updateAttributeWindSpeed(val, "windGustMaxDaily");
    break;
  }

  updateAttributeHtml(val);
}

// Driver lifecycle -----------------------------------------------------------------------------------------------------------

void installed() { logDebug("addedSensor(${device.deviceNetworkId})"); }
void updated() {}
void uninstalled() { logDebug("deletedSensor(${device.deviceNetworkId})"); }
void parse(String msg) {}

// EOF ------------------------------------------------------------------------------------------------------------------------
