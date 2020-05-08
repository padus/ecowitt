## Ecowitt WiFi Gateway
*Ecowitt WiFi Gateway driver for Hubitat Elevation*

### Features

- LAN comunication only, no cloud/weather service needed.
- One Hubitat device for each Ecowitt sensor for easy dashboard tiles and RM rules handling.
- On-the-fly Imperial <-> Metric conversion.

### Installation Instructions

#### Ecowitt WS View:

1.  Make sure all your sensors are properly registered:  

    <img src="https://i.imgur.com/YBOsGDg.png" width="300" height="600">  

2.  <span>Setup a local/customized weather service as follow (replacing hostname/IP with your own):  

    <img src="https://i.imgur.com/STF5v6d.png" width="300" height="600">

#### Hubitat: 

1.  If the Ecowitt Gateway has been setup correctly, every 5 minutes, you should see the following warning in the Hubitat system log:

    <img src="https://i.imgur.com/Q6w2S7W.png">
    
    That's because this driver has not been installed yet and the hub has nowhere to forward the data to.
    
2.  In "Drivers Code" add the Ecowitt [WiFi Gateway](https://raw.githubusercontent.com/mircolino/ecowitt/master/ecowitt_gateway.groovy) and [RF Sensor](https://raw.githubusercontent.com/mircolino/ecowitt/master/ecowitt_sensor.groovy) drivers:

    <img src="https://i.imgur.com/F66oitb.png">
    
3.  In "Devices" add a new "Ecowitt WiFi Gateway" virtual device:

    <img src="https://i.imgur.com/3oPQpJ2.png">

4.  Enter the Gateway MAC address (in any legal form) and click "Save Preferences":

    <img src="https://i.imgur.com/YKYtm98.png">

5.  That should be all.
    The first time Hubitat receives data from the Gateway, the driver will automatically create child devices for all the present (and supported) sensors (depending on the frequency you setup your Gateway to POST, this may take a few minutes):
    
    <img src="https://i.imgur.com/Nad8ScL.png">

### Disclaimer

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, TITLE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR ANYONE DISTRIBUTING THE SOFTWARE BE LIABLE FOR ANY DAMAGES OR OTHER LIABILITY, WHETHER IN CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

In other words, install it on a production Hubitat hub at your own risk ;-)
