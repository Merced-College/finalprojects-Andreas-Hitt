// Andreas Hitt
// CPSC - 39
// Prof. Kanemoto
// May 13 2025

package idleSorcererV2;

import java.util.List;

import idleSorcererV2.IO.EnemyLoader;
import idleSorcererV2.IO.InputHandler;
import idleSorcererV2.IO.TerminalDisplay;
import idleSorcererV2.data.BaseEnchant;
import idleSorcererV2.data.BaseSpellTemplate;
import idleSorvererV2.enums.GameState;

public class Main {

    public static void main(String[] args) {
        // --- 1. Initialization ---
        TerminalDisplay display = new TerminalDisplay();
        display.showMessage("Starting Idle Sorcerer V2...");

        // Load game data
        List<BaseSpellTemplate> baseSpellTemplates = BaseSpellTemplate.loadBaseSpellTemplatesFromFile("/player_base_spells.txt");
        List<BaseEnchant> baseEnchants = BaseEnchant.loadBaseEnchantsFromFile("/base_enchants.txt");

        if (baseSpellTemplates.isEmpty()) {
            display.showError("CRITICAL: No base spell templates loaded. Exiting.");
            return;
        }
        // It's okay if baseEnchants is empty, spells will just have no enchants.
        if (baseEnchants.isEmpty()) {
            display.showWarning("No base enchants loaded. Spells will not have random enchantments.");
        }

        EnemyLoader enemyLoader = new EnemyLoader(); // Loads enemies from "/enemies.txt" in its constructor
        if (enemyLoader.getNumberOfEnemyTypesLoaded() == 0) {
            display.showError("CRITICAL: No enemies loaded. Exiting.");
            return;
        }

        Player player = new Player("Sorcerer"); // Default player name
        SpellGenerator spellGenerator = new SpellGenerator(baseSpellTemplates, baseEnchants);
        GameManager gameManager = new GameManager(player, enemyLoader, spellGenerator);
        InputHandler inputHandler = new InputHandler(gameManager, player, display); // Pass display

        // Initialize game (sets initial state to MANAGEMENT_PAUSED)
        gameManager.initializeGame(); // This will print initial welcome messages

        display.displayWelcomeMessage(player.getName());
        // Display initial help or prompt after GameManager's own initialization messages
        if (gameManager.getCurrentGameState() == GameState.MANAGEMENT_PAUSED) {
             // GameManager.initializeGame() already prints initial prompts
        }


        // --- 2. Main Game Loop ---
        boolean keepPlaying = true;

        while (keepPlaying && gameManager.isGameRunning()) {
            GameState currentState = gameManager.getCurrentGameState();

            if (currentState == GameState.MANAGEMENT_PAUSED) {
                keepPlaying = inputHandler.processPlayerInput(); // processPlayerInput returns false on "quit"
            } else if (currentState == GameState.AUTO_BATTLING) {
                gameManager.update(); // Process one tick of combat
                inputHandler.checkForStopCommandDuringBattle(); // Attempt to check for "stop"

                // Periodically display combat status during auto-battle for visibility
                //if (System.currentTimeMillis() - lastDisplayTime > 2000) { // Every 2 seconds
                //    display.printSeparator();
                //    display.showMessage("--- Combat Update ---");
                //     display.displayCombatRoundSummary(player, gameManager.getCurrentEnemy());
                //    lastDisplayTime = System.currentTimeMillis();
                //}

                // Control game speed for terminal readability
                try {
                    Thread.sleep(100); // e.g., 10 updates per second for combat logic
                } catch (InterruptedException e) {
                    display.showError("Game loop interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt(); // Restore interruption status
                    keepPlaying = false;
                }
            } else if (currentState == GameState.INITIALIZING) {
                // This state should be brief. If stuck here, something is wrong.
                display.showMessage("Game is still initializing...");
                try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            else { // For states like PLAYER_WON_ENCOUNTER, PLAYER_LOST_ENCOUNTER, STARTING_NEW_FLOOR
                gameManager.update(); // Allow GameManager to transition through these states
            }

            if (currentState == GameState.GAME_OVER) {
                display.displayGameOver("The adventure ends here."); // GameManager might have more specific message
                keepPlaying = false;
            }
        }

        display.showMessage("Thank you for playing Idle Sorcerer!");
    }
}
