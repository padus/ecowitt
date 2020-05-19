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

    // command "test1";
    // command "test2";

 // attribute "battery", "number";                             // 0-100%
    attribute "batteryOrg", "number";                          // original/un-translated battery value returned by the sensor 

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

    attribute "html", "string";                                // e.g. "<p>Temperature: ${temperature}°F</p><p>Humidity: ${humidity}%</p>"
  }

  preferences {
    input(name: "htmlTemplate", type: "string", title: "<font style='font-size:12px; color:#1a77c9'>HTML Tile Template</font>", description: "<font style='font-size:12px; font-style: italic'>An HTML snippet formatted with one or more \${attribute} as described <a href='https://github.com/mircolino/ecowitt/blob/master/readme.md#templates' target='_blank'>here</a></font>", defaultValue: "");
  }
}

/*
 * State variables used by the driver
 *
 * "HTML Template Error"                                        // User error notification
 */

// Logging --------------------------------------------------------------------------------------------------------------------

private void logError(String str) { log.error(str); }
private void logWarning(String str) { if (getParent().logGetLevel() > 0) log.warn(str); }
private void logInfo(String str) { if (getParent().logGetLevel() > 1) log.info(str); }
private void logDebug(String str) { if (getParent().logGetLevel() > 2) log.debug(str); }
private void logTrace(String str) { if (getParent().logGetLevel() > 3) log.trace(str); }

// Conversions ----------------------------------------------------------------------------------------------------------------

