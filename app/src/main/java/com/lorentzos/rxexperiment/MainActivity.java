package com.lorentzos.rxexperiment;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

	private static final int REQUEST_CODE = 1122;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

		long seconds3 = TimeUnit.MILLISECONDS.convert(3, TimeUnit.SECONDS);
		long time = System.currentTimeMillis() + seconds3;

		PendingIntent pendingIntent = PendingIntent.getService(this, REQUEST_CODE, MyService.create(this), PendingIntent.FLAG_IMMUTABLE);

		alarmManager.set(AlarmManager.RTC, time, pendingIntent);
		Log.wtf("MainActivity", "onCreate seting alarm manager");

	}
}
