package ca.yorku.eecs.mack.demolunarlanderplus;

import android.app.Application;
import android.os.Bundle;

/**
 *          DemoLunarLanderPlus - with modifications by ...
 *
 *          LoginID - nguye688
 *          StudentID - nguye688
 *          Last name - Nguyen
 *          First name - Jeremy
 */

public class MyApplication extends Application
{
	Bundle b;

	@Override
	public void onCreate()
	{
		super.onCreate();
	}

	public void setBundle(Bundle bArg)
	{
		b = bArg;
	}

	public Bundle getBundle()
	{
		return b;
	}
}
