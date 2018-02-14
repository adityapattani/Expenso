package com.expenso.aditya.expenso;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeDefault extends Fragment {
    RecyclerView expensesRecycler;
    LineChart dailyExpenses;
    ExpenseAdapter expenseAdapter;
    ScrollView scrollView;
    FloatingActionMenu menu;
    FloatingActionButton buttonExpense, buttonIncome;
    TextView emptyView;
    Database database;
    String month, year;

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_home_def, container, false);
        scrollView = view.findViewById(R.id.parentScrollView);
        expensesRecycler = view.findViewById(R.id.expense_list);
        dailyExpenses = view.findViewById(R.id.graph_expense);
        emptyView = view.findViewById(R.id.emptyExpenses);
        buttonExpense = view.findViewById(R.id.add_expense);
        buttonIncome = view.findViewById(R.id.add_income);

        database = new Database(getContext());

        menu = view.findViewById(R.id.fab_menu);
        menu.setClosedOnTouchOutside(true);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        expensesRecycler.setLayoutManager(mLayoutManager);
        expensesRecycler.setFocusable(false);

        @SuppressLint("SimpleDateFormat") SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM-yyyy");

        Calendar calendar = Calendar.getInstance();
        month = monthYearFormat.format(calendar.getTime()).split("-")[0];
        year = monthYearFormat.format(calendar.getTime()).split("-")[1];

        final List<Expense> expenses = database.getExpensesForMonth(month, year);

        if (expenses.size() == 0) {
            emptyView.setVisibility(View.VISIBLE);
            expensesRecycler.setVisibility(View.INVISIBLE);
        } else {
            emptyView.setVisibility(View.INVISIBLE);
            expensesRecycler.setVisibility(View.VISIBLE);
        }

        expenseAdapter = new ExpenseAdapter(getContext(), expenses);
        expensesRecycler.setAdapter(expenseAdapter);

        constructGraph();

        buttonIncome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showIncomeAlert();
                menu.close(true);
            }
        });

        buttonExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showExpenseAlert();
                menu.close(true);
            }
        });

        return view;
    }

    private void constructGraph() {
        int firstDate, lastDate;

        ArrayList<Entry> data = new ArrayList<>();
        JSONObject graphData = database.getGraphData(month, year);
        YAxis leftAxis = dailyExpenses.getAxisLeft();
        YAxis rightAxis = dailyExpenses.getAxisRight();

        XAxis xAxis = dailyExpenses.getXAxis();
        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        try {
            lastDate = Integer.parseInt(graphData.getString("lastDate").split("-")[0]);
            firstDate = Integer.parseInt(graphData.getString("firstDate").split("-")[0]);

            rightAxis.setAxisMaximum(graphData.getInt("totalIncome") + 50);
            rightAxis.setDrawGridLines(false);
            rightAxis.setEnabled(false);
            leftAxis.setDrawGridLines(false);

            dailyExpenses.getLegend().setEnabled(false);
            dailyExpenses.setDrawBorders(false);
            dailyExpenses.getDescription().setEnabled(false);
            dailyExpenses.setTouchEnabled(false);
            dailyExpenses.getXAxis().setTextSize(14);
            dailyExpenses.getAxisLeft().setTextSize(14);

            if(lastDate != 0 && firstDate != 0){
                Log.e("Here", "Here");
                for (int i = firstDate; i<=lastDate; i++) {
                    data.add(new Entry(i, database.getExpensesForDate(i, month, year)));
                }
                LineDataSet lineDataSet = new LineDataSet(data, "");
                ArrayList<ILineDataSet> dataSets = new ArrayList<>();
                dataSets.add(lineDataSet);

                LineData lineData = new LineData(dataSets);
                lineData.setValueTextSize(14);
                dailyExpenses.setData(lineData);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void showExpenseAlert() {
        @SuppressLint("InflateParams") View alertView = getLayoutInflater().inflate(R.layout.alert_expense, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(alertView);

        final EditText amountEntered = alertView.findViewById(R.id.alert_expense_amount);
        final EditText descEntered = alertView.findViewById(R.id.alert_expense_desc);
        final Spinner typeEntered = alertView.findViewById(R.id.alert_expense_type);

        List<String> spinnerArray = new ArrayList<>();
        spinnerArray.add("Food");
        spinnerArray.add("Shopping");
        spinnerArray.add("Transport");
        spinnerArray.add("Debt");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, spinnerArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeEntered.setAdapter(adapter);

        final Button addBtn = alertView.findViewById(R.id.alert_expense_add);
        final Button cancelBtn = alertView.findViewById(R.id.alert_expense_cancel);

        final AlertDialog expenseDialog = builder.create();

        expenseDialog.setCanceledOnTouchOutside(true);
        expenseDialog.show();

        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean check = true;
                @SuppressLint("SimpleDateFormat") SimpleDateFormat monthYearFormat = new SimpleDateFormat("dd-MM-yyyy");
                Calendar calendar = Calendar.getInstance();
                String date = monthYearFormat.format(calendar.getTime());
                String desc = descEntered.getText().toString().trim();
                String type = typeEntered.getSelectedItem().toString();
                int amount = 0;

                try {
                    amount = Integer.parseInt(amountEntered.getText().toString().trim());
                } catch (NumberFormatException e) {
                    amountEntered.setError("Amount value cannot be accepted");
                }

                if (amount <= 0) {
                    amountEntered.setError("Amount value cannot be accepted");
                    check = false;
                }
                if (desc.length() == 0) {
                    descEntered.setError("Description cannot be blank");
                    check = false;
                }
                if (type.length() == 0) {
                    check = false;
                }

                if (check){
                    database.addExpense(new Expense(desc, type, date, amount));
                    expenseDialog.dismiss();
                }
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                expenseDialog.dismiss();
            }
        });

        expenseDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                expenseAdapter = new ExpenseAdapter(getContext(), database.getExpensesForMonth(month, year));
                expensesRecycler.setAdapter(expenseAdapter);
                constructGraph();
            }
        });
    }

    private void showIncomeAlert() {
        @SuppressLint("InflateParams") View alertView = getLayoutInflater().inflate(R.layout.alert_income, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(alertView);

        final EditText amountEntered = alertView.findViewById(R.id.amount_alert);
        final Button addBtn = alertView.findViewById(R.id.alert_income_add);
        final Button cancelBtn = alertView.findViewById(R.id.alert_income_cancel);

        final AlertDialog incomeDialog = builder.create();

        incomeDialog.setCanceledOnTouchOutside(true);
        incomeDialog.show();

        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat monthYearFormat = new SimpleDateFormat("dd-MM-yyyy");
                Calendar calendar = Calendar.getInstance();
                String date = monthYearFormat.format(calendar.getTime());
                int amount = 0;
                try {
                    amount = Integer.parseInt(amountEntered.getText().toString().trim());
                } catch (NumberFormatException e) {
                    amountEntered.setError("Amount value cannot be accepted");
                }

                if (amount <= 0) {
                    amountEntered.setError("Amount value cannot be accepted");
                } else {
                    database.addIncome(amount, date);
                    incomeDialog.dismiss();
                }
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                incomeDialog.dismiss();
            }
        });

        incomeDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                dailyExpenses.notifyDataSetChanged();
            }
        });
    }
}
