### Templates Repository

***

##### ID: 0

<img src="https://github.com/padus/ecowitt/raw/main/images/T00.png" width="140" height="100">

```
<i class="ewi-temperature"></i> ${temperature} &deg;F<br><i class="ewi-humidity"></i> ${humidity} %
```
Valid for:

- Indoor Weather Sensor
- Outdoor Weather Sensor
- Multi-channel Weather Sensor
- Wind Solar Sensor
- PWS Sensor

***

##### ID: 1

<img src="https://github.com/padus/ecowitt/raw/main/images/T01.png" width="140" height="100">

```
<i class="ewi-temperature"></i> ${temperature} &deg;F<br><i class="ewi-humidity"></i> ${humidity} %<br><i class="ewi-pressure"></i> ${pressure} inHg
```
Valid for:

- Indoor Weather Sensor

***

##### ID: 2

<img src="https://github.com/padus/ecowitt/raw/main/images/T02.png" width="140" height="100">

```
<i class="ewi-air" style="color:#${aqiColor}"></i> ${aqiDanger}<br>PM2.5: ${pm25} &micro;g/m&sup3;<br>AQI: ${aqi}
```
Valid for:

- Air Quality Sensor

***

##### ID: 3

<img src="https://github.com/padus/ecowitt/raw/main/images/T03.png" width="100" height="100">

```
<i class="ewi-windspeed"></i> ${windSpeed} mph<br><i class="ewi-wind${windCompass}"></i> ${windDirection}&deg; ${windCompass}<br><i class="ewi-rain"></i> ${rainRate} in/h<br><i class="ewi-light"></i> ${illuminance} lux<br>UV ${ultravioletDanger}
```
Valid for:

- PWS Sensor

***

##### ID: 6

<img src="https://github.com/padus/ecowitt/raw/main/images/T06.png" width="140" height="100">

```
<i class="ewi-temperature"></i>${temperature} &deg;F<br><i class="ewi-humidity"></i>${humidity} %<br><i class="ewi-dew"></i>${dewPoint} &deg;F<br><i class="ewi-heat"></i>${simmerIndex} SSI<br><p style="color:#${simmerColor}">${simmerDanger}</p>
```
Valid for:

- Sensors with both Temperature and Relative Humidity

***
