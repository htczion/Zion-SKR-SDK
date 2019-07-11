# Zion Social Key Recovery SDK

Integration guide for using Zion Social Key Recovery (SKR for short) SDK to backup and restore seeds in HTC Exodus phone.
https://github.com/htczion/Zion-SKR-SDK/wiki

## Introduction

Zion SKR SDK provides Java interfaces for developers to integrate HTC Zion Vault's Social Key Recovery feature into their apps. 3rd party app who integrate Zion SKR SDK will be able to use SKR to backup or restore their wallet's HD seed in HTC Exodus phone. For details about HTC Zion Vault's Social Key Recovery feature, please refer to the following support link on HTC Exodus offical website.
https://www.htcexodus.com/uk/support/exodus-one/faq/what-is-social-key-recovery-and-why-use-it.html

![SDK](media/sdk.png "SDK")


## Architecture

The below architecture describes the components involved when we implement the Social Key Recovery.

![Architecture](media/architecture.png "Architecture")

* Wallet app
 * Android  
 The Android app works as a blockchain HD wallet, it can help user manage their's blockchain asset (with integrating [ZKMA](https://github.com/htczion/ZKMA)) and also can help user's friend to backup their seeds ([HTC Zion on Google Play](https://play.google.com/store/apps/details?id=com.htc.wallet&hl])).
 * iOS  
 The iOS app can help user's friend to backup their seeds ([HTC Zion on iOS App Store](https://apps.apple.com/tw/app/htc-zion/id1442810459)).
* TrustZone  
 * The seeds is securely saved in TrustZone.
 * The Shamir secret sharing is implemented in TrustZone. The SSS implementation is using [SSS from dsprenkels](https://github.com/dsprenkels/sss). Only the encypted partial seeds will be sent out to Android.
* Cloud storage  
 Cloud storage is only used for saving user's trusted contact list. It is to help user remembering who they have asked to help backup.
* Cloud messaing  
 The data exchanging is using cloud messaing. Current implementation is using Firebase Cloud Messaging (FCM).
* Deep link  
 The deep link is used for initial connection set up. Current implementation is using Firebase Dynamic Links.

## Flow

![Backup](media/skr_seeds_backup.png "Backup")
![Restore](media/skr_seeds_restore.png "Restore")

## Usage

## Gradle plugin integration

1. Copy the .aar file into your app's project library path.  
    \<Project Name\>\app\libs\ZionSkrSdk-release.aar  

2. Add following dependency to your build.gradle.  

```gradle
dependencies {
  implementation(name: 'ZionSkrSdk-release', ext: 'aar')     
}
```


## License
