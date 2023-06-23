package com.golem.golemmod.command.commands;

import net.minecraft.util.ChatComponentText;

import java.util.List;

public interface Help {
	List<String> getHelpStrings();

	String getHelpMessage();

	List<String> getHoverStrings();

	List<String> getCommandAliases();

//	void addHelpString(String[] helpString);
	Help getHelp(); // New getHelp() method

	void addHelpString();
}
