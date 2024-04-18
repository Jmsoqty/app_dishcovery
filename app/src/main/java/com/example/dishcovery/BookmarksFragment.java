package com.example.dishcovery;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BookmarksFragment extends Fragment {

    private OkHttpClient client = new OkHttpClient();
    private RecipeAdapter recipeAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bookmarks, container, false);

        // Retrieve the user's email from arguments
        String email = getArguments() != null ? getArguments().getString("userEmail") : null;

        // Initialize the RecyclerView and RecipeAdapter
        RecyclerView recipeRecyclerView = view.findViewById(R.id.recipe_recycler_view);
        recipeRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recipeAdapter = new RecipeAdapter(new ArrayList<>(), email);
        recipeRecyclerView.setAdapter(recipeAdapter);

        // Fetch the data
        fetchData(email);

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

    private void fetchData(String email) {
        // Ensure email is not null or empty
        if (email == null || email.isEmpty()) {
            Log.e("fetchData", "Email is null or empty");
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "User email not provided. Unable to fetch data.", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        // API URL for the POST request
        String url = "http://192.168.1.15/dishcovery/api/fetch_bookmarks.php";

        // Create the request body with the email parameter
        RequestBody requestBody = new FormBody.Builder()
                .add("email", email)
                .build();

        // Create a POST request with the URL and request body
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        // Enqueue the request for asynchronous execution
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Log the error and handle the failure
                Log.e("fetchData", "API request failed", e);
                getActivity().runOnUiThread(() -> {
                    // Show a user-friendly error message
                    Toast.makeText(getContext(), "Failed to fetch data. Please try again.", Toast.LENGTH_SHORT).show();
                    ImageView placeholderImage = getView().findViewById(R.id.placeholder_image);
                    placeholderImage.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonData = response.body().string();
                    Gson gson = new Gson();

                    // Define the type for the response data
                    Type responseType = new TypeToken<Map<String, Object>>() {}.getType();
                    Map<String, Object> responseData = gson.fromJson(jsonData, responseType);

                    if ("success".equals(responseData.get("status"))) {
                        // Parse the JSON response and retrieve the list of recipes
                        List<Map<String, Object>> recipesData = (List<Map<String, Object>>) responseData.get("recipes");
                        List<Recipe> recipes = parseRecipes(recipesData);

                        // Update the RecipeAdapter with the new data on the UI thread
                        getActivity().runOnUiThread(() -> recipeAdapter.setRecipes(recipes));
                    } else {
                        // Handle error response
                        String errorMessage = (String) responseData.get("message");
                        Log.e("fetchData", errorMessage);
                        getActivity().runOnUiThread(() -> {
                            ImageView placeholderImage = getView().findViewById(R.id.placeholder_image);
                            placeholderImage.setVisibility(View.VISIBLE);
                        });
                    }
                } else {
                    // Handle non-successful HTTP status
                    Log.e("fetchData", "HTTP error code: " + response.code());
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Failed to fetch data. Please try again.", Toast.LENGTH_SHORT).show();
                        ImageView placeholderImage = getView().findViewById(R.id.placeholder_image);
                        placeholderImage.setVisibility(View.VISIBLE);
                    });
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
