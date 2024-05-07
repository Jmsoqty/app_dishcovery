package com.example.dishcovery;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HomeFragment extends Fragment {

    private OkHttpClient client = new OkHttpClient();
    private RecipeAdapter recipeAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Retrieve the user's email from arguments
        String email = getArguments() != null ? getArguments().getString("userEmail") : null;

        // Initialize the Floating Action Button
        FloatingActionButton fabAdd = view.findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(v -> {
            ShareRecipeDialog shareRecipeDialog = new ShareRecipeDialog();
            Bundle args = new Bundle();
            args.putString("userEmail", email);
            shareRecipeDialog.setArguments(args);
            shareRecipeDialog.show(getFragmentManager(), "ShareRecipeDialog");
        });

        // Initialize the RecyclerView and RecipeAdapter
        RecyclerView recipeRecyclerView = view.findViewById(R.id.recipe_recycler_view);
        recipeRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recipeAdapter = new RecipeAdapter(new ArrayList<>(), email);
        recipeRecyclerView.setAdapter(recipeAdapter);

        // Fetch the data
        fetchData();

        // Add a TextWatcher to the search bar for filtering recipes
        EditText searchBar = view.findViewById(R.id.search_bar);
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Filter the recipes based on the query
                recipeAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed
            }
        });

        return view;
    }

    private void fetchData() {
        String url = "http://admin.plantiq.info/api_dishcovery/fetch_recipes.php"; // API URL

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Handle error
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonData = response.body().string();
                    Gson gson = new Gson();

                    // Define the type for the list of Recipe objects
                    Type listType = new TypeToken<List<Map<String, Object>>>() {
                    }.getType();

                    Map<String, Object> responseData = gson.fromJson(jsonData, Map.class);

                    if ("success".equals(responseData.get("status"))) {
                        // Parse the JSON response and retrieve the list of recipes
                        List<Map<String, Object>> recipesData = (List<Map<String, Object>>) responseData.get("recipes");

                        List<Recipe> recipes = parseRecipes(recipesData);

                        // Update the RecipeAdapter with the new data on the UI thread
                        getActivity().runOnUiThread(() -> recipeAdapter.setRecipes(recipes));
                    }
                }
            }
        });
    }

    // Function to parse recipes data and create a list of Recipe objects
    private List<Recipe> parseRecipes(List<Map<String, Object>> recipesData) {
        List<Recipe> recipes = new ArrayList<>();

        for (Map<String, Object> recipeData : recipesData) {
            Gson gson = new Gson();
            Recipe recipe = gson.fromJson(new Gson().toJson(recipeData), Recipe.class);
            recipes.add(recipe);
        }

        return recipes;
    }


}
