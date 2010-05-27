package com.adrup.saldo.bank.statoil;


import com.adrup.saldo.bank.BankException;

public class StatoilException extends BankException {
	private static final long serialVersionUID = 1L;
	
	public StatoilException(String message, Throwable cause) {
        super(message, cause);
    }
    public StatoilException(String message) {
        super(message);
    }
}
