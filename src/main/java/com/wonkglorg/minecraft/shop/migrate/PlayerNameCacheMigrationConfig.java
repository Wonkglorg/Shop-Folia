package com.wonkglorg.minecraft.shop.migrate;

import com.wonkglorg.minecraft.config.types.Config;
import com.wonkglorg.minecraft.shop.Main;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerNameCacheMigrationConfig extends Config{
	public PlayerNameCacheMigrationConfig() {
		super(Main.getPlugin(), Path.of("migration", "names", "playerNameCache.yml"));
	}
	
	public Map<UUID, String> getNames() {
		Map<UUID, String> names = new HashMap<>();
		
		for(var entry : getKeys(false)){
			names.put(UUID.fromString(entry), getString(entry));
		}
		return names;
	}
	
}
