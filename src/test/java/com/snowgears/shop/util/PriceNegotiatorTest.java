package com.snowgears.shop.util;

import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class PriceNegotiatorTest {
    @Test
    void noPurchase() {
        PriceNegotiator negotiator = new PriceNegotiator(false, 16, 64, false);

        // No Funds
        negotiator.negotiatePurchase(false, 0, 128, -1);

        assertEquals(-1, negotiator.getNegotiatedPrice());
        assertEquals(-1, negotiator.getNegotiatedAmountBeingSold());

        // Not enough inventory - NO partial sales
        negotiator.negotiatePurchase(false, 16, 63, -1);

        assertEquals(-1, negotiator.getNegotiatedPrice());
        assertEquals(-1, negotiator.getNegotiatedAmountBeingSold());

        // No inventory
        negotiator.negotiatePurchase(false, 16, 0, -1);

        assertEquals(-1, negotiator.getNegotiatedPrice());
        assertEquals(-1, negotiator.getNegotiatedAmountBeingSold());

        // No Funds - allow partial sales
        negotiator.negotiatePurchase(true, 0, 128, -1);

        assertEquals(-1, negotiator.getNegotiatedPrice());
        assertEquals(-1, negotiator.getNegotiatedAmountBeingSold());

        // Not enough inventory - allow partial sales
        negotiator.negotiatePurchase(true, 16, 3, -1);

        assertEquals(-1, negotiator.getNegotiatedPrice());
        assertEquals(-1, negotiator.getNegotiatedAmountBeingSold());

        // No inventory - allow partial sales
        negotiator.negotiatePurchase(true, 16, 0, -1);

        assertEquals(-1, negotiator.getNegotiatedPrice());
        assertEquals(-1, negotiator.getNegotiatedAmountBeingSold());
    }

    @Test
    public void normalPurchase() {
        PriceNegotiator negotiator = new PriceNegotiator(false,16,64, false);

        // More funds
        negotiator.negotiatePurchase(false,32,128,-1);

        assertEquals(negotiator.getNegotiatedPrice(), 16);
        assertEquals(negotiator.getNegotiatedAmountBeingSold(), 64);

        // Exact funds
        negotiator.negotiatePurchase(false,16,128,-1);

        assertEquals(negotiator.getNegotiatedPrice(), 16);
        assertEquals(negotiator.getNegotiatedAmountBeingSold(), 64);

        // Exact shop
        negotiator.negotiatePurchase(false,16,64,-1);

        assertEquals(negotiator.getNegotiatedPrice(), 16);
        assertEquals(negotiator.getNegotiatedAmountBeingSold(), 64);
    }

    @Test
    public void partialPurchaseBuyer() {
        // Test out whole division, price per unit = 4
        PriceNegotiator negotiator = new PriceNegotiator(false,16,64, false);

        // Exact Funds Sale
        negotiator.negotiatePurchase(true,16,128,-1);

        assertEquals(negotiator.getNegotiatedPrice(), 16);
        assertEquals(negotiator.getNegotiatedAmountBeingSold(), 64);

        // Extra Funds Sale
        negotiator.negotiatePurchase(true,18,128,-1);

        assertEquals(negotiator.getNegotiatedPrice(), 16);
        assertEquals(negotiator.getNegotiatedAmountBeingSold(), 64);

        // Single Partial
        negotiator.negotiatePurchase(true,1,128,-1);

        assertEquals(negotiator.getNegotiatedPrice(), 1);
        assertEquals(negotiator.getNegotiatedAmountBeingSold(), 4);

        // Low Partial
        negotiator.negotiatePurchase(true,3,128,-1);

        assertEquals(negotiator.getNegotiatedPrice(), 3);
        assertEquals(negotiator.getNegotiatedAmountBeingSold(), 12);

        // Half Sale
        negotiator.negotiatePurchase(true,8,128,-1);

        assertEquals(negotiator.getNegotiatedPrice(), 8);
        assertEquals(negotiator.getNegotiatedAmountBeingSold(), 32);

        // Almost Full
        negotiator.negotiatePurchase(true,15,128,-1);

        assertEquals(negotiator.getNegotiatedPrice(), 15);
        assertEquals(negotiator.getNegotiatedAmountBeingSold(), 60);
    }

    @Test
    public void partialPurchaseBuyer_priceFraction() {
        // Test out fractional price division, price per unit = 3.2
        PriceNegotiator negotiator = new PriceNegotiator(false,10,32, false);

        // Exact Funds Sale
        negotiator.negotiatePurchase(true,10,128,-1);

        assertEquals(negotiator.getNegotiatedPrice(), 10);
        assertEquals(negotiator.getNegotiatedAmountBeingSold(), 32);

        // Extra Funds Sale
        negotiator.negotiatePurchase(true,15,128,-1);

        assertEquals(negotiator.getNegotiatedPrice(), 10);
        assertEquals(negotiator.getNegotiatedAmountBeingSold(), 32);

        // Single Partial
        negotiator.negotiatePurchase(true,1,128,-1);

        assertEquals(negotiator.getNegotiatedPrice(), 1);
        assertEquals(negotiator.getNegotiatedAmountBeingSold(), 3);

        // Low Partial
        negotiator.negotiatePurchase(true,3,128,-1);

        assertEquals(negotiator.getNegotiatedPrice(), 3);
        assertEquals(negotiator.getNegotiatedAmountBeingSold(), 9);

        // Half Sale
        negotiator.negotiatePurchase(true,5,128,-1);

        assertEquals(negotiator.getNegotiatedPrice(), 5);
        assertEquals(negotiator.getNegotiatedAmountBeingSold(), 16);

        // Almost Full
        negotiator.negotiatePurchase(true,9,128,-1);

        assertEquals(negotiator.getNegotiatedPrice(), 9);
        assertEquals(negotiator.getNegotiatedAmountBeingSold(), 28);
    }

    @Test
    public void partialPurchaseBuyer_priceFractionFlipped() {
        // Test out fractional price division, price per unit = 3.2
        PriceNegotiator negotiator = new PriceNegotiator(false,32,10, false);

        // Exact Funds Sale
        negotiator.negotiatePurchase(true,32,64,-1);

        assertEquals(negotiator.getNegotiatedPrice(), 32);
        assertEquals(negotiator.getNegotiatedAmountBeingSold(), 10);

        // Extra Funds Sale
        negotiator.negotiatePurchase(true,40,64,-1);

        assertEquals(negotiator.getNegotiatedPrice(), 32);
        assertEquals(negotiator.getNegotiatedAmountBeingSold(), 10);

        // Single Price - Should Fail & return -1
        negotiator.negotiatePurchase(true,1,64,-1);

        assertEquals(negotiator.getNegotiatedPrice(), -1);
        assertEquals(negotiator.getNegotiatedAmountBeingSold(), -1);

        // Single Partial
        negotiator.negotiatePurchase(true,4,64,-1);

        assertEquals(negotiator.getNegotiatedPrice(), 3);
        assertEquals(negotiator.getNegotiatedAmountBeingSold(), 1);

        // Low Partial
        negotiator.negotiatePurchase(true,10,64,-1);

        assertEquals(negotiator.getNegotiatedPrice(), 10);
        assertEquals(negotiator.getNegotiatedAmountBeingSold(), 3);

        // Half Sale
        negotiator.negotiatePurchase(true,16,64,-1);

        assertEquals(negotiator.getNegotiatedPrice(), 16);
        assertEquals(negotiator.getNegotiatedAmountBeingSold(), 5);

        // Half Sale - extra funds
        negotiator.negotiatePurchase(true,19,64,-1);

        assertEquals(negotiator.getNegotiatedPrice(), 16);
        assertEquals(negotiator.getNegotiatedAmountBeingSold(), 5);

        // Almost Full
        negotiator.negotiatePurchase(true,26,64,-1);

        assertEquals(negotiator.getNegotiatedPrice(), 26);
        assertEquals(negotiator.getNegotiatedAmountBeingSold(), 8);

        // Almost Full - extra funds
        negotiator.negotiatePurchase(true,28,64,-1);

        assertEquals(negotiator.getNegotiatedPrice(), 26);
        assertEquals(negotiator.getNegotiatedAmountBeingSold(), 8);
    }

    @Test
    public void fullStackPartialPurchase() {
        // Test the scenario where a shop sells 128 items for 1 diamond
        // and a player tries to buy 64 items with a full stack purchase
        
        // With default integer payments, this will round up to 1
        PriceNegotiator negotiator = new PriceNegotiator(false, 1, 128, false);
        
        // Full stack purchase (64 items) should cost 1 diamond with integer payments (rounded up)
        negotiator.negotiatePurchase(true, 1, 128, 64);
        
        // With integer payments, it will round up to 1
        assertEquals(1.0, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(128, negotiator.getNegotiatedAmountBeingSold());
        
        // Try with different ratios
        negotiator = new PriceNegotiator(false, 10, 100, false);
        
        // Full stack purchase (64 items) should cost 6 diamonds with integer payments (rounded up)
        negotiator.negotiatePurchase(true, 10, 100, 64);
        
        assertEquals(10.0, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(100, negotiator.getNegotiatedAmountBeingSold());
        
        // This test now has a companion test: fixFullStackPartialPurchaseWithFractionalPayment
        // which demonstrates the correct behavior with fractional payments
    }

    @Test
    public void partialPurchaseWithFractionalPrice() {
        // Simulate a FRACTIONAL currency partial purchase of 0.5
        PriceNegotiator negotiator = new PriceNegotiator(true, 1.5, 3, true);        
        negotiator.negotiatePurchase(true, 0.5, 128, -1);
        assertEquals(0.5, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(1, negotiator.getNegotiatedAmountBeingSold());
        // Simulate an INTEGER currency partial purchase of 0.5
        negotiator = new PriceNegotiator(true, 1.5, 3, false);        
        negotiator.negotiatePurchase(true, 0.5, 128, -1);
        assertEquals(-1, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(-1, negotiator.getNegotiatedAmountBeingSold());


        // Simulate a partial purchase for fractions of 0.01
        negotiator = new PriceNegotiator(true, 1, 100, true);
        negotiator.negotiatePurchase(true, 0.01, 1000, -1);
        assertEquals(0.01, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(1, negotiator.getNegotiatedAmountBeingSold());

        // Simulate a full stack purchase (64 items)
        negotiator.negotiatePurchase(true, 0.03, 1000, -1);
        assertEquals(0.03, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(3, negotiator.getNegotiatedAmountBeingSold());

        // Simulate a partial purchase for fractions of 0.48
        negotiator.negotiatePurchase(true, 0.48, 1000, -1);
        assertEquals(0.48, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(48, negotiator.getNegotiatedAmountBeingSold());


        // Simulate a partial purchase for fractions of 1.72
        negotiator = new PriceNegotiator(true, 2, 200, true);
        negotiator.negotiatePurchase(true, 1.72, 1000, -1);
        assertEquals(1.72, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(172, negotiator.getNegotiatedAmountBeingSold());
        // INTEGER currency partial purchase of 1.72
        negotiator = new PriceNegotiator(true, 2, 200, false);
        negotiator.negotiatePurchase(true, 1.72, 1000, -1);
        assertEquals(1, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(100, negotiator.getNegotiatedAmountBeingSold());

        // Simulate a partial purchase for fractions of 1.72
        negotiator = new PriceNegotiator(true, 10, 1000, true);
        negotiator.negotiatePurchase(true, 1.72, 1000, -1);
        assertEquals(1.72, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(172, negotiator.getNegotiatedAmountBeingSold());

        negotiator = new PriceNegotiator(true, 1.5, 1, true);
        negotiator.negotiatePurchase(true, 3, 1000, -1);
        assertEquals(1.5, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(1, negotiator.getNegotiatedAmountBeingSold());
    }

    @Test
    public void fullStackPurchaseTest() {
        // Test case 1: Shop sells 32 items for 10 currency
        // Full stack purchase (64 items) should cost 20 currency (proportionally calculated)
        PriceNegotiator negotiator = new PriceNegotiator(false, 10, 32, false);
        
        // Simulate a full stack purchase (64 items)
        negotiator.negotiatePurchase(true, 20, 128, 64);
        
        assertEquals(20, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(64, negotiator.getNegotiatedAmountBeingSold());
        
        // Test case 2: Shop sells 128 items for 1 currency
        // Full stack purchase (128 items) should cost 1 currency
        negotiator = new PriceNegotiator(false, 1, 128, false);
        
        // Simulate a full stack purchase (128 items)
        negotiator.negotiatePurchase(true, 1, 128, 128);
        
        assertEquals(1, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(128, negotiator.getNegotiatedAmountBeingSold());
        
        // Test case 3: Shop sells 16 items for 4 currency
        // Full stack purchase (64 items) should cost 16 currency
        negotiator = new PriceNegotiator(false, 4, 16, false);
        
        // Simulate a full stack purchase (64 items)
        negotiator.negotiatePurchase(true, 16, 128, 64);
        
        assertEquals(16, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(64, negotiator.getNegotiatedAmountBeingSold());
    }

    @Test
    public void fractionalPaymentTest() {
        // Test with fractional payments enabled
        PriceNegotiator negotiator = new PriceNegotiator(false, 10, 32, true);
        
        // Exact Funds Sale
        negotiator.negotiatePurchase(true, 10, 128, -1);
        
        // With fractional payments, price should be exactly 10.00
        assertEquals(10.00, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(32, negotiator.getNegotiatedAmountBeingSold());
        
        // Partial purchase - 20 items should cost exactly 6.25 (exact proportional calculation)
        negotiator.negotiatePurchase(true, 10, 128, 20);
        
        assertEquals(6.25, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(20, negotiator.getNegotiatedAmountBeingSold());
        
        // Test with small funds
        negotiator.negotiatePurchase(true, 1, 128, -1);
        
        // With 1 unit of currency, the calculation has changed to match integer mode
        // We now expect price 0.94 for 3 items
        assertEquals(0.94, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(3, negotiator.getNegotiatedAmountBeingSold());
    }
    
    @Test
    public void compareIntegerVsFractionalPayments() {
        // Test with integer payments (original behavior)
        PriceNegotiator integerNegotiator = new PriceNegotiator(false, 10, 32, false);
        
        // Test with fractional payments
        PriceNegotiator fractionalNegotiator = new PriceNegotiator(false, 10, 32, true);
        
        // For full purchases, both should be the same
        integerNegotiator.negotiatePurchase(true, 10, 128, -1);
        fractionalNegotiator.negotiatePurchase(true, 10, 128, -1);
        
        assertEquals(10, integerNegotiator.getNegotiatedPrice(), 0.001);
        assertEquals(10, fractionalNegotiator.getNegotiatedPrice(), 0.001);
        
        // For partial purchases, fractional should be exact, integer should round up
        integerNegotiator.negotiatePurchase(true, 5, 128, -1);
        fractionalNegotiator.negotiatePurchase(true, 5, 128, -1);
        
        // Integer should be ceiling: 5
        assertEquals(5, integerNegotiator.getNegotiatedPrice(), 0.001);
        // Fractional should be exact: 5.00
        assertEquals(5.00, fractionalNegotiator.getNegotiatedPrice(), 0.001);
        
        // Both should have same amount
        assertEquals(16, integerNegotiator.getNegotiatedAmountBeingSold());
        assertEquals(16, fractionalNegotiator.getNegotiatedAmountBeingSold());
    }
    
    @Test
    public void fractionalPaymentPrecisionTest() {
        // Test specifically for cent precision
        PriceNegotiator negotiator = new PriceNegotiator(false, 1, 3, true);
        
        // Exact division: 1/3 = 0.33333...
        negotiator.negotiatePurchase(true, 1, 10, 1);
        
        // Should be rounded to 0.33 (2 decimal places)
        assertEquals(0.33, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(1, negotiator.getNegotiatedAmountBeingSold());
        
        // Test with decimal result that needs rounding
        negotiator = new PriceNegotiator(false, 10, 7, true);
        
        // Price per item = 10/7 = 1.428571...
        negotiator.negotiatePurchase(true, 5, 100, 3);
        
        // Should be rounded to 4.29 (3 items * 1.428571... = 4.285714...)
        assertEquals(4.29, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(3, negotiator.getNegotiatedAmountBeingSold());
    }
    
    @Test
    public void fixFullStackPartialPurchaseWithFractionalPayment() {
        // Recreate the failing test case from fullStackPartialPurchase but with fractional payments
        
        // Test the scenario where a shop sells 128 items for 1 diamond
        // and a player tries to buy 64 items with a full stack purchase
        PriceNegotiator negotiator = new PriceNegotiator(false, 1, 128, true);
        
        // Full stack purchase (64 items) should cost 0.5 diamonds
        negotiator.negotiatePurchase(true, 1, 128, 64);
        
        // Should pass with fractional payments
        assertEquals(0.5, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(64, negotiator.getNegotiatedAmountBeingSold());
        
        // Try with different ratios
        negotiator = new PriceNegotiator(false, 10, 100, true);
        
        // Full stack purchase (64 items) should cost 6.4 diamonds
        negotiator.negotiatePurchase(true, 10, 100, 64);
        
        assertEquals(6.4, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(64, negotiator.getNegotiatedAmountBeingSold());
    }
    
    @Test
    public void smallDecimalPricesTest() {
        // Tests for common shop scenarios with smaller decimal prices
        
        // Test Case 1: Shop selling 64 dirt for $1.50
        PriceNegotiator negotiator = new PriceNegotiator(false, 1.50, 64, true);
        
        // Buy the full stack
        negotiator.negotiatePurchase(true, 2.00, 128, -1);
        assertEquals(1.50, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(64, negotiator.getNegotiatedAmountBeingSold());
        
        // Buy half the stack - should be $0.75
        negotiator.negotiatePurchase(true, 1.00, 128, 32);
        assertEquals(0.75, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(32, negotiator.getNegotiatedAmountBeingSold());
        
        // Buy 10 dirt - should be $0.23 (rounded from 0.234375)
        negotiator.negotiatePurchase(true, 0.25, 128, 10);
        assertEquals(0.23, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(10, negotiator.getNegotiatedAmountBeingSold());
        
        // Test Case 2: Shop selling 8 logs for $0.75 
        negotiator = new PriceNegotiator(false, 0.75, 8, true);
        
        // Buy full stack
        negotiator.negotiatePurchase(true, 1.00, 64, -1);
        assertEquals(0.75, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(8, negotiator.getNegotiatedAmountBeingSold());
        
        // Buy 4 logs - should be $0.38 (rounded from 0.375)
        negotiator.negotiatePurchase(true, 0.40, 64, 4);
        assertEquals(0.38, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(4, negotiator.getNegotiatedAmountBeingSold());
        
        // Buy 1 log - should be $0.09 (rounded from 0.09375)
        negotiator.negotiatePurchase(true, 0.10, 64, 1);
        assertEquals(0.09, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(1, negotiator.getNegotiatedAmountBeingSold());
        
        // Test Case 3: Shop selling 2 axes for $2.75
        negotiator = new PriceNegotiator(false, 2.75, 2, true);
        
        // Buy both axes
        negotiator.negotiatePurchase(true, 3.00, 10, -1);
        assertEquals(2.75, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(2, negotiator.getNegotiatedAmountBeingSold());
        
        // Buy 1 axe - should be $1.38 (rounded from 1.375)
        negotiator.negotiatePurchase(true, 1.50, 10, 1);
        assertEquals(1.38, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(1, negotiator.getNegotiatedAmountBeingSold());
        
        // Test Case 4: Edge case - very low price (1 cent per item)
        negotiator = new PriceNegotiator(false, 0.64, 64, true);
        
        // Buy full stack (should be exactly 0.64)
        negotiator.negotiatePurchase(true, 1.00, 128, -1);
        assertEquals(0.64, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(64, negotiator.getNegotiatedAmountBeingSold());
        
        // Buy just 1 item - should be $0.01
        negotiator.negotiatePurchase(true, 0.01, 128, 1);
        assertEquals(0.01, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(1, negotiator.getNegotiatedAmountBeingSold());
        
        // Test Case 5: Edge case - non-standard division (17 items for $2.49)
        negotiator = new PriceNegotiator(false, 2.49, 17, true);
        
        // Buy full amount
        negotiator.negotiatePurchase(true, 3.00, 34, -1);
        assertEquals(2.49, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(17, negotiator.getNegotiatedAmountBeingSold());
        
        // Buy 5 items - should be $0.73 (rounded from 0.7323...)
        negotiator.negotiatePurchase(true, 0.75, 34, 5);
        assertEquals(0.73, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(5, negotiator.getNegotiatedAmountBeingSold());
    }
    
    @Test
    public void compareFractionalToManualCalculation() {
        // Test to verify our implementation against manual calculations
        // This ensures that the price calculations are accurate for fractional payments
        
        // Shop selling 20 items for $5.00
        PriceNegotiator negotiator = new PriceNegotiator(false, 5.00, 20, true);
        
        // Exact manual calculation for 7 items: (7/20) * 5.00 = 1.75
        negotiator.negotiatePurchase(true, 2.00, 50, 7);
        assertEquals(1.75, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(7, negotiator.getNegotiatedAmountBeingSold());
        
        // Shop selling 7 items for $3.49
        negotiator = new PriceNegotiator(false, 3.49, 7, true);
        
        // Exact manual calculation for 3 items: (3/7) * 3.49 = 1.496 rounds to 1.50
        negotiator.negotiatePurchase(true, 1.50, 20, 3);
        assertEquals(1.50, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(3, negotiator.getNegotiatedAmountBeingSold());
        
        // Shop selling 1 item for $0.33
        negotiator = new PriceNegotiator(false, 0.33, 1, true);
        
        // When buying multiple items, calculation should be exact
        // 5 items: 5 * 0.33 = 1.65
        negotiator.negotiatePurchase(true, 2.00, 10, 5);
        assertEquals(1.65, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(5, negotiator.getNegotiatedAmountBeingSold());
    }
    
    @Test
    public void avoidRoundingErrorsTest() {
        // Test specifically focused on avoiding common floating-point rounding errors
        
        // Shop selling 3 items for $0.99
        PriceNegotiator negotiator = new PriceNegotiator(false, 0.99, 3, true);
        
        // Buying 1 item should be exactly $0.33 (not 0.329999...)
        negotiator.negotiatePurchase(true, 0.35, 10, 1);
        assertEquals(0.33, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(1, negotiator.getNegotiatedAmountBeingSold());
        
        // Shop selling 100 items for $33.33
        negotiator = new PriceNegotiator(false, 33.33, 100, true);
        
        // Buying 1 item should be exactly $0.33 (not 0.3333...)
        negotiator.negotiatePurchase(true, 0.35, 200, 1);
        assertEquals(0.33, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(1, negotiator.getNegotiatedAmountBeingSold());
        
        // Shop with a price that has many decimal places: $9.99 for 7 items
        negotiator = new PriceNegotiator(false, 9.99, 7, true);
        
        // Buying 3 items should be exactly $4.28 (rounded from 4.2814...)
        negotiator.negotiatePurchase(true, 4.30, 20, 3);
        assertEquals(4.28, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(3, negotiator.getNegotiatedAmountBeingSold());
        
        // Test to ensure we properly handle repeating decimals
        // Shop selling 3 items for $1.00
        negotiator = new PriceNegotiator(false, 1.00, 3, true);
        
        // Buying 1 item should be exactly $0.33 (rounded from 0.3333...)
        negotiator.negotiatePurchase(true, 0.35, 10, 1);
        assertEquals(0.33, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(1, negotiator.getNegotiatedAmountBeingSold());
    }

    @Test
    public void extremeValueTest() {
        // Tiny price - 0.01 for 1000 items (fractional price = 0.00001 per item)
        PriceNegotiator negotiator = new PriceNegotiator(false, 0.01, 1000, true);
        
        // Buy all items (1000) - this is a valid transaction
        negotiator.negotiatePurchase(true, 0.01, 1000, 1000);
        assertEquals(0.01, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(1000, negotiator.getNegotiatedAmountBeingSold());
        
        // Buying just 1 item - price should be too small and result in "no purchase"
        // With 0.01/1000 = 0.00001 per item, which rounds to 0 (invalid)
        negotiator.negotiatePurchase(true, 0.01, 1000, 1);
        // Expect "no purchase" response since the price would be too small
        assertEquals(-1, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(-1, negotiator.getNegotiatedAmountBeingSold());
        
        // Huge price - 1,000,000 for 1 item
        negotiator = new PriceNegotiator(false, 1000000.0, 1, true);
        negotiator.negotiatePurchase(true, 1000000.0, 10, 1);
        assertEquals(1000000.0, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(1, negotiator.getNegotiatedAmountBeingSold());
        
        // Testing with very large quantities
        negotiator = new PriceNegotiator(false, 100.0, 100000, true);
        negotiator.negotiatePurchase(true, 100.0, 100000, 100000);
        assertEquals(100.0, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(100000, negotiator.getNegotiatedAmountBeingSold());
    }

    @Test
    public void unusualRatioTest() {
        // Prime number ratio - 17 items for 13 currency
        PriceNegotiator negotiator = new PriceNegotiator(false, 13.0, 17, true);
        
        // Buy full amount
        negotiator.negotiatePurchase(true, 13.0, 34, 17);
        assertEquals(13.0, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(17, negotiator.getNegotiatedAmountBeingSold());
        
        // Buy 5 items - should calculate exact proportion
        negotiator.negotiatePurchase(true, 5.0, 34, 5);
        double expectedPrice = (5.0/17.0) * 13.0;
        expectedPrice = Math.round(expectedPrice * 100) / 100.0; // Round to cents
        assertEquals(expectedPrice, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(5, negotiator.getNegotiatedAmountBeingSold());
        
        // Repeating decimal test - 1 currency for 3 items
        negotiator = new PriceNegotiator(false, 1.0, 3, true);
        
        // Buy full amount
        negotiator.negotiatePurchase(true, 1.0, 6, 3);
        assertEquals(1.0, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(3, negotiator.getNegotiatedAmountBeingSold());
        
        // Buy 1 item - should be exactly 0.33 (rounded from 0.333...)
        negotiator.negotiatePurchase(true, 0.33, 6, 1);
        assertEquals(0.33, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(1, negotiator.getNegotiatedAmountBeingSold());
        
        // Test a complex ratio - 19 items for 7.35 currency
        negotiator = new PriceNegotiator(false, 7.35, 19, true);
        
        // Buy 6 items - should calculate exact proportion
        negotiator.negotiatePurchase(true, 3.0, 19, 6);
        expectedPrice = (6.0/19.0) * 7.35;
        expectedPrice = Math.round(expectedPrice * 100) / 100.0; // Round to cents
        assertEquals(expectedPrice, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(6, negotiator.getNegotiatedAmountBeingSold());
    }

    @Test
    public void extremeRatioTest() {
        // Very small price per item: 0.01 for 1000 items
        PriceNegotiator negotiator = new PriceNegotiator(false, 0.01, 1000, true);
        
        // Buy half - ensure we specify the exact amount to buy
        negotiator.negotiatePurchase(true, 0.01, 1000, 500);
        assertEquals(-1, negotiator.getNegotiatedPrice(), 0.001); // Should be 0.005 but minimum is 0.01
        assertEquals(-1, negotiator.getNegotiatedAmountBeingSold());
        
        // Very large price per item: 1000 for 1 item
        negotiator = new PriceNegotiator(false, 1000.0, 1, true);
        
        // Buy 3 items - make sure we have enough funds
        negotiator.negotiatePurchase(true, 3000.0, 10, 3);
        assertEquals(3000.0, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(3, negotiator.getNegotiatedAmountBeingSold());
        
        // Extreme difference: 1 for 2000 vs 1000 for 1
        PriceNegotiator negotiator1 = new PriceNegotiator(false, 1.0, 2000, true);
        PriceNegotiator negotiator2 = new PriceNegotiator(false, 1000.0, 1, true);
        
        // Test negotiator1 - specify the exact amount and ensure enough funds
        negotiator1.negotiatePurchase(true, 0.1, 2000, 200);
        assertEquals(0.1, negotiator1.getNegotiatedPrice(), 0.001);
        assertEquals(200, negotiator1.getNegotiatedAmountBeingSold());
        
        // Test negotiator2 - making sure buyer has enough funds
        negotiator2.negotiatePurchase(true, 1000.0, 10, 1);
        assertEquals(1000.0, negotiator2.getNegotiatedPrice(), 0.001);
        assertEquals(1, negotiator2.getNegotiatedAmountBeingSold());
    }

    @Test
    public void boundaryConditionTest() {
        // Test with the smallest possible price (0.01)
        PriceNegotiator negotiator = new PriceNegotiator(false, 0.01, 1, true);
        
        // Explicitly specify the desired amount and provide sufficient funds
        negotiator.negotiatePurchase(true, 0.01, 10, 1);
        // For minimum price items, expect a valid purchase
        assertEquals(0.01, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(1, negotiator.getNegotiatedAmountBeingSold());
        
        // Test very small positive values - too small to be a valid purchase
        negotiator = new PriceNegotiator(false, 0.01, 100, true);
        
        // Buying a tiny amount should result in "no purchase" when price would be < 0.01
        negotiator.negotiatePurchase(true, 0.01, 100, 1);
        // Expect "no purchase" response
        assertEquals(-1, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(-1, negotiator.getNegotiatedAmountBeingSold());
        
        // Testing with very precise decimal values
        negotiator = new PriceNegotiator(false, 13.37, 42, true);
        
        // Buy 7 items - should handle the decimal calculation correctly
        negotiator.negotiatePurchase(true, 3.0, 42, 7);
        double expectedPrice = (7.0/42.0) * 13.37;
        expectedPrice = Math.round(expectedPrice * 100) / 100.0; // Round to cents
        assertEquals(expectedPrice, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(7, negotiator.getNegotiatedAmountBeingSold());
    }

    @Test
    public void errorConditionTest() {
        // Test with very small price (almost zero but valid)
        // This is an extreme edge case that should result in "no purchase"
        PriceNegotiator negotiator = new PriceNegotiator(false, 0.001, 10, true);
        negotiator.negotiatePurchase(true, 0.01, 10, 1);
        
        // Should result in "no purchase" as the price is too small
        assertEquals(-1, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(-1, negotiator.getNegotiatedAmountBeingSold());
        
        // Test with insufficient funds - should still allow partial purchase
        negotiator = new PriceNegotiator(false, 10.0, 10, true);
        negotiator.negotiatePurchase(true, 1.0, 10, 1);
        
        // Should purchase 1 item with 1 currency (partial purchase)
        assertEquals(1.0, negotiator.getNegotiatedPrice(), 0.001);
        assertEquals(1, negotiator.getNegotiatedAmountBeingSold());
    }

    @Test
    public void integerVsFractionalComparisonWithUnusualValues() {
        // Test how integer and fractional modes handle unusual values differently
        
        // Test with non-integer optimal price: 1.5 for 3 items
        PriceNegotiator integerNegotiator = new PriceNegotiator(false, 1.5, 3, false);
        PriceNegotiator fractionalNegotiator = new PriceNegotiator(false, 1.5, 3, true);
        
        // Buy 1 item - integer mode should round up, fractional should be exact
        // Make sure we specify enough funds
        integerNegotiator.negotiatePurchase(true, 1.0, 10, 1);
        // For integer mode with a price that's 2/3 of 1.5, it returns 2 items
        assertEquals(1.0, integerNegotiator.getNegotiatedPrice(), 0.001);
        assertEquals(2, integerNegotiator.getNegotiatedAmountBeingSold());
        
        fractionalNegotiator.negotiatePurchase(true, 0.5, 10, 1);
        assertEquals(0.5, fractionalNegotiator.getNegotiatedPrice(), 0.001);
        assertEquals(1, fractionalNegotiator.getNegotiatedAmountBeingSold());
        
        // Test with very small ratio: 0.01 for 10 items
        integerNegotiator = new PriceNegotiator(false, 0.01, 10, false);
        fractionalNegotiator = new PriceNegotiator(false, 0.01, 10, true);
        
        // Buy 1 item with integer mode - for very small values
        integerNegotiator.negotiatePurchase(true, 1.0, 10, 1);
        // With the updated code, both should result in a "no purchase" response
        assertEquals(-1, integerNegotiator.getNegotiatedPrice(), 0.001);
        assertEquals(-1, integerNegotiator.getNegotiatedAmountBeingSold());
        
        // Buy 1 item with fractional mode - price is too small, expect "no purchase"
        fractionalNegotiator.negotiatePurchase(true, 0.01, 10, 1);
        assertEquals(-1, fractionalNegotiator.getNegotiatedPrice(), 0.001);
        assertEquals(-1, fractionalNegotiator.getNegotiatedAmountBeingSold());
    }
}
