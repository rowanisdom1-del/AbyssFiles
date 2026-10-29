# Abyss boot animation

`bootanimation.zip` is the finished animation (1280x720, 15 fps, about 5 MB). `preview.gif` shows what it looks like.
It is only an animation: it never launches an app and nothing starts automatically.

1. **Fake coding screen**: code types itself into a terminal window, with a progress bar.
2. **Game client splash**: a generic "Abyss Play" splash with a spinner and a loading bar.
3. **Abyss Home appears**: the launcher tiles slide in and the first one gets focus.
4. **Idle loop**: the focused tile glows gently until Android has finished booting.

## Install (needs root on the Switch's Android)

Make a backup of the original first. Run these in an adb shell with root:

```
adb push bootanimation.zip /sdcard/
adb shell
su
mount -o rw,remount /system        # if this fails, use the Magisk route below
cp /system/media/bootanimation.zip /sdcard/bootanimation-original.zip
cp /sdcard/bootanimation.zip /system/media/bootanimation.zip
chmod 644 /system/media/bootanimation.zip
reboot
```

If `/system` can't be remounted, the safer route is a small Magisk module that contains
`system/media/bootanimation.zip`. Tell me which one you end up using and I'll write the module for you.

## Change it

Edit `generate.py` (the code lines, colours, texts, timings) and run `python3 generate.py`
(needs `pip install pillow`). It rebuilds `bootanimation.zip` and `preview.gif`.
The zip must stay *stored* (uncompressed), which the script already does.
