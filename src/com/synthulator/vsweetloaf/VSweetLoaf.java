package com.synthulator.vsweetloaf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.sf.supercollider.android.ISuperCollider;
import net.sf.supercollider.android.OscMessage;
import net.sf.supercollider.android.ScService;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;
import android.widget.Toast;

public class VSweetLoaf extends Activity {
	private ServiceConnection conn = new ScServiceConnection();
	private ISuperCollider superCollider;
	private TextView mainWidget = null;
	public static final String sampleName = "satan.wav";
	public static final String samplePath = ScService.scDirStr + "/sounds/" + sampleName;
	public static final String synthDefName = "shiftie";
	/*
	 * Gets us a SuperCollider service. 
	 */
	private class ScServiceConnection implements ServiceConnection {
		//@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			VSweetLoaf.this.superCollider = ISuperCollider.Stub.asInterface(service);
			try {
				// Kick off the supercollider playback routine
				superCollider.start();
				// Start a synth playing
				superCollider.sendMessage(new OscMessage(new Object[] {"b_allocRead", 10, samplePath}));
				superCollider.sendMessage(OscMessage.createSynthMessage(synthDefName,OscMessage.defaultNodeId,0,1));
				setUpControls(); // now we have an audio engine, let the activity hook up its controls
			} catch (RemoteException re) {
				re.printStackTrace();
			}
		}
		//@Override
		public void onServiceDisconnected(ComponentName name) {

		}
	}
	
	/**
	 * Provide the glue between the user's greasy fingers and the supercollider's shiny metal body
	 */
	public void setUpControls() {
		if (mainWidget!=null) mainWidget.setOnTouchListener(new OnTouchListener() {
			//@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction()==MotionEvent.ACTION_UP) {
					// OSC message right here!
					OscMessage noteMessage = new OscMessage( new Object[] {
							"/n_set", OscMessage.defaultNodeId, "trigger", 0f
					});
					try {
						// Now send it over the interprocess link to SuperCollider running as a Service
						superCollider.sendMessage(noteMessage);
					} catch (RemoteException e) {
						Toast.makeText(
								VSweetLoaf.this, 
								"Failed to communicate with SuperCollider!", 
								Toast.LENGTH_SHORT);
						e.printStackTrace();
					}
				} else if (event.getAction()==MotionEvent.ACTION_DOWN) {
					float stretch = 0.1f+(event.getX()*4)/mainWidget.getHeight();
					OscMessage stretchMessage = new OscMessage( new Object[] {
							"/n_set", OscMessage.defaultNodeId, "stretch", stretch
					});
					float pitch = 0.1f+(event.getY()*4)/mainWidget.getHeight();
					    OscMessage pitchMessage = new OscMessage( new Object[] {
							"/n_set", OscMessage.defaultNodeId, "pitch", pitch
					});
						OscMessage triggerMessage = new OscMessage (new Object[] {
							"/n_set", OscMessage.defaultNodeId, "trigger",1f
						});
					try {
						superCollider.sendMessage(stretchMessage);
						superCollider.sendMessage(pitchMessage);
						superCollider.sendMessage(triggerMessage);
					} catch (RemoteException e) {
						Toast.makeText(
								VSweetLoaf.this, 
								"Failed to communicate with SuperCollider!", 
								Toast.LENGTH_SHORT);
						e.printStackTrace();
					}
				}
				return true;
			}
		});

	}
	

	public void pushAssetToSD(String assetName, String dest) throws IOException {
		File destDir = new File(dest);
		destDir.mkdirs(); // Just in case they need making
		
		InputStream is = getApplicationContext().getAssets().open(assetName);
		OutputStream os = new FileOutputStream(dest+"/"+assetName);
		byte[] buf = new byte[1024];
		int bytesRead = 0;
		while (-1 != (bytesRead = is.read(buf))) {
			os.write(buf,0,bytesRead);
		}
		is.close();
		os.close();
	}

	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			pushAssetToSD(synthDefName +".scsyndef",ScService.dataDirStr);
			pushAssetToSD(sampleName,ScService.scDirStr+"/sounds");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		mainWidget = new TextView(this);
		mainWidget.setText("Virtual\nSweat\nLoaf");
		mainWidget.setTextSize(40);
		
		setContentView(mainWidget);
		// Here's where we request the audio engine
		bindService(new Intent("supercollider.START_SERVICE"),conn,BIND_AUTO_CREATE);
	}
	@Override
	public void onPause() {
		super.onPause();
		try {
			// Free up audio when the activity is not in the foreground
			if (superCollider!=null) superCollider.stop();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			if (superCollider!=null)
				superCollider.stop();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		unbindService(conn);
	}

}