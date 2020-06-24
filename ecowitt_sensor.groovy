/**
 * Driver:     Ecowitt RF Sensor 
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
  definition(name: "Ecowitt RF Sensor", namespace: "mircolino", author: "Mirco Caramori", importUrl: "https://raw.githubusercontent.com/mircolino/ecowitt/master/ecowitt_sensor.groovy") {
    capability "Sensor";

    capability "Battery"; 
    capability "Temperature Measurement";
    capability "Relative Humidity Measurement";
    capability "Pressure Measurement";
    capability "Ultraviolet Index";
    capability "Illuminance Measurement";
    capability "Water Sensor";

 // attribute "battery", "number";                             // 0-100%
    attribute "batteryIcon", "number";                         // 0, 20, 40, 60, 80, 100 
    attribute "batteryOrg", "number";                          // original/un-translated battery value returned by the sensor 

    attribute "batteryTemp", "number";                         // 
    attribute "batteryTempIcon", "number";                     // Only created/used when a WH32 is compounded in a PWS
    attribute "batteryTempOrg", "number";                      // 

    attribute "batteryRain", "number";                         // 
    attribute "batteryRainIcon", "number";                     // Only created/used when a WH40 is compounded in a PWS
    attribute "batteryRainOrg", "number";                      // 

    attribute "batteryWind", "number";                         // 
    attribute "batteryWindIcon", "number";                     // Only created/used when a WH68/WH80 is compounded in a PWS 
    attribute "batteryWindOrg", "number";                      // 

 // attribute "temperature", "number";                         // °F

 // attribute "humidity", "number";                            // 0-100%
    attribute "dewPoint", "number";                            // °F - calculated using outdoor "temperature" & "humidity" 
    attribute "heatIndex", "number";                           // °F - calculated using outdoor "temperature" & "humidity"
    attribute "heatDanger", "string";                          // Heat index danger level
    attribute "heatColor", "string";                           // Heat index HTML color

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
    attribute "aqi", "number";                                 // AQI (0-500)
    attribute "aqiDanger", "string";                           // AQI danger level  
    attribute "aqiColor", "string";                            // AQI HTML color  

    attribute "pm25_avg_24h", "number";                        // µg/m³ - PM2.5 particle reading - average over the last 24 hours
    attribute "aqi_avg_24h", "number";                         // AQI (0-500) 
    attribute "aqiDanger_avg_24h", "string";                   // AQI danger level
    attribute "aqiColor_avg_24h", "string";                    // AQI HTML color  

 // attribute "water", "enum";                                 // "dry" or "wet"
    attribute "leak", "number";                                // dry) 0, wet) 1
    attribute "leakMsg", "string";                             // dry) "Dry", wet) "Leak detected!"
    attribute "leakColor", "string";                           // dry) "ffffff", wet) "ff0000" to colorize the icon
    
    attribute "lightningDistance", "number";                   // Strike distance - km
    attribute "lightningCount", "number";                      // Strike count
    attribute "lightningTime", "string";                       // Strike time - local time

 // attribute "ultravioletIndex", "number";                    // UV index (0-11+) 
    attribute "ultravioletDanger", "string";                   // UV danger (0-2.9) Low, (3-5.9) Medium, (6-7.9) High, (8-10.9) Very High, (11+) Extreme
    attribute "ultravioletColor", "string";                    // UV HTML color

 // attribute "illuminance", "number";                         // lux
    attribute "solarRadiation", "number";                      // W/m²

    attribute "windDirection", "number";                       // 0-359°
    attribute "windCompass", "string";                         // NNE
    attribute "windDirection_avg_10m", "number";               // 0-359° - average over the last 10 minutes 
    attribute "windCompass_avg_10m", "string";                 // NNE - average over the last 10 minutes 
    attribute "windSpeed", "number";                           // mph
    attribute "windSpeed_avg_10m", "number";                   // mph - average over the last 10 minutes
    attribute "windGust", "number";                            // mph
    attribute "windGustMaxDaily", "number";                    // mph - max in the current day 
    attribute "windChill", "number";                           // °F - calculated using outdoor "temperature" & "windSpeed"
    attribute "windDanger", "string";                          // Windchill danger level
    attribute "windColor", "string";                           // Windchill HTML color

    attribute "html", "string";                                // 
    attribute "html1", "string";                               // 
    attribute "html2", "string";                               // e.g. "<div>Temperature: ${temperature}°F<br>Humidity: ${humidity}%</div>"
    attribute "html3", "string";                               // 
    attribute "html4", "string";                               // 

    attribute "time", "string";                                // Time last data was posted
    attribute "status", "string";                              // Display current driver status
  }

  preferences {
    input(name: "htmlTemplate", type: "string", title: "<font style='font-size:12px; color:#1a77c9'>Tile HTML Template(s)</font>", description: "<font style='font-size:12px; font-style: italic'>See <u><a href='https://github.com/mircolino/ecowitt/blob/master/readme.md#templates' target='_blank'>documentation</a></u> for input formats</font>", defaultValue: "");
    if (localAltitude != null) {
      input(name: "localAltitude", type: "string", title: "<font style='font-size:12px; color:#1a77c9'><u><a href='https://www.advancedconverter.com/map-tools/altitude-on-google-maps' target='_blank'>Altitude</a></u> to Correct Sea Level Pressure</font>", description: "<font style='font-size:12px; font-style: italic'>Examples: \"378 ft\" or \"115 m\"</font>", defaultValue: "", required: true);
    }
  }
}

/*
 * State variables used by the driver:
 *
 * status: -1) driver is in error
 *          0) driver is waiting to receive data
 *          1) driver is OK and processing data
 *
 * compounded: true) child sensor bundles more than one physical sensor
 */

