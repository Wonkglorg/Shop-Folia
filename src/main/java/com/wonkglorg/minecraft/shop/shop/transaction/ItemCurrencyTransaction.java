package com.wonkglorg.minecraft.shop.shop.transaction;

import com.wonkglorg.minecraft.shop.ShopPlugin;
import com.wonkglorg.minecraft.shop.config.ItemConfig.CurrencyDenomination;
import com.wonkglorg.minecraft.shop.shop.transaction.party.TransactionParty;

import static com.wonkglorg.minecraft.shop.shop.transaction.party.TransactionParty.removeItemSmallestStacksFirst;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Item-currency transaction supporting optional condensed denominations.
 * Condensed currency is converted into base currency in the buyer's
 * inventory before the exact payment is transferred.
 */
public class ItemCurrencyTransaction extends ItemTransaction{
	
	private final List<CurrencyDenomination> condensedCurrencies;
	
	private final boolean allowCondensedCurrency = ShopPlugin.getPlugin().getSettingsConfig().isAllowUseCondensedCurrencyForShop();
	
	/**
	 * The validated conversion and payment plan.
	 * This will be set after {@link #canBuyerAcceptItems()} finishes evaluation
	 */
	private PaymentPlan paymentPlan;
	
	public ItemCurrencyTransaction(TransactionParty buyer,
								   TransactionParty seller,
								   int amount,
								   double price,
								   ItemStack tradedStack,
								   ItemStack currency,
								   List<CurrencyDenomination> condensedCurrencies) {
		super(buyer, seller, amount, price, tradedStack, currency);
		
		this.condensedCurrencies = condensedCurrencies == null ? List.of() : List.copyOf(condensedCurrencies);
	}
	
	@Override
	public double getBuyerAvailableFunds() {
		if(!usesCondensedCurrency()){
			return super.getBuyerAvailableFunds();
		}
		
		return buyer.getAvailableItemFunds(currency, condensedCurrencies);
	}
	
	@Override
	public boolean canBuyerAcceptItems() {
		paymentPlan = null;
		
		if(!usesCondensedCurrency()){
			return super.canBuyerAcceptItems();
		}
		
		if(buyerIsAdminShop){
			paymentPlan = new PaymentPlan(List.of(), (int) price);
			return true;
		}
		
		Inventory virtualInventory = buyer.createVirtualInventory();
		
		PaymentPlan plan = createPaymentPlan(virtualInventory, (int) price);
		
		if(plan == null){
			return false;
		}
		
		if(!applyConversions(virtualInventory, plan.denominationsToConvert())){
			return false;
		}
		
		if(removeItemSmallestStacksFirst(virtualInventory, currency, plan.basePayment()) != 0){
			return false;
		}
		
		if(amount > 0){
			ItemStack trade = tradedStack.clone();
			trade.setAmount(amount);
			
			if(!virtualInventory.addItem(trade).isEmpty()){
				return false;
			}
		}
		
		paymentPlan = plan;
		return true;
	}
	
	@Override
	public void execute() {
		if(!usesCondensedCurrency()){
			super.execute();
			return;
		}
		PaymentPlan plan = paymentPlan;
		
		if(!buyerIsAdminShop){
			for(ItemStack denomination : plan.denominationsToConvert()){
				int count = denomination.getAmount();
				int convertedValue = Math.multiplyExact(getValueFromCondensedCurrency(denomination), count);
				
				int remaining = buyer.removeItem(denomination, count);
				if(remaining != 0){
					throw new IllegalStateException("Validated condensed currency could not be removed");
				}
				
				buyer.addItem(currency, convertedValue);
			}
			
			int basePayment = Math.toIntExact(plan.basePayment());
			if(basePayment > 0){
				int remaining = buyer.removeItem(currency, basePayment);
				if(remaining != 0){
					throw new IllegalStateException("Validated base currency could not be removed");
				}
			}
		}
		
		if(!sellerIsAdminShop && plan.basePayment() > 0){
			seller.addItem(currency, plan.basePayment());
		}
		
		if(amount > 0){
			int remaining = seller.removeItem(tradedStack, amount);
			
			if(remaining != 0){
				throw new IllegalStateException("Validated traded items could not be removed");
			}
			buyer.addItem(tradedStack, amount);
		}
		
		paymentPlan = null;
	}
	
