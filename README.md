# RandomDataLogger
An Android app for logging cell signal strength and automatically toggling airplane mode on/off when the detected signal falls below the user-set 
threshold in order to encourage the user's phone to reconnect to the cell tower with the strongest signal; intended to maintain a strong signal strength 
for users relying on their phone to provide a mobile hotspot in order to access the internet on other devices (for example, me trying to complete remote coursework during the COVID pandemic without having internet service at home...).

Toggling airplane mode is useful if there are multiple cell towers within range and your phone keeps getting kicked off the LTE band with the strongest signal. Upon toggling airplane mode, your phone may reconnect to the strongest LTE band (see the screenshot below to observe how toggling airplane mode after detecting a low signal again improves the signal strength). However, newer versions of the Android API forbid programmatically toggling airplane mode. This app uses an Accessibility Service workaround, which may require modifying the service for functionality on different devices (e.g., altering the nodeText.equals() fields in AccessibilityServiceToggleAirplaneMode.java).

Users may set the cell signal threshold (triggering airplane mode toggle if a signal below the threshold value is detected), frequency of signal strength logging, and minimum time interval before the next airplane mode toggle is initiated. An option to use randomly-generated signal data is included for app testing purposes. Since this app was intended to be used while the phone is creating a mobile hotspot, the app also reinstates the mobile hotspot after toggling airplane mode and before returning to the main activity; this functionality may be removed by modifying the Accessibility Service. An SQLite database is also generated during each logging session, mainly for debugging purposes.

Tested on Samsung Galaxy S8 with Android version 9.

## Screenshots
<p float="left">
  <img src="https://github.com/nina-af/RandomDataLogger/blob/master/RandomDataLogger_sh1.jpg" alt="Main Activity" width="270" height="550" />
  <img src="https://github.com/nina-af/RandomDataLogger/blob/master/RandomDataLogger_sg2.jpg" alt="Main Activity" width="270" height="550" />
</p>

## Activities and Services
**MainActivity.java**: Requests privileges, activates Accessibility Service for toggling airplane mode and mobile hotspot, displays a signal data chart which updates upon the arrival of each new signal strength data point, and logs signal data in SQLite database. Creates Broadcast Receivers to listen for chart data and for signals below the user-set signal threshold.

**DatabaseContract.java / DatabaseHelper.java**: Initializes SQLite database.

**SettingsActivity.java**: Allows user to specify the signal strength threshold, logging frequency, minimum airplane mode toggle interval; provides an option to use randomly-generated signal data.

**RandomDataService.java**: Service for measuring either real or randomly-generated signal strength data.

**ReadingDataPoint.java**: Class representing a single signal strength reading; parcelable for sending from service to main activity for charting.

**AccessibilityServiceToggleAirplaneMode.java**: Accessibility service triggered when low signal sends user to mobile settings screen; automatically performs gestures to toggle airplane mode and enable the mobile hotspot by recursively searching the wireless settings screen for text indicating the positions of the desired switches and buttons. May need to be modified for compatibility with other devices.




