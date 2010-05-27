package com.adrup.saldo.bank.preem;


import com.adrup.saldo.bank.BankException;

public class PreemException extends BankException {
	private static final long serialVersionUID = 1L;
	
	public PreemException(String message, Throwable cause) {
        super(message, cause);
    }
    public PreemException(String message) {
        super(message);
    }
}
