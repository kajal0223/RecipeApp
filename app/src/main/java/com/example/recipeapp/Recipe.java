package com.example.recipeapp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

//Represents recipe with metadata, ingredients, instructions, and matching helpers
public class Recipe {
    private final String id;
    private final String title;
    private final String category;
    private final int prepTimeMinutes;
    private final int servings;
    private final int imageResId;
    private boolean favorite;
    private final List<String> ingredientNames;
    private final List<String> instructions;
    private final String notes;
    private String imageUri; // Optional persisted URI string from gallery picker

    //Initializes recipe fields and normalizes ingredient names and optional lists
    public Recipe(String id, String title, String category, int prepTimeMinutes,
                  int servings, int imageResId, boolean favorite,
                  List<String> ingredientNames, List<String> instructions, String notes) {
        this(id, title, category, prepTimeMinutes, servings, imageResId, "",
                favorite, ingredientNames, instructions, notes);
    }

    //Overloaded constructor supporting a persisted recipe image URI string
    public Recipe(String id, String title, String category, int prepTimeMinutes,
                  int servings, int imageResId, String imageUri, boolean favorite,
                  List<String> ingredientNames, List<String> instructions, String notes) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.prepTimeMinutes = prepTimeMinutes;
        this.servings = servings;
        this.imageResId = imageResId;
        this.imageUri = imageUri != null ? imageUri : "";
        this.favorite = favorite;
        this.ingredientNames = normalizeList(ingredientNames);
        this.instructions = instructions != null ? instructions : new ArrayList<>();
        this.notes = notes != null ? notes : "";
    }

    //Normalizes individual ingredient strings and checks for duplicate ingredient lists
    public static String normalizeIngredient(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.US);
    }

    private static List<String> normalizeList(List<String> rawItems) {
        List<String> cleaned = new ArrayList<>();
        if (rawItems == null) {
            return cleaned;
        }
        for (String item : rawItems) {
            String normalized = normalizeIngredient(item);
            if (!normalized.isEmpty() && !cleaned.contains(normalized)) {
                cleaned.add(normalized);
            }
        }
        return cleaned;
    }

    //Calculates ingredient matches and formats the match label for Search results
    public int countMatchingIngredients(Set<String> selectedIngredients) {
        if (selectedIngredients == null || selectedIngredients.isEmpty()) {
            return 0;
        }
        Set<String> normalizedSelected = new HashSet<>();
        for (String item : selectedIngredients) {
            String normalized = normalizeIngredient(item);
            if (!normalized.isEmpty()) {
                normalizedSelected.add(normalized);
            }
        }

        int matches = 0;
        for (String required : ingredientNames) {
            if (normalizedSelected.contains(required)) {
                matches++;
            }
        }
        return matches;
    }

    public String formatIngredientMatchText(Set<String> selectedIngredients) {
        int matched = countMatchingIngredients(selectedIngredients);
        int selectedCount = 0;
        if (selectedIngredients != null) {
            Set<String> normalizedSelected = new HashSet<>();
            for (String item : selectedIngredients) {
                String normalized = normalizeIngredient(item);
                if (!normalized.isEmpty()) {
                    normalizedSelected.add(normalized);
                }
            }
            selectedCount = normalizedSelected.size();
        }
        return "Uses " + matched + "/" + selectedCount + " selected ingredients";
    }

    //Checks whether recipe title matches search query
    public boolean matchesTitleQuery(String query) {
        if (query == null) {
            return true;
        }
        String trimmed = query.trim().toLowerCase(Locale.US);
        if (trimmed.isEmpty()) {
            return true;
        }
        return title != null && title.toLowerCase(Locale.US).contains(trimmed);
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public int getPrepTimeMinutes() { return prepTimeMinutes; }
    public int getServings() { return servings; }
    public int getImageResId() { return imageResId; }
    public String getImageUri() { return imageUri != null ? imageUri : ""; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri != null ? imageUri : ""; }
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    public List<String> getIngredientNames() { return ingredientNames; }
    public List<String> getInstructions() { return instructions; }
    public String getNotes() { return notes; }
}