/*
 * Data variables used by the driver:
 *
 * "htmlTemplate"                                              // User template 0
 * "htmlTemplate1"                                             // User template 1
 * "htmlTemplate2"                                             // User template 2
 * "htmlTemplate3"                                             // User template 3
 * "htmlTemplate4"                                             // User template 4
 */

// Logging --------------------------------------------------------------------------------------------------------------------

private void logError(String str) { log.error(str); }
private void logWarning(String str) { if (getParent().logGetLevel() > 0) log.warn(str); }
private void logInfo(String str) { if (getParent().logGetLevel() > 1) log.info(str); }
private void logDebug(String str) { if (getParent().logGetLevel() > 2) log.debug(str); }
private void logTrace(String str) { if (getParent().logGetLevel() > 3) log.trace(str); }

// Ztatus ---------------------------------------------------------------------------------------------------------------------

private Boolean ztatus(String str, String color = null) {

  if (color) str = "<font style='color:${color}'>${str}</font>";

  return (attributeUpdateString(str, "status"));
}

// Conversions ----------------------------------------------------------------------------------------------------------------

private Boolean unitSystemIsMetric() {
  //
  // Return true if the selected unit system is metric
  //
  return (getParent().unitSystemIsMetric());
}

// ------------------------------------------------------------

private String timeEpochToLocal(String time) {
  //
  // Convert Unix Epoch time (seconds) to local time with locale format 
  //
  try {
    Long epoch = time.toLong() * 1000L;

    Date date = new Date(epoch);

    java.text.SimpleDateFormat format = new java.text.SimpleDateFormat();
    time = format.format(date);
  }
  catch (Exception e) {
    logError("Exception in timeEpochToLocal(): ${e}");
  }

  return (time);
}

// ------------------------------------------------------------

private BigDecimal convertRange(BigDecimal val, BigDecimal inMin, BigDecimal inMax, BigDecimal outMin, BigDecimal outMax, Boolean returnInt = true) {
  // Let make sure ranges are correct
  assert (inMin <= inMax);
  assert (outMin <= outMax);

  // Restrain input value
  if (val < inMin) val = inMin;
  else if (val > inMax) val = inMax;

  val = ((val - inMin) * (outMax - outMin)) / (inMax - inMin) + outMin;
  if (returnInt) {
    // If integer is required we use the Float round because the BigDecimal one is not supported/not working on Hubitat
    val = val.toFloat().round().toBigDecimal();
  }

  return (val);
}

// ------------------------------------------------------------

private BigDecimal convert_F_to_C(BigDecimal val) {
  return ((val - 32) / 1.8);
}

// ------------------------------------------------------------

private BigDecimal convert_C_to_F(BigDecimal val) {
  return ((val * 1.8) + 32);
}

// ------------------------------------------------------------

private BigDecimal convert_inHg_to_hPa(BigDecimal val) {
  return (val * 33.863886666667);
}

// ------------------------------------------------------------

private BigDecimal convert_hPa_to_inHg(BigDecimal val) {
  return (val / 33.863886666667);
}

// ------------------------------------------------------------

private BigDecimal convert_in_to_mm(BigDecimal val) {
  return (val * 25.4);
}

// ------------------------------------------------------------

private BigDecimal convert_mm_to_in(BigDecimal val) {
  return (val / 25.4);
}

// ------------------------------------------------------------

private BigDecimal convert_ft_to_m(BigDecimal val) {
  return (val / 3.28084);
}

// ------------------------------------------------------------

private BigDecimal convert_m_to_ft(BigDecimal val) {
  return (val * 3.28084);
}

// ------------------------------------------------------------

private BigDecimal convert_mi_to_km(BigDecimal val) {
  return (val * 1.609344);
}

// ------------------------------------------------------------

private BigDecimal convert_km_to_mi(BigDecimal val) {
  return (val / 1.609344);
}

// ------------------------------------------------------------

private BigDecimal convert_Wm2_to_lux(BigDecimal val) {
  return (val / 0.0079);
}

// ------------------------------------------------------------

private BigDecimal convert_lux_to_Wm2(BigDecimal val) {
  return (val * 0.0079);
}

// Attribute handling ----------------------------------------------------------------------------------------------------------

private Boolean attributeUpdateString(String val, String attribute) {
  //
  // Only update "attribute" if different
  // Return true if "attribute" has actually been updated/created
  //

  // If starving, register the sensor as alive and receiving data
  if (!state.status) state.status = 1;

  if ((device.currentValue(attribute) as String) != val) {
    sendEvent(name: attribute, value: val);
    return (true);
  }

  return (false);
}

// ------------------------------------------------------------

