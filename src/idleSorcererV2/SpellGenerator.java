// Andreas Hitt
// CPSC - 39
// Prof. Kanemoto
// May 13 2025

package idleSorcererV2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections; // For Collections.unmodifiableList
import java.util.List;
import java.util.Random;

import idleSorcererV2.data.BaseEnchant;
import idleSorcererV2.data.BaseSpellTemplate;
import idleSorcererV2.data.CoreEffectData;
import idleSorcererV2.data.EnchantInstance;
import idleSorcererV2.data.PlayerSpell;


public class SpellGenerator {
    private final List<BaseSpellTemplate> allBaseSpellTemplates;
    private final List<BaseEnchant> allPossibleBaseEnchants;
    private final Random random = new Random();

    public static final int GLOBAL_BASE_AP_COST = 5;
    public static final double[] ENCHANT_COUNT_AP_MULTIPLIERS = {1.0, 1.25, 1.50, 1.75, 2.0}; // Index = num enchants

    /**
     * Constructor for SpellGenerator.
     * @param allBaseSpellTemplates List of all available base spell templates.
     * @param allPossibleBaseEnchants List of all available base enchant definitions.
     */
    public SpellGenerator(List<BaseSpellTemplate> allBaseSpellTemplates, List<BaseEnchant> allPossibleBaseEnchants) {
        this.allBaseSpellTemplates = new ArrayList<>(allBaseSpellTemplates); // Store copies
        this.allPossibleBaseEnchants = new ArrayList<>(allPossibleBaseEnchants);
    }

    /**
     * Generates a random player spell based on the current floor.
     * @param currentFloor The current floor number (1-10).
     * @return A fully generated PlayerSpell object, or null if generation fails.
     */
    public PlayerSpell generateSpellDrop(int currentFloor) {
        if (allBaseSpellTemplates == null || allBaseSpellTemplates.isEmpty()) {
            System.err.println("SpellGenerator Error: No base spell templates loaded!");
            return null;
        }
        if (allPossibleBaseEnchants == null) {
            System.err.println("SpellGenerator Warning: No base enchants loaded. Spells will have no enchants.");
        }
        if (currentFloor < 1) {
            currentFloor = 1; // Ensure floor is at least 1
        }

        // 1. Select Random Base Spell Template
        BaseSpellTemplate selectedTemplate = allBaseSpellTemplates.get(random.nextInt(allBaseSpellTemplates.size()));

        // 2. Randomize Cooldown
        double minCD = selectedTemplate.cooldownRange().minSeconds();
        double maxCD = selectedTemplate.cooldownRange().maxSeconds();
        double rawCooldown = minCD + (maxCD - minCD) * random.nextDouble();
        double actualCooldownSeconds = BigDecimal.valueOf(rawCooldown).setScale(1, RoundingMode.HALF_UP).doubleValue();

        // 3. Randomize Spell Core Effect Strength
        int M_spell = random.nextInt(currentFloor) + 1; // M is 1 to currentFloor
        double effectiveCoreEffectValue = selectedTemplate.coreEffect().baseValue() * M_spell;

        // 4. Determine Number of Enchants (0-4)
        int numberOfEnchants = random.nextInt(5);
        List<EnchantInstance> appliedEnchants = new ArrayList<>();

        // 5. Generate Enchant Instances
        if (allPossibleBaseEnchants != null && !allPossibleBaseEnchants.isEmpty() && numberOfEnchants > 0) {
            for (int i = 0; i < numberOfEnchants; i++) {
                BaseEnchant selectedBaseEnchant = allPossibleBaseEnchants.get(random.nextInt(allPossibleBaseEnchants.size()));

                double trueBaseValue = selectedBaseEnchant.trueBaseValue();
                double maxTotalValueAtFloor50 = selectedBaseEnchant.maxTotalValueAtFloor50();
                double maxPotentialIncreaseByF50 = maxTotalValueAtFloor50 - trueBaseValue;

                int M_enchant = random.nextInt(currentFloor) + 1; // M is 1 to currentFloor

                // Strength is calculated based on M_enchant (effective floor)
                double finalEnchantValue = trueBaseValue +
                                           Math.round(((maxPotentialIncreaseByF50 / 50.0) * M_enchant));

                finalEnchantValue = Math.min(finalEnchantValue, maxTotalValueAtFloor50);
                finalEnchantValue = Math.max(finalEnchantValue, trueBaseValue);

                // Create EnchantInstance without mValueUsed, as it's not needed for AP cost now
                appliedEnchants.add(new EnchantInstance(selectedBaseEnchant, finalEnchantValue));
            }
        }

        // 6. Calculate Final AP Cost
        double apSum = GLOBAL_BASE_AP_COST;

        CoreEffectData coreEffect = selectedTemplate.coreEffect();
        // Check if the core effect type is one that has a directly quantifiable 'strength'
        // that should contribute to AP cost.
        switch (coreEffect.type()) {
            case DAMAGE:
            case HEALING:
            case SHIELD_APPLICATION:
            case APPLY_DOT:
            case BUFF_PLAYER:
            case DEBUFF_ENEMY:
                apSum += Math.abs(effectiveCoreEffectValue);
                break;
            default:
                // For other core effect types that might not have a simple numerical "strength"
                // or shouldn't contribute to AP cost from their core effect, do nothing here.
                System.out.println("SpellGenerator Note: CoreEffectType " + coreEffect.type() +
                                   " does not currently have a defined AP cost contribution for its core effect.");
                break;
        }

        // Add enchant values to AP sum
        for (EnchantInstance enchant : appliedEnchants) {
            // For percentage enchants, add the number (e.g., 25 for 25%)
            // For flat value enchants, add the flat value.
            apSum += Math.abs(enchant.finalRolledValue());
        }

        double enchantMultiplier = ENCHANT_COUNT_AP_MULTIPLIERS[Math.min(numberOfEnchants, ENCHANT_COUNT_AP_MULTIPLIERS.length - 1)];
        int finalAPCost = (int) Math.round(apSum * enchantMultiplier);
        finalAPCost = Math.max(5, finalAPCost); // Ensure AP cost is at least 5

        // 7. Construct and Return PlayerSpell Instance
        return new PlayerSpell(
                selectedTemplate,
                actualCooldownSeconds,
                effectiveCoreEffectValue,
                appliedEnchants,
                finalAPCost
        );
    } // End of generateSpellDrop method

    /**
     * Provides access to the list of base spell templates loaded by this generator.
     * This is useful for other parts of the game that might need to look up base spell definitions,
     * for example, when creating fixed starting spells for the player in GameManager.
     * @return An unmodifiable list of BaseSpellTemplate objects.
     */
    public List<BaseSpellTemplate> getLoadedBaseSpellTemplates() {
        // Return an unmodifiable list to protect the internal list from external changes.
        return Collections.unmodifiableList(new ArrayList<>(this.allBaseSpellTemplates));
    }

}
