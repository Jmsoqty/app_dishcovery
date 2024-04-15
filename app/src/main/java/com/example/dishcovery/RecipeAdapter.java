package com.example.dishcovery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.ArrayList;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private List<Recipe> recipes;
    private List<Recipe> filteredRecipes;
    private String currentUserEmail; // Current user's email

    public RecipeAdapter(List<Recipe> recipes, String currentUserEmail) {
        this.recipes = recipes;
        this.filteredRecipes = new ArrayList<>(recipes);
        this.currentUserEmail = currentUserEmail;
    }

    public List<Recipe> getRecipes() {
        return filteredRecipes;
    }

    public void setRecipes(List<Recipe> recipes) {
        this.recipes = recipes;
        this.filteredRecipes = new ArrayList<>(recipes);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recipe_item, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = filteredRecipes.get(position);
        holder.bind(recipe);
    }

    @Override
    public int getItemCount() {
        return filteredRecipes != null ? filteredRecipes.size() : 0;
    }

    // Add the filter method
    public void filter(String query) {
        List<Recipe> filteredList = new ArrayList<>();

        // Loop through all recipes and filter based on query
        for (Recipe recipe : recipes) {
            if (recipe.getRecipeName().toLowerCase().contains(query.toLowerCase()) ||
                    recipe.getCategory().toLowerCase().contains(query.toLowerCase()) ||
                    recipe.getPostedByName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(recipe);
            }
        }

        // Update the filtered recipes list and notify the adapter
        filteredRecipes.clear();
        filteredRecipes.addAll(filteredList);
        notifyDataSetChanged();
    }

    // ViewHolder class to bind recipe data to the layout
    class RecipeViewHolder extends RecyclerView.ViewHolder {
        private final TextView usernameText;
        private final TextView postDateText;
        private final TextView recipeNameText;
        private final TextView ingredientsText;
        private final TextView instructionsText;
        private final ImageView dishImageView;
        private final ImageView profilePictureView;
        private final ImageButton deleteRecipeButton;
        private final ImageButton bookmarkButton;
        private final ImageButton sendFundsButton;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.username_text);
            postDateText = itemView.findViewById(R.id.post_date_text);
            recipeNameText = itemView.findViewById(R.id.recipe_name_text);
            ingredientsText = itemView.findViewById(R.id.ingredients_text);
            instructionsText = itemView.findViewById(R.id.instructions_text);
            dishImageView = itemView.findViewById(R.id.dish_image_view);
            profilePictureView = itemView.findViewById(R.id.profile_picture);
            deleteRecipeButton = itemView.findViewById(R.id.delete_recipe_button);
            bookmarkButton = itemView.findViewById(R.id.bookmark_button);
            sendFundsButton = itemView.findViewById(R.id.send_funds_button);
        }

        // Bind the recipe data to the view
        public void bind(Recipe recipe) {
            // Bind username, post date, and recipe name
            usernameText.setText(recipe.getPostedByName());
            postDateText.setText(recipe.getFormattedDateUpdated());
            recipeNameText.setText(recipe.getRecipeName());

            // Parse and format ingredients
            List<String> ingredients = recipe.getFormattedIngredients();
            if (ingredients != null) {
                String formattedIngredients = String.join("\n", ingredients);
                ingredientsText.setText(formattedIngredients);
            } else {
                ingredientsText.setText(""); // Handle case when ingredients are null
            }

            // Parse and format instructions
            List<String> instructions = recipe.getFormattedInstructions();
            if (instructions != null) {
                String formattedInstructions = String.join("\n", instructions);
                instructionsText.setText(formattedInstructions);
            } else {
                instructionsText.setText(""); // Handle case when instructions are null
            }

            // Decode and set the dish image
            String dishImageString = recipe.getImage();
            if (dishImageString != null && !dishImageString.isEmpty()) {
                byte[] decodedDishImage = Base64.decode(dishImageString, Base64.DEFAULT);
                Bitmap dishBitmap = BitmapFactory.decodeByteArray(decodedDishImage, 0, decodedDishImage.length);
                dishImageView.setImageBitmap(dishBitmap);
            } else {
                dishImageView.setImageDrawable(null); // Handle case when dish image is null or empty
            }

            // Decode and set the profile picture
            String profileImageString = recipe.getPostedByImage();
            if (profileImageString != null && !profileImageString.isEmpty()) {
                byte[] decodedProfileImage = Base64.decode(profileImageString, Base64.DEFAULT);
                Bitmap profileBitmap = BitmapFactory.decodeByteArray(decodedProfileImage, 0, decodedProfileImage.length);
                profilePictureView.setImageBitmap(profileBitmap);
            } else {
                profilePictureView.setImageDrawable(null); // Handle case when profile image is null or empty
            }

            // Set visibility of buttons based on user and recipe poster's email
            if (currentUserEmail != null && currentUserEmail.equals(recipe.getPostedBy())) {
                // Show the delete button and hide the bookmark and send funds buttons
                deleteRecipeButton.setVisibility(View.VISIBLE);
                bookmarkButton.setVisibility(View.GONE);
                sendFundsButton.setVisibility(View.GONE);
            } else {
                // Hide the delete button and show the bookmark and send funds buttons
                deleteRecipeButton.setVisibility(View.GONE);
                bookmarkButton.setVisibility(View.VISIBLE);
                sendFundsButton.setVisibility(View.VISIBLE);
            }
        }
    }
}
