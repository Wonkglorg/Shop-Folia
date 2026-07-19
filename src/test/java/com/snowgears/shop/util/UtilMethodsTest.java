package com.snowgears.shop.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class UtilMethodsTest {
    @Test
    public void firstLineOverMaxLength() {
        List<String> result = UtilMethods.splitStringIntoLines("&aThe silly fox ran around the house", 15);

        Assertions.assertEquals("§aThe silly fox ran", result.get(0));
        Assertions.assertEquals("§aaround the house", result.get(1));
        Assertions.assertEquals(2, result.size());
    }  
    @Test
    public void testFormatNewColorsEachLine() {
        List<String> result = UtilMethods.splitStringIntoLines("&aHello &bWorld", 8);

        Assertions.assertEquals("§aHello", result.get(0));
        Assertions.assertEquals("§bWorld", result.get(1));
        Assertions.assertEquals(2, result.size());
    }  
    @Test
    public void keepColorsOnSameLineIfShort() {
        // The line is less than 40 characters so we should keep multiple colors on the same line.
        String input = "&eThis is short &band this is too.";
        List<String> result = UtilMethods.splitStringIntoLines(input, 40);
        
        Assertions.assertEquals("§eThis is short §band this is too.", result.get(0));
        Assertions.assertEquals(1, result.size());
    } 
    @Test
    public void trimUselessColors() {
        // If we put in useless color codes, they will be ignored and trimmed out
        String input = "&e&c&dThis is short &b&bbut this section is 34 characters.";
        List<String> result = UtilMethods.splitStringIntoLines(input, 40);
        
        Assertions.assertEquals("§dThis is short", result.get(0));
        Assertions.assertEquals("§bbut this section is 34 characters.", result.get(1));
        Assertions.assertEquals(2, result.size());
    }
    @Test
    public void splitColorLongSection() {
        // The first line is short, but the second line would push us past 40 characters, so we split it to a new line
        String input = "&eThis is short &bbut this section is 34 characters.";
        List<String> result = UtilMethods.splitStringIntoLines(input, 40);
        
        Assertions.assertEquals("§eThis is short", result.get(0));
        Assertions.assertEquals("§bbut this section is 34 characters.", result.get(1));
        Assertions.assertEquals(2, result.size());
    }
    @Test
    public void splitColorLongSection2() {
        // The first line is short, but the second line would push us past 40 characters, so we split it to a new line
        String input = "&eThis is short &bbut this section splits itself since it is well over 40 characters.";
        List<String> result = UtilMethods.splitStringIntoLines(input, 40);
        
        Assertions.assertEquals("§eThis is short", result.get(0));
        Assertions.assertEquals("§bbut this section splits itself since it is", result.get(1));
        Assertions.assertEquals("§bwell over 40 characters.", result.get(2));
        Assertions.assertEquals(3, result.size());
    }
    @Test
    public void testFormatMulticolorLine() {
        List<String> result = UtilMethods.splitStringIntoLines("&aThe &csilly &ffox &eran &4around &athe &bhouse", 15);

        Assertions.assertEquals("§aThe §csilly §ffox", result.get(0));
        Assertions.assertEquals("§eran §4around §athe", result.get(1));
        Assertions.assertEquals("§bhouse", result.get(2));
        Assertions.assertEquals(3, result.size());
    }   
    @Test
    public void splitOnSpaceTest() {
        // When we calculate where to split the string we always split on a space. 
        // Before we split, we check if the next word pushes us past the max length.
        // If it does, we still add the next word, then split the rest to a new line
        String input = "&eHere are some extremely delicately precicely long words";
        List<String> result = UtilMethods.splitStringIntoLines(input, 40);
        
        // It splits after 'precicely' because that word pushes us past the max length.
        Assertions.assertEquals("§eHere are some extremely delicately precicely", result.get(0));
        Assertions.assertEquals(46, result.get(0).length());
        Assertions.assertEquals("§elong words", result.get(1));
        Assertions.assertEquals(12, result.get(1).length());
        Assertions.assertEquals(2, result.size());
    }
    @Test
    public void testFormatMulticolorLine2() {
        List<String> result = UtilMethods.splitStringIntoLines("&aThe &csilly &ffox &eran &4around &athe &bhouse", 25);

        Assertions.assertEquals("§aThe §csilly §ffox §eran §4around", result.get(0));
        Assertions.assertEquals("§athe §bhouse", result.get(1));
        Assertions.assertEquals(2, result.size());
    }   

    @Test
    public void processesHexCodesWithAnds() {
        // There should never be unprocessed color codes sent to us, but just in case we do have some, we should process them
        List<String> result = UtilMethods.splitStringIntoLines("&x&1&2&3&4&5&6The silly fox ran around the house", 15);

        Assertions.assertEquals("§x§1§2§3§4§5§6The silly fox ran", result.get(0));
        Assertions.assertEquals("§x§1§2§3§4§5§6around the house", result.get(1));
        Assertions.assertEquals(2, result.size());
    }

    @Test
    public void testFormatHexSameColorsEachLine() {
        List<String> result = UtilMethods.splitStringIntoLines("§x§1§2§3§4§5§6The silly fox ran around the house", 15);

        Assertions.assertEquals("§x§1§2§3§4§5§6The silly fox ran", result.get(0));
        Assertions.assertEquals("§x§1§2§3§4§5§6around the house", result.get(1));
        Assertions.assertEquals(2, result.size());
    }

    @Test
    public void testFormatHexSameColorsEachLineWithBold() {
        List<String> result = UtilMethods.splitStringIntoLines("&l§x§1§2§3§4§5§6The silly fox ran around the house", 15);

        Assertions.assertEquals("§x§1§2§3§4§5§6§lThe silly fox ran", result.get(0));
        Assertions.assertEquals("§x§1§2§3§4§5§6§laround the house", result.get(1));
        Assertions.assertEquals(2, result.size());
    }

    @Test
    public void swapFromHexToStandardColor() {
        List<String> result = UtilMethods.splitStringIntoLines("&l§x§1§2§3§4§5§6The &csilly &dfox ran around the house", 15);

        Assertions.assertEquals("§x§1§2§3§4§5§6§lThe §c§lsilly", result.get(0));
        Assertions.assertEquals("§d§lfox ran around the", result.get(1));
        Assertions.assertEquals("§d§lhouse", result.get(2));
        Assertions.assertEquals(3, result.size());
    }

    @Test
    public void testLatestColorsBold() {
        List<String> result = UtilMethods.splitStringIntoLines("&l&aHello &bWorld", 8);

        Assertions.assertEquals("§a§lHello", result.get(0));
        Assertions.assertEquals("§b§lWorld", result.get(1));
        Assertions.assertEquals(2, result.size());
    }  

    @Test
    public void testResetBold() {
        List<String> result = UtilMethods.splitStringIntoLines("&l&aHello &ra &aWorld", 8);

        Assertions.assertEquals("§a§lHello §fa", result.get(0));
        Assertions.assertEquals("§aWorld", result.get(1));
        Assertions.assertEquals(2, result.size());
    }  

    
    @Test
    public void testSplitStringWithHexColors() {
        // From log: "§eEnter in chat §x§3§d§0§0§3§dwhat to do with §a§aBirch Log§a(s)§b§b§b §7(§7sell, buy, barter, gamble, combo§7)"
        String input = "&eEnter in chat §x§3§d§0§0§3§dwhat to do with &a&aBirch Log&a(s) &7(&7sell, buy, barter, gamble, combo&7)";
        List<String> result = UtilMethods.splitStringIntoLines(input, 40);
        
        Assertions.assertEquals("§eEnter in chat §x§3§d§0§0§3§dwhat to do with", result.get(0));
        Assertions.assertEquals("§aBirch Log(s)", result.get(1));
        Assertions.assertEquals("§7(sell, buy, barter, gamble, combo)", result.get(2));
        Assertions.assertEquals(3, result.size());
    }
    
    @Test
    public void testSplitStringWithRepeatedFormattingCodes() {
        // From log: "§eEnter in chat the amount of §a§aBirch Log§a(s)§b§b§b §eyou want to sell per transaction."
        String input = "&eEnter in chat the amount of &a&aBirch Log&a(s) &eyou want to sell per transaction.";
        List<String> result = UtilMethods.splitStringIntoLines(input, 40);
        
        Assertions.assertEquals("§eEnter in chat the amount of", result.get(0));
        Assertions.assertEquals("§aBirch Log(s)", result.get(1));
        Assertions.assertEquals("§eyou want to sell per transaction.", result.get(2));
        Assertions.assertEquals(3, result.size());
    }
    
    @Test
    public void testSplitStringWithFormattingAndNumbers() {
        // From log: "§eEnter in chat the price you will sell §a§aBirch Log§a(x§a1§a)§b§b§b §efor."
        String input = "&eEnter in chat the price you will sell &a&aBirch Log&a(x&a1&a) &efor.";
        List<String> result = UtilMethods.splitStringIntoLines(input, 40);
        
        Assertions.assertEquals("§eEnter in chat the price you will sell", result.get(0));
        Assertions.assertEquals("§aBirch Log(x1) §efor.", result.get(1));
        Assertions.assertEquals(2, result.size());
    }
    
    @Test
    public void testSplitStringWithLongNormalColorText() {
        String input = "&cEnter in chat what to do with except have a really long line with overflow &a&aBirch Log&a(s)&b&b&b &7(&7sell, buy, barter, gamble, combo&7)";
        List<String> result = UtilMethods.splitStringIntoLines(input, 40);
        
        Assertions.assertEquals("§cEnter in chat what to do with except have", result.get(0));
        Assertions.assertEquals("§ca really long line with overflow", result.get(1));
        Assertions.assertEquals("§aBirch Log(s)§b", result.get(2));
        Assertions.assertEquals("§7(sell, buy, barter, gamble, combo)", result.get(3));
        Assertions.assertEquals(4, result.size());
    }
    
    @Test
    // There was a bug where the hex color code was not being carried over to the next line
    // This test verifies that we have fixed the bug.
    public void testSplitStringWithLongHexColorText() {
        // From log: "§x§3§d§0§0§3§dEnter in chat what to do with except have a really long line with overflow §a§aBirch Log§a(s)§b§b§b §7(§7sell, buy, barter, gamble, combo§7)"
        String input = "§x§3§d§0§0§3§dEnter in chat what to do with except have a really long line with overflow &a&aBirch Log&a(s)&b&b&b &7(&7sell, buy, barter, gamble, combo&7)";
        List<String> result = UtilMethods.splitStringIntoLines(input, 40);
        
        Assertions.assertEquals("§x§3§d§0§0§3§dEnter in chat what to do with except have", result.get(0));
        Assertions.assertEquals("§x§3§d§0§0§3§da really long line with overflow", result.get(1));
        Assertions.assertEquals("§aBirch Log(s)§b", result.get(2));
        Assertions.assertEquals("§7(sell, buy, barter, gamble, combo)", result.get(3));
        Assertions.assertEquals(4, result.size());
    }

    @Test
    public void testSplitStringWithLongMixedColorText() {
        String input = "&cEnter in chat what to do with except have a really long line with overflow &a&aBirch Log&a(s)&b&b&b &7(&7sell, buy, barter, gamble, combo&7)";
        List<String> result = UtilMethods.splitStringIntoLines(input, 40);
        
        Assertions.assertEquals("§cEnter in chat what to do with except have", result.get(0));
        Assertions.assertEquals("§ca really long line with overflow", result.get(1));
        Assertions.assertEquals("§aBirch Log(s)§b", result.get(2));
        Assertions.assertEquals("§7(sell, buy, barter, gamble, combo)", result.get(3));
        Assertions.assertEquals(4, result.size());
    }

    @Test
    public void testGradientWordIsKeptIntact() {
        // From log: "§eEnter in chat what to do with §a§ct§x§F§E§5§9§3§Fe§x§F§E§7§3§3§Fs§x§F§E§8§D§3§Et§x§F§E§A§7§3§Fi§x§F§E§C§1§3§Fn§x§F§E§D§B§3§Fg §x§F§E§F§5§3§Fa §x§E§C§F§E§3§Fr§x§D§2§F§E§3§Fa§x§B§8§F§E§3§Fi§x§9§E§F§E§3§Fn§x§8§4§F§E§3§Fb§x§6§A§F§E§3§Fo§x§5§0§F§E§3§Fw §x§3§F§F§5§4§7m§x§3§F§D§B§6§1e§x§3§F§C§1§7§Bs§x§3§F§A§7§9§5s§x§3§F§8§D§A§Fa§x§3§F§7§3§C§9g§x§3§F§5§9§E§3e§9!§a(s)"
        String input = "&eEnter in chat what to do with &a&ct§x§F§E§5§9§3§Fe§x§F§E§7§3§3§Fs§x§F§E§8§D§3§Et§x§F§E§A§7§3§Fi§x§F§E§C§1§3§Fn§x§F§E§D§B§3§Fg §x§F§E§F§5§3§Fa §x§E§C§F§E§3§Fr§x§D§2§F§E§3§Fa§x§B§8§F§E§3§Fi§x§9§E§F§E§3§Fn§x§8§4§F§E§3§Fb§x§6§A§F§E§3§Fo§x§5§0§F§E§3§Fw §x§3§F§F§5§4§7m§x§3§F§D§B§6§1e§x§3§F§C§1§7§Bs§x§3§F§A§7§9§5s§x§3§F§8§D§A§Fa§x§3§F§7§3§C§9g§x§3§F§5§9§E§3e§9!&a(s)";
        List<String> result = UtilMethods.splitStringIntoLines(input, 35);
        
        // The first line should break after "what to do with"
        Assertions.assertEquals("§eEnter in chat what to do with", result.get(0));
        
        // The gradient word "testing" should be kept intact in one line, not split across lines
        String secondLine = result.get(1);
        Assertions.assertTrue(secondLine.contains("§ct§x§F§E§5§9§3§Fe§x§F§E§7§3§3§Fs§x§F§E§8§D§3§Et§x§F§E§A§7§3§Fi§x§F§E§C§1§3§Fn§x§F§E§D§B§3§Fg"));
        
        // The gradient word "rainbow" should be kept intact
        Assertions.assertTrue(secondLine.contains("§x§E§C§F§E§3§Fr§x§D§2§F§E§3§Fa§x§B§8§F§E§3§Fi§x§9§E§F§E§3§Fn§x§8§4§F§E§3§Fb§x§6§A§F§E§3§Fo§x§5§0§F§E§3§Fw"));
        
        // The gradient word "message" should be kept intact
        Assertions.assertTrue(secondLine.contains("§x§3§F§F§5§4§7m§x§3§F§D§B§6§1e§x§3§F§C§1§7§Bs§x§3§F§A§7§9§5s§x§3§F§8§D§A§Fa§x§3§F§7§3§C§9g§x§3§F§5§9§E§3e"));
        
    }
    
    @Test
    public void testGradientNonAsciiWordIsKeptIntact() {
        // From log: "§eEnter in chat what to do with §a§x§F§6§C§9§2§8V§x§F§3§C§3§2§5o§x§F§0§B§C§2§3t§x§E§D§B§6§2§0e§x§E§A§B§0§1§EB§x§E§7§A§A§1§Bo§x§E§4§A§3§1§8x §x§E§1§9§D§1§6k§x§D§E§9§7§1§3ľ§x§D§B§9§0§1§1ú§x§D§8§8§A§0§Eč§a(s)"
        String input = "&eEnter in chat what to do with &a§x§F§6§C§9§2§8V§x§F§3§C§3§2§5o§x§F§0§B§C§2§3t§x§E§D§B§6§2§0e§x§E§A§B§0§1§EB§x§E§7§A§A§1§Bo§x§E§4§A§3§1§8x §x§E§1§9§D§1§6k§x§D§E§9§7§1§3ľ§x§D§B§9§0§1§1ú§x§D§8§8§A§0§Eč&a(s)";
        List<String> result = UtilMethods.splitStringIntoLines(input, 35);
        
        // The first line should break after "what to do with"
        Assertions.assertEquals("§eEnter in chat what to do with", result.get(0));
        
        // The gradient word "VoteBox" should be kept intact in one line
        String secondLine = result.get(1);
        Assertions.assertTrue(secondLine.contains("§x§F§6§C§9§2§8V§x§F§3§C§3§2§5o§x§F§0§B§C§2§3t§x§E§D§B§6§2§0e§x§E§A§B§0§1§EB§x§E§7§A§A§1§Bo§x§E§4§A§3§1§8x"));
        
        // The gradient word with non-ASCII chars "klúč" should be kept intact
        Assertions.assertTrue(secondLine.contains("§x§E§1§9§D§1§6k§x§D§E§9§7§1§3ľ§x§D§B§9§0§1§1ú§x§D§8§8§A§0§Eč"));
    }

    @Test
    public void testCompleteGradientMessageSplitting() {
        // Complete test for the first gradient example
        String input = "&eEnter in chat what to do with &a&ct§x§F§E§5§9§3§Fe§x§F§E§7§3§3§Fs§x§F§E§8§D§3§Et§x§F§E§A§7§3§Fi§x§F§E§C§1§3§Fn§x§F§E§D§B§3§Fg §x§F§E§F§5§3§Fa §x§E§C§F§E§3§Fr§x§D§2§F§E§3§Fa§x§B§8§F§E§3§Fi§x§9§E§F§E§3§Fn§x§8§4§F§E§3§Fb§x§6§A§F§E§3§Fo§x§5§0§F§E§3§Fw §x§3§F§F§5§4§7m§x§3§F§D§B§6§1e§x§3§F§C§1§7§Bs§x§3§F§A§7§9§5s§x§3§F§8§D§A§Fa§x§3§F§7§3§C§9g§x§3§F§5§9§E§3e§9!&a(s)&b&b&b &7(&7sell, buy, barter, gamble, combo&7)";
        List<String> result = UtilMethods.splitStringIntoLines(input, 35);
        
        // Verify there are 3 lines as shown in the log
        Assertions.assertEquals(3, result.size());
        
        // Verify the content of each line
        Assertions.assertEquals("§eEnter in chat what to do with", result.get(0));
        
        // The second line contains the complex gradient text and should keep all gradient words intact
        String gradientLine = result.get(1);
        Assertions.assertTrue(gradientLine.contains("§ct§x§F§E§5§9§3§Fe§x§F§E§7§3§3§Fs§x§F§E§8§D§3§Et§x§F§E§A§7§3§Fi§x§F§E§C§1§3§Fn§x§F§E§D§B§3§Fg"));
        Assertions.assertTrue(gradientLine.contains("§x§F§E§F§5§3§Fa"));
        Assertions.assertTrue(gradientLine.contains("§x§E§C§F§E§3§Fr§x§D§2§F§E§3§Fa§x§B§8§F§E§3§Fi§x§9§E§F§E§3§Fn§x§8§4§F§E§3§Fb§x§6§A§F§E§3§Fo§x§5§0§F§E§3§Fw"));
        Assertions.assertTrue(gradientLine.contains("§x§3§F§F§5§4§7m§x§3§F§D§B§6§1e§x§3§F§C§1§7§Bs§x§3§F§A§7§9§5s§x§3§F§8§D§A§Fa§x§3§F§7§3§C§9g§x§3§F§5§9§E§3e"));
        Assertions.assertTrue(gradientLine.contains("§a(s)"));
        
        // The third line should be the options text
        Assertions.assertEquals("§7(sell, buy, barter, gamble, combo)", result.get(2));
    }
    
    @Test
    public void testVoteBoxGradientMessageSplitting() {
        // Complete test for the second gradient example with VoteBox
        String input = "&eEnter in chat what to do with &a§x§F§6§C§9§2§8V§x§F§3§C§3§2§5o§x§F§0§B§C§2§3t§x§E§D§B§6§2§0e§x§E§A§B§0§1§EB§x§E§7§A§A§1§Bo§x§E§4§A§3§1§8x §x§E§1§9§D§1§6k§x§D§E§9§7§1§3ľ§x§D§B§9§0§1§1ú§x§D§8§8§A§0§Eč&a(s)&b&b&b &7(&7sell, buy, barter, gamble, combo&7)";
        List<String> result = UtilMethods.splitStringIntoLines(input, 35);
        
        // Verify there are 3 lines as shown in the log
        Assertions.assertEquals(3, result.size());
        
        // Verify the content of each line
        Assertions.assertEquals("§eEnter in chat what to do with", result.get(0));
        
        // The second line contains the complex gradient text for VoteBox klúč
        String gradientLine = result.get(1);
        Assertions.assertTrue(gradientLine.contains("§x§F§6§C§9§2§8V§x§F§3§C§3§2§5o§x§F§0§B§C§2§3t§x§E§D§B§6§2§0e§x§E§A§B§0§1§EB§x§E§7§A§A§1§Bo§x§E§4§A§3§1§8x"));
        Assertions.assertTrue(gradientLine.contains(" §x§E§1§9§D§1§6k§x§D§E§9§7§1§3ľ§x§D§B§9§0§1§1ú§x§D§8§8§A§0§Eč"));
        Assertions.assertTrue(gradientLine.contains("§a(s)"));
        
        // The third line should be the options text
        Assertions.assertEquals("§7(sell, buy, barter, gamble, combo)", result.get(2));
    }
}

