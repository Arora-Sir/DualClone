# README screenshots

The README references `home.png`, `settings.png` and `palette.png` in this folder. Capture them with demo data only. Never copy captures from a personal device, because they show your real app list.

## Capture steps

1. Use an emulator, or a clean device or profile, with only well-known apps installed (a browser, a notes app, a messenger).
2. Create the target profile (see SETUP.md) and clone two or three of those apps so the Cloned tab is not empty.
3. Set the language to English, font scale to 1.0 and a plain wallpaper.
4. Capture each screen:

```bash
adb -s <serial> exec-out screencap -p > docs/screenshots/home.png
```

5. Crop the status bar off (time, notification icons, battery), then scale to about 540 pixels wide so the repository stays small. PNG or WebP only.
6. Check each image for personal data (device name, notification text, status bar items) before committing.

| File | Screen |
| :--- | :--- |
| `home.png` | Home with the Cloned tab and Shizuku Ready |
| `settings.png` | Settings top of page |
| `palette.png` | More colors sheet |
