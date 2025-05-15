// Andreas Hitt
// CPSC - 39
// Prof. Kanemoto
// May 13 2025

package idleSorcererV2;

import java.util.ArrayList;
import java.util.List;

import idleSorcererV2.IO.EnemyLoader;
import idleSorcererV2.data.BaseSpellTemplate;
import idleSorcererV2.data.CooldownRangeData;
import idleSorcererV2.data.CoreEffectData;
import idleSorcererV2.data.EnchantInstance;
import idleSorcererV2.data.InherentPropertiesData;
import idleSorcererV2.data.PlayerSpell;
import idleSorvererV2.enums.CoreEffectType;
import idleSorvererV2.enums.DamageType;
import idleSorvererV2.enums.EnchantEffectType;
import idleSorvererV2.enums.EnchantTargetParameter;
import idleSorvererV2.enums.GameState;
import idleSorvererV2.enums.HealingType;
import idleSorvererV2.enums.PrimaryAttributeType;

public class GameManager {

    private Player player;
    private Enemy currentEnemy;
    private EnemyLoader enemyLoader;
    private SpellGenerator spellGenerator;

    private int currentFloor;
    private GameState currentGameState;

    private long lastUpdateTimeNanos;
    private double timeAccumulatorForSecondTick;

    private static final double ONE_SECOND_IN_NANOS = 1_000_000_000.0;
    private static final int MAX_FLOOR = 10;

    public GameManager(Player player, EnemyLoader enemyLoader, SpellGenerator spellGenerator) {
        this.player = player;
        this.enemyLoader = enemyLoader;
        this.spellGenerator = spellGenerator;
        this.currentFloor = 1;
        this.currentGameState = GameState.INITIALIZING;
        this.timeAccumulatorForSecondTick = 0.0;
    }

    public void initializeGame() {
        System.out.println("Game Initializing..."); // Log
        this.lastUpdateTimeNanos = System.nanoTime();

        addStartingSpellsToPlayer(player);

        if (!loadEnemyForCurrentFloor()) {
            this.currentGameState = GameState.GAME_OVER;
            System.err.println("Failed to load initial enemy. Game cannot start properly.");
            return;
        }
        player.resetCombatState();
        if (currentEnemy != null) {
            currentEnemy.resetCombatState();
        }

        this.currentGameState = GameState.MANAGEMENT_PAUSED;
        System.out.println("Game Initialized. Welcome, " + player.getName() + "!");
        System.out.println("You are on Floor " + currentFloor + ". Enemy: " + (currentEnemy != null ? currentEnemy.getName() : "None"));
        System.out.println("You start with Singe and Minor Heal equipped (if found).");
        System.out.println("Type 'battle advance' or 'battle farm' to begin, or 'help' for commands.");
    }

    public void update() {
        long currentTimeNanos = System.nanoTime();
        double deltaTimeSeconds = (currentTimeNanos - this.lastUpdateTimeNanos) / ONE_SECOND_IN_NANOS;
        this.lastUpdateTimeNanos = currentTimeNanos;
        deltaTimeSeconds = Math.min(deltaTimeSeconds, 0.1);

        switch (currentGameState) {
            case AUTO_BATTLING:
                processCombatTick(deltaTimeSeconds);
                break;
            case PLAYER_WON_ENCOUNTER:
                handlePlayerWonEncounter();
                break;
            case PLAYER_LOST_ENCOUNTER:
                handlePlayerLostEncounter();
                break;
            case STARTING_NEW_FLOOR:
                prepareForNextEncounterAndBattle();
                break;
            case MANAGEMENT_PAUSED:
            case INITIALIZING:
            case GAME_OVER:
                break;
        }
    }

