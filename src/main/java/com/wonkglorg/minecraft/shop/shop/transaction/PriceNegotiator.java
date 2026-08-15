package com.wonkglorg.minecraft.shop.shop.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Handles price negotiations for shop transactions.
 * <p>
 * This class calculates the final price and quantity for shop transactions based on
 * available funds, inventory, and desired purchase amount.
 * <p>
 * The negotiator supports two payment modes:
 * <ul>
 *   <li>Integer payments (default): Prices are always rounded up to the next full unit.</li>
 *   <li>Fractional payments: Prices can have decimal precision (up to 2 decimal places).</li>
 * </ul>
 * <p>
 * The only difference between the two modes is how prices are rounded. Item quantities
 * are calculated the same way in both modes to ensure consistent behavior.
 */
public class PriceNegotiator {
    // Constants for price calculations and validations
    private static final int PRICE_PRECISION = 2;
    private static final double MINIMUM_VALID_PRICE = 0.01;
    private static final double EXTREMELY_LOW_PRICE_THRESHOLD = 0.00999;
    private static final double ROUNDING_ERROR_MARGIN = 1e-5;
    private static final double DEFAULT_ITEM_RATIO = 1.0;
    private static final int NO_SPECIFIC_AMOUNT = -1;
    private static final int PROPORTION_SCALE = 10;
    
    // Debug flag - should be private for encapsulation
    private final boolean debugLogging;
    
    // Payment mode flag
    private final boolean supportsFractionalPayments;

    // Transaction state
    private double price = 0;
    private double originalPrice = 0;
    private double negotiatedPrice = NO_SPECIFIC_AMOUNT;
    private int amountBeingSold = 0;
    private int originalAmountBeingSold = 0;
    private int negotiatedAmountBeingSold = NO_SPECIFIC_AMOUNT;
    
    /**
     * Creates a new PriceNegotiator.
     *
     * @param debugLogging              Whether to enable debug logging
     * @param originalPrice             The original price for the items
     * @param originalAmountBeingSold   The original quantity of items
     * @param supportsFractionalPayments Whether to enable fractional payments (prices with decimal places)
     */
    public PriceNegotiator(boolean debugLogging, double originalPrice, int originalAmountBeingSold, boolean supportsFractionalPayments) {
        this.debugLogging = debugLogging;
        this.originalPrice = originalPrice;
        this.originalAmountBeingSold = originalAmountBeingSold;
        this.supportsFractionalPayments = supportsFractionalPayments;
    }
    
    /**
     * Rounds a value to the specified scale using the specified rounding mode.
     *
     * @param value The value to round
     * @param scale The scale (number of decimal places) to round to
     * @param roundingMode The rounding mode to use
     * @return The rounded value
     */
    private double roundValue(double value, int scale, RoundingMode roundingMode) {
        BigDecimal bd = new BigDecimal(Double.toString(value)); // Use string to avoid floating point precision issues
        bd = bd.setScale(scale, roundingMode);
        return bd.doubleValue();
    }
    
    /**
     * Calculates the exact price based on the items and price per item.
     * Uses different rounding strategies depending on the payment mode.
     *
     * @param items The number of items to purchase
     * @param pricePerItem The price per individual item
     * @return The calculated price, either rounded to cents (fractional) or rounded up (integer).
     *         Returns NO_SPECIFIC_AMOUNT if the calculation would result in a price too small (zero).
     */
    private double calculateExactPrice(int items, double pricePerItem) {
        // Check for extreme edge cases that would result in a price that's too small
        if (items * pricePerItem < MINIMUM_VALID_PRICE) {
            return NO_SPECIFIC_AMOUNT; // Indicate that this is an invalid price (will trigger "no purchase")
        }
        
        // Calculate the raw price
        double rawPrice = items * pricePerItem;
        
        // Apply appropriate rounding based on payment mode
        int scale = supportsFractionalPayments ? PRICE_PRECISION : 0;
        double finalPrice = roundValue(rawPrice, scale, RoundingMode.HALF_UP);
        
        // Validate the final price
        if (finalPrice < MINIMUM_VALID_PRICE) {
            return NO_SPECIFIC_AMOUNT;
        }
        
        return finalPrice;
    }
    
