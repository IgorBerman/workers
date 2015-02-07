package com.tenant.sample.service;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Optional;
import com.tenant.sample.Sample;
import com.tenant.sample.dal.mapper.SampleMapper;

@Service
public class SampleServiceImpl implements SampleService {
    private static final Logger logger = LoggerFactory.getLogger(SampleServiceImpl.class);
    @Inject ConfigurationService configurationService;
    @Inject SampleMapper mapper;

    @Override
    @Transactional(rollbackFor=Exception.class)
    public int add(Sample sample) {
        logger.info("add");
        String prefix = configurationService.getProperty("a");
        sample.setDescription(prefix + sample.getDescription());
        mapper.add(sample);
        return sample.getId();
    }

    @Override
    @Transactional(rollbackFor=Exception.class)
    public void update(Sample sample) {
        logger.info("update");
        mapper.update(sample);
    }

    @Override
    @Transactional(rollbackFor=Exception.class)
    public void remove(int id) {
        logger.info("remove");
        mapper.remove(id);
    }

    @Override
    @Transactional(readOnly=true)
    public List<Sample> readAll() {
        logger.info("readAll");
        return mapper.readAll();
    }

    @Override
    @Transactional(readOnly=true)
    public Optional<Sample> readById(int id) {
        logger.info("readById");
        Sample r = mapper.readById(id);
        return r != null ? Optional.of(r) : Optional.<Sample>absent();
    }

    @Override
    @Transactional(rollbackFor=Exception.class)
    public void initDB() {
        logger.info("initDB");
        mapper.executeUpdate("create table sample (id integer auto_increment not null, description text)");
    }
}
