package com.adrup.saldo.bank.ica;

import com.adrup.saldo.bank.BankException;

public class IcaException extends BankException {
	private static final long serialVersionUID = 1L;

	public IcaException(String message, Throwable cause) {
		super(message, cause);
	}

	public IcaException(String message) {
		super(message);
	}
}
