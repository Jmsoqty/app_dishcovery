package com.example.dishcovery;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.paypal.checkout.approve.Approval;
import com.paypal.checkout.approve.OnApprove;
import com.paypal.checkout.createorder.CreateOrder;
import com.paypal.checkout.createorder.CreateOrderActions;
import com.paypal.checkout.createorder.CurrencyCode;
import com.paypal.checkout.createorder.OrderIntent;
import com.paypal.checkout.createorder.UserAction;
import com.paypal.checkout.order.Amount;
import com.paypal.checkout.order.AppContext;
import com.paypal.checkout.order.OrderRequest;
import com.paypal.checkout.order.PurchaseUnit;
import com.paypal.checkout.paymentbutton.PaymentButtonContainer;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FundsFragment extends Fragment {
    private List<Transaction> transactionsList = new ArrayList<>();

    private static final String TAG = "FundsFragment";
    private static final String BASE_URL = "http://admin.plantiq.info/api_dishcovery/";

    private PaymentButtonContainer paymentButtonContainer;
    private View view;
    private OkHttpClient httpClient = new OkHttpClient();
    // Declare the transactionId as a class-level variable
    private String transactionId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_funds, container, false);

        EditText searchBar = view.findViewById(R.id.searchBar);

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTransactions(s.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        String email = getArguments() != null ? getArguments().getString("userEmail") : null;
        paymentButtonContainer = view.findViewById(R.id.payment_button_container);

        fetchCurrentBalance(email);
        fetchAndPopulateTransactionHistory(email);

        // Setup the payment button container
        if (paymentButtonContainer != null) {
            setupPaymentButton();
        }

        return view;
    }

    private void fetchCurrentBalance(String email) {
        // Ensure email is not null before proceeding
        if (email == null) {
            Log.e(TAG, "Email is null. Cannot fetch current balance.");
            return;
        }

        // Create a URL for the request
        String url = BASE_URL + "fetch_balance.php";

        // Create a request body with the email
        RequestBody requestBody = new FormBody.Builder()
                .add("email", email)
                .build();

        // Create an OkHttp Request
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        // Make the request using OkHttpClient
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API request failed: " + e.getMessage());
                getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "API request failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        if (jsonResponse.getString("status").equals("success")) {
                            double balance = jsonResponse.getDouble("ewallet_value");
                            // Update the UI with the balance
                            getActivity().runOnUiThread(() -> {
                                TextView balanceTextView = view.findViewById(R.id.amountTextView);
                                balanceTextView.setText(String.format("Current Balance: $%.2f", balance));
                            });
                        } else {
                            Log.e(TAG, "API response error: " + jsonResponse.getString("message"));
                        }
                    } catch (JSONException e) {
                        // Handle JSON parsing errors
                        Log.e(TAG, "Error parsing API response: " + e.getMessage());
                        getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Error parsing API response", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    // Handle unsuccessful response
                    Log.e(TAG, "API response unsuccessful: " + response.code());
                    getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "API response unsuccessful: " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void setupPaymentButton() {
        paymentButtonContainer.setup(
                new CreateOrder() {
                    @Override
                    public void create(@NotNull CreateOrderActions createOrderActions) {
                        EditText paymentInput = view.findViewById(R.id.paymentInput);
                        String paymentAmount = paymentInput.getText().toString();

                        // Check if paymentAmount is empty
                        if (paymentAmount.isEmpty()) {
                            // Display a toast message to the user
                            getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Payment amount cannot be empty", Toast.LENGTH_SHORT).show());
                            // Do not proceed with the order creation
                            return;
                        }

                        // Create the purchase unit
                        PurchaseUnit purchaseUnit = new PurchaseUnit.Builder()
                                .amount(new Amount.Builder()
                                        .currencyCode(CurrencyCode.USD)
                                        .value(paymentAmount)
                                        .build())
                                .build();

                        // Create the purchase units list
                        ArrayList<PurchaseUnit> purchaseUnits = new ArrayList<>();
                        purchaseUnits.add(purchaseUnit);

                        // Create the order request
                        OrderRequest order = new OrderRequest(
                                OrderIntent.CAPTURE,
                                new AppContext.Builder()
                                        .userAction(UserAction.PAY_NOW)
                                        .build(),
                                purchaseUnits
                        );

                        createOrderActions.create(order, orderId -> {
                            // Handle the order ID
                            Log.d(TAG, "Order ID created: " + orderId);
                            // Set the transactionId variable
                            transactionId = orderId;
                        });
                    }
                },
                new OnApprove() {
                    @Override
                    public void onApprove(@NotNull Approval approval) {
                        // Handle the approval and capture the order
                        approval.getOrderActions().capture(result -> {
                            if (result != null) {
                                Log.d(TAG, String.format("Transaction ID: %s", transactionId));
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getActivity(), "Transaction successful!", Toast.LENGTH_SHORT).show();
                                    topUpWallet(transactionId);
                                });
                            } else {
                                // Handle capture error
                                Log.e(TAG, "Capture error");
                                getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Capture error occurred.", Toast.LENGTH_SHORT).show());
                            }
                        });
                    }
                }
        );
    }

    private void fetchAndPopulateTransactionHistory(String email) {
        if (email == null) {
            Log.e(TAG, "Email is null. Cannot fetch transaction history.");
            return;
        }

        String url = BASE_URL + "fetch_transactions.php";

        RequestBody requestBody = new FormBody.Builder()
                .add("email", email)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API request failed: " + e.getMessage());
                getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "API request failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "API response: " + responseBody);

                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        if (jsonResponse.getString("status").equals("success")) {
                            // Clear the existing list of transactions
                            transactionsList.clear();

                            for (int i = 0; i < jsonResponse.length() - 1; i++) {
                                String key = String.valueOf(i);
                                if (jsonResponse.has(key)) {
                                    JSONObject transactionJson = jsonResponse.getJSONObject(key);

                                    Transaction transaction = new Transaction();
                                    transaction.setTransactionId(transactionJson.getString("transaction_id"));
                                    transaction.setAmount(transactionJson.getString("amount"));
                                    transaction.setSentBy(transactionJson.getString("sent_by"));
                                    transaction.setSentTo(transactionJson.getString("sent_to"));
                                    transaction.setDateSent(transactionJson.getString("date_sent"));

                                    transactionsList.add(transaction);
                                }
                            }

                            // Populate the table with the list of transactions
                            populateTransactionTable(transactionsList);
                        } else {
                            Log.e(TAG, "API response error: " + jsonResponse.getString("message"));
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing API response: " + e.getMessage());
                        getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Error parsing API response", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Log.e(TAG, "API response unsuccessful: " + response.code());
                    getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "API response unsuccessful: " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void filterTransactions(String query) {
        List<Transaction> filteredTransactions = new ArrayList<>();

        // Iterate through the transactions list
        for (Transaction transaction : transactionsList) {
            // Check if any transaction property contains the query (case-insensitive)
            if (transaction.getTransactionId().toLowerCase().contains(query.toLowerCase()) ||
                    transaction.getAmount().toLowerCase().contains(query.toLowerCase()) ||
                    transaction.getSentBy().toLowerCase().contains(query.toLowerCase()) ||
                    transaction.getSentTo().toLowerCase().contains(query.toLowerCase()) ||
                    transaction.getDateSent().toLowerCase().contains(query.toLowerCase())) {
                // Add matching transactions to the filtered list
                filteredTransactions.add(transaction);
            }
        }

        // Populate the table with filtered transactions
        populateTransactionTable(filteredTransactions);
    }


    private void populateTransactionTable(List<Transaction> transactions) {
        // Retrieve the table layout view
        TableLayout tableLayout = view.findViewById(R.id.transactionsTable);
        int blackColor = ContextCompat.getColor(getContext(), R.color.black);

        // Clear existing rows but keep the header row (first row) intact
        getActivity().runOnUiThread(() -> {
            while (tableLayout.getChildCount() > 1) {
                tableLayout.removeViewAt(1); // Remove rows starting from index 1 (skip header row)
            }

            // Iterate through the transactions and add rows to the table
            for (Transaction transaction : transactions) {
                // Create a new row
                TableRow row = new TableRow(getContext());
                row.setGravity(Gravity.CENTER); // Center-align the row
                row.setPadding(5, 10, 5, 10); // Add padding to the row

                // Add the columns (Transaction ID, Amount, Sent By, Sent To, Date Sent) to the row
                // Transaction ID
                TextView transactionIdView = new TextView(getContext());
                transactionIdView.setText(transaction.getTransactionId());
                transactionIdView.setGravity(Gravity.CENTER);
                transactionIdView.setTextColor(blackColor);
                transactionIdView.setPadding(10, 10, 10, 10); // Add padding
                row.addView(transactionIdView);

                // Amount (with a dollar sign at the end)
                TextView amountView = new TextView(getContext());
                String amount = transaction.getAmount();
                amountView.setText(String.format("%s $", amount));
                amountView.setGravity(Gravity.CENTER);
                amountView.setTextColor(blackColor);
                amountView.setPadding(10, 10, 10, 10); // Add padding
                row.addView(amountView);

                // Determine the background color based on the amount
                if (amount.startsWith("+")) {
                    // Light green background for positive amounts
                    row.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.green));
                } else if (amount.startsWith("-")) {
                    // Light red background for negative amounts
                    row.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.light_red));
                }

                // Sent By
                TextView sentByView = new TextView(getContext());
                sentByView.setText(transaction.getSentBy());
                sentByView.setGravity(Gravity.CENTER);
                sentByView.setTextColor(blackColor);
                sentByView.setPadding(10, 10, 10, 10); // Add padding
                row.addView(sentByView);

                // Sent To
                TextView sentToView = new TextView(getContext());
                sentToView.setText(transaction.getSentTo());
                sentToView.setGravity(Gravity.CENTER);
                sentToView.setTextColor(blackColor);
                sentToView.setPadding(10, 10, 10, 10); // Add padding
                row.addView(sentToView);

                // Date Sent
                TextView dateSentView = new TextView(getContext());
                dateSentView.setText(transaction.getDateSent());
                dateSentView.setGravity(Gravity.CENTER);
                dateSentView.setTextColor(blackColor);
                dateSentView.setPadding(10, 10, 10, 10); // Add padding
                row.addView(dateSentView);

                // Add the completed row to the table
                tableLayout.addView(row);
            }
        });
    }





    private void topUpWallet(String transactionId) {
        EditText paymentInput = view.findViewById(R.id.paymentInput);
        String paymentAmount = paymentInput.getText().toString();
        String email = getArguments() != null ? getArguments().getString("userEmail") : null;

        if (email == null || paymentAmount == null || transactionId == null) {
            Log.e(TAG, "Email, payment amount, or transaction ID is null. Cannot top up wallet.");
            return;
        }

        String url = BASE_URL + "topup.php";
        RequestBody requestBody = new FormBody.Builder()
                .add("email", email)
                .add("payment", paymentAmount)
                .add("transaction_id", transactionId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API request failed: " + e.getMessage());
                getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "API request failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        if (jsonResponse.has("message")) {
                            final String message = jsonResponse.getString("message");
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                                fetchCurrentBalance(email);
                                paymentInput.setText("");
                            });
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing API response: " + e.getMessage());
                        getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Error parsing API response", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Log.e(TAG, "API response unsuccessful: " + response.code());
                    getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "API response unsuccessful: " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
