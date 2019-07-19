# Zion Social Key Recovery SDK



## Introduction

Zion Social Key Recovery SDK (Zion-SKR-SDK) is an Android library that provides Java APIs for developers to integrate HTC Zion Vault's Social Key Recovery (SKR) feature into their apps. 3rd party apps that integrate Zion-SKR-SDK will be able to use SKR to backup or restore their HD wallet's seed in HTC Exodus phone. For details about HTC Zion Vault's Social Key Recovery feature, please refer to the following link on HTC Exodus offical website.  
[https://www.htcexodus.com/us/support/exodus-one/faq/what-is-social-key-recovery-and-why-use-it.html](https://www.htcexodus.com/us/support/exodus-one/faq/what-is-social-key-recovery-and-why-use-it.html)

![SDK](media/sdk.png "SDK")


## Architecture

The below architecture diagram describes the components involved when we implement the Social Key Recovery.

![Architecture](media/architecture.png "Architecture")

* Wallet app
  * Android  
 The Android app works as a HD wallet, it can help user manage their's digital assets (e.g., sending or receiving Bitcoin, for how to integrate Zion Key Management APIs, please refer to [ZKMA](https://github.com/htczion/ZKMA)) and also can help user backup or restore their HD wallet. (For demo app, please check HTC Zion on Android play store, [https://play.google.com/store/apps/details?id=com.htc.wallet&hl](https://play.google.com/store/apps/details?id=com.htc.wallet&hl])).
  * iOS  
 The iOS app can help user's friend to backup their partial seeds (For demo app, please check HTC Zion on iOS app store, [https://apps.apple.com/tw/app/htc-zion/id1442810459](https://apps.apple.com/tw/app/htc-zion/id1442810459)).
* TrustZone  
  * Quote from [Wikipedia](https://en.wikipedia.org/wiki/Trusted_execution_environment)
  > A trusted execution environment (TEE) is a secure area of a main processor. It guarantees code and data loaded inside to be protected with respect to confidentiality and integrity[clarification needed].[1] A TEE as an isolated execution environment provides security features such as isolated execution, integrity of applications executing with the TEE, along with confidentiality of their assets.[2] In general terms, the TEE offers an execution space that provides a higher level of security[for whom?] than a rich mobile operating system open (mobile OS) and more functionality than a 'secure element' (SE).[3]  
  * The user's HD seed is securely saved in TrustZone.  
  * The seed splitting and sharing is implemented using the Shamir secret sharing and is implemented inside TrustZone. The SSS implementation is using [SSS from dsprenkels](https://github.com/dsprenkels/sss).  
  * Only the encypted partial seeds will be sent out to Android world.
* Cloud storage  
  * Cloud storage is used for saving user's trusted contact list. It is to help user remembering who they have asked to help backup.
* Cloud messaing  
  * The data exchanging is using cloud messaing. Current implementation is using Firebase Cloud Messaging (FCM).
* Deep link  
  * The deep link is used for initial connection set up. Current implementation is using Firebase Dynamic Links.


## Flow

Sequence diagram describes the partial seed's backup flow with friend.  
![Backup](media/skr_seeds_backup.png "Backup")

Sequence diagram describes the partial seed's restore flow with friend.  
![Restore](media/skr_seeds_restore.png "Restore")


## Usage

1. Integration guide for using Zion-SKR-SDK to backup and restore seeds in HTC Exodus phone.
  * [https://github.com/htczion/Zion-SKR-SDK/wiki](https://github.com/htczion/Zion-SKR-SDK/wiki)
2. Apply and integrate related cloud services.
  * [https://github.com/htczion/Zion-SKR-SDK/wiki/Cloud-Settings](https://github.com/htczion/Zion-SKR-SDK/wiki/Cloud-Settings)
3. Provide your app's info for applying HTC Key Server usage.  
 Please provide following infomation in your mail:
  * Full name, e.g., Hank Chiu
  * Company name, e.g., HTC
  * Email
  * App name, e.g., HTC Zion
  * App's package name, e.g., "com.htc.wallet"
  * App's SHA256 signature (Base64 encoded), e.g., xpghBVzbMWoosDUOJl1/trHXXlSBBilUanBSiwVe/rk=
 ```
 # keytool -list -printcert -jarfile app-partner-release.apk  
  ...
   SHA256: C6:98:21:05:5C:DB:31:6A:28:B0:35:0E:26:5D:7F:B6:B1:D7:5E:54:81:06:29:54:6A:70:52:8B:05:5E:FE:B9
  ...
 # echo "C6:98:21:05:5C:DB:31:6A:28:B0:35:0E:26:5D:7F:B6:B1:D7:5E:54:81:06:29:54:6A:70:52:8B:05:5E:FE:B9" | xxd -r -p | base64  
  xpghBVzbMWoosDUOJl1/trHXXlSBBilUanBSiwVe/rk=
 ```   
  And send to [Hank_Chiu@htc.com](mailto:hank_chiu@htc.com)


## Gradle plugin integration

1. Copy the .aar file into your app's project library path.  
    \<Project Name\>\app\libs\ZionSkrSdk-release.aar  
2. Add following dependency to your build.gradle.  

```gradle
dependencies {
  implementation(name: 'ZionSkrSdk-release', ext: 'aar')     
}
```

For details, please refer to [https://github.com/htczion/Zion-SKR-SDK/wiki](https://github.com/htczion/Zion-SKR-SDK/wiki)


## License
