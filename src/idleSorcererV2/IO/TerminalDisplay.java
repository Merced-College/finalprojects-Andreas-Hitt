package idleSorcererV2.IO;

import java.util.List;

import idleSorcererV2.Enemy;
import idleSorcererV2.GameManager;
import idleSorcererV2.Inventory;
import idleSorcererV2.Player;
import idleSorcererV2.Stats;
import idleSorcererV2.data.PlayerSpell;
import idleSorvererV2.enums.PrimaryAttributeType;

public class TerminalDisplay {

    public TerminalDisplay() {
        // Constructor, if any setup is needed
    }

    public void showMessage(String message) {
        System.out.println(message);
    }

    public void showError(String errorMessage) {
        System.err.println("ERROR: " + errorMessage);
    }

    public void showWarning(String warningMessage) {
        System.out.println("WARNING: " + warningMessage);
    }

    public void printSeparator() {
        System.out.println("--------------------------------------------------");
    }

    public void printThickSeparator() {
        System.out.println("==================================================");
    }

    public void displayWelcomeMessage(String playerName) {
        printThickSeparator();
        showMessage("Welcome to Idle Sorcerer, " + playerName + "!");
        printThickSeparator();
    }

    public void displayHelp(List<String> helpCommands) { // Or pass pre-formatted string
        printSeparator();
        showMessage("Available Commands:");
        for (String command : helpCommands) {
            showMessage("  " + command);
        }
        printSeparator();
    }

    public void displayPrompt() {
        System.out.print("> ");
    }

    public void displayPlayerStats(Player player, GameManager gameManager) {
        if (player == null || gameManager == null) {
            showError("Cannot display player stats - player or gameManager is null.");
            return;
        }
        printThickSeparator();
        showMessage("--- PLAYER STATUS ---");
        showMessage("Name: " + player.getName());
        showMessage("Floor: " + gameManager.getCurrentFloor());
        showMessage("Combat Mode: " + player.getCombatMode());
        showMessage("HP: " + player.getCurrentHP() + " / " + player.getMaxHP());
        showMessage("Shield: " + player.getCurrentShield());
        showMessage("AP (Attribute Points): " + player.getAttributePoints());

        showMessage("\n-- Attributes --");
        Stats stats = player.getStats(); // Assuming Player has getStats()
        if (stats != null) {
            for (PrimaryAttributeType type : PrimaryAttributeType.values()) {
                if (type != PrimaryAttributeType.NONE) {
                    showMessage(String.format("  %-12s: %d", type.name(), stats.getPrimaryAttributeValue(type)));
                }
            }
        }

        showMessage("\n-- Effective Combat Stats --");
        showMessage(String.format("  %-18s: %d", "Armor:", player.getEffectiveArmor()));
        showMessage(String.format("  %-18s: %d%%", "Dodge:", player.getEffectiveDodge()));
        showMessage(String.format("  %-18s: %d%%", "Accuracy:", player.getEffectiveAccuracy()));
        showMessage(String.format("  %-18s: %d/sec", "HP Regen:", player.getEffectiveRegenPerSecond()));
        showMessage(String.format("  %-18s: %d/sec", "Poison Taken:", player.getCurrentPoisonTakenPerSecond()));

        printThickSeparator();
    }

    public void displayInventory(Inventory inventory) {
        if (inventory == null) {
            showError("Cannot display inventory - inventory is null.");
            return;
        }
        List<PlayerSpell> spells = inventory.getAllSpells();
        printSeparator();
        if (spells.isEmpty()) {
            showMessage("Inventory is empty.");
        } else {
            showMessage("--- Player Inventory (" + spells.size() + " spells) ---");
            for (int i = 0; i < spells.size(); i++) {
                PlayerSpell spell = spells.get(i);
                if (spell != null) {
                    // Basic display: Index, Name, AP Cost.
                    showMessage(String.format("%2d: %-25s (AP: %3d)", i, spell.getName(), spell.finalAPCost()));
                } else {
                    showMessage(String.format("%2d: [Empty Slot or Error]", i));
                }
            }
            showMessage("Type 'inv details <index>' for more info on a spell.");
        }
        printSeparator();
    }

