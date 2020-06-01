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

    <img src="https://github.com/mircolino/ecowitt/raw/master/images/D01.png" width="300" height="600">  

2.  <span>Setup a local/customized weather service as follow (replacing hostname/IP with your own):  

    <img src="https://github.com/mircolino/ecowitt/raw/master/images/D02.png" width="300" height="600">

#### Hubitat: 

1.  If the Ecowitt Gateway has been setup correctly, every 5 minutes, you should see the following warning in the Hubitat system log:

    <img src="https://github.com/mircolino/ecowitt/raw/master/images/D03.png">
    
    That's because this driver has not been installed yet and the hub has nowhere to forward the gateway data to.
    
2.  In "Drivers Code" add the Ecowitt [WiFi Gateway](https://raw.githubusercontent.com/mircolino/ecowitt/master/ecowitt_gateway.groovy) and [RF Sensor](https://raw.githubusercontent.com/mircolino/ecowitt/master/ecowitt_sensor.groovy) drivers:

    <img src="https://github.com/mircolino/ecowitt/raw/master/images/D04.png">
    
3.  In "Devices" add a new "Ecowitt WiFi Gateway" virtual device and click "Save Device":

    <img src="https://github.com/mircolino/ecowitt/raw/master/images/D05.png">

4.  Enter the Gateway MAC address (in any legal form) and click "Save Preferences":

    <img src="https://github.com/mircolino/ecowitt/raw/master/images/D06.png">

5.  That should be all.
    The first time Hubitat receives data from the Gateway, the driver will automatically create child devices for all the present (and supported) sensors (depending on the frequency you setup your Gateway to send data, this may take a few minutes):
    
    <img src="https://github.com/mircolino/ecowitt/raw/master/images/D07.png">

### <a name="templates"></a> HTML Templates

HTML templates are a powerful way to gang-up multiple Ecowitt sensor attributes in a single Hubitat dashboard tile with endless customization.
The following is a basic example of what you can achieve with a simple HTML template:

<img src="https://github.com/mircolino/ecowitt/raw/master/images/D08.png" width="400" height="110">

Each sensor can specify up to 5 templates which will allow the creation of 5 customized dashboard tiles.

#### <a name="format"></a> HTML Template Format

Templates are pure HTML code with embedded servlets, which are nothing but sensor attributes surrounded by a \$\{\} expression.
For example the following two templates are used to create the tiles in the image above:
     
   ```
   <i class="ewi-temperature"></i> ${temperature} &deg;F<br><i class="ewi-humidity"></i> ${humidity} %<br><i class="ewi-pressure"></i> ${pressure} inHg
   ```  
   ```
   <i class="ewi-air" style="color:#${aqiColor}"></i> ${aqiDanger}<br>PM2.5: ${pm25} &micro;g/m&sup3;<br>AQI: ${aqi}
   ```
NB:

1. When you enter the template in the device preferences DO NOT surround it with quotation marks.
2. The maximum lenght of a template (imposed by the Hubitat GUI) is 256 characters. If you enter a longer template, Hubitat will return a "Server 500 error".
3. For each specific sensor template, ONLY use the attributes you see displayed on the upper-right corner of the sensor preferences (those highlighted in red in the image below).
4. For obvious reasons, in a template, NEVER use the expression **\$\{html}**. Or your hub will enter a wormhole and resurface in a parallel universe made only of antimatter ;-) 

#### HTML Templates Quick Start

1.  In "Hubitat -> Devices" select an Ecowitt sensor (not the gateway) you'd like to "templetize".
    
    Then In "Preferences -> HTML Tile Template" enter, either your template directly or up to five (comma separated) pre-made template IDs which the driver will automatically retrieve from the [template repository](#repository).

    Finally click "Save Preferences".
    
    <img src="https://github.com/mircolino/ecowitt/raw/master/images/D09.png">

    NB: if you enter a template directly, make sure it's 256 character or shorter. If you enter a list of templates make sure it's 5 IDs or shorter without duplicates.

2.  In the Hubitat dashboard you intend to use to create HTML template tiles, click "Cog icon -> Advanced -> CSS" and add the following line to the beginning of the CSS file:
    
     ```  
    @import url("https://mircolino.github.io/ecowitt/ecowitt.css");
    ```
     Click "Save CSS"

    <img src="https://github.com/mircolino/ecowitt/raw/master/images/D10.png">   


3.  Now, in the same dashboard, add a new tile, on the left select the Ecowitt sensor, in the center select "Attribute" and on the right select the "html" attribute
    
    <img src="https://github.com/mircolino/ecowitt/raw/master/images/D11.png"> 

    Tip: you can remove the tile "html" title by entering the following in the dashboard CSS (assuming the tile ID is 66):

     ```
    #tile-66 .tile-secondary { visibility: hidden; }
    ```

#### <a name="icons"></a> HTML Template Icons

Using a small true-type font with only specific weather icons, it is possible to add dynamic icons to the Ecowitt dashboard tiles.

For example, using the driver **windCompass** attribute, the following is the syntax to obtain a 360° wind icon which always points to the current wind direction as reported by the Ecowitt gateway:

  ```
  <i class="ewi-wind${windCompass}"></i>
  ```

These icons are in reality text, so of course all the standard CSS font styling, shuch as size, color etc. applies as well.

[This is a complete list](https://mircolino.github.io/ecowitt/ecowitt.html) of all the icons available. Just access the page source html to see all the defined icon classes, and how to use them.

<img src="https://github.com/mircolino/ecowitt/raw/master/images/D12.png" width="710" height="625">

#### <a name="repository"></a> HTML Template Repository

To facilitate reusing and sharing templates, the Ecowitt driver uses a [central JSON repository](https://raw.githubusercontent.com/mircolino/ecowitt/master/html/ecowitt.json) where all the templates can be accessed by ID.
This is a [complete up-to-date list](https://github.com/mircolino/ecowitt/blob/master/templates.md) of all the templates available in the repository.

*Templates in the repository are measurement system agnostic and will display the correct unit system based on the parent selection.* 

If you come up with interesting and useful new templates, please [share them here](https://community.hubitat.com/t/release-ecowitt-gw1000-wi-fi-gateway/38983/last), along with an image of the rendered tile, and I'll add them to the repository.


***

### Disclaimer

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, TITLE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR ANYONE DISTRIBUTING THE SOFTWARE BE LIABLE FOR ANY DAMAGES OR OTHER LIABILITY, WHETHER IN CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
