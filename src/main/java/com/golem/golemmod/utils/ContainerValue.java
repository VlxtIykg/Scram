package com.golem.golemmod.utils;

import com.golem.golemmod.Main;
import com.golem.golemmod.injection.mixins.minecraft.client.AccessorGuiContainer;
import com.golem.golemmod.models.AttributePrice;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.golem.golemmod.Main.configFile;
import static com.golem.golemmod.models.AttributePrice.*;

public class ContainerValue {
	public static boolean isActive = false;

	@SubscribeEvent
	public void guiDraw(GuiScreenEvent.BackgroundDrawnEvent event) {
		try {
			if (!(event.gui instanceof GuiChest)) return;
			if (!isActive) return;

			GuiChest gui = (GuiChest) event.gui;
			Container container = gui.inventorySlots;
			if (!(container instanceof ContainerChest)) return;
			String chestName = ((ContainerChest) container).getLowerChestInventory().getDisplayName().getUnformattedText();
			if (chestName.contains("Paid Chest") || chestName.contains("Free Chest")) return;
			List<Slot> chestInventory = ((GuiChest) Minecraft.getMinecraft().currentScreen).inventorySlots.inventorySlots;
			List<String> displayStrings = new ArrayList<>();
			BigInteger totalValue = new BigInteger("0");
			new BigInteger("0");
			BigInteger totalProfit;

			int xSize = ((AccessorGuiContainer) gui).getXSize();
			int guiLeft = ((AccessorGuiContainer) gui).getGuiLeft();
			int guiTop = ((AccessorGuiContainer) gui).getGuiTop();

			chestInventory = chestInventory.subList(0, 33);
			for (Slot slot : chestInventory) {
				try {
					if (!slot.getHasStack() || slot.getStack().getItem() == Item.getItemFromBlock(Blocks.stained_glass_pane))
						continue;
					NBTTagCompound nbt = slot.getStack().serializeNBT().getCompoundTag("tag").getCompoundTag("ExtraAttributes").getCompoundTag("attributes");
					String item_id = slot.getStack().serializeNBT().getCompoundTag("tag").getCompoundTag("ExtraAttributes").getString("id");
					for (String key : all_kuudra_categories) {
						if (!item_id.contains(key)) continue;
						if ((key.equals("HELMET") || key.equals("CHESTPLATE") || key.equals("LEGGINGS") || key.equals("BOOTS")) &&
								!item_id.contains("AURORA") && !item_id.contains("CRIMSON") && !item_id.contains("TERROR") && !item_id.contains("FERVOR") && !item_id.contains("HOLLOW")
						) continue;

						String best_attribute = "";
						int best_tier = 0;
						int best_value = 0;
						int added_value = 0;

						for (String key2 : nbt.getKeySet()) {
							ArrayList<JsonObject> items = AttributePrices.get(key).get(key2);
							if (items == null || items.size() == 0) {
								continue;
							}
							items = items.stream().filter(i -> i.get(key2).getAsInt() >= configFile.min_tier).collect(Collectors.toCollection(ArrayList::new));
							if (items.size() == 0) {
								continue;
							}
							items.sort(Comparator.comparingDouble((JsonObject o) -> o.get("price_per_tier").getAsDouble()));
							int value = items.get(0).get("price_per_tier").getAsInt();
							added_value += (value * Math.pow(2, nbt.getInteger(key2) - 1));
							if (value * Math.pow(2, nbt.getInteger(key2) - 1) > best_value) {
								best_value = (int) (value * Math.pow(2, nbt.getInteger(key2) - 1));
								best_attribute = key2;
								best_tier = nbt.getInteger(key2);
							}
						}
						JsonObject comboitem;
						comboitem = AttributePrice.getComboValue(item_id, new ArrayList<>(nbt.getKeySet()));
						if (best_tier == 0) continue;
						if (best_tier > 5 && comboitem != null && comboitem.get("starting_bid").getAsInt() > Main.configFile.min_godroll_price * 1000000) {
							displayStrings.add(AttributePrice.ShortenedAttribute(new ArrayList<>(nbt.getKeySet()).get(0)) + " " + nbt.getInteger(new ArrayList<>(nbt.getKeySet()).get(0)) + " " + AttributePrice.ShortenedAttribute(new ArrayList<>(nbt.getKeySet()).get(1)) + " " + nbt.getInteger(new ArrayList<>(nbt.getKeySet()).get(1)) + " " + slot.getStack().getDisplayName() + EnumChatFormatting.YELLOW + ": " + EnumChatFormatting.GREEN + Main.formatNumber(comboitem.get("starting_bid").getAsDouble() + added_value));
							totalValue = totalValue.add(new BigInteger(comboitem.get("starting_bid").getAsString())).add(new BigInteger(String.valueOf(added_value)));
						} else if (comboitem != null && comboitem.get("starting_bid").getAsInt() > Math.max(best_value, Main.configFile.min_godroll_price * 1000000)) {
							displayStrings.add(AttributePrice.ShortenedAttribute(new ArrayList<>(nbt.getKeySet()).get(0)) + " " + AttributePrice.ShortenedAttribute(new ArrayList<>(nbt.getKeySet()).get(1)) + " " + slot.getStack().getDisplayName() + EnumChatFormatting.YELLOW + ": " + EnumChatFormatting.GREEN + Main.formatNumber(comboitem.get("starting_bid").getAsDouble()));
							totalValue = totalValue.add(new BigInteger(comboitem.get("starting_bid").getAsString()));
						} else if (best_value > LowestBin.get(item_id)) {
							displayStrings.add(AttributePrice.ShortenedAttribute(best_attribute) + " " + best_tier + " " + slot.getStack().getDisplayName() + EnumChatFormatting.YELLOW + ": " + EnumChatFormatting.GREEN + Main.formatNumber(best_value));
							totalValue = totalValue.add(new BigInteger(String.valueOf(best_value)));
						} else {
							displayStrings.add("LBIN " + slot.getStack().getDisplayName() + EnumChatFormatting.YELLOW + ": " + EnumChatFormatting.GREEN + Main.formatNumber(LowestBin.get(item_id)));
							totalValue = totalValue.add(new BigInteger(String.valueOf(LowestBin.get(item_id))));
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}


			GlStateManager.disableLighting();
			GlStateManager.translate(0, 0, 200);
			for (int i = 0; i < displayStrings.size(); i++) {
				Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(
						displayStrings.get(i),
						guiLeft + xSize + 5,
						guiTop + 5 + 10*i,
						0xffffffff
				);
			}

			Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(
					EnumChatFormatting.YELLOW + "Total Value: " + EnumChatFormatting.GREEN + Main.formatNumber(totalValue),
					guiLeft + xSize + 5,
					guiTop + 5 + 10 * displayStrings.size(),
					0xffffffff
			);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
