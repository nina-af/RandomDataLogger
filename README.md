# RandomDataLogger
An Android app for logging cell signal strength and automatically toggling airplane mode on/off when the detected signal falls below the user-set 
threshold in order to encourage the user's phone to reconnect to the cell tower with the strongest signal; intended to maintain a strong signal strength 
for users relying on their phone to provide a mobile hotspot in order to access the internet on other devices.

Toggling airplane mode is useful if there are multiple cell towers within range and your phone keeps getting kicked off the LTE band with the strongest signal (see screenshot). Upon toggling airplane mode, your phone may reconnect to the strongest LTE band. However, newer versions of the Android API forbid programmatically toggling airplane mode. This app uses an Accessibility Service workaround, which may require modifying the service for functionality on different devices.

Users may set the cell signal threshold (triggering airplane mode toggle if a signal below the threshold value is detected), frequency of signal strength logging, and minimum time interval before the next airplane mode toggle is initiated.

## Screenshots
<p float="left">
  <img src="https://github.com/nina-af/RandomDataLogger/blob/master/RandomDataLogger_sh1.jpg" alt="Main Activity" width="270" height="550" />
  <img src="https://github.com/nina-af/RandomDataLogger/blob/master/RandomDataLogger_sg2.jpg" alt="Main Activity" width="270" height="550" />
</p>