    public void displaySpellDetails(PlayerSpell spell, int index) {
        if (spell == null) {
            showError("Cannot display details for null spell at index " + index);
            return;
        }
        printSeparator();
        showMessage("--- Spell Details (Inventory Index " + index + ") ---");
        showMessage(spell.getFullDescription()); // PlayerSpell.getFullDescription() does the detailed formatting
        printSeparator();
    }

    public void displayEquippedSpells(Player player) {
        if (player == null) {
			return;
		}
        printSeparator();
        showMessage("--- Equipped Spells ---");
        showMessage("-- Active Slots --");
        PlayerSpell[] activeSpells = player.getActiveSpells(); // Assuming Player has getActiveSpells()
        for (int i = 0; i < activeSpells.length; i++) {
            if (activeSpells[i] != null) {
                showMessage(String.format("  Slot %d: %s (CD: %.1fs)", i, activeSpells[i].getName(), player.getActiveSpellCooldown(activeSpells[i])));
            } else {
                showMessage(String.format("  Slot %d: [Empty]", i));
            }
        }
        showMessage("-- Passive Slots --");
        PlayerSpell[] passiveSpells = player.getPassiveSpells(); // Assuming Player has getPassiveSpells()
        for (int i = 0; i < passiveSpells.length; i++) {
            if (passiveSpells[i] != null) {
                showMessage(String.format("  Slot %d: %s", i, passiveSpells[i].getName()));
            } else {
                showMessage(String.format("  Slot %d: [Empty]", i));
            }
        }
        printSeparator();
    }


    public void displayCombatStart(Player player, Enemy enemy, int floor) {
        printThickSeparator();
        showMessage("Floor " + floor + ": " + player.getName() + " VS " + enemy.getName());
        showMessage(player.getName() + " HP: " + player.getCurrentHP() + "/" + player.getMaxHP() + " | Shield: " + player.getCurrentShield());
        showMessage(enemy.getName() + " HP: " + enemy.getCurrentHP() + "/" + enemy.getMaxHP() + " | Shield: " + enemy.getCurrentShield());
        printThickSeparator();
    }

    public void displayCombatLog(String message) {
        // Could add timestamps or differentiate player/enemy actions
        showMessage("  LOG: " + message);
    }

    public void displayCombatRoundSummary(Player player, Enemy enemy) {
        // This could be called less frequently than every action, e.g., every second.
        // Or after a burst of actions.
        printSeparator();
        showMessage(String.format("%s HP: %d/%d | Shield: %d", player.getName(), player.getCurrentHP(), player.getMaxHP(), player.getCurrentShield()));
        showMessage(String.format("%s HP: %d/%d | Shield: %d", enemy.getName(), enemy.getCurrentHP(), enemy.getMaxHP(), enemy.getCurrentShield()));
        printSeparator();
    }

    public void displayPlayerWonEncounter(String enemyName, int floor) {
        printThickSeparator();
        showMessage(enemyName + " Defeated on Floor " + floor + "!");
        printThickSeparator();
    }

    public void displayPlayerLostEncounter(int floor) {
        printThickSeparator();
        showMessage("You were defeated on Floor " + floor + "...");
        printThickSeparator();
    }

    public void displayLootGained(PlayerSpell spell) {
        if (spell != null) {
            showMessage("Loot Gained: " + spell.getName() + " (AP Value: " + spell.finalAPCost() + ")");
            showMessage("  (Type 'inv details <index>' to see its full stats)");
        } else {
            showMessage("No spell dropped this time.");
        }
    }

    public void displayFloorChange(int newFloor, String reason) {
        showMessage(reason + " Moving to Floor " + newFloor + ".");
    }

    public void displayGamePaused() {
        showMessage("Battle paused. Entering management mode.");
        showMessage("Type 'help' for commands.");
    }

    public void displayExitingMessage() {
        showMessage("Exiting game. Thanks for playing Idle Sorcerer!");
    }

    public void displayGameOver(String message) {
        printThickSeparator();
        showMessage("!!! GAME OVER !!!");
        if (message != null && !message.isEmpty()) {
            showMessage(message);
        }
        printThickSeparator();
    }
}
