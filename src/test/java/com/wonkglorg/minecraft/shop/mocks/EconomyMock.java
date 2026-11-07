package com.wonkglorg.minecraft.shop.mocks;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyMock implements Economy{
	private Map<UUID, Double> ownedCurrency = new ConcurrentHashMap<>();
	
	@Override
	public boolean isEnabled() {
		return false;
	}
	
	@Override
	public String getName() {
		return "";
	}
	
	@Override
	public boolean hasBankSupport() {
		return false;
	}
	
	@Override
	public int fractionalDigits() {
		return 0;
	}
	
	@Override
	public String format(double v) {
		return "";
	}
	
	@Override
	public String currencyNamePlural() {
		return "";
	}
	
	@Override
	public String currencyNameSingular() {
		return "";
	}
	
	@Override
	public boolean hasAccount(String s) {
		return false;
	}
	
	@Override
	public boolean hasAccount(OfflinePlayer offlinePlayer) {
		return false;
	}
	
	@Override
	public boolean hasAccount(String s, String s1) {
		return false;
	}
	
	@Override
	public boolean hasAccount(OfflinePlayer offlinePlayer, String s) {
		return false;
	}
	
	@Override
	public double getBalance(String s) {
		return 0;
	}
	
	@Override
	public double getBalance(OfflinePlayer offlinePlayer) {
		return ownedCurrency.getOrDefault(offlinePlayer.getUniqueId(), 0.0);
	}
	
	@Override
	public double getBalance(String s, String s1) {
		return 0;
	}
	
	@Override
	public double getBalance(OfflinePlayer offlinePlayer, String s) {
		return 0;
	}
	
	@Override
	public boolean has(String s, double v) {
		return false;
	}
	
	@Override
	public boolean has(OfflinePlayer offlinePlayer, double v) {
		return false;
	}
	
	@Override
	public boolean has(String s, String s1, double v) {
		return false;
	}
	
	@Override
	public boolean has(OfflinePlayer offlinePlayer, String s, double v) {
		return false;
	}
	
	@Override
	public EconomyResponse withdrawPlayer(String s, double v) {
		return null;
	}
	
	@Override
	public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, double v) {
		double balance = ownedCurrency.getOrDefault(offlinePlayer.getUniqueId(), 0.0);
		if(balance < v){
			return new EconomyResponse(v, balance, ResponseType.FAILURE, "Not enough balance");
		}
		
		ownedCurrency.put(offlinePlayer.getUniqueId(), balance - v);
		return new EconomyResponse(v, balance, ResponseType.SUCCESS, null);
	}
	
	@Override
	public EconomyResponse withdrawPlayer(String s, String s1, double v) {
		return null;
	}
	
	@Override
	public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, String s, double v) {
		return null;
	}
	
	@Override
	public EconomyResponse depositPlayer(String s, double v) {
		return null;
	}
	
	@Override
	public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, double v) {
		double balance = ownedCurrency.getOrDefault(offlinePlayer.getUniqueId(), 0.0);
		ownedCurrency.put(offlinePlayer.getUniqueId(), balance + v);
		return new EconomyResponse(v, balance, ResponseType.SUCCESS, null);
	}
	
	@Override
	public EconomyResponse depositPlayer(String s, String s1, double v) {
		return null;
	}
	
	@Override
	public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, String s, double v) {
		return null;
	}
	
	@Override
	public EconomyResponse createBank(String s, String s1) {
		return null;
	}
	
	@Override
	public EconomyResponse createBank(String s, OfflinePlayer offlinePlayer) {
		return null;
	}
	
	@Override
	public EconomyResponse deleteBank(String s) {
		return null;
	}
	
	@Override
	public EconomyResponse bankBalance(String s) {
		return null;
	}
	
	@Override
	public EconomyResponse bankHas(String s, double v) {
		return null;
	}
	
	@Override
	public EconomyResponse bankWithdraw(String s, double v) {
		return null;
	}
	
	@Override
	public EconomyResponse bankDeposit(String s, double v) {
		return null;
	}
	
	@Override
	public EconomyResponse isBankOwner(String s, String s1) {
		return null;
	}
	
	@Override
	public EconomyResponse isBankOwner(String s, OfflinePlayer offlinePlayer) {
		return null;
	}
	
	@Override
	public EconomyResponse isBankMember(String s, String s1) {
		return null;
	}
	
	@Override
	public EconomyResponse isBankMember(String s, OfflinePlayer offlinePlayer) {
		return null;
	}
	
	@Override
	public List<String> getBanks() {
		return List.of();
	}
	
	@Override
	public boolean createPlayerAccount(String s) {
		return false;
	}
	
	@Override
	public boolean createPlayerAccount(OfflinePlayer offlinePlayer) {
		return false;
	}
	
	@Override
	public boolean createPlayerAccount(String s, String s1) {
		return false;
	}
	
	@Override
	public boolean createPlayerAccount(OfflinePlayer offlinePlayer, String s) {
		return false;
	}
}
