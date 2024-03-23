package ru.madmax.pet.microcurrency.consumer.repository;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.r2dbc.BadSqlGrammarException;
import org.springframework.r2dbc.UncategorizedR2dbcException;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.madmax.pet.microcurrency.consumer.model.TestConversionEntityBuilder;
import ru.madmax.pet.microcurrency.consumer.model.ConversionEntity;

import static org.assertj.core.api.Assertions.*;

@DataR2dbcTest
@ActiveProfiles("test")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Tag("EmbeddedKafka+H2")
class ConversionRepositoryTest {
    final ConversionRepository conversionRepository;

    @Test
    void saveConversion_AndFindIt() {
        ConversionEntity conversionEntity = TestConversionEntityBuilder.aConversionEntity()
                .withId("key_" + System.currentTimeMillis()).build();
        ConversionEntity conversionDB = conversionRepository.save(conversionEntity).block();

        assertThat(conversionDB).isNotNull();
        assertThat(conversionDB.getId()).isEqualTo(conversionEntity.getId());

        Mono<ConversionEntity> currencyDBMono = conversionRepository.findById(conversionDB.getId());
        StepVerifier.create(currencyDBMono)
                .assertNext(element -> {
                    assertThat(element.getId()).isEqualTo(conversionDB.getId());
                    assertThat(element.getBase()).isEqualTo(conversionDB.getBase());
                    assertThat(element.getConvert()).isEqualTo(conversionDB.getConvert());
                    assertThat(element.getBaseAmount()).isEqualByComparingTo(conversionDB.getBaseAmount());
                    assertThat(element.getConversionAmount()).isEqualByComparingTo(conversionDB.getConversionAmount());
                    assertThat(element.getSourceId()).isEqualTo(conversionDB.getSourceId());
                })
                .expectComplete()
                .verify();


    }

    @Test
    void saveTwoConversions_withSameKeys_AndGetDuplicateKeyException() {
        ConversionEntity conversionEntity1 = TestConversionEntityBuilder.aConversionEntity()
                .withId("key_" + System.currentTimeMillis()).build();
        ConversionEntity conversionEntity2 = TestConversionEntityBuilder.aConversionEntity()
                .withId(conversionEntity1.getId()).build();

        var currency1Mono = conversionRepository.save(conversionEntity1);
        var currency2Mono = conversionRepository.save(conversionEntity2);
        currency1Mono.block();
        assertThatThrownBy(currency2Mono::block).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void saveConversion_withTooLongFields_InH2DB_AndGetUncategorizedR2dbcException() {
        ConversionEntity conversionEntity1 = TestConversionEntityBuilder.aConversionEntity()
                .withId("__________________________________________________________________________________key_"
                        + System.currentTimeMillis()).build();
        var currency1Mono = conversionRepository.save(conversionEntity1);
        assertThatThrownBy(currency1Mono::block).isInstanceOf(UncategorizedR2dbcException.class);

    }

}