	/**
	 * Whether this transaction should use the condensed-currency path.
	 */
	private boolean usesCondensedCurrency() {
		return allowCondensedCurrency && !condensedCurrencies.isEmpty();
	}
	
	/**
	 * Creates a conversion plan using the available base currency first,
	 * followed by condensed denominations in the configured order.
	 *
	 * The planner converts only enough condensed items to cover the
	 * shortfall. Any excess converted value remains as base currency.
	 */
	private PaymentPlan createPaymentPlan(Inventory inventory, int price) {
		if(price < 0){
			return null;
		}
		
		long availableBase = countMatching(inventory, currency);
		long shortfall = Math.max(0L, price - availableBase);
		
		List<ItemStack> conversions = new ArrayList<>();
		
		for(CurrencyDenomination denomination : condensedCurrencies){
			if(shortfall == 0){
				break;
			}
			
			int available = countMatching(inventory, denomination.item());
			
			if(available <= 0){
				continue;
			}
			
			long value = denomination.value();
			
			long needed = 1 + (shortfall - 1) / value;
			
			int toConvert = (int) Math.min(available, needed);
			
			if(toConvert <= 0){
				continue;
			}
			
			ItemStack conversion = denomination.item().clone();
			conversion.setAmount(toConvert);
			
			conversions.add(conversion);
			
			long convertedValue = Math.multiplyExact(toConvert, value);
			
			shortfall = Math.max(0L, shortfall - convertedValue);
		}
		
		if(shortfall > 0){
			return null;
		}
		
		return new PaymentPlan(List.copyOf(conversions), price);
	}
	
	/**
	 * Simulates converting the selected denominations into base currency.
	 */
	private boolean applyConversions(Inventory inventory, List<ItemStack> conversions) {
		for(ItemStack denomination : conversions){
			int count = denomination.getAmount();
			
			if(removeItemSmallestStacksFirst(inventory, denomination, count) != 0){
				return false;
			}
			
			long baseAmount = Math.multiplyExact(count, getValueFromCondensedCurrency(denomination));
			
			if(!addBaseCurrency(inventory, baseAmount)){
				return false;
			}
		}
		
		return true;
	}
	
	private int getValueFromCondensedCurrency(ItemStack item) {
		return condensedCurrencies.stream()
								  .filter(d -> d.item().isSimilar(item))
								  .mapToInt(CurrencyDenomination::value)
								  .findFirst()
								  .orElseThrow(() -> new IllegalArgumentException("Unknown condensed currency item: " + item));
	}
	
	/**
	 * Counts matching items in storage slots.
	 */
	private int countMatching(Inventory inventory, ItemStack item) {
		long total = 0;
		
		for(ItemStack stack : inventory.getStorageContents()){
			if(stack != null && stack.getAmount() > 0 && item.isSimilar(stack)){
				total += stack.getAmount();
			}
		}
		
		return Math.toIntExact(total);
	}
	
	/**
	 * Adds base currency to a virtual inventory in stack-sized chunks.
	 *
	 * @return false if any amount cannot fit.
	 */
	private boolean addBaseCurrency(Inventory inventory, long amount) {
		if(amount < 0){
			return false;
		}
		
		int maxStackSize = Math.max(1, currency.getMaxStackSize());
		
		while(amount > 0){
			int stackAmount = (int) Math.min(amount, maxStackSize);
			
			ItemStack stack = currency.clone();
			stack.setAmount(stackAmount);
			
			if(!inventory.addItem(stack).isEmpty()){
				return false;
			}
			
			amount -= stackAmount;
		}
		
		return true;
	}
	
	private record PaymentPlan(List<ItemStack> denominationsToConvert, int basePayment){
		private PaymentPlan {
			Objects.requireNonNull(denominationsToConvert);
		}
	}
	
	@Override
	public String toString() {
		return "ItemCurrencyTransaction{" +
			   "currency=" +
			   currency +
			   ", condensedCurrencies=" +
			   condensedCurrencies +
			   ", buyer=" +
			   buyer +
			   ", seller=" +
			   seller +
			   ", price=" +
			   price +
			   ", amount=" +
			   amount +
			   ", tradedStack=" +
			   tradedStack +
			   '}';
	}
}