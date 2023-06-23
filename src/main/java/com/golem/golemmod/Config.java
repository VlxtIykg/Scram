package com.golem.golemmod;

import gg.essential.vigilance.Vigilant;
import gg.essential.vigilance.data.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class Config extends Vigilant
{
	public static String stonksFolder;
	public File CONFIG_FILE;


	@Property(
			type = PropertyType.SLIDER,
			name = "Time between AH Checks",
			description = "Time to wait between updating AH data. (0 turns it off)",
			category = "General",
			subcategory = "General",
			max = 30
	)
	public int time_between_checks;
	@Property(
			type = PropertyType.SLIDER,
			name = "Minimum Tier",
			description = "Minimum tier to consider for finding cheapest price per tier in /ap and /ep when tier is not specified.",
			category = "General",
			subcategory = "Kuudra Pricing",
			min = 1,
			max = 5
	)
	public int min_tier;

	@Property(
			type = PropertyType.SLIDER,
			name = "Min. Godroll Price",
			description = "Minimum Price for a combo to be considered a godroll (in millions).",
			category = "General",
			subcategory = "Attribute Overlay",
			min = 1,
			max = 300
	)
	public int min_godroll_price;
	@Property(
			type = PropertyType.SWITCH,
			name = "Display Attribute Overlay",
			description = "Show the best attribute on any attribute item (Will also show if it's a godroll).",
			category = "General",
			subcategory = "Attribute Overlay"
	)
	public boolean attribute_overlay;
	@Property(
			type = PropertyType.SWITCH,
			name = "Display Kuudra Overlay",
			description = "Accurate Kuudra Profit Overlay.",
			category = "General",
			subcategory = "Kuudra Profit Overlay"
	)
	public boolean kuudra_overlay;
	@Property(
			type = PropertyType.SELECTOR,
			name = "Book Valuation",
			description = "Choose whether books are instasold/sell offer. (Hardened Mana is always instasold)",
			category = "General",
			subcategory = "Kuudra Profit Overlay",
			options = {"Instant Sell", "Sell Offer"}
	)
	public int book_sell_method;

	@Property(
			type = PropertyType.SELECTOR,
			name = "Faction",
			description = "Needed to calculate key cost for kuudra.",
			category = "General",
			options = {"Mage", "Barbarian"},
			subcategory = "Kuudra Profit Overlay"
	)
	public int faction;


	private void checkFolderExists() {
		Path directory = Paths.get(stonksFolder);
		if (!Files.exists(directory)) {
			try {
				System.out.println("Created directory!");
				Files.createDirectory(directory);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public Config() {
		super(new File(Config.stonksFolder + "config.toml"), "golemmod", new JVMAnnotationPropertyCollector(),
				new ConfigSorting());

		this.min_tier = 1;
		this.time_between_checks = 5;
		this.min_godroll_price = 50;
		this.attribute_overlay = true;
		this.kuudra_overlay = true;
		this.faction = 0;
		this.book_sell_method = 0;

		this.checkFolderExists();
		this.CONFIG_FILE = new File(Config.stonksFolder + "config.toml");
		this.initialize();
	}

	static {
		Config.stonksFolder = "config/golemmod/";
	}

	public static class ConfigSorting extends SortingBehavior {
		@NotNull
		@Override
		public Comparator<Category> getCategoryComparator() {
			return Comparator.comparingInt(o -> this.categories.indexOf(o.getName()));
		}

		private final List<String> categories = Arrays.asList("General"); //
		// insert categories here
	}
}