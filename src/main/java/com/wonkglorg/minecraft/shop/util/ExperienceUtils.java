package com.wonkglorg.minecraft.shop.util;

import org.bukkit.entity.Player;

public class ExperienceUtils{
	
	private ExperienceUtils() {
		// Utility class
	}
	
	/**
	 * Gets the player's total experience points.
	 *
	 * @param player the player
	 * @return total experience points
	 */
	public static int getTotalExperience(Player player) {
		int level = player.getLevel();
		int experienceAtLevel = getExperienceAtLevel(level);
		int experienceIntoLevel = Math.round(player.getExp() * getExperienceToNextLevel(level));
		
		return experienceAtLevel + experienceIntoLevel;
	}
	
	/**
	 * Sets the player's total experience points.
	 *
	 * @param player the player
	 * @param totalExperience the total experience to set
	 */
	public static void setTotalExperience(Player player, int totalExperience) {
		totalExperience = Math.max(0, totalExperience);
		
		// Reset first so Bukkit doesn't retain the old level/progress.
		player.setLevel(0);
		player.setExp(0.0F);
		player.setTotalExperience(0);
		
		if(totalExperience == 0){
			return;
		}
		
		int level = getLevelForExperience(totalExperience);
		int experienceAtLevel = getExperienceAtLevel(level);
		int experienceIntoLevel = totalExperience - experienceAtLevel;
		int experienceToNextLevel = getExperienceToNextLevel(level);
		
		float progress = experienceIntoLevel / (float) experienceToNextLevel;
		
		// Protect against floating-point/rounding errors.
		progress = Math.clamp(progress, 0.0F, 1.0F);
		
		player.setLevel(level);
		player.setExp(progress);
		player.setTotalExperience(totalExperience);
	}
	
	/**
	 * Gets the level corresponding to the given total experience.
	 *
	 * @param experience total experience
	 * @return the player's level
	 */
	public static int getLevelForExperience(int experience) {
		if(experience <= 0){
			return 0;
		}
		
		int level = 0;
		
		while(getExperienceAtLevel(level + 1) <= experience){
			level++;
		}
		
		return level;
	}
	
	/**
	 * Gets the total experience required to reach the given level.
	 *
	 * @param level the level
	 * @return total experience required for the level
	 */
	public static int getExperienceAtLevel(int level) {
		if(level <= 16){
			return level * level + 6 * level;
		}
		
		if(level <= 31){
			return (int) (2.5 * level * level - 40.5 * level + 360);
		}
		
		return (int) (4.5 * level * level - 162.5 * level + 2220);
	}
	
	/**
	 * Gets the amount of experience required to advance from the given level
	 * to the next level.
	 *
	 * @param level the current level
	 * @return experience required for the next level
	 */
	public static int getExperienceToNextLevel(int level) {
		if(level >= 30){
			return 9 * level - 158;
		}
		
		if(level >= 15){
			return 5 * level - 38;
		}
		
		return 2 * level + 7;
	}
}