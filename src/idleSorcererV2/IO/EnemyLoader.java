package idleSorcererV2.IO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import idleSorcererV2.Enemy;
import idleSorcererV2.EnemySpellData;
import idleSorvererV2.enums.DamageType;

// Assuming DamageType, Enemy, EnemySpellData enums/classes are defined

public class EnemyLoader {

    private List<Enemy> loadedEnemies; // Stores the "template" Enemy objects

    /**
     * Constructor for EnemyLoader.
     * Attempts to load enemies from the default path upon instantiation.
     */
    public EnemyLoader() {
        this.loadedEnemies = new ArrayList<>();
        // Path relative to the resources folder (which should be on the classpath)
        loadEnemiesFromTxtFile("/enemies.txt");
    }

    /**
     * Loads enemy definitions from a TXT file located in the classpath.
     * @param filePathInResources The path to the TXT file (e.g., "/enemies.txt").
     */
    private void loadEnemiesFromTxtFile(String filePathInResources) {
        try (InputStream inputStream = EnemyLoader.class.getResourceAsStream(filePathInResources);
             InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)) {

            if (inputStream == null) {
                System.err.println("EnemyLoader Error: Cannot find enemy file: " + filePathInResources);
                return;
            }

            String line;
            // Temporary variables to build up current enemy
            String currentId = null;
            String currentName = null;
            int currentMaxHP = 0;
            int currentBaseAccuracy = 0;
            int currentBaseDodge = 0;
            int currentArmor = 0;
            int currentInitialShield = 0;
            int currentRegenPerSecond = 0;
            List<EnemySpellData> currentSpells = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) { // Skip empty lines and comments
                    continue;
                }

                if (line.equals("---")) { // End of an enemy block
                    if (currentId != null && currentName != null) { // Check if we have valid data to add
                        loadedEnemies.add(new Enemy(currentId, currentName, currentMaxHP, currentBaseAccuracy,
                                                    currentBaseDodge, currentArmor, currentInitialShield,
                                                    currentRegenPerSecond, new ArrayList<>(currentSpells))); // Add a copy of spells
                        // Reset for next enemy
                        currentId = null;
                        currentName = null;
                        currentSpells.clear();
                    }
                    continue;
                }

                String[] parts = line.split(":", 2); // Split by the first colon
                if (parts.length < 2) {
                    System.err.println("EnemyLoader Warning: Malformed line (missing colon): " + line);
                    continue;
                }
                String key = parts[0].trim();
                String value = parts[1].trim();

                switch (key) {
                    case "id":
                        currentId = value;
                        break;
                    case "name":
                        currentName = value;
                        break;
                    case "maxHP":
                        currentMaxHP = Integer.parseInt(value);
                        break;
                    case "baseAccuracy":
                        currentBaseAccuracy = Integer.parseInt(value);
                        break;
                    case "baseDodge":
                        currentBaseDodge = Integer.parseInt(value);
                        break;
                    case "armor":
                        currentArmor = Integer.parseInt(value);
                        break;
                    case "initialShield":
                        currentInitialShield = Integer.parseInt(value);
                        break;
                    case "regenPerSecond":
                        currentRegenPerSecond = Integer.parseInt(value);
                        break;
                    case "spell":
                        String[] spellParts = value.split(",");
                        if (spellParts.length == 5) {
                            String spellName = spellParts[0].trim();
                            int spellDamage = Integer.parseInt(spellParts[1].trim());
                            double spellCooldown = Double.parseDouble(spellParts[2].trim());
                            boolean ignoresArmor = Boolean.parseBoolean(spellParts[3].trim());
                            boolean ignoresShield = Boolean.parseBoolean(spellParts[4].trim());

                            // Enemy spells don't use DamageType for their own attacks based on our last discussion
                            DamageType spellDamageType = null;
                            // Assuming EnemySpellData constructor: name, damage, cooldown, damageType, ignoresArmor, ignoresShield, alwaysHits
                            currentSpells.add(new EnemySpellData(spellName, spellDamage, spellCooldown,
                                                                 spellDamageType, ignoresArmor, ignoresShield, false /*alwaysHits placeholder*/));
                        } else {
                            System.err.println("EnemyLoader Warning: Malformed spell line: " + value);
                        }
                        break;
                    default:
                        System.err.println("EnemyLoader Warning: Unknown key '" + key + "' in line: " + line);
                        break;
                }
            }
            // Add the last enemy if the file doesn't end with "---" but has data
            if (currentId != null && currentName != null) {
                 loadedEnemies.add(new Enemy(currentId, currentName, currentMaxHP, currentBaseAccuracy,
                                            currentBaseDodge, currentArmor, currentInitialShield,
                                            currentRegenPerSecond, new ArrayList<>(currentSpells)));
            }

            System.out.println("EnemyLoader: Successfully loaded " + loadedEnemies.size() + " enemy types from " + filePathInResources);

        } catch (IOException e) {
            System.err.println("EnemyLoader Error: Failed to read enemy file: " + filePathInResources);
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.err.println("EnemyLoader Error: Malformed number in enemy file.");
            e.printStackTrace();
        } catch (Exception e) { // Catch any other unexpected errors during parsing
            System.err.println("EnemyLoader Error: Unexpected error parsing enemy file: " + filePathInResources);
            e.printStackTrace();
        }
    }

    /**
     * Retrieves a *new instance* of an enemy for a specific floor.
     * Assumes enemies in the TXT file are ordered by their floor appearance (Floor 1 is at index 0).
     * @param floorNumber The floor number (1-based).
     * @return A new Enemy object for that floor, or null if the floor number is invalid.
     */
    public Enemy getEnemyForFloor(int floorNumber) {
        if (loadedEnemies != null && floorNumber >= 1 && floorNumber <= loadedEnemies.size()) {
            Enemy template = loadedEnemies.get(floorNumber - 1);

            // Create and return a new Enemy instance based on the template.
            // The Enemy constructor should handle setting initial combat state (HP, cooldowns, etc.).
            return new Enemy(
                template.getId(),
                template.getName(),
                template.getMaxHP(),
                template.getBaseAccuracy(),
                template.getBaseDodge(),
                template.getBaseArmor(),
                template.getCurrentShield(), // This should be the template's *initial* shield
                                             // Ensure Enemy class has a way to get initialShield or that currentShield on template is initial.
                template.getBaseRegenPerSecond(),
                new ArrayList<>(template.getDefinedSpells()) // Pass a copy of the spell list
            );
        } else {
            System.err.println("EnemyLoader Error: Invalid floor number " + floorNumber + ". Max floor with loaded enemy: " + (loadedEnemies != null ? loadedEnemies.size() : 0));
            return null;
        }
    }

    /**
     * Gets the total number of unique enemy types loaded.
     * @return The count of loaded enemies.
     */
    public int getNumberOfEnemyTypesLoaded() {
        if (loadedEnemies == null) {
            return 0;
        }
        return loadedEnemies.size();
    }
}
