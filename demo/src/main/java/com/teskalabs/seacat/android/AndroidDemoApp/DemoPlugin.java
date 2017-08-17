package com.teskalabs.seacat.android.AndroidDemoApp;

import com.teskalabs.seacat.android.client.SeaCatPlugin;

import java.util.Properties;

final class DemoPlugin extends SeaCatPlugin {

	@Override
	public Properties getCharacteristics(){
		Properties p = new Properties();
		p.setProperty("DummyCap", "123 ahoj!");
		return p;
	}

}
