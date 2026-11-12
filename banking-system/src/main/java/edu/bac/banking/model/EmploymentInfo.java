package edu.bac.banking.model;

import edu.bac.banking.util.BankingException;

/**
 * Value object required by a ChequeAccount. The brief states a cheque account
 * "can only be opened for any person who is working" and that the customer must
 * provide the company and company address.
 */
public final class EmploymentInfo {

    private final String companyName;
    private final String companyAddress;

    public EmploymentInfo(String companyName, String companyAddress) {
        if (companyName == null || companyName.isBlank()) {
            throw new BankingException("Company name is required for a cheque account.");
        }
        if (companyAddress == null || companyAddress.isBlank()) {
            throw new BankingException("Company address is required for a cheque account.");
        }
        this.companyName = companyName.trim();
        this.companyAddress = companyAddress.trim();
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getCompanyAddress() {
        return companyAddress;
    }

    @Override
    public String toString() {
        return companyName + ", " + companyAddress;
    }
}