private Boolean attributeUpdateNumber(BigDecimal val, String attribute, String measure = null, Integer decimals = -1) {
  //
  // Only update "attribute" if different
  // Return true if "attribute" has actually been updated/created
  //

  // If starving, register the sensor as alive and receiving data
  if (!state.status) state.status = 1;

  // If rounding is required we use the Float one because the BigDecimal is not supported/not working on Hubitat
  if (decimals >= 0) val = val.toFloat().round(decimals).toBigDecimal();

  BigDecimal integer = val.toBigInteger();

  // We don't strip zeros on an integer otherwise it gets converted to scientific exponential notation
  val = (val == integer)? integer: val.stripTrailingZeros();

  // Coerce Object -> BigDecimal
  if ((device.currentValue(attribute) as BigDecimal) != val) {
    if (measure) sendEvent(name: attribute, value: val, unit: measure);
    else sendEvent(name: attribute, value: val); 
    return (true);
  }

  return (false);
}

// ------------------------------------------------------------

private List<String> attributeEnumerate(Boolean existing = true) {
  //
  // Return a list of all available attributes
  // If "existing" == true return only those that have been already created (non-null ones)
  // Returned list can be empty but never return null
  //
  List<String> list = [];
  List<com.hubitat.hub.domain.Attribute> attrib = device.getSupportedAttributes();
  if (attrib) {
    attrib.each {
      if (existing == false || device.currentValue(it.name) != null) list.add(it.name);
    }
  }

  return (list);
}

// ------------------------------------------------------------

private Boolean attributeUpdateBattery(String val, String attribBattery, String attribBatteryIcon, String attribBatteryOrg, Integer type) {
  //
  // Convert all different batteries returned values to a 0-100% range
  // Type: 1) soil moisture sensor - range from 1.40V (empty) to 1.65V (full)
  //       2) air quality sensor - range from 0 (empty) to 5 (full)
  //       0) other sensors - 0 (full) or 1 (empty)
  //
  BigDecimal original = val.toBigDecimal();
  BigDecimal percent;
  BigDecimal icon;
  String unitOrg;

  switch (type) {
  case 1:
    // Change range from (1.40V - 1.65V) to (0% - 100%)
    percent = convertRange(original, 1.40, 1.65, 0, 100);
    unitOrg = "V";
    break;

  case 2:
    // Change range from (0 - 5) to (0% - 100%)
    percent = convertRange(original, 0, 5, 0, 100);
    unitOrg = "level";
    break;

  default:
    // Change range from (0  or 1) to (100% or 0%)
    percent = (original == 0)? 100: 0;
    unitOrg = "!bool";
  }

  if (percent < 10) icon = 0;
  else if (percent < 30) icon = 20;
  else if (percent < 50) icon = 40;
  else if (percent < 70) icon = 60;
  else if (percent < 90) icon = 80;
  else icon = 100;

  Boolean updated = attributeUpdateNumber(percent, attribBattery, "%", 0);
  if (attributeUpdateNumber(icon, attribBatteryIcon, "%")) updated = true;
  if (attributeUpdateNumber(original, attribBatteryOrg, unitOrg)) updated = true;

  return (updated);
}

// ----------------------------

private Boolean attributeUpdateLowestBattery() {
  BigDecimal percent = 100;
  String org = "0";
  Integer type = 0;

  BigDecimal temp = device.currentValue("batteryTemp") as BigDecimal;
  BigDecimal rain = device.currentValue("batteryRain") as BigDecimal;
  BigDecimal wind = device.currentValue("batteryWind") as BigDecimal;

  if (temp != null) {
    percent = temp;
    org = device.currentValue("batteryTempOrg") as String;
    type = 0; 
  }
  
  if (rain != null && rain < percent) {
    percent = rain;
    org = device.currentValue("batteryRainOrg") as String;
    type = 1; 
  }

  if (wind != null && wind < percent) {
    percent = wind;
    org = device.currentValue("batteryWindOrg") as String;
    type = 1; 
  }

  return (attributeUpdateBattery(org, "battery", "batteryIcon", "batteryOrg", type));
}

// ------------------------------------------------------------

private Boolean attributeUpdateTemperature(String val, String attribTemperature) {
  
  BigDecimal degrees = val.toBigDecimal();
  String measure = "°F";

  // Convert to metric if requested
  if (unitSystemIsMetric()) {
    degrees = convert_F_to_C(degrees);
    measure = "°C";
  }

  return (attributeUpdateNumber(degrees, attribTemperature, measure, 1));
}

// ------------------------------------------------------------

private Boolean attributeUpdateHumidity(String val, String attribHumidity) {
  
  BigDecimal percent = val.toBigDecimal();

  return (attributeUpdateNumber(percent, attribHumidity, "%", 0));
}

// ------------------------------------------------------------

