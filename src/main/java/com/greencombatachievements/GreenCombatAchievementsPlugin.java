package com.greencombatachievements;

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
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Green Combat Achievements"
)
public class GreenCombatAchievementsPlugin extends Plugin
{
	private static final int COMPLETE_COLOR = 0x0DC10D;

	@Inject
	private Client client;

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
		notifyIfDataMissing();
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

			bossCompletion.put(name, bossName.getTextColor() == COMPLETE_COLOR);
		}
	}

	private void notifyIfDataMissing()
	{
		if (hasNotifiedMissingData || !bossCompletion.isEmpty())
		{
			return;
		}

		Widget caFrame = client.getWidget(InterfaceID.CaTasks.FRAME);
		if (caFrame == null || caFrame.isHidden())
		{
			return;
		}

		client.addChatMessage(ChatMessageType.CONSOLE, "", "Green Combat Achievements: open the Bosses menu once so completed bosses can be highlighted.", null);
		hasNotifiedMissingData = true;
	}

	private void highlightMonsterDropdown()
	{
		Widget dropdown = client.getWidget(InterfaceID.CaTasks.DROPDOWN_MONSTER);
		if (dropdown == null || dropdown.isHidden())
		{
			return;
		}

		for (Widget entry : dropdown.getDynamicChildren())
		{
			String name = entry.getText();
			if (name != null && Boolean.TRUE.equals(bossCompletion.get(name)))
			{
				entry.setTextColor(COMPLETE_COLOR);
			}
		}
	}
}
