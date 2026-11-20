package com.example.recipeapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

//Fragment displaying 2-column folder cards, folder recipe lists, and folder CRUD actions
public class SavedFragment extends Fragment {

    private View layoutFoldersView;
    private GridLayout gridFolders;
    private TextView tvFolderHint;
    private TextView tvEmptyFolders;
    private MaterialButton btnAddFolder;

    private View layoutFolderDetailView;
    private TextView tvSelectedFolderTitle;
    private TextView tvFolderEmpty;
    private RecyclerView rvFolderRecipes;
    private FolderRecipesAdapter folderRecipesAdapter;

    @Nullable
    private String selectedFolderId = null;

    private RecipeRepository repository;

    //Inflates layout and binds Saved screen controls
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saved, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = RecipeRepository.getInstance();
        repository.init(requireContext());

        layoutFoldersView = view.findViewById(R.id.layout_folders_view);
        gridFolders = view.findViewById(R.id.grid_folders);
        tvFolderHint = view.findViewById(R.id.tv_folder_hint);
        tvEmptyFolders = view.findViewById(R.id.tv_empty_folders);
        btnAddFolder = view.findViewById(R.id.btn_add_folder);

        layoutFolderDetailView = view.findViewById(R.id.layout_folder_detail_view);
        tvSelectedFolderTitle = view.findViewById(R.id.tv_selected_folder_title);
        tvFolderEmpty = view.findViewById(R.id.tv_folder_empty);
        rvFolderRecipes = view.findViewById(R.id.rv_folder_recipes);

        btnAddFolder.setOnClickListener(v -> showCreateFolderDialog());

        ImageButton btnBackToFolders = view.findViewById(R.id.btn_back_to_folders);
        if (btnBackToFolders != null) {
            btnBackToFolders.setOnClickListener(v -> showFoldersGrid());
        }

