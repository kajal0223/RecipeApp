package com.example.recipeapp;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

//Screen for viewing, adding, editing, and deleting user recipes under Profile
public class MyRecipesFragment extends Fragment {

    private RecipeRepository repository;
    private MyRecipesAdapter adapter;
    private TextView tvCount;
    private TextView tvEmpty;

    //Launches image picker and persists chosen photos into app internal storage
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null || !isAdded()) {
                    return;
                }
                String copiedUri = copyImageToInternalStorage(uri);
                if (copiedUri != null) {
                    AddRecipeDialog.deliverPickedImage(copiedUri);
                } else {
                    Toast.makeText(requireContext(), "Could not load selected image",
                            Toast.LENGTH_SHORT).show();
                }
            });

    //Fragment lifecycle and UI initialization
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_recipes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = RecipeRepository.getInstance();
        repository.init(requireContext());

        tvCount = view.findViewById(R.id.tv_my_recipes_count);
        tvEmpty = view.findViewById(R.id.tv_empty_my_recipes);

        ImageButton btnBack = view.findViewById(R.id.btn_back_my_recipes);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (isAdded()) {
                    getParentFragmentManager().popBackStack();
                }
            });
        }

        MaterialButton btnAdd = view.findViewById(R.id.btn_add_my_recipe);
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v ->
                    AddRecipeDialog.show(
                            requireContext(),
                            repository,
                            null,
                            callback -> pickImageLauncher.launch("image/*"),
                            saved -> refreshList()
                    )
            );
        }

        RecyclerView rvRecipes = view.findViewById(R.id.rv_my_recipes);
        rvRecipes.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MyRecipesAdapter(new MyRecipesAdapter.OnRecipeActionListener() {
            @Override
            public void onEditRecipe(Recipe recipe) {
                AddRecipeDialog.show(
                        requireContext(),
                        repository,
                        recipe,
                        callback -> pickImageLauncher.launch("image/*"),
                        saved -> refreshList()
                );
            }

            @Override
            public void onDeleteRecipe(Recipe recipe) {
                confirmDelete(recipe);
            }
        });
        rvRecipes.setAdapter(adapter);

        refreshList();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();
    }

    //Storage, deletion, and list refresh helpers
    @Nullable
    private String copyImageToInternalStorage(Uri sourceUri) {
        if (!isAdded()) {
            return null;
        }
        File outFile = new File(requireContext().getFilesDir(), "recipe_" + UUID.randomUUID() + ".jpg");
        try (InputStream in = requireContext().getContentResolver().openInputStream(sourceUri);
             OutputStream out = new FileOutputStream(outFile)) {
            if (in == null) {
                return null;
            }
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
            return Uri.fromFile(outFile).toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void confirmDelete(Recipe recipe) {
        if (recipe == null || !isAdded()) {
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Recipe")
                .setMessage("Remove \"" + recipe.getTitle() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (repository.deleteRecipe(recipe.getId())) {
                        Toast.makeText(requireContext(), "Deleted \"" + recipe.getTitle() + "\"",
                                Toast.LENGTH_SHORT).show();
                        refreshList();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refreshList() {
        if (repository == null || adapter == null) {
            return;
        }
        List<Recipe> allRecipes = repository.getAllRecipes();
        adapter.submitList(allRecipes);

        int count = allRecipes.size();
        if (tvCount != null) {
            String word = count == 1 ? "Recipe" : "Recipes";
            tvCount.setText(count + " " + word);
        }
        if (tvEmpty != null) {
            tvEmpty.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        }
    }
}
