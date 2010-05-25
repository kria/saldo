package com.adrup.saldo.bank.firstcard;

import com.adrup.saldo.bank.BankException;

public class FirstcardException extends BankException {
	private static final long serialVersionUID = 1L;
	
	public FirstcardException(String message, Throwable cause) {
        super(message, cause);
    }
    public FirstcardException(String message) {
        super(message);
    }
}
