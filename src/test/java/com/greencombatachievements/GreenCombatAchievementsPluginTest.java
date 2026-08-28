package com.greencombatachievements;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GreenCombatAchievementsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GreenCombatAchievementsPlugin.class);
		RuneLite.main(args);
	}
}
