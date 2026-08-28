package com.greencombatachievements;

import com.google.inject.Provides;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Green Combat Achievements"
)
public class GreenCombatAchievementsPlugin extends Plugin
{
	private static final int STOCK_COMPLETE_COLOR = 0x0DC10D;

	@Inject
	private Client client;

	@Inject
	private GreenCombatAchievementsConfig config;

	private final Map<String, Boolean> bossCompletion = new HashMap<>();
	private boolean hasNotifiedMissingData;

	@Override
	protected void startUp() throws Exception
	{
		bossCompletion.clear();
		hasNotifiedMissingData = false;
	}

	@Override
	protected void shutDown() throws Exception
	{
		bossCompletion.clear();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN)
		{
			bossCompletion.clear();
			hasNotifiedMissingData = false;
		}
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		captureBossCompletion();
		highlightMonsterDropdown();
	}

	private void captureBossCompletion()
	{
		Widget bossNames = client.getWidget(InterfaceID.CaBosses.BOSSES_NAME);
		if (bossNames == null || bossNames.isHidden())
		{
			return;
		}

		for (Widget bossName : bossNames.getDynamicChildren())
		{
			String name = bossName.getText();
			if (name == null || name.isEmpty())
			{
				continue;
			}

			bossCompletion.put(name, bossName.getTextColor() == STOCK_COMPLETE_COLOR);
		}
	}

	private void highlightMonsterDropdown()
	{
		Widget dropdown = client.getWidget(InterfaceID.CaTasks.DROPDOWN_MONSTER);
		if (dropdown == null || dropdown.isHidden())
		{
			return;
		}

		if (bossCompletion.isEmpty())
		{
			notifyMissingData();
			return;
		}

		int highlightColor = config.highlightColor().getRGB() & 0xFFFFFF;
		for (Widget entry : dropdown.getDynamicChildren())
		{
			String name = entry.getText();
			if (name != null && Boolean.TRUE.equals(bossCompletion.get(name)))
			{
				entry.setTextColor(highlightColor);
			}
		}
	}

	private void notifyMissingData()
	{
		if (hasNotifiedMissingData)
		{
			return;
		}

		client.addChatMessage(ChatMessageType.CONSOLE, "", "Green Combat Achievements: open the Combat Achievements Bosses menu once so completed bosses can be highlighted.", null);
		hasNotifiedMissingData = true;
	}

	@Provides
	GreenCombatAchievementsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GreenCombatAchievementsConfig.class);
	}
}
