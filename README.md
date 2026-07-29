# xposedYT

Xposed / LSPosed module that hides YouTube ads. No comments, no SponsorBlock — just ad blocking.

Adapted from [Morphe](https://github.com/MorpheApp/morphe-patches) / [ReVanced](https://github.com/ReVanced) patches.

## Features

- Hide general ads (home feed, search results)
- Hide video ads (preroll, midroll)
- Hide Premium promotions / "Get Premium" button
- Hide merchandise banners
- Hide shopping links
- Hide paid promotion labels
- Hide player popup ads (shopping, product stickers)
- Hide self-sponsor cards
- Hide end screen store banner

## How it works

1. **Litho component filtering** — hooks into YouTube's Litho UI rendering and filters out components matching ad patterns (`carousel_ad`, `paid_content_overlay`, `shopping_timely_shelf`, etc.)
2. **OS spoof** — spoofs the device as "Android Automotive", which YouTube does not serve ads to
3. **View hook fallback** — intercepts ViewGroup.addView and hides ad-related views

## Requirements

- Android 8.0+ (API 26)
- [LSPosed](https://github.com/LSPosed/LSPosed) or Xposed Framework (API 82+)

## Build

```bash
git clone https://github.com/YOUR_USER/xposedyt
cd xposedyt
./gradlew assembleRelease
```

APK: `app/build/outputs/apk/release/`

## Install

1. Install the APK
2. Enable the module in LSPosed
3. Target **com.google.android.youtube**
4. Reboot

## Credits

- [Morphe](https://github.com/MorpheApp/morphe-patches) — original patch patterns
- [ReVanced](https://github.com/ReVanced) — original patch work (GPLv3)