private BigDecimal convertRange(BigDecimal val, BigDecimal inMin, BigDecimal inMax, BigDecimal outMin, BigDecimal outMax, Boolean returnInt = true) {
  // Let make sure ranges are correct
  assert (inMin <= inMax);
  assert (outMin <= outMax);

  // Restrain input value
  if (val < inMin) val = inMin;
  else if (val > inMax) val = inMax;

  val = ((val - inMin) * (outMax - outMin)) / (inMax - inMin);
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
  return (val * 0.00029529983071445);
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
  // Never return null
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

private Boolean attributeInvalidate(String attribute = null) {
  //
  // Invalidate ("n/a") "attribute" if != null or all the attributes if == null
  // Invalidate only those that have been already created (non-null ones)
  // Return true if (any) "attribute" has actually been updated
  //
  Boolean invalidated = false;

  if (attribute) {
    if (device.currentValue(attribute) != null) invalidated = attributeUpdateString("n/a", attribute);
  }
  else {
    /* List<String> */ attributeEnumerate().each { if (attributeUpdateString("n/a", it)) invalidated = true; }
  }

  return (invalidated);
}

// ------------------------------------------------------------

private Boolean attributeUpdateBattery(String val, String attribBattery, String attribBatteryOrg, Integer type) {
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

  Boolean updated = attributeUpdateNumber(percent, attribBattery, "%", 0);
  if (attributeUpdateNumber(original, attribBatteryOrg, unitOrg)) updated = true;

  return (updated);
}

// ------------------------------------------------------------

private Boolean attributeUpdateTemperature(String val, String attribTemperature) {
  
  BigDecimal degrees = val.toBigDecimal();
  String measure = "°F";

  // Convert to metric if requested
  if (getParent().unitSystemIsMetric()) {
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

private Boolean attributeUpdatePressure(String val, String attribPressure) {
  
  BigDecimal length = val.toBigDecimal();
  String measure = "inHg";

  // Convert to metric if requested
  if (getParent().unitSystemIsMetric()) {
    length = convert_inHg_to_hPa(length);
    measure = "hPa";
  }

  return (attributeUpdateNumber(length, attribPressure, measure, 2));
}

// ------------------------------------------------------------

private Boolean attributeUpdateRain(String val, String attribRain, Boolean hour = false) {
  
  BigDecimal amount = val.toBigDecimal();
  String measure = hour? "in/h": "in";

  // Convert to metric if requested
  if (getParent().unitSystemIsMetric()) {
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
  if (getParent().unitSystemIsMetric()) {
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
    if (getParent().unitSystemIsMetric()) {
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
    if (getParent().unitSystemIsMetric()) {
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
    if (getParent().unitSystemIsMetric()) {
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

private Boolean attributeUpdateHtml(String val, String attribHtml, Boolean checkSyntax = false) {

  Boolean updated = false;

  String error = "HTML Template Error";

  if (state."${error}" == null && val) {

    String pattern = /\$\{([^}]+)\}/;

    if (checkSyntax) {
      // Check template syntax: between ${} we only allow valid child attributes
      
      // Build a list of valid attributes names excluding the null ones and ourself (for obvious reasons)
      List<String> attribDrv = attributeEnumerate();
      attribDrv.remove(attribHtml);

      // Go through all the ${attribute} expressions in the template and collect both good and bad ones
      List<String> attribOk = [];
      List<String> attribErr = [];
      String attrib;

      val.findAll(~pattern) { java.util.ArrayList match -> 
        attrib = match[1].trim();  

        if (attribDrv.contains(attrib)) attribOk.add(attrib);
        else attribErr.add(attrib);
      }

      if (attribErr.size() != 0) state."${error}" = "<font style='color:#ff0000'>\"${attribErr[0]}\" is not a valid attribute.</font>";
      else if (attribOk.size() == 0) state."${error}" = "<font style='color:#ff0000'>No valid attributes found.</font>";

      // Invalidate (without creating it) the current html attribute if an error is pending
      if (state."${error}" != null) attributeInvalidate(attribHtml);
      else updated = attributeUpdateString("pending", attribHtml);
      // else updated = true;
    }
    else {
      // Expand template  
      // Coerce Object -> String
      val = val.replaceAll(~pattern) { java.util.ArrayList match -> (device.currentValue(match[1].trim()) as String); }
      updated = attributeUpdateString(val, attribHtml);
    }
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

  case ~/soilbatt[1-8]/:
    updated = attributeUpdateBattery(val, "battery", "batteryOrg", 1);
    break;
  
  case ~/pm25batt[1-4]/:
    updated = attributeUpdateBattery(val, "battery", "batteryOrg", 2);
    break;
  
  case "wh25batt":
  case "wh26batt":
  case ~/batt[1-8]/:
  case "wh40batt": 
  case "wh65batt":
    updated = attributeUpdateBattery(val, "battery", "batteryOrg", 0);
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
    updated = attributeUpdatePressure(val, "pressure");
    break;

  case "baromabsin":
    updated = attributeUpdatePressure(val, "pressureAbs");
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
    updated = attributeUpdateDewPoint(val, "dewPoint");
    break;

  case "inferheatindex":
    updated = attributeUpdateHeatIndex(val, "heatIndex", "heatDanger", "heatColor");
    break;

  case "inferwindchill":
    updated = attributeUpdateWindChill(val, "windChill", "windDanger", "windColor");
    break;

  case "htmltemplate":
    updated = attributeUpdateHtml(settings.htmlTemplate as String, "html");
    break;

  case "attribinvalidate":
    updated = attributeInvalidate();
    break;

  default:
    logDebug("Unrecognized attribute: ${key} = ${val}");
    break;
  }

  return (updated);
}

// Commands -------------------------------------------------------------------------------------------------------------------

/*

void test1() {
  try {
    logDebug("test1()");

    //-------------------------


    //-------------------------
  }
  catch (Exception e) {
    logError("Exception in test1(): ${e}");
  }
} 

// ------------------------------------------------------------

void test2() {
  try {
    logDebug("test2()");

    //-------------------------


    //-------------------------
  }
  catch (Exception e) {
    logError("Exception in test2(): ${e}");
  }
} 

*/

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

    // Check HTML template syntax (if any)
    attributeUpdateHtml(settings.htmlTemplate as String, "html", true);
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

// Recycle --------------------------------------------------------------------------------------------------------------------

/*

private Boolean attributeIsClassSupported(Integer class) {
  //
  // Return 'true' if one of the following classes of attributes is supported by the current sensor
  //
  //10) Lightning -------------------------------------------------------------------------
  // 9) Water (leak) ---------------------------------------------------------------       |
  // 8) Pollution (air) -----------------------------------------------------       |      |
  // 7) Moisture (soil) ----------------------------------------------       |      |      |
  // 6) Light -------------------------------------------------       |      |      |      |
  // 5) UV ---------------------------------------------       |      |      |      |      |
  // 4) Wind ------------------------------------       |      |      |      |      |      |
  // 3) Rain -----------------------------       |      |      |      |      |      |      |
  // 2) Pressure ------------------       |      |      |      |      |      |      |      |
  // 1) Humidity (air) -----       |      |      |      |      |      |      |      |      |
  // 0) Temperature -       |      |      |      |      |      |      |      |      |      |
  //                 0      1      2      3      4      5      6      7      8      9      10
  Boolean[] WH25 = [true,  true,  true,  false, false, false, false, false, false, false, false];
  Boolean[] WH26 = [true,  true,  false, false, false, false, false, false, false, false, false];
  Boolean[] WH31 = [true,  true,  false, false, false, false, false, false, false, false, false];
  Boolean[] WH40 = [false, false, false, true,  false, false, false, false, false, false, false];
  Boolean[] WH41 = [false, false, false, false, false, false, false, false, true,  false, false];
  Boolean[] WH51 = [false, false, false, false, false, false, false, true,  false, false, false];
  Boolean[] WH55 = [false, false, false, false, false, false, false, false, false, true,  false];
  Boolean[] WH57 = [false, false, false, false, false, false, false, false, false, false, true ];
  Boolean[] WH80 = [true,  true,  false, false, true,  true,  true,  false, false, false, false];
  Boolean[] WH69 = [true,  true,  false, true,  true,  true,  true,  false, false, false, false];

  String model = device.getDeviceNetworkId().take(4).toUpperCase();
  return ("${model}"[class]);
}

*/

// EOF ------------------------------------------------------------------------------------------------------------------------
