package com.example.android.faciallandmarkdetection;

import android.view.View;
import android.widget.AdapterView;

public class ProcessingOptionsSpinner implements AdapterView.OnItemSelectedListener {

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {

        // To retrieve a selected item
        parent.getItemAtPosition(pos).toString();
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Not implemeted as one option is selected by default
    }

}
