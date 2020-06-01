## Ecowitt WiFi Gateway
*Ecowitt WiFi Gateway driver for Hubitat Elevation*

### Features

- LAN comunication only, no cloud/weather service needed.
- One Hubitat device for each Ecowitt sensor for easy dashboard tiles and RM rules handling.
- On-the-fly Imperial <-> Metric conversion.
- Tile [HTML templates](#templates), which allows endless tiles customization, including displaying multiple attributes in a single tile. 

### Installation Instructions

#### Ecowitt WS View:

1.  Make sure all your sensors are properly registered:  

    <img src="https://github.com/mircolino/ecowitt/raw/master/images/01.png" width="300" height="600">  

2.  <span>Setup a local/customized weather service as follow (replacing hostname/IP with your own):  

    <img src="https://github.com/mircolino/ecowitt/raw/master/images/02.png" width="300" height="600">

#### Hubitat: 

1.  If the Ecowitt Gateway has been setup correctly, every 5 minutes, you should see the following warning in the Hubitat system log:

    <img src="https://github.com/mircolino/ecowitt/raw/master/images/03.png">
    
    That's because this driver has not been installed yet and the hub has nowhere to forward the gateway data to.
    
2.  In "Drivers Code" add the Ecowitt [WiFi Gateway](https://raw.githubusercontent.com/mircolino/ecowitt/master/ecowitt_gateway.groovy) and [RF Sensor](https://raw.githubusercontent.com/mircolino/ecowitt/master/ecowitt_sensor.groovy) drivers:

    <img src="https://github.com/mircolino/ecowitt/raw/master/images/04.png">
    
3.  In "Devices" add a new "Ecowitt WiFi Gateway" virtual device and click "Save Device":

    <img src="https://github.com/mircolino/ecowitt/raw/master/images/05.png">

4.  Enter the Gateway MAC address (in any legal form) and click "Save Preferences":

    <img src="https://github.com/mircolino/ecowitt/raw/master/images/06.png">

5.  That should be all.
    The first time Hubitat receives data from the Gateway, the driver will automatically create child devices for all the present (and supported) sensors (depending on the frequency you setup your Gateway to send data, this may take a few minutes):
    
    <img src="https://github.com/mircolino/ecowitt/raw/master/images/07.png">

### <a name="templates"></a> HTML Template:

HTML templates are a powerful way to gang-up multiple Ecowitt sensor attributes in a single Hubitat dashboard tile with endless customization.
The following is a basic example of what you can achieve with a simple HTML template:

<img src="https://github.com/mircolino/ecowitt/raw/master/images/08.png" width="400" height="110">

To use them:

1.  In "Hubitat -> Devices" select the Ecowitt sensor (not the gateway) you'd like to "templetize":
    
    <img src="https://i.imgur.com/nkSaORs.png">

2.  In "Preferences -> HTML Tile Template" enter your template (see below how to format them) and click "Save Preferences"

3.  Now, in any Hubitat dashboard, add a new tile, on the left select the Ecowitt sensor, in the center select "Attribute" and on the right select the "html" attribute:
    
    <img src="https://i.imgur.com/YxTja4A.png">   

    You can also remove the tile "html" title by entering the following in the dashboard CSS:

     ```
    #tile-0 .tile-secondary { visibility: hidden; }
    ```
   
#### HTML Template Format:

Templates are pure HTML code with embedded servlets, which are nothing but sensor attributes surrounded by a \$\{\} expression.
For example the following template is used to create the tile in the example above:
     
  ```
  <p>Temperature: ${temperature} °F</p><p>Humidity: ${humidity} %</p><p>Pressure: ${pressure} inHg</p>
  ```
Tips:

1. When you enter the template in the device preferences DO NOT surround it with quotation marks.
2. For each specific sensor template, ONLY use the attributes you see displayed on the upper-right corner of the sensor preferences.
3. For obvious reasons, in a template, NEVER use the expression **\$\{html}**. Or your hub will enter a wormhole and resurface in a parallel universe made only of antimatter ;-) 

#### <a name="icons"></a> HTML Template Icons:

Using a small true-type font with only specific weather icons, it is possible to add dynamic icons to the Ecowitt dashboard tiles.

For example, using the driver **windCompass** attribute, the following is the syntax to obtain a 360° wind icon which always points to the current wind direction as reported by the Ecowitt gateway:

  ```
  <i class="ewi-wind${windCompass}"></i>
  ```

These icons are in reality text, so of course all the standard CSS font styling, shuch as size, color etc. applies as well.

[This is a complete list](https://mircolino.github.io/ecowitt/ecowitt.html) of all the icons available. Just access the page source html to see all the defined icon classes, and how to use them.

<img src="https://i.imgur.com/RRlwsHw.png" width="430" height="510">

To start using HTML templates including weather icons, simply add the following line to the beginning of your dashboard CSS:

  ```
  @import url("https://mircolino.github.io/ecowitt/ecowitt.css");
  ```

That's all. Now an Ecowitt Indoor Weather Sensor with the following template:

  ```
  <div class="ewv"><i class="ewi-temperature"></i> ${temperature} <span class="ewu">°F</span><br><i class="ewi-humidity"></i> ${humidity} <span class="ewu">%</span><br><i class="ewi-pressure"></i> ${pressure} <span class="ewu">inHg</span></div>
  ```
and an Ecowitt Air Quality Sensor with the following template:

  ```
  <div class="ewv"><i class="ewi-air" style="color:#${aqiColor}"></i> ${aqiDanger}<br>PM2.5: ${pm25} <span class="ewu">µg/m³</span><br>AQI: ${aqi}</div>
  ```
will produce the following tiles:

<img src="https://i.imgur.com/Lf73tCk.png" width="320" height="130">


The only hard limitation imposed by the hubitat driver interface is the template lenght which ***cannot be longer than 256 characters***.
A template longer than that will trigger a "Server Error 500" in hubitat. 



***

### Disclaimer

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, TITLE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR ANYONE DISTRIBUTING THE SOFTWARE BE LIABLE FOR ANY DAMAGES OR OTHER LIABILITY, WHETHER IN CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
