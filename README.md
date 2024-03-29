# 1KAPanel
Nejc Berzelak, Uroš Podkrižnik and Vasja Vehovar  
*University of Ljubljana, Faculty of Social Sciences*

# What is 1KAPanel?

The application is an extension to the existing web survey software 1KA (http://www.1ka.si), with 1KA used as the central management tool that performs the following main tasks related to the application:
-	create studies and set up data collection methods used by each study (questionnaires, activity tracking…);
-	manage research participants and assign them to individual studies;
-	render web questionnaires to be displayed in the application’s integrated web browser;
-	store collected data and paradata.

A research participant who installs the application can participate in multiple studies at the same time. A simplified panel approach is used for this purpose: all research participants are centrally managed and can be assigned (subscribed) to individual studies. Upon subscription and informed consent to collect the requested data, the data collection instructions are sent from 1KA to the mobile application. 

The application currently supports the following core data collection features in addition to standard web questionnaires:
-	requesting a completion of new questionnaires presented at predefined time intervals or displaying a notification per the study manager’s request;
-	defining geofences that trigger a request to complete a new questionnaire
-	location and activity tracking with questionnaire completion requests based on location and activity-related criteria (e.g. at the end of a specific activity type that exceeds a threshold distance or duration).

# How to make it work?
Before you start to use 1KAPanel app, you have to install open source project 1KA - OneClickSurvey. You can get more info about this project [here](https://www.1ka.si/d/en/about/uses-of-1ka-services/own-installation). You can also contact user support via e-mail on enklikanketa@gmail.com.

## Generate 1KA API key
After your own installation of 1KA project is running, you need to generate 1KA API key following the instructions [here](https://www.1ka.si/d/en/about/1ka-api/api-key). Copy generated 1KA API key to variable *enka_api_private_key* in *res/values/private.xml* file of your 1KAPanel app.

## Get your Google Maps API keys
Google maps API key is needed for both ends - web installation of 1KA project and 1KAPanel app. 

1KA web uses interactive way to set geofences via Google Maps, which can be then further sent to 1KAPanel app users. This interface uses Google's Maps JavaScrip API - to get its private key, follow the instructions [here](https://developers.google.com/maps/documentation/javascript/get-api-key), then assign this private key to variable *$google_maps_API_key* in *settings_optional.php* file in root directory of your 1KA project installation.

1KAPanel app also uses Google Maps for viewing active geofences, viewing and editing locations and setting location before questionnaire entry. To get Google's Maps SDK for Android API key, follow instructions [here](https://developers.google.com/maps/documentation/android-sdk/get-api-key) and then assing this private key to variable *google_maps_api_key* in *res/values/private.xml* file of your 1KApanel app.

## Set URLs
To connect 1KA web project and 1KAPanel app, set URL of your running 1KA web project in *res/values/general.xml* file of your 1KAPanel app. With 1KA web project, you can also create simple feedback-like questionnaire and use link to this questionnaire in variable in the same file.

## Firebase Cloud Messaging
During research, instant communication is needed between 1KA web project and 1KAPanel app (adding, deleting or editing geofences, editing tracking settings, simple push notification etc.). For this, we are using Firebase Cloud Messaging. To get FCM key pair, follow instructions of [this paragraph](https://firebase.google.com/docs/cloud-messaging/js/client#configure_web_credentials_with_fcm), then assign value of generated pair to variable *$FCM_server_key* in *settings_optional.php* file in root directory of your 1KA project installation.

## Connect with Crashlytics (optional)
It is very important to get feedback of possible crash reports in 1KAPanel app and unwanted catched exceptions. We advise you to use this kit to get reports and repear them as fast as you can, so future data gathering can not be affected. To use it in app, follow the instructions [here](https://firebase.google.com/docs/crashlytics/get-started?platform=android&utm_source=fabric&utm_medium=inline_banner&utm_campaign=fabric_sunset&utm_content=kits_crashlytics). As mentioned, this is optional and if you don't want to use it, all you have to do is comment two lines - first in *App.java* and second in *Libraries/GeneralLib.java*.

# Acknowledgments
*The project ‘Integration of mobile devices into survey research in social sciences: Development of a comprehensive methodological approach’ (ID J5-8233) was financially supported by the Slovenian Research Agency.*

*The authors acknowledge the financial support from the Slovenian Research Agency (research core funding No. P5-0399).*
