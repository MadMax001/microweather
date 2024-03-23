package ru.madmax.pet.microcurrency.consumer.repository;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.r2dbc.UncategorizedR2dbcException;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.madmax.pet.microcurrency.consumer.model.ErrorEntity;
import ru.madmax.pet.microcurrency.consumer.model.TestErrorDomainBuilder;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataR2dbcTest
@ActiveProfiles("test")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Tag("EmbeddedKafka+H2")
class ErrorRepositoryTest {
    final ErrorRepository errorRepository;

    @Test
    void saveError_AndFindIt() {
        ErrorEntity errorEntity = TestErrorDomainBuilder.anErrorDomain()
                .withId("key_" + System.currentTimeMillis()).build();
        ErrorEntity error = errorRepository.save(errorEntity).block();

        assertThat(error).isNotNull();
        assertThat(error.getId()).isEqualTo(errorEntity.getId());

        Mono<ErrorEntity> errorDBMono = errorRepository.findById(error.getId());
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
        ErrorEntity testError1 = TestErrorDomainBuilder.anErrorDomain()
                .withId("key_" + System.currentTimeMillis()).build();
        ErrorEntity testError2 = TestErrorDomainBuilder.anErrorDomain()
                .withId(testError1.getId()).build();

        var error1Mono = errorRepository.save(testError1);
        var error2Mono = errorRepository.save(testError2);
        error1Mono.block();
        assertThatThrownBy(error2Mono::block).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void saveError_withTooLongFields_InH2DB_AndGetUncategorizedR2dbcException() {
        ErrorEntity testError1 = TestErrorDomainBuilder.anErrorDomain()
                .withId("__________________________________________________________________________________key_"
                        + System.currentTimeMillis()).build();
        var error1Mono = errorRepository.save(testError1);
        assertThatThrownBy(error1Mono::block).isInstanceOf(UncategorizedR2dbcException.class);

    }

}