// Andreas Hitt
// CPSC - 39
// Prof. Kanemoto
// May 13 2025

package idleSorcererV2.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import idleSorvererV2.enums.EnchantEffectType;

public record BaseEnchant(
    String enchantID,             // e.g., "ENCH_ADD_AGILITY"
    String descriptionTemplate,   // e.g., "+{value} Agility"
    EnchantEffectType effectType, // Enum: e.g., PLAYER_PRIMARY_STAT_ADD_FLAT
    String targetKey,             // String from txt: e.g., "AGILITY", "FIRE_DAMAGE", "THIS_SPELL_POTENCY"
                                  // This key will be interpreted based on effectType
                                  // to apply the enchant correctly.
    double trueBaseValue,         // The minimum value of the enchant
    double maxTotalValueAtFloor50 // The maximum value at floor 50 (with max M roll)
) {

    /**
     * Loads base enchant definitions from a specified text file in the resources folder.
     * The text file should follow the defined key-value format with "---" separators.
     * @param filePathInResources The path to the file within the resources folder (e.g., "/base_enchants.txt").
     * @return A List of BaseEnchant objects parsed from the file.
     */
    public static List<BaseEnchant> loadBaseEnchantsFromFile(String filePathInResources) {
        List<BaseEnchant> enchants = new ArrayList<>();

        try (InputStream inputStream = BaseEnchant.class.getResourceAsStream(filePathInResources);
             InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)) {

            if (inputStream == null) {
                System.err.println("BaseEnchant Loader Error: Cannot find file: " + filePathInResources);
                return enchants; // Return empty list
            }

            String line;
            // Temporary holders for the current enchant being built
            String currentEnchantID = null;
            String currentDescriptionTemplate = null;
            EnchantEffectType currentEffectType = null;
            String currentTargetKey = null;
            double currentTrueBaseValue = 0.0;
            double currentMaxTotalValueAtFloor50 = 0.0;

            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) { // Skip empty lines and comments
                    continue;
                }

                if (line.equals("---")) { // End of an enchant block
                    // Construct and add the enchant if we have minimal valid data (id and name)
                    if (currentEnchantID != null && currentDescriptionTemplate != null && currentEffectType != null && currentTargetKey != null) {
                        enchants.add(new BaseEnchant(currentEnchantID, currentDescriptionTemplate, currentEffectType,
                                                     currentTargetKey, currentTrueBaseValue, currentMaxTotalValueAtFloor50));
                    } else {
                        System.err.println("BaseEnchant Loader Warning: Incomplete enchant data block ending before line " + lineNumber + ". Skipping.");
                    }
                    // Reset for the next enchant
                    currentEnchantID = null; currentDescriptionTemplate = null; currentEffectType = null;
                    currentTargetKey = null; currentTrueBaseValue = 0.0; currentMaxTotalValueAtFloor50 = 0.0;
                    continue;
                }

                String[] parts = line.split(":", 2);
                if (parts.length < 2) {
                    System.err.println("BaseEnchant Loader Warning: Malformed line (missing colon) at line " + lineNumber + ": " + line);
                    continue;
                }
                String key = parts[0].trim();
                String value = parts[1].trim();

                try {
                    switch (key) {
                        case "enchantID":
                            currentEnchantID = value;
                            break;
                        case "descriptionTemplate":
                            currentDescriptionTemplate = value;
                            break;
                        case "effectType":
                            currentEffectType = EnchantEffectType.valueOf(value.toUpperCase());
                            break;
                        case "targetKey":
                            currentTargetKey = value; // Store as string, interpretation is later
                            break;
                        case "trueBaseValue":
                            currentTrueBaseValue = Double.parseDouble(value);
                            break;
                        case "maxTotalValueAtFloor50":
                            currentMaxTotalValueAtFloor50 = Double.parseDouble(value);
                            break;
                        default:
                            System.err.println("BaseEnchant Loader Warning: Unknown key '" + key + "' at line " + lineNumber + ": " + line);
                            break;
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("BaseEnchant Loader Error: Invalid enum value or number format for key '" + key + "' at line " + lineNumber + ": " + line + " - " + e.getMessage());
                }
            }

            // Add the last enchant if the file doesn't end with "---" but has data
            if (currentEnchantID != null && currentDescriptionTemplate != null && currentEffectType != null && currentTargetKey != null) {
                 enchants.add(new BaseEnchant(currentEnchantID, currentDescriptionTemplate, currentEffectType,
                                             currentTargetKey, currentTrueBaseValue, currentMaxTotalValueAtFloor50));
            }

        } catch (IOException e) {
            System.err.println("BaseEnchant Loader Error: Failed to read file: " + filePathInResources);
            e.printStackTrace();
        } catch (NullPointerException e) {
            // This can happen if getResourceAsStream returns null and it's not caught early
            System.err.println("BaseEnchant Loader Error: Could not find the resource file (NullPointerException): " + filePathInResources);
            e.printStackTrace();
        }

        System.out.println("BaseEnchant Loader: Successfully loaded " + enchants.size() + " base enchant definitions.");
        return enchants;
    }
}
