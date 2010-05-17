package com.adrup.saldo.bank.coop;

import com.adrup.saldo.bank.BankException;

public class CoopException extends BankException {
	private static final long serialVersionUID = 1L;

	public CoopException(String message, Throwable cause) {
		super(message, cause);
	}

	public CoopException(String message) {
		super(message);
	}
}