package idleSorvererV2.enums;

public enum GameState {
    INITIALIZING,         // Game is setting up, loading resources
    MANAGEMENT_PAUSED,    // Player is in menus (inventory, stats, etc.), combat is paused or not active
    STARTING_NEW_FLOOR,   // Preparing to start combat on a new/current floor
    AUTO_BATTLING,        // Player and enemy are actively fighting
    PLAYER_WON_ENCOUNTER, // Player defeated the current enemy instance
    PLAYER_LOST_ENCOUNTER,// Player was defeated by the current enemy instance
    GAME_OVER             // Potentially if player loses on floor 1 in farming mode (or other end condition)
}