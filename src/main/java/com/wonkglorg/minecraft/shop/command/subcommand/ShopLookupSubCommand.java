package com.wonkglorg.minecraft.shop.command.subcommand;

import com.mojang.brigadier.context.CommandContext;
import com.wonkglorg.minecraft.command.paged.ChatPagination;
import com.wonkglorg.minecraft.command.paged.PageResult;
import com.wonkglorg.minecraft.command.paged.data.DataManager;
import com.wonkglorg.minecraft.config.LangManager;
import com.wonkglorg.minecraft.config.lang.LangRequest;
import com.wonkglorg.minecraft.shop.ShopPlugin;
import com.wonkglorg.minecraft.shop.db.ShopDatabase.ShopHistoryData;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.util.ItemNameUtil;
import com.wonkglorg.minecraft.util.date.DurationBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public class ShopLookupSubCommand{
	
	private final ShopPlugin plugin;
	private final LangManager lang;
	
	private final DataManager<ShopHistoryPagination> dataManager = new DataManager<>(-1);
	
	public ShopLookupSubCommand() {
		this.plugin = ShopPlugin.getPlugin();
		this.lang = ShopPlugin.langManager();
	}
	
	public int lookup(CommandContext<CommandSourceStack> ctx, Map<String, String> args) {
		
		if(!(ctx.getSource().getSender() instanceof Player player)){
			return -1;
		}
		
		/*
		 * If page is present AND there are no other arguments,
		 * use the existing pagination result.
		 */
		boolean hasPage = args.containsKey("page");
		
		boolean hasFilters = args.keySet().stream().anyMatch(key -> !key.equals("page"));
		
		if(hasPage && !hasFilters){
			dataManager.get(player.getUniqueId()).ifPresent(d -> {
				try{
					int page = Integer.parseInt(args.get("page")) - 1;
					
					d.setPage(page).thenAccept(_ -> d.sendToAudience(player));
					
				} catch(Exception ignored){
				}
			});
			
			return 1;
		}
		
		UUID user;
		Integer radius = null;
		
		Long before = null;
		Long after = null;
		
		ShopHistoryAction action = null;
		
		/*
		 * ---------------------------------------------------------
		 * User
		 * ---------------------------------------------------------
		 */
		if(args.containsKey("user")){
			
			OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args.get("user"));
			
			if(!offlinePlayer.hasPlayedBefore()){
				return -1;
			}
			
			user = offlinePlayer.getUniqueId();
		} else {
			user = null;
		}
		
		/*
		 * ---------------------------------------------------------
		 * Radius
		 * ---------------------------------------------------------
		 */
		if(args.containsKey("radius")){
			
			try{
				radius = Integer.parseInt(args.get("radius"));
			} catch(NumberFormatException e){
				return -1;
			}
		}
		
		/*
		 * ---------------------------------------------------------
		 * Time
		 * ---------------------------------------------------------
		 */
		if(args.containsKey("before")){
			before = parseTime(args.get("before"));
		}
		
		if(args.containsKey("after")){
			after = parseTime(args.get("after"));
		}
		
		/*
		 * ---------------------------------------------------------
		 * Action
		 * ---------------------------------------------------------
		 */
		if(args.containsKey("action")){
			
			try{
				action = ShopHistoryAction.valueOf(args.get("action").toUpperCase());
			} catch(IllegalArgumentException ignored){
			}
		}
		
		int page = 0;
		
		if(hasPage){
			try{
				page = Integer.parseInt(args.get("page")) - 1;
			} catch(Exception _){
			}
		}
		
		ShopHistoryPagination pagination = new ShopHistoryPagination(25,
				radius,
				action,
				before,
				after,
				(parent, providedPage) -> ShopPlugin.shopManager()
													.getDatabase()
													.getHistory(user,
															player.getLocation(),
															parent.radius,
															parent.action,
															parent.after,
															parent.before,
															parent.getRequestTime(),
															25,
															providedPage));
		
		dataManager.add(player.getUniqueId(), pagination);
		pagination.setPage(page).thenAccept(_ -> pagination.sendToAudience(player));
		return 1;
	}
	
	private long parseTime(String input) {
		return System.currentTimeMillis() - DurationBuilder.create(input).toMillis();
	}
	
	private class ShopHistoryPagination extends ChatPagination<ShopHistoryData, ShopHistoryPagination>{
		
		@Getter
		private final Integer radius;
		@Getter
		private final ShopHistoryAction action;
		@Getter
		private final Long before;
		@Getter
		private final Long after;
		
		public ShopHistoryPagination(int pageSize,
									 Integer radius,
									 ShopHistoryAction action,
									 Long before,
									 Long after,
									 BiFunction<ShopHistoryPagination, Integer, CompletableFuture<PageResult<ShopHistoryData>>> entries) {
			super(pageSize, entries);
			this.radius = radius;
			this.action = action;
			this.before = before;
			this.after = after;
		}
		
		@Override
		protected @NotNull List<Component> constructHeader() {
			return lang.request("command.shop.lookup.header").toComponent();
		}
		
		@Override
		protected @NotNull List<Component> constructHeaderEmpty() {
			return lang.request("command.shop.lookup.data-no-results").toComponent();
		}
		
		@Override
		protected @NotNull List<Component> constructEntry(int entryCount, ShopHistoryData data) {
			try{
				List<Component> entry = applyChanges(lang.request("command.shop.lookup.entry." + data.action().toString().toLowerCase() + ".text"),
						data).toComponent();
				
				List<Component> hover = applyChanges(lang.request("command.shop.lookup.entry." + data.action().toString().toLowerCase() + ".hover"),
						data).toComponent();
				
				if(entry.isEmpty()){
					return entry;
				}
				
				if(hover.isEmpty()){
					return entry;
				}
				
				return entry.stream().map(component -> component.hoverEvent(HoverEvent.showText(hover.getFirst()))
																.clickEvent(runCommand("tp %s %s %s %s".formatted(data.worldName(),
																		data.x(),
																		data.y(),
																		data.z())))).toList();
			} catch(Exception e){
				ShopPlugin.logger().severe(e.getMessage(), e);
				throw new RuntimeException(e);
			}
		}
		
		private LangRequest applyChanges(LangRequest request, ShopHistoryData data) {
			//@formatter:off
			request = request
					.replace("%timestamp%", data.timestamp())
					.replace("%time-formatted%", data.formattedTime(requestTime))
					.replace("%player%", data.playerName())
					.replace("%player-uuid%", data.playerUuid().toString())
					.replace("%shop%", data.shopUuid().toString())
					.replace("%world%", data.worldName())
					.replace("%x%", data.x())
					.replace("%y%", data.y())
					.replace("%z%", data.z());
			
			if(data.item() != null){
				request.replace("%item%",()-> ItemNameUtil.getName(data.item()).hoverEvent(ItemNameUtil.getItemHover(data.item())));
			}
			
			if(data.barterItem() != null){
				request.replace("%barter-item%", ()->ItemNameUtil.getName(data.barterItem()).hoverEvent(ItemNameUtil.getItemHover(data.barterItem())));
			}
			
			switch(data.action()) {
				case BUY, SELL, BARTER -> {
					request = request
							.replace("%owner%", data.ownerName())
							.replace("%owner-uuid%", data.ownerUuid().toString())
							.replace("%price%", AbstractShop.formatPrice(data.price()))
							.replace("%amount%", data.amount())
							.replace("%transaction-count%", data.transactionCount());
				}
				
				case GAMBLE -> {
					request = request
							.replace("%owner%", data.ownerName())
							.replace("%owner-uuid%", data.ownerUuid().toString())
							.replace("%price%", AbstractShop.formatPrice(data.price()))
							.replace("%amount%", data.amount());
					if(data.gambleReward() != null){
						request.replace("%reward%", ()->ItemNameUtil.getName(data.gambleReward()).hoverEvent(ItemNameUtil.getItemHover(data.gambleReward())));
					}
				}
				
				case CREATE -> {
					request = request
							.replace("%owner%", data.ownerName())
							.replace("%owner-uuid%", data.ownerUuid().toString())
							.replace("%price%", AbstractShop.formatPrice(data.price()))
							.replace("%amount%", data.amount());
				}
				
				case DESTROY -> {
					request = request
							.replace("%owner%", data.ownerName())
							.replace("%owner-uuid%", data.ownerUuid().toString());
				}
				
				case RESIZE -> {
					request = request
							.replace("%owner%", data.ownerName())
							.replace("%owner-uuid%", data.ownerUuid().toString())
							.replace("%amount%", data.amount());
				}
			//@formatter:on
			}
			return request;
		}
		
		@Override
		protected @NotNull List<Component> constructFooter() {
			return lang.request("command.shop.lookup.footer").replace("%page%", getPageDisplay()).toComponent();
		}
		
		@Override
		protected @NotNull List<Component> constructFooterEmpty() {
			return List.of();
		}
		
		@Override
		protected @NotNull List<Component> pageControls() {
			
			int page = getPage();
			
			if(page < 0){
				return List.of();
			}
			
			Component back = page > 0 ? text("⬅ Previous").clickEvent(runCommand("shop lookup page:" + (getPageDisplay() - 1))) : empty();
			
			Component forward = hasNextPage() ? text(" Next ➡").clickEvent(runCommand("shop lookup page:" + (getPageDisplay() + 1))) : empty();
			
			return List.of(back.append(space()).append(forward));
		}
		
	}
	
	public enum ShopHistoryAction{
		
		BUY,
		SELL,
		BARTER,
		GAMBLE,
		
		CREATE,
		INIT,
		DESTROY,
		RESIZE
	}
}