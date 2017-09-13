package com.zmxv.RNSound;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.media.AudioManager;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.ExceptionsManagerModule;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import android.util.Log;

public class RNSoundModule extends ReactContextBaseJavaModule {
  Map<Integer, MediaPlayer> playerPool = new HashMap<>();
  ReactApplicationContext context;
  private RNSound sound;
  public RNSoundModule(ReactApplicationContext context) {
    super(context);
    this.context = context;
    sound = RNSound.getInstance(context.getApplicationContext());//new RNSound(context);
  }

  @Override
  public String getName() {
    return "RNSound";
  }

  @ReactMethod
  public void prepare(final String fileName, final Integer key, final ReadableMap options, final Callback callback){

    Log.e("ReactNativeJS", "fileName = "+fileName);
    sound.prepare(fileName, key, options, new SoundManager.Prepare() {
      @Override
      public void onPrepared(Double duration) {
        WritableMap props = Arguments.createMap();
        props.putDouble("duration", duration);
        try {
          callback.invoke(null, props);
        } catch(RuntimeException runtimeException) {
          // The callback was already invoked
          Log.e("RNSoundModule", "Exception", runtimeException);
        }
      }

      @Override
      public void onError(int what, int extra) {
        try {
          WritableMap props = Arguments.createMap();
          props.putInt("what", what);
          props.putInt("extra", extra);
          callback.invoke(props, null);
        } catch(RuntimeException runtimeException) {
          // The callback was already invoked
          Log.e("RNSoundModule", "Exception", runtimeException);
        }
      }
    });
  }

  @ReactMethod
  public void play(final Integer key, final Callback callback) {
    sound.play(key, new SoundManager.Play() {
      @Override
      public void onResult(boolean isOK) {
        callback.invoke(isOK);
      }
    });
  }
  @ReactMethod
  public void pause(final Integer key, final Callback callback) {
    sound.pause(key);
    callback.invoke();
  }

  @ReactMethod
  public void stop(final Integer key, final Callback callback) {
    sound.stop(key);
    callback.invoke();
  }
  @ReactMethod
  public void stopAll(){
    sound.stopAll();
  }
  @ReactMethod
  public void release(final Integer key) {
    sound.release(key);
  }

  @ReactMethod
  public void setVolume(final Integer key, final Float left, final Float right) {
    sound.setVolume(key,left,right);
  }

  @ReactMethod
  public void setLooping(final Integer key, final Boolean looping) {
    sound.setLooping(key,looping);
  }

  @ReactMethod
  public void setSpeed(final Integer key, final Float speed) {
    sound.setSpeed(key,speed);
  }

  @ReactMethod
  public void setCurrentTime(final Integer key, final Float sec) {
    sound.setCurrentTime(key,sec);
  }

  @ReactMethod
  public void getCurrentTime(final Integer key, final Callback callback) {
    sound.getCurrentTime(key, new SoundManager.Time() {
      @Override
      public void onresult(double position, boolean isPlaying) {
        callback.invoke(position,isPlaying);
      }
    });
  }

  //turn speaker on
  @ReactMethod
  public void setSpeakerphoneOn(final Integer key, final Boolean speaker) {
    sound.setSpeakerphoneOn(key,speaker);
  }

  @ReactMethod
  public void enable(final Boolean enabled) {
    // no op
  }

  @Override
  public Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap<>();
    constants.put("IsAndroid", true);
    return constants;
  }
}
