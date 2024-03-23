package ru.madmax.pet.microcurrency.consumer.service.stub;

import org.springframework.stereotype.Service;

@Service
public class SourceCacheStubImpl implements SourceCacheStub {
    @Override
    public Long getIdBySource(String source) {
        return 1L;
    }
}
