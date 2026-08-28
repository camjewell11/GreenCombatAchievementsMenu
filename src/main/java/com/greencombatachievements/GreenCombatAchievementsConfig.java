package com.greencombatachievements;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("greencombatachievements")
public interface GreenCombatAchievementsConfig extends Config
{
	@ConfigItem(
		keyName = "highlightColor",
		name = "Highlight color",
		description = "The color applied to a boss's name when all of its combat achievements are completed"
	)
	default String highlightColor()
	{
		return "#00FF00";
	}
}
