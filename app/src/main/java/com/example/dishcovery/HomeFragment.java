package com.example.dishcovery;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class HomeFragment extends Fragment {

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Get reference to the Floating Action Button (FAB)
        FloatingActionButton fabAdd = view.findViewById(R.id.fab_add);

        // Retrieve the user's email from arguments, if available
        String email = getArguments() != null ? getArguments().getString("userEmail") : null;
        // Set up a click listener for the FAB
        fabAdd.setOnClickListener(v -> {
            // Create an instance of ShareRecipeDialog
            ShareRecipeDialog shareRecipeDialog = new ShareRecipeDialog();

            // Create a Bundle to pass the email to the dialog
            Bundle args = new Bundle();
            args.putString("userEmail", email);

            // Set the arguments for the dialog
            shareRecipeDialog.setArguments(args);

            // Show the ShareRecipeDialog
            shareRecipeDialog.show(getFragmentManager(), "ShareRecipeDialog");
        });

        return view;
    }
}