private Boolean attributeUpdatePressure(String val, String attribPressure, String attribPressureAbs) {
  
  // Get unit system
  Boolean metric = unitSystemIsMetric();

  // Get pressure in hectopascal
  BigDecimal absolute = convert_inHg_to_hPa(val.toBigDecimal());

  // Get altitude in meters
  val = settings.localAltitude;
  if (!val) {
    // First time: initialize and show the preference
    val = metric? "0 m": "0 ft";
    device.updateSetting("localAltitude", [value: val, type: "string"]);
  }

  BigDecimal altitude;
  try {
    String[] field = val.split();
    altitude = field[0].toBigDecimal();
    if (field.size() == 1) {
      // No unit found: let's use the parent setting
      if (!metric) altitude = convert_ft_to_m(altitude);
    }
    else {
      // Found a unit: convert accordingly
      if (field[1][0] == "f" || field[1][0] == "F") altitude = convert_ft_to_m(altitude);
    }
  }
  catch(Exception ignored) {
    altitude = 0;
  }

  // Get temperature in celsious
  BigDecimal temperature = (device.currentValue("temperature") as BigDecimal);
  if (temperature == null) temperature = 18;
  else if (!metric) temperature = convert_F_to_C(temperature);

  // Correct pressure to sea level using this conversion formula: https://keisan.casio.com/exec/system/1224575267
  BigDecimal relative = absolute * Math.pow(1 - ((altitude * 0.0065) / (temperature + (altitude * 0.0065) + 273.15)), -5.257);

  // Convert to imperial if requested
  if (metric) val = "hPa";
  else {
    absolute = convert_hPa_to_inHg(absolute);
    relative = convert_hPa_to_inHg(relative);
    val = "inHg";
  }

  Boolean updated = attributeUpdateNumber(relative, attribPressure, val, 2);
  if (attributeUpdateNumber(absolute, attribPressureAbs, val, 2)) updated = true;

  return (updated);
}

// ------------------------------------------------------------

private Boolean attributeUpdateRain(String val, String attribRain, Boolean hour = false) {
  
  BigDecimal amount = val.toBigDecimal();
  String measure = hour? "in/h": "in";

  // Convert to metric if requested
  if (unitSystemIsMetric()) {
    amount = convert_in_to_mm(amount);
    measure = hour? "mm/h": "mm";
  }

  return (attributeUpdateNumber(amount, attribRain, measure, 2));
}

// ------------------------------------------------------------

private Boolean attributeUpdatePM25(String val, String attribPm25, String attribAqi, String attribAqiDanger, String attribAqiColor) {
  //
  // Conversions based on https://en.wikipedia.org/wiki/Air_quality_index
  //
  BigDecimal pm25 = val.toBigDecimal();

  BigDecimal aqi;
  String danger;
  String color;

  if (pm25 < 12.1) {
    aqi = convertRange(pm25, 0, 12, 0, 50);
    danger = "Good";
    color = "3ea72d";
  }
  else if (pm25 < 35.5) {
    aqi = convertRange(pm25, 12.1, 35.4, 51, 100);
    danger = "Moderate";
    color = "fff300";
  }
  else if (pm25 < 55.5) {
    aqi = convertRange(pm25, 35.5, 55.4, 101, 150);
    danger = "Unhealthy for Sensitive Groups";
    color = "f18b00";
  }
  else if (pm25 < 150.5) {
    aqi = convertRange(pm25, 55.5, 150.4, 151, 200);
    danger = "Unhealthy";
    color = "e53210";
  }
  else if (pm25 < 250.5) {
    aqi = convertRange(pm25, 150.5, 250.4, 201, 300);
    danger = "Very Unhealthy";
    color = "b567a4";
  }
  else if (pm25 < 350.5) {
    aqi = convertRange(pm25, 250.5, 350.4, 301, 400);
    danger = "Hazardous";
    color = "7e0023";
  }
  else {
    aqi = convertRange(pm25, 350.5, 500.4, 401, 500);
    danger = "Hazardous";
    color = "7e0023";
  }

  Boolean updated = attributeUpdateNumber(pm25, attribPm25, "µg/m³");
  if (attributeUpdateNumber(aqi, attribAqi, "AQI")) updated = true;
  if (attributeUpdateString(danger, attribAqiDanger)) updated = true;
  if (attributeUpdateString(color, attribAqiColor)) updated = true;

  return (updated);
}

// ------------------------------------------------------------

private Boolean attributeUpdateLeak(String val, String attribWater, String attribLeak, String attribLeakMsg, String attribLeakColor) {

  BigDecimal leak = (val.toBigDecimal())? 1: 0;
  String water, message, color;

  if (leak) {
    water = "wet";
    leak = 1;
    message = "Leak detected!";
    color = "ff0000";
  }
  else {
    water = "dry";
    leak = 0;
    message = "Dry";
    color = "ffffff";
  }

  Boolean updated = attributeUpdateString(water, attribWater);
  if (attributeUpdateNumber(leak, attribLeak)) updated = true;
  if (attributeUpdateString(message, attribLeakMsg)) updated = true;
  if (attributeUpdateString(color, attribLeakColor)) updated = true;

  return (updated);
}

// ------------------------------------------------------------

private Boolean attributeUpdateLightningDistance(String val, String attrib) {

  if (!val) return (attributeUpdateString("n/a", attrib));

  BigDecimal distance = val.toBigDecimal();
  String measure = "km";

  // Convert to imperial if requested
  if (unitSystemIsMetric() == false) {
    distance = convert_km_to_mi(distance);
    measure = "mi";
  }

  return (attributeUpdateNumber(distance, attrib, measure, 1));
}

// ------------------------------------------------------------

private Boolean attributeUpdateLightningCount(String val, String attrib) {

  if (!val) return (attributeUpdateString("n/a", attrib));

  return (attributeUpdateNumber(val.toBigDecimal(), attrib));
}

