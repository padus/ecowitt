### Repository Templates

***

##### ID: 0

<img src="https://github.com/mircolino/ecowitt/raw/master/images/T00.png" width="140" height="100">

```
<div><i class="ewi-temperature"></i> ${temperature} &deg;C<br><i class="ewi-humidity"></i> ${humidity} %</div>
```
Valid for:

- Indoor Weather Sensor
- Outdoor Weather Sensor
- Multi-channel Weather Sensor
- Wind Solar Sensor
- PWS Sensor

***

##### ID: 1

<img src="https://github.com/mircolino/ecowitt/raw/master/images/T01.png" width="140" height="100">

```
<div><i class="ewi-temperature"></i> ${temperature} &deg;F<br><i class="ewi-humidity"></i> ${humidity} %<br><i class="ewi-pressure"></i> ${pressure} inHg</div>
```
Valid for:

- Indoor Weather Sensor

***

##### ID: 2

<img src="https://github.com/mircolino/ecowitt/raw/master/images/T02.png" width="140" height="100">

```
<div><i class="ewi-air" style="color:#${aqiColor}"></i> ${aqiDanger}<br>PM2.5: ${pm25} &micro;g/m&sup3;<br>AQI: ${aqi}</div>
```
Valid for:

- Air Quality Sensor

***

##### ID: 3

<img src="https://github.com/mircolino/ecowitt/raw/master/images/T03.png" width="100" height="100">

```
<div><i class="ewi-windspeed"></i> ${windSpeed} mhp<br><i class="ewi-wind${windCompass}"></i> ${windDirection}&deg; ${windCompass}<br><i class="ewi-rain"></i> ${rainRate} in/h<br><i class="ewi-light"></i> ${illuminance} lux<br>UV ${ultravioletDanger}</div>
```
Valid for:

- PWS Sensor

***
