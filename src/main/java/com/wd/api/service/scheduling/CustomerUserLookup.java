package com.wd.api.service.scheduling;

public interface CustomerUserLookup {
    /**
     * @return contact for the customer user.
     * @throws java.util.NoSuchElementException if the user does not exist.
     */
    CustomerUserContact contactFor(Long customerUserId);
}
