// Andreas Hitt
// CPSC - 39
// Prof. Kanemoto
// May 13 2025

package idleSorcererV2.data;

import java.text.DecimalFormat; // For formatting percentages

public record EnchantInstance(
    BaseEnchant baseEnchant,
    double finalRolledValue
) {
    public String getDescription() {
        String formattedValue;
        // Check if the effect type implies a percentage
        // This check might need to be more robust based on your EnchantEffectType enum
        if (baseEnchant.effectType().name().contains("PERCENT") ||
            baseEnchant.descriptionTemplate().contains("%")) {
            // Format percentages with one decimal place and a % sign
            DecimalFormat df = new DecimalFormat("#.#");
            formattedValue = df.format(finalRolledValue) + "%";
        } else {
            // Format flat values as integers (or rounded floats if they can be fractional)
            formattedValue = String.format("%.0f", finalRolledValue);
        }
        return baseEnchant.descriptionTemplate().replace("{value}", formattedValue.replace("%",""));
    }
}