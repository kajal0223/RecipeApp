package com.example.recipeapp;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

//Represents user-created folder used to organize saved recipes
public class Folder {
    private final String id;
    private String name;
    private final List<String> recipeIds;

    //Initializes folder with trimmed name and checks for duplicated recipe IDs
    public Folder(String id, String name, List<String> recipeIds) {
        this.id = id;
        this.name = name != null ? name.trim() : "";
        this.recipeIds = new ArrayList<>();
        if (recipeIds != null) {
            for (String recipeId : recipeIds) {
                addRecipeId(recipeId);
            }
        }
    }

    //Manages recipe IDs stored inside this folder
    public boolean addRecipeId(String recipeId) {
        if (recipeId == null || recipeId.trim().isEmpty()) {
            return false;
        }
        String cleanId = recipeId.trim();
        if (!recipeIds.contains(cleanId)) {
            recipeIds.add(cleanId);
            return true;
        }
        return false;
    }

    public boolean removeRecipeId(String recipeId) {
        return recipeIds.remove(recipeId);
    }

    //Returns recipe counts and formatted labels for UI
    public int getRecipeCount() {
        return recipeIds.size();
    }

    public String getRecipeCountLabel() {
        int count = getRecipeCount();
        return count + (count == 1 ? " recipe" : " recipes");
    }

    //Validates folder names against blanks and case-insensitive duplicates
    public static boolean isValidFolderName(String candidate, List<Folder> existingFolders) {
        if (candidate == null || candidate.trim().isEmpty()) {
            return false;
        }
        String normalizedCandidate = candidate.trim().toLowerCase(Locale.US);
        if (existingFolders != null) {
            for (Folder folder : existingFolders) {
                if (folder.getName().toLowerCase(Locale.US).equals(normalizedCandidate)) {
                    return false;
                }
            }
        }
        return true;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name.trim();
        }
    }
    public List<String> getRecipeIds() { return recipeIds; }
}
