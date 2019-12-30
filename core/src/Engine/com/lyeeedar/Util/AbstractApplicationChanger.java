package com.lyeeedar.Util;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.lyeeedar.MainGame;

public abstract class AbstractApplicationChanger
{
	public Preferences prefs;

	public AbstractApplicationChanger( Preferences prefs )
	{
		this.prefs = prefs;
		if ( !prefs.getBoolean( "created" ) )
		{
			setDefaultPrefs( prefs );
			prefs.putBoolean( "created", true );

			prefs.flush();
		}
	}

	public void createApplication()
	{
		if ( Gdx.app != null )
		{
			System.err.println( "Application already exists!" );
			return;
		}

		Gdx.app = createApplication( Statics.game, prefs );
	}

	public abstract void processResources();

	public abstract void setDefaultPrefs( Preferences prefs );

	public abstract Application createApplication( MainGame game, Preferences pref );

	public abstract void updateApplication( Preferences pref );
}
