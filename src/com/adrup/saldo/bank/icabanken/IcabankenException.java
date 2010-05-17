package com.adrup.saldo.bank.icabanken;

import com.adrup.saldo.bank.BankException;

public class IcabankenException extends BankException {
	private static final long serialVersionUID = 1L;

	public IcabankenException(String message, Throwable cause) {
		super(message, cause);
	}

	public IcabankenException(String message) {
		super(message);
	}
}