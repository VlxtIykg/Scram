package com.golem.golemmod.command.commands;

import com.golem.golemmod.Main;
import com.golem.golemmod.utils.RenderUtils;
import com.golem.golemmod.utils.RequestData;
import com.golem.golemmod.utils.RequestUtil;
import com.golem.golemmod.utils.ToolTipListener;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.util.IChatComponent;

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.zip.GZIPInputStream;

import static com.golem.golemmod.Main.coolFormat;
import static com.golem.golemmod.Main.mc;

public class StatCommand extends CommandBase implements Help {

	private final List<String> helpStrings;

	public StatCommand() {
		helpStrings = new ArrayList<>();
	}

	@Override
	public Help getHelp() {
		return this;
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 0;
	}

	@Override
	public List<String> getHelpStrings() {
		return helpStrings;
	}

	@Override
	public List<String> getHoverStrings() {
		return Arrays.asList(
				EnumChatFormatting.BLUE + "====================Kuudra help menu!====================",
				EnumChatFormatting.RESET + "\n",
				example() + "                  Checks kuudra stats of a person!                  ",
				EnumChatFormatting.RESET + "\n",
				EnumChatFormatting.GOLD + "/ks help" +
				EnumChatFormatting.GRAY + " ⬅ Shows this elaborate menu.",
				EnumChatFormatting.RESET + "\n",
				EnumChatFormatting.RESET + "\n",
				EnumChatFormatting.GOLD + "/ks [ign]" +
				EnumChatFormatting.GRAY  + " ⬅ Shows player's kuudra stats unless [ign] is specified",
				EnumChatFormatting.RESET + "\n",
				example() + "E.g. /ks duophug",
				EnumChatFormatting.RESET + "\n",
				EnumChatFormatting.RESET + "\n",
				EnumChatFormatting.RED + "Legend:" +
				EnumChatFormatting.RESET + "\n  " +
				EnumChatFormatting.DARK_AQUA + "ign: Pheiaa" +
				EnumChatFormatting.RESET + "\n",
				EnumChatFormatting.BLUE + " ======================================================== "
		);
	}

	@Override
	public String getHelpMessage() {
		return
				EnumChatFormatting.GRAY + "▶ " +
				EnumChatFormatting.GOLD + "/kuudrastats " +
				EnumChatFormatting.AQUA + "ign" +
				example() +	"(Aliases: /kuudra /ks /stats)" +
				EnumChatFormatting.RESET + "\n" +
				EnumChatFormatting.WHITE + "Try it out! /ks drfie" +
				EnumChatFormatting.RESET + "\n";
	}

	@Override
	public void addHelpString() {
		helpStrings.add(getHelpMessage());
	}

	@Override
	public String getCommandName() {
		return "kuudrastats";
	}

	@Override
	public List<String> getCommandAliases() {
		List<String> al = new ArrayList<>();
		al.add("kuudra");
		al.add("ks");
		al.add("stats");
		return al;
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/kuudrastats help for more information" +
				"\n" +
				"Try it out! /ks drfie";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (args.length == 0) {
			sendHelpMessage();
		}

		if (args.length == 1) {
			if (args[0].equals("help")) {
				sendHelpMessage();
				return;
			}
			try {
				String ign = args[0];
				String uuid = fetchUUID(ign);
				JsonArray profileData = fetchProfileData(uuid);
				processProfileData(profileData, uuid, ign);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public String example() {
		return EnumChatFormatting.GRAY + " " + EnumChatFormatting.ITALIC;
	}

	private void sendHelpMessage() {
		StringBuilder sb = new StringBuilder();
		for (String str : getHoverStrings()) {
			sb.append(str);
		}
		String hover = sb.toString();
		Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(getHelpMessage()).setChatStyle(new ChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(hover)))));
	}

