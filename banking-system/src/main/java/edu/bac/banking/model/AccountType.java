package edu.bac.banking.model;

/** Discriminator persisted in accounts.account_type and used by AccountFactory / JdbcAccountDAO. */
public enum AccountType {
    SAVINGS("Savings account", "0.05% monthly interest, deposits only"),
    INVESTMENT("Investment account", "5% monthly interest, minimum BWP 500.00 opening deposit"),
    CHEQUE("Cheque account", "salary account, employed customers only");

    private final String label;
    private final String rule;

    AccountType(String label, String rule) {
        this.label = label;
        this.rule = rule;
    }

    public String getLabel() {
        return label;
    }

    public String getRule() {
        return rule;
    }
}