/**


// Verify we keep gradient words in one piece instead of splitting them in the middle of a word
[23:47:05 INFO]: [Shop] [Debug] [ShopMessage] postFormat: §eEnter in chat what to do with §a§ct§x§F§E§5§9§3§Fe§x§F§E§7§3§3§Fs§x§F§E§8§D§3§Et§x§F§E§A§7§3§Fi§x§F§E§C§1§3§Fn§x§F§E§D§B§3§Fg §x§F§E§F§5§3§Fa §x§E§C§F§E§3§Fr§x§D§2§F§E§3§Fa§x§B§8§F§E§3§Fi§x§9§E§F§E§3§Fn§x§8§4§F§E§3§Fb§x§6§A§F§E§3§Fo§x§5§0§F§E§3§Fw §x§3§F§F§5§4§7m§x§3§F§D§B§6§1e§x§3§F§C§1§7§Bs§x§3§F§A§7§9§5s§x§3§F§8§D§A§Fa§x§3§F§7§3§C§9g§x§3§F§5§9§E§3e§9!§a(s)§b§b§b §7(§7sell, buy, barter, gamble, combo§7)
[23:47:05 INFO]: [Shop] [Debug] Spawning hologram for player spaceGurlSky at -2113/68/2026: §eEnter in chat what to do with
[23:47:05 INFO]: [Shop] [Debug] Spawning hologram for player spaceGurlSky at -2113/68/2026: §ct§x§F§E§5§9§3§Fe§x§F§E§7§3§3§Fs§x§F§E§8§D§3§Et§x§F§E§A§7§3§Fi§x§F§E§C§1§3§Fn§x§F§E§D§B§3§Fg §x§F§E§F§5§3§Fa §x§E§C§F§E§3§Fr§x§D§2§F§E§3§Fa§x§B§8§F§E§3§Fi§x§9§E§F§E§3§Fn§x§8§4§F§E§3§Fb§x§6§A§F§E§3§Fo§x§5§0§F§E§3§Fw §x§3§F§F§5§4§7m§x§3§F§D§B§6§1e§x§3§F§C§1§7§Bs§x§3§F§A§7§9§5s§x§3§F§8§D§A§Fa§x§3§F§7§3§C§9g§x§3§F§5§9§E§3e§9!§a(s)§b
[23:47:05 INFO]: [Shop] [Debug] Spawning hologram for player spaceGurlSky at -2113/67/2026: §7(sell, buy, barter, gamble, combo)

// Second example
[23:48:40 INFO]: [Shop] [Debug] [ShopMessage] postFormat: §eEnter in chat what to do with §a§x§F§6§C§9§2§8V§x§F§3§C§3§2§5o§x§F§0§B§C§2§3t§x§E§D§B§6§2§0e§x§E§A§B§0§1§EB§x§E§7§A§A§1§Bo§x§E§4§A§3§1§8x §x§E§1§9§D§1§6k§x§D§E§9§7§1§3ľ§x§D§B§9§0§1§1ú§x§D§8§8§A§0§Eč§a(s)§b§b§b §7(§7sell, buy, barter, gamble, combo§7)
[23:48:40 INFO]: [Shop] [Debug] Spawning hologram for player spaceGurlSky at -2112/68/2026: §eEnter in chat what to do with
[23:48:40 INFO]: [Shop] [Debug] Spawning hologram for player spaceGurlSky at -2112/68/2026: §x§F§6§C§9§2§8V§x§F§3§C§3§2§5o§x§F§0§B§C§2§3t§x§E§D§B§6§2§0e§x§E§A§B§0§1§EB§x§E§7§A§A§1§Bo§x§E§4§A§3§1§8x §x§E§1§9§D§1§6k§x§D§E§9§7§1§3ľ§x§D§B§9§0§1§1ú§x§D§8§8§A§0§Eč§a(s)§b
[23:48:40 INFO]: [Shop] [Debug] Spawning hologram for player spaceGurlSky at -2112/67/2026: §7(sell, buy, barter, gamble, combo)


 */