package com.lorentzos.rxexperiment;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class MyService extends Service {

	public static Intent create(Context context) {
		Intent intent = new Intent(context, MyService.class);
		intent.putExtra("1", new Car("Marvin"));

		return intent;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		Car extra = (Car) intent.getParcelableExtra("1");
		Log.wtf("MyService", "onStartCommand " + extra.shit);

		return super.onStartCommand(intent, flags, startId);
	}

	static class Car implements Parcelable {

		String shit;

		Car(String s) {
			shit = s;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(this.shit);
		}

		protected Car(Parcel in) {
			this.shit = in.readString();
		}

		public static final Creator<Car> CREATOR = new Creator<Car>() {
			@Override
			public Car createFromParcel(Parcel source) {
				return new Car(source);
			}

			@Override
			public Car[] newArray(int size) {
				return new Car[size];
			}
		};
	}

}
