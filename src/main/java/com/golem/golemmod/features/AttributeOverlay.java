package com.golem.golemmod.features;

import com.golem.golemmod.Main;
import com.golem.golemmod.models.AttributePrice;
import com.google.gson.JsonObject;
import gg.essential.universal.UGraphics;
import gg.essential.universal.UMatrixStack;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

import static com.golem.golemmod.models.AttributePrice.AttributePrices;
import static com.golem.golemmod.models.AttributePrice.all_kuudra_categories;

public class AttributeOverlay {


	public static void drawSlot(Slot slot) {
		if (slot == null || !slot.getHasStack() || !Main.configFile.attribute_overlay) return;

		try {
			NBTTagCompound nbt = slot.getStack().serializeNBT().getCompoundTag("tag").getCompoundTag("ExtraAttributes").getCompoundTag("attributes");
			String item_id = slot.getStack().serializeNBT().getCompoundTag("tag").getCompoundTag("ExtraAttributes").getString("id");
			for (String key : all_kuudra_categories) {
				if (!item_id.contains(key)) continue;

				String best_attribute = "";
				int best_tier = 0;
				int best_value = 0;

				for (String key2 : nbt.getKeySet()) {
					ArrayList<JsonObject> items = AttributePrices.get(key).get(key2);
					items.sort(Comparator.comparingDouble((JsonObject o) -> o.get("price_per_tier").getAsDouble()));
					int value = items.get(0).get("price_per_tier").getAsInt();
					if (value * Math.pow(2, nbt.getInteger(key2)-1) > best_value) {
						best_value = (int) (value * Math.pow(2, nbt.getInteger(key2)-1));
						best_attribute = key2;
						best_tier = nbt.getInteger(key2);
					}
				}
				JsonObject comboitem;
				comboitem = AttributePrice.getComboValue(item_id, new ArrayList<>(nbt.getKeySet()));
				if (comboitem != null && comboitem.get("starting_bid").getAsInt() > Math.max(best_value, Main.configFile.min_godroll_price * 1000000)) {
					UGraphics.disableLighting();
					UGraphics.disableDepth();
					UGraphics.disableBlend();
					UMatrixStack matrixStack = new UMatrixStack();
					matrixStack.push();
					matrixStack.translate(slot.xDisplayPosition, slot.yDisplayPosition, 1f);
					matrixStack.scale(0.8, 0.8, 1.0);

					matrixStack.runWithGlobalState(() -> {
						Main.mc.fontRendererObj.drawString("GR", 0, 0, 0x00FFFF);
					});

					matrixStack.pop();
					UGraphics.enableLighting();
					UGraphics.enableDepth();
//					UGraphics.enableBlend(); Keep the comment in-case it breaks something in the future, temp fix for overlay where it goes behind background
				} else if (best_tier != 0 && !best_attribute.equals("") && best_value > AttributePrice.LowestBin.get(item_id)) {
					GlStateManager.disableLighting();
					GlStateManager.disableDepth();
					GlStateManager.disableBlend();
					Main.mc.fontRendererObj.drawStringWithShadow(String.valueOf(best_tier),
							(float) (slot.xDisplayPosition + 17 - Main.mc.fontRendererObj.getStringWidth(String.valueOf(best_tier))),
							slot.yDisplayPosition + 9,
							0xFFFFFFFF
					);
					GlStateManager.enableLighting();
					GlStateManager.enableDepth();
					UGraphics.disableLighting();
					UGraphics.disableDepth();
					UGraphics.disableBlend();
					UMatrixStack matrixStack = new UMatrixStack();
					matrixStack.push();
					matrixStack.translate(slot.xDisplayPosition, slot.yDisplayPosition, 1f);
					matrixStack.scale(0.8, 0.8, 1.0);

					String finalBest_attribute = best_attribute;
					matrixStack.runWithGlobalState(() -> {
						Main.mc.fontRendererObj.drawString(AttributePrice.ShortenedAttribute(finalBest_attribute), 0, 0, 0x00FFFF);
					});

					matrixStack.pop();
					UGraphics.enableLighting();
					UGraphics.enableDepth();
					UGraphics.enableBlend();
				} else if (nbt.getKeySet().size() > 0){
					UGraphics.disableLighting();
					UGraphics.disableDepth();
					UGraphics.disableBlend();
					UMatrixStack matrixStack = new UMatrixStack();
					matrixStack.push();
					matrixStack.translate(slot.xDisplayPosition, slot.yDisplayPosition, 1f);
					matrixStack.scale(0.8, 0.8, 1.0);

					matrixStack.runWithGlobalState(() -> {
						Main.mc.fontRendererObj.drawString("LBIN", 0, 0, 0x00FFFF);
					});

					matrixStack.pop();
					UGraphics.enableLighting();
					UGraphics.enableDepth();
					UGraphics.enableBlend();
				}


			}
		} catch (Exception ignored) {}

	}
}
