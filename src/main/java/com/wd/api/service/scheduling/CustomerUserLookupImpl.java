package com.wd.api.service.scheduling;

import com.wd.api.model.CustomerUser;
import com.wd.api.repository.CustomerUserRepository;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;

@Component
public class CustomerUserLookupImpl implements CustomerUserLookup {

    private final CustomerUserRepository repo;

    public CustomerUserLookupImpl(CustomerUserRepository repo) {
        this.repo = repo;
    }

    @Override
    public CustomerUserContact contactFor(Long customerUserId) {
        CustomerUser u = repo.findById(customerUserId)
            .orElseThrow(() -> new NoSuchElementException(
                "CustomerUser not found: " + customerUserId));
        String name = (u.getFirstName() == null ? "" : u.getFirstName())
                    + (u.getLastName() == null ? "" : (" " + u.getLastName()));
        return new CustomerUserContact(u.getEmail(), name.trim());
    }
}
