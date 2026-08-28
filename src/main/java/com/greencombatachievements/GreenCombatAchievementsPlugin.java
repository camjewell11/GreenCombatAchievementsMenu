package com.greencombatachievements;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import java.util.concurrent.ScheduledExecutorService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
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
	private static final File SAVE_DIR = new File(RuneLite.RUNELITE_DIR, "green-combat-achievements");
	private static final File SAVE_FILE = new File(SAVE_DIR, "completion.json");
	private static final Type PROFILE_MAP_TYPE = new TypeToken<Map<String, ProfileData>>()
	{
	}.getType();

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private Gson gson;

	private final Map<String, Boolean> bossCompletion = new HashMap<>();
	private final Map<String, Boolean> tierCompletion = new HashMap<>();
	private volatile Map<String, ProfileData> savedProfiles = new HashMap<>();
	private boolean hasNotifiedMissingData;
	private boolean dirty;

	@Override
	protected void startUp() throws Exception
	{
		bossCompletion.clear();
		tierCompletion.clear();
		hasNotifiedMissingData = false;
		executor.execute(this::loadFromDisk);
	}

	@Override
	protected void shutDown() throws Exception
	{
		bossCompletion.clear();
		tierCompletion.clear();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		GameState state = gameStateChanged.getGameState();
		if (state == GameState.LOGIN_SCREEN)
		{
			bossCompletion.clear();
			tierCompletion.clear();
			hasNotifiedMissingData = false;
		}
		else if (state == GameState.LOGGED_IN)
		{
			applySavedProfile();
		}
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		captureBossCompletion();
		captureTierCompletion();
		notifyIfDataMissing();

		if (dirty)
		{
			saveToDisk();
			dirty = false;
		}
	}

	@Subscribe
	public void onClientTick(ClientTick clientTick)
	{
		// Re-applied every frame because the game's own hover-highlight script
		// resets the entry's color while the mouse is over it.
		highlightDropdown(InterfaceID.CaTasks.DROPDOWN_MONSTER, bossCompletion);
		highlightDropdown(InterfaceID.CaTasks.DROPDOWN_TIER, tierCompletion);
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

			put(bossCompletion, name, bossName.getTextColor() == COMPLETE_COLOR);
		}
	}

	private void captureTierCompletion()
	{
		Widget tierNames = client.getWidget(InterfaceID.CaOverview.CA_TIER_NAME);
		Widget tierProgress = client.getWidget(InterfaceID.CaOverview.CA_PROGRESS);
		if (tierNames == null || tierNames.isHidden() || tierProgress == null || tierProgress.isHidden())
		{
			return;
		}

		// CA_TIER_NAME and CA_PROGRESS are separate dynamic child arrays with an
		// unused, blank-text slot that isn't in the same position in both lists,
		// so they're paired up by list order after dropping the blank entries.
		List<Widget> names = withText(tierNames.getDynamicChildren());
		List<Widget> progress = withText(tierProgress.getDynamicChildren());
		int count = Math.min(names.size(), progress.size());
		for (int i = 0; i < count; i++)
		{
			put(tierCompletion, names.get(i).getText(), progress.get(i).getTextColor() == COMPLETE_COLOR);
		}
	}

	private static List<Widget> withText(Widget[] widgets)
	{
		List<Widget> result = new ArrayList<>();
		for (Widget widget : widgets)
		{
			String text = widget.getText();
			if (text != null && !text.isEmpty())
			{
				result.add(widget);
			}
		}
		return result;
	}

	private void put(Map<String, Boolean> map, String key, boolean value)
	{
		Boolean previous = map.put(key, value);
		if (previous == null || previous != value)
		{
			dirty = true;
		}
	}

	private void notifyIfDataMissing()
	{
		if (hasNotifiedMissingData || (!bossCompletion.isEmpty() && !tierCompletion.isEmpty()))
		{
			return;
		}

		Widget caFrame = client.getWidget(InterfaceID.CaTasks.FRAME);
		if (caFrame == null || caFrame.isHidden())
		{
			return;
		}

		client.addChatMessage(ChatMessageType.CONSOLE, "", "Green Combat Achievements: open the Bosses menu and the Combat Achievements overview once so completed bosses and tiers can be highlighted.", null);
		hasNotifiedMissingData = true;
	}

	private void highlightDropdown(int dropdownComponentId, Map<String, Boolean> completion)
	{
		Widget dropdown = client.getWidget(dropdownComponentId);
		if (dropdown == null || dropdown.isHidden())
		{
			return;
		}

		for (Widget entry : dropdown.getDynamicChildren())
		{
			String name = entry.getText();
			if (name != null && Boolean.TRUE.equals(completion.get(name)))
			{
				entry.setTextColor(COMPLETE_COLOR);
			}
		}
	}

	private String getPlayerName()
	{
		Player localPlayer = client.getLocalPlayer();
		return localPlayer == null ? null : localPlayer.getName();
	}

	private void applySavedProfile()
	{
		String name = getPlayerName();
		if (name == null)
		{
			return;
		}

		ProfileData data = savedProfiles.get(name);
		if (data != null)
		{
			bossCompletion.putAll(data.bosses);
			tierCompletion.putAll(data.tiers);
		}
	}

	private void loadFromDisk()
	{
		if (!SAVE_FILE.exists())
		{
			return;
		}

		try (Reader reader = new FileReader(SAVE_FILE))
		{
			Map<String, ProfileData> loaded = gson.fromJson(reader, PROFILE_MAP_TYPE);
			if (loaded != null)
			{
				savedProfiles = loaded;
				clientThread.invoke(this::applySavedProfile);
			}
		}
		catch (IOException e)
		{
			log.debug("Failed to load Green Combat Achievements data", e);
		}
	}

	private void saveToDisk()
	{
		String name = getPlayerName();
		if (name == null)
		{
			return;
		}

		ProfileData data = new ProfileData();
		data.bosses = new HashMap<>(bossCompletion);
		data.tiers = new HashMap<>(tierCompletion);

		Map<String, ProfileData> updated = new HashMap<>(savedProfiles);
		updated.put(name, data);
		savedProfiles = updated;

		executor.execute(() -> persist(updated));
	}

	private void persist(Map<String, ProfileData> profiles)
	{
		SAVE_DIR.mkdirs();
		try (Writer writer = new FileWriter(SAVE_FILE))
		{
			gson.toJson(profiles, PROFILE_MAP_TYPE, writer);
		}
		catch (IOException e)
		{
			log.debug("Failed to save Green Combat Achievements data", e);
		}
	}

	private static class ProfileData
	{
		Map<String, Boolean> bosses = new HashMap<>();
		Map<String, Boolean> tiers = new HashMap<>();
	}
}