	public JsonArray fetchProfileData(String uuid) {
		try {
			String urlString = "https://mastermindgolem.pythonanywhere.com/?uuid=" + uuid;
			JsonObject pageData = new RequestUtil().sendGetRequest(urlString).getJsonAsObject();

			if (!pageData.get("success").getAsBoolean()) {
				System.out.println(pageData);
				return null;
			}

			return pageData.get("profiles").getAsJsonArray();

		} catch (Exception e) {e.printStackTrace();}
		return null;
	}

	public String fetchUUID(String ign) {
		try {
			URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + ign);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");

			int responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				StringBuilder response = new StringBuilder();
				String line;

				while ((line = reader.readLine()) != null) {
					response.append(line);
				}

				reader.close();

				String jsonResponse = response.toString();
				JsonParser parser = new JsonParser();
				JsonObject jsonObject = parser.parse(jsonResponse).getAsJsonObject();

				return jsonObject.get("id").getAsString();
			} else {
				System.out.println("Request failed with response code: " + responseCode);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}


		return null;
	}

	public void processProfileData(JsonArray data, String uuid, String ign) {

		try {
			for (JsonElement profile : data) {
				if (!(boolean) getOrDefault(profile.getAsJsonObject(), "selected", "bool")) {
					continue;
				}
				int KuudraScore;
				double KuudraLevel;
				int MageReputation;
				int BarbarianReputation;
				int BasicComps;
				int HotComps;
				int BurningComps;
				int FieryComps;
				int InfernalComps;
				int MagicalPower;
				boolean ElleQuest;
				boolean Level200Gdrag = false;
				int VanquisherChance = 0;
				boolean WitherImpactWeapon = false;
				boolean PrecursorEye = false;
				boolean GyrokineticWand = false;
				boolean WardenHelmet = false;
				boolean BillionBank;
				boolean ReaperArmor;
				boolean ReaperChestplate = false;
				boolean ReaperLeggings = false;
				boolean ReaperBoots = false;
				boolean DuplexTerm = false;
				boolean FatalTempoTerm = false;
				boolean RendTerm = false;
				JsonObject AuroraChestplate = null;
				JsonObject AuroraLeggings = null;
				JsonObject AuroraBoots = null;
				JsonObject TerrorChestplate = null;
				JsonObject TerrorLeggings = null;
				JsonObject TerrorBoots = null;
				JsonObject Equipment1 = null;
				JsonObject Equipment2 = null;
				JsonObject Equipment3 = null;
				JsonObject Equipment4 = null;
				System.out.println(profile.getAsJsonObject().get("cute_name").getAsString());
				JsonObject profileData = profile.getAsJsonObject().get("members").getAsJsonObject().get(uuid).getAsJsonObject();
				JsonArray petData = profileData.get("pets").getAsJsonArray();
				for (JsonElement pet : petData) {
					if ((Objects.equals(pet.getAsJsonObject().get("type").getAsString(), "GOLDEN_DRAGON")) && (pet.getAsJsonObject().get("exp").getAsInt() >= 220000000)) Level200Gdrag = true;
				}
				JsonObject netherData = (JsonObject) getOrDefault(profileData, "nether_island_player_data", "jsonobject");
				BasicComps = (int) getOrDefault(netherData, "kuudra_completed_tiers/none", "int");
				HotComps = (int) getOrDefault(netherData, "kuudra_completed_tiers/hot", "int");
				BurningComps = (int) getOrDefault(netherData, "kuudra_completed_tiers/burning", "int");
				FieryComps = (int) getOrDefault(netherData, "kuudra_completed_tiers/fiery", "int");
				InfernalComps = (int) getOrDefault(netherData, "kuudra_completed_tiers/infernal", "int");

				KuudraScore = (int) (BasicComps * 0.5 + HotComps + BurningComps * 2 + FieryComps * 4 + InfernalComps * 8);
				KuudraLevel = KuudraScore / 100F;
				MageReputation = (int) getOrDefault(netherData, "mages_reputation", "int");
				BarbarianReputation = (int) getOrDefault(netherData, "barbarians_reputation", "int");
				ElleQuest = (int) (getOrDefault(profileData, "quests/talk_to_elle_1/completed_at", "int")) > 0;
				MagicalPower = (int) getOrDefault(profileData, "accessory_bag_storage/highest_magical_power", "int");
				BillionBank = ((BigInteger) getOrDefault(profile.getAsJsonObject(), "banking/balance", "bigint")).compareTo(new BigInteger("1000000000")) > 0;


				JsonArray CurrentArmorData = inventoryData((String) getOrDefault(profileData, "inv_armor/data", "str"));
				JsonArray InventoryData = inventoryData((String) getOrDefault(profileData, "inv_contents/data", "str"));
				JsonArray CurrentEquipmentData = inventoryData((String) getOrDefault(profileData, "equippment_contents/data", "str"));
				Equipment1 = CurrentEquipmentData.get(0).getAsJsonObject();
				Equipment2 = CurrentEquipmentData.get(1).getAsJsonObject();
				Equipment3 = CurrentEquipmentData.get(2).getAsJsonObject();
				Equipment4 = CurrentEquipmentData.get(3).getAsJsonObject();
				JsonArray EnderChestContents = inventoryData((String) getOrDefault(profileData, "ender_chest_contents/data", "str"));
				JsonArray WardrobeContents = inventoryData((String) getOrDefault(profileData, "wardrobe_contents/data", "str"));
				JsonArray BackpackContents = new JsonArray();
				for (Map.Entry<String, JsonElement> backpack : ((JsonObject) getOrDefault(profileData, "backpack_contents", "jsonobject")).entrySet()) BackpackContents.addAll(
						inventoryData(backpack.getValue().getAsJsonObject().get("data").getAsString())
				);

				JsonArray AllItems = new JsonArray();
				AllItems.addAll(CurrentArmorData);
				AllItems.addAll(InventoryData);
				AllItems.addAll(CurrentEquipmentData);
				AllItems.addAll(EnderChestContents);
				AllItems.addAll(WardrobeContents);
				AllItems.addAll(BackpackContents);

				for (JsonElement item : AllItems) {
					if (!item.isJsonObject()) continue;
					String item_id = Objects.requireNonNull(getOrDefault(item.getAsJsonObject(), "item_id", "str")).toString();
					if (Objects.equals(item_id, "GYROKINETIC_WAND")) GyrokineticWand = true;
					if (Objects.equals(item_id, "PRECURSOR_EYE")) PrecursorEye = true;
					if (Objects.equals(item_id, "WARDEN_HELMET")) WardenHelmet = true;
					if (Objects.equals(item_id, "REAPER_CHESTPLATE")) ReaperChestplate = true;
					if (Objects.equals(item_id, "REAPER_LEGGINGS")) ReaperLeggings = true;
					if (Objects.equals(item_id, "REAPER_BOOTS")) ReaperBoots = true;
					if (Arrays.stream(new Gson().fromJson(item.getAsJsonObject().get("lore").getAsJsonArray(), String[].class)).anyMatch(element -> element.contains("Wither Impact"))) WitherImpactWeapon = true;
					if (Objects.equals(item_id, "TERMINATOR")
							&& Arrays.stream(new Gson().fromJson(item.getAsJsonObject().get("lore").getAsJsonArray(), String[].class)).anyMatch(element -> element.contains("Duplex"))) DuplexTerm = true;
					if (Objects.equals(item_id, "TERMINATOR")
							&& Arrays.stream(new Gson().fromJson(item.getAsJsonObject().get("lore").getAsJsonArray(), String[].class)).anyMatch(element -> element.contains("Fatal Tempo"))) FatalTempoTerm = true;
					if (Objects.equals(item_id, "TERMINATOR")
							&& Arrays.stream(new Gson().fromJson(item.getAsJsonObject().get("lore").getAsJsonArray(), String[].class)).anyMatch(element -> element.contains("Rend"))) RendTerm = true;

					if (item_id.contains("AURORA_") || item_id.contains("TERROR_")) {
						String displayname = item.getAsJsonObject().get("displayname").getAsString();
						int stars = (displayname.split("b", -1).length - 1) * 3 + (displayname.split("d", -1).length - 1) * 3 + (displayname.split("6", -1).length - 1);
						if (displayname.contains("Hot")) stars += 10;
						if (displayname.contains("Burning")) stars += 10;
						if (displayname.contains("Fiery")) stars += 10;
						if (displayname.contains("Infernal")) stars += 10;
						item.getAsJsonObject().addProperty("stars", stars);
						if (item_id.contains("AURORA_CHESTPLATE") && stars > (int) getOrDefault(AuroraChestplate, "stars", "int")) AuroraChestplate = item.getAsJsonObject();
						if (item_id.contains("AURORA_LEGGINGS") && stars > (int) getOrDefault(AuroraLeggings, "stars", "int")) AuroraLeggings = item.getAsJsonObject();
						if (item_id.contains("AURORA_BOOTS") && stars > (int) getOrDefault(AuroraBoots, "stars", "int")) AuroraBoots = item.getAsJsonObject();
						if (item_id.contains("TERROR_CHESTPLATE") && stars > (int) getOrDefault(TerrorChestplate, "stars", "int")) TerrorChestplate = item.getAsJsonObject();
						if (item_id.contains("TERROR_LEGGINGS") && stars > (int) getOrDefault(TerrorLeggings, "stars", "int")) TerrorLeggings = item.getAsJsonObject();
						if (item_id.contains("TERROR_BOOTS") && stars > (int) getOrDefault(TerrorBoots, "stars", "int")) TerrorBoots = item.getAsJsonObject();
					}

				}

				ReaperArmor = ReaperChestplate && ReaperLeggings && ReaperBoots;

				IChatComponent msg;
				DecimalFormat formatter = new DecimalFormat("#,###");
				DecimalFormat formatter2 = new DecimalFormat("#,###.###");


				addChatMessage(EnumChatFormatting.RED + "------------------");
				addChatMessage(EnumChatFormatting.AQUA + "Kuudra Stats for " + ign);
				addChatMessage(EnumChatFormatting.GREEN + "Kuudra Level: " + EnumChatFormatting.YELLOW + (int) KuudraLevel);
				addChatMessage(EnumChatFormatting.GREEN + "Magical Power: " + EnumChatFormatting.YELLOW + formatter.format(MagicalPower));


				msg = new ChatComponentText(
						EnumChatFormatting.AQUA + "Kuudra Completions: " + EnumChatFormatting.GRAY + "(Hover)"
				).setChatStyle(new ChatStyle().setChatHoverEvent(
						new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(
								EnumChatFormatting.GOLD + "Basic: " + EnumChatFormatting.YELLOW + formatter.format(BasicComps) + "\n" +
										EnumChatFormatting.GOLD + "Hot: " + EnumChatFormatting.YELLOW + formatter.format(HotComps) + "\n" +
										EnumChatFormatting.GOLD + "Burning: " + EnumChatFormatting.YELLOW + formatter.format(BurningComps) + "\n" +
										EnumChatFormatting.GOLD + "Fiery: " + EnumChatFormatting.YELLOW + formatter.format(FieryComps) + "\n" +
										EnumChatFormatting.GOLD + "Infernal: " + EnumChatFormatting.YELLOW + formatter.format(InfernalComps)
						))));
				Main.mc.thePlayer.addChatMessage(msg);

				msg = new ChatComponentText(
						EnumChatFormatting.AQUA + "Important Items: " + EnumChatFormatting.GRAY + "(Hover)"
				).setChatStyle(new ChatStyle().setChatHoverEvent(
						new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(
								(WitherImpactWeapon ? EnumChatFormatting.GREEN : EnumChatFormatting.DARK_RED) + "Wither Impact Weapon\n" +
										(PrecursorEye ? EnumChatFormatting.GREEN : EnumChatFormatting.DARK_RED) + "Precursor Eye\n" +
										(GyrokineticWand ? EnumChatFormatting.GREEN : EnumChatFormatting.DARK_RED) + "Gyrokinetic Wand\n" +
										(WardenHelmet ? EnumChatFormatting.GREEN : EnumChatFormatting.DARK_RED) + "Warden Helmet\n" +
										(BillionBank ? EnumChatFormatting.GREEN : EnumChatFormatting.DARK_RED) + "1 Billion Bank\n" +
										(Level200Gdrag ? EnumChatFormatting.GREEN : EnumChatFormatting.DARK_RED) + "Level 200 Golden Dragon\n" +
										(ReaperArmor ? EnumChatFormatting.GREEN : EnumChatFormatting.DARK_RED) + "Reaper Armor\n" +
										(DuplexTerm ? EnumChatFormatting.GREEN : EnumChatFormatting.DARK_RED) + "Duplex Terminator\n" +
										(FatalTempoTerm ? EnumChatFormatting.GREEN : EnumChatFormatting.DARK_RED) + "Fatal Tempo Terminator\n" +
										(RendTerm ? EnumChatFormatting.GREEN : EnumChatFormatting.DARK_RED) + "Rend Terminator"
						))));
				Main.mc.thePlayer.addChatMessage(msg);

				displayItem(AuroraChestplate);
				displayItem(AuroraLeggings);
				displayItem(AuroraBoots);
				displayItem(TerrorChestplate);
				displayItem(TerrorLeggings);
				displayItem(TerrorBoots);
				displayItem(Equipment1);
				displayItem(Equipment2);
				displayItem(Equipment3);
				displayItem(Equipment4);

				msg = new ChatComponentText(
						EnumChatFormatting.AQUA + "General Information: " + EnumChatFormatting.GRAY + "(Hover)"
				).setChatStyle(new ChatStyle().setChatHoverEvent(
						new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(
								EnumChatFormatting.GOLD + "Kuudra Score: " + EnumChatFormatting.YELLOW + formatter.format(KuudraScore) + "\n" +
										EnumChatFormatting.GOLD + "Vanquisher Chance: " + EnumChatFormatting.YELLOW + formatter2.format(VanquisherChance) + "%" + "\n" +
										EnumChatFormatting.GOLD + "Mage Reputation: " + EnumChatFormatting.YELLOW + formatter.format(MageReputation) + "\n" +
										EnumChatFormatting.GOLD + "Barbarian Reputation: " + EnumChatFormatting.YELLOW + formatter.format(BarbarianReputation) + "\n" +
										EnumChatFormatting.GOLD + "Elle Quest: " + (ElleQuest ? EnumChatFormatting.GREEN + "COMPLETED" : EnumChatFormatting.DARK_RED + "NOT COMPLETED")
						))));
				Main.mc.thePlayer.addChatMessage(msg);

				addChatMessage(EnumChatFormatting.RED + "------------------");
			}


		} catch (Exception e) {
			e.printStackTrace();
		}


	}

	public JsonArray inventoryData(String encodedData) {
		JsonArray contents = new JsonArray();
		try {
			NBTTagCompound inv_contents_nbt = CompressedStreamTools.readCompressed(
					new ByteArrayInputStream(Base64.getDecoder().decode(encodedData))
			);
			NBTTagList items = inv_contents_nbt.getTagList("i", 10);
			for (int j = 0; j < items.tagCount(); j++) {
				JsonObject item = getJsonFromNBTEntry(items.getCompoundTagAt(j));
				contents.add(item);
			}

		} catch (EOFException ignored) {} catch (JsonSyntaxException | IOException e) {
			e.printStackTrace();
		}
		return contents;
	}

	public JsonObject getJsonFromNBTEntry(NBTTagCompound tag) {
		if (tag.getKeySet().size() == 0) return null;

		int id = tag.getShort("id");
		int damage = tag.getShort("Damage");
		int count = tag.getShort("Count");
		tag = tag.getCompoundTag("tag");

		if (id == 141) id = 391; //for some reason hypixel thinks carrots have id 141

		NBTTagCompound display = tag.getCompoundTag("display");
		String[] lore = getLore(tag).toArray(new String[0]);;

		Item itemMc = Item.getItemById(id);
		String itemid = "null";
		if (itemMc != null) {
			itemid = itemMc.getRegistryName();
		}
		String displayName = display.getString("Name");
		String[] info = new String[0];
		String clickcommand = "";

		JsonObject item = new JsonObject();
		item.addProperty("displayname", displayName);

		if (tag.hasKey("ExtraAttributes", 10)) {
			NBTTagCompound ea = tag.getCompoundTag("ExtraAttributes");
			item.addProperty("item_id", ea.getString("id"));

			byte[] bytes = null;
			for (String key : ea.getKeySet()) {
				if (key.endsWith("backpack_data") || key.equals("new_year_cake_bag_data")) {
					bytes = ea.getByteArray(key);
					break;
				}
			}
			if (bytes != null) {
				JsonArray bytesArr = new JsonArray();
				for (byte b : bytes) {
					bytesArr.add(new JsonPrimitive(b));
				}
				item.add("item_contents", bytesArr);
			}
			if (ea.hasKey("enchantments")) item.addProperty("enchantments", String.valueOf(ea.getCompoundTag("enchantments")));
		}

		if (lore.length > 0) {
			JsonArray jsonLore = new JsonArray();
			for (String line : lore) {
				jsonLore.add(new JsonPrimitive(line));
			}
			item.add("lore", jsonLore);
		}

		item.addProperty("damage", damage);
		if (count > 1) item.addProperty("count", count);
		item.addProperty("nbttag", tag.toString());

		return item;
	}

	public static List<String> getLore(NBTTagCompound tagCompound) {
		if (tagCompound == null) {
			return Collections.emptyList();
		}
		NBTTagList tagList = tagCompound.getCompoundTag("display").getTagList("Lore", 8);
		List<String> list = new ArrayList<>();
		for (int i = 0; i < tagList.tagCount(); i++) {
			list.add(tagList.getStringTagAt(i));
		}
		return list;
	}

	public static Object getOrDefault(JsonObject jsonObject, String stringPath, String type) {
		String[] path = stringPath.split("/");
		JsonElement result = jsonObject;
		try {
			for (String key : path) {
				result = result.getAsJsonObject().get(key);
			}
		} catch (NullPointerException e) {
			result = null;
		}
		switch (type) {
			case "int":
				return (result == null ? 0 : result.getAsInt());
			case "bigint":
				try {
					return (result == null ? new BigInteger("0") : result.getAsBigDecimal().toBigInteger());
				} catch (NumberFormatException ignored) {return "0";}
			case "str":
				return (result == null ? "" : result.getAsString());
			case "bool":
				return (result != null && result.getAsBoolean());
			case "jsonobject":
				return (result == null ? new JsonObject() : result.getAsJsonObject());
		}
		return null;
	}

	public void displayItem(JsonObject item) {
		try {
			final IChatComponent msg = new ChatComponentText(
					item.get("displayname").getAsString()
			).setChatStyle(new ChatStyle().setChatHoverEvent(
					new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(
							item.get("displayname").getAsString()
									+ "\n"
									+ String.join("\n", new Gson().fromJson(item.get("lore").getAsJsonArray(), String[].class))))));
			Main.mc.thePlayer.addChatMessage(msg);
		} catch (Exception e) {e.printStackTrace();}
	}

	public void addChatMessage(String string) {
		mc.thePlayer.addChatMessage(new ChatComponentText(string));
	}



}
