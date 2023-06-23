package com.golem.golemmod.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

import com.golem.golemmod.Main;
import com.golem.golemmod.command.commands.StatCommand;
import com.golem.golemmod.models.AttributePrice;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.ChatComponentText;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import static com.golem.golemmod.Main.*;

public class AuctionHouse {
	public static long lastKnownLastUpdated = 0;
	public static boolean isRunning = false;


	public void run(){
		while(true) {
			try {
				if (configFile.time_between_checks == 0) continue;
				long sleepTime = lastKnownLastUpdated + 60000L * configFile.time_between_checks - System.currentTimeMillis() + 10000L;
				if (sleepTime > 0) {
					try {
						System.out.println("Sleep Time:" + sleepTime);
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				String urlString = "https://mastermindgolem.pythonanywhere.com/?auctions=mb";
				try {
					auctions = new RequestUtil().sendGetRequest(urlString).getJsonAsObject().get("auctions").getAsJsonArray();
					lastKnownLastUpdated = System.currentTimeMillis();
					AttributePrice.checkAuctions(auctions);
				} catch (NullPointerException ignored) {System.out.println("Error fetching auctions");}


				bazaar = new RequestUtil().sendGetRequest("https://api.hypixel.net/skyblock/bazaar").getJsonAsObject();


			} catch (Exception ignored) {}
		}
	}
}