        rvFolderRecipes.setLayoutManager(new LinearLayoutManager(requireContext()));
        folderRecipesAdapter = new FolderRecipesAdapter(new FolderRecipesAdapter.OnFolderRecipeListener() {
            @Override
            public void onRecipeClick(Recipe recipe) {
                Toast.makeText(requireContext(), recipe.getTitle(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFavoriteClick(Recipe recipe) {
                repository.toggleFavorite(recipe);
                refreshAll();
            }
        });
        rvFolderRecipes.setAdapter(folderRecipesAdapter);

        refreshAll();
    }

    //Refreshes folder cards and open folder recipes when returning to Saved Recipes
    @Override
    public void onResume() {
        super.onResume();
        if (repository != null) {
            refreshAll();
        }
    }

    private void refreshAll() {
        refreshFolders();
        if (selectedFolderId != null) {
            renderSelectedFolder();
        } else {
            showFoldersGrid();
        }
    }

    private void openFolder(Folder folder) {
        if (folder == null) {
            return;
        }
        selectedFolderId = folder.getId();
        renderSelectedFolder();
    }

    private void showFoldersGrid() {
        selectedFolderId = null;
        if (layoutFolderDetailView != null) {
            layoutFolderDetailView.setVisibility(View.GONE);
        }
        if (layoutFoldersView != null) {
            layoutFoldersView.setVisibility(View.VISIBLE);
        }
    }

    @Nullable
    private Folder findSelectedFolder() {
        if (selectedFolderId == null || repository == null) {
            return null;
        }
        for (Folder folder : repository.getFolders()) {
            if (selectedFolderId.equals(folder.getId())) {
                return folder;
            }
        }
        return null;
    }

    private void renderSelectedFolder() {
        Folder activeFolder = findSelectedFolder();
        if (activeFolder == null) {
            showFoldersGrid();
            return;
        }

        if (layoutFoldersView != null) {
            layoutFoldersView.setVisibility(View.GONE);
        }
        if (layoutFolderDetailView != null) {
            layoutFolderDetailView.setVisibility(View.VISIBLE);
        }

        List<Recipe> folderRecipes = repository.getRecipesInFolder(activeFolder);
        int count = folderRecipes.size();
        String unit = count == 1 ? "recipe" : "recipes";
        if (tvSelectedFolderTitle != null) {
            tvSelectedFolderTitle.setText(activeFolder.getName() + " (" + count + " " + unit + ")");
        }

        folderRecipesAdapter.submitList(folderRecipes);
        if (tvFolderEmpty != null) {
            tvFolderEmpty.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        }
        rvFolderRecipes.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
    }

    //Prompts for folder name and creates it if non-blank and unique
    private void showCreateFolderDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("e.g., Quick Weeknight, Desserts");

        new AlertDialog.Builder(requireContext())
                .setTitle("New Folder")
                .setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String rawName = input.getText().toString();
                    Folder created = repository.createFolder(rawName);
                    if (created == null) {
                        Toast.makeText(
                                requireContext(),
                                "Enter a unique folder name",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }
                    refreshFolders();
                    Toast.makeText(
                            requireContext(),
                            "Created \"" + created.getName() + "\"",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    //Shows options to rename or delete a folder on long-press
    private void showFolderOptionsDialog(Folder folder) {
        String[] options = {"Rename Folder", "Delete Folder"};

        new AlertDialog.Builder(requireContext())
                .setTitle(folder.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showRenameFolderDialog(folder);
                    } else if (which == 1) {
                        showDeleteFolderDialog(folder);
                    }
                })
                .show();
    }

    //Prompts for a new folder name and applies it if valid and unique
    private void showRenameFolderDialog(Folder folder) {
        EditText input = new EditText(requireContext());
        input.setText(folder.getName());
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(requireContext())
                .setTitle("Rename Folder")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    boolean renamed = repository.renameFolder(folder, input.getText().toString());
                    if (!renamed) {
                        Toast.makeText(
                                requireContext(),
                                "Enter a unique folder name",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }
                    refreshFolders();
                    Toast.makeText(
                            requireContext(),
                            "Renamed to \"" + folder.getName() + "\"",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    //Confirms folder deletion while keeping saved recipes intact
    private void showDeleteFolderDialog(Folder folder) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Folder")
                .setMessage("Delete \"" + folder.getName() + "\"? Saved recipes themselves won't be deleted.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (folder.getId().equals(selectedFolderId)) {
                        selectedFolderId = null;
                    }
                    repository.deleteFolder(folder);
                    refreshAll();
                    Toast.makeText(
                            requireContext(),
                            "Deleted \"" + folder.getName() + "\"",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    //Rebuilds 2-column folder grid and toggles empty-state visibility
    private void refreshFolders() {
        List<Folder> folders = repository.getFolders();
        gridFolders.removeAllViews();

        if (folders.isEmpty()) {
            gridFolders.setVisibility(View.GONE);
            tvFolderHint.setVisibility(View.GONE);
            tvEmptyFolders.setVisibility(View.VISIBLE);
            return;
        }

        gridFolders.setVisibility(View.VISIBLE);
        tvFolderHint.setVisibility(View.VISIBLE);
        tvEmptyFolders.setVisibility(View.GONE);
        gridFolders.setColumnCount(1);

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int gapPx = dpToPx(12);
        int cardHeightPx = dpToPx(136);

        for (int i = 0; i < folders.size(); i += 2) {
            int rowIndex = i / 2;

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setWeightSum(2f);

            GridLayout.LayoutParams rowParams = new GridLayout.LayoutParams(
                    GridLayout.spec(rowIndex),
                    GridLayout.spec(0, GridLayout.FILL, 1f)
            );
            rowParams.width = GridLayout.LayoutParams.MATCH_PARENT;
            rowParams.height = cardHeightPx;
            rowParams.bottomMargin = gapPx;
            row.setLayoutParams(rowParams);

            //Left card (always present)
            View leftCard = createFolderCard(inflater, row, folders.get(i), cardHeightPx);
            LinearLayout.LayoutParams leftParams =
                    new LinearLayout.LayoutParams(0, cardHeightPx, 1f);
            leftParams.setMarginEnd(gapPx / 2);
            leftCard.setLayoutParams(leftParams);
            row.addView(leftCard);

            //Right card (if a second folder exists in this row)
            if (i + 1 < folders.size()) {
                View rightCard = createFolderCard(inflater, row, folders.get(i + 1), cardHeightPx);
                LinearLayout.LayoutParams rightParams =
                        new LinearLayout.LayoutParams(0, cardHeightPx, 1f);
                rightParams.setMarginStart(gapPx / 2);
                rightCard.setLayoutParams(rightParams);
                row.addView(rightCard);
            } else {
                View spacer = new View(requireContext());
                spacer.setLayoutParams(new LinearLayout.LayoutParams(0, cardHeightPx, 1f));
                row.addView(spacer);
            }

            gridFolders.addView(row);
        }

        gridFolders.requestLayout();
    }

    private View createFolderCard(LayoutInflater inflater, ViewGroup parent, Folder folder, int heightPx) {
        View cardView = inflater.inflate(R.layout.item_folder_grid, parent, false);
        cardView.setMinimumHeight(heightPx);

        TextView tvFolderName = cardView.findViewById(R.id.tv_folder_name);
        TextView tvFolderCount = cardView.findViewById(R.id.tv_folder_count);

        tvFolderName.setText(folder.getName());
        tvFolderCount.setText(folder.getRecipeCountLabel());

        cardView.setOnClickListener(v -> openFolder(folder));

        cardView.setOnLongClickListener(v -> {
            showFolderOptionsDialog(folder);
            return true;
        });

        return cardView;
    }

    //Converts dp values to pixels for grid spacing
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}