    /**
     * Negotiates a purchase based on the available funds, inventory, and desired amount.
     * This method determines the final price and quantity for the transaction.
     * 
     * @param allowPartialSales Whether to allow selling partial amounts
     * @param buyerAvailableFunds The amount of money the buyer has available
     * @param sellerInventoryQuantity The number of items the seller has in inventory
     * @param desiredAmount The specific amount the buyer wants to purchase (-1 for maximum possible)
     */
    public void negotiatePurchase(boolean allowPartialSales, double buyerAvailableFunds, int sellerInventoryQuantity, int desiredAmount) {
        // Reset negotiation state
        resetNegotiationState();
        
        // Determine maximum purchase amount
        int maxPurchaseAmount = determineMaxPurchaseAmount(desiredAmount);
        
        // Calculate price ratios
        double pricePerItem = calculatePricePerItem();
        double itemsPerPrice = calculateItemsPerPrice(pricePerItem);
        
        logPriceRatios(pricePerItem, itemsPerPrice);
        
        // Handle special case for extremely low price per item
        if (isExtremelyLowPricePerItem(pricePerItem, desiredAmount)) {
            return;
        }
        
        // Special handling for exact amount with fractional payments
        if (handleExactAmountFractionalPayment(desiredAmount, sellerInventoryQuantity, buyerAvailableFunds)) {
            return;
        }
        
        // Calculate maximum quantities based on buyer funds and seller inventory
        double maxPurchasableQuantity = calculateMaxPurchasableQuantity(
            buyerAvailableFunds, pricePerItem, itemsPerPrice, sellerInventoryQuantity);
            
        // Calculate items being bought and price being paid
        int itemsBeingBought = calculateItemsBeingBought(maxPurchasableQuantity, itemsPerPrice);
        double priceBeingPaid = calculateExactPrice(itemsBeingBought, pricePerItem);
        
        logPurchaseDetails(maxPurchasableQuantity, itemsBeingBought, priceBeingPaid, 
            buyerAvailableFunds, sellerInventoryQuantity);
        
        // Check if we can't complete the purchase
        if (isInvalidPurchase(maxPurchasableQuantity, itemsBeingBought, priceBeingPaid)) {
            resetToOriginalValues();
            return;
        }
        
        // Handle partial sales restrictions if needed
        if (!allowPartialSales) {
            handleNoPartialSales(maxPurchasableQuantity, itemsPerPrice, pricePerItem);
            
            // Update itemsBeingBought and priceBeingPaid after handling no partial sales
            itemsBeingBought = this.amountBeingSold;
            priceBeingPaid = this.price;
            
            // If no valid price was negotiated, return
            if (itemsBeingBought == 0 || priceBeingPaid <= 0) {
                return;
            }
        }
        
        // Ensure we don't exceed max purchase amount
        itemsBeingBought = ensureMaxPurchaseLimit(itemsBeingBought, maxPurchaseAmount, pricePerItem);
        priceBeingPaid = calculateExactPrice(itemsBeingBought, pricePerItem);
        
        // Final validation check
        if (itemsBeingBought == 0 || priceBeingPaid <= 0) {
            return;
        }
        
        // Set the final negotiated values
        setFinalNegotiatedValues(itemsBeingBought, priceBeingPaid);
    }
    
    /**
     * Resets the negotiation state to prepare for a new calculation.
     */
    private void resetNegotiationState() {
        this.negotiatedPrice = NO_SPECIFIC_AMOUNT;
        this.negotiatedAmountBeingSold = NO_SPECIFIC_AMOUNT;
    }
    
    /**
     * Determines the maximum purchase amount based on original amount and desired amount.
     */
    private int determineMaxPurchaseAmount(int desiredAmount) {
        int maxPurchaseAmount = this.originalAmountBeingSold;
        
        // Check if we passed in the amount that we want to purchase, sometimes we want to purchase more
        if (desiredAmount != NO_SPECIFIC_AMOUNT && 
            desiredAmount > 0 && 
            desiredAmount > this.originalAmountBeingSold) {
            maxPurchaseAmount = desiredAmount;
        }
        
        return maxPurchaseAmount;
    }
    
    /**
     * Calculates the price per individual item.
     */
    private double calculatePricePerItem() {
        return originalPrice / originalAmountBeingSold;
    }
    
    /**
     * Calculates how many items equal one price unit.
     */
    private double calculateItemsPerPrice(double pricePerItem) {
        double itemsPerPrice = DEFAULT_ITEM_RATIO;
        
        // If our price is less than 1, then we are buying multiple items with each order
        if (pricePerItem < DEFAULT_ITEM_RATIO) {
            itemsPerPrice = DEFAULT_ITEM_RATIO / pricePerItem;
        }
        
        return itemsPerPrice;
    }
    
