package com.example.dishcovery;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

public class GroupsFragment extends Fragment {

    private OkHttpClient client = new OkHttpClient();
    private RecipeAdapter recipeAdapter;

    private EditText searchBar;
    private ImageView createGroupButton;
    private ImageView joinGroupButton;
    private ImageView visitGroupButton;
    private ImageView leaveGroupButton;
    private Group selectedGroup;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_groups, container, false);

        // Initialize views
        createGroupButton = view.findViewById(R.id.create_group);
        joinGroupButton = view.findViewById(R.id.join_group);
        visitGroupButton = view.findViewById(R.id.visit_group);
        leaveGroupButton = view.findViewById(R.id.leave_group);
        searchBar = view.findViewById(R.id.search_bar);

        Bundle args = getArguments();
        String community = args != null ? args.getString("community") : null;

        RecyclerView recipeRecyclerView = view.findViewById(R.id.recipe_recycler_view);
        recipeRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        FloatingActionButton fabAdd = view.findViewById(R.id.fab_add);

        TextView joinedGroupTextView = view.findViewById(R.id.joined_group);

        // Set visibility based on the value of community argument
        if (community != null && !community.isEmpty()) {
            recipeRecyclerView.setVisibility(View.VISIBLE);
            fabAdd.setVisibility(View.VISIBLE);
            joinedGroupTextView.setVisibility(View.VISIBLE);

            joinedGroupTextView.setText(community);

            recipeAdapter = new RecipeAdapter(new ArrayList<>(), args != null ? args.getString("userEmail") : null);
            recipeRecyclerView.setAdapter(recipeAdapter);

            // Set the click listener for Floating Action Button
            // Set the click listener for Floating Action Button
            fabAdd.setOnClickListener(v -> {
                ShareRecipeDialog shareRecipeDialog = new ShareRecipeDialog();

                // Create a new Bundle to pass arguments to the dialog
                Bundle dialogArgs = new Bundle();
                // Add the community name to the Bundle
                dialogArgs.putString("community", community);

                // Add userEmail argument if present in args
                if (args != null) {
                    dialogArgs.putString("userEmail", args.getString("userEmail"));
                }

                // Set the arguments for the ShareRecipeDialog
                shareRecipeDialog.setArguments(dialogArgs);

                // Show the ShareRecipeDialog
                shareRecipeDialog.show(getParentFragmentManager(), "ShareRecipeDialog");
            });


            // Fetch data for the RecyclerView
            fetchData(community);
        } else {
            recipeRecyclerView.setVisibility(View.GONE);
            fabAdd.setVisibility(View.GONE);
            joinedGroupTextView.setVisibility(View.GONE);
        }

        // Set up button listeners
        setButtonListeners();

        // Add a TextWatcher to the search bar for filtering recipes
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

    private void setButtonListeners() {

        // Listener for create group button
        createGroupButton.setOnClickListener(v -> {
            // Create a new AlertDialog to create a group
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_create_group, null);

            // Set the dialog view to the custom layout
            builder.setView(dialogView);

            // Initialize EditText for group name
            EditText groupNameEditText = dialogView.findViewById(R.id.create_title);
            Button createGroupButton = dialogView.findViewById(R.id.btn_add_group);

            // Create the AlertDialog
            AlertDialog dialog = builder.create();

            // Set the onClick listener for the "Create Group" button
            createGroupButton.setOnClickListener(createButtonView -> {
                // Get the group name from the EditText
                String groupName = groupNameEditText.getText().toString().trim();

                // Check if the group name is empty
                if (groupName.isEmpty()) {
                    // Display a toast message if the group name is empty
                    Toast.makeText(getContext(), "Group name cannot be empty", Toast.LENGTH_SHORT).show();
                } else {
                    // Create the group and pass the dialog reference
                    createGroup(groupName, dialog);
                }
            });

            // Set the negative button for the dialog
            builder.setNegativeButton("Cancel", null);

            // Show the dialog
            dialog.show();
        });

        joinGroupButton.setOnClickListener(v -> {
            // Create a new AlertDialog for the join group dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            View modalView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_join_group, null);
            builder.setView(modalView);

            // Initialize the RecyclerView
            RecyclerView rvAvailableGroups = modalView.findViewById(R.id.rv_available_groups);
            rvAvailableGroups.setLayoutManager(new LinearLayoutManager(getContext()));

            // Initialize the list of available groups
            List<Group> availableGroups = new ArrayList<>();

            // Initialize the GroupAdapter with the available groups and a click listener
            GroupAdapter groupAdapter = new GroupAdapter(availableGroups, group -> {
                // Handle group selection
                selectedGroup = group;
            });

            // Set the adapter for the RecyclerView
            rvAvailableGroups.setAdapter(groupAdapter);

            // Fetch available groups from the API and populate the RecyclerView
            TextView noGroupsTextView = modalView.findViewById(R.id.no_groups_text_view);
            fetchAvailableGroups(groupAdapter, noGroupsTextView);

            // Add search functionality to the search EditText
            EditText etSearchGroupJoined = modalView.findViewById(R.id.et_search_group_joined);
            etSearchGroupJoined.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // No action needed before text change
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Filter the list of available groups based on the search query
                    groupAdapter.getFilter().filter(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // No action needed after text change
                }
            });

            // Reference the join group button
            Button btnJoinGroup = modalView.findViewById(R.id.btn_join_group);

            // Declare the AlertDialog variable at the top of the function
            AlertDialog alertDialog = builder.setPositiveButton("Close", null).create();

            // Listener for the join group button within the dialog
            btnJoinGroup.setOnClickListener(view -> {
                if (selectedGroup != null) {
                    // Call joinGroup and pass the AlertDialog instance to close it upon success
                    joinGroup(selectedGroup.getCommunityName(), alertDialog);
                } else {
                    Toast.makeText(getContext(), "Please select a group to join.", Toast.LENGTH_SHORT).show();
                }
            });

            // Show the AlertDialog
            alertDialog.show();
        });

        visitGroupButton.setOnClickListener(v -> {
            // Inflate and set the dialog view
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            View modalView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_visit_group, null);
            builder.setView(modalView);

            // Create and show the AlertDialog
            AlertDialog alertDialog = builder.setPositiveButton("Close", null).create();
            alertDialog.show();

            // Initialize the RecyclerView
            RecyclerView rvAvailableGroups = modalView.findViewById(R.id.rv_available_groups);
            rvAvailableGroups.setLayoutManager(new LinearLayoutManager(getContext()));

            // Initialize the GroupAdapter
            List<Group> availableGroups = new ArrayList<>();
            GroupAdapter groupAdapter = new GroupAdapter(availableGroups, group -> {
                // Store the selected group
                selectedGroup = group;
            });
            rvAvailableGroups.setAdapter(groupAdapter);

            // Fetch groups from API
            TextView noGroupsTextView = modalView.findViewById(R.id.no_groups_text_view);
            fetchAvailableGroupsToVisit(groupAdapter, noGroupsTextView);

            // Add search functionality to the search EditText
            EditText etSearchGroupVisit = modalView.findViewById(R.id.et_search_group_visit);
            etSearchGroupVisit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // No action needed before text change
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Filter the list of joined groups based on the search query
                    groupAdapter.getFilter().filter(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // No action needed after text change
                }
            });

            // Handle "Visit Group" button click
            Button btnVisitGroup = modalView.findViewById(R.id.btn_visit_group);
            btnVisitGroup.setOnClickListener(view -> {
                if (selectedGroup != null) {
                    // Get the community name from the selected group
                    String communityName = selectedGroup.getCommunityName();

                    // Create a new instance of GroupsFragment
                    GroupsFragment groupsFragment = new GroupsFragment();

                    // Create a new Bundle to hold the arguments
                    Bundle args = new Bundle();

                    // Add the community name and user email to the arguments
                    args.putString("community", communityName);
                    args.putString("userEmail", getArguments().getString("userEmail"));

                    // Set the arguments for the new GroupsFragment
                    groupsFragment.setArguments(args);

                    // Replace the current fragment with the new GroupsFragment
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, groupsFragment)
                            .addToBackStack(null)
                            .commit();

                    // Close the alert dialog after visiting the group
                    alertDialog.dismiss();
                } else {
                    // Show a toast if no group is selected
                    Toast.makeText(getContext(), "Please select a group to visit.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Listener for leave group button
        leaveGroupButton.setOnClickListener(v -> {
            // Inflate and set the dialog view
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            View modalView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_leave_group, null);
            builder.setView(modalView);

            // Create and show the AlertDialog
            AlertDialog alertDialog = builder.setPositiveButton("Close", null).create();
            alertDialog.show();

            // Initialize the RecyclerView
            RecyclerView rvAvailableGroups = modalView.findViewById(R.id.rv_available_groups);
            rvAvailableGroups.setLayoutManager(new LinearLayoutManager(getContext()));

            // Initialize the GroupAdapter
            List<Group> availableGroups = new ArrayList<>();
            GroupAdapter groupAdapter = new GroupAdapter(availableGroups, group -> {
                // Store the selected group
                selectedGroup = group;
            });
            rvAvailableGroups.setAdapter(groupAdapter);

            // Fetch groups from API
            TextView noGroupsTextView = modalView.findViewById(R.id.no_groups_text_view);
            fetchAvailableGroupsToVisit(groupAdapter, noGroupsTextView);

            // Add search functionality to the search EditText
            EditText etSearchGroupLeave = modalView.findViewById(R.id.et_search_group_leave);
            etSearchGroupLeave.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // No action needed before text change
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Filter the list of joined groups based on the search query
                    groupAdapter.getFilter().filter(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // No action needed after text change
                }
            });

            // Handle "Leave Group" button click
            Button btnLeaveGroup = modalView.findViewById(R.id.btn_leave_group);
            btnLeaveGroup.setOnClickListener(view -> {
                if (selectedGroup != null) {
                    // Get the community name from the selected group
                    String communityName = selectedGroup.getCommunityName();

                    // Retrieve the user email from the arguments
                    String userEmail = getArguments().getString("userEmail");

                    // Call the leaveGroup function to handle leaving the group
                    leaveGroup(communityName, userEmail, alertDialog);

                    // Check if the communityName matches the community argument
                    Bundle args = getArguments();
                    if (args != null) {
                        String communityArgument = args.getString("community");

                        if (communityName.equals(communityArgument)) {
                            // Set the community argument to null if it matches the selected group
                            args.putString("community", null);
                        }
                    }

                    // Create a new instance of GroupsFragment
                    GroupsFragment groupsFragment = new GroupsFragment();
                    groupsFragment.setArguments(args);

                    // Replace the current fragment with the new GroupsFragment
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, groupsFragment)
                            .addToBackStack(null)
                            .commit();

                    // Close the alert dialog after leaving the group
                    alertDialog.dismiss();
                } else {
                    // Show a toast if no group is selected
                    Toast.makeText(getContext(), "Please select a group to leave.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

        private void fetchAvailableGroups(GroupAdapter adapter, TextView noGroupsTextView) {
        // Define the API URL for fetching available groups
        String url = "http://192.168.1.18/dishcovery/api/fetch_joined_groups.php";

        // Create a request body with the email parameter
        RequestBody requestBody = new FormBody.Builder()
                .add("email", getArguments().getString("userEmail"))
                .build();

        // Create a POST request
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        // Make the HTTP request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Handle error
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseText = response.body().string();

                    try {
                        JSONObject jsonResponse = new JSONObject(responseText);
                        JSONArray data = jsonResponse.getJSONArray("data");

                        List<Group> groups = new ArrayList<>();

                        // Parse the JSON response
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject groupJson = data.getJSONObject(i);

                            // Extract group data from JSON
                            String communityName = groupJson.getString("community_name");
                            int numberOfMembers = groupJson.getInt("number_of_members");
                            String dateCreated = groupJson.getString("date_created");

                            // Create a Group object and add it to the list
                            Group group = new Group(communityName, numberOfMembers, dateCreated);
                            groups.add(group);
                        }

                        // Update the adapter with the list of groups
                        getActivity().runOnUiThread(() -> {
                            adapter.setGroups(groups);

                            // Check if the groups list is empty
                            if (groups.isEmpty()) {
                                // Set the TextView for "No groups available" to VISIBLE and set the text
                                noGroupsTextView.setVisibility(View.VISIBLE);
                                noGroupsTextView.setText("No groups available");
                            } else {
                                // Hide the TextView since groups are available
                                noGroupsTextView.setVisibility(View.GONE);
                            }
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void fetchAvailableGroupsToVisit(GroupAdapter adapter, TextView noGroupsTextView) {
        // Define the API URL for fetching available groups
        String url = "http://192.168.1.18/dishcovery/api/fetch_visit_groups.php";

        // Create a request body with the email parameter
        RequestBody requestBody = new FormBody.Builder()
                .add("email", getArguments().getString("userEmail"))
                .build();

        // Create a POST request
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        // Make the HTTP request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Handle error
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseText = response.body().string();

                    try {
                        JSONObject jsonResponse = new JSONObject(responseText);
                        JSONArray data = jsonResponse.getJSONArray("data");

                        List<Group> groups = new ArrayList<>();

                        // Parse the JSON response
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject groupJson = data.getJSONObject(i);

                            // Extract group data from JSON
                            String communityName = groupJson.getString("community_name");
                            int numberOfMembers = groupJson.getInt("number_of_members");
                            String dateCreated = groupJson.getString("date_created");

                            // Create a Group object and add it to the list
                            Group group = new Group(communityName, numberOfMembers, dateCreated);
                            groups.add(group);
                        }

                        // Update the adapter with the list of groups
                        getActivity().runOnUiThread(() -> {
                            adapter.setGroups(groups);

                            // Check if the groups list is empty
                            if (groups.isEmpty()) {
                                // Set the TextView for "No groups available" to VISIBLE and set the text
                                noGroupsTextView.setVisibility(View.VISIBLE);
                                noGroupsTextView.setText("No groups available");
                            } else {
                                // Hide the TextView since groups are available
                                noGroupsTextView.setVisibility(View.GONE);
                            }
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void joinGroup(String groupName, AlertDialog dialog) {
        // Define the API URL for joining a group
        String url = "http://192.168.1.18/dishcovery/api/join_group.php";

        // Create a request body with the group name and user email
        RequestBody requestBody = new FormBody.Builder()
                .add("group_name", groupName)
                .add("email", getArguments().getString("userEmail"))
                .build();

        // Create a POST request
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        // Make the HTTP request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Handle error
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseText = response.body().string();
                    getActivity().runOnUiThread(() -> {
                        // Handle the server's response
                        Toast.makeText(getContext(), responseText, Toast.LENGTH_SHORT).show();
                        // Close the dialog upon successful group joining
                        dialog.dismiss();
                    });
                }
            }
        });
    }

    private void createGroup(String groupName, AlertDialog dialog) {
        // Define the API URL for creating a group
        String url = "http://192.168.1.18/dishcovery/api/create_group.php";

        // Create a request body with the group name and user email
        RequestBody requestBody = new FormBody.Builder()
                .add("group_name", groupName)
                .add("email", getArguments().getString("userEmail")) // Use user email from arguments
                .build();

        // Create a POST request
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        // Make the HTTP request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Handle error
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseText = response.body().string();
                    getActivity().runOnUiThread(() -> {
                        // Handle the server's response
                        if (responseText.equals("Group created successfully.")) {
                            // Notify the user and update UI (if needed)
                            Toast.makeText(getContext(), "Group created successfully.", Toast.LENGTH_SHORT).show();
                            // Close the dialog
                            dialog.dismiss();
                            // Optionally, you can refresh the list of groups here
                        } else {
                            // Handle any errors or failure
                            Toast.makeText(getContext(), responseText, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void leaveGroup(String groupName, String email, AlertDialog dialog) {
        // Define the API URL for creating a group
        String url = "http://192.168.1.18/dishcovery/api/leave_group.php";

        // Create a request body with the group name and user email
        RequestBody requestBody = new FormBody.Builder()
                .add("group_name", groupName)
                .add("email", email) // Use user email from arguments
                .build();

        // Create a POST request
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        // Make the HTTP request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Handle error
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseText = response.body().string();
                    getActivity().runOnUiThread(() -> {
                        // Handle the server's response
                        if (responseText.equals("Left group successfully.")) {
                            // Notify the user and update UI (if needed)
                            Toast.makeText(getContext(), "Successfully left " + groupName, Toast.LENGTH_SHORT).show();
                            // Close the dialog
                            dialog.dismiss();
                            // Optionally, you can refresh the list of groups here
                        } else {
                            // Handle any errors or failure
                            Toast.makeText(getContext(), responseText, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void fetchData(String communityName) {
        String url = "http://192.168.1.18/dishcovery/api/fetch_recipes_from_groups.php?community=" + communityName; // API URL with community parameter

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
                    Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();

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
            Recipe recipe = gson.fromJson(gson.toJson(recipeData), Recipe.class);
            recipes.add(recipe);
        }

        return recipes;
    }
}
