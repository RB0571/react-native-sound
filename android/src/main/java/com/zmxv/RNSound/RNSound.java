package com.zmxv.RNSound;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class RNSound {
  private static final String TAG = "ReactNativeJS";
  Map<Integer, MediaPlayer> playerPool = new HashMap<>();
  Context context;
  private static RNSound sound;
  public static RNSound getInstance(Context context){
    if (sound == null){
      sound = new RNSound(context);
    }
    return sound;
  }
  private RNSound(Context context) {
    this.context = context;
  }

  public void prepare(final String fileName, final Integer key, final ReadableMap options, final SoundManager.Prepare prepare) {
    Log.i(TAG,"prepare === > : key = "+key+" , filename = "+fileName);
    MediaPlayer player = createMediaPlayer(fileName);
    if (player == null) {
      WritableMap e = Arguments.createMap();
      e.putInt("code", -1);
      e.putString("message", "resource not found");
      return;
    }

    final RNSound module = this;
    player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
      boolean callbackWasCalled = false;
      @Override
      public synchronized void onPrepared(MediaPlayer mp) {
        if (callbackWasCalled) return;
        callbackWasCalled = true;

        module.playerPool.put(key, mp);

        prepare.onPrepared(mp.getDuration()*0.001);
      }

    });

    player.setOnErrorListener(new OnErrorListener() {
      boolean callbackWasCalled = false;
      @Override
      public synchronized boolean onError(MediaPlayer mp, int what, int extra) {
        if (callbackWasCalled) return true;
        callbackWasCalled = true;
        prepare.onError(what,extra);
        return true;
      }
    });

    try {
      player.prepareAsync();
    } catch (IllegalStateException ignored) {
      // When loading files from a file, we useMediaPlayer.create, which actually
      // prepares the audio for us already. So we catch and ignore this error
    }
  }
  private MediaPlayer player;
  private void setPlayer(final String fileName) {
    int res = this.context.getResources().getIdentifier(fileName, "raw", this.context.getPackageName());
    try {
      if (res != 0) {
        AssetFileDescriptor file = this.context.getResources().openRawResourceFd(res);
        player.setDataSource(file.getFileDescriptor(),file.getStartOffset(),file.getLength());
      } else if (fileName.startsWith("http://") || fileName.startsWith("https://")) {
        player.setDataSource(fileName);
      } else {
        File file = new File(fileName);
        if (file.exists()) {
          Uri uri = Uri.fromFile(file);
          player.setDataSource(this.context, uri);
        }
      }
      player.setAudioStreamType(audioType);
    }catch (IOException e){
      e.printStackTrace();
    }
  }

  private int audioType = AudioManager.STREAM_MUSIC;
  protected MediaPlayer createMediaPlayer(final String fileName) {
    MediaPlayer mediaPlayer = null;
    int res = this.context.getResources().getIdentifier(fileName, "raw", this.context.getPackageName());
    if (res != 0) {
      mediaPlayer = MediaPlayer.create(this.context, res);
    }else if(fileName.startsWith("http://") || fileName.startsWith("https://")) {
      mediaPlayer = new MediaPlayer();
      Log.i("RNSoundModule", fileName);
      try {
        mediaPlayer.setDataSource(fileName);
      } catch(IOException e) {
        Log.e("RNSoundModule", "Exception", e);
        return null;
      }
    }else{
      File file = new File(fileName);
      if (file.exists()) {
        Uri uri = Uri.fromFile(file);
        // Mediaplayer is already prepared here.
        mediaPlayer = MediaPlayer.create(this.context, uri);
      }
    }
    if(mediaPlayer != null){
      mediaPlayer.setAudioStreamType(audioType);
    }
    return mediaPlayer;
  }

  public void play(final Integer key, final SoundManager.Play callback) {
    MediaPlayer player = this.playerPool.get(key);
    if (player == null) {
      callback.onResult(false);
      return;
    }
    if (player.isPlaying()) {
      callback.onResult(false);
      return;
    }
    player.setOnCompletionListener(new OnCompletionListener() {
      boolean callbackWasCalled = false;

      @Override
      public synchronized void onCompletion(MediaPlayer mp) {
        if (!mp.isLooping()) {
          if (callbackWasCalled) return;
          callbackWasCalled = true;
          callback.onResult(true);
        }
      }
    });
    player.setOnErrorListener(new OnErrorListener() {
      boolean callbackWasCalled = false;

      @Override
      public synchronized boolean onError(MediaPlayer mp, int what, int extra) {
        if (callbackWasCalled) return true;
        callbackWasCalled = true;
        callback.onResult(false);
        return true;
      }
    });
    player.start();

  }

  public void pause(final Integer key) {
    MediaPlayer player = this.playerPool.get(key);
    if (player != null && player.isPlaying()) {
      player.pause();
    }
  }
  public void stopAll(){
    /*for(int i = 0;i<this.playerPool.size();i++){
      if (player != null && player.isPlaying()) {
        player.pause();
        player.seekTo(0);
      }
    }*/
    for(Map.Entry<Integer,MediaPlayer> entry:playerPool.entrySet()){
      stop(entry.getKey());
      release(entry.getKey());
    }
  }

  public void stop(final Integer key) {
    MediaPlayer player = this.playerPool.get(key);
    if (player != null && player.isPlaying()) {
      player.pause();
      player.seekTo(0);
    }
  }

  public void release(final Integer key) {
    MediaPlayer player = this.playerPool.get(key);
    if (player != null) {
      player.release();
      this.playerPool.remove(key);
    }
  }

  public void setVolume(final Integer key, final Float left, final Float right) {
    MediaPlayer player = this.playerPool.get(key);
    if (player != null) {
      player.setVolume(left, right);
    }
  }

  public void setLooping(final Integer key, final Boolean looping) {
    MediaPlayer player = this.playerPool.get(key);
    if (player != null) {
      player.setLooping(looping);
    }
  }

  public void setSpeed(final Integer key, final Float speed) {
    MediaPlayer player = this.playerPool.get(key);
    if (player != null) {
      player.setPlaybackParams(player.getPlaybackParams().setSpeed(speed));
    }
  }

  public void setCurrentTime(final Integer key, final Float sec) {
    MediaPlayer player = this.playerPool.get(key);
    if (player != null) {
      player.seekTo((int)Math.round(sec * 1000));
    }
  }

  public void getCurrentTime(final Integer key, final SoundManager.Time time) {
    MediaPlayer player = this.playerPool.get(key);
    if (player == null) {
      time.onresult(-1, false);
      return;
    }
    time.onresult(player.getCurrentPosition() * .001, player.isPlaying());
  }

  //turn speaker on
  public void setSpeakerphoneOn(final Integer key, final Boolean speaker) {
    MediaPlayer player = this.playerPool.get(key);
    if (player != null) {
      player.setAudioStreamType(AudioManager.STREAM_ALARM);
      AudioManager audioManager = (AudioManager)this.context.getSystemService(this.context.AUDIO_SERVICE);
      audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
      audioManager.setSpeakerphoneOn(speaker);
    }
  }
  public boolean isPlaying(int key){
    MediaPlayer player = this.playerPool.get(key);
    return player !=null && player.isPlaying();
  }
  public void enable(final Boolean enabled) {
    // no op
  }


}