// ------------------------------------------------------------

private Boolean attributeUpdateLightningTime(String val, String attrib) {

  if (!val) return (attributeUpdateString("n/a", attrib));

  return (attributeUpdateString(timeEpochToLocal(val), attrib));
}

// ------------------------------------------------------------

private Boolean attributeUpdateUV(String val, String attribUvIndex, String attribUvDanger, String attribUvColor) {
  //
  // Conversions based on https://en.wikipedia.org/wiki/Ultraviolet_index
  // 
  BigDecimal index = val.toBigDecimal();

  String danger;
  String color;

  if (index < 3)       { danger = "Low";       color = "3ea72d"; }
  else if (index < 6)  { danger = "Medium";    color = "fff300"; }
  else if (index < 8)  { danger = "High";      color = "f18b00"; }
  else if (index < 11) { danger = "Very High"; color = "e53210"; }
  else                 { danger = "Extreme";   color = "b567a4"; }
    
  Boolean updated = attributeUpdateNumber(index, attribUvIndex, "uvi");
  if (attributeUpdateString(danger, attribUvDanger)) updated = true;
  if (attributeUpdateString(color, attribUvColor)) updated = true;

  return (updated);
}

// ------------------------------------------------------------

private Boolean attributeUpdateLight(String val, String attribSolarRadiation, String attribIlluminance) {

  BigDecimal light = val.toBigDecimal();

  Boolean updated = attributeUpdateNumber(light, attribSolarRadiation, "W/m²");
  if (attributeUpdateNumber(convert_Wm2_to_lux(light), attribIlluminance, "lux", 0)) updated = true;

  return (updated);
}

// ------------------------------------------------------------

private Boolean attributeUpdateWindSpeed(String val, String attribWindSpeed) {
  
  BigDecimal speed = val.toBigDecimal();
  String measure = "mph";

  // Convert to metric if requested
  if (unitSystemIsMetric()) {
    speed = convert_mi_to_km(speed);
    measure = "km/h";
  }

  return (attributeUpdateNumber(speed, attribWindSpeed, measure, 1));
}

// ------------------------------------------------------------

private Boolean attributeUpdateWindDirection(String val, String attribWindDirection, String attribWindCompass) {
  
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
  
  Boolean updated = attributeUpdateNumber(direction, attribWindDirection, "°");
  if (attributeUpdateString(compass, attribWindCompass)) updated = true;

  return (updated);
}

// ------------------------------------------------------------

private Boolean attributeUpdateDewPoint(String val, String attribDewPoint) {
  Boolean updated = false;

  BigDecimal temperature = (device.currentValue("temperature") as BigDecimal);
  if (temperature != null) {
    if (unitSystemIsMetric()) {
      // Convert temperature back to F
      temperature = convert_C_to_F(temperature);
    }  

    BigDecimal humidity = (device.currentValue("humidity") as BigDecimal);
    if (humidity != null) {
      // Calculate dewPoint based on https://en.wikipedia.org/wiki/Dew_point
      BigDecimal degrees = temperature - (0.36 * (100 - humidity));
      if (attributeUpdateTemperature(degrees.toString(), attribDewPoint)) updated = true;
    }
  }

  return (updated);
}

// ------------------------------------------------------------

private Boolean attributeUpdateHeatIndex(val, attribHeatIndex, attribHeatDanger, attribHeatColor) {
  Boolean updated = false;

  BigDecimal temperature = (device.currentValue("temperature") as BigDecimal);
  if (temperature != null) {
    if (unitSystemIsMetric()) {
      // Convert temperature back to F
      temperature = convert_C_to_F(temperature);
    }  

    BigDecimal humidity = (device.currentValue("humidity") as BigDecimal);
    if (humidity != null) {
      // Calculate heatIndex based on https://en.wikipedia.org/wiki/Heat_index
      BigDecimal degrees = -42.379 +
                          (  2.04901523 * temperature) +
                          ( 10.14333127 * humidity) -
                          (  0.22475541 * (temperature * humidity)) -
                          (  0.00683783 * (temperature ** 2)) -
                          (  0.05481717 * (humidity ** 2)) +
                          (  0.00122874 * ((temperature ** 2) * humidity)) +
                          (  0.00085282 * (temperature * (humidity ** 2))) -
                          (  0.00000199 * ((temperature ** 2) * (humidity ** 2)));
      String danger;
      String color;

      if (degrees < 80)       { danger = "Safe";            color = "ffffff"; }
      else if (degrees < 91)  { danger = "Caution";         color = "ffff66"; }
      else if (degrees < 104) { danger = "Extreme Caution"; color = "ffd700"; }
      else if (degrees < 126) { danger = "Danger";          color = "ff8c00"; }
      else                    { danger = "Extreme Danger";  color = "ff0000"; }
    
      updated = attributeUpdateTemperature(degrees.toString(), attribHeatIndex);
      if (attributeUpdateString(danger, attribHeatDanger)) updated = true;
      if (attributeUpdateString(color, attribHeatColor)) updated = true;
    }
  }

  return (updated);
}

// ------------------------------------------------------------

