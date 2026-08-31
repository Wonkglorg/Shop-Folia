package com.wonkglorg.minecraft.shop.shop.creation;

import com.wonkglorg.minecraft.shop.shop.ShopType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class SignCreationLayoutParser{
	
	private SignCreationLayoutParser() {
	}
	
	/**
	 * Parses the configured creation layouts once.
	 *
	 * Runtime validation should use the returned map rather than this parser.
	 */
	public static Map<ShopType, CreationLayout> parse(Map<String, Object> config) {
		Map<ShopType, CreationLayout> result = new EnumMap<>(ShopType.class);
		
		for(ShopType type : ShopType.values()){
			Object raw = config.get(type.name().toLowerCase());
			
			if(!(raw instanceof Map<?, ?> map)){
				throw new IllegalArgumentException("Missing creation layout for shop type " + type);
			}
			
			CreationRequirement[] requirements = new CreationRequirement[4];
			
			for(int line = 1; line <= 4; line++){
				Object rawLine = map.get(line);
				
				// Also support YAML keys being loaded as Strings.
				if(rawLine == null){
					rawLine = map.get(String.valueOf(line));
				}
				
				if(!(rawLine instanceof List<?> values)){
					throw new IllegalArgumentException("Creation layout for " + type + " line " + line + " must be a list");
				}
				
				requirements[line - 1] = parseLine(values, type, line);
			}
			
			result.put(type, new CreationLayout(requirements));
		}
		
		return Collections.unmodifiableMap(result);
	}
	
	private static CreationRequirement parseLine(List<?> values, ShopType type, int line) {
		if(values.isEmpty()){
			throw new IllegalArgumentException("Creation layout for " + type + " line " + line + " cannot be empty");
		}
		
		List<CreationRequirement> requirements = new ArrayList<>(values.size());
		
		for(Object value : values){
			if(!(value instanceof String string)){
				throw new IllegalArgumentException("Creation layout for " + type + " line " + line + " contains a non-string value");
			}
			
			requirements.add(parseRequirement(string));
		}
		
		return new AnyOf(requirements);
	}
	
	private static CreationRequirement parseRequirement(String value) {
		return switch(value) {
			case "%amount%" -> AmountRequirement.INSTANCE;
			case "%price%" -> PriceRequirement.INSTANCE;
			default -> new ExactRequirement(value);
		};
	}
	
	public record CreationLayout(CreationRequirement[] lines){
		
		public CreationLayout {
			if(lines.length != 4){
				throw new IllegalArgumentException("A creation layout must contain exactly 4 lines");
			}
		}
		
		/**
		 * Checks whether the four sign lines match this layout.
		 */
		public boolean matches(String[] signLines) {
			if(signLines.length != 4){
				return false;
			}
			
			return lines[0].matches(signLines[0]) &&
				   lines[1].matches(signLines[1]) &&
				   lines[2].matches(signLines[2]) &&
				   lines[3].matches(signLines[3]);
		}
		
		public CreationRequirement line(int index) {
			return lines[index];
		}
	}
	
	public interface CreationRequirement{
		
		boolean matches(String value);
	}
	
	private record ExactRequirement(String value) implements CreationRequirement{
		
		@Override
		public boolean matches(String input) {
			return value.equalsIgnoreCase(input);
		}
	}
	
	private static final class AnyOf implements CreationRequirement{
		
		private final CreationRequirement[] requirements;
		
		private AnyOf(List<CreationRequirement> requirements) {
			this.requirements = requirements.toArray(CreationRequirement[]::new);
		}
		
		@Override
		public boolean matches(String value) {
			for(CreationRequirement requirement : requirements){
				if(requirement.matches(value)){
					return true;
				}
			}
			
			return false;
		}
	}
	
	private static final class AmountRequirement implements CreationRequirement{
		
		private static final AmountRequirement INSTANCE = new AmountRequirement();
		
		@Override
		public boolean matches(String value) {
			try{
				return Integer.parseInt(value) > 0;
			} catch(NumberFormatException ignored){
				return false;
			}
		}
	}
	
	private static final class PriceRequirement implements CreationRequirement{
		
		private static final PriceRequirement INSTANCE = new PriceRequirement();
		
		@Override
		public boolean matches(String value) {
			try{
				return Double.parseDouble(value) >= 0;
			} catch(NumberFormatException ignored){
				return false;
			}
		}
	}
}