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
    attribute "batteryOrg", "number";                          // original/un-translated battery value returned by the sensor 

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

    attribute "pm25", "number";                                // µg/m³ - PM2.5 particle reading - current
    attribute "aqiIndex", "number";                            // AQI index (0-500)
    attribute "aqiDanger", "string";                           // AQI danger  
    attribute "aqiColor", "string";                            // AQI HTML color  

    attribute "pm25_avg_24h", "number";                        // µg/m³ - PM2.5 particle reading - average over the last 24 hours
    attribute "aqiIndex_avg_24h", "number";                    // AQI index (0-500) 
    attribute "aqiDanger_avg_24h", "string";                   // AQI danger
    attribute "aqiColor_avg_24h", "string";                    // AQI HTML color  
    
 // attribute "ultravioletIndex", "number";                    // UV index (0-11+) 
    attribute "ultravioletDanger", "string";                   // UV Danger (0-2.9) Low, (3-5.9) Medium, (6-7.9) High, (8-10.9) Very High, (11+) Extreme
    attribute "ultravioletColor", "string";                    // UV HTML color

 // attribute "illuminance", "number";                         // lux

    attribute "windDirection", "number";                       // 0-359°
    attribute "windDirectionCompass", "string";                // NNE
    attribute "windDirection_avg_10m", "number";               // 0-359° - average over the last 10 minutes 
    attribute "windDirectionCompass_avg_10m", "string";        // NNE - average over the last 10 minutes 
    attribute "windSpeed", "number";                           // mph
    attribute "windSpeed_avg_10m", "number";                   // mph - average over the last 10 minutes
    attribute "windGust", "number";                            // mph
    attribute "windGustMaxDaily", "number";                    // mph - max in the current day 
    
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