private Boolean attributeUpdateWindChill(val, attribWindChill, attribWindDanger, attribWindColor) {
  Boolean updated = false;

  BigDecimal temperature = (device.currentValue("temperature") as BigDecimal);
  if (temperature != null) {
    if (unitSystemIsMetric()) {
      // Convert temperature back to F
      temperature = convert_C_to_F(temperature);
    } 
    
    BigDecimal windSpeed = (device.currentValue("windSpeed") as BigDecimal);
    if (windSpeed != null) {
      // Calculate windChill based on https://en.wikipedia.org/wiki/Wind_chill
      BigDecimal degrees = 35.74 +
                          ( 0.6215 * temperature) -
                          (35.75 * (windSpeed ** 0.16)) +
                          ((0.4275 * temperature) * (windSpeed ** 0.16));
      String danger;
      String color;

      if (degrees < -69)      { danger = "Frostbite certain";  color = "2d2c52"; }
      else if (degrees < -19) { danger = "Frostbite likely";   color = "1f479f"; }
      else if (degrees < 1)   { danger = "Frostbite possible"; color = "0c6cb5"; }
      else if (degrees < 21)  { danger = "Very Unpleasant";    color = "2f9fda"; }
      else if (degrees < 41)  { danger = "Unpleasant";         color = "9dc8e6"; }
      else                    { danger = "Safe";               color = "ffffff"; }

      updated = attributeUpdateTemperature(degrees.toString(), attribWindChill);
      if (attributeUpdateString(danger, attribWindDanger)) updated = true;
      if (attributeUpdateString(color, attribWindColor)) updated = true;
    }
  }

  return (updated);
}

// ------------------------------------------------------------

private Boolean attributeUpdateHtml(String templHtml, String attribHtml) {

  Boolean updated = false;

  String pattern = /\$\{([^}]+)\}/;

  String index;
  String val;

  for (Integer idx = 0; idx < 16; idx++) {
    index = idx? "${idx}": "";

    val = device.getDataValue("${templHtml}${index}");
    if (!val) break;

    val = val.replaceAll(~pattern) { java.util.ArrayList match -> (device.currentValue(match[1].trim()) as String); }
    if (attributeUpdateString(val, "${attribHtml}${index}")) updated = true;
  }

  return (updated);
}

// ------------------------------------------------------------

