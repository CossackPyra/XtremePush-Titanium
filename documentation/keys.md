## iOS Push Notification Certificates

To integrate XtremePush with an app you need to upload some certificates to your app dashboard on xtremepush.com.
This is because to send push notifications to iOS devices, you need to set up an iOS Push Notifications certificate.
- Go to: https://developer.apple.com/membercenter/
- Log in
- Select Certificates, Identifiers and Profiles

![Apple Developer Member Centre](http://cl.ly/image/2U3N3U2l123W/apple_dev_member_centre.png) 

Select "Identifiers" under iOS Apps column.

![iOS App Identifiers](http://cl.ly/image/1O2G2i203w0S/apple_dev_ios_identifiers.png)

Select the app you are integrating with XtremePush and choose "Edit".

![Edit App ID settings](http://cl.ly/image/3p2K002K3T0N/apple_dev_appid_settings.png)

In iOS App ID Settings select the Push Notifications Check Box and then Click "Create Certificate" in "Development SSL Certificate".

![Create Certificate](http://cl.ly/image/0d1f1N210b17/apple_dev_create_cert.png)

Follow Apple's step by step instructions to generate the certificate. Download the certificate when prompted to do so.

![Cert is Ready](http://cl.ly/image/2W441O0W3w40/apple_dev_cert_ready.png)

Repeat's this procedure for the "Production SSL Certificate". Push should now be enabled for development and distribution.

![Push has been enabled](http://cl.ly/image/040w1Z3r3v0K/apple_dev_push_enabled.png)

Now that both certs have been created both certificates must be uploaded to xtremepush.com to link your app to the XtremePush platform.  Download the certificates if you have not already done so and then open them.The certificates will open in Keychain Access. For each certificate in turn, right click select 'Export'.

![Exporting keychain to link app to XtremePush](http://cl.ly/image/2k0L373I0V3b/exportin_keychain_for_upload.png)

Choose the default export format (.p12) and export the certificates.
Log in to your XtremePush dashboard on xtremepush.com go to your app home and navigate to Settings > Application Keys and upload the two exported certificates.

![Adding iOS push notification certs](http://cl.ly/image/3s0U0n3P1x3p/adding_certs.png)


## Getting an API Key for Google Cloud Messaging <a name="android_keys"></a> 
To integrate XtremePush with an Android app you need to upload your GCM API key to your app dashboard on xtremepush.com. This is because to send push notifications to Android devices, you need to set up a Google API Project, enable the GCM service and obtain an API key for it.
In this section we will summarise the main steps involved. You can also find Google's own guide to setting up a Google API Project and obtaining an API key for the GCM service [here](http://developer.android.com/google/gcm/gs.html).

The first step is to:
- Go to: https://console.developers.google.com/
- Log in
- Create a project for your app if you don't already have one

![Google Developer Console API Project](http://cl.ly/image/1a3x3L0M0U2A/google_dev_console.png)

Next click on your project and you will be taken to your project home. Your **project number** is displayed on top of this page you will need that later to integrate your app with XtremePush but first you must select *APIs & auth* to enable GCM.

![Google Developer Console Project Home](http://cl.ly/image/1M1b3M2O1324/google_dev_console_home.png)

In *APIs & auth > API* scroll down until you find Google Cloud Messaging for Android and switch it on:

![Google Developer Console GCM ON](http://cl.ly/image/2H2F1N180q1K/google_dev_gcm_on.png)

In the sidebar on the left, select *APIs & auth > Credentials*. On the right under  *Public API access* , click *Create New Key* and select *Server Key*. Do not specify any ip address and click *Create*.

![Generate API Key](http://cl.ly/image/1Q2G372k0w2w/google_dev_new_key.png)

Copy the new *API Key* you are given under *Public API Access > Key for server applications* in.  Log in to your XtremePush dashboard on xtremepush.com. Go to your app home and navigate to *Settings > Application Keys* and select Android App. Paste the key into *Android Application Key* and click *save*.

![Adding the API key](http://cl.ly/image/1U1N022r1B32/adding_API_key.png)


## Connect your App to the XtremePush Platform

1. Add your App on the platform by clicking "Integrate Push Features" on your XtremePush Dashboard at xtremepush.com
   
   ![Adding your app on the platform click integrate push features](http://cl.ly/image/050s3O0F2N2N/integrate_app.png)

2. Enter the App Name, upload the App icon, and give a short description of the app. An App key and token have been automatically generated. The App key is used in your Android project to connect the app and platform. The app token will only be used if you use the external API.  Save your settings and *copy the app key*. Your saved settings should be similar to the following.
   
   ![A saved apps settings on the platform](http://cl.ly/image/3I1K2V1t161l/app_saved.png) 
   
3. Still in your App Home on xtremepush.com go to Settings > Application Keys 
and copy your API Key for Google Cloud Messaging into *Android Application Key* and click *save*. If you don't now where to get this key please read our documentation on *Getting an API Key for Google Cloud Messaging* [here](#keys) 

   ![Adding the API key](http://cl.ly/image/1U1N022r1B32/adding_API_key.png)


4. Next you will use your app key from the *Settings > General Settings* section of your app home on xtremepush.com and your project number from the google developer console to connect your app to the platform. If you don't know where to get the project number please refer to our documentation on *Getting an API Key for Google Cloud Messaging* [here](#keys) 


## Sending your first Push

1. To send a basic push go to your app home and select create campaign. The first step is to name your campaign and add some content for the push. In this section you can also link to app pages, urls, or a custom html page for a richer push but for now we will just add text for a simple push.

   ![ Adding Content ](http://cl.ly/image/2S1f1R1K1J47/create_campaign_content.png)

2. Click next and you will be taken to the segments section. For your first push select broadcast to all users and refresh the number of addressable devices if you are using one development device this value should be one.
   
   ![ Selecting a segment ](http://cl.ly/image/0Q260e123V44/add_segment.png)

3. Click next and you will be taken to location, for your first push you will not be tying it to a location so click next and you are taken to schedule. In schedule the default selection is Send Now and to test your first push you will want to keep it that way.

   ![Schedule your push](http://cl.ly/image/0r2913470O2g/add_schedule.png) 

4. Click next and you will be taken to platform. Select Android as your platform. 

   ![Configure your platform](http://cl.ly/image/2q2P2U141M1o/push_add_android.png)

5. You are almost ready to send your first push. Click preview, review your text and then hit send push. Your Android device will receive the push: 

   ![ Your First Push ](http://cl.ly/image/3i1g2L3L321B/first_push_android.png)