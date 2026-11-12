package edu.bac.banking.controller;

import edu.bac.banking.dao.CustomerDAO;
import edu.bac.banking.model.Customer;
import edu.bac.banking.model.EmploymentInfo;
import edu.bac.banking.util.BankingException;
import edu.bac.banking.util.PasswordHasher;
import edu.bac.banking.util.Validation;

import java.util.Locale;

/** Mediates between a login/register view and the CustomerDAO + core model. */
public class LoginController {

    private final CustomerDAO customerDAO;

    public LoginController(CustomerDAO customerDAO) {
        this.customerDAO = customerDAO;
    }

    public Result<Customer> login(String email, String password) {
        try {
            String key = Validation.required(email, "Email").toLowerCase(Locale.ROOT);
            Customer customer = customerDAO.findByEmail(key)
                    .orElseThrow(() -> new BankingException("No account matches that email."));
            if (!PasswordHasher.verify(password, customer.getPasswordHash())) {
                return Result.failure("Invalid email or password.");
            }
            return Result.success("Welcome back, " + customer.getFirstName() + ".", customer);
        } catch (RuntimeException ex) {
            return Result.failure(ex.getMessage());
        }
    }

    public Result<Customer> register(String firstName, String surname, String address,
                                     String email, String password, boolean employed,
                                     String companyName, String companyAddress) {
        try {
            Validation.required(password, "Password");
            if (password.length() < 8) {
                throw new BankingException("Password must be at least 8 characters long.");
            }
            EmploymentInfo info = null;
            boolean hasCompany = companyName != null && !companyName.isBlank();
            if (employed || hasCompany) {
                info = new EmploymentInfo(companyName, companyAddress);
            }
            Customer customer = new Customer(
                    Validation.required(firstName, "First name"),
                    Validation.required(surname, "Surname"),
                    address,
                    Validation.required(email, "Email").toLowerCase(Locale.ROOT),
                    PasswordHasher.hash(password),
                    employed,
                    info);
            return Result.success("Registration successful.", customerDAO.save(customer));
        } catch (RuntimeException ex) {
            return Result.failure(ex.getMessage());
        }
    }
}
