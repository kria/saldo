package com.adrup.saldo.bank.lf;

import com.adrup.saldo.bank.BankException;

public class LfBankException extends BankException {
	private static final long serialVersionUID = 1L;

	public LfBankException(String message, Throwable cause) {
        super(message, cause);
    }
    public LfBankException(String message) {
        super(message);
    }
}
