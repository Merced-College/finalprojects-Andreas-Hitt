package idleSorcererV2.IO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import idleSorcererV2.GameManager;
import idleSorcererV2.Player;
import idleSorcererV2.data.PlayerSpell;
import idleSorvererV2.enums.GameState;
import idleSorvererV2.enums.PrimaryAttributeType;


public class InputHandler {
    private Scanner scanner;
    private GameManager gameManager;
    private Player player;
    private TerminalDisplay display; // Added TerminalDisplay

    public InputHandler(GameManager gameManager, Player player, TerminalDisplay display) {
        this.scanner = new Scanner(System.in);
        this.gameManager = gameManager;
        this.player = player;
        this.display = display; // Initialize TerminalDisplay
    }

    /**
     * Processes player input when the game is in a state that expects commands.
     * @return true if the game should continue processing input, false if 'quit' command was issued.
     */
    public boolean processPlayerInput() {
        if (gameManager.getCurrentGameState() == GameState.MANAGEMENT_PAUSED) {
            display.displayPrompt(); // "> "
            String inputLine = scanner.nextLine().trim().toLowerCase();

            if (inputLine.isEmpty()) {
                return true; // Continue processing
            }

            String[] parts = inputLine.split("\\s+");
            String command = parts[0];
            String[] args = new String[parts.length - 1];
            System.arraycopy(parts, 1, args, 0, parts.length - 1);

            try {
                switch (command) {
                    case "help":
                        displayHelp();
                        break;
                    case "battle":
                        handleBattleCommand(args);
                        break;
                    case "stop":
                        gameManager.requestPauseBattle(); // GameManager will print messages via its own System.out for now
                        break;
                    case "stats":
                        display.displayPlayerStats(player, gameManager);
                        break;
                    case "inventory":
                    case "inv":
                        handleInventoryCommand(args);
                        break;
                    case "equip":
                        handleEquipCommand(args);
                        break;
                    case "unequip":
                        handleUnequipCommand(args);
                        break;
                    case "deconstruct":
                    case "dec":
                        handleDeconstructCommand(args);
                        break;
                    case "upgrade":
                    case "upg":
                        handleUpgradeCommand(args);
                        break;
                    case "floor":
                        display.showMessage("Current Floor: " + gameManager.getCurrentFloor());
                        break;
                    case "sort":
                        handleSortCommand(args);
                        break;
                    case "quit":
                    case "exit":
                        display.displayExitingMessage();
                        return false; // Signal to quit the game loop
                    default:
                        display.showError("Unknown command: '" + command + "'. Type 'help' for a list of commands.");
                        break;
                }
            } catch (Exception e) {
                display.showError("Error processing command '" + inputLine + "': " + e.getMessage());
                // e.printStackTrace(); // For debugging
            }
        } else if (gameManager.getCurrentGameState() == GameState.AUTO_BATTLING) {
            // Non-blocking input check for 'stop' is still tricky here.
            // The main game loop in Main.java can attempt a basic check.
        }
        return true; // Continue processing
    }

    private void displayHelp() {
        List<String> helpLines = new ArrayList<>(Arrays.asList(
            "battle <advance|farm>              - Start battling in specified mode.",
            "stop                               - Pause current battle and return to menu.",
            "stats                              - View your current stats and attributes.",
            "inventory (or inv)                 - View your spell inventory.",
            "inventory details <index>          - View details of a spell at inventory index.",
            "equip <inv_idx> <active|passive> <slot_idx> - Equip spell from inventory.",
            "unequip <active|passive> <slot_idx> - Unequip spell from slot.",
            "deconstruct <inv_idx> (or dec)     - Deconstruct spell at inventory index for AP.",
            "upgrade <attribute> (or upg)       - Upgrade a primary attribute.",
            "floor                              - Show current floor number.",
            "quit (or exit)                     - Exit the game."
        ));
        display.displayHelp(helpLines);
    }

    private void handleBattleCommand(String[] args) {
        if (args.length < 1) {
            display.showMessage("Usage: battle <advance|farm>");
            return;
        }
        String modeStr = args[0];
        if (modeStr.equals("advance")) {
            gameManager.orderStartBattle(Player.CombatMode.ADVANCE);
        } else if (modeStr.equals("farm")) {
            gameManager.orderStartBattle(Player.CombatMode.FARMING);
        } else {
            display.showError("Invalid battle mode. Use 'advance' or 'farm'.");
        }
    }

    private void handleInventoryCommand(String[] args) {
        if (args.length > 0 && args[0].equals("details")) {
            if (args.length > 1) {
                try {
                    int index = Integer.parseInt(args[1]);
                    PlayerSpell spell = player.getInventory().getSpell(index);
                    if (spell != null) {
                        display.displaySpellDetails(spell, index);
                    } else {
                        display.showError("No spell found at inventory index " + index + ".");
                    }
                } catch (NumberFormatException e) {
                    display.showError("Invalid index for 'inventory details'. Please use a number.");
                }
            } else {
                display.showMessage("Usage: inventory details <index>");
            }
            return;
        }
        display.displayInventory(player.getInventory());
        display.displayEquippedSpells(player); // Also show equipped for context
    }


