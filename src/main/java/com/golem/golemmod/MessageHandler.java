package com.golem.golemmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class MessageHandler {
	public static void sound() {
		final float volF = 1;
		ISound sound = new PositionedSound(new ResourceLocation("note.pling")) {{
			volume = volF;
			pitch = 2f;
			repeat = false;
			repeatDelay = 0;
			attenuationType = ISound.AttenuationType.NONE;
		}};
		float oldLevel = Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.RECORDS);
		Minecraft.getMinecraft().gameSettings.setSoundLevel(SoundCategory.RECORDS, 1);
		Minecraft.getMinecraft().getSoundHandler().playSound(sound);
		Minecraft.getMinecraft().gameSettings.setSoundLevel(SoundCategory.RECORDS, oldLevel);
	}
}
