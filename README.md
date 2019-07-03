# Ringly Android app


## Development

### Optional instead of null
The code makes a lot of use of Guava's `Optional<T>` class. Use it! It makes NullPointerExceptions less likely by making the possibility of missing values apparent in the type system.

You should **never** assign a variable to null or return null (except in the rare case when an external interface expects you to), and you should wrap potential nulls passed from external code using `Optional.fromNullable()`.

### Android Studio tips
- *Command-Shift-O* is the fastest way to navigate to a file. For example to get to `NotificationType.java`, just type Command-Shift-O "nottyp" :boom:
- *Command-clicking* on a function goes to its definition, which is a great way to get around, and also lets you view the Android source itself, which is sometimes superior to its documentation.

### Adding support for new applications
1. Find the app's id, by looking up its Play Store URL. For example, Googling "Facebook android" leads you to https://play.google.com/store/apps/details?id=com.facebook.katana, so the id of the Facebook app is `com.facebook.katana`
2. Copy the 60x60 black and white icon to `app/src/main/res/drawable-xhdpi/notifications_<app-name>.png`
3. Create a 30x30 version of the icon in the `drawable-mdpi` folder, either by resizing in a program like Preview.app, or from the command line using ImageMagick (`brew install imagemagick`) with the command `convert app/src/main/res/drawable-xhdpi/notifications_<app-name>.png -resize 50% app/src/main/res/drawable-mdpi/notifications_<app-name>.png`
4. Add the name of the app (in its proper alphabetical location) to `app/src/main/res/values/strings.xml`
5. Now that you have the icon, name, and id you can add an enum (in its proper alphabetical location) to `NotifcationType.java`, at which point the app is now supported!


## Architecture

The high-level architecture of the app is encoded in the `app/src/main/AndroidManifest.xml` file.

The core of the app is `RinglyService`, which maintains the Bluetooth connection to the ring.

Notifications are received either through `TelephonyReceiver`—for phone calls and SMS—or `NotificationListener`, which is alerted every time a notificaiton appears in the phone's status bar. These objects then send messages to `RinglyService` to perform the actual notification on the ring, with methods like `RinglyService.doNotification()`.

The user interface of the app is entirely in two activites—`MainActivity` for seeing the status of the ring and changing settings, and `DfuActivity` for performing firmware updates. Both of these send messages to `RinglyService` to communicate with the ring, with methods like `RinglyService.doCommand()`.

The UI can also "listen" to the ring, using the `addListener()` method of `RinglyService`, which will result in ring state changes being sent to an `onUpdate()` method on the listener.

The UI is all a single Activity, but it is made up of multiple Fragments, which are transitioned between. The main Activity coordinates these transitions, through callbacks from the Fragments.

### Possible improvements
The whole `addListener` thing is pretty annoying, because you have to be careful about removing listeners later.

Probably a better approach would be to use Android's builtin local broadcasts. The only downside of this is needing to marshall and unmarshall objects instead of being able to access them directly, but using Android's Parcelable might make this not too bad.

### Bluetooth hacks
Android's BLE stack doesn't seem to queue writes to GATT characeristics, so we have our own queueing implemented in `GattCallback.java`. This must be a problem for lots of people, so ideally there is a better or more standard solution.

Also, the `BluetoothGatt` connections sometimes get into status=133, at which point we simply give up and start from scratch. It would be better to know why this happens and avoid it.