    private void handleEquipCommand(String[] args) {
        if (args.length < 3) {
            display.showMessage("Usage: equip <inventory_index> <active|passive> <slot_index>");
            display.showMessage("Example: equip 0 active 0");
            return;
        }
        try {
            int inventoryIndex = Integer.parseInt(args[0]);
            String slotTypeStr = args[1].toLowerCase();
            int slotIndex = Integer.parseInt(args[2]);

            boolean isActiveSlot;
            if (slotTypeStr.equals("active")) {
                isActiveSlot = true;
            } else if (slotTypeStr.equals("passive")) {
                isActiveSlot = false;
            } else {
                display.showError("Invalid slot type. Use 'active' or 'passive'.");
                return;
            }
            // Player.equipSpell should print its own success/failure via System.out for now
            // Or ideally, return a status that InputHandler then passes to display
            if (!player.equipSpell(inventoryIndex, slotIndex, isActiveSlot)) {
                 display.showError("Failed to equip spell. (Check Player class logs for details)");
            } else {
                display.showMessage("Spell equipped. (Check Player class logs for details)");
                display.displayEquippedSpells(player); // Show updated equipped spells
            }
        } catch (NumberFormatException e) {
            display.showError("Invalid number for index or slot index.");
        }
    }

    private void handleUnequipCommand(String[] args) {
        if (args.length < 2) {
            display.showMessage("Usage: unequip <active|passive> <slot_index>");
            display.showMessage("Example: unequip active 0");
            return;
        }
        try {
            String slotTypeStr = args[0].toLowerCase();
            int slotIndex = Integer.parseInt(args[1]);
            boolean isActiveSlot;
            if (slotTypeStr.equals("active")) {
				isActiveSlot = true;
			} else if (slotTypeStr.equals("passive")) {
				isActiveSlot = false;
			} else {
                display.showError("Invalid slot type. Use 'active' or 'passive'.");
                return;
            }
            if (!player.unequipSpell(slotIndex, isActiveSlot)) {
                display.showError("Failed to unequip spell. (Check Player class logs for details)");
            } else {
                 display.showMessage("Spell unequipped. (Check Player class logs for details)");
                 display.displayEquippedSpells(player);
            }
        } catch (NumberFormatException e) {
            display.showError("Invalid number for slot index.");
        }
    }

    private void handleDeconstructCommand(String[] args) {
        if (args.length < 1) {
            display.showMessage("Usage: deconstruct <inventory_index>");
            return;
        }
        try {
            int inventoryIndex = Integer.parseInt(args[0]);
            // Player.deconstructSpell prints its own messages for now
            if (!player.deconstructSpell(inventoryIndex)){
                 display.showError("Failed to deconstruct. (Check Player class logs for details)");
            } else {
                 display.showMessage("Deconstruction attempt made. (Check Player class logs for details)");
            }
        } catch (NumberFormatException e) {
            display.showError("Invalid inventory index. Please use a number.");
        }
    }

    private void handleUpgradeCommand(String[] args) {
        if (args.length < 1) {
            display.showMessage("Usage: upgrade <attribute_name>");
            display.showMessage("Attributes: agility, charm, cunning, dexterity, fortitude, intellect, malice, mind, piety, wisdom");
            return;
        }
        String attributeName = args[0].toLowerCase();
        try {
            PrimaryAttributeType attributeToUpgrade = PrimaryAttributeType.valueOf(attributeName.toUpperCase());
            // Player.spendAPForAttribute prints its own messages
            if(!player.spendAPForAttribute(attributeToUpgrade)){
                // Error message likely already printed by player method
            } else {
                 display.displayPlayerStats(player, gameManager); // Show updated stats
            }
        } catch (IllegalArgumentException e) {
            display.showError("Invalid attribute name: '" + attributeName + "'. Type 'upgrade' for list.");
        }
    }

    private void handleSortCommand(String[] args) {
        if (args.length < 2) {
            display.showMessage("Usage: sort inventory <criteria>");
            display.showMessage("Example: sort inventory ap");
            return;
        }

        String target = args[0].toLowerCase();
        String criteria = args[1].toLowerCase();

        if (target.equals("inventory") || target.equals("inv")) {
            if (criteria.equals("ap")) {
                player.getInventory().sortByAPCostRecursive(); // This method prints its own confirmation
                display.displayInventory(player.getInventory()); // Display the sorted inventory
            } else {
                display.showError("Unknown sort criteria for inventory: '" + criteria + "'. Try 'ap'.");
            }
        } else {
            display.showError("Cannot sort '" + target + "'. Only 'inventory' sorting is supported.");
        }
    }

    // Basic attempt for non-blocking "stop" check.
    // For a real terminal app, a library or more complex threading would be better.
    public void checkForStopCommandDuringBattle() {
        try {
            if (System.in.available() > 0) {
                if (scanner.hasNextLine()){ // Check if there is actually a line to prevent blocking
                    String input = scanner.nextLine().trim().toLowerCase();
                    if (input.equals("stop")) {
                        gameManager.requestPauseBattle();
                    } else {
                        display.showMessage("(Input '" + input + "' ignored during battle. Type 'stop' to pause.)");
                    }
                }
            }
        } catch (Exception e) {
            // Silently ignore exceptions here to prevent crashing the game loop
            // System.err.println("InputHandler: Error checking for stop command: " + e.getMessage());
        }
    }
}