    private void processCombatTick(double deltaTimeSeconds) {
        if (player == null || currentEnemy == null) {
             System.err.println("Error: Combat tick with null player or enemy.");
             currentGameState = GameState.MANAGEMENT_PAUSED;
             return;
        }
        if (!player.isAlive()) {
            currentGameState = GameState.PLAYER_LOST_ENCOUNTER;
            return;
        }
        if (!currentEnemy.isAlive()) {
            currentGameState = GameState.PLAYER_WON_ENCOUNTER;
            return;
        }

        player.updateCooldowns(deltaTimeSeconds);
        currentEnemy.updateCooldowns(deltaTimeSeconds);

        List<PlayerSpell> playerReadySpells = player.getReadySpells();
        for (PlayerSpell spell : playerReadySpells) {
            if (!currentEnemy.isAlive()) {
				break;
			}
            System.out.println(player.getName() + " casts " + spell.getName() + "!");
            applyPlayerSpellEffects(spell, currentEnemy);
            player.triggerCooldownForSpell(spell);
            if (!currentEnemy.isAlive()) {
                currentGameState = GameState.PLAYER_WON_ENCOUNTER;
                return;
            }
        }

        List<EnemySpellData> enemyReadySpells = currentEnemy.getReadySpells();
        for (EnemySpellData spell : enemyReadySpells) {
            if (!player.isAlive()) {
				break;
			}
            System.out.println(currentEnemy.getName() + " casts " + spell.getName() + "!");
            applyEnemySpellEffects(spell, player);
            currentEnemy.triggerCooldownForSpell(spell);
            if (!player.isAlive()) {
                currentGameState = GameState.PLAYER_LOST_ENCOUNTER;
                return;
            }
        }

        timeAccumulatorForSecondTick += deltaTimeSeconds;
        if (timeAccumulatorForSecondTick >= 1.0) {
            if (player.isAlive()) {
				player.applyPeriodicEffects();
			}
            if (!player.isAlive()) {
                currentGameState = GameState.PLAYER_LOST_ENCOUNTER;
                return;
            }
            if (currentEnemy.isAlive()) {
				currentEnemy.applyPeriodicEffects();
			}
            if (!currentEnemy.isAlive()) {
                currentGameState = GameState.PLAYER_WON_ENCOUNTER;
                return;
            }
            timeAccumulatorForSecondTick -= 1.0;
        }
    }

