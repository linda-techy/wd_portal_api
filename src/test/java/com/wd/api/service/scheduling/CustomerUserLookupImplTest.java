package com.wd.api.service.scheduling;

import com.wd.api.model.CustomerUser;
import com.wd.api.repository.CustomerUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerUserLookupImplTest {

    @Mock CustomerUserRepository repo;
    @InjectMocks CustomerUserLookupImpl impl;

    @Test
    void returnsEmailAndJoinedNameForExistingUser() {
        CustomerUser u = new CustomerUser();
        u.setId(7L);
        u.setEmail("ravi@example.com");
        u.setFirstName("Ravi");
        u.setLastName("Kumar");
        when(repo.findById(7L)).thenReturn(Optional.of(u));

        CustomerUserContact c = impl.contactFor(7L);
        assertThat(c.email()).isEqualTo("ravi@example.com");
        assertThat(c.name()).isEqualTo("Ravi Kumar");
    }

    @Test
    void throwsForUnknownUser() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> impl.contactFor(99L))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("99");
    }
}