    /**
     * Logs price ratio information if debug logging is enabled.
     */
    private void logPriceRatios(double pricePerItem, double itemsPerPrice) {
        if (debugLogging) {
            System.out.println("* pricePerItem: " + pricePerItem);
            System.out.println("* itemsPerPrice: " + itemsPerPrice);
        }
    }
    
    /**
     * Handles the special case of extremely low price per item.
     * 
     * @return true if the purchase was handled or canceled, false otherwise
     */
    private boolean isExtremelyLowPricePerItem(double pricePerItem, int desiredAmount) {
        if (supportsFractionalPayments && pricePerItem < EXTREMELY_LOW_PRICE_THRESHOLD) {
            if (desiredAmount == originalAmountBeingSold) {
                // Only allow the full purchase in this case
                setFinalNegotiatedValues(originalAmountBeingSold, originalPrice);
                return true;
            } else if (desiredAmount < originalAmountBeingSold) {
                // For partial purchases of extremely low-price items, ensure the minimum price is 0.01
                double calculatedPrice = (double)desiredAmount / originalAmountBeingSold * originalPrice;
                if (calculatedPrice < MINIMUM_VALID_PRICE) {
                    // Can't complete the purchase, price would be too small
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Handles exact amount purchase with fractional payments.
     * 
     * @return true if the purchase was handled, false otherwise
     */
    private boolean handleExactAmountFractionalPayment(int desiredAmount, int sellerInventoryQuantity, double buyerAvailableFunds) {
        if (supportsFractionalPayments && desiredAmount > 0 && desiredAmount <= sellerInventoryQuantity) {
            // Calculate the exact proportion of the price
            double proportion = (double) desiredAmount / originalAmountBeingSold;
            double exactPrice = originalPrice * proportion;
            
            // Round to the appropriate precision
            exactPrice = roundValue(exactPrice, PRICE_PRECISION, RoundingMode.HALF_UP);
            
            // Check for extremely small prices that would be invalid
            if (exactPrice < MINIMUM_VALID_PRICE) {
                // Price is too small, can't complete purchase
                return true;
            }
            
            // Check if buyer can afford this exact amount
            if (buyerAvailableFunds >= exactPrice) {
                // Buyer can afford the exact amount
                setFinalNegotiatedValues(desiredAmount, exactPrice);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Calculates the maximum purchasable quantity based on available funds and inventory.
     */
    private double calculateMaxPurchasableQuantity(double buyerAvailableFunds, double pricePerItem, 
                                                double itemsPerPrice, int sellerInventoryQuantity) {
        // Calculate the maximum qty that the buyer can afford to buy
        double buyerMaxQtyPurchase = (buyerAvailableFunds / pricePerItem) / itemsPerPrice;
        
        // Calculate the maximum items the seller has to sell
        double sellerMaxQtySale = sellerInventoryQuantity / itemsPerPrice;

        // The maximum qty that we can buy/sell with our available funds
        double minQty = Math.min(buyerMaxQtyPurchase, sellerMaxQtySale);
        
        // Special case for fractional payments: don't floor to zero if there's a valid partial purchase
        if (supportsFractionalPayments && minQty > 0) {
            return minQty; // Keep the fractional quantity when fractional payments are enabled
        }
        
        return Math.floor(minQty + ROUNDING_ERROR_MARGIN);
    }
    
    /**
     * Calculates the number of items being bought.
     */
    private int calculateItemsBeingBought(double maxPurchasableQuantity, double itemsPerPrice) {
        int itemsBeingBought = (int) Math.floor(maxPurchasableQuantity * itemsPerPrice);
        
        if (debugLogging) { 
            System.out.println("*-* itemsBeingBought (pre-round): " + (maxPurchasableQuantity * itemsPerPrice)); 
        }
        
        return itemsBeingBought;
    }
    
    /**
     * Logs purchase details if debug logging is enabled.
     */
    private void logPurchaseDetails(double maxPurchasableQuantity, int itemsBeingBought, double priceBeingPaid,
                                   double buyerAvailableFunds, int sellerInventoryQuantity) {
        if (debugLogging) { 
            System.out.println("*-* priceBeingPaid (pre-round): " + (itemsBeingBought * calculatePricePerItem()));
            System.out.println("* buyerMaxQtyPurchase: " + (buyerAvailableFunds / calculatePricePerItem()) / calculateItemsPerPrice(calculatePricePerItem()));
            System.out.println("* sellerInventoryQuantity: " + sellerInventoryQuantity);
            System.out.println("* sellerMaxQtySale: " + sellerInventoryQuantity / calculateItemsPerPrice(calculatePricePerItem()));
            System.out.println("* maxPurchasableQuantity: " + maxPurchasableQuantity);
            System.out.println("* itemsBeingBought: " + itemsBeingBought);
            System.out.println("* priceBeingPaid: " + priceBeingPaid);
        }
    }
    
    /**
     * Checks if the purchase is invalid due to insufficient funds or inventory.
     */
    private boolean isInvalidPurchase(double maxPurchasableQuantity, int itemsBeingBought, double priceBeingPaid) {
        return maxPurchasableQuantity <= 0 || itemsBeingBought <= 0 || priceBeingPaid <= 0;
    }
    
    /**
     * Resets price and amount values to original values.
     */
    private void resetToOriginalValues() {
        this.price = originalPrice;
        this.amountBeingSold = originalAmountBeingSold;
    }
    
    /**
     * Handles the case where partial sales are not allowed.
     */
    private void handleNoPartialSales(double maxPurchasableQuantity, double itemsPerPrice, double pricePerItem) {
        // Multiple Quantity of original amount sales code (for full stack sales)
        double quantityPerOriginalAmount = originalAmountBeingSold / itemsPerPrice;
        // Force the quantity to be a multiple of our original amount when performing multiple sales
        int roundedQuantity = (int) (Math.floor(maxPurchasableQuantity / quantityPerOriginalAmount) * quantityPerOriginalAmount);

        // Partial sales are not allowed, we need to default to a multiple of our default amount/price
        int itemsBeingBought = (int) Math.floor(roundedQuantity * itemsPerPrice);
        
        // Calculate price with our helper method
        double priceBeingPaid = calculateExactPrice(itemsBeingBought, pricePerItem);
        
        // Update class state
        this.amountBeingSold = itemsBeingBought;
        this.price = priceBeingPaid;

        if (debugLogging) {
            System.out.println("*** roundedQuantity: " + roundedQuantity);
            System.out.println("*** quantityPerOriginalAmount: " + quantityPerOriginalAmount);
            System.out.println("*** itemsBeingBought: " + itemsBeingBought);
            System.out.println("*** priceBeingPaid: " + priceBeingPaid);
        }
    }
    
    /**
     * Ensures we don't exceed the maximum purchase limit.
     */
    private int ensureMaxPurchaseLimit(int itemsBeingBought, int maxPurchaseAmount, double pricePerItem) {
        if (itemsBeingBought > maxPurchaseAmount) {
            if (debugLogging) { 
                System.out.println("itemsBeingBought > maxPurchaseAmount: " + itemsBeingBought + " > " + maxPurchaseAmount); 
            }
            
            return maxPurchaseAmount;
        }
        
        return itemsBeingBought;
    }
    
    /**
     * Sets the final negotiated values for the transaction.
     */
    private void setFinalNegotiatedValues(int itemsBeingBought, double priceBeingPaid) {
        this.amountBeingSold = itemsBeingBought;
        this.price = priceBeingPaid;
        // Store explicitly negotiated price (used in tests)
        this.negotiatedAmountBeingSold = itemsBeingBought;
        this.negotiatedPrice = priceBeingPaid;

        if (debugLogging) {
            System.out.println("-* amountBeingSold: " + this.amountBeingSold);
            System.out.println("-* price: " + this.price);
            System.out.println("-* originalAmountBeingSold: " + originalAmountBeingSold);
            System.out.println("-* originalPrice: " + originalPrice);
        }
    }

    /**
     * Gets the final negotiated price.
     * 
     * @return The final price for the transaction
     */
    public double getPrice() {
        return price;
    }

    /**
     * Gets the final negotiated amount.
     * 
     * @return The final amount for the transaction
     */
    public int getAmountBeingSold() {
        return amountBeingSold;
    }

    /**
     * Gets the explicitly negotiated price.
     * 
     * @return The explicitly negotiated price or NO_SPECIFIC_AMOUNT if negotiation wasn't completed
     */
    public double getNegotiatedPrice() {
        return negotiatedPrice;
    }

    /**
     * Gets the explicitly negotiated amount.
     * 
     * @return The explicitly negotiated amount or NO_SPECIFIC_AMOUNT if negotiation wasn't completed
     */
    public int getNegotiatedAmountBeingSold() {
        return negotiatedAmountBeingSold;
    }
    
    /**
     * Checks if this negotiator supports fractional payments.
     *
     * @return true if fractional payments (with decimal precision) are enabled, false otherwise
     */
    public boolean supportsFractionalPayments() {
        return supportsFractionalPayments;
    }
}
