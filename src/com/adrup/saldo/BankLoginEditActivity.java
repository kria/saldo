/*
 * Saldo - http://github.com/kria/saldo
 * 
 * Copyright (C) 2010 Kristian Adrup
 * 
 * This file is part of Saldo.
 * 
 * Saldo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Saldo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.adrup.saldo;

import com.adrup.saldo.bank.BankLogin;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * An activity that displays and handles the creation and editing of a single {@link BankLogin}.
 * 
 * @author Kristian Adrup
 *
 */
public class BankLoginEditActivity extends Activity {

	private EditText mNameText;
    private EditText mUsernameText;
    private EditText mPasswordText;
    private Spinner  mSpinner;
    private Long mRowId;
    private DatabaseAdapter mDbAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDbAdapter = new DatabaseAdapter(this);
        mDbAdapter.open();
        setContentView(R.layout.bank_logins_edit);
        
        mSpinner = (Spinner)findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.banks_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(new BankSelectedListener());
        
        mNameText = (EditText) findViewById(R.id.layout_bankloginedit_name);
        mUsernameText = (EditText) findViewById(R.id.layout_bankloginedit_username);
        mPasswordText = (EditText) findViewById(R.id.layout_bankloginedit_password);
      
        Button saveButton = (Button) findViewById(R.id.layout_bankloginedit_save_btn);
        Button cancelButton = (Button) findViewById(R.id.layout_bankloginedit_cancel_btn);
       
        mRowId = savedInstanceState != null ? savedInstanceState.getLong(BankLogin.KEY_ID) : null;
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();            
			mRowId = extras != null ? extras.getLong(BankLogin.KEY_ID): null;
		}

		populateFields();
		
		saveButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View view) {
        		saveState();
        	    setResult(RESULT_OK);
        	    finish();
        	}
        });
		cancelButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View view) {
        	    setResult(RESULT_CANCELED);
        	    finish();
        	}
          
        });
    }
    
    private void populateFields() {
        if (mRowId != null) {
            BankLogin bankLogin = mDbAdapter.fetchBankLogin(mRowId.intValue());
            
            mNameText.setText(bankLogin.getName());
            mUsernameText.setText(bankLogin.getUsername());
            mPasswordText.setText(bankLogin.getPassword());
            mSpinner.setSelection(bankLogin.getBankId() - 1); // TODO: not very robust, fix later
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(BankLogin.KEY_ID, mRowId);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        populateFields();
    }
    
    private void saveState() {
        String name = mNameText.getText().toString();
        if (name.trim().length() == 0) name = "New bank login";
        String username = mUsernameText.getText().toString();
        String password = mPasswordText.getText().toString();
        int bankId = mSpinner.getSelectedItemPosition() + 1;  // TODO: not very robust, fix later
        
        BankLogin bankLogin = new BankLogin(mRowId == null ? 0 : mRowId.intValue(), bankId, name, username, password);
        
        int id = mDbAdapter.saveBankLogin(bankLogin);
        if (id > 0) {
            mRowId = Long.valueOf(id);
        }
    }
    
    public class BankSelectedListener implements OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent,
            View view, int pos, long id) {
        	if(mRowId == null) {
        		mNameText.setText(parent.getItemAtPosition(pos).toString());
        	}
        }

        public void onNothingSelected(AdapterView parent) {
          // Do nothing.
        }
    }

}