/* Not used yet, but fully functional to display sensor preferences based on capabilities
 
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

private BigDecimal translateAttributeRange(BigDecimal val, BigDecimal inMin, BigDecimal inMax, BigDecimal outMin, BigDecimal outMax, Boolean returnInt = true) {
  // Let make sure ranges are correct
  assert (inMin <= inMax);
  assert (outMin <= outMax);

  // Refrain input value
  if (val < inMin) val = inMin;
  else if (val > inMax) val = inMax;

  val = ((val - inMin) * (outMax - outMin)) / (inMax - inMin);
  if (returnInt) val = val.toBigInteger();

  return (val);
}

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
  BigDecimal percent;
  String unitOrg;

  switch (type) {
  case 1:
    // Change range from (1.40V - 1.65V) to (0% - 100%)
    percent = translateAttributeRange(original, 1.40, 1.65, 0, 100);
    unitOrg = "V";
    break;

  case 2:
    // Change range from (0 - 5) to (0% - 100%)
    percent = translateAttributeRange(original, 0, 5, 0, 100);
    unitOrg = "level";
    break;

  default:
    // Change range from (0  or 1) to (100% or 0%)
    percent = (original == 0)? 100: 0;
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

private void updateAttributePM25(String val, String attribute, String attributeIndex, String attributeDanger, String attributeColor) {
  //
  // Conversions based on https://en.wikipedia.org/wiki/Air_quality_index
  //
  BigDecimal pm25 = val.toBigDecimal();

  BigDecimal aqi;
  String danger;
  String color;

  if (pm25 < 12.1) {
    aqi = translateAttributeRange(pm25, 0, 12, 0, 50);
    danger = "Good";
    color = "#3EA72D";
  }
  else if (pm25 < 35.5) {
    aqi = translateAttributeRange(pm25, 12.1, 35.4, 51, 100);
    danger = "Moderate";
    color = "#FFF300";
  }
  else if (pm25 < 55.5) {
    aqi = translateAttributeRange(pm25, 35.5, 55.4, 101, 150);
    danger = "Unhealthy for Sensitive Groups";
    color = "#F18B00";
  }
  else if (pm25 < 150.5) {
    aqi = translateAttributeRange(pm25, 55.5, 150.4, 151, 200);
    danger = "Unhealthy";
    color = "#E53210";
  }
  else if (pm25 < 250.5) {
    aqi = translateAttributeRange(pm25, 150.5, 250.4, 201, 300);
    danger = "Very Unhealthy";
    color = "#B567A4";
  }
  else if (pm25 < 350.5) {
    aqi = translateAttributeRange(pm25, 250.5, 350.4, 301, 400);
    danger = "Hazardous";
    color = "#7E0023";
  }
  else {
    aqi = translateAttributeRange(pm25, 350.5, 500.4, 401, 500);
    danger = "Hazardous";
    color = "#7E0023";
  }

  updateAttributeNumber(pm25, attribute, "µg/m³");
  updateAttributeNumber(aqi, attributeIndex, "AQI");
  updateAttributeString(danger, attributeDanger);
  updateAttributeString(color, attributeColor);
}

// ------------------------------------------------------------

private void updateAttributeUV(String val, String attribute, String attributeDanger, String attributeColor) {
  //
  // Conversions based on https://en.wikipedia.org/wiki/Ultraviolet_index
  // 
  BigDecimal index = val.toBigDecimal();

  String danger;
  String color;

  if (index < 3)       { danger = "Low";       color = "#3EA72D"; }
  else if (index < 6)  { danger = "Medium";    color = "#FFF300"; }
  else if (index < 8)  { danger = "High";      color = "#F18B00"; }
  else if (index < 11) { danger = "Very High"; color = "#E53210"; }
  else                 { danger = "Extreme";   color = "#B567A4"; }
    
  updateAttributeNumber(index, attribute, "uvi");
  updateAttributeString(danger, attributeDanger);
  updateAttributeString(color, attributeColor);
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

private void updateAttributeWindDirection(String val, String attribute, String attributeCompass) {
  
  BigDecimal direction = val.toBigDecimal();

  // BigDecimal doesn't support modulo operation so we roll up our own  
  direction = direction - (direction.divideToIntegralValue(360) * 360);

  String compass;

  if (direction >= 348.75 || direction < 11.25) compass = "N";
  else if (direction < 33.75)                   compass = "NNE";
  else if (direction < 56.25)                   compass = "NE";
  else if (direction < 78.75)                   compass = "ENE";
  else if (direction < 101.25)                  compass = "E";
  else if (direction < 123.75)                  compass = "ESE";
  else if (direction < 146.25)                  compass = "SE";
  else if (direction < 168.75)                  compass = "SSE";
  else if (direction < 191.25)                  compass = "S";
  else if (direction < 213.75)                  compass = "SSW";
  else if (direction < 236.25)                  compass = "SW";
  else if (direction < 258.75)                  compass = "WSW";
  else if (direction < 281.25)                  compass = "W";
  else if (direction < 303.75)                  compass = "WNW";
  else if (direction < 326.25)                  compass = "NW";
  else                                          compass = "NNW";
  
  updateAttributeNumber(direction, attribute, "°");
  updateAttributeString(compass, attributeCompass);
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
    updateAttributePM25(val, "pm25", "aqiIndex", "aqiDanger", "aqiColor");
    break;

  case ~/pm25_avg_24h_ch[1-4]/:
    updateAttributePM25(val, "pm25_avg_24h", "aqiIndex_avg_24h", "aqiDanger_avg_24h", "aqiColor_avg_24h");
    break;

  case "uv":
    updateAttributeUV(val, "ultravioletIndex", "ultravioletDanger", "ultravioletColor");
    break;

  case "solarradiation":
    updateAttributeNumber((val.toBigDecimal() / 0.0079), "illuminance", "lux", 0);
    break;

  case "winddir":
    updateAttributeWindDirection(val, "windDirection", "windDirectionCompass");
    break;

  case "winddir_avg10m":
    updateAttributeWindDirection(val, "windDirection_avg_10m", "windDirectionCompass_avg_10m");
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
