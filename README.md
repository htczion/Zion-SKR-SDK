# Zion Social Key Recovery SDK

Integration guide for backup and restore seeds in HTC Exodus phone using Zion Social Key Recovery (SKR for short) SDK.  
https://github.com/htczion/Zion-SKR-SDK/wiki

## Introduction

Zion SKR SDK provides interfaces for 3rd party apps to integrate HTC Zion Vault's Social Key Recovery feature into their app. 3rd party app who integrate Zion SKR SDK will be able to use SKR to backup or restore their own HD seed in HTC Exodus phone. For details about HTC Zion Vault's Social Key Recovery, refer to the following link:   https://www.htcexodus.com/uk/support/exodus-one/faq/what-is-social-key-recovery-and-why-use-it.html

![SDK](media/sdk.png "SDK")


## Architecture

![Architecture](media/architecture.png "Architecture")

## Flow

## Usage

## Gradle plugin integration

1. Copy the .aar file into your app's project library path.  
    \<Project Name\>\app\libs\ZionSkrSdk-release.aar  

2. Add following snippets to your build.gradle.  

```gradle
dependencies {
  implementation(name: 'ZionSkrSdk-release', ext: 'aar')     
}
```


## License
