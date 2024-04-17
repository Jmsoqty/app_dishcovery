package com.example.dishcovery;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.ViewHolder> implements Filterable {
    private List<Group> groups;
    private List<Group> filteredGroups;
    private int selectedPosition = -1;
    private final GroupClickListener groupClickListener;

    interface GroupClickListener {
        void onGroupClick(Group group);
    }

    GroupAdapter(List<Group> groups, GroupClickListener groupClickListener) {
        this.groups = groups;
        this.filteredGroups = new ArrayList<>(groups);
        this.groupClickListener = groupClickListener;
    }

    void setGroups(List<Group> groups) {
        this.groups = groups;
        this.filteredGroups.clear();
        this.filteredGroups.addAll(groups);
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Group group = filteredGroups.get(position);
        holder.bind(group);

        // Set the click listener on the item view
        holder.itemView.setOnClickListener(v -> {
            // Use `getAdapterPosition()` to ensure position is valid
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                // Update the selected position
                int previousSelectedPosition = selectedPosition;
                selectedPosition = currentPosition;

                // Notify changes if selection changed
                if (previousSelectedPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(previousSelectedPosition);
                }
                notifyItemChanged(selectedPosition);

                // Trigger the callback to handle group click
                groupClickListener.onGroupClick(filteredGroups.get(currentPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredGroups.size();
    }

    // ViewHolder class for holding the view items
    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCommunityName, tvNoOfMembers, tvDateCreated;

        ViewHolder(View itemView) {
            super(itemView);
            tvCommunityName = itemView.findViewById(R.id.tv_community_name);
            tvNoOfMembers = itemView.findViewById(R.id.tv_number_of_members);
            tvDateCreated = itemView.findViewById(R.id.tv_date_created);
        }

        void bind(Group group) {
            tvCommunityName.setText(group.getCommunityName());
            tvNoOfMembers.setText(String.valueOf(group.getNumberOfMembers()));
            tvDateCreated.setText(group.getDateCreated());

            // Highlight the selected item
            itemView.setBackgroundColor(selectedPosition == getAdapterPosition() ? Color.LTGRAY : Color.TRANSPARENT);
        }
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                List<Group> filteredList = new ArrayList<>();

                if (constraint == null || constraint.length() == 0) {
                    filteredList.addAll(groups);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();
                    for (Group group : groups) {
                        if (group.getCommunityName().toLowerCase().contains(filterPattern)) {
                            filteredList.add(group);
                        }
                    }
                }

                results.values = filteredList;
                results.count = filteredList.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredGroups.clear();
                filteredGroups.addAll((List<Group>) results.values);
                notifyDataSetChanged();
            }
        };
    }
}