    private void applyPlayerSpellEffects(PlayerSpell spell, Combatant<?> target) {
        BaseSpellTemplate template = spell.baseTemplate();
        CoreEffectData coreEffect = template.coreEffect();
        InherentPropertiesData inherentProps = template.inherentProperties();
        int attackerAccuracy = player.getEffectiveAccuracy();

        if (inherentProps != null && inherentProps.accuracyBonus() > 0) {
            attackerAccuracy += inherentProps.accuracyBonus();
        }
        for(EnchantInstance enchant : spell.appliedEnchants()){
            if (enchant.baseEnchant().targetKey() != null &&
                enchant.baseEnchant().effectType() == EnchantEffectType.SPELL_ACCURACY_ADD_FLAT &&
                enchant.baseEnchant().targetKey().equals(EnchantTargetParameter.THIS_SPELL_ACCURACY.name())) {
                attackerAccuracy += (int) enchant.finalRolledValue();
            }
        }

        boolean alwaysHits = inherentProps != null && inherentProps.alwaysHits();
        if (alwaysHits) {
			attackerAccuracy = 99999;
		}

        boolean ignoresArmor = inherentProps != null && inherentProps.ignoresArmor();
        boolean ignoresShield = inherentProps != null && inherentProps.ignoresShield();
        boolean dealsDoubleDamageToShields = inherentProps != null && inherentProps.dealsDoubleDamageToShields();

        double currentPotency = spell.effectiveCoreEffectValue();

        PrimaryAttributeType scalingAttr = template.scalingAttribute();
        if (scalingAttr != PrimaryAttributeType.NONE && coreEffect.type() != null) {
            double statBonusPercent = 0;
            if (coreEffect.type() == CoreEffectType.DAMAGE && coreEffect.damageType() != null) {
                statBonusPercent = player.getStats().getCalculatedDamageBonusPercent(coreEffect.damageType());
            } else if (coreEffect.type() == CoreEffectType.HEALING && coreEffect.healingType() != null) {
                statBonusPercent = player.getStats().getCalculatedHealingBonusPercent(coreEffect.healingType());
            }
            currentPotency *= (1.0 + (statBonusPercent / 100.0));
        }

        double globalEnchantBonusPercent = 0;
        if (coreEffect.type() == CoreEffectType.DAMAGE && coreEffect.damageType() != null) {
            globalEnchantBonusPercent = player.getGlobalEnchantDamageBonusPercent(coreEffect.damageType());
        } else if (coreEffect.type() == CoreEffectType.HEALING && coreEffect.healingType() != null) {
            globalEnchantBonusPercent = player.getGlobalEnchantHealingBonusPercent(coreEffect.healingType());
        }
        currentPotency *= (1.0 + (globalEnchantBonusPercent / 100.0));

        for (EnchantInstance enchant : spell.appliedEnchants()) {
             if (enchant.baseEnchant().targetKey() != null &&
                 enchant.baseEnchant().targetKey().equals(EnchantTargetParameter.THIS_SPELL_POTENCY.name())) {
                if (enchant.baseEnchant().effectType() == EnchantEffectType.SPELL_CORE_VALUE_ADD_FLAT) {
                    currentPotency += enchant.finalRolledValue();
                } else if (enchant.baseEnchant().effectType() == EnchantEffectType.SPELL_CORE_VALUE_ADD_PERCENT) {
                    currentPotency *= (1.0 + (enchant.finalRolledValue() / 100.0));
                }
            }
        }
        int roundedPotency = (int) Math.round(Math.max(0, currentPotency));
        int finalDamageToApply = roundedPotency;

        if (coreEffect.type() == CoreEffectType.DAMAGE && dealsDoubleDamageToShields && target.getCurrentShield() > 0) {
            System.out.println("("+ template.name() + " deals double damage vs shields!)");
            finalDamageToApply *= 2;
        }

        switch (coreEffect.type()) {
            case DAMAGE:
                System.out.println("  Calculated damage for " + template.name() + ": " + finalDamageToApply);
                target.takeDamage(finalDamageToApply, coreEffect.damageType(), ignoresArmor, ignoresShield, attackerAccuracy);
                if (target.isAlive() && inherentProps != null && inherentProps.onHitEffect() != null && !inherentProps.onHitEffect().isEmpty()) {
                    applyInherentOnHitEffect(inherentProps.onHitEffect(), target);
                }
                break;
            case HEALING:
                player.applyHealing(roundedPotency);
                break;
            case SHIELD_APPLICATION:
                player.applyShield(roundedPotency);
                break;
            case APPLY_DOT:
                if (target instanceof Enemy) {
                    ((Enemy) target).applyPoisonDamagePerSecond(roundedPotency);
                    System.out.println("Applied " + roundedPotency + " " + coreEffect.dotType() + " DPS to " + target.getName());
                }
                break;
            default:
                System.out.println("Player spell effect type " + coreEffect.type() + " not fully implemented.");
                break;
        }
    }

    private void applyInherentOnHitEffect(String onHitEffectKey, Combatant<?> target) {
        System.out.println("GameManager: Applying on-hit effect: " + onHitEffectKey + " to " + target.getName());
        String[] parts = onHitEffectKey.toUpperCase().split("_");
        int amount = 1;
        for(String part : parts) {
            try {
                amount = Integer.parseInt(part);
                break;
            } catch (NumberFormatException e) { /* ignore */ }
        }

        if (onHitEffectKey.contains("REDUCE_ENEMY_ARMOR")) {
			target.modifyTemporaryArmor(-amount);
		} else if (onHitEffectKey.contains("REDUCE_ENEMY_ACCURACY")) {
			target.modifyTemporaryAccuracy(-amount);
		} else if (onHitEffectKey.contains("REDUCE_ENEMY_DODGE")) {
			target.modifyTemporaryDodge(-amount);
		} else if (onHitEffectKey.contains("REDUCE_ENEMY_REGEN")) {
			target.modifyTemporaryRegenPerSecond(-amount);
		}
    }

    private void applyEnemySpellEffects(EnemySpellData spell, Combatant<?> target) {
        target.takeDamage(spell.getDamage(), null, spell.isArmorPiercing(), spell.isShieldPiercing(), currentEnemy.getEffectiveAccuracy());
    }

