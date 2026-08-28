package com.greencombatachievements;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Green Combat Achievements"
)
public class GreenCombatAchievementsPlugin extends Plugin
{
	@Inject
	private GreenCombatAchievementsConfig config;

	@Override
	protected void startUp() throws Exception
	{
		log.debug("Green Combat Achievements started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("Green Combat Achievements stopped!");
	}

	@Provides
	GreenCombatAchievementsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GreenCombatAchievementsConfig.class);
	}
}
