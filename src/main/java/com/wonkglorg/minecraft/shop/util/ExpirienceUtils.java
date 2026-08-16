package com.wonkglorg.minecraft.shop.util;

import org.bukkit.entity.Player;

public class ExpirienceUtils{
	public static int getTotalExperience(Player player) {
		int level = player.getLevel();
		
		if(level <= 16){
			return level * level + 6 * level + (int) (player.getExp() * getExperienceToNextLevel(level));
		}
		
		if(level <= 31){
			return (int) (2.5 * level * level - 40.5 * level + 360 + player.getExp() * getExperienceToNextLevel(level));
		}
		
		return (int) (4.5 * level * level - 162.5 * level + 2220 + player.getExp() * getExperienceToNextLevel(level));
	}
	
	public static void setTotalExperience(Player player, int totalExperience) {
		totalExperience = Math.max(0, totalExperience);
		
		player.setLevel(0);
		player.setExp(0);
		player.setTotalExperience(0);
		
		if(totalExperience == 0){
			return;
		}
		
		int level = getLevelForExperience(totalExperience);
		int experienceAtLevel = getExperienceAtLevel(level);
		int experienceIntoLevel = totalExperience - experienceAtLevel;
		int experienceToNextLevel = getExperienceToNextLevel(level);
		
		player.setLevel(level);
		player.setExp((float) experienceIntoLevel / experienceToNextLevel);
		player.setTotalExperience(totalExperience);
	}
	
	public static int getLevelForExperience(int experience) {
		if(experience < 0){
			return 0;
		}
		
		if(experience < 352){
			return (int) ((Math.sqrt(72 * experience + 81) - 9) / 2);
		}
		
		if(experience < 1507){
			return (int) ((Math.sqrt(40 * experience - 7839) + 81) / 10);
		}
		
		return (int) ((Math.sqrt(72 * experience - 54215) + 325) / 18);
	}
	
	public static int getExperienceAtLevel(int level) {
		if(level <= 16){
			return level * level + 6 * level;
		}
		
		if(level <= 31){
			return (int) (2.5 * level * level - 40.5 * level + 360);
		}
		
		return (int) (4.5 * level * level - 162.5 * level + 2220);
	}
	
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