    private void handlePlayerWonEncounter() {
        System.out.println(currentEnemy.getName() + " Defeated on Floor " + currentFloor + "!");

        PlayerSpell droppedSpell = spellGenerator.generateSpellDrop(currentFloor);
        if (droppedSpell != null) {
            player.addSpellToInventory(droppedSpell);
            System.out.println("Loot Gained: " + droppedSpell.getName() + " (AP: " + droppedSpell.finalAPCost() + ")");
        } else {
            System.out.println("No spell dropped this time.");
        }

        if (player.getCombatMode() == Player.CombatMode.ADVANCE) {
            currentFloor++;
            System.out.println("Advancing to Floor " + currentFloor + ".");
            if (currentFloor > MAX_FLOOR) {
                System.out.println("Congratulations! You have cleared all " + MAX_FLOOR + " available floors!");
                currentGameState = GameState.GAME_OVER;
                return;
            }
        } else {
            System.out.println("Continuing to farm Floor " + currentFloor + ".");
        }
        currentGameState = GameState.STARTING_NEW_FLOOR;
    }

    private void handlePlayerLostEncounter() {
        System.out.println(player.getName() + " was defeated on Floor " + currentFloor + "...");

        if (player.getCombatMode() == Player.CombatMode.ADVANCE) {
            player.setCombatMode(Player.CombatMode.FARMING);
        }
        currentFloor = Math.max(1, currentFloor - 1);
        System.out.println("Dropped to Floor " + currentFloor + ".");
        currentGameState = GameState.STARTING_NEW_FLOOR;
    }

    private void prepareForNextEncounterAndBattle() {
        System.out.println("Preparing encounter on Floor " + currentFloor + "...");
        player.resetCombatState();

        if (!loadEnemyForCurrentFloor()) {
            System.err.println("Failed to load enemy for floor " + currentFloor + ". Pausing game.");
            currentGameState = GameState.MANAGEMENT_PAUSED;
            return;
        }
        currentEnemy.resetCombatState();

        System.out.println("Starting battle on Floor " + currentFloor + " in " + player.getCombatMode() + " mode against " + currentEnemy.getName() + "!");
        currentGameState = GameState.AUTO_BATTLING;
        this.lastUpdateTimeNanos = System.nanoTime();
        this.timeAccumulatorForSecondTick = 0;
    }

    private boolean loadEnemyForCurrentFloor() {
        currentEnemy = enemyLoader.getEnemyForFloor(currentFloor);
        if (currentEnemy == null) {
            System.err.println("GameManager: No enemy definition found for floor " + currentFloor);
            return false;
        }
        return true;
    }

    public void orderStartBattle(Player.CombatMode mode) {
        if (currentGameState != GameState.MANAGEMENT_PAUSED) {
            System.out.println("Battle can only be started from management/paused state.");
            return;
        }
        player.setCombatMode(mode);
        System.out.println("Player chose " + mode + " mode.");
        if (currentEnemy == null || (enemyLoader.getEnemyForFloor(currentFloor) != null &&
                                   !currentEnemy.getName().equals(enemyLoader.getEnemyForFloor(currentFloor).getName()))) {
            if (!loadEnemyForCurrentFloor()) {
                 System.out.println("Cannot start battle: Failed to load enemy for floor " + currentFloor);
                 return;
            }
        }
        currentGameState = GameState.STARTING_NEW_FLOOR;
    }

    public void requestPauseBattle() {
        if (currentGameState == GameState.AUTO_BATTLING) {
            currentGameState = GameState.MANAGEMENT_PAUSED;
            System.out.println("Battle paused. Entering management mode.");
            System.out.println("Current Floor: " + currentFloor + ". Enemy: " + (currentEnemy != null ? currentEnemy.getName() : "None"));
        } else {
            System.out.println("No battle in progress to pause.");
        }
    }

    public GameState getCurrentGameState() { return currentGameState; }
    public int getCurrentFloor() { return currentFloor; }
    public Enemy getCurrentEnemy() { return currentEnemy; }
    public boolean isGameRunning() { return currentGameState != GameState.GAME_OVER; }