Boolean attributeUpdate(String key, String val) {
  //
  // Dispatch attributes changes to hub
  //
  
  Boolean updated = false;

  switch (key) {

  case "wh26batt":
    if (device.getDeviceNetworkId() != "WH26") {
      state.compounded = true;
      updated = attributeUpdateBattery(val, "batteryTemp", "batteryTempIcon", "batteryTempOrg", 0);  // !boolean
    }
    else updated = attributeUpdateBattery(val, "battery", "batteryIcon", "batteryOrg", 0);
    break;

  case "wh40batt":
    if (device.getDeviceNetworkId() != "WH40") {
      state.compounded = true;
      updated = attributeUpdateBattery(val, "batteryRain", "batteryRainIcon", "batteryRainOrg", 1);  // voltage
    }
    else updated = attributeUpdateBattery(val, "battery", "batteryIcon", "batteryOrg", 1);
    break;

  case "wh68batt":
  case "wh80batt":
    if (device.getDeviceNetworkId() != "WH80") {
      state.compounded = true;
      updated = attributeUpdateBattery(val, "batteryWind", "batteryWindIcon", "batteryWindOrg", 1);  // voltage
    }
    else updated = attributeUpdateBattery(val, "battery", "batteryIcon", "batteryOrg", 1);
    break;

  case ~/batt[1-8]/:
  case "wh25batt":
  case "wh65batt":
    updated = attributeUpdateBattery(val, "battery", "batteryIcon", "batteryOrg", 0);  // !boolean
    break;

  case ~/soilbatt[1-8]/:
    updated = attributeUpdateBattery(val, "battery", "batteryIcon", "batteryOrg", 1);  // voltage
    break;

  case ~/pm25batt[1-4]/:
  case ~/leakbatt([1-4])/:
  case "wh57batt":
    updated = attributeUpdateBattery(val, "battery", "batteryIcon", "batteryOrg", 2);  // 0 - 5
    break;
  
  case "tempinf":
  case "tempf":
  case ~/temp[1-8]f/:
    updated = attributeUpdateTemperature(val, "temperature");
    break;

  case "humidityin":
  case "humidity":
  case ~/humidity[1-8]/:
  case ~/soilmoisture[1-8]/:
    updated = attributeUpdateHumidity(val, "humidity");
    break;

  case "baromrelin":
    // we ignore this value as we do our own correction
    break;

  case "baromabsin":
    updated = attributeUpdatePressure(val, "pressure", "pressureAbs");
    break;

  case "rainratein":
    updated = attributeUpdateRain(val, "rainRate", true);
    break;

  case "eventrainin":
    updated = attributeUpdateRain(val, "rainEvent");
    break;

  case "hourlyrainin":
    updated = attributeUpdateRain(val, "rainHourly");
    break;

  case "dailyrainin":
    updated = attributeUpdateRain(val, "rainDaily");
    break;

  case "weeklyrainin":
    updated = attributeUpdateRain(val, "rainWeekly");
    break;

  case "monthlyrainin":
    updated = attributeUpdateRain(val, "rainMonthly");
    break;

  case "yearlyrainin":
    updated = attributeUpdateRain(val, "rainYearly");
    break;

  case "totalrainin":
    updated = attributeUpdateRain(val, "rainTotal");
    break;

  case ~/pm25_ch[1-4]/:
    updated = attributeUpdatePM25(val, "pm25", "aqi", "aqiDanger", "aqiColor");
    break;

  case ~/pm25_avg_24h_ch[1-4]/:
    updated = attributeUpdatePM25(val, "pm25_avg_24h", "aqi_avg_24h", "aqiDanger_avg_24h", "aqiColor_avg_24h");
    break;

  case ~/leak_ch([1-4])/:
    updated = attributeUpdateLeak(val, "water", "leak", "leakMsg", "leakColor");
    break;

  case "lightning":
    updated = attributeUpdateLightningDistance(val, "lightningDistance");
    break;

  case "lightning_num":
    updated = attributeUpdateLightningCount(val, "lightningCount");
    break;

  case "lightning_time":
    updated = attributeUpdateLightningTime(val, "lightningTime");
    break;

  case "uv":
    updated = attributeUpdateUV(val, "ultravioletIndex", "ultravioletDanger", "ultravioletColor");
    break;

  case "solarradiation":
    updated = attributeUpdateLight(val, "solarRadiation", "illuminance");
    break;

  case "winddir":
    updated = attributeUpdateWindDirection(val, "windDirection", "windCompass");
    break;

  case "winddir_avg10m":
    updated = attributeUpdateWindDirection(val, "windDirection_avg_10m", "windCompass_avg_10m");
    break;

  case "windspeedmph":
    updated = attributeUpdateWindSpeed(val, "windSpeed");
    break;

  case "windspdmph_avg10m":
    updated = attributeUpdateWindSpeed(val, "windSpeed_avg_10m");
    break;

  case "windgustmph":
    updated = attributeUpdateWindSpeed(val, "windGust");
    break;

  case "maxdailygust":
    updated = attributeUpdateWindSpeed(val, "windGustMaxDaily");
    break;

  //
  // Internal calculated commands
  //
  case "inferdewpoint":
    if (state.status) updated = attributeUpdateDewPoint(val, "dewPoint");
    break;

  case "inferheatindex":
    if (state.status) updated = attributeUpdateHeatIndex(val, "heatIndex", "heatDanger", "heatColor");
    break;

  case "inferwindchill":
    if (state.status) updated = attributeUpdateWindChill(val, "windChill", "windDanger", "windColor");
    break;

  case "endofdata":
    // If we are a compounded sensor, at the endofdata we update the "virtual" battery with the lowest of all the "physical" batteries
    if (state.compounded) attributeUpdateLowestBattery();

    // Last thing we do on the driver: update status
    if (!state.status) {
      // Last round we have not received any data
      ztatus("Orphaned", "orange");
      state.status = 0;
    }
    else {
      // Receiving data or user error
      updated = attributeUpdateHtml("htmlTemplate", "html");
      if (attributeUpdateString(val, "time")) updated = true;

      if (state.status != -1) {
        // If no user error pending we trigger a starving next cycle if we don't receive data
        ztatus("OK", "green");
        state.status = 0;
      }
    }

    break;

  default:
    logDebug("Unrecognized attribute: ${key} = ${val}");
    break;
  }

  return (updated);
}

// HTML templates --------------------------------------------------------------------------------------------------------------

private Object htmlGetRepository() {
  //
  // Return an Object containing all the templates
  // or null if something went wrong
  //
  Object repository = null;

  try {
    String repositoryText = "https://mircolino.github.io/ecowitt/ecowitt.json".toURL().getText();
    if (repositoryText) {
      // text -> json
      Object parser = new groovy.json.JsonSlurper();
      repository = parser.parseText(repositoryText);
    }
  }
  catch (Exception e) {
    logError("Exception in versionUpdate(): ${e}");
  }

  return (repository);
}

// ------------------------------------------------------------

private Integer htmlCountAttributes(String htmlAttrib) {
  //
  // Return the number of html attributes the driver has
  //
  Integer count = 0;

  // Get a list of all attributes (present/null or not)
  List<String> attribDrv = attributeEnumerate(false);
  String attrib;

  for (Integer idx = 0; idx < 16; idx++) {
    attrib = idx? "${htmlAttrib}${idx}": htmlAttrib;

    if (attribDrv.contains(attrib) == false) break;
    count++;
  }

  return (count);
} 

// ------------------------------------------------------------

private Boolean htmlSetAttributes(String val, String htmlAttrib, Integer count, Boolean onlyPresent) {

  Boolean updated = false;

  String attrib;

  for (Integer idx = 0; idx < count; idx++) {
    attrib = idx? "${htmlAttrib}${idx}": htmlAttrib;

    if (onlyPresent == false || device.currentValue(attrib) != null) {
      if (attributeUpdateString(val, attrib)) updated = true;
    }
  }

  return (updated);
}

// ------------------------------------------------------------

