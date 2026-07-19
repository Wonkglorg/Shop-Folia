/*
Copyright 2019 https://github.com/OughtToPrevail

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.wonkglorg.pluginnames;

import com.wonkglorg.utilitylib.config.LangManager;
import com.wonkglorg.utilitylib.config.types.LangConfig;
import com.wonkglorg.utilitylib.scheduler.RegionScheduler;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Main Entry point of the plugin
 */
public class Main extends JavaPlugin{
	private LangManager langManager;
	private RegionScheduler regionScheduler;
	private static Main plugin;
	
	@Override
	public void onLoad() {
		Main.plugin = this;
		langManager = LangManager.createInstance(this);
		regionScheduler = RegionScheduler.createInstance(this);
		PluginLogger.createInstance(this);
		langManager.setDefaultLang(Locale.US);
		langManager.addLanguage(new LangConfig(this, Path.of("lang", "en-us.yml")), Locale.US);
		langManager.addAllLangFilesFromPath(Paths.get("lang"));
	}
	
	@Override
	public void onEnable() {
	
	}
	
	@Override
	public void onDisable() {
		regionScheduler.shutdown();
	}
	
	public static Main instance() {
		return plugin;
	}
	
	public LangManager langManager() {
		return langManager;
	}
	
	public RegionScheduler regionScheduler() {
		return regionScheduler;
	}
}