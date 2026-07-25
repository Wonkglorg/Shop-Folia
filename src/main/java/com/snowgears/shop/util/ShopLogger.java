/**
 * =============================================================================
 * ShopLogger Class
 * =============================================================================
 *
 * The `ShopLogger` class extends the standard {@link Logger} provided by Bukkit,
 * offering a customized logging system tailored specifically for Shop
 * This enhanced logger introduces additional log levels to provide
 * server operators and developers with granular control over the verbosity and
 * detail of log outputs, facilitating effective monitoring and debugging.
 *
 * ## Logging Levels
 *
 * The logger defines the following custom log levels, in addition to the standard
 * Java logging levels (`SEVERE`, `WARNING`, `INFO`), ordered from highest to
 * lowest severity:
 *
 * 1. **SEVERE**
 * - **Purpose**: Captures critical errors that prevent the plugin from
 * functioning correctly.
 * - **Use Cases**:
 * - Plugin failed to initialize due to missing dependencies.
 * - Critical data corruption or inability to access essential resources.
 *
 * 2. **WARNING**
 * - **Purpose**: Highlights potential issues that may not immediately affect
 * functionality but could lead to problems if unaddressed.
 * - **Use Cases**:
 * - Attempt to create a shop with invalid parameters.
 * - Deprecated API usage or conflicts with other plugins.
 *
 * 3. **INFO**
 * - **Purpose**: Provides general informational messages about the plugin’s
 * operations, typically logged during startup and shutdown.
 * - **Use Cases**:
 * - *"ChestShop plugin enabled successfully."*
 * - *"Loaded 25 shops from the database."*
 *
 * 4. **NOTICE**
 * - **Purpose**: Records significant but non-critical events that operators
 * might want to monitor.
 * - **Use Cases**:
 * - A shop has been successfully created or deleted.
 * - Major configuration changes applied.
 *
 * 5. **HELPFUL**
 * - **Purpose**: Logs important in-game events related to shop operations,
 * aiding in tracking player interactions and transactions.
 * - **Use Cases**:
 * - *"Transaction: Player A sold 5 diamonds to Player B for 100 coins."*
 * - *"Player 'Steve' created a new shop at location (100, 64, 200)."*
 *
 * 6. **DEBUG**
 * - **Purpose**: Provides detailed information useful for debugging specific
 * components or functionalities within the plugin.
 * - **Use Cases**:
 * - *"Processing transaction: Player A buys 10 apples from Player B."*
 * - *"Loading shop data for player 'Alex'."*
 *
 * 7. **TRACE**
 * - **Purpose**: Offers an intermediary level of verbosity between `DEBUG` and
 * `SPAM`, capturing step-by-step execution details without flooding the logs.
 * - **Use Cases**:
 * - *"Entering method `createShop` with parameters: player=Steve, location=(100,64,200)."*
 * - *"Validating shop parameters for player Alex."*
 *
 * 8. **SPAM**
 * - **Purpose**: The most verbose log level, intended for deep diagnostics
 * and capturing every minor operation or event.
 * - **Use Cases**:
 * - *"Player 'Steve' accessed shop inventory at (100, 64, 200)."*
 * - *"Detailed player command processing logs."*
 *
 * ## Configuration
 *
 * Server operators can configure the desired log level via the `config.yml` file:
 *
 * ```yaml
 * logging:
 * level: INFO  # Options: SEVERE, WARNING, INFO, NOTICE, HELPFUL, DEBUG, TRACE, SPAM
 * ```
 *
 * Setting a higher log level (e.g., `INFO`) ensures that only the most critical
 * items are logged, while lower levels (e.g., `SPAM`) provide exhaustive
 * details for in-depth troubleshooting.
 *
 * ## Best Practices
 *
 * - **Consistent Usage**: Maintain consistency in log level assignments across
 * the plugin to facilitate easier log management and interpretation.
 */
package com.snowgears.shop.util;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ShopLogger extends Logger{
	
	Plugin plugin;
	
	// Custom log levels - Note: must be greater than INFO (800) to show up!
	public static final Level NOTICE = new Level("NOTICE", 700){};
	public static final Level HELPFUL = new Level("HELPFUL", 600){};
	public static final Level DEBUG = new Level("DEBUG", 500){};
	public static final Level TRACE = new Level("TRACE", 400){};
	public static final Level SPAM = new Level("SPAM", 300){};
	public static final Level HYPER = new Level("HYPER", 100){};
	
	public ShopLogger(@NotNull Plugin plugin) {
		super(plugin.getPluginMeta().getName(), null);
		this.plugin = plugin;
		
		setParent(plugin.getLogger());
		setLevel(Level.ALL);
	}
	
	public boolean isLevelEnabled(Level level) {
		return this.getLogLevel().intValue() <= level.intValue();
	}
	
	@Override
	public void log(@NotNull Level level, @NotNull String message) {
		if(getLogLevel().intValue() > level.intValue()){
			return;
		}
		
		if(level == Level.SEVERE){
			super.log(Level.SEVERE, message);
		} else if(level == Level.WARNING){
			super.log(Level.WARNING, message);
		} else if(level == Level.INFO){
			super.log(Level.INFO, message);
		} else if(level == NOTICE){
			super.log(Level.INFO, "[Notice] {}", message);
		} else if(level == HELPFUL){
			super.log(Level.INFO, "[Helpful] {}", message);
		} else if(level == DEBUG){
			super.log(Level.INFO, "[Debug] {}", message);
		} else if(level == TRACE){
			super.log(Level.INFO, "[Trace] {}", message);
		} else if(level == SPAM){
			super.log(Level.INFO, "[Spam] {}", message);
		} else if(level == HYPER){
			super.log(Level.INFO, "[Hyper] {}", message);
		} else {
			super.log(level, message);
		}
	}
	
	public void logFilterLevel(Level level, String message) {
		if(this.getLogLevel().intValue() > level.intValue()){
			return;
		}
		super.log(Level.INFO, message);
	}
	
	// Additional Log Levels
	public void notice(String message) {logFilterLevel(NOTICE, "[Notice] " + message);}
	
	public void helpful(String message) {logFilterLevel(HELPFUL, "[Helpful] " + message);}
	
	public void debug(String message) {logFilterLevel(DEBUG, "[Debug] " + message);}
	
	public void debug(String message, Throwable t) {
		debug(message + " " + getStackTrace(t));
	}
	
	public void trace(String message) {logFilterLevel(TRACE, "[Trace] " + message);}
	
	public void spam(String message) {logFilterLevel(SPAM, "[Spam] " + message);}
	
	public void hyper(String message) {logFilterLevel(SPAM, "[Hyper] " + message);}
	
	public void setLogLevel(String level) {
		if(level == null){
			setLevel(Level.INFO);
			return;
		}
		if(level.equalsIgnoreCase("info") || level.equalsIgnoreCase("normal")){
			setLevel(Level.INFO);
		} else if(level.equalsIgnoreCase("notice")){
			setLevel(NOTICE);
		} else if(level.equalsIgnoreCase("helpful")){
			setLevel(HELPFUL);
		} else if(level.equalsIgnoreCase("debug")){
			setLevel(DEBUG);
		} else if(level.equalsIgnoreCase("trace")){
			setLevel(TRACE);
		} else if(level.equalsIgnoreCase("spam")){
			setLevel(SPAM);
		} else if(level.equalsIgnoreCase("hyper")){
			setLevel(HYPER);
		}
	}
	
	public Level getLogLevel() {
		return getLevel();
	}
	
	// Get the stack trace of a Throwable as a string
	public String getStackTrace(Throwable t) {
		StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
}