private Integer htmlValidateTemplate(String htmlTempl, String htmlAttrib, Integer count) {
  //
  // Return  <0) number of invalid attributes in "htmlTempl"
  //        >=0) number of valid attributes in "htmlTempl"
  // Template is valid only if return > 0
  //
  String pattern = /\$\{([^}]+)\}/;

  // Build a list of valid attributes names excluding the null ones and ourself (for obvious reasons)
  List<String> attribDrv = attributeEnumerate();
  String attrib;

  for (Integer idx = 0; idx < count; idx++) {
    attrib = idx? "${htmlAttrib}${idx}": htmlAttrib;

    attribDrv.remove(attrib);
  }

  // Go through all the ${attribute} expressions in the htmlTempl and collect both good and bad ones
  List<String> attribOk = [];
  List<String> attribErr = [];

  htmlTempl.findAll(~pattern) { java.util.ArrayList match -> 
    attrib = match[1].trim();  

    if (attribDrv.contains(attrib)) attribOk.add(attrib);
    else attribErr.add(attrib);
  }

  if (attribErr.size() != 0) return (-attribErr.size());
  return (attribOk.size());
}

// ------------------------------------------------------------

private List<String> htmlGetUserInput(String input, Integer count) {
  //
  // Return null if user input is null or empty
  // Return empty list if user input is invalid: template(s) not found, duplicates, too many, etc.
  // Otherwise return a list of (unvalidated) templates entered by the user
  //
  if (!input) return (null);

  List<String> templateList = [];

  if (input.find(/[<>{};:=\'\"#&\$]/)) {
    // If input has at least one typical html character, then it's a real template
    templateList.add(input);
  }
  else {
    // Input is an array of repository template IDs
    List<String> idList = input.tokenize("[, ]");
    if (idList) {
      // We found at least one template ID in the user input, make sure they are not too many
      Object repository = htmlGetRepository();
      if (repository) {
        Boolean metric = unitSystemIsMetric();

        for (Integer idx = 0; idx < idList.size(); idx++) {
          // Try first the normal templates
          input = repository.templates."${idList[idx]}";

          // If not found try the unit templates
          if (!input) input = metric? repository.templatesMetric."${idList[idx]}": repository.templatesImperial."${idList[idx]}";

          // If still not found, or already found, or exceeded number of templates, return error
          if (!input || templateList.contains(input) || templateList.size() == count) return ([]);

          // Good one, let's add it
          templateList.add(input);
        }
      }
    }
  }

  return (templateList);
}

// ------------------------------------------------------------

private String htmlUpdateUserInput(String input) {
  //
  // Return true if HTML templates have been pre-processed sucesfully
  //
  String htmlTemplate = "htmlTemplate";
  String htmlAttrib = "html";

  String template;

  // Get the maximum number of supported templates
  Integer count = htmlCountAttributes(htmlAttrib);

  if (!count) {
    // Return if we do not support HTML templates 
    return ("");
  }

  // Cleanup previous states
  htmlSetAttributes("n/a", htmlAttrib, count, true);

  for (Integer idx = 0; idx < count; idx++) {
    template = idx? "${htmlTemplate}${idx}": htmlTemplate;

    if (device.getDataValue(template)) device.updateDataValue(template, null);
    device.data.remove(template);
  }

  // Parse user input
  List<String> templateList = htmlGetUserInput(input, count);
  if (templateList == null) {
    // Templates are disabled/empty
    return ("");
  }

  if (templateList.size() == 0) {
    // Invalid user input
    return ("Invalid template(s) id, count or repetition");
  }

  for (Integer idx = 0; idx < templateList.size(); idx++) {
    // We have valid templates: let's validate them    
    if (htmlValidateTemplate(templateList[idx], htmlAttrib, count) < 1) {
      // Invalid or no attribute in template
      return ("Invalid attribute or template for the current sensor");    
    }
  }

  // Finally! We have a (1 <= number <= count) of valid templates: let's write them down
  for (Integer idx = 0; idx < templateList.size(); idx++) {
    template = idx? "${htmlTemplate}${idx}": htmlTemplate;

    device.updateDataValue(template, templateList[idx]);
  }

  htmlSetAttributes("pending", htmlAttrib, templateList.size(), false);

  return ("");
}

// Driver lifecycle -----------------------------------------------------------------------------------------------------------

void installed() {
  try {
    logDebug("addedSensor(${device.getDeviceNetworkId()})");
  }
  catch (Exception e) {
    logError("Exception in installed(): ${e}");
  }
}

// ------------------------------------------------------------

void updated() {
  try {
    // Clear previous states
    state.clear();

    // Pre-process HTML templates (if any)
    String error = htmlUpdateUserInput(settings.htmlTemplate as String);
    if (error) {
      ztatus(error, "red");
      state.status = -1;
    }
    else {
      ztatus("OK", "green");
      state.status = 0;
    }
  }
  catch (Exception e) {
    logError("Exception in updated(): ${e}");
  }
}

// ------------------------------------------------------------

void uninstalled() {
  try {
    // Notify the parent we are being deleted
    getParent().uninstalledChildDevice(device.getDeviceNetworkId());

    logDebug("deletedSensor(${device.getDeviceNetworkId()})");
  }
  catch (Exception e) {
    logError("Exception in uninstalled(): ${e}");
  }
}

// ------------------------------------------------------------

void parse(String msg) {
  try {
  }
  catch (Exception e) {
    logError("Exception in parse(): ${e}");
  }
}

// Recycle bin ----------------------------------------------------------------------------------------------------------------

/*


*/

// EOF ------------------------------------------------------------------------------------------------------------------------