    private void addStartingSpellsToPlayer(Player player) {
        if (player == null) {
            System.err.println("Cannot add starting spells: Player object is null.");
            return;
        }

        BaseSpellTemplate singeTemplate = findBaseSpellById("base_singe");
        BaseSpellTemplate minorHealTemplate = findBaseSpellById("base_minor_heal");
        List<PlayerSpell> startingSpells = new ArrayList<>();

        if (singeTemplate != null) {
            double singeEffectiveDmg = 4.0;
            double singeCooldown = 3.5;
            int singeAP = calculateFixedSpellAP(singeEffectiveDmg, 0);

            PlayerSpell singe = new PlayerSpell(
                singeTemplate, singeCooldown, singeEffectiveDmg,
                new ArrayList<>(), singeAP
            );
            startingSpells.add(singe);
        } else {
            System.err.println("Warning: Could not create starting spell 'Singe'.");
        }

        if (minorHealTemplate != null) {
            double minorHealEffectiveHealing = 2.0;
            double minorHealCooldown = 4.0;
            int minorHealAP = calculateFixedSpellAP(minorHealEffectiveHealing, 0);

            PlayerSpell minorHeal = new PlayerSpell(
                minorHealTemplate, minorHealCooldown, minorHealEffectiveHealing,
                new ArrayList<>(), minorHealAP
            );
            startingSpells.add(minorHeal);
        } else {
            System.err.println("Warning: Could not create starting spell 'Minor Heal'.");
        }

        int activeSlotIndex = 0;
        for (PlayerSpell spell : startingSpells) {
            player.getInventory().addSpell(spell);
            if (activeSlotIndex < Player.NUM_ACTIVE_SPELL_SLOTS) {
                int invIndex = player.getInventory().getSpellCount() - 1;
                if (invIndex >= 0) {
                    player.equipSpell(invIndex, activeSlotIndex, true);
                    activeSlotIndex++;
                }
            }
        }
    }

    /**
     * Creates predefined BaseSpellTemplate objects for the player's starting spells.
     * This method is specifically for "Singe" and "Minor Heal".
     * @param id The ID of the starting spell template to create (e.g., "base_singe").
     * @return The BaseSpellTemplate, or null if the id is not a known starting spell.
     */
    private BaseSpellTemplate findBaseSpellById(String id) {
        // This method now directly constructs the known starting spell templates.
        // It no longer relies on searching a list from SpellGenerator for these specific IDs.
        if (id == null) {
			return null;
		}

        if (id.equalsIgnoreCase("base_singe")) {
            // Fixed definition for starting "Singe"
            // CoreEffect: 3 base FIRE damage (before M_spell modifier, which won't apply to fixed starting spells)
            // CooldownRange: A representative range, actual starting spell will have a fixed CD.
            // InherentProperties: Default (none)
            return new BaseSpellTemplate(
                "base_singe", "Singe", PrimaryAttributeType.INTELLECT,
                new CoreEffectData(CoreEffectType.DAMAGE, 3, DamageType.FIRE, null, null, false, false),
                new CooldownRangeData(3.0, 4.0), // Actual starting spell will use a fixed 3.5s
                new InherentPropertiesData(false,false,false,false,0,null)
            );
        } else if (id.equalsIgnoreCase("base_minor_heal")) {
            // Fixed definition for starting "Minor Heal"
            // CoreEffect: 2 base PIETY_HEALING
            // CooldownRange: A representative range, actual starting spell will have a fixed CD.
            return new BaseSpellTemplate(
                "base_minor_heal", "Minor Heal", PrimaryAttributeType.PIETY,
                new CoreEffectData(CoreEffectType.HEALING, 2, null, HealingType.PIETY_HEALING, null, false, false),
                new CooldownRangeData(3.5, 4.5), // Actual starting spell will use a fixed 4.0s
                new InherentPropertiesData(false,false,false,false,0,null)
            );
        }

        // If the ID is not for a known starting spell, this method is not intended to find it.
        System.err.println("GameManager.findBaseSpellById: ID '" + id + "' is not a recognized starting spell. " +
                           "This method is only for predefined starting spells.");
        return null;
    }

    private int calculateFixedSpellAP(double effectiveValue, int numEnchants) {
        double apSum = SpellGenerator.GLOBAL_BASE_AP_COST;
        apSum += Math.abs(effectiveValue);
        // For starting spells with 0 enchants, multiplier is ENCHANT_COUNT_AP_MULTIPLIERS[0]
        double multiplier = SpellGenerator.ENCHANT_COUNT_AP_MULTIPLIERS[0];
        int finalAPCost = (int) Math.round(apSum * multiplier);
        return Math.max(1, finalAPCost);
    }
}