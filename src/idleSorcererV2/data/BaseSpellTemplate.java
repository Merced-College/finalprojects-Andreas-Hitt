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

import idleSorvererV2.enums.CoreEffectType;
import idleSorvererV2.enums.DamageType;
import idleSorvererV2.enums.HealingType;
import idleSorvererV2.enums.PrimaryAttributeType;

// every spell is loaded in from a text file in this format
// it will then be plugged into SpellGenerator to modify the spell's strength and enchant it
public record BaseSpellTemplate(
    String id, // spell identifier
    String name, // display name
    PrimaryAttributeType scalingAttribute, // the attribute which affects this spell
    CoreEffectData coreEffect, // What the spell does (damage/healing/shieldiing) + how much
    CooldownRangeData cooldownRange, // the spell's randomly selected cooldown
    InherentPropertiesData inherentProperties //
) {

/**
 * Loads base spell templates from a specified text file in the resources folder.
 * The text file should follow the defined key-value format with "---" separators.
 * @param filePathInResources The path to the file within the resources folder (e.g., "/player_base_spells.txt").
 * @return A List of BaseSpellTemplate objects parsed from the file.
 */
public static List<BaseSpellTemplate> loadBaseSpellTemplatesFromFile(String filePathInResources) {
    List<BaseSpellTemplate> templates = new ArrayList<>();

    try (InputStream inputStream = BaseSpellTemplate.class.getResourceAsStream(filePathInResources);
         InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
         BufferedReader reader = new BufferedReader(streamReader)) {

        if (inputStream == null) {
            System.err.println("BaseSpellTemplate Loader Error: Cannot find file: " + filePathInResources);
            return templates; // Return empty list
        }

        String line;
        // Temporary holders for the current spell template being built
        String currentId = null;
        String currentName = null;
        PrimaryAttributeType currentScalingAttribute = PrimaryAttributeType.NONE; // Default

        // Temp holders for CoreEffectData
        CoreEffectType currentCoreEffectType = null;
        double currentCoreBaseValue = 0.0;
        DamageType currentCoreDamageType = null;
        HealingType currentCoreHealingType = null;
        String currentCoreDotType = null;
        boolean currentCoreDealsNoInitialDamage = false;
        boolean currentCoreStacking = false;

        // Temp holders for CooldownRangeData
        double currentCdMin = 0.0;
        double currentCdMax = 0.0;

        // Temp holders for InherentPropertiesData
        boolean currentInherentDealsDoubleDamageToShields = false;
        boolean currentInherentIgnoresArmor = false;
        boolean currentInherentIgnoresShield = false;
        boolean currentInherentAlwaysHits = false;
        int currentInherentAccuracyBonus = 0;
        String currentInherentOnHitEffect = null;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) { // Skip empty lines and comments
                continue;
            }

            if (line.equals("---")) { // End of a spell template block
                // Construct and add the spell template if we have minimal valid data (id and name)
                if (currentId != null && currentName != null) {
                    CoreEffectData coreEffect = new CoreEffectData(currentCoreEffectType, currentCoreBaseValue,
                                                                 currentCoreDamageType, currentCoreHealingType,
                                                                 currentCoreDotType, currentCoreDealsNoInitialDamage,
                                                                 currentCoreStacking);
                    CooldownRangeData cooldownRange = new CooldownRangeData(currentCdMin, currentCdMax);
                    InherentPropertiesData inherentProperties = new InherentPropertiesData(
                                                                 currentInherentDealsDoubleDamageToShields,
                                                                 currentInherentIgnoresArmor, currentInherentIgnoresShield,
                                                                 currentInherentAlwaysHits, currentInherentAccuracyBonus,
                                                                 currentInherentOnHitEffect);

                    templates.add(new BaseSpellTemplate(currentId, currentName, currentScalingAttribute,
                                                        coreEffect, cooldownRange, inherentProperties));
                }
                // Reset for the next template
                currentId = null; currentName = null; currentScalingAttribute = PrimaryAttributeType.NONE;
                currentCoreEffectType = null; currentCoreBaseValue = 0.0; currentCoreDamageType = null;
                currentCoreHealingType = null; currentCoreDotType = null; currentCoreDealsNoInitialDamage = false;
                currentCoreStacking = false;
                currentCdMin = 0.0; currentCdMax = 0.0;
                currentInherentDealsDoubleDamageToShields = false; currentInherentIgnoresArmor = false;
                currentInherentIgnoresShield = false; currentInherentAlwaysHits = false;
                currentInherentAccuracyBonus = 0; currentInherentOnHitEffect = null;
                continue;
            }

            String[] parts = line.split(":", 2);
            if (parts.length < 2) {
                System.err.println("BaseSpellTemplate Loader Warning: Malformed line (missing colon): " + line);
                continue;
            }
            String key = parts[0].trim();
            String value = parts[1].trim();

            try {
                switch (key) {
                    case "id": currentId = value; break;
                    case "name": currentName = value; break;
                    case "scalingAttribute": currentScalingAttribute = PrimaryAttributeType.valueOf(value.toUpperCase()); break;

                    // CoreEffect properties
                    case "coreEffect.type": currentCoreEffectType = CoreEffectType.valueOf(value.toUpperCase()); break;
                    case "coreEffect.baseValue": currentCoreBaseValue = Double.parseDouble(value); break;
                    case "coreEffect.damageType": currentCoreDamageType = DamageType.valueOf(value.toUpperCase()); break;
                    case "coreEffect.healingType": currentCoreHealingType = HealingType.valueOf(value.toUpperCase()); break;
                    case "coreEffect.dotType": currentCoreDotType = value; break;
                    case "coreEffect.dealsNoInitialDamage": currentCoreDealsNoInitialDamage = Boolean.parseBoolean(value); break;
                    case "coreEffect.stacking": currentCoreStacking = Boolean.parseBoolean(value); break;

                    // CooldownRange properties
                    case "cooldownRange.minSeconds": currentCdMin = Double.parseDouble(value); break;
                    case "cooldownRange.maxSeconds": currentCdMax = Double.parseDouble(value); break;

                    // InherentProperties
                    case "inherentProperties.dealsDoubleDamageToShields": currentInherentDealsDoubleDamageToShields = Boolean.parseBoolean(value); break;
                    case "inherentProperties.ignoresArmor": currentInherentIgnoresArmor = Boolean.parseBoolean(value); break;
                    case "inherentProperties.ignoresShield": currentInherentIgnoresShield = Boolean.parseBoolean(value); break;
                    case "inherentProperties.alwaysHits": currentInherentAlwaysHits = Boolean.parseBoolean(value); break;
                    case "inherentProperties.accuracyBonus": currentInherentAccuracyBonus = Integer.parseInt(value); break;
                    case "inherentProperties.onHitEffect": currentInherentOnHitEffect = value; break;

                    default:
                        System.err.println("BaseSpellTemplate Loader Warning: Unknown key '" + key + "' in line: " + line);
                        break;
                }
            } catch (IllegalArgumentException e) {
                System.err.println("BaseSpellTemplate Loader Error: Invalid enum value or number format for key '" + key + "' in line: " + line + " - " + e.getMessage());
            }
        }

        // Add the last spell template if the file doesn't end with "---" but has data
        if (currentId != null && currentName != null) {
            CoreEffectData coreEffect = new CoreEffectData(currentCoreEffectType, currentCoreBaseValue,
                                                         currentCoreDamageType, currentCoreHealingType,
                                                         currentCoreDotType, currentCoreDealsNoInitialDamage,
                                                         currentCoreStacking);
            CooldownRangeData cooldownRange = new CooldownRangeData(currentCdMin, currentCdMax);
            InherentPropertiesData inherentProperties = new InherentPropertiesData(
                                                         currentInherentDealsDoubleDamageToShields,
                                                         currentInherentIgnoresArmor, currentInherentIgnoresShield,
                                                         currentInherentAlwaysHits, currentInherentAccuracyBonus,
                                                         currentInherentOnHitEffect);

            templates.add(new BaseSpellTemplate(currentId, currentName, currentScalingAttribute,
                                                coreEffect, cooldownRange, inherentProperties));
        }

    } catch (IOException e) {
        System.err.println("BaseSpellTemplate Loader Error: Failed to read file: " + filePathInResources);
        e.printStackTrace();
    } catch (NullPointerException e) {
        // This can happen if getResourceAsStream returns null and it's not caught early,
        // though the check "if (inputStream == null)" should prevent this.
        System.err.println("BaseSpellTemplate Loader Error: Could not find the resource file (NullPointerException): " + filePathInResources);
        e.printStackTrace();
    }

    System.out.println("BaseSpellTemplate Loader: Successfully loaded " + templates.size() + " spell templates.");
    return templates;
}
}
