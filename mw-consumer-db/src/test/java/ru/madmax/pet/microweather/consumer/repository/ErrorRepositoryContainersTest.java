package ru.madmax.pet.microweather.consumer.repository;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.r2dbc.BadSqlGrammarException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.madmax.pet.microweather.consumer.AbstractContainersIntegrationTest;
import ru.madmax.pet.microweather.consumer.model.ErrorDomain;
import ru.madmax.pet.microweather.consumer.model.TestErrorDomainBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataR2dbcTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Tag("Containers")
class ErrorRepositoryContainersTest extends AbstractContainersIntegrationTest {
    final ErrorRepository errorRepository;

    @Test
    void saveError_AndFindIt() {
        ErrorDomain errorDomain = TestErrorDomainBuilder.anErrorDomain()
                .withId("key_" + System.currentTimeMillis()).build();
        ErrorDomain error = errorRepository.save(errorDomain).block();

        assertThat(error).isNotNull();
        assertThat(error.getId()).isEqualTo(errorDomain.getId());

        Mono<ErrorDomain> errorDBMono = errorRepository.findById(error.getId());
        StepVerifier.create(errorDBMono)
                .assertNext(element -> {
                    assertThat(element.getId()).isEqualTo(error.getId());
                    assertThat(element.getDetails()).isEqualTo(error.getDetails());
                })
                .expectComplete()
                .verify();
    }

    @Test
    void saveTwoErrors_withSameKeys_AndGetDuplicateKeyException() {
        ErrorDomain testError1 = TestErrorDomainBuilder.anErrorDomain()
                .withId("key_" + System.currentTimeMillis()).build();
        ErrorDomain testError2 = TestErrorDomainBuilder.anErrorDomain()
                .withId(testError1.getId()).build();

        var error1Mono = errorRepository.save(testError1);
        var error2Mono = errorRepository.save(testError2);
        error1Mono.block();
        assertThatThrownBy(error2Mono::block).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void saveWeather_withTooLongFields_InPostgreSQLDB_AndGetBadSqlGrammarException() {
        ErrorDomain testError1 = TestErrorDomainBuilder.anErrorDomain()
                .withId("__________________________________________________________________________________key_"
                        + System.currentTimeMillis()).build();
        var error1Mono = errorRepository.save(testError1);
        assertThatThrownBy(error1Mono::block).isInstanceOf(BadSqlGrammarException.class);

    }
}
