package com.greencombatachievements;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("greencombatachievements")
public interface GreenCombatAchievementsConfig extends Config
{
	@ConfigItem(
		keyName = "highlightColor",
		name = "Highlight color",
		description = "The color applied to a boss's name in the Monster filter dropdown once all of its combat achievements are completed"
	)
	default Color highlightColor()
	{
		return new Color(0x0D, 0xC1, 0x0D);
	}
}
