// Andreas Hitt
// CPSC - 39
// Prof. Kanemoto
// May 13 2025

package idleSorcererV2;

import java.util.ArrayList;
import java.util.List;

import idleSorcererV2.data.PlayerSpell;

public class Inventory {

    private List<PlayerSpell> spells;

    // constructor
    public Inventory() {
        this.spells = new ArrayList<>();
    }

    /**
     * Adds a spell to the inventory.
     * @param spell The PlayerSpell object to add. Cannot be null.
     * @return true if the spell was added successfully, false otherwise.
     */
    public boolean addSpell(PlayerSpell spell) {
        if (spell == null) {
            System.err.println("Inventory Error: Cannot add a null spell.");
            return false;
        }
        return this.spells.add(spell);
    }

    /**
     * Removes a spell from the inventory at the specified index.
     * @param index The index of the spell to remove (0-based).
     * @return The PlayerSpell object that was removed, or null if the index was invalid.
     */
    public PlayerSpell removeSpell(int index) {
        if (index >= 0 && index < this.spells.size()) {
            return this.spells.remove(index);
        } else {
            System.err.println("Inventory Error: Invalid index " + index + " for removing spell. Inventory size: " + this.spells.size());
            return null;
        }
    }

    /**
     * Removes a specific spell object from the inventory.
     * Useful if you have the spell object itself and don't know its index.
     * @param spell The PlayerSpell object to remove.
     * @return true if the spell was found and removed, false otherwise.
     */
    public boolean removeSpell(PlayerSpell spell) {
        if (spell == null) {
            return false;
        }
        return this.spells.remove(spell);
    }


    /**
     * Retrieves a spell from the inventory at the specified index without removing it.
     * @param index The index of the spell to retrieve (0-based).
     * @return The PlayerSpell object at the given index, or null if the index is invalid.
     */
    public PlayerSpell getSpell(int index) {
        if (index >= 0 && index < this.spells.size()) {
            return this.spells.get(index);
        } else {
            System.err.println("Inventory Error: Invalid index " + index + " for getting spell. Inventory size: " + this.spells.size());
            return null;
        }
    }

    /**
     * Returns a list of all spells currently in the inventory.
     */
    public List<PlayerSpell> getAllSpells() {
        // Returning a new ArrayList to prevent external modification of the internal list structure.
        // PlayerSpell records are immutable, so sharing references to them is fine.
        return new ArrayList<>(this.spells);
    }

    /**
     * Gets the current number of spells in the inventory.
     * @return The count of spells.
     */
    public int getSpellCount() {
        return this.spells.size();
    }

    /**
     * Sorts the inventory by spell AP cost in ascending order using a recursive selection sort.
     * This method modifies the internal list of spells.
     */
    public void sortByAPCostRecursive() {
        if (this.spells == null || this.spells.size() < 2) {
            return;
        }
        selectionSortInventory(this.spells, this.spells.size());
        System.out.println("Inventory sorted by AP Cost.");
    }

    /**
     * Helper method for recursive selection sort.
     * Sorts the first n elements of the list.
     * In each step, it finds the maximum element in the unsorted part and places it at the end of that part.
     * @param list The list of PlayerSpells to sort.
     * @param n The number of elements from the beginning of the list to consider for sorting.
     */
    private void selectionSortInventory(List<PlayerSpell> list, int n) {
    	// base case
        if (n <= 1) {
            return;
        }

        // Find the index of the maximum element in the subarray list
        int indexOfMax = 0;
        for (int i = 1; i < n; i++) {
            if (list.get(i).finalAPCost() > list.get(indexOfMax).finalAPCost()) {
                indexOfMax = i;
            }
        }

        // Swap the maximum element with the element at the current end of the unsorted portion (n-1)
        if (indexOfMax != n - 1) {
            PlayerSpell temp = list.get(indexOfMax);
            list.set(indexOfMax, list.get(n - 1));
            list.set(n - 1, temp);
        }

        // Recursively call for the remaining n-1 elements (excluding the now sorted largest element)
        selectionSortInventory(list, n - 1);
    }

    // If you wanted descending order, the comparison in the loop would be:
    // if (list.get(i).finalAPCost() < list.get(minIndex).finalAPCost())
    // And you'd swap the minimum with the element at n-1.
    // The current implementation sorts in ASCENDING order (smallest AP cost first).
    // Let me re-verify the selection sort logic for ascending.
    // Standard recursive selection sort for ascending:
    // In each step, find the minimum in list[currentIndex ... n-1] and swap with list[currentIndex]
    // Then recurse for list[currentIndex+1 ... n-1]
    // The version above places the MAX at the END of the current segment. This results in ascending